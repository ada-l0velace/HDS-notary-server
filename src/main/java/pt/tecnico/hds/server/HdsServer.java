package pt.tecnico.hds.server;

import java.io.*;
import java.net.Socket;

public class HdsServer implements Runnable {
    private Socket connection;
    private String TimeStamp;
    private int ID;
    private DataInputStream dis;
    private DataOutputStream dos;

    HdsServer(Socket s, int i, DataInputStream dis, DataOutputStream dos) {
        this.connection = s;
        this.ID = i;
        this.dis = dis;
        this.dos = dos;
    }

    public void run() {
        String received;
        String toreturn;
        while (true) {
            try {

                // Ask user what he wants
                dos.writeUTF("What do you want?[transferGood | intentionToSell | buyGood | getStateOfGood]..\n" +
                        "Type Exit to terminate connection.");

                // receive the answer from client
                received = dis.readUTF();

                if (received.equals("Exit")) {
                    System.out.println("Client " + this.connection + " sends exit...");
                    System.out.println("Closing this connection.");
                    this.connection.close();
                    System.out.println("Connection closed");
                    this.dis.close();
                    this.dos.close();
                    break;
                }

                this.TimeStamp = new java.util.Date().toString();

                // write on output stream based on the
                // answer from the client
                switch (received) {

                    case "transferGood" :
                        toreturn = "transferGood";
                        dos.writeUTF(toreturn);
                        break;

                    case "intentionToSell" :
                        toreturn = "intentionToSell";
                        dos.writeUTF(toreturn);
                        break;

                    case "buyGood" :
                        toreturn = "buyGood";
                        dos.writeUTF(toreturn);
                        break;

                    case "getStateOfGood" :
                        toreturn = "getStateOfGood";
                        dos.writeUTF(toreturn);
                        break;

                    default:
                        dos.writeUTF("Invalid input");
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
