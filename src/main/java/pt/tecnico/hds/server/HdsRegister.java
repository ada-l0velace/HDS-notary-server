package pt.tecnico.hds.server;


import java.util.ArrayList;
import java.util.List;

public class HdsRegister {

    RegisterValue _v;
    long _rid;
    List<Long> _acks;
    List<RegisterValue> _readList;
    long _ts;
    

    public HdsRegister(){
        _readList = new ArrayList<RegisterValue>();
        _acks = new ArrayList<Long>();
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
