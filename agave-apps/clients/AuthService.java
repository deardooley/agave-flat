/**
 * 
 */
package org.iplantc.service.clients;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.iplantc.service.clients.exceptions.APIClientException;
import org.iplantc.service.clients.exceptions.AuthenticationException;
import org.iplantc.service.clients.model.AuthenticationToken;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Client class for interacting with the Auth Service. All API clients
 * require the use of an authentication token to access secured services.
 * This class enables the retrieval and management of tokens.
 * 
 * @author dooley
 *
 */
public class AuthService extends AbstractService {
	
	private AuthenticationToken token = null;

	public AuthService(String baseUrl)
	throws AuthenticationException
	{
		this.baseUrl = baseUrl;
	}
	
	/**
	 * Retrieves a basic 2 hour token for the user.
	 * 
	 * @param apiUsername
	 * @param apiPassword
	 * @return
	 * @throws AuthenticationException
	 */
	public AuthenticationToken getToken(String apiUsername, String apiPassword)
	throws AuthenticationException
	{
		if (isTokenValid(token)) {
			return token;
		} else {
			this.token = getToken(apiUsername, apiPassword, null, null, null);
			return token;
		}
	}
	
	/**
	 * Retrieves a token for the user valid for 2 hours or maxUses, whichever comes first.
	 * 
	 * @param apiUsername
	 * @param apiPassword
	 * @param maxUses maximum number of times the token can be used.
	 * @return
	 * @throws AuthenticationException
	 */
	public AuthenticationToken getToken(String apiUsername, String apiPassword, Integer maxUses)
	throws AuthenticationException
	{
		if (isTokenValid(token)) {
			return token;
		} else {
			this.token = getToken(apiUsername, apiPassword, null, null, maxUses);
			return token;
		}
	}
	
	/**
	 * Retrieves a token good for the given number of seconds.
	 * 
	 * @param apiUsername
	 * @param apiPassword
	 * @param lifetime
	 * @return
	 * @throws AuthenticationException
	 */
	public AuthenticationToken getToken(String apiUsername, String apiPassword, Long lifetime)
	throws AuthenticationException
	{
		if (isTokenValid(token)) {
			return token;
		} else {
			this.token = getToken(apiUsername, apiPassword, null, lifetime, null);
			return token;
		}
	}
	
	/**
	 * Retrieves a token good for 2 hours with the given internalUsername attached to it. This
	 * can then be used to assign credentials to the user via the SystemsService.addCredentialToUser()
	 * method. To create users use the ProfileService.createUser() method.
	 * 
	 * @param apiUsername
	 * @param apiPassword
	 * @param internalUsername
	 * @return
	 * @throws AuthenticationException
	 */
	public AuthenticationToken getToken(String apiUsername, String apiPassword, 
			String internalUsername)
	throws AuthenticationException
	{
		if (isTokenValid(token)) {
			return token;
		} else {
			this.token = getToken(apiUsername, apiPassword, internalUsername, null, null);
			return token;
		}
	}
	
	/**
	 * Retrieves a token with the given internalUsername attached to it that is good 
	 * either maxUses or lifetime seconds, whichever comes first. This can then be 
	 * used to assign credentials to the user via the SystemsService.addCredentialToUser()
	 * method. To create users use the ProfileService.createUser() method.
	 * 
	 * @param apiUsername
	 * @param apiPassword
	 * @param internalUsername
	 * @param lifetime
	 * @param maxUses
	 * @return
	 * @throws AuthenticationException
	 */
	public AuthenticationToken getToken(String apiUsername, String apiPassword, 
			String internalUsername, Long lifetime, Integer maxUses) 
	throws AuthenticationException
	{
		if (isEmpty(apiUsername)) {
			throw new AuthenticationException("No api username provided.");
		}
		
		if (isEmpty(apiPassword)) {
			throw new AuthenticationException("No api username provided.");
		}
		
		List <NameValuePair> nvps = new ArrayList<NameValuePair>();
		
		if (!isEmpty(apiUsername)) {
			nvps.add(new BasicNameValuePair("internal_username", internalUsername));
		}
		
		if (lifetime != null && lifetime.longValue() > 0) {
			nvps.add(new BasicNameValuePair("lifetime", lifetime.toString()));
		}
		
		if (maxUses != null && maxUses.intValue() > 0) {
			nvps.add(new BasicNameValuePair("max_uses", maxUses.toString()));
		}
		AuthenticationToken token = new AuthenticationToken();
		token.setUsername(apiUsername);
		token.setToken(apiPassword);
		
		APIResponse response = null;
		try 
		{
			response = post( Settings.SERVICE_BASE_URL + "auth-v1/", token, nvps );
		} 
		catch (APIClientException e) {
			throw new AuthenticationException("Error authenticating to service.", e);
		}
		
		if (response.isSuccess()) {
			return parseToken(response.getResult());
		} else {
			throw new AuthenticationException(response.getMessage());
		}
	}
	
