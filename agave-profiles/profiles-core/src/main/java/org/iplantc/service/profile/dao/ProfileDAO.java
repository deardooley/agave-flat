package org.iplantc.service.profile.dao;

import java.util.List;

import org.iplantc.service.profile.exceptions.RemoteDataException;
import org.iplantc.service.profile.model.Profile;

public interface ProfileDAO {

	public abstract Profile getByUsername(String username) throws RemoteDataException;

	public abstract List<Profile> searchByUsername(String username) throws RemoteDataException;

	public abstract List<Profile> searchByEmail(String email) throws RemoteDataException;

	public abstract List<Profile> searchByFullName(String name) throws RemoteDataException;	
	
	public abstract List<Profile> searchByType(String type, String value) throws RemoteDataException;

}