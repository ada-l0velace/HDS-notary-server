package pt.tecnico.hds.server;


import org.json.JSONObject;

import java.util.concurrent.Semaphore;

public class AuthenticatedBroadcast implements Broadcast {
    Notary notary;
    boolean sentEcho;
    boolean delivered;
    boolean sentReady;
    int acks;
    int responses;
    Semaphore sem;
    BroadcastValue[] echos;

    public AuthenticatedBroadcast(Notary notary) {
        this.notary = notary;
        init();
    }


    public void init() {
        echos = new BroadcastValue[notary.nServers];
        sentEcho = false;
        delivered = false;
        sentReady = false;
        acks = 0;
        responses = 0;
        sem = new Semaphore(1);
    }

    @Override
    public void broadcast(JSONObject request) {
        System.out.println("starting broadcast");
        if (!sentEcho) {
            sentEcho = true;
            for (int i = 0; i < notary.nServers; i++) {
                notary.connectToServer("localhost", notary._port + i, buildMessage(request));
                // new BroadcastThread(notary,"localhost", notary._port + i, buildMessage(request));
                // for some reason threads don't work
            }
        }

        if(!delivered) {
            try {
                sem.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }

    public JSONObject buildMessage(JSONObject request) {
        JSONObject echoMessage = new JSONObject();
        echoMessage.put("Action","Echo");
        echoMessage.put("pid",notary.notaryIndex);
        echoMessage.put("Value",request);
        return notary.buildReply(echoMessage);
    }

    @Override
    public void echo(JSONObject echo) {

        JSONObject messageE = new JSONObject(echo.getString("Message"));
        int pid = messageE.getInt("pid");
        BroadcastValue bv = new BroadcastValue(echo, pid);

        if(echos[pid] == null) {
            responses++;
            echos[pid] = bv;
            for (int i = 0; i < Main.N; i++) {
                System.out.println("#####################ECHOLOL#################");
                System.out.println(echos[i]);
                System.out.println(bv);
                System.out.println("#############################################");
                if(echos[i] != null & echos[i].equals(bv)) {
                    acks++;
                    System.out.println("ack echo from: "+ bv+ " total acks: "+ acks);
                    System.out.println(acks>(Main.N+Main.f)/2 );
                    System.out.println((Main.N + Main.f)/2);
                    if(!delivered & acks > (Main.N + Main.f)/2) {
                        sentReady = true;
                        delivered = true;
                        sem.release();
                        //acks = 0;
                        //responses = 0;
                        System.out.println(bv.message.toString());
                        doubleEcho(bv.message);
                        System.out.println("Echo phase is done...");
                    }
                }
            }
        }

        /*if(responses > (notary.nServers+1)/2 && acks<2f) {
            //delivered = true;
            sem.release();
        }*/
    }

    public void ready(JSONObject ready){}

    public void doubleEcho(JSONObject ready) {}

}
