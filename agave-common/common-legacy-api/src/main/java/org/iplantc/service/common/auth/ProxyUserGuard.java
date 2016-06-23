package org.iplantc.service.common.auth;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.X509Certificate;

import javax.crypto.Cipher;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.io.pem.PemReader;
import org.iplantc.service.common.Settings;

/**
 * Used to verify user proxy tokens.
 */
public class ProxyUserGuard {

    /**
     * We need the Bouncy Castle provider to read PEM files.
     */
    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    /**
     * The encoding we expect for all tokens.
     */
    public static final String ENCODING = "ISO-8859-1";

    /**
     * Used to log informational messages.
     */
    private static final Logger LOG = Logger.getLogger(ProxyUserGuard.class);

    /**
     * The delimiter used to separate token components.
     */
    private static final String TOKEN_COMPONENT_SEPARATOR = "|";

    /**
     * Determines whether or not the given token is valid for any of our trusted clients.
     * 
     * @param identifier the user ID.
     * @param token the secret token.
     * @return true if the token is valid.
     * @throws IOException if the certificate file can't be read for one of our trusted users.
     * @throws GeneralSecurityException if a certificate can't be loaded or uses an unsupported algorithm.
     */
    public boolean checkToken(String identifier, char[] token) throws IOException, GeneralSecurityException {
        boolean isValid = false;
        for (String trustedUser : Settings.TRUSTED_USERS) {
            if (tokenValid(trustedUser, identifier, token)) {
                isValid = true;
                break;
            }
        }
        return isValid;
    }

    /**
     * Determines whether or not the given token is valid for the given certificate file and user ID.
     * 
     * @param certFile the name of the certificate file.
     * @param id the user ID.
     * @param token the secret token.
     * @return true if the token is valid.
     * @throws IOException if the certificate file can't be read.
     * @throws GeneralSecurityException if the certificate can't be loaded or uses an unsupported algorithm.
     */
    private boolean tokenValid(String certFile, String id, char[] token) throws IOException, GeneralSecurityException {
        PublicKey publicKey = loadPublicKeyFromCertificateFile(certFile);
        String decryptedToken = decryptToken(publicKey, token);
        return decryptedToken == null ? false : isValidTokenFormat(id, decryptedToken);
    }

    /**
     * Determines whether or not the token is in the format we expect. At this time, we expect the token to contain
     * the user ID, a token component separator, and a timestamp.
     * 
     * @param id the expected user ID.
     * @param token the token.
     * @return true if the token is in the expected format.
     */
    private boolean isValidTokenFormat(String id, String token) {
        boolean isValid = false;
        String[] components = token.split("\\Q" + TOKEN_COMPONENT_SEPARATOR);
        if (components.length == 2 && id.equals(components[0]) && isValidTimestamp(components[1])) {
            isValid = true;
        }
        return isValid;
    }

    /**
     * Determines whether or not the given timestamp is valid. At this time, any timestamp that can be parsed as a
     * long value is considered to be valid.
     * 
     * @param timestamp the timestamp.
     * @return true if the timestamp is valid.
     */
    private boolean isValidTimestamp(String timestamp) {
        try {
            Long.parseLong(timestamp);
            return true;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Decrypts a token using the given public key.
     * 
     * @param publicKey the public key.
     * @param token the token to decrypt.
     * @return the decrypted token as a string.
     * @throws GeneralSecurityException if the public key uses an unsupported algorithm.
     */
    private String decryptToken(PublicKey publicKey, char[] token) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(publicKey.getAlgorithm());
        cipher.init(Cipher.DECRYPT_MODE, publicKey);
        byte[] decrypted = null;
        try {
            LOG.warn("Encrypted Token Length: " + Base64.decode(new String(token)).length);
            decrypted = cipher.doFinal(Base64.decode(new String(token)));
        }
        catch (GeneralSecurityException e) {
            return null;
        }
        return stringFromBytes(decrypted);
    }

    /**
     * Converts a byte array to a String using the encoding that we expect for all tokens. An unsupported encoding
     * exception is an unrecoverable error at this point, so a runtime exception is thrown if the encoding isn't
     * supported.
     * 
     * @param bytes the byte array to convert.
     * @return the byte array as a String.
     */
    private String stringFromBytes(byte[] bytes) {
        try {
            return new String(bytes, ENCODING);
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads a public key from a PEM encoded certificate file.
     * 
     * @param file the name of the certificate file.
     * @return the public key.
     * @throws IOException if the file can't be read or does not contain an X.509 certificate.
     */
    private PublicKey loadPublicKeyFromCertificateFile(String file) throws IOException {
    	PemReader pemReader = null;
    	try {
    		pemReader = new PemReader(new InputStreamReader(getCertFileAsStream(file)));
	    	Object extractedObject = pemReader.readPemObject();
	        if (!(extractedObject instanceof X509Certificate)) {
	            throw new IOException(file + " does not contain an X.509 certificate");
	        }
	        return ((X509Certificate) extractedObject).getPublicKey();
    	}
    	finally {
    		try { pemReader.close(); } catch (Exception e) {}
    	}
    }

    /**
     * Gets the certificate file as an input stream.
     * 
     * @param file the basename of the file to get as an input stream.
     * @return the input stream.
     * @throws IOException if the file can't be opened.
     */
    private InputStream getCertFileAsStream(String file) throws IOException {
        String fullName = "certs/" + file + ".crt";
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(fullName);
        if (in == null) {
            throw new IOException("unable to open " + fullName + " for input");
        }
        return in;
    }
}
