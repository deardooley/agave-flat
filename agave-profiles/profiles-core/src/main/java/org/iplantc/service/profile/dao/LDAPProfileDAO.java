package org.iplantc.service.profile.dao;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.naming.directory.Attributes;

import org.iplantc.service.common.auth.LDAPClient;
import org.iplantc.service.profile.Settings;
import org.iplantc.service.profile.exceptions.RemoteDataException;
import org.iplantc.service.profile.model.LdapProfile;
import org.iplantc.service.profile.model.Profile;
import org.iplantc.service.profile.util.ServiceUtils;

public class LDAPProfileDAO extends AbstractProfileDAO {

	private LDAPClient client = null;
	
	public LDAPProfileDAO() 
	{
		this.client = new LDAPClient(Settings.IPLANT_LDAP_USERNAME, Settings.IPLANT_LDAP_PASSWORD);
	}
	
	@Override
	public List<Profile> searchByType(String type, String value)
	throws RemoteDataException 
	{
		List<Profile> profiles = new ArrayList<Profile>();
		
		type = convertTypeToAttribute(type);
		if (type == null) {
			throw new RemoteDataException("Invalid search type. Valid search types are: \"name, email, and username\"");
		}
		
		List<Attributes> list = client.searchDirectory(type, value);
		
		try {
			if (list != null) {
				for (Iterator<Attributes> iterator = list.iterator(); iterator
						.hasNext();) {
					Attributes attrs = (Attributes) iterator.next();
					Profile profile = new LdapProfile(attrs);
					profiles.add(profile);
				}
			}
		} catch (Exception e) {
			throw new RemoteDataException("Failed to retrieve user query from LDAP", e);
		}
		
		return profiles;
	}
	
	@Override
	public Profile getByUsername(String username) throws RemoteDataException
	{
		if (!ServiceUtils.isValid(username)) {
			throw new RemoteDataException("No username specified.");
		}
		try {
			
			Attributes attrs = client.getAttributes(username);
				
			/*
			 * No data found
			 */
			if (attrs == null) {
				return null;
			}		
			
			Profile profile = new LdapProfile(attrs);
			
			return profile;
		
		} catch (Exception e) {
			throw new RemoteDataException("Failed to retrieve user " + username + " from LDAP", e);
		}
	}
	
	@Override
	public List<Profile> searchByEmail(String email) throws RemoteDataException
	{
		return searchByType("email", email);
	}

	@Override
	public List<Profile> searchByFullName(String name) throws RemoteDataException
	{
		return searchByType("name", name);
	}

	@Override
	public List<Profile> searchByUsername(String username) throws RemoteDataException
	{
		return searchByType("username", username);
	}

	private String convertTypeToAttribute(String type) {
		if (type.equalsIgnoreCase("name")) {
			return "cn";
		} else if (type.equalsIgnoreCase("email")) {
			return "mail";
		} else if (type.equalsIgnoreCase("username")) {
			return "uid";
		}
		return null;
	}
}
