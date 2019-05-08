package pt.tecnico.hds.server;


import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;

public class HdsRegister {

    RegisterValue _v;
    long _rid;
    HashMap<String, JSONObject> _goods;
    long _ts;
    

    public HdsRegister(){
        _goods = new HashMap<>();
        _rid = 0;
        _ts = 0;
    }

    public void deliveryWrite(String good, long ts) {
        JSONObject val = getGoodInfo(good);
        _goods.put(good, val);
        _ts = ts;


        //send ACK to client
    }
    
    public long getTimestamp() {
    	return _ts;
    }

    public JSONObject getGoodInfo(String good){
        JSONObject j = new JSONObject();
        try {
            Connection conn = Notary.connect();
            String sql = "SELECT userId, onSale FROM notary WHERE goodsId = ?";
            Boolean result = false;
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, good);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                result = rs.getBoolean("OnSale");
                j.put("OnSale", result.toString());
                j.put("Owner", rs.getString("userId"));
            } else{
                j.put("Action", "NO");
            }
            conn.close();
        } catch (Exception e){
            e.printStackTrace();
            j.put("Action", "NO");
        }
        return j;
    }
}
