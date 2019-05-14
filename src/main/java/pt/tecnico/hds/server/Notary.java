package pt.tecnico.hds.server;

import com.sun.net.httpserver.Authenticator;
import com.sun.org.apache.bcel.internal.generic.JsrInstruction;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Notary {

	public SigningInterface cc;
	public static final int _port = 19999;
	public int notaryIndex;
	public static final int nServers = 4;
	public static String path;
	public ByzantineRegister reg;
	protected Broadcast rm;
	public final static Logger logger = LoggerFactory.getLogger(HdsServer.class);


	public Notary() {
		try {
			new DatabaseManager().createDatabase();
			if (Main.debug)
				cc = new DebugSigning(0);
			else
				cc = new eIDLib_PKCS11();
			path = "db/hds0.db";
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		reg = new ByzantineAtomicRegister(this);
		rm = new AuthenticatedDoubleEchoBroadcast(this);
		notaryIndex = 0;
		System.out.println("HDS-server starting");
		startServer();

		//populateRegister();

	}

	public Notary(int i) {
		try {
			new DatabaseManager(i).createDatabase();
		    if (Main.debug || i > 0)
		        cc = new DebugSigning(i);
		    else
		        cc = new eIDLib_PKCS11();

			notaryIndex = i;
			path = String.format("db/hds%d.db", i);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		//reg = new ByzantineAtomicRegister(this);
		reg = new ByzantineAtomicRegister(this);
		rm = new AuthenticatedDoubleEchoBroadcast(this);
		System.out.println("HDS-server starting");
		startServer();
		//populateRegister();
		System.out.println("Server Ready");

	}

	public void startServer() {
		Runnable runnable = new NotaryStarter(_port+ notaryIndex, this);
		Thread thread = new Thread(runnable);
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
    public static Connection connect() {
        // SQLite connection string
        Connection conn = null;
        try {
        	System.out.println("Connection Opening");
            conn = DriverManager.getConnection("jdbc:sqlite:"+ path);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    public boolean verifyReplay(String hash, Connection conn) {
        String sql = "SELECT requestId FROM requests WHERE requestId=?";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, hash);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean isReal(String type, String table, String id, Connection conn) {
        String sql = "SELECT " + type + " FROM " + table + " WHERE " + type + " = ?";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next())
                return true;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }

    public boolean isOwner(String owner, String good, Connection conn) {
        String sql = "SELECT userId FROM notary WHERE goodsId = ?";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, good);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next() && rs.getString("userId").equals(owner))
                return true;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }

    public boolean isOnSale(String good, Connection conn) {
        String sql = "SELECT onSale FROM notary WHERE goodsId = ?";

        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, good);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("onSale");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }


    public JSONObject getStateOfGood(JSONObject message, String hash) {
    	JSONObject reply = new JSONObject();
    	System.out.println("Starting GetStateOfGood");
    	try {
    		Connection conn = this.connect();
    		String buyer = message.getString("Buyer");
    		if (isReal("userid", "users", buyer, conn) && Utils.verifySignWithPubKeyFile(message.toString(), hash,"assymetricKeys/" + buyer + ".pub")) {
    			String good = message.getString("Good");
    			String sql = "SELECT userId, onSale FROM notary WHERE goodsId = ?";
    			Boolean result = false;
    			PreparedStatement pstmt = conn.prepareStatement(sql);
    			if (verifyReplay(hash, conn) && isReal("goodsId", "goods", good, conn)) {
    				addToRequests(hash, message.toString(), conn);

    				pstmt.setString(1, good);
    				ResultSet rs = pstmt.executeQuery();
    				if (rs.next()) {
    					result = rs.getBoolean("onSale");
    					String user = rs.getString("userId");
    					reply.put("OnSale", result.toString());
    					reply.put("Owner", user);
    					reply.put("Good", good);
                    } else{
    					reply.put("Action", "NO");
                    }
    			} else {
                	reply.put("Action", "NO");
                }
    		} else {
				reply.put("Action", "NO");
    		}
    		conn.close();
        	System.out.println("Connection Closing");

    	} catch (SQLException e) {
    		reply.put("Action", "NO");
    		System.out.println(e.getMessage());
    	}
        return reply;
    }

    public JSONObject transferGood(JSONObject message, JSONObject message2, String hash, String hash2) {
		JSONObject reply = new JSONObject();
    	System.out.println("Starting TransferGood");
		String sql = "UPDATE notary SET onSale = FALSE , userId = ? WHERE goodsId = ?";
		
    	try {
    		Connection conn = this.connect();
    		PreparedStatement pstmt = conn.prepareStatement(sql);
    		String seller = message.getString("Seller");
    		String buyer = message.getString("Buyer");
    		if (isReal("userid", "users", buyer, conn) && isReal("userid", "users", seller, conn) && Utils.verifySignWithPubKeyFile(message.toString(), hash, "assymetricKeys/" + seller + ".pub") &&
    				Utils.verifySignWithPubKeyFile(message2.toString(), hash2, "assymetricKeys/" + buyer + ".pub")) {
    			String good = message.getString("Good");
    			if (verifyReplay(hash, conn) && verifyReplay(hash2, conn)) {
    				addToRequests(hash, message.toString(), conn);
    				addToRequests(hash2, message2.toString(), conn);
    				if (isReal("userId", "users", buyer, conn) &&
    						isReal("userId", "users", seller, conn) &&
    						isReal("goodsId", "goods", good, conn) &&
    						isOwner(seller, good,conn) &&
    						isOnSale(good, conn) &&
    						!buyer.equals(seller)) {

						pstmt.setString(1, buyer);
    					pstmt.setString(2, good);
    					pstmt.executeUpdate();
    					reply.put("Action", "YES");
    					System.out.println("Updating register");
						updateRegister(message, hash);
    				} else {
    					reply.put("Action", "NO");
    				}
    			}  else {
    				reply.put("Action", "NO");
    			}
    		} else {
    			reply.put("Action", "NO");
    		}
			conn.close();
        	System.out.println("Connection Closing");

    	} catch (SQLException e) {
    		System.out.println(e.getMessage());
    		reply.put("Action", "NO");	
    	}
		long ts = message.getLong("wts");
		reg.ack(reply, ts);
        return reply;
    }

    public JSONObject intentionToSell(JSONObject message, String hash) {
        String seller = message.getString("Seller");
		System.out.println("Starting IntentionToSell");

        JSONObject reply = new JSONObject();
        String sql = "UPDATE notary SET onSale = ? WHERE goodsId = ?";
        String goodsId = message.getString("Good");
        try {
        	Connection conn = this.connect();
        	PreparedStatement pstmt = conn.prepareStatement(sql);
        	if (isReal("userid", "users", seller, conn) && Utils.verifySignWithPubKeyFile(message.toString(), hash, "assymetricKeys/" + seller + ".pub") && verifyReplay(hash, conn)) {
        		addToRequests(hash, message.toString(), conn);
        		if (isReal("goodsId", "goods", goodsId, conn) &&
        				isOwner(seller, goodsId, conn)) {

        			pstmt.setBoolean(1, true);
        			pstmt.setString(2, goodsId);
        			pstmt.executeUpdate();
        			//query();
        			reply.put("Action", "YES");
					updateRegister(message, hash);
        		} else
        			reply.put("Action", "NO");
        	} else {
        		reply.put("Action", "NO");
        	}
        conn.close();
    	System.out.println("Connection Closing");

        } catch (SQLException e) {
        	reply.put("Action", "NO");
        	System.out.println(e.getMessage());
        }
		long ts = message.getLong("wts");
        reg.ack(reply, ts);
        return reply;
    }


    public void addToRequests(String hash, String message, Connection conn){
        String sql = "INSERT INTO requests(requestId, message) Values(?, ?)";

        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, hash);
			pstmt.setString(2, message);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public JSONObject buildReply(JSONObject j) {
        JSONObject reply = new JSONObject();
        System.out.println(j);
        j.put("Timestamp", new java.util.Date().getTime());
        j.put("pid", notaryIndex);
		j.put("signer", cc.getKeyName());
        reply.put("Message", j.toString());


		try {
			reply.put("Hash", cc.signWithPrivateKey(j.toString()));
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (Throwable e) {
			e.printStackTrace();
		}
        return reply;
    }
    
    public JSONObject invalid() {
    	JSONObject reply = new JSONObject();
    	reply.put("Action", "NO");
    	return reply;
    }


	public void updateRegister(JSONObject value, String sig) {
		long ts = value.getLong("wts");
		String good = value.getString("Good");
		int pid = notaryIndex;
		long rid = value.getLong("rid");

		System.out.println("Writing to Register");
		reg.write(good, value.toString(), sig, rid, pid, ts);
	}

	public String buildState(String msg, String good, JSONObject request) {
		return reg.read(good, msg, request);
	}

	public String isServerDebug(String name) {
		if (Main.debug)
			return "serverDebug";
		else
			return name;
	}

	public JSONObject writeback(JSONObject j){
			JSONObject reply = new JSONObject();
			JSONObject message = new JSONObject(j.getString("Message"));
			String signer = message.getString("signer");
			long ts = message.getLong("t");
			long rid = message.getLong("rid");
			JSONObject val = message.getJSONObject("v");
			String good = val.getString("Good");
			String sig = j.getString("Hash");
			if (cc.verifySignWithPubKey(message.toString(), sig)){
				if (ts > reg.getGood(good).getTimestamp()){
					//val.put("wts", ts);
					reg.write(good,val.toString(), sig, val.getLong("rid"),val.getInt("pid"), ts);
				}
			}
			reply.put("Action", "ack");
			reply.put("rid", rid);
			return reply;
	}

	public JSONObject connectToServer(String _host, int port, JSONObject _msg){
		JSONObject answer = null;
		int maxRetries = 10;
		int retries = 0;


		while (true) {
			try {
				System.out.println("Connecting to " + port);
				InetAddress ip = InetAddress.getByName(_host);

				Socket s = new Socket(ip, port);
				s.setSoTimeout(10 * 1000);

				DataInputStream dis = new DataInputStream(s.getInputStream());
				DataOutputStream dos = new DataOutputStream(s.getOutputStream());


				try {

					System.out.println("Message to be Sent->" +_msg.toString());
					dos.writeUTF(_msg.toString());

					dis.close();
					dos.close();
					s.close();


				} catch (java.net.SocketTimeoutException timeout) {
					timeout.printStackTrace();
					s.close();
					break;

				} catch (java.io.EOFException e0) {
					e0.printStackTrace();
					s.close();
					break;
				} catch (Exception e) {
					e.printStackTrace();
					s.close();
					break;
				}


			} catch (IOException e) {
				logger.error(e.getMessage() + " on port:" + port);
				//e.printStackTrace();
				retries++;
				if (retries == maxRetries)
					break;
				continue;
			}
			break;
		}
		return answer;
	}
}
