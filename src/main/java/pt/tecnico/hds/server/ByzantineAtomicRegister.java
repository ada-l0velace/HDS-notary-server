package pt.tecnico.hds.server;

import org.json.JSONObject;

public class ByzantineAtomicRegister extends ByzantineRegister {

    private int acks;

    public ByzantineAtomicRegister(Notary _notary) {
        super(_notary);
    }

    @Override
    void write(String good, String msg, String sig, long pid, long ts) {
        JSONObject e = this.echo(msg, sig);
        String result;
        acks = 0;

        //if (ts > _goods.get(good).getTimestamp()) {
        for (int i = 0; i < notary.nServers; i++){
                result = notary.connectToServers("localhost", notary._port, e);
                if (result != null){
                    acks++;
                }
            }
            if (acks == notary.nServers) {
                _goods.put(good, new RegisterValue(sig, msg, pid, ts));
            }
        //}
    }

    @Override
    String read(String good, String msg, JSONObject request) {
        JSONObject j = new JSONObject(msg);
        if (_goods.containsKey(good)) {
            RegisterValue val = _goods.get(good);
            //j.put("rid", val.getRid());
            j.put("Value", val.getValue());
            j.put("SignatureValue", val.getSignature());
        }
        return j.toString();
    }

    @Override
    void ack(JSONObject ack, long ts) {
        ack.put("wts", ts);
    }

    JSONObject echo(String msg, String sig){
        JSONObject echo = new JSONObject();
        JSONObject message = new JSONObject();
        message.put("Action", "Echo");
        message.put("Value", msg);
        echo.put("Message", message.toString());
        echo.put("Hash", sig);
        return echo;
    }

}

