package pt.tecnico.hds.server;

public class Main {

    public static Boolean debug = true;

    public static void main(String[] args) {
        if(args.length == 0) {
            Notary nt = new Notary();
        }
        else {
            Notary nt = new Notary(Integer.parseInt(args[0]));
        }
    }

}
