package pt.tecnico.hds.server;

import org.json.JSONObject;

import java.util.concurrent.Semaphore;

public class AuthenticatedDoubleEchoBroadcast extends AuthenticatedBroadcast {
    BroadcastValue[] reads;

    public AuthenticatedDoubleEchoBroadcast(Notary notary) {
        super(notary);
        reads = new BroadcastValue[notary.nServers];
        echos = new BroadcastValue[notary.nServers];
    }

    @Override
    public void init() {
       super.init();
       reads = new BroadcastValue[notary.nServers];
        //sem = new Semaphore(2);
    }

    public JSONObject buildMessage2(JSONObject request) {

        JSONObject echoMessage = new JSONObject();
        echoMessage.put("Action","Ready");
        echoMessage.put("pid",notary.notaryIndex);
        if (request.has("Message"))
            echoMessage.put("Value",request.toString());
        return notary.buildReply(echoMessage);
    }

    @Override
    public void doubleEcho(JSONObject request) {
        System.out.println("STARTING DOUBLE ECHO !!!!!!!!!!!!!!!!!!!!!!!");
        if (!sentReady) {
            sentReady = true;
            for (int i = 0; i < Main.N; i++) {
                System.out.println(buildMessage2(request).toString());
                //new BroadcastThread(notary,"localhost", notary._port+i, buildMessage(request));
                notary.connectToServer("localhost", notary._port + i, buildMessage2(request).toString());
            }
        }
    }

    @Override
    public void ready(JSONObject echo) {
        System.out.println("Receiving ready "+echo);
        responses++;
        JSONObject messageE = new JSONObject(echo.getString("Message"));
        int pid = messageE.getInt("pid");
        BroadcastValue bv = new BroadcastValue(echo, pid);

        if(reads[pid] == null) {
            reads[pid] = bv;
            for (int i = 0; i < notary.nServers; i++) {
                System.out.println("###################DOUBLE###################");
                System.out.println(reads[i]);
                System.out.println(bv);
                System.out.println("#############################################");
                if(reads[i]!= null && reads[i].equals(bv)) {
                    acks2++;
                    System.out.println("ack ready from: "+ bv+ " total acks: "+ acks);
                    System.out.println(acks>Main.f);
                    System.out.println(acks>2*Main.f);
                    if(acks2>Main.f && !sentReady) {
                        doubleEcho(bv.message);
                    }
                    if(acks2>2*Main.f && !this.delivered) {
                        delivered=true;
                        sem.release();
                        System.out.println("###################WIN#######################");
                        System.out.println(reads[i]);
                        System.out.println("#############################################");


                    }
                }
            }
            if(responses > (Main.N+Main.f)/2 && acks<2f) {
                //sem.release();
            }
        }

    }
}
