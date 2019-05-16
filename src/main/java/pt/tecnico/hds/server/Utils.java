package pt.tecnico.hds.server;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;


public class Utils {
    public static Boolean verifySignWithPubKeyFile(String message, String signedMessage, String pubKeyFile) {
        try {
            Key loadedKey = read(pubKeyFile);
            return verifySignWithPubKey(message, signedMessage, loadedKey);
            /*byte[] pubKeyBytes = loadedKey.getEncoded();


            X509EncodedKeySpec ks = new X509EncodedKeySpec(pubKeyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey pub = kf.generatePublic(ks);

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(pub);
            sig.update(message.getBytes());


            return sig.verify(new BASE64Decoder().decodeBuffer(signedMessage));*/
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static Boolean verifySignWithPubKey(String message, String signedMessage, Key pubKey) {
        try {
            Key loadedKey = pubKey;
            byte[] pubKeyBytes = loadedKey.getEncoded();


            X509EncodedKeySpec ks = new X509EncodedKeySpec(pubKeyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey pub = kf.generatePublic(ks);

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(pub);
            sig.update(message.getBytes());


            return sig.verify(new BASE64Decoder().decodeBuffer(signedMessage));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String signWithPrivateKey(String message, String privKeyFile) {
        try {
            Key loadedKey = read(privKeyFile);
            byte[] privKeyBytes = loadedKey.getEncoded();

            PKCS8EncodedKeySpec ks = new PKCS8EncodedKeySpec(privKeyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");

            PrivateKey priv = kf.generatePrivate(ks);

            Signature sig = Signature.getInstance("SHA256withRSA");

            sig.initSign(priv);
            sig.update(message.getBytes("UTF-8"));
            return new BASE64Encoder().encode(sig.sign());
        }
        catch (Exception e) {
            //e.printStackTrace();
        }
        return "";
    }

    public static Key read(String keyPath) throws IOException {
        //System.out.println("Reading key from file " + keyPath + " ...");
        FileInputStream fis = new FileInputStream(keyPath);
        byte[] encoded = new byte[fis.available()];
        fis.read(encoded);
        fis.close();
        return new SecretKeySpec(encoded, "RSA");
    }


    public static String getSHA256(String input) {

        try {

            // Static getInstance method is called with hashing SHA
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // digest() method called
            // to calculate message digest of an input
            // and return array of byte
            byte[] messageDigest = md.digest(input.getBytes());

            // Convert byte array into signum representation
            BigInteger no = new BigInteger(1, messageDigest);

            // Convert message digest into hex value
            String hashtext = no.toString(16);

            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }

            return hashtext;
        }

        // For specifying wrong message digest algorithms
        catch (NoSuchAlgorithmException e) {
            System.out.println("Exception thrown"
                    + " for incorrect algorithm: " + e);

            return null;
        }
    }

    public static String getSHA512(String input) {

        try {

            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for(int i=0; i< bytes.length ;i++){
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        }

        // For specifying wrong message digest algorithms
        catch (NoSuchAlgorithmException e) {
            System.out.println("Exception thrown"
                    + " for incorrect algorithm: " + e);

            return null;
        }
    }

}
