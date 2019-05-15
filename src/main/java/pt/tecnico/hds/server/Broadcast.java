package pt.tecnico.hds.server;

import org.json.JSONObject;

import java.util.concurrent.Semaphore;

public interface Broadcast {
    void init();
    void broadcast(JSONObject request) throws InterruptedException;
    void echo(int pid, String message);
    void ready(JSONObject ready);
    void doubleEcho(JSONObject request);
    boolean isDelivered();
    Semaphore getLock();
}
