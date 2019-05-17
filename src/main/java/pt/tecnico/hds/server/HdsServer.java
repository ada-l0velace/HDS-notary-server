package pt.tecnico.hds.server;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;


import org.apache.log4j.PropertyConfigurator;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.date.ExceptionUtils;

public class HdsServer implements Runnable {

    private Socket connection;
    private String TimeStamp;
    private int ID;
    private DataInputStream dis;
    private Notary nt;
    private DataOutputStream dos;
    private String a;
    public final static Logger logger = LoggerFactory.getLogger(HdsServer.class);


    public HdsServer(Socket s, int i, DataInputStream dis, DataOutputStream dos, Notary nt) {
        this.connection = s;
        this.ID = i;
        this.dis = dis;
        this.dos = dos;
        this.nt = nt;
    }

    public String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    public void run() {
        String received = "";
        String toreturn = "";
        //System.out.println("Server " + this.connection + " Opens...");
        try {
            boolean eof = false;
            while (!eof) {
                try {
                    received = dis.readUTF();
                    eof = true;
                } catch (EOFException e) {
                    eof = false;
                }
            }

            this.TimeStamp = new java.util.Date().toString();
            // write on output stream based on the
            // answer from the client
            JSONObject json = new JSONObject(received);
            String hash = json.getString("Hash");
            JSONObject jsonObj = new JSONObject(json.getString("Message"));

            Request r = new Request(nt, dis, dos);
            JSONObject jsontr;
            String action = jsonObj.getString("Action");
            if (!action.equals("Echo") && !action.equals("Ready")) {
                if (r.computationalCostChallenge())
                    received = action;
                else
                    received = "Error";
            } else {
                //System.out.println(json.toString());
                //must be signed by a server
                if (nt.verifySignatureAndFreshness(jsonObj, hash))
                    received = action;
                else {

                    dis.close();
                    dos.close();
                    connection.close();
                    return;
                }
            }
            //String signature;
            JSONObject message;
            //System.out.println(received);
            Broadcast broadcaster;
            String good;
            JSONObject _value;
            switch (received) {

                case "transferGood":
                    good = jsonObj.getString("Good");
                    nt.getBroadcasterLock(good).acquire();
                    broadcaster = nt.getBroadcaster(good);

                    broadcaster.broadcast(json.toString());

                    if (broadcaster.isDelivered()) {
                        JSONObject message2 = new JSONObject(json.getString("Message2"));
                        jsontr = nt.transferGood(jsonObj, message2, hash, json.getString("Hash2"));
                    } else {
                        jsontr = new JSONObject();
                        jsontr.put("Action", "NO");

                    }
                    broadcaster.clear();
                    nt.getBroadcasterLock(good).release();
                    toreturn = nt.buildReply(jsontr).toString();
                    dos.writeUTF(toreturn);
                    System.out.println("Returning message is: " + toreturn);
                    break;

                case "intentionToSell":
                    good = jsonObj.getString("Good");
                    nt.getBroadcasterLock(good).acquire();
                    broadcaster = nt.getBroadcaster(good);
                    broadcaster.broadcast(json.toString());
                    System.out.println("DELIVERED" + broadcaster.isDelivered());
                    if (broadcaster.isDelivered()) {
                        jsontr = nt.intentionToSell(jsonObj, hash);
                    } else {
                        jsontr = new JSONObject();
                        jsontr.put("Action", "NO");
                    }
                    broadcaster.clear();
                    nt.getBroadcasterLock(good).release();
                    toreturn = nt.buildReply(jsontr).toString();
                    //System.out.println(toreturn);
                    dos.writeUTF(toreturn);
                    System.out.println("Returning message is: " + toreturn);
                    break;

                case "getStateOfGood":
                    good = jsonObj.getString("Good");
                    message = nt.getStateOfGood(jsonObj, hash);
                    message.put("rid", jsonObj.getLong("rid"));
                    toreturn = nt.buildReply(message).toString();
                    toreturn = nt.buildState(toreturn, good, jsonObj);
                    dos.writeUTF(toreturn);
                    System.out.println("Returning message is: " + toreturn);
                    break;

                case "Echo":
                    _value = new JSONObject(jsonObj.getString("Value"));
                    message = new JSONObject(_value.getString("Message"));
                    good = message.getString("Good");
                    broadcaster = nt.getBroadcaster(good);
                    //System.out.println("new echo from: "+json.toString());
                    broadcaster.echo(jsonObj.getInt("pid"), jsonObj.getString("Value"));


                    break;

                case "Ready":
                    _value = new JSONObject(jsonObj.getString("Value"));
                    message = new JSONObject(_value.getString("Message"));
                    good = message.getString("Good");
                    broadcaster = nt.getBroadcaster(good);
                    //System.out.println("new ready from: "+json.toString());
                    broadcaster.ready(jsonObj.getInt("pid"), jsonObj.getString("Value"));
                    break;

                case "WriteBack":
                    //System.out.println(received);
                    message = nt.writeback(json);
                    toreturn = nt.buildReply(message).toString();
                    dos.writeUTF(toreturn);
                    break;


                default:
                    message = nt.invalid();
                    toreturn = nt.buildReply(message).toString();
                    dos.writeUTF(toreturn);
                    System.out.println("Returning message is: " + toreturn);
                    break;
            }
        } catch (EOFException | SocketException eofError) { // Normally Occurs when the client socket dies
            //eofError.printStackTrace();
            logger.info(getStackTrace(eofError));
            //nt.reset();
        } catch (Exception e) {
            e.printStackTrace();
            //nt.reset();
            System.out.println("ERROR: " + received);
            logger.info(getStackTrace(e));
            try {
                dos.writeUTF("Invalid input");
            } catch (IOException e1) {
                //e1.printStackTrace();
                logger.info(getStackTrace(e));
            }
        }
        try {
            //System.out.println("Client " + this.connection + " sends exit...");
            //System.out.println("Connection closed");
            this.dis.close();
            this.dos.close();
            this.connection.close();
        } catch (Exception e) {
            //nt.reset();
            logger.info(getStackTrace(e));
        }
    }
}