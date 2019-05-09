package pt.tecnico.hds.server;

public class RegisterValue {
    String _signature;
    String _value;
    long _rid;
    long _timestamp;


    public RegisterValue(String sig, String val, long rid, long ts ){
        this._signature = sig;
        this._value = val;
        this._rid = rid;
        this._timestamp = ts;
    }

    public long getTimestamp() {
        return _timestamp;
    }


}
