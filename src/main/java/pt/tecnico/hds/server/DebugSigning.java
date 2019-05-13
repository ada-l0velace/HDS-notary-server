package pt.tecnico.hds.server;

public class DebugSigning implements SigningInterface {
    private final String folder;
    private int index;
    public DebugSigning(int index) {
        this.index = index;
        folder = "assymetricKeys/";
    }

    public Boolean verifySignWithPubKey(String message, String signedMessage) {
        return Utils.verifySignWithPubKeyFile(message,signedMessage, folder+ getKeyName()+".pub");
    }

    public String signWithPrivateKey(String message) throws Throwable {
        return Utils.signWithPrivateKey(message, folder+ getKeyName());
    }

    public String getKeyName(){
        return String.format("serverDebug%d", index);
    }

}
