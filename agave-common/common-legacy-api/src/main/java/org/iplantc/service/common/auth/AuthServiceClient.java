/**
 * 
 */
package org.iplantc.service.common.auth;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.iplantc.service.auth.dao.AuthenticationTokenDao;
import org.iplantc.service.auth.model.AuthenticationToken;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.clients.HTTPSClient;
import org.json.JSONObject;

/**
 * Class to authenticate against the iPlant Auth API
 * @author dooley
 *
 */
public class AuthServiceClient {
	private static Logger log = Logger.getLogger(AuthServiceClient.class);
	private String username;
	private String pass;
	private AuthenticationToken authToken;
	
	
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
	 * @return boolean result of authenticating with the credentials from the constructor
	 */
	public boolean remoteLogin()
	{
		HTTPSClient client = null;
		try {
			client = new HTTPSClient(Settings.IPLANT_AUTH_SERVICE, username, pass);
			String response = client.getText();
			JSONObject json = null;
			if (!StringUtils.isEmpty(response))
			{
				json = new JSONObject(response);
				if (json.has("status"))
					return json.getString("status").equalsIgnoreCase("success");
				else
					return false;
			}
			throw new IOException("Invalid response received from the server");
			
		} catch (Exception e) {
			log.error("Failed to authenticate against " + Settings.IPLANT_AUTH_SERVICE, e);
			return false;
		}
	}
	
	/**
	 * Queries the auth service database for a matching. If none is found, it
	 * tries ldap to see if the token is a valid password. If either succees
	 * an AuthenticationToken is created and stored as this object's authToken
	 * variable.
	 * 
	 * @return true is the u/p is valid or a valid token. false otherwise.
	 * @throws PermissionException 
	 */
	public boolean manualLogin() throws PermissionException 
	{
		try {
			AuthenticationTokenDao dao = new AuthenticationTokenDao();
			authToken = dao.findByToken(pass);
//			if (authToken == null) {
//				 no token, try ldap instead
//				LDAPClient client = new LDAPClient(username, pass);
//		        if (!client.login()) {
//		        	return false;
//		        } else {
//		        	authToken = new AuthenticationToken(username);
//		        	return true;
//		        }
//			} else 
			if (authToken != null && StringUtils.equals(authToken.getUsername(), username))
			{
				this.setAuthToken(authToken);
				return true;
			} else {
				return false;
			}
		} 
		catch (HibernateException e) {
			throw new PermissionException("Failed to query database for token.", e);
		} 
		catch (Exception e) {
			throw new PermissionException("Failed to authenticate user", e);
		}
	}
	
	/**
	 * @return the authToken
	 */
	public AuthenticationToken getAuthToken()
	{
		return authToken;
	}


	/**
	 * @param authToken the authToken to set
	 */
	public void setAuthToken(AuthenticationToken authToken)
	{
		this.authToken = authToken;
	}
}
