package pt.tecnico.hds.server;

import org.json.JSONObject;

public class BroadcastThread implements Runnable {
    Notary notary;
    String localhost;
    int i;
    String jsonObject;
    public BroadcastThread(Notary notary, String localhost, int i, String jsonObject) {
        this.notary = notary;
        this.localhost = localhost;
        this.i = i;
        this.jsonObject = jsonObject;
    }

    public void run() {
        notary.connectToServer("localhost", notary._port, jsonObject);
    }
}
