package pt.tecnico.hds.server;


import org.json.JSONObject;
import java.util.HashMap;

public class HdsRegister {

    RegisterValue _v;
    long _rid;
    HashMap<String, JSONObject> _goods;
    long _ts;
    

    public HdsRegister(){
        _goods = new HashMap<String, JSONObject>();
        _rid = 0;
        _ts = 0;
    }

    public void deliveryWrite(RegisterValue v) {
        if (_v.getTimestamp() >  v.getTimestamp());
            _v = v;

        //send ACK to client
    }
    
    public long getTimestamp() {
    	return _ts;
    }


}
