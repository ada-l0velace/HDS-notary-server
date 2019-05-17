package pt.tecnico.hds.server;


import org.apache.log4j.PropertyConfigurator;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.awt.Mutex;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class AuthenticatedBroadcast implements Broadcast {
    Notary notary;
    boolean[] sentEcho;
    boolean[] delivered;
    boolean[] sentReady;
    int[][] acks;
    int[][] responses;
    Mutex mutex;

    long waitID;
    int ni;
    Mutex echoMutex;
    Semaphore[] sem;
    BroadcastValue[] echos;
    public final static Logger logger = LoggerFactory.getLogger(AuthenticatedBroadcast.class);

    public AuthenticatedBroadcast(Notary notary) {
        PropertyConfigurator.configure(getClass().getClassLoader().getResource("logsServerThread.properties"));
        this.notary = notary;
        ni = notary.notaryIndex;
        init();
    }


    public void init() {

        echos = new BroadcastValue[Notary.nServers];
        echoMutex = new Mutex();
        sem= new Semaphore[Notary.nServers];
        sentEcho = new boolean[Notary.nServers];
        delivered = new boolean[Notary.nServers];
        sentReady = new boolean[Notary.nServers];
        acks = new int [Main.N][Main.N];
        responses = new int [Main.N][Main.N];
        for (int i = 0; i < Main.N; i++) {
            sem[i] = new Semaphore(0);
        }

    }

    public void clear() {
        init();
    }

    @Override
    public void broadcast(String request) {
        if (!sentEcho[ni]) {
            sentEcho[ni] = true;
            for (int i = 0; i < Notary.nServers; i++) {
                //new BroadcastThread(notary,"localhost", notary._port + i, buildMessage(request)).run();
                notary.connectToServer("localhost", Notary._port + i, buildMessage(request));
            }

        }

        if (!delivered[ni]) {

            try {
                logger.info(String.format("#########%d is LOCKED########", notary.notaryIndex));
                getLock().acquire();
                //sem.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
                //getLock().release();
            }

        }
    }

    public String buildMessage(String request) {
        JSONObject echoMessage = new JSONObject();
        echoMessage.put("Action","Echo");
        echoMessage.put("pid",notary.notaryIndex);
        echoMessage.put("Value",request);
        return notary.buildReply(echoMessage).toString();
    }

    // pid, message

    public void releaseLock() {
        getLock().release();
    }

    @Override
    public synchronized void echo(int pid, String message) {
        //echoMutex.lock();
        logger.info(String.format("Starting Echo from %d to %d: ", pid, notary.notaryIndex));
        BroadcastValue bv = new BroadcastValue(message, pid);

        if (echos[pid] == null) {
            responses[ni][pid]++;
            echos[pid] = bv;
            for (int i = 0; i < Main.N; i++) {
                if (echos[i] != null && echos[i].equals(bv) && bv.verifySignature()) {
                    acks[ni][pid]++;
                }
                if (!sentReady[ni] && acks[ni][pid] > (Main.N + Main.f) / 2) {
                    logger.info(String.format("|Echo :) %d Achieved QORUM with %d acks|",notary.notaryIndex, acks[ni][pid]));
                    sentReady[ni] = true;
                    doubleEcho(message);
                }
            }
            if (responses[ni][pid] > (Main.N + Main.f) / 2 && acks[ni][pid] < 2 * Main.f) {
                logger.info(String.format("|Replay QORUM not achieved :( %d |",notary.notaryIndex));
                releaseLock();
            }

        }

    }

    public void ready(int pid, String message){}

    public void doubleEcho(String ready) {}

    public boolean isDelivered(){
        return delivered[ni];
    }

    @Override
    public Semaphore getLock() {
        return sem[ni];
    }
}
