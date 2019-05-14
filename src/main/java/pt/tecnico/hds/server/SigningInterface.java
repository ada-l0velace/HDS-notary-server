package pt.tecnico.hds.server;

public interface SigningInterface {

    Boolean verifySignWithPubKey(String message, String signedMessage);
    String signWithPrivateKey(String message) throws Throwable;
    String getKeyName();
}
