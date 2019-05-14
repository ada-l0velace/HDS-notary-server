package pt.tecnico.hds.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class NotaryStarter implements Runnable  {
    private int _port;
    private Notary _notary;

    NotaryStarter(int port, Notary server) {
        _notary = server;
        _port = port;
    }

    public void run() {
        int count = 0;
        try {
            ServerSocket socket1 = new ServerSocket(_port);
            socket1.setReuseAddress(true);
            //System.out.println("HDS Client Server Starter Initialized");
            while (true) {
                Socket connection = socket1.accept();

                // obtaining input and out streams
                DataInputStream dis = new DataInputStream(connection.getInputStream());
                DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
                Runnable runnable = new HdsServer(connection, ++count, dis, dos, _notary);
                Thread thread = new Thread(runnable);
                thread.start();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
