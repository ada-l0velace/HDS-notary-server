package pt.tecnico.hds.server;

import org.json.JSONObject;

import java.util.Date;

public class RegisterValue {
    JSONObject _value;
    long _timestamp;

    public RegisterValue(JSONObject value){
        _value = value;
        _timestamp = new Date().getTime();
    }

    public JSONObject getValue(){
        return _value;
    }

    public long getTimestamp(){
        return _timestamp;
    }

}
