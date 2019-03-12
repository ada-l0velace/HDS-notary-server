package pt.tecnico.hds.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;

import static java.lang.System.exit;

public class Main {


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
                Runnable runnable = new HdsServer(connection, ++count);
                Thread thread = new Thread(runnable);
                thread.start();
            }
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static Connection createDatabase(){

        String url = "jdbc:sqlite:db/hds.db";

        // SQL statement for creating a new table
        String sql = "CREATE TABLE IF NOT EXISTS notary (\n"
                + "	goodsId text PRIMARY KEY,\n"
                + "	userId text NOT NULL,\n"
                + "	onSale boolean NOT NULL\n"
                + ");";

        try{
            Connection conn = DriverManager.getConnection(url);
            Statement stmt = conn.createStatement();
            // create a new table
            stmt.execute(sql);
            return conn;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

}
