package pt.tecnico.hds.server;

import org.apache.log4j.PropertyConfigurator;

public class Main {

    public static Boolean debug = true;
    public static int f = 1;
    public static int N = 3*f+1;

    public static void main(String[] args) {
        if(args.length == 0) {
            Notary nt = new Notary();
        }
        else {
            System.out.println(args[0]);
            Notary nt = new Notary(Integer.parseInt(args[0]));
        }
    }

}
