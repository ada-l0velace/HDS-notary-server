package pt.tecnico.hds.server;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.*;

public class Notary {

	public eIDLib_PKCS11 cc;
	
	public Notary() {
		try {
			cc = new eIDLib_PKCS11();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
    private Connection connect() {
        // SQLite connection string
        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:db/hds.db");
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
        String buyer = message.getString("Buyer");
        JSONObject reply = new JSONObject();
        if (Utils.verifySignWithPubKeyFile(message.toString(), hash,"assymetricKeys/" + buyer + ".pub")) {
            String good = message.getString("Good");
            String sql = "SELECT userId, onSale FROM notary WHERE goodsId = ?";
            Boolean result = false;

            try {
                Connection conn = this.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                if (verifyReplay(hash, conn) && isReal("goodsId", "goods", good, conn)) {
                    addToRequests(hash, conn);
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
                }
                else{
                    reply.put("Action", "NO");
                }
                conn.close();
            } catch (SQLException e) {
                reply.put("Action", "NO");
                System.out.println(e.getMessage());
            }
        }
        else{
            reply.put("Action", "NO");
        }
        return reply;
    }

    public JSONObject transferGood(JSONObject message, JSONObject message2, String hash, String hash2) {
        String seller = message.getString("Seller");
        String buyer = message.getString("Buyer");
        JSONObject reply = new JSONObject();
        if (Utils.verifySignWithPubKeyFile(message.toString(), hash, "assymetricKeys/" + seller + ".pub") &&
            Utils.verifySignWithPubKeyFile(message2.toString(), hash2, "assymetricKeys/" + buyer + ".pub")) {
            String good = message.getString("Good");

            String sql = "UPDATE notary SET onSale = FALSE , userId = ? WHERE goodsId = ?";

            try {
                Connection conn = this.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                if (verifyReplay(hash, conn) && verifyReplay(hash2, conn)) {
                    addToRequests(hash, conn);
                    addToRequests(hash2, conn);
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
                    } else {
                        reply.put("Action", "NO");
                    }
                }  else {
                    reply.put("Action", "NO");
                }
                conn.close();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
                reply.put("Action", "NO");

            }
        } else {
            reply.put("Action", "NO");
        }
        return reply;
    }

    public JSONObject intentionToSell(JSONObject message, String hash) {
        String seller = message.getString("Seller");
        JSONObject reply = new JSONObject();

        if (Utils.verifySignWithPubKeyFile(message.toString(), hash, "assymetricKeys/" + seller + ".pub")) {

            String goodsId = message.getString("Good");

            String sql = "UPDATE notary SET onSale = ? WHERE goodsId = ?";


            try {
                Connection conn = this.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                if (verifyReplay(hash, conn)) {
                	System.out.println("Got Here");
                	addToRequests(hash, conn);
                	query();
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
                	System.out.println("REPLAY");
                    reply.put("Action", "NO");
                }
                conn.close();
            } catch (SQLException e) {
                reply.put("Action", "NO");
                System.out.println(e.getMessage());
            }
        } else {
            reply.put("Action", "NO");
        }
        return reply;
    }

    public void query() {
        try {
//          String sql = "SELECT goodsId, userId, onSale FROM notary";
            String sql = "SELECT requestId FROM requests";

            Connection conn = this.connect();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            // Only expecting a single result
            while (rs.next()) {
//                System.out.println(rs.getString("goodsId") +  "\t" +
//                        rs.getString("userId") + "\t" +
//                        rs.getBoolean("onSale"));
                System.out.println(rs.getString("requestId"));
            }




        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addToRequests(String hash, Connection conn){
        String sql = "INSERT INTO requests(requestId) Values(?)";

        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, hash);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public JSONObject buildReply(JSONObject j) {
        JSONObject reply = new JSONObject();
        j.put("Timestamp", new java.util.Date().toString());
        reply.put("Message", j.toString());
        try {
			reply.put("Hash",cc.signWithPrivateKey(j.toString()));
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
}
