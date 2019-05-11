package pt.tecnico.hds.server;

import org.json.JSONObject;

public class ByzantineAtomicRegister extends ByzantineRegister {

    public ByzantineAtomicRegister(Notary _notary) {
        super(_notary);
    }

    @Override
    void write(String good, String msg, String sig, long pid, long ts) {

    }

    @Override
    String read(String good, String msg) {
        return null;
    }

    @Override
    void ack(JSONObject ack, long ts) {

    }
}
