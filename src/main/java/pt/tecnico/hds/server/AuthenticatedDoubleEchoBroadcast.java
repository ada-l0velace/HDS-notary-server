package pt.tecnico.hds.server;

import org.json.JSONObject;

import java.util.concurrent.Semaphore;

public class AuthenticatedDoubleEchoBroadcast extends AuthenticatedBroadcast {
    BroadcastValue[] reads;

    public AuthenticatedDoubleEchoBroadcast(Notary notary) {
        super(notary);
        reads = new BroadcastValue[notary.nServers];
    }

    @Override
    public void init() {
       super.init();
       reads = new BroadcastValue[notary.nServers];
        //sem = new Semaphore(2);
    }

    @Override
    public JSONObject buildMessage(JSONObject request) {
        JSONObject echoMessage = new JSONObject();
        echoMessage.put("Action","Ready");
        echoMessage.put("pid",notary.notaryIndex);
        echoMessage.put("Value",request);
        return notary.buildReply(echoMessage);
    }

    @Override
    public void doubleEcho(JSONObject request) {
        if (!sentReady) {
            sentReady = true;
            for (int i = 0; i < Main.N; i++) {
                notary.connectToServer("localhost", notary._port + i, buildMessage(request));
            }
        }
    }

    @Override
    public void ready(JSONObject echo) {
        responses++;
        JSONObject messageE = new JSONObject(echo.getString("Message"));
        int pid = messageE.getInt("pid");
        BroadcastValue bv = new BroadcastValue(echo, pid);

        if(echos[pid] == null) {
            reads[pid] = bv;
            for (int i = 0; i < notary.nServers; i++) {
                System.out.println("###################DOUBLE###################");
                System.out.println(echos[i]);
                System.out.println(bv);
                System.out.println("#############################################");
                if(reads[i]!= null && reads[i].equals(bv)) {
                    acks++;
                    System.out.println("ack ready from: "+ bv+ " total acks: "+ acks);
                    System.out.println(acks>Main.f);
                    System.out.println(acks>2f);
                    if(acks>Main.f && !sentReady) {
                        doubleEcho(bv.message);
                    }
                    if(acks>2f && !this.delivered) {
                        System.out.println("###################WIN#######################");
                        System.out.println(echos[i]);
                        System.out.println("#############################################");
                        delivered=true;
                        sem.release();

                    }
                }
            }
        }
        if(responses > (Main.N+Main.f)/2 && acks<2f) {
            sem.release();
        }
    }
}
