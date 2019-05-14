package pt.tecnico.hds.server;

import org.json.JSONObject;

public interface Broadcast {
    void init();
    void  broadcast(JSONObject request) throws InterruptedException;
    void echo(JSONObject echo);


}
