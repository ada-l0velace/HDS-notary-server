package pt.tecnico.hds.server;

import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;

public abstract class ByzantineRegister {
    HashMap<String, RegisterValue> _goods;
    Notary notary;

    public ByzantineRegister(Notary _notary) {
        notary = _notary;
        _goods = new HashMap<>();
        populateRegister();
    }

    public void printGoods(){
        for (String g : _goods.keySet()) {
            System.out.println("Good: " + g);
            System.out.println("Value: " + _goods.get(g).getValue());
            System.out.println("Signature: " + _goods.get(g).getSignature());
        }
    }

    public void populateRegister() {
        String sql = "SELECT userId, onSale, goodsId FROM notary";
        String good, user, sig;
        Boolean onSale;
        JSONObject value = new JSONObject();
        try {
            Connection conn = notary.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                good = rs.getString("goodsId");
                user = rs.getString("userId");
                onSale = rs.getBoolean("onSale");
                value.put("Good", good);
                value.put("Owner", user);
                value.put("OnSale", onSale);
                value.put("wts", 0);
                value.put("pid", notary.notaryIndex);
                value.put("signer", "server");

                sig = notary.cc.signWithPrivateKey(value.toString());

                //write(good, value.toString(),sig, 0, 0);
                _goods.put(good, new RegisterValue(sig, value.toString(), notary.notaryIndex, 0));
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Throwable t) {
            t.printStackTrace();
        }

    }

    public RegisterValue getGood(String good){
        return _goods.get(good);
    }

    abstract void write(String good, String msg, String sig, long pid, long ts);
    abstract String read (String good, String msg, JSONObject request);
    abstract void ack(JSONObject ack, long ts);
}
