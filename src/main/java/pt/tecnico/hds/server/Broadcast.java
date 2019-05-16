package pt.tecnico.hds.server;

import org.json.JSONObject;

import java.util.concurrent.Semaphore;

public interface Broadcast {
    void clear();
    void broadcast(String request);
    void echo(int pid, String message);
    void ready(int pid, String message);
    void doubleEcho(String request);
    boolean isDelivered();
    Semaphore getLock();
}
