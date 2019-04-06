import org.json.JSONObject;
import org.junit.*;

import pt.tecnico.hds.server.Notary;

public class ServerServiceTest {
	@Test
    public void testServerSignature() throws Throwable {
		JSONObject reply = new JSONObject();
		Notary n = new Notary();
		reply.put("Action", "NO");
		reply = n.buildReply(reply);
		Assert.assertTrue(n.cc.verifySignWithPubKey(reply.getString("Message"), reply.getString("Hash")));
    }

}
