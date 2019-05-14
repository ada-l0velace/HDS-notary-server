package pt.tecnico.hds.server;

import org.json.JSONObject;

public interface Broadcast {
    JSONObject waitForEcho(JSONObject request);
    JSONObject echo(String msg, String sig);
    void broadcast(String msg);
    void setManager(JSONObject j, JSONObject reply);
    void getEcho(JSONObject j);
}
