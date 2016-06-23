package org.iplantc.service.profile.dao;

import java.util.ArrayList;
import java.util.List;

import org.iplantc.service.profile.exceptions.RemoteDataException;
import org.iplantc.service.profile.model.Profile;

public abstract class AbstractProfileDAO implements ProfileDAO {

	public List<Profile> searchByType(String type, String value)
	throws RemoteDataException 
	{
		if (type.equalsIgnoreCase("name")) {
			return searchByFullName(value);
		} else if (type.equalsIgnoreCase("email")) {
			return searchByEmail(value);
		} else if (type.equalsIgnoreCase("username")) {
			return searchByUsername(value);
		} else {
			return new ArrayList<Profile>();
		}
	}

}
