package pt.tecnico.hds.server;

import org.json.JSONObject;



public class BroadcastThread extends Thread {
    public String host;
    private int port;
    private JSONObject request;
    private Notary notary;

    public BroadcastThread(Notary n, String host, int port, JSONObject request) {
        this.host = host;
        this.port = port;
        this.request = request;
        this.notary = n;
    }

    public void run(){
        notary.connectToServer("localhost", port, request);
    }


}