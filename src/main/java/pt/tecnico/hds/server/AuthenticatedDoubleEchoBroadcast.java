package pt.tecnico.hds.server;

import org.json.JSONObject;


public class AuthenticatedDoubleEchoBroadcast extends AuthenticatedBroadcast {
    BroadcastValue[] reads;
    int responses2;
    int acks2;
    public AuthenticatedDoubleEchoBroadcast(Notary notary) {
        super(notary);
        reads = new BroadcastValue[notary.nServers];
        acks2 = 0;
        responses2 = 0;
    }

    @Override
    public void init() {
       super.init();
       reads = new BroadcastValue[notary.nServers];
       acks2 = 0;
       responses2 = 0;
    }

    public String buildMessage2(String request) {
        JSONObject echoMessage = new JSONObject();
        echoMessage.put("Action","Ready");
        echoMessage.put("pid",notary.notaryIndex);
        echoMessage.put("Value",request);
        return notary.buildReply(echoMessage).toString();
    }

    @Override
    public void doubleEcho(String request) {
        for (int i = 0; i < Main.N; i++) {
            notary.connectToServer("localhost", notary._port + i, buildMessage2(request));
        }
    }

    @Override
    public synchronized void ready(int pid, String message) {
        logger.info(String.format("Starting Ready from %d to %d: ", pid, notary.notaryIndex));

        BroadcastValue bv = new BroadcastValue(message, pid);

        if(reads[pid] == null) {
            responses2++;
            reads[pid] = bv;
            for (int i = 0; i < notary.nServers; i++) {
                if(reads[i]!= null && reads[i].equals(bv)) {
                    acks2++;
                    if(acks2>Main.f && !sentReady) {
                        sentReady = true;
                        doubleEcho(message);
                    }
                    if(acks2>2*Main.f && !this.delivered) {
                        //releaseLock();
                        delivered=true;
                        notifyAll();
                        logger.info(String.format("|Ready :) %d Achieved QORUM|",notary.notaryIndex));
                        //logger.info(message);

                    }
                }
            }
            if(responses2 > (Main.N+Main.f)/2 && acks2<2*Main.f) {
                logger.info(String.format("|Replay QORUM not achieved :( %d |",notary.notaryIndex));
                Thread[] list = new Thread[Thread.activeCount()];
                Thread.currentThread().getThreadGroup().enumerate(list);
                for(Thread t:list) {
                    if (t.getId()==waitID) {
                        t.interrupt();
                    }
                }


            }
                //releaseLock();
            }

        }
    }

