package pt.tecnico.hds.server;


import org.json.JSONObject;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class AuthenticatedBroadcast implements Broadcast {
    Notary notary;
    boolean sentEcho;
    boolean delivered;
    boolean sentReady;
    int acks;
    int acks2;
    int responses;
    Semaphore sem;
    BroadcastValue[] echos;

    public AuthenticatedBroadcast(Notary notary) {
        this.notary = notary;
        init();
    }


    public void init() {
        echos = new BroadcastValue[notary.nServers];
        sem=new Semaphore(0);
        sentEcho = false;
        delivered = false;
        sentReady = false;
        acks = 0;
        acks2 = 0;
        responses = 0;

    }

    @Override
    public void broadcast(JSONObject request) {
        System.out.println("starting broadcast");
        if (!sentEcho) {
            sentEcho = true;
            for (int i = 0; i < notary.nServers; i++) {
                notary.connectToServer("localhost", notary._port + i, buildMessage(request).toString());
                //new BroadcastThread(notary,"localhost", notary._port + i, buildMessage(request));
                // for some reason threads don't work
            }
        }

        if (!delivered) {

            try {
                System.out.println(String.format("#########%d is LOCKED########", notary.notaryIndex));
                sem.acquire();
                //sem.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        System.out.println(String.format("%d is OUT BEWARE!!!!!!!!!!!!!!!!!!!!!!!!!!", notary.notaryIndex));
        System.out.println("THE DELIVERED VAR IS "+delivered);
    }

    public JSONObject buildMessage(JSONObject request) {
        JSONObject echoMessage = new JSONObject();
        echoMessage.put("Action","Echo");
        echoMessage.put("pid",notary.notaryIndex);
        echoMessage.put("Value",request.toString());
        return notary.buildReply(echoMessage);
    }

    // pid, message

    @Override
    public void echo(int pid, String message) {

        JSONObject messageE = new JSONObject(message);
        BroadcastValue bv = new BroadcastValue(new JSONObject(message), pid);

        if(echos[pid] == null) {
            responses++;
            echos[pid] = bv;
            System.out.println(String.format("%d is OUT BEWARE!!!!!!!!!!!!!!!!!!!!!!!!!!", notary.notaryIndex));
            for (int i = 0; i < Main.N; i++) {
                /*System.out.println("#####################ECHOLOL#################");
                System.out.println(echos[i]);
                System.out.println(bv);
                System.out.println("#############################################");*/
                if(echos[i] != null && echos[i].equals(bv)) {
                    acks++;
                    System.out.println("ack echo from: "+ bv+ " total acks: "+ acks);
                    System.out.println(acks>(Main.N+Main.f)/2 );
                    System.out.println((Main.N + Main.f)/2);
                    if(!sentReady && acks > (Main.N + Main.f)/2) {
                        System.out.println("#####################QORUM#################");
                        System.out.println(bv.message.toString());
                        System.out.println("#############################################");
                        delivered = true;
                        sem.release();
                        doubleEcho(messageE);
                        sentReady = true;
                        responses = 0;
                        //delivered = true;
                        //acks = 0;
                 ;

                        System.out.println("Echo phase is done...");
                    }
                }
            }
        }

        if(responses > (notary.nServers+Main.f)/2 && acks<2*Main.f) {
            delivered = false;
            sem.release();
        }
    }

    public void ready(JSONObject ready){}

    public void doubleEcho(JSONObject ready) {}

    public boolean isDelivered(){
        return delivered;
    }

    @Override
    public Semaphore getLock() {
        return sem;
    }
}
