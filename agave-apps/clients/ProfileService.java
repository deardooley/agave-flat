/**
 * 
 */
package org.iplantc.service.clients;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.iplantc.service.clients.exceptions.APIClientException;
import org.iplantc.service.clients.exceptions.AuthenticationException;
import org.iplantc.service.clients.exceptions.ProfileException;
import org.iplantc.service.clients.model.AuthenticationToken;
import org.iplantc.service.clients.model.InternalUser;
import org.iplantc.service.clients.model.Profile;
import org.iplantc.service.clients.util.ClientUtils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Client class for interacting with the Profile Service. All methods
 * require authentication using the token from the constructor.
 * 
 * 
 * @author dooley
 *
 */
public class ProfileService extends AbstractService {
	
	private AuthenticationToken token = null;

	public ProfileService(AuthenticationToken token)
	throws AuthenticationException
	{
		this.setToken(token);
	}
	
	/**
	 * @return the token
	 */
	public AuthenticationToken getToken()
	{
		return token;
	}

	/**
	 * @param token the token to set
	 */
	public void setToken(AuthenticationToken token)
	{
		this.token = token;
	}

	/**
	 * Returns a list of API users whose username matches the given
	 * string in part. This will not return users you have created. 
	 * Use the {@link #searchInternalByUsername()} method to query for
	 * InternalUser.
	 *  
	 * @param username
	 * @return list of Profile objects
	 * @throws AuthenticationException
	 * @throws ProfileException
	 */
	public List<Profile> searchByUsername(String username)
	throws AuthenticationException, ProfileException
	{
		return _doSearch("username", username);
	}
	
	/**
	 * Returns a list of API users whose email matches the given
	 * string in part. This will not return users you have created. 
	 * Use the {@link #searchInternalByEmail()} method to query for
	 * InternalUser.
	 *  
	 * @param email
	 * @return list of Profile objects
	 * @throws AuthenticationException
	 * @throws ProfileException
	 */
	public List<Profile> searchByEmail(String email)
	throws AuthenticationException, ProfileException
	{
		return _doSearch("email", email);
	}
	
	/**
	 * Returns a list of API users whose name matches the given
	 * string in part. This will not return users you have created. 
	 * Use the {@link #searchInternalByName()} method to query for
	 * InternalUser.
	 *  
	 * @param name
	 * @return list of Profile objects
	 * @throws AuthenticationException
	 * @throws ProfileException
	 */
	public List<Profile> searchByName(String name)
	throws AuthenticationException, ProfileException
	{
		return _doSearch("name", name);
	}
	
	/**
	 * Calls the profile service to search for Profile by term and value.
	 * 
	 * @param term one of "username","email","name"
	 * @param value
	 * @return List of Profile
	 * @throws AuthenticationException
	 * @throws ProfileException
	 */
	private List<Profile> _doSearch(String term, String value)
	throws AuthenticationException, ProfileException
	{
		if (isEmpty(term)) {
			throw new ProfileException("No term provided.");
		} else if ( !Arrays.asList("username","email","name").contains(term.toLowerCase())) {
			throw new ProfileException("Invalid search term provided. Please search by 'username', 'email', or 'name'");
		}
		
		if (isEmpty(value)) {
			throw new ProfileException("No search value provided.");
		}
		
		if (isTokenValid(token)) 
		{
			throw new AuthenticationException("Token is expired");
		} 
		else 
		{
			APIResponse response = null;
			try 
			{
				response = get( Settings.SERVICE_BASE_URL + "profile-v1/profile/search/" + term + "/" + value, token );
			} 
			catch (APIClientException e) {
				throw new ProfileException(e);
			}
			
			if (response.isSuccess()) {
				return parseProfiles(response.getResult());
			} else {
				throw new ProfileException(response.getMessage());
			}
		}
	}
	
	
	/**
	 * Returns a list of InternalUsers whose username matches the given
	 * string in part. 
	 * @see {@link org.iplantc.service.clients.model.InternalUser}
	 * 
	 * @param username
	 * @return list of Profile objects
	 * @throws AuthenticationException
	 * @throws ProfileException
	 */
	public List<InternalUser> searchInternalUsersByUsername(String username)
	throws AuthenticationException, ProfileException
	{
		return _doInternalUserSearch("username", username);
	}
	
	/**
	 * Returns a list of InternalUsers whose email matches the given
	 * string in part. 
	 * @see {@link org.iplantc.service.clients.model.InternalUser}
	 * 
	 * @param email
	 * @return list of Profile objects
	 * @throws AuthenticationException
	 * @throws ProfileException
	 */
	public List<InternalUser> searchInternalUsersByEmail(String email)
	throws AuthenticationException, ProfileException
	{
		return _doInternalUserSearch("email", email);
	}
	
