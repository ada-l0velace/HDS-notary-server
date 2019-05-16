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
<<<<<<< HEAD
    boolean sentEcho;
    boolean delivered;
    boolean sentReady;
    int acks;
    int responses;
    long waitID;
    Mutex[] mutex;
=======
    boolean[] sentEcho;
    boolean[] delivered;
    boolean[] sentReady;
    int[] acks;
    int[] responses;
    Mutex mutex;

    long waitID;
>>>>>>> 5a21496b414f94f7892dffba6c962a436aa2ca4a
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

        echos = new BroadcastValue[notary.nServers];
        mutex = new Mutex[notary.nServers];
        echoMutex = new Mutex();
        sem= new Semaphore[notary.nServers];
        sentEcho = false;
        delivered = false;
        sentReady = false;
        acks = 0;
        responses = 0;
        for (int i = 0; i < Main.N; i++) {
            sem[i] = new Semaphore(0);
        }
        for (int i = 0; i < Main.N; i++) {
            mutex[i] = new Mutex();
        }

    }

    public void clear() {
        init();
    }

    @Override
    public synchronized void broadcast(String request) {
        if (!sentEcho) {
            sentEcho = true;
            for (int i = 0; i < notary.nServers; i++) {
                notary.connectToServer("localhost", notary._port + i, buildMessage(request));
            }
        }

        while (!delivered) {

            try {
                logger.info(String.format("#########%d is LOCKED########", notary.notaryIndex));
                waitID=Thread.currentThread().getId();
<<<<<<< HEAD
                wait();

                //getLock().acquire();
=======
                getLock().acquire();
>>>>>>> 5a21496b414f94f7892dffba6c962a436aa2ca4a
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
            responses++;
            echos[pid] = bv;
            for (int i = 0; i < Main.N; i++) {
                if (echos[i] != null && echos[i].equals(bv)) {
                    acks++;
                }
                if (!sentReady && acks > (Main.N + Main.f) / 2) {
                    logger.info(String.format("|Echo :) %d Achieved QORUM|",notary.notaryIndex));
                    sentReady = true;
                    doubleEcho(message);
                }
<<<<<<< HEAD
                else if (responses > (Main.N + Main.f) / 2 && acks < 2 * Main.f) {
                    Thread[] list = new Thread[Thread.activeCount()];
                    Thread.currentThread().getThreadGroup().enumerate(list);
                    for(Thread t:list) {
                        if (t.getId()==waitID) {
                            t.interrupt();
                        }
                    }
                }
            }

        }
        //echoMutex.unlock();
=======
            }
            if (responses[ni] > (Main.N + Main.f) / 2 && acks[ni] < 2 * Main.f) {
                logger.info(String.format("|Replay QORUM not achieved :( %d |",notary.notaryIndex));
                releaseLock();
            }

        }

>>>>>>> 5a21496b414f94f7892dffba6c962a436aa2ca4a
    }

    public void ready(int pid, String message){}

    public void doubleEcho(String ready) {}

    public boolean isDelivered(){
        return delivered;
    }

    @Override
    public Semaphore getLock() {
        return sem[ni];
    }
}
