package pt.tecnico.hds.server;

public class DebugSigning implements SigningInterface {
    private final String folder;

    public DebugSigning() {
        folder = "assymetricKeys/";
    }

    public Boolean verifySignWithPubKey(String message, String signedMessage) {
        return Utils.verifySignWithPubKeyFile(message,signedMessage, folder+"serverDebug.pub");
    }

    public String signWithPrivateKey(String message) throws Throwable {
        return Utils.signWithPrivateKey(message, folder+"serverDebug");
    }

}
