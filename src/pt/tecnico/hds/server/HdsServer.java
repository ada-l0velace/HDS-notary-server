package pt.tecnico.hds.server;

import java.net.Socket;

public class HdsServer implements Runnable {
    private Socket connection;
    private String TimeStamp;
    private int ID;

    HdsServer(Socket s, int i) {
        this.connection = s;
        this.ID = i;
    }

    public void run() {

    }

}
