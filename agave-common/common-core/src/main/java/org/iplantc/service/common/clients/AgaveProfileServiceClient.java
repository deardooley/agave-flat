package org.iplantc.service.common.clients;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.auth.JWTClient;
import org.iplantc.service.common.clients.beans.Profile;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.persistence.TenancyHelper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This is used to lookup users in the API. Previosuly the profile service pointed
 * to trellis within iplant. Now that tenant idp are hooked up to the AM, we need to
 * query that for user lookup and we can use the profile service for more detailed
 * info if need be.
 *  
 * @author dooley
 *
 */
public class AgaveProfileServiceClient 
{
	private static Logger log = Logger.getLogger(AgaveProfileServiceClient.class);
	
	private String			username;
	private String			passwd;
	private String			endpoint;

	public AgaveProfileServiceClient(String endpoint, String username,
			String pass)
	{
		this.username = username;
		this.passwd = pass;
		this.endpoint = endpoint;
	}
	
	public Profile getUser(String user, String tenantId) throws Exception
	{
		
		if (StringUtils.isEmpty(user)) {
			throw new IOException("No user specified");
		}
		
		try
		{
			Map<String, String> headers = new HashMap<String, String>();
			headers.put(JWTClient.getJwtHeaderKeyForTenant(tenantId), JWTClient.createJwtForTenantUser(user, tenantId, false));
			HTTPSClient client = new HTTPSClient(endpoint + "profiles/me" + user, headers);
			
			String response = client.getText();
			
			ObjectMapper mapper = new ObjectMapper();
			
			if (response != null)
			{
				JsonNode json = mapper.readTree(response);
				if (json.has("result") && json.get("result").isArray()) {
					if (json.get("result").size() > 0) {
						JsonNode jsonProfile = json.get("result").get(0);
						Profile bean = new Profile();
						bean.setUsername(jsonProfile.get("username").asText());
						bean.setFirstName(jsonProfile.get("firstName").asText());
						bean.setLastName(jsonProfile.get("lastName").asText());
						bean.setEmail(jsonProfile.get("email").asText());
						bean.setBusPhoneNumber(jsonProfile.get("phone").asText());
						bean.setFaxNumber(jsonProfile.get("fax").asText());
						bean.setOrganization(jsonProfile.get("company").asText());
						bean.setDepartment(jsonProfile.get("department").asText());
						bean.setPosition(jsonProfile.get("position").asText());
						return bean;
					}
				}
				
			}
			//return null;
			return new Profile();
		}
		catch (FileNotFoundException e) {
			return null;
		}
		catch (IOException ioe)
		{
			// Unauthorized access
			throw new PermissionException("Access not authorized by the server, "
					+ "check your credentials", ioe);
		}
		catch (Exception e)
		{
			throw new PermissionException("Failed to retrieve output from "
					+ endpoint, e);
		}
	}

	public Profile getUser(String user) throws Exception
	{
		if (true) {
			Profile profile = new Profile();
			profile.setUsername(user);
			return profile;
//			return getUser(user, TenancyHelper.getCurrentTenantId());
		}
		else
		{
			if (StringUtils.isEmpty(user)) {
				throw new IOException("No user specified");
			}
			
			try
			{
				HTTPSClient client = new HTTPSClient(endpoint + "profile/search/username/" + user, username, passwd);
				String response = client.getText();
				
				ObjectMapper mapper = new ObjectMapper();
				
				if (response != null)
				{
					JsonNode json = mapper.readTree(response);
					if (json.has("result") && json.get("result").isArray()) {
						if (json.get("result").size() > 0) {
							JsonNode jsonProfile = json.get("result").get(0);
							Profile bean = new Profile();
							bean.setUsername(jsonProfile.get("username").asText());
							bean.setFirstName(jsonProfile.get("firstName").asText());
							bean.setLastName(jsonProfile.get("lastName").asText());
							bean.setEmail(jsonProfile.get("email").asText());
							bean.setBusPhoneNumber(jsonProfile.get("phone").asText());
							bean.setFaxNumber(jsonProfile.get("fax").asText());
							bean.setOrganization(jsonProfile.get("company").asText());
							bean.setDepartment(jsonProfile.get("department").asText());
							bean.setPosition(jsonProfile.get("position").asText());
							return bean;
						}
					}
					
				}
				//return null;
				return new Profile();
			}
			catch (FileNotFoundException e) {
				return null;
			}
			catch (IOException ioe)
			{
				// Unauthorized access
				throw new PermissionException("Access not authorized by the server, "
						+ "check your credentials", ioe);
			}
			catch (Exception e)
			{
				throw new PermissionException("Failed to retrieve output from "
						+ endpoint, e);
			}
		}
	}
	
	public List<Profile> getUsers() throws Exception
	{
		List<Profile> profiles = new ArrayList<Profile>();
		
		try
		{
			HTTPSClient client = new HTTPSClient(endpoint, username, passwd);
			String response = client.getText();
			ObjectMapper mapper = new ObjectMapper();
			
			if (response != null)
			{
				JsonNode json = mapper.readTree(response);
				
				if (json.has("result") && json.get("result").isArray())
				{
					JsonNode jsonProfiles = json.get("result");
					for(int i=0; i<jsonProfiles.size(); i++)
					{
						JsonNode jsonProfile = jsonProfiles.get(i);
						Profile bean = new Profile();
						bean.setUsername(jsonProfile.get("username").asText());
						bean.setFirstName(jsonProfile.get("firstName").asText());
						bean.setLastName(jsonProfile.get("lastName").asText());
						bean.setEmail(jsonProfile.get("email").asText());
						bean.setBusPhoneNumber(jsonProfile.get("phone").asText());
						bean.setFaxNumber(jsonProfile.get("fax").asText());
						bean.setOrganization(jsonProfile.get("company").asText());
						bean.setDepartment(jsonProfile.get("department").asText());
						bean.setPosition(jsonProfile.get("position").asText());
						profiles.add(bean);
					}
				}
				
			}
			return profiles;
		}
		catch (FileNotFoundException e) {
			return profiles;
		}
		catch (IOException ioe)
		{
			// Unauthorized access
			throw new PermissionException("Access not authorized by the server, "
					+ "check your credentials", ioe);
		}
		catch (Exception e)
		{
			throw new PermissionException("Failed to retrieve output from "
					+ endpoint, e);
		}
	}

	@SuppressWarnings("unused")
	private Date parseDate(String date)
	{
		Date fdate = null;

		try
		{
			fdate = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'")
					.parse(date);
		}
		catch (ParseException e)
		{
			log.error("Failed to parse date of TeraGrid import", e);
		}

		return fdate;
	}
}
