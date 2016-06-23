package org.iplantc.service.profile.dao;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.iplantc.service.common.clients.HTTPSClient;
import org.iplantc.service.profile.Settings;
import org.iplantc.service.profile.exceptions.RemoteDataException;
import org.iplantc.service.profile.model.Profile;
import org.iplantc.service.profile.model.TrellisProfile;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class TrellisProfileDAO extends AbstractProfileDAO {

	public TrellisProfileDAO() {}
	
	private static final String URL_ENCODING_FORMAT = "utf-8";
	@Override
	public Profile getByUsername(String username)
	throws RemoteDataException 
	{
		List<Profile> profiles = fetchResults(Settings.QUERY_URL + "username/" + username);
		return profiles.get(0);
	}
	
	@Override
	public List<Profile> searchByEmail(String email)
	throws RemoteDataException 
	{
		if (email == null) {
			email = "";
		} else {
			try
			{
				email = URLEncoder.encode(email, URL_ENCODING_FORMAT);
			}
			catch (UnsupportedEncodingException e) {}
		}
		
		return fetchResults(Settings.QUERY_URL + "email/" + email);
	}

	@Override
	public List<Profile> searchByFullName(String name)
	throws RemoteDataException 
	{
		if (name == null) {
			name = "";
		} else {
			try
			{
				name = URLEncoder.encode(name, URL_ENCODING_FORMAT);
			}
			catch (UnsupportedEncodingException e) {}
		}
		return fetchResults(Settings.QUERY_URL + "name/" + name);
	}

	@Override
	public List<Profile> searchByUsername(String username)
	throws RemoteDataException 
	{
		if (username == null) {
			username = "";
		} else {
			try
			{
				username = URLEncoder.encode(username, URL_ENCODING_FORMAT);
			}
			catch (UnsupportedEncodingException e) {}
		}
		return fetchResults(Settings.QUERY_URL + "username/" + username);
	}
	
	private List<Profile> fetchResults(String trellisUrl) throws RemoteDataException 
	{
		List<Profile> profiles = new ArrayList<Profile>();
		
		HTTPSClient client = null;
		try {
			
			client = new HTTPSClient(trellisUrl, 
					Settings.QUERY_URL_USERNAME, 
					Settings.QUERY_URL_PASSWORD, 
					new HashMap<String,String>());
			
			// execute method and handle any error responses.
			
			String response = client.getText();
			//String response = "{\"users\":[{\"id\":\"6102\",\"username\":\"jnvaughn\",\"firstname\":\"Justin\",\"lastname\":\"Vaughn\",\"email\":\"jnvaughn@uga.edu\",\"position\":null,\"institution\":\"University of Georgia\"},{\"id\":\"3819\",\"username\":\"vaughn\",\"firstname\":\"Matthew\",\"lastname\":\"Vaughn\",\"email\":\"vaughn@tacc.utexas.edu\",\"position\":null,\"institution\":\"University of Texas Austin\"}]}";
			JSONObject jsonObject = new JSONObject(response);
			
			if (jsonObject.has("users")) 
			{
				JSONArray json = jsonObject.getJSONArray("users");
				
				for (int i=0; i< json.length(); i++)
				{
					profiles.add(new TrellisProfile(json.getJSONObject(i)));
				}
			}
			
			return profiles;
		} 
		catch (JSONException e) {
			throw new RemoteDataException("Failed to parse profile data", e);
		} 
		catch (Exception e) {
			throw new RemoteDataException("Failed to retrieve profile data", e);
		}
	}

}
