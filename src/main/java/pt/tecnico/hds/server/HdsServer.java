package pt.tecnico.hds.server;

import java.io.*;
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
    private HdsRegister reg = new HdsRegister();



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

        while (true) {
            try {
                // receive the answer from client
                received = dis.readUTF();

                if (received.equals("Exit")) {
                    break;
                }

                this.TimeStamp = new java.util.Date().toString();
                // write on output stream based on the
                // answer from the client

                JSONObject json = new JSONObject(received);
                String hash = json.getString("Hash");
                JSONObject jsonObj = new JSONObject(json.getString("Message"));
                Request r = new Request(nt, dis,dos);
                JSONObject jsontr;
                if (r.computationalCostChallenge())
                    received = jsonObj.getString("Action");
                else
                    received = "Error";


                JSONObject message;
                switch (received) {

                    case "transferGood":
                        JSONObject message2 = new JSONObject(json.getString("Message2"));
                        message = nt.transferGood(jsonObj, message2, hash, json.getString("Hash2"));
                        jsontr = nt.buildReply(message);
                        //updateRegister(jsontr);
                        toreturn = jsontr.toString();
                        dos.writeUTF(toreturn);
                        System.out.println(toreturn);
                        break;

                    case "intentionToSell":
                        message = nt.intentionToSell(jsonObj, hash);
                        //System.out.println(toreturn);
                        jsontr = nt.buildReply(message);
                        updateRegister(jsonObj);
                        toreturn = jsontr.toString();
                        dos.writeUTF(toreturn);
                        System.out.println(toreturn);
                        break;

                    case "getStateOfGood":
                        message = nt.getStateOfGood(jsonObj, hash);
                        toreturn = nt.buildReply(message).toString();
                        System.out.println(toreturn);
                        dos.writeUTF(toreturn);
                        System.out.println(toreturn);
                        break;

                    default:
                    	message = nt.invalid();
                    	toreturn = nt.buildReply(message).toString();
                        dos.writeUTF(toreturn);
                        System.out.println(toreturn);
                        break;
                }
            } catch (EOFException | SocketException eofError) { // Normally Occurs when the client socket dies
                eofError.printStackTrace();
                //System.out.println(e0.getMessage());
                break;
            }

            // Client Socket closed
            //System.out.println(socketEx.getMessage());

            catch (Exception e) {
                e.printStackTrace();
                System.out.println(received);
                try {
                    dos.writeUTF("Invalid input");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        try {
            System.out.println("Client " + this.connection + " sends exit...");
            System.out.println("Closing this connection.");
            this.connection.close();
            System.out.println("Connection closed");
            this.dis.close();
            this.dos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void updateRegister(JSONObject message) {
    	long ts = message.getLong("Timestamp");
        reg._rid++;

    	//System.out.println(j);
    	if ( ts > reg.getTimestamp()) {
            reg.deliveryWrite(message.getString("Good"), ts);
    	}
    }

}