package pt.tecnico.hds.server;

import org.json.JSONObject;

import java.sql.*;

public class Notary {


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
            System.out.println(type + " " + id + " nonexistent");
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
            System.out.println(owner + " IS NOT OWNER OF " + good);
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


    public JSONObject getStateOfGood(String good) {
        String sql = "SELECT userId, onSale FROM notary WHERE goodsId = ?";
        JSONObject reply = new JSONObject();
        Boolean result = false;

        try {
            Connection conn = this.connect();
            PreparedStatement pstmt =    conn.prepareStatement(sql);
            pstmt.setString(1, good);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                result = rs.getBoolean("onSale");
                String user = rs.getString("userId");
                reply.put("OnSale", result.toString());
                reply.put("Owner", user);
                reply.put("Good", good);
                conn.close();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return reply;
    }

    public JSONObject transferGood(JSONObject message, String hash) {
        String seller = message.getString("Seller");
        JSONObject reply = new JSONObject();
        if (Utils.verifySignWithPubKey(message.toString(), hash, "assymetricKeys/" + seller + ".pub")) {
            String buyer = message.getString("Buyer");
            String good = message.getString("Good");

            String sql = "UPDATE notary SET onSale = FALSE , userId = ? WHERE goodsId = ?";
            System.out.println(buyer + " " + seller);

            try {
                Connection conn = this.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                if (isReal("userId", "users", buyer, conn) &&
                        isReal("userId", "users", seller, conn) &&
                        isReal("goodsId", "goods", good, conn) &&
                        isOnSale(good, conn) &&
                        !buyer.equals(seller)) {
                    pstmt.setString(1, buyer);
                    pstmt.setString(2, good);
                    pstmt.executeUpdate();
                    reply.put("Action", "YES");
                } else {
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

        if (Utils.verifySignWithPubKey(message.toString(), hash, "assymetricKeys/" + seller + ".pub")) {


            String goodsId = message.getString("Good");

            String sql = "UPDATE notary SET onSale = ? WHERE goodsId = ?";


            try {
                Connection conn = this.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                if (verifyReplay(hash, conn)) {
                    System.out.println("Hash verified");
                    query();
                    addToRequests(hash);
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
            } catch (SQLException e) {
                reply.put("Action", "NO");
                System.out.println(e.getMessage());
            }
        } else {
            reply.put("Action", "NO");
        }
        reply.put("Timestamp", new java.util.Date().toString());
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

    public void addToRequests(String hash){
        String sql = "INSERT INTO requests(requestId) Values(?)";

        try {
            Connection conn = this.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, hash);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

}