	/**
	 * Returns a list of InternalUsers whose name matches the given
	 * string in part.
	 * @see {@link org.iplantc.service.clients.model.InternalUser}
	 *  
	 * @param name
	 * @return list of Profile objects
	 * @throws AuthenticationException
	 * @throws ProfileException
	 */
	public List<InternalUser> searchInternalUsersByName(String name)
	throws AuthenticationException, ProfileException
	{
		return _doInternalUserSearch("name", name);
	}
	
	/**
	 * Calls the profile service to search for InternalUser by term and value.
	 * 
	 * @param term one of "username","email","name"
	 * @param value
	 * @return List of InternalUser
	 * @throws AuthenticationException
	 * @throws ProfileException
	 */
	private List<InternalUser> _doInternalUserSearch(String term, String value)
	throws AuthenticationException, ProfileException
	{
		if (isEmpty(term)) {
			throw new ProfileException("No term provided.");
		} else if ( !Arrays.asList("username","email","name").contains(term.toLowerCase())) {
			throw new ProfileException("Invalid search term provided. Please search by 'username', 'email', or 'name'");
		}
		
		if (isEmpty(value)) {
			throw new ProfileException("No search value provided.");
		}
		
		if (isTokenValid(token)) 
		{
			throw new AuthenticationException("Token is expired");
		} 
		else 
		{
			APIResponse response = null;
			try 
			{
				response = get( Settings.SERVICE_BASE_URL + "profile-v1/internal/search/" + term + "/" + value, token );
			} 
			catch (APIClientException e) {
				throw new ProfileException(e);
			}
			
			if (response.isSuccess()) {
				return parseInternalUsers(response.getResult());
			} else {
				throw new ProfileException(response.getMessage());
			}
		}
	}
	
	/**
	 * Creates an InternalUser bound to the token user's API credentials. Users
	 * created via this method will not be users in terms of the API and cannot
	 * authenticate against the API themself, but they can be attached to an
	 * AuthenticationToken and have an AuthConfig on a RemoteSystem attached to
	 * them, thus allowing for 3rd party user authentication and accounting from
	 * a single set of API credentials. 
	 * 
	 * @param user
	 * @return
	 * @throws AuthenticationException
	 * @throws ProfileException
	 */
	public InternalUser createInternalUser(InternalUser user) 
	throws AuthenticationException, ProfileException
	{
		if (user == null) {
			throw new ProfileException("No internal user profile provided.");
		} else if (StringUtils.isEmpty(user.getUsername())) {
			throw new ProfileException("Unique username must be provided for internal users.");
		} else if (!ClientUtils.isValidEmailAddress(user.getEmail())) {
			throw new ProfileException("Unique username must be provided for internal users.");
		} 
		
		APIResponse response = null;
		try 
		{
			List <NameValuePair> nvps = new ArrayList<NameValuePair>();
			ObjectMapper mapper = new ObjectMapper();
			JsonFactory factory = mapper.getFactory();
			JsonParser jp = factory.createJsonParser(mapper.writeValueAsString(user));
			JsonNode node = mapper.readTree(jp);
			
			Iterator<String> fieldNames = node.fieldNames();
			while(fieldNames.hasNext()) {
				String fieldName = fieldNames.next();
				nvps.add(new BasicNameValuePair(fieldName, node.get(fieldName).asText()));
			}
			
			response = post( Settings.SERVICE_BASE_URL + "profile-v1/internal", token, nvps );
		} 
		catch (APIClientException e) {
			throw new ProfileException("Error querying service.", e);
		}
		catch (JsonParseException e)
		{
			throw new ProfileException("Failed to create new user from the given internal user.", e);
		}
		catch (JsonProcessingException e)
		{
			throw new ProfileException("Failed to create new user from the given internal user.", e);
		}
		catch (IOException e)
		{
			throw new ProfileException("Failed to create new user from the given internal user.", e);
		}
		
		if (response.isSuccess()) {
			List<InternalUser> users = parseInternalUsers(response.getResult());
			return users.get(0);
		} else {
			throw new ProfileException(response.getMessage());
		}
	}
	
