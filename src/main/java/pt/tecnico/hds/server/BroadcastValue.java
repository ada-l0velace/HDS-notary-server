package pt.tecnico.hds.server;
import org.json.JSONObject;

public class BroadcastValue {
    public String action;
    public int pid;
    public long rid = 0;
    public long wts = 0;
    public String seller;
    public String buyer;
    public String good;
    public String signer;

    public String action2;
    public int pid2;
    public long rid2 = 0;
    public long wts2 = 0;
    public String seller2;
    public String buyer2;
    public String good2;
    public String signer2;
    public JSONObject message=null;
    public JSONObject message2=null;

    public BroadcastValue(JSONObject request, int pid) {
        this.pid = pid;
        System.out.println(request.getString("Message"));
        JSONObject m0 = new JSONObject(request.getString("Message"));
        message = m0;
        if (message.has("rid"))
            rid = message.getLong("rid");
        else
            rid = 0;
        if (message.has("wts"))
            wts = message.getLong("wts");
        else
            wts = 0;
        if (message.has("Seller"))
            seller = message.getString("Seller");
        else
            seller ="";

        if (message.has("Buyer"))
            buyer = message.getString("Buyer");
        else
            buyer = "";
        if (message.has("Good"))
            good = message.getString("Good");
        else
            good = "";
        if (message.has("signer"))
            signer = message.getString("signer");
        else
            signer = "";
        System.out.println(message.toString());
        action = message.getString("Action");

        if (request.has("Message2")) {

            message2 = new JSONObject(request.getString("Message2"));
            action2 = message.getString("Action");
            rid2 = message2.getLong("rid");
            wts2 = message2.getLong("wts");
            seller2 = message2.getString("Seller");
            buyer2 = message2.getString("Buyer");
            good2 =message2.getString("Good");
            signer2 = message2.getString("signer");
        }
    }

    @Override
    public String toString() {
        return action+ "," + pid + ","+ seller+ ","+buyer + ","+good+","+signer+ " "+message; //+ message.toString();
    }


    public boolean equals(BroadcastValue that){
        //boolean msg =  action.equals(that.action) && seller.equals(that.seller) && buyer.equals(that.buyer) && good.equals(that.good) && signer.equals(that.signer);
        //boolean msg2 = action2.equals(that.action2) && seller2.equals(that.seller2) && buyer2.equals(that.buyer2) && good2.equals(that.good2) && signer2.equals(that.signer2);

        return (rid == that.rid) && wts == that.wts ;
    }
}
