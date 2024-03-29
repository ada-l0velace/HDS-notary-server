package pt.tecnico.hds.server;

import pteidlib.PTEID_Certif;
//import pteidlib.PteidException;
import pteidlib.pteid;

import sun.security.pkcs11.wrapper.CK_ATTRIBUTE;
import sun.security.pkcs11.wrapper.CK_C_INITIALIZE_ARGS;
import sun.security.pkcs11.wrapper.CK_MECHANISM;
import sun.security.pkcs11.wrapper.CK_SESSION_INFO;
import sun.security.pkcs11.wrapper.PKCS11;
import sun.security.pkcs11.wrapper.PKCS11Constants;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.security.Key;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class eIDLib_PKCS11 implements SigningInterface {


	private final String pubKeyPath = "assymetricKeys/server.pub"; 
    private PKCS11 pkcs11;
    private String osName;
    private long p11_session;
    private String javaVersion;
    private String libName = "libpteidpkcs11.so";
    private java.util.Base64.Encoder encoder;
    private Key pub;


    public eIDLib_PKCS11() throws java.security.cert.CertificateException {
        System.out.println("            //Load the PTEidlibj");

        //System.load("");
        osName = System.getProperty("os.name");

        if (osName.equals(PTeID4JUtils.OS_LINUX)) {

            try {
                //LOGGER.debug("Loading the PTeID lib from {}...", "/usr/local/lib/libpteidlibj.so");
                System.load("/usr/local/lib/libpteidlibj.so");

            } catch (UnsatisfiedLinkError unsatisfiedLinkErrorLinux01) {
                //LOGGER.debug("Loading the PTeID lib from {}...","/usr/local/lib/pteid_jni/libpteidlibj.so");
                System.load("/usr/local/lib/pteid_jni/libpteidlibj.so");
            	//System.load("/usr/local/lib/libpteid.so");
              
            }
        }
        javaVersion = System.getProperty("java.version");

        encoder = java.util.Base64.getEncoder();
        X509Certificate cert = getCertFromByteArray(getCertificateInBytes(0));
        pub = cert.getPublicKey();
        PublicKey pubKey = (PublicKey) pub;
        byte[] pubKeyEncoded = pubKey.getEncoded();
        //System.out.println(printHexBinary(pubKeyEncoded));

        try {
            pteid.Init(""); // Initializes the eID Lib
            
            Class pkcs11Class = Class.forName("sun.security.pkcs11.wrapper.PKCS11");

            if (javaVersion.startsWith("1.5."))
            {
                Method getInstanceMethode = pkcs11Class.getDeclaredMethod("getInstance", String.class, CK_C_INITIALIZE_ARGS.class, boolean.class);
                pkcs11 = (PKCS11)getInstanceMethode.invoke(null, new Object[] { libName, null, false });
            }
            else
            {
                Method getInstanceMethode = pkcs11Class.getDeclaredMethod("getInstance", String.class, String.class, CK_C_INITIALIZE_ARGS.class, boolean.class);
                pkcs11 = (PKCS11)getInstanceMethode.invoke(null, new Object[] { libName, "C_GetFunctionList", null, false });
            }
            
            //Open the PKCS11 session
            System.out.println("            //Open the PKCS11 session");
            p11_session = pkcs11.C_OpenSession(0, PKCS11Constants.CKF_SERIAL_SESSION, null, null);

            // Token login
            System.out.println("            //Token login");
            pkcs11.C_Login(p11_session, 1, null);
            CK_SESSION_INFO info = pkcs11.C_GetSessionInfo(p11_session);
            
            System.out.println("Writing Pubic key to '" + pubKeyPath + "' ..." );
        	FileOutputStream pubFos = new FileOutputStream(pubKeyPath);
        	pubFos.write(pubKeyEncoded);
        	pubFos.close();
        } catch(Exception e) {
        	e.printStackTrace();
        	System.exit(1);
        }
        if (-1 != osName.indexOf("Windows"))
            libName = "pteidpkcs11.dll";
        else if (-1 != osName.indexOf("Mac"))
            libName = "pteidpkcs11.dylib";

    }

    public Boolean verifySignWithPubKey(String message, String signedMessage) {
        return Utils.verifySignWithPubKey(message,signedMessage, pub);
    }

    public String signWithPrivateKey(String message) throws Throwable {
        pteid.SetSODChecking(false); // Don't check the integrity of the ID, address and photo (!)



        
        // Get available keys
        System.out.println("            //Get available keys");
        CK_ATTRIBUTE[] attributes = new CK_ATTRIBUTE[1];
        attributes[0] = new CK_ATTRIBUTE();
        attributes[0].type = PKCS11Constants.CKA_CLASS;
        attributes[0].pValue = new Long(PKCS11Constants.CKO_PRIVATE_KEY);

        pkcs11.C_FindObjectsInit(p11_session, attributes);
        long[] keyHandles = pkcs11.C_FindObjects(p11_session, 5);

        // points to auth_key
        System.out.println("            //points to auth_key. No. of keys:"+keyHandles.length);

        long signatureKey = keyHandles[0]; // 0- authentication; 1- signature
        pkcs11.C_FindObjectsFinal(p11_session);


        // initialize the signature method
        System.out.println("            //initialize the signature method");
        CK_MECHANISM mechanism = new CK_MECHANISM();
        mechanism.mechanism = PKCS11Constants.CKM_SHA256_RSA_PKCS; // CKM_SHA1_RSA_PKCS
        mechanism.pParameter = null;
        pkcs11.C_SignInit(p11_session, mechanism, signatureKey);

        byte[] signature = pkcs11.C_Sign(p11_session, message.getBytes(Charset.forName("UTF-8")));
        //pkcs11.C_Verify(p11_session, mechanism, );


        return encoder.encodeToString(signature);
    }

    @Override
    public String getKeyName() {
        return "server";
    }

    private static  byte[] getCertificateInBytes(int n) {
        byte[] certificate_bytes = null;
        try {
            PTEID_Certif[] certs = pteid.GetCertificates();
            System.out.println("Number of certs found: " + certs.length);
            int i = 0;
            for (PTEID_Certif cert : certs) {
                System.out.println("-------------------------------\nCertificate #"+(i++));
                System.out.println(cert.certifLabel);
            }

            certificate_bytes = certs[n].certif; //gets the byte[] with the n-th certif

        } catch (Exception e) {
            e.printStackTrace();
        }
        return certificate_bytes;
    }
    
    public void terminate() {
    	try {
    		pkcs11.C_Logout(p11_session);
    		pkcs11.C_CloseSession(p11_session);
    		pteid.Exit(pteid.PTEID_EXIT_LEAVE_CARD);
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }

    public static X509Certificate getCertFromByteArray(byte[] certificateEncoded) throws CertificateException{
        CertificateFactory f = CertificateFactory.getInstance("X.509");
        InputStream in = new ByteArrayInputStream(certificateEncoded);
        X509Certificate cert = (X509Certificate)f.generateCertificate(in);
        return cert;
    }
}
