package pt.tecnico.hds.server;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.sql.*;
import org.json.JSONObject;

public class HdsServer implements Runnable {

    private static final String url = "jdbc:sqlite:db/hds.db";

    private Socket connection;
    private String TimeStamp;
    private int ID;
    private DataInputStream dis;
    private DataOutputStream dos;

    private Connection connect() {
        // SQLite connection string
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    public HdsServer(Socket s, int i, DataInputStream dis, DataOutputStream dos) {
        this.connection = s;
        this.ID = i;
        this.dis = dis;
        this.dos = dos;
    }

    public void run() {
        String received = "";
        String toreturn = "";
        System.out.println("Server " + this.connection + " Opens...");
        while (true) {
            try {
                // receive the answer from client
                received = dis.readUTF();

                if (received.equals("Exit")) {
                    break;
                }

                this.TimeStamp = new java.util.Date().toString();

                // write on output stream based on the
                // answer from the client

                JSONObject jsonObj = new JSONObject(received);
                jsonObj = new JSONObject(jsonObj.getString("Message"));
                received = jsonObj.getString("Action");

                switch (received) {

                    case "transferGood" :
                        toreturn = transferGood(jsonObj.getString("Buyer"), jsonObj.getString("Seller"), jsonObj.getString("Good"));
                        dos.writeUTF(toreturn);
                        break;

                    case "intentionToSell" :
                        JSONObject message = intentionToSell(jsonObj.getString("Good"));
                        //System.out.println(toreturn);
                        toreturn = buildReply(message).toString();
                        dos.writeUTF(toreturn);
                        System.out.println(toreturn);
                        break;

                    case "getStateOfGood" :
                        toreturn = getStateOfGood(jsonObj.getString("Good"));
                        dos.writeUTF(toreturn);
                        break;

                    default:
                        dos.writeUTF("Invalid input");
                        break;
                }
            }

            catch (EOFException | SocketException eofError) { // Normally Occurs when the client socket dies
                eofError.printStackTrace();
                //System.out.println(e0.getMessage());
                break;
            }

            // Client Socket closed
            //System.out.println(socketEx.getMessage());

            catch (Exception e) {
                e.printStackTrace();
                System.out.println(received);
                try {
                    dos.writeUTF("Invalid input");
                }
                catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        try {
            System.out.println("Client " + this.connection + " sends exit...");
            System.out.println("Closing this connection.");
            this.connection.close();
            System.out.println("Connection closed");
            this.dis.close();
            this.dos.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JSONObject buildReply(JSONObject j){
        JSONObject reply = new JSONObject();
        reply.put("Message", j.toString());
        reply.put("Hash", Utils.getSHA256(j.toString()));
        return reply;
    }

    public String getStateOfGood(String good){
        String sql = "SELECT onSale FROM notary WHERE goodsId = ?";
        Boolean result = false;

        try (Connection conn = this.connect();
             PreparedStatement pstmt  = conn.prepareStatement(sql)) {

            pstmt.setString(1, good);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()){
                result = rs.getBoolean("onSale");
                //query();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        if (result)
            return "FOR SALE";
        else
            return "NOT FOR SALE";
    }

    public String transferGood(String buyer, String seller, String good){
        String sql = "UPDATE notary SET onSale = FALSE , userId = ? WHERE goodsId = ?";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, buyer);
            pstmt.setString(2, good);
            pstmt.executeUpdate();
            //query();
            return "YES";
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return "NO";
    }

    public JSONObject intentionToSell(String goodsId){
        String sql = "UPDATE notary SET onSale = ? WHERE goodsId = ?";
        JSONObject reply = new JSONObject();

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBoolean(1, true);
            pstmt.setString(2, goodsId);
            pstmt.executeUpdate();
            //query();
            reply.put("Action", "YES");

        } catch (SQLException e) {
            reply.put("Action","NO");
            System.out.println(e.getMessage());
        }
        reply.put("Timestamp", new java.util.Date().toString());
        return reply;
    }

    public void query() {
        try {
            Connection conn = this.connect();
            String sql = "SELECT goodsId, userId, onSale FROM notary";

            Statement stmt  = conn.createStatement();
            ResultSet rs    = stmt.executeQuery(sql);
            // Only expecting a single result
            while (rs.next()) {
                System.out.println(rs.getString("goodsId") +  "\t" +
                        rs.getString("userId") + "\t" +
                        rs.getBoolean("onSale"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}
