package pt.tecnico.hds.server;

import org.json.JSONObject;

public class ByzantineAtomicRegister extends ByzantineRegister {

    private int acks;

    public ByzantineAtomicRegister(Notary _notary) {
        super(_notary);
    }

    @Override
    void write(String good, String msg, String sig, long rid, int pid, long ts) {
        acks = 0;
        _goods.put(good, new RegisterValue(sig, msg, rid, pid, ts));
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



}