	/**
	 * Parses the repsonse json from the auth service into an AuthenticationToken object.
	 * @param response JsonNode object containing the service result attribute value.
	 * @return AuthenticationToken
	 */
	private AuthenticationToken parseToken(JsonNode response)
	{
		if (response == null) {
			return null;
		} 
		else 
		{
			AuthenticationToken token = new AuthenticationToken();
			token.setUsername(response.get("username").asText());
			token.setToken(response.get("token").asText());
			if (response.has("internal_username")) {
				token.setInternalUsername(response.get("internal_username").asText());
			}
			token.setLastRenewal(new Date(response.get("renewed").asLong()));
			token.setCreated(new Date(response.get("created").asLong()));
			token.setExpirationDate(new Date(response.get("expires").asLong()));
			if (response.get("remaining_uses").isNumber()) {
				token.setRemainingUses(response.get("remaining_uses").intValue());
			} else {
				token.setRemainingUses(-1);
			}
			return token;
		}
	}
	
	/**
	 * Checks the provided token for validity by calling the auth service.
	 * 
	 * @param token AuthenticationToken to verify
	 * @return true if the AuthenticationToken is valid. false otherwise.
	 * @throws APIClientException
	 * @throws AuthenticationException
	 */
	public boolean verify(AuthenticationToken token) 
	throws APIClientException, AuthenticationException
	{
		if (token == null) {
			throw new APIClientException("No token provided.");
		}
		
		APIResponse response = null;
		try 
		{
			response = get( Settings.SERVICE_BASE_URL + "auth-v1/", token );
			return response.isSuccess();
		} 
		catch (APIClientException e) {
			throw new AuthenticationException("Error authenticating to service.", e);
		}
	}
	
	/**
	 * Expires the given token immediately from use throughout the API.
	 * 
	 * @param token AuthenticationToken to verify
	 * @return true if the AuthenticationToken is valid. false otherwise.
	 * @throws APIClientException
	 * @throws AuthenticationException
	 */
	public void delete(AuthenticationToken token) 
	throws APIClientException, AuthenticationException
	{
		if (token == null) {
			throw new APIClientException("No token provided.");
		}
		
		try 
		{
			delete( Settings.SERVICE_BASE_URL + "auth-v1/" + token.getToken(), token );
		} 
		catch (APIClientException e) {
			throw new AuthenticationException("Error authenticating to service.", e);
		}
	}

	/**
	 * Renews the given token for another 2 hours.
	 * 
	 * @param token token to refresh
	 * @param username api username
	 * @param password api password. Note that tokens can be used to refresh themselves.
	 * 
	 * @return renewed AuthenticationToken
	 * @throws APIClientException
	 * @throws AuthenticationException
	 */
	public AuthenticationToken renew(AuthenticationToken token, String username, String password) 
	throws APIClientException, AuthenticationException
	{
		if (StringUtils.isEmpty(username)) {
			throw new APIClientException("No username provided.");
		}
		
		if (StringUtils.isEmpty(password)) {
			throw new APIClientException("No password provided.");
		}
		
		if (token == null) {
			throw new APIClientException("No token provided.");
		}
		
		APIResponse response = null;
		try 
		{
			List <NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair("token", token.getToken()));
			
			token.setUsername(username);
			token.setToken(password);
			
			response = post( Settings.SERVICE_BASE_URL + "auth-v1/renew.php", token, nvps );
		} 
		catch (APIClientException e) {
			throw new AuthenticationException("Error authenticating to service.", e);
		}
		
		if (response.isSuccess()) {
			return parseToken(response.getResult());
		} else {
			throw new AuthenticationException(response.getMessage());
		}
		
	}
}
