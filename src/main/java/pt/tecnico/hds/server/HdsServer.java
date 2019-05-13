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
             //if (!action.equals("Echo")) {
                 if (r.computationalCostChallenge())
                     received = action;
                 else
                     received = "Error";
             /*} else {
                 received = action;
             }*/
             //String signature;
             JSONObject message;
             JSONObject value = new JSONObject(json.getString("Message"));
             System.out.println(received);
             switch (received) {

                 case "transferGood":
                     JSONObject message2 = new JSONObject(json.getString("Message2"));
                     //signature = json.getString("ValueSignature");
                     message = nt.transferGood(jsonObj, message2, hash, json.getString("Hash2"));
                     jsontr = nt.buildReply(message);
                     //nt.updateRegister(value, hash);
                     toreturn = jsontr.toString();
                     //dos.writeUTF(toreturn);
                     System.out.println("Returning message is: " + toreturn);
                     break;

                 case "intentionToSell":
                     message = nt.intentionToSell(jsonObj, hash);
                     System.out.println(json);
                     //signature = json.getString("ValueSignature");
                     jsontr = nt.buildReply(message);
                     toreturn = jsontr.toString();
                     //dos.writeUTF(toreturn);
                     System.out.println("Returning message is: " + toreturn);
                     break;

                 case "getStateOfGood":
                     System.out.println("WTF");
                     String good = jsonObj.getString("Good");
                     message = nt.getStateOfGood(jsonObj, hash);
                     System.out.println("WTF2 + " + message);
                     message.put("rid",jsonObj.getLong("rid"));
                     toreturn = nt.buildReply(message).toString();
                     toreturn = nt.buildState(toreturn, good, jsonObj);
                     dos.writeUTF(toreturn);
                     System.out.println("Returning message is: " + toreturn);
                     break;

                 case "Echo":
                     System.out.println("Got Something");
                     message = nt.receiveEcho(json);
                     toreturn = nt.buildReply(message).toString();
                     dos.writeUTF(toreturn);

                 case "WriteBack":
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