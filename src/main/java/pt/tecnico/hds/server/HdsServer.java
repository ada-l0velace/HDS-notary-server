package pt.tecnico.hds.server;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.sql.*;


import org.json.JSONObject;

public class HdsServer implements Runnable {

    private Socket connection;
    private String TimeStamp;
    private int ID;
    private DataInputStream dis;
    private Notary nt;
    private DataOutputStream dos;
    private String a;


    public HdsServer(Socket s, int i, DataInputStream dis, DataOutputStream dos, Notary nt) {
        this.connection = s;
        this.ID = i;
        this.dis = dis;
        this.dos = dos;
        this.nt = nt;
    }

    public void run() {
        String received = "";
        String toreturn = "";
        System.out.println("Server " + this.connection + " Opens...");
         try {
             // receive the answer from client
             received = dis.readUTF();
             System.out.println(received);
             this.TimeStamp = new java.util.Date().toString();
             // write on output stream based on the
             // answer from the client

             JSONObject json = new JSONObject(received);
             String hash = json.getString("Hash");
             JSONObject jsonObj = new JSONObject(json.getString("Message"));
             Request r = new Request(nt, dis,dos);
             JSONObject jsontr;
             String action = jsonObj.getString("Action");
             if (!action.equals("Echo")) {
                 if (r.computationalCostChallenge())
                     received = action;
                 else
                     received = "Error";
             } else {
                 received = action;
             }
             //String signature;
             JSONObject message;
             JSONObject value = new JSONObject(json.getString("Message"));
             System.out.println(received);
             switch (received) {

                 case "transferGood":
                     JSONObject message2 = new JSONObject(json.getString("Message2"));
                     jsontr = nt.transferGood(jsonObj, message2, hash, json.getString("Hash2"));
                     if (jsontr.getString("Action").equals("YES"))
                         jsontr = nt.rm.waitForEcho(json);
                     toreturn = nt.buildReply(jsontr).toString();
                     dos.writeUTF(toreturn);
                     System.out.println("Returning message is: " + toreturn);
                     break;

                 case "intentionToSell":
                     jsontr = nt.intentionToSell(jsonObj, hash);
                     if (jsontr.getString("Action").equals("YES"))
                         jsontr = nt.rm.waitForEcho(json);
                     System.out.println("WTFIGO");
                     toreturn = nt.buildReply(jsontr).toString();
                     System.out.println(toreturn);
                     System.out.println("WTFIGOdasd");
                     dos.writeUTF(toreturn);
                     System.out.println("WTFIGOwritten");
                     System.out.println("Returning message is: " + toreturn);
                     break;

                 case "getStateOfGood":
                     String good = jsonObj.getString("Good");
                     message = nt.getStateOfGood(jsonObj, hash);
                     message.put("rid",jsonObj.getLong("rid"));
                     toreturn = nt.buildReply(message).toString();
                     toreturn = nt.buildState(toreturn, good, jsonObj);
                     dos.writeUTF(toreturn);
                     System.out.println("Returning message is: " + toreturn);
                     break;

                 case "Echo":
                     nt.receiveEchoes(jsonObj);
                     break;


                 case "WriteBack":
                     System.out.println(received);
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
             eofError.printStackTrace();
         }

          catch (Exception e) {
             e.printStackTrace();
             System.out.println("ERROR: " + received );
             try {
                 dos.writeUTF("Invalid input");
             } catch (IOException e1) {
                 e1.printStackTrace();
             }
         }
         try {
             System.out.println("Client " + this.connection + " sends exit...");
             this.connection.close();
             System.out.println("Connection closed");
             this.dis.close();
             this.dos.close();
         } catch (Exception e) {
             e.printStackTrace();
         }
    }
}