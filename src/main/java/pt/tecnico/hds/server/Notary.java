package pt.tecnico.hds.server;

import org.json.JSONException;
import org.json.JSONObject;
import java.sql.*;


public class Notary {

	public SigningInterface cc;
	public static final int _port = 19999;
	public int notaryIndex;
	public static String path;
	private static HdsRegister reg = new HdsRegister();
	public Notary() {
		try {
			new DatabaseManager().createDatabase();
			if (Main.debug)
				cc = new DebugSigning();
			else
				cc = new eIDLib_PKCS11();
			path = "db/hds0.db";
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		notaryIndex = 0;
		System.out.println("HDS-server starting");
		startServer();

	}

	public Notary(int i) {
		try {
			new DatabaseManager(i).createDatabase();
		    if (Main.debug)
		        cc = new DebugSigning();
		    else
		        cc = new eIDLib_PKCS11();

			notaryIndex = i;
			path = String.format("db/hds%d.db", i);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("HDS-server starting");
		startServer();

	}

	public void startServer() {
		Runnable runnable = new NotaryStarter(_port+ notaryIndex, this);
		Thread thread = new Thread(runnable);
		thread.start();
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
						System.out.println("WTF LOLOL");

						pstmt.setString(1, buyer);
    					pstmt.setString(2, good);
    					pstmt.executeUpdate();
    					reply.put("Action", "YES");
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
        j.put("Timestamp", new java.util.Date().getTime());
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


	public void updateRegister(JSONObject message) {
		long ts = message.getLong("Timestamp");
		String good = message.getString("Good");
		reg._rid++;

		if (reg.goodExists(good)) {
			if (!reg.checkTimestamp(good, ts)) {
				return;
			}
		}
		/*
		reg.deliveryWrite(good, ts);
		reg.printGoods();
		*/
	}
}
