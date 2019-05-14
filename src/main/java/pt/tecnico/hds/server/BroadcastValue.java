package pt.tecnico.hds.server;

import org.json.JSONObject;

public class BroadcastValue {

    private Boolean[] _echoList;
    private String _request;
    private int _nAns;

    public BroadcastValue(String request, int nServers){
        _request = request;
        _echoList = new Boolean[nServers];
        for (int i = 0; i < nServers; i++){
            _echoList[i] = false;
        }
        _nAns = 1;
    }

    public Boolean[] getEchoList(){
        return _echoList;
    }

    public int nEchoes(){
        int count = 0;
        for (Boolean b : _echoList){
            if (b){
                count = count + 1;
            }
        }
        return count;
    }

    public void insertEcho(int pid){
        if (!_echoList[pid]) {
            _echoList[pid] = true;
            _nAns++;
        }
    }

    public int getAnsNum() {
        return _nAns;
    }

    public void setAnsNum(int ansN) {
        _nAns = ansN;
    }
}
