package org.iplantc.service.systems.crypt;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class EncryptionTest {

	@DataProvider(name="authConfigPasswordEncryption")
	public Object[][] authConfigTypeChecks() {
    	return new Object[][] {
    			{ "this is the salt", new BigInteger(130, new SecureRandom()).toString(32),  "Encryption is bidirectional"},
    	};
	}
	
	@Test (groups={"encryption", "auth"}, dataProvider="authConfigPasswordEncryption")
    public void authConfigPasswordEncryptionTest(String key, String password, String message) 
    throws Exception 
    {
        Encryption encryption = new Encryption();
        encryption.setPassword(key.toCharArray());
        String encryptedPassword = encryption.encrypt(password);
        
        Assert.assertNotEquals(password, encryptedPassword, "Password was not encrypted.");
        
        String decryptedPassword = encryption.decrypt(encryptedPassword);
        
        Assert.assertEquals(password, decryptedPassword, "Password was not decrypted.");
    }
}
