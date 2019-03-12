package pt.tecnico.hds.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    public static void main(String[] args) {
        int port = 19999;
        int count = 0;
        try {
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
}
