package pt.tecnico.hds.server;


import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;

public class HdsRegister {

    HashMap<String, RegisterValue> _goods;


    public HdsRegister(){
        _goods = new HashMap<>();
    }

    public void deliveryWrite(String good, String msg, String sig, long pid, long ts) {
        _goods.put(good, new RegisterValue(sig, msg, pid, ts));
        //send ACK to client
    }


    public RegisterValue findGood(String good){
        if (_goods.containsKey(good)){
            return _goods.get(good);
        }
        return null;
    }

    public void printGoods(){
        for (String g : _goods.keySet()) {
            System.out.println("Good: " + g);
            System.out.println("Value: " + _goods.get(g).getValue());
            System.out.println("Signature: " + _goods.get(g).getSignature());
        }
    }

    public boolean goodExists(String good){
        return _goods.containsKey(good);
    }

    public boolean checkTimestamp(String good, long ts){
        return  _goods.get(good).getTimestamp() <= ts;
    }


/*    public JSONObject getGoodInfo(String good, long ts){
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
        j.put("Timestamp", ts);
        return j;
    }
    */
}
