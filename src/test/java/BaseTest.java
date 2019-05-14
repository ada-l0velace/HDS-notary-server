import org.dbunit.IDatabaseTester;
import org.dbunit.JdbcDatabaseTester;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import pt.tecnico.hds.client.HdsClient;
import pt.tecnico.hds.client.Main;
import pt.tecnico.hds.client.Utils;
import pt.tecnico.hds.client.exception.HdsClientException;

import java.io.*;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class BaseTest {

    private ArrayList<IDatabaseTester> databaseTester = new ArrayList<IDatabaseTester>();
    protected int serverPort = 19999;
    protected final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    protected final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    protected final PrintStream originalOut = System.out;
    protected final PrintStream originalErr = System.err;


    public BaseTest () {
        super();
    }

    protected IDataSet getDataSet() throws Exception {
        return new FlatXmlDataSetBuilder().build(new FileInputStream("dbunitData.xml"));
    }

    @Before
    public void setUp() throws Exception {
        for (int i = 0; i < Main.replicas; i++) {
            databaseTester.add(new JdbcDatabaseTester("org.sqlite.JDBC",
                    String.format("jdbc:sqlite:db/hds%d.db", i), "", ""));
            IDataSet dataSet = getDataSet();
            databaseTester.get(i).setDataSet(dataSet);
            databaseTester.get(i).setSetUpOperation(getSetUpOperation());
            databaseTester.get(i).onSetup();
        }
    }

    @After
    public void tearDown() throws Exception {
        /*for (int i = 0; i < Main.replicas; i++) {
            databaseTester.get(i).setTearDownOperation(getTearDownOperation());
            databaseTester.get(i).onTearDown();
        }*/
    }

    protected DatabaseOperation getSetUpOperation() throws Exception {
        return DatabaseOperation.CLEAN_INSERT;
    }

    protected DatabaseOperation getTearDownOperation() throws Exception {
        return DatabaseOperation.CLEAN_INSERT;
    }

    protected Boolean serverIsUp() {
        try {
            Socket s = new Socket("localhost", serverPort);
            s.close();
            return true;
        }
        catch (ConnectException e) {
            return false;
        }
        catch (Exception e) {
            return false;
        }
    }

    public String getServerKey(JSONObject request){
        has_parameters(request, Arrays.asList("Message", "Hash"));
        JSONObject message = new JSONObject(request.getString("Message"));
        return "assymetricKeys/"+message.getString("signer")+".pub";
    }

    public void has_parameters(JSONObject request, List<String> parameters) {
        for (String parameter:parameters) {
            Assert.assertTrue("Request doesn't include the parameter "+ parameter,request.has(parameter));
        }
    }

    public void isSigned(JSONObject request, String serverPubKey) {
        has_parameters(request, Arrays.asList("Message", "Hash"));
        String message = request.getString("Message");
        String signedMessage = request.getString("Hash");
        Assert.assertTrue("This message has not been signed by user and is subject to attacks.",
                Utils.verifySignWithPubKeyFile(message, signedMessage, serverPubKey));
    }

    public void isSigned(JSONObject request, String pubkey1, String pubkey2) {
        has_parameters(request, Arrays.asList("Message","Message2", "Hash", "Hash2"));
        String message1 = request.getString("Message");
        String signedMessage1 = request.getString("Hash");
        String message2 = request.getString("Message2");
        String signedMessage2 = request.getString("Hash2");
        Assert.assertTrue("This message has not been signed by user and is subject to attacks.",
                Utils.verifySignWithPubKeyFile(message1, signedMessage1, pubkey1));
        Assert.assertTrue("This message has not been signed by user and is subject to attacks.",
                Utils.verifySignWithPubKeyFile(message2, signedMessage2, pubkey2));
    }

    public void commandSignatureChecker(JSONArray requests, String serverPubKey, String userPubKey, int index) {

        // Client sends cmd
        isSigned(requests.getJSONObject(index), "assymetricKeys/"+userPubKey);
        // Server sends a Challenge
        isSigned(requests.getJSONObject(index+1), serverPubKey);
        // Client Response to the Challenge
        isSigned(requests.getJSONObject(index+2), "assymetricKeys/"+userPubKey);
        // Server sends the result of cmd
        isSigned(requests.getJSONObject(index+3), serverPubKey);

    }

    public void commandSignatureChecker(JSONArray requests, String serverPubKey, String pubkey1, String pubkey2, int index) {

        // Client2 send a request to the server
        isSigned(requests.getJSONObject(index), "assymetricKeys/"+pubkey1,"assymetricKeys/"+pubkey2);
        // Server sends a Challenge
        isSigned(requests.getJSONObject(index+1), serverPubKey);
        // Client Response to the Challenge
        isSigned(requests.getJSONObject(index+2), "assymetricKeys/"+pubkey1);
        // Server sends the result of cmd
        isSigned(requests.getJSONObject(index+3), serverPubKey);

    }

    public void getStateOfGoodChecker(JSONArray requests, String user, String good, String onSale, int index) {
        JSONObject jsonObj = new JSONObject(requests.getJSONObject(index).getString("Message"));
        checkGood(jsonObj, user, good, onSale);
    }

    public void intentionToSellChecker(JSONArray requests, String answer, int index) {
        JSONObject jsonObj = new JSONObject(requests.getJSONObject(index).getString("Message"));
        has_parameters(jsonObj, Arrays.asList("Action", "Timestamp"));
        Assert.assertEquals(answer, jsonObj.getString("Action"));
    }

    public void checkAnswer(JSONObject request, String answer) {
        JSONObject message = new JSONObject(request.getString("Message"));
        has_parameters(message, Arrays.asList("Action", "Timestamp"));
        Assert.assertEquals(answer, message.getString("Action"));
    }

    public void checkGood(JSONObject request, String user, String good, String onSale) {
        JSONObject message;
        if (request.has("Message"))
            message = new JSONObject(request.getString("Message"));
        else
            message = request;
        has_parameters(message, Arrays.asList("Owner", "Good", "OnSale", "Timestamp"));
        Assert.assertEquals("The Owner value is wrong." ,user, message.getString("Owner"));
        Assert.assertEquals("The Good value is wrong.", good, message.getString("Good"));
        Assert.assertEquals("The OnSale value is wrong.", onSale, message.getString("OnSale"));
    }

    public String sendTo(HdsClient c, String hostname, int port, String payload) throws HdsClientException {
        boolean sent = false;

        try {
            // getting localhost ip
            InetAddress ip = InetAddress.getByName(hostname);

            // establish the connection with server port 5056
            Socket s = new Socket(ip, port);

            DataInputStream dis = new DataInputStream(s.getInputStream());
            DataOutputStream dos = new DataOutputStream(s.getOutputStream());
            dos.writeUTF(payload);

            String value = dis.readUTF();
            value = c.solveChallenge(new JSONObject(value), dis, dos);
            s.close();
            dis.close();
            dos.close();
            return value;
        } catch (UnknownHostException e) {
            // TODO
        } catch (IOException e) {
            // TODO
        }
        return "";
    }
    public String sendTo(String hostname, int port, String payload) {
        boolean sent = false;

        try {
            // getting localhost ip
            InetAddress ip = InetAddress.getByName(hostname);

            // establish the connection with server port 5056
            Socket s = new Socket(ip, port);

            DataInputStream dis = new DataInputStream(s.getInputStream());
            DataOutputStream dos = new DataOutputStream(s.getOutputStream());
            dos.writeUTF(payload);

            String value = dis.readUTF();
            s.close();
            dis.close();
            dos.close();
            return value;
        } catch (UnknownHostException e) {
            // TODO
        } catch (IOException e) {
            // TODO
        }
        return "";
    }


}
