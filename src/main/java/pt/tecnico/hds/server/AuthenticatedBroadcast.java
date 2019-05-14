package pt.tecnico.hds.server;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class AuthenticatedBroadcast implements Broadcast {

    private int _ansN;
    private int _pid;
    private HashMap<String, Boolean[]> _echoes;
    private int _acks;
    private int _nServers;
    private String _host;
    private int _port;
    private int _q;
    private boolean sentEcho;
    private boolean delivered;
    private JSONObject _reply;
    private JSONObject _msg;
    private Notary _notary;
    private final static Logger logger = LoggerFactory.getLogger(AuthenticatedBroadcast.class);


    public AuthenticatedBroadcast(int f, int n, String host, int port, int pid, Notary notary) {
        _echoes = new HashMap<>();
        _q = (f + _nServers) / 2;
        _port = port;
        _host = host;
        _pid = pid;
        _nServers = n;
        _acks = 1;
        _ansN = 1;
        _notary = notary;
        sentEcho = false;
        delivered = false;

    }


    public int getAcks(){ return _ansN; }

    public int nEchoes(JSONObject j){
        int count = 0;
        Boolean[] list = _echoes.get(j.toString());
        System.out.println(list);
        for (Boolean b : list){
            if (b){
                ++count;
            }
        }
        return count;
    }


    public JSONObject getReply(){
        return _reply;
    }

    public void getEcho(JSONObject j) {
        JSONObject val = new JSONObject(j.getString("Message"));
        System.out.println("Got Something");
        insertEcho(j);
        System.out.println(_echoes);
        /*
        if (_msg == null) {
            _msg = j;
            _acks++;
        }
        else if (this.compareEchoes(new JSONObject(val.getString("Value")))){
            _acks++;
        }
        */
        _ansN++;
        System.out.println("Got " + _ansN + " Answers: " + nEchoes(j) + " correct");
    }

    public void insertEcho(JSONObject j){
        if (_echoes.containsKey(j)){
            _echoes.get(j)[_pid] = true;
        }
        else {
            Boolean[] echoes = new Boolean[_nServers];
            for (int i = 0; i < _nServers; i++){
                if (i != _pid){ echoes[i] = false; }
                else { echoes[_pid] = true; }
            }
            _echoes.put(j.toString(), echoes);
        }
    }


    public void setManager(JSONObject j, JSONObject reply){
        insertEcho(j);
        _acks = 1;
        _ansN = 1;
        sentEcho = false;
        delivered = false;
        _msg = j;
        _reply = reply;
    }

    public Boolean ackd(){ return _ansN == _nServers; }

    public Boolean quorum(JSONObject j){
        //return _acks >= _q;
        return nEchoes(j) >= _q;
    }

    public Boolean compareEchoes(JSONObject j){

        JSONObject val = new JSONObject(_msg.getString("Message"));
        JSONObject message = new JSONObject(val.getString("Value"));
        for (String k : j.keySet()) {
            if (!k.equals("Timestamp") &&
                    !k.equals("rid") &&
                    !k.equals("wts") &&
                    message.has(k)) {
                if (!j.getString(k).equals(message.getString(k)))
                    return false;
            }
        }
        if (j.getLong("rid") != message.getLong("rid") ||
            j.getLong("wts") != message.getLong("wts"))
            return false;

        return true;
    }

    public JSONObject echo(String msg, String sig){
        JSONObject echo = new JSONObject();
        JSONObject message = new JSONObject();
        message.put("Action", "Echo");
        message.put("Value", msg);
        echo.put("Message", message.toString());
        echo.put("Hash", sig);
        System.out.println("Echo Message:");
        System.out.println(echo);
        return echo;
    }

    public JSONObject waitForEcho(JSONObject request){
        JSONObject message = new JSONObject(request.getString("Message"));
        JSONObject reply = new JSONObject();

        while (!delivered){
            System.out.println("Got " + getAcks() + " Echoes");
            System.out.println(request);
            if (quorum(echo(message.toString(), request.getString("Hash")))) {
                delivered = true;
                System.out.println("Success");
                _notary.reg.write(message.getString("Good"), message.toString(), request.getString("Hash"), message.getLong("rid"), _notary.notaryIndex, message.getLong("Timestamp"));
                reply = getReply();
                return reply;
            }
            else if (ackd()){
                delivered = true;
                System.out.println("Failed");
                reply.put("Action", "NO");
                return reply;
            }
        }
        return null;
    }

    public void broadcast() {
        if (!sentEcho) {
            sentEcho = true;
            int notaryPort = _port + _pid;
            for (int s = _port; s < _port + _nServers; s++) {
                if (s != notaryPort) {
                    _notary.connectToServer(_host,_port,_msg);
                    System.out.println(_msg + "Sent to " + s);
                }
            }
        }
    }

}