	/**
	 * Updates an internal user. Note that usernames cannot be changed. 
	 * 
	 * @param user
	 * @return
	 * @throws AuthenticationException
	 * @throws ProfileException
	 */
	public InternalUser updateInternalUser(InternalUser user) 
	throws AuthenticationException, ProfileException
	{
		if (user == null) {
			throw new ProfileException("No internal user profile provided.");
		} else if (StringUtils.isEmpty(user.getUsername())) {
			throw new ProfileException("Unique username must be provided for internal users.");
		} else if (!ClientUtils.isValidEmailAddress(user.getEmail())) {
			throw new ProfileException("Unique username must be provided for internal users.");
		} 
		
		APIResponse response = null;
		try 
		{
			List <NameValuePair> nvps = new ArrayList<NameValuePair>();
			ObjectMapper mapper = new ObjectMapper();
			JsonFactory factory = mapper.getFactory();
			JsonParser jp = factory.createJsonParser(mapper.writeValueAsString(user));
			JsonNode node = mapper.readTree(jp);
			
			Iterator<String> fieldNames = node.fieldNames();
			while(fieldNames.hasNext()) {
				String fieldName = fieldNames.next();
				nvps.add(new BasicNameValuePair(fieldName, node.get(fieldName).asText()));
			}
			
			String endpoint = Settings.SERVICE_BASE_URL + "profile-v1/internal/" + user.getUsername();
			response = post( endpoint , token, nvps );
		} 
		catch (APIClientException e) {
			throw new ProfileException("Error querying service.", e);
		}
		catch (JsonParseException e)
		{
			throw new ProfileException("Failed to create new user from the given internal user.", e);
		}
		catch (JsonProcessingException e)
		{
			throw new ProfileException("Failed to create new user from the given internal user.", e);
		}
		catch (IOException e)
		{
			throw new ProfileException("Failed to create new user from the given internal user.", e);
		}
		
		if (response.isSuccess()) {
			List<InternalUser> users = parseInternalUsers(response.getResult());
			return users.get(0);
		} else {
			throw new ProfileException(response.getMessage());
		}
	}
	
	/**
	 * Deletes an internal user. Note that all user AuthCredentials on individual
	 * systems will be deleted and any jobs or data transfers currently underway
	 * will more than likely fail.
	 * 
	 * @param user InternalUser
	 * @return
	 * @throws AuthenticationException
	 * @throws ProfileException
	 */
	public void deleteInternalUser(String internalUsername) 
	throws AuthenticationException, ProfileException
	{
		if (StringUtils.isEmpty(internalUsername)) {
			throw new ProfileException("No internal username provided.");
		} 
		
		APIResponse response = null;
		try 
		{
			String endpoint = Settings.SERVICE_BASE_URL + "profile-v1/internal/" + internalUsername;
			response = delete( endpoint, token );
		} 
		catch (APIClientException e) {
			throw new ProfileException("Error querying service.", e);
		}
		
		if (!response.isSuccess()) {
			throw new ProfileException(response.getMessage());
		}
	}
	
	/**
	 * Parses the repsonse json from the profile service into a list of Profile objects.
	 * 
	 * @param response JsonNode object containing the service result attribute value.
	 * @return List of Profile objects
	 * @throws ProfileException 
	 */
	private List<Profile> parseProfiles(JsonNode response) throws ProfileException
	{
		List<Profile> profiles = new ArrayList<Profile>();
		
		if (response == null) {
			return profiles;
		} 
		else 
		{
			
			if (response.isArray()) {
				for(int i=0; i<response.size(); i++) {
					JsonNode jsonProfile = response.get(i);
					profiles.add(Profile.fromJSON(jsonProfile));
				}
			} else {
				profiles.add(Profile.fromJSON(response));
			}
			
			return profiles;
		}
	}
	
	/**
	 * Parses the repsonse json from the profile service into a list of InternalUser objects.
	 * 
	 * @param response JsonNode object containing the service result attribute value.
	 * @return List of Profile objects
	 * @throws ProfileException 
	 */
	private List<InternalUser> parseInternalUsers(JsonNode response) throws ProfileException
	{
		List<InternalUser> profiles = new ArrayList<InternalUser>();
		
		if (response == null) {
			return profiles;
		} 
		else 
		{
			
			if (response.isArray()) {
				for(int i=0; i<response.size(); i++) {
					JsonNode jsonProfile = response.get(i);
					profiles.add(InternalUser.fromJSON(jsonProfile));
				}
			} else {
				profiles.add(InternalUser.fromJSON(response));
			}
			
			return profiles;
		}
	}
	
	
	/**
	 * Expires the given token immediately from use throughout the API.
	 * 
	 * @param token AuthenticationToken to verify
	 * @return true if the AuthenticationToken is valid. false otherwise.
	 * @throws APIClientException
	 * @throws ProfileException
	 */
	public void delete(AuthenticationToken token) 
	throws APIClientException, ProfileException
	{
		if (token == null) {
			throw new APIClientException("No token provided.");
		}
		
		try 
		{
			delete( Settings.SERVICE_BASE_URL + "auth-v1/" + token.getToken(), token );
		} 
		catch (APIClientException e) {
			throw new ProfileException("Error querying service.", e);
		}
	}
}
