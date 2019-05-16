package pt.tecnico.hds.server;

import org.json.JSONObject;


public class AuthenticatedDoubleEchoBroadcast extends AuthenticatedBroadcast {
    BroadcastValue[] reads;
    int[] responses2;
    int[] acks2;

    public AuthenticatedDoubleEchoBroadcast(Notary notary) {
        super(notary);
        init();
    }

    @Override
    public void init() {
        super.init();
        reads = new BroadcastValue[Notary.nServers];
        acks2 = new int[Notary.nServers];
        responses2 = new int[Notary.nServers];

    }

    @Override
    public void clear() {
        super.init();
        reads = new BroadcastValue[Notary.nServers];
        acks2 = new int[Notary.nServers];
        responses2 = new int[Notary.nServers];

    }

    public String buildMessage2(String request) {
        JSONObject echoMessage = new JSONObject();
        echoMessage.put("Action", "Ready");
        echoMessage.put("pid", notary.notaryIndex);
        echoMessage.put("Value", request);
        return notary.buildReply(echoMessage).toString();
    }

    @Override
    public void doubleEcho(String request) {
        for (int i = 0; i < Main.N; i++) {
            notary.connectToServer("localhost", Notary._port + i, buildMessage2(request));
        }
    }

    @Override
    public synchronized void ready(int pid, String message) {
        logger.info(String.format("Starting Ready from %d to %d: ", pid, notary.notaryIndex));

        BroadcastValue bv = new BroadcastValue(message, pid);

        if (reads[pid] == null) {
            responses2[ni]++;
            reads[pid] = bv;
            for (int i = 0; i < Notary.nServers; i++) {
                if (reads[i] != null && reads[i].equals(bv)) {
                    acks2[ni]++;
                    if (acks2[ni] > Main.f && !sentReady[ni]) {
                        sentReady[ni] = true;
                        doubleEcho(message);
                    }

                    if (acks2[ni] > 2 * Main.f && !this.delivered[ni]) {
                        delivered[ni] = true;
                        logger.info(String.format("|Ready :) %d Achieved QORUM|", notary.notaryIndex));
                        //logger.info(message);
                        releaseLock();
                    }
                }
            }
            if (responses2[ni] > (Main.N + Main.f) / 2 && acks2[ni] < 2 * Main.f) {
                logger.info(String.format("|Replay QORUM not achieved :( %d |", notary.notaryIndex));
                releaseLock();
            }

        }
    }
}

