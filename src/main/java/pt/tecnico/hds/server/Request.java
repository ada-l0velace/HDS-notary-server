package pt.tecnico.hds.server;

import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class Request {
    DataInputStream dis;
    DataOutputStream dos;
    Notary notary;
    JSONObject reply;

    public Request(Notary n, DataInputStream dis, DataOutputStream dos) {
        notary = n;
        this.dos = dos;
        this.dis = dis;
    }

    protected Boolean computationalCostChallenge() throws IOException, SQLException {
        RandomString rsGenerator = new RandomString(26);
        String rs = rsGenerator.nextString();
        String hash = Utils.getSHA512(rs);
        //String toSend = String.format("Notary needs proof of work. SHA512 (X + \"%s\").hexdigest() = \"%s...\",\nX is an alphanumeric string and |X| = 4\nEnter X: ",
        //        rs.substring(4), hash.substring(0,32));
        reply = new JSONObject();
        reply.put("Action", "Challenge");
        reply.put("RandomString", rs.substring(4));
        reply.put("SHA512", hash.substring(0,32));
        String m = getReply().toString();
        System.out.println(m);
        dos.writeUTF(m);
        String clientAnswer = dis.readUTF();
        JSONObject j = new JSONObject(clientAnswer);
        if (!verifySignature(j))
            return false;
        String X = new JSONObject(j.getString("Message")).getString("Answer");

        return Utils.getSHA256(X + rs.substring(4)).equals(Utils.getSHA256(rs));
    }

    public Boolean verifySignature(JSONObject request) throws SQLException {
        JSONObject message = new JSONObject(request.getString("Message"));
        Connection conn = notary.connect();
        String signature = request.getString("Hash");
        if (notary.isReal("userid", "users", message.getString("User"), conn) &&
                Utils.verifySignWithPubKeyFile(message.toString(), signature , "assymetricKeys/"+message.getString("User") + ".pub") &&
                notary.verifyReplay(signature, conn)) {
            notary.addToRequests(signature, message.toString(), conn);
            conn.close();
            return true;
        }

        conn.close();
        return false;
    }


    protected JSONObject getReply() {
        return notary.buildReply(reply);
    }
}
