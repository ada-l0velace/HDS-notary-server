package pt.tecnico.hds.server;

import org.json.JSONObject;

public class ByzantineRegularRegister extends ByzantineRegister {

    public ByzantineRegularRegister(Notary notary) {
        super(notary);
    }

    @Override
    void write(String good, String msg, String sig, long pid, long ts) {
        System.out.println(_goods);
        if (ts > _goods.get(good).getTimestamp()) {
            _goods.put(good, new RegisterValue(sig, msg, pid, ts));
        }
    }

    @Override
    String read (String good, String msg) {
        JSONObject j = new JSONObject(msg);
        if (_goods.containsKey(good)) {
            RegisterValue val = _goods.get(good);
            j.put("Value", val.getValue());
            j.put("SignatureValue", val.getSignature());
        }
        return j.toString();
    }
}
