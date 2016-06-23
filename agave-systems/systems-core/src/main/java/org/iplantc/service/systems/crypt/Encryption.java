
package org.iplantc.service.systems.crypt;

import org.jasypt.util.text.BasicTextEncryptor;

/**
 * Provides bidirection encryption based on the PKCS #5 algorithm.
 * 
 * @author dooley
 *
 */
public class Encryption {
    
	private BasicTextEncryptor encryptor = null;

    public void setPassword(char[] password)
    {	
    	if (password.length == 0) {
            encryptor = null;
        } else {
            encryptor = new BasicTextEncryptor();
            encryptor.setPassword(new String(password));
        	
        }
    }

    public String encrypt(String cleartext)
    {
        return encryptor.encrypt(cleartext);
    }

    public String decrypt(String content)
    {
        return encryptor.decrypt(content);
    }
}
