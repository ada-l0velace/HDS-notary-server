package pt.tecnico.hds.server;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class AuthenticatedBroadcast implements Broadcast {

    private int _pid;
    private HashMap<String, BroadcastValue> _echoes;
    private int _nServers;
    private String _host;
    private int _port;
    private int _q;
    private boolean sentEcho;
    private boolean delivered;
    private JSONObject _reply;
    private Notary _notary;
    private final static Logger logger = LoggerFactory.getLogger(AuthenticatedBroadcast.class);


    public AuthenticatedBroadcast(int f, int n, String host, int port, int pid, Notary notary) {
        _echoes = new HashMap<>();
        _q = 3;
        _port = port;
        _host = host;
        _pid = pid;
        _nServers = n;
        _notary = notary;
        sentEcho = false;
        delivered = false;

    }


    public JSONObject getReply(){
        return _reply;
    }

    public void getEcho(JSONObject j) {
        System.out.println("Got Something");
        JSONObject val = new JSONObject(j.getString("Value"));
        insertEcho(val, j.getInt("pid"));
        System.out.println(_echoes);
        int ansN = _echoes.get(val.toString()).getAnsNum();
        System.out.println("Got " + ansN + " Answers: " + _echoes.get(val.toString()).nEchoes() + " correct");
    }

    public void insertEcho(JSONObject j, int pid){
        if (!_echoes.containsKey(j.toString())){
            _echoes.put(j.toString(), new BroadcastValue(j.toString(), _nServers));
            _echoes.get(j.toString()).insertEcho(_pid);
        }
        _echoes.get(j.toString()).insertEcho(pid);

    }


    public void setManager(JSONObject j, JSONObject reply){
        insertEcho(j, _pid);
        sentEcho = false;
        delivered = false;
        _reply = reply;
    }

    public Boolean ackd(String s){ return _echoes.get(s).getAnsNum() == _nServers; }

    public Boolean quorum(String s){
        //return _acks >= _q;
        return _echoes.get(s).nEchoes() >= _q;
    }

    public JSONObject echo(String msg, String sig){
        JSONObject echo = new JSONObject();
        JSONObject message = new JSONObject();
        message.put("Action", "Echo");
        message.put("Value", msg);
        message.put("pid", _pid);
        echo.put("Message", message.toString());
        echo.put("Hash", sig);
        return echo;
    }

    public JSONObject waitForEcho(JSONObject request){
        JSONObject message = new JSONObject(request.getString("Message"));
        JSONObject reply = new JSONObject();

        while (!delivered){
            System.out.println(quorum(message.toString()));
            if (quorum(message.toString())) {
                delivered = true;
                System.out.println("Success");
                _notary.reg.write(message.getString("Good"), message.toString(), request.getString("Hash"), message.getLong("rid"), _notary.notaryIndex, message.getLong("Timestamp"));
                reply = getReply();
                return reply;
            }
            else if (ackd(message.toString())){
                delivered = true;
                System.out.println("Failed");
                reply.put("Action", "NO");
                return reply;
            }
        }
        return null;
    }


    public void broadcast(String msg) {
        if (!sentEcho) {
            sentEcho = true;
            int notaryPort = _port + _pid;
            for (int s = _port; s < _port + _nServers; s++) {
                if (s != notaryPort) {
                    _notary.connectToServer(_host, s, msg);
                    System.out.println(msg + "Sent to " + s);
                }
            }
        }
    }

}
