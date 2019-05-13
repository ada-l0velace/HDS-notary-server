package pt.tecnico.hds.server;

import org.json.JSONObject;

public class ByzantineRegularRegister extends ByzantineRegister {

    public ByzantineRegularRegister(Notary notary) {
        super(notary);
    }

    @Override
    void write(String good, String msg, String sig, long rid, int pid, long ts) {
        if (ts > _goods.get(good).getTimestamp()) {

            JSONObject jMsg = new JSONObject(msg);
            _goods.put(good, new RegisterValue(sig, msg, rid, pid, ts));
        }
    }

    @Override
    String read (String good, String msg, JSONObject request) {
        JSONObject j = new JSONObject(msg);
        if (_goods.containsKey(good)) {
            RegisterValue val = _goods.get(good);
            val._rid=request.getLong("rid");
            //val._timestamp = request.getLong("wts");
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
