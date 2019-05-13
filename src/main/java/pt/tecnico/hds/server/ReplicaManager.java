package pt.tecnico.hds.server;

import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplicaManager {

    private int _ansN;
    private int _pid;
    private int _acks;
    private int _nServers;
    private String _host;
    private int _port;
    private int _q;
    private JSONObject _msg;
    private final static Logger logger = LoggerFactory.getLogger(ReplicaManager.class);


    public ReplicaManager(int f, int n, String host, int port, int pid){
        _q = f + 1;
        _port = port;
        _host = host;
        _pid = pid;
        _nServers = n;
        _acks = 1;
        _ansN = 1;
    }

    public int getAcks(){ return _ansN; }

    public void getEcho(JSONObject j){
        JSONObject val = new JSONObject(j.getString("Message"));
        if (this.compareEchoes(val)){
            _acks++;
        }
        _ansN++;

    }

    public void setManager(JSONObject j){
        _acks = 1;
        _msg = j;
    }

    public Boolean ackd(){ return _ansN == _nServers; }

    public Boolean quorum(){ return _acks >= _q; }

    public Boolean compareEchoes(JSONObject j){
        for (String k : j.keySet()) {
            if (!k.equals("Timestamp") &&
                    !k.equals("rid") &&
                    !k.equals("wts") &&
                    _msg.has(k)) {
                if (j.getString(k).equals(_msg.getString(k)))
                    return false;
            }
        }
        if (j.getLong("rid") != _msg.getLong("rid") ||
            j.getLong("wts") != _msg.getLong("wts"))
            return false;

        return true;
    }

    JSONObject echo(String msg, String sig){
        JSONObject echo = new JSONObject();
        JSONObject message = new JSONObject();
        message.put("Action", "Echo");
        message.put("Value", msg);
        echo.put("Message", message.toString());
        echo.put("Hash", sig);
        return echo;
    }

    public String connectToServer(int port ){
        String answer = null;
        int maxRetries = 10;
        int retries = 0;


        while (true) {
            try {
                System.out.println("Connecting to " + port);
                InetAddress ip = InetAddress.getByName(_host);

                Socket s = new Socket(ip, port);
                s.setSoTimeout(10 * 1000);

                DataInputStream dis = new DataInputStream(s.getInputStream());
                DataOutputStream dos = new DataOutputStream(s.getOutputStream());


                try {

                    dos.writeUTF(_msg.toString());
                    dis.close();
                    dos.close();
                    s.close();


                } catch (java.net.SocketTimeoutException timeout) {
                    timeout.printStackTrace();
                    s.close();
                    break;

                } catch (java.io.EOFException e0) {
                    e0.printStackTrace();
                    s.close();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                    s.close();
                    break;
                }


            } catch (IOException e) {
                logger.error(e.getMessage() + " on port:" + port);
                //e.printStackTrace();
                retries++;
                if (retries == maxRetries)
                    break;
                continue;
            }
            break;
        }
        return answer;
    }

    public void broadcast(){
        int notaryPort = _port + _pid;
        for (int s = _port; s < _port + _nServers; s++){
            if (s != notaryPort){
                connectToServer(s);
                System.out.println(_msg + "Sent to " + s);
            }
        }
    }

}
