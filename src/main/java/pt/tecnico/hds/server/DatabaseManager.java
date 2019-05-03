package pt.tecnico.hds.server;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;

public class DatabaseManager {
    private String url;
    private String path;

    private Connection conn = null;

    public DatabaseManager() {
        path = "db/hds.db";
        url = String.format("jdbc:sqlite:"+path);
        createDatabase();
    }

    public DatabaseManager(int notaryId) {
        path = String.format("db/hds%d.db", notaryId);
        url = "jdbc:sqlite:"+path;
        createDatabase();
    }

    public void createDatabase(){
        String pragmaMode = "PRAGMA journal_mode = WAL;";
        String notary = "CREATE TABLE IF NOT EXISTS notary (\n"
                + "	goodsId text PRIMARY KEY ,\n"
                + "	userId text NOT NULL,\n"
                + "	onSale boolean NOT NULL\n"
                + ");";

        String users = "CREATE TABLE IF NOT EXISTS users (userId text PRIMARY KEY);";
        String goods = "CREATE TABLE IF NOT EXISTS goods (goodsId text PRIMARY KEY);";
        String requests = "CREATE TABLE IF NOT EXISTS requests(\n"
                + " requestId text PRIMARY KEY, \n"
                + " message text NOT NULL\n"
                +");";
        try {
            if (!Files.exists(Paths.get(path))) {
                conn = DriverManager.getConnection(url);
                Statement stmt1 = conn.createStatement();
                Statement stmt2 = conn.createStatement();
                Statement stmt3 = conn.createStatement();
                Statement stmt4 = conn.createStatement();
                //Statement stmt0 = conn.createStatement();
                // create a new table
                stmt1.execute(notary);
                stmt2.execute(users);
                stmt3.execute(goods);
                stmt4.execute(requests);
                populate();
                //stmt0.execute(pragmaMode);
                conn.close();
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }


    public void populate(){

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
        insert("user1", "userId", "users");
        insert("user2", "userId", "users");
        insert("user3", "userId", "users");
        insert("user4", "userId", "users");
        insert("user5", "userId", "users");
        insert("user6", "userId", "users");
        insert("user7", "userId", "users");
        insert("user8", "userId", "users");
        insert("user9", "userId", "users");
        insert("user10", "userId", "users");
        insert("good1", "goodsId", "goods");
        insert("good2", "goodsId", "goods");
        insert("good3", "goodsId", "goods");
        insert("good4", "goodsId", "goods");
        insert("good5", "goodsId", "goods");
        insert("good6", "goodsId", "goods");
        insert("good7", "goodsId", "goods");
        insert("good8", "goodsId", "goods");
        insert("good9", "goodsId", "goods");
        insert("good10", "goodsId", "goods");
        insert("good11", "goodsId", "goods");
        insert("good12", "goodsId", "goods");
        insert("good13", "goodsId", "goods");
        insert("good14", "goodsId", "goods");
        insert("good15", "goodsId", "goods");
        insert("good16", "goodsId", "goods");
        insert("good17", "goodsId", "goods");
        insert("good18", "goodsId", "goods");
        insert("good19", "goodsId", "goods");
        insert("good20", "goodsId", "goods");
        insert("good21", "goodsId", "goods");

    }

    public void insert(String goodsId, String userId){
        String sql = "INSERT INTO notary(goodsId, userId, onSale) Values(?,?, FALSE )";

        try {
            conn = DriverManager.getConnection(url);
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, goodsId);
            pstmt.setString(2, userId);
            pstmt.executeUpdate();
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void insert(String id, String type, String table){
        String sql = "INSERT INTO " + table + "(" + type + ") Values (?)";
        try{
            conn = DriverManager.getConnection(url);
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, id);
            pstmt.executeUpdate();
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
