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
        message = new JSONObject(request.getString("Message"));
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
        action = message.getString("Action");

        if (request.has("Message2")) {

            message2 = new JSONObject(request.getString("Message2"));
            action2 = message.getString("Action2");
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
        return action+ " " + pid + " "; //+ message.toString();
    }


    public boolean equals(BroadcastValue that){
        boolean msg = pid == that.pid && action == that.action && seller == that.seller && buyer == that.buyer && good == that.good && signer == that.signer && message == that.message;
        boolean msg2 = action2 == that.action2 && seller2 == that.seller2 && buyer2 == that.buyer2 && good == that.good && signer == that.signer && message == that.message;

        return msg && msg2;
    }
}
