package org.iplantc.service.profile.manager;

import java.util.Date;
import java.util.List;

import org.iplantc.service.notification.managers.NotificationManager;
import org.iplantc.service.profile.dao.InternalUserDao;
import org.iplantc.service.profile.exceptions.InternalUsernameConflictException;
import org.iplantc.service.profile.exceptions.ProfileArgumentException;
import org.iplantc.service.profile.exceptions.ProfileException;
import org.iplantc.service.profile.model.InternalUser;
import org.json.JSONObject;

/**
 * Provides the logic in front of the dao class to manage internal users.
 *
 * @author dooley
 *
 */
public class InternalUserManager {

	private InternalUserDao dao;
	
	public InternalUserManager()
	{
		dao = new InternalUserDao();
	}
	
	public List<InternalUser> getActiveInternalUsers(String apiUsername) 
	throws ProfileException 
	{
		return dao.getActiveInternalUsersCreatedByAPIUser(apiUsername);
	}
	
	public List<InternalUser> getAllInternalUsers(String apiUsername) 
	throws ProfileException 
	{
		return dao.getAllInternalUsersCreatedByAPIUser(apiUsername);
	}
	
	/**
	 * Creates a new InternalUser and assigns it to the current API user.
	 * @param jsonInternalUser
	 * @param apiUsername
	 * @return
	 * @throws ProfileException
	 */
	public InternalUser addInternalUser(JSONObject jsonInternalUser, String apiUsername) 
	throws ProfileException, InternalUsernameConflictException {
		try 
		{
			InternalUser internalUser = InternalUser.fromJSON(jsonInternalUser);
			internalUser.setCreatedBy(apiUsername);
			
			if (dao.isUsernameAvailable(apiUsername, internalUser.getUsername())) 
			{
				dao.persist(internalUser);
				
				NotificationManager.process(internalUser.getUuid(), "CREATED", apiUsername);
			} 
			else 
			{
				throw new InternalUsernameConflictException("Username '" + internalUser.getUsername() + "' " +
						"is not available. Please select another username.");
			}
			
			return internalUser;
		} 
		catch(InternalUsernameConflictException e) {
			throw e;
		}
		catch(ProfileException e) {
			throw e;
		}
		catch(ProfileArgumentException e) {
			throw new ProfileException(e.getMessage(), e);
		}
		catch(Exception e) {
			throw new ProfileException("Failed to create new internal user.", e);
		}
	}
	
	/**
	 * Performs a simple update on the currently defined internal user with the
	 * information from the json representation of this InternalUser.
	 * 
	 * @param jsonInternalUser
	 * @param currentUser
	 * @return
	 * @throws ProfileException
	 */
	public InternalUser updateInternalUser(JSONObject jsonInternalUser, InternalUser currentInternalUser) 
	throws ProfileException {
		try 
		{
			currentInternalUser = InternalUser.fromJSON(jsonInternalUser, currentInternalUser);
			currentInternalUser.setLastUpdated(new Date());
			
			dao.persist(currentInternalUser);
			
			NotificationManager.process(currentInternalUser.getUuid(), "UPDATED", currentInternalUser.getCreatedBy());
			
			return currentInternalUser;
		} 
		catch(ProfileArgumentException e) {
			throw new ProfileException(e.getMessage(), e);
		}
		catch(Exception e) {
			throw new ProfileException("Failed to create new internal user.", e);
		}
	}
	
	/**
	 * This removes the internal user from the API user's directory of internal users,
	 * cancels any running jobs, and deletes their credentials on all the API user's
	 * systems.
	 * 
	 * @param json
	 * @param currentUser
	 * @return
	 * @throws ProfileException, RemoteDataException
	 */
	public void deleteInternalUser(InternalUser currentUser) 
	throws ProfileException 
	{
		try 
		{
			currentUser.setActive(false);
			currentUser.setLastUpdated(new Date());
			
			dao.persist(currentUser);
			
			NotificationManager.process(currentUser.getUuid(), "DELETED", currentUser.getCreatedBy());
			
		} 
		catch(Exception e) {
			throw new ProfileException("Failed to delete internal user.", e);
		}
	}

	/**
	 * Looks up an InternalUser by username for the current API user.
	 *  
	 * @param internalUserUsername
	 * @param apiUsername
	 * @return InternalUser
	 * @throws ProfileException
	 */
	public InternalUser getInternalUser(String internalUserUsername, String apiUsername)
	throws ProfileException
	{
		return dao.getInternalUserByAPIUserAndUsername(apiUsername, internalUserUsername);
	}

}
