package pt.tecnico.hds.server;

public class RegisterValue {
    String _signature;
    String _value;
    long _rid;
    long _timestamp;
    int _pid;


    public RegisterValue(String sig, String val, long rid, int pid, long ts ){
        this._signature = sig;
        this._value = val;
        this._rid = rid;
        this._timestamp = ts;
        this._pid = pid;
    }

    public long getTimestamp() {
        return _timestamp;
    }

    public String getValue() {
        return _value;
    }

    public String getSignature() {
        return _signature;
    }

    public long getRid(){
        return _rid;
    }
}
