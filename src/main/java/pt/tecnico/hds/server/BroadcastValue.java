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
    public JSONObject message;

    public BroadcastValue(JSONObject request, int pid) {
        this.pid = pid;
        message = new JSONObject(request.getString("Message"));
        if (message.has("rid"))
            rid = message.getLong("rid");
        if (message.has("wts"))
            wts = message.getLong("wts");
        if (message.has("Seller"))
            seller = message.getString("Seller");
        if (message.has("Buyer"))
            buyer = message.getString("Buyer");
        if (message.has("Good"))
            good = message.getString("Good");
        if (message.has("signer"))
            signer = message.getString("signer");
        action = message.getString("Action");

    }

    @Override
    public String toString() {
        return action+ " " + pid + " " + message.toString();
    }


    public boolean equals(BroadcastValue that){
        return pid == that.pid && action == that.action && seller == that.seller && buyer == that.buyer && good == that.good && signer == that.signer && message == that.message;
    }
}
