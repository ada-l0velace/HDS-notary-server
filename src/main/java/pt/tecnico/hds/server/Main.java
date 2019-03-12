package pt.tecnico.hds.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;

public class Main {


    private static final String url = "jdbc:sqlite:db/hds.db";

    private static Connection conn = null;

    public static void main(String[] args) {
        int port = 19999;
        int count = 0;
        try{
            createDatabase();
            ServerSocket socket1 = new ServerSocket(port);
            socket1.setReuseAddress(true);
            System.out.println("HDS Notary Server Initialized");
            while (true) {
                Socket connection = socket1.accept();

                // obtaining input and out streams
                DataInputStream dis = new DataInputStream(connection.getInputStream());
                DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
                Runnable runnable = new HdsServer(connection, ++count, dis, dos);
                Thread thread = new Thread(runnable);
                thread.start();
            }
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }



    public static void createDatabase(){


        // SQL statement for creating a new table
        String sql = "CREATE TABLE IF NOT EXISTS notary (\n"
                + "	goodsId text PRIMARY KEY ,\n"
                + "	userId text NOT NULL,\n"
                + "	onSale boolean NOT NULL\n"
                + ");";
        try {
            if (!Files.exists(Paths.get("db/hds.db"))) {
                conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                // create a new table
                stmt.execute(sql);
                populate();
             }
             else{
                conn = DriverManager.getConnection(url);
             }
        } catch (SQLException e) {
                System.out.println(e.getMessage());
        }
    }

    public static void populate(){

        insert("good1", "user5");
        insert("good2", "user3");
        insert("good3", "user9");
        insert("good4", "user10");
        insert("good5", "user6");
        insert("good6", "user8");
        insert("good7", "user1");
        insert("good8", "user4");
        insert("good9", "user7");
        insert("good10", "user3");
        insert("good11", "user2");
        insert("good12", "user10");
        insert("good13", "user9");
        insert("good14", "user8");
        insert("good15", "user7");
        insert("good16", "user6");
        insert("good17", "user5");
        insert("good18", "user4");
        insert("good19", "user3");
        insert("good20", "user2");
        insert("good21", "user1");
    }

    public static void insert(String goodsId, String userId){
        String sql = "INSERT INTO notary(goodsId, userId, onSale) Values(?,?, FALSE )";

        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, goodsId);
            pstmt.setString(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }


}
