/**
 * 
 */
package org.iplantc.service.common.auth;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.clients.HTTPSClient;
import org.json.JSONObject;
import org.restlet.security.Verifier;

/**
 * Class to authenticate against the iPlant Auth API
 * @author dooley
 *
 */
public class AuthServiceClient {
//	private static Logger log = Logger.getLogger(AuthServiceClient.class);
	private String username;
	private String pass;
	
	/**
	 *  Instantiates an object that authenticates a user against the iPlant
	 *  auth service using their username and either password or token.
	 */
	public AuthServiceClient(String username, String password)
	{
		this.username = username;
		this.pass = password;
	}
	
	
	/**
	 * This method invokes the iPlant auth service using the credentials from the
	 * constructor. It returns true on success and null on failure or exception.
	 * 
	 * @return int result of authenticating with the credentials from the constructor
	 * @throws Exception 
	 */
	public int login() throws Exception
	{
		HTTPSClient client = new HTTPSClient(Settings.IPLANT_AUTH_SERVICE, username, pass);
		String response = client.getText();
		JSONObject json = null;
		if (!StringUtils.isEmpty(response))
		{
			json = new JSONObject(response);
			if (json.has("status") && json.getString("status").equalsIgnoreCase("success")) 
				return Verifier.RESULT_VALID;
			else
				return Verifier.RESULT_INVALID;
		} else {
			return Verifier.RESULT_INVALID;
		}
	}
}
