package org.iplantc.service.apps.managers;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.apps.Settings;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.dao.SoftwarePermissionDao;
import org.iplantc.service.apps.exceptions.SoftwareException;
import org.iplantc.service.apps.exceptions.SoftwarePermissionException;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwarePermission;
import org.iplantc.service.apps.model.enumerations.SoftwareRoleType;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.common.auth.AuthorizationHelper;
import org.iplantc.service.systems.manager.SystemRoleManager;
import org.iplantc.service.systems.model.SystemRole;
import org.iplantc.service.transfer.model.enumerations.PermissionType;

public class SoftwarePermissionManager {

	private Software	software;
	private SoftwareEventProcessor eventProcessor;

	public SoftwarePermissionManager(Software software) throws SoftwareException
	{
		if (software == null) { throw new SoftwareException("Software cannot be null"); }
		this.software = software;
		this.eventProcessor = new SoftwareEventProcessor();
	}

	public boolean hasPermission(String username,
			PermissionType softwarePermissionType) throws SoftwareException
	{

		if (StringUtils.isEmpty(username) || StringUtils.equals(username, "null")) { return false; }

		if (software.getOwner().equals(username) || ServiceUtils.isAdmin(username))
			return true;
		
		for (SoftwarePermission pem : software.getPermissions())
		{
			if (pem.getUsername().equals(username) || 
					pem.getUsername().equals(Settings.WORLD_USER_USERNAME)) { 
				return pem.getPermission().equals(softwarePermissionType); 
			}
		}

		return false;
	}

	public boolean canRead(String username) throws SoftwareException
	{

		if (StringUtils.isEmpty(username) || StringUtils.equals(username, "null")) { return false; }

		if (software.getOwner().equals(username) || 
				software.isPubliclyAvailable() ||
				ServiceUtils.isAdmin(username)) {
			return true;
		}
		
		for (SoftwarePermission pem : software.getPermissions())
		{
			if (pem.getUsername().equals(username) || 
					pem.getUsername().equals(Settings.WORLD_USER_USERNAME)) { 
				return pem.canRead(); 
			}
		}

		return false;
	}

	public boolean canWrite(String username) throws SoftwareException
	{

		if (StringUtils.isEmpty(username) || StringUtils.equals(username, "null")) { return false; }

		if (software.isPubliclyAvailable()) {
			return ServiceUtils.isAdmin(username);
		} else if (software.getOwner().equals(username) || ServiceUtils.isAdmin(username)) {
			return true;
		} else {
			for (SoftwarePermission pem : software.getPermissions())
			{
				if (pem.getUsername().equals(username) || 
						pem.getUsername().equals(Settings.WORLD_USER_USERNAME)) { 
					return pem.canWrite(); 
				}
			}
	
			return false;
		}	
	}
	
	public boolean canExecute(String username) throws SoftwareException
	{

		if (StringUtils.isEmpty(username) || StringUtils.equals(username, "null")) { return false; }

		if (software.getOwner().equals(username) || 
				software.isPubliclyAvailable() ||
				ServiceUtils.isAdmin(username)) {
			return true;
		}
		
		for (SoftwarePermission pem : software.getPermissions())
		{
			if (pem.getUsername().equals(username) || 
					pem.getUsername().equals(Settings.WORLD_USER_USERNAME)) { 
				return pem.canExecute(); 
			}
		}

		return false;
	}

	public void setPermission(String username, String sPermission)
	throws SoftwareException
	{
		if (StringUtils.isEmpty(username) || StringUtils.equals(username, "null")) {  
			throw new SoftwareException("Invalid username"); 
		}

		if (software.getOwner().equals(username))
			return;
		
		SoftwarePermission pem = SoftwarePermissionDao.getUserSoftwarePermissions(username, software.getId());
		
		if (StringUtils.isEmpty(sPermission) || 
				sPermission.equalsIgnoreCase("none") || 
				sPermission.equalsIgnoreCase("null")) {
			if (pem != null) {
				pem.setSoftware(null);
				software.getPermissions().remove(pem);
				SoftwareDao.persist(software);
				
				eventProcessor.processPermissionEvent(software, pem, username);
//				ApplicationManager.addEvent(software, 
//                        SoftwareEventType.PERMISSION_REVOKE, 
//                        sPermission +" permission was revoked for user " + username + " by " + TenancyHelper.getCurrentEndUser(), 
//                        username);
			}
		} else {
			PermissionType newPermissionType = PermissionType
					.valueOf(sPermission.toUpperCase());
			
			if (pem == null) {
				pem = new SoftwarePermission(software, username, newPermissionType);
			} else {
				//JobPermissionType resolvedPermissionType = pem.getPermission().add(newPermissionType);
				pem.setPermission(newPermissionType);
			}
			
			SoftwarePermissionDao.persist(pem);
			
			eventProcessor.processPermissionEvent(software, pem, username);
//			ApplicationManager.addEvent(software, 
//                    SoftwareEventType.PERMISSION_GRANT, 
//                    sPermission +" permission was granted to user " + username + " by " + TenancyHelper.getCurrentEndUser(), 
//                    username);
		}
		
		
	}

	/**
	 * Clears all permissions on a Software object. The userResponsible is used in the 
	 * log messages.
	 * 
	 * @param software
	 * @param userResponsible
	 * @throws SoftwareException
	 */
	public void clearAllPermissions(Software software, String userResponsible) throws SoftwareException
	{
		if (software == null) {
			throw new SoftwareException("No software specified");
		}
		
		for (SoftwarePermission pem: software.getPermissions()) {
			pem.setSoftware(null);
		}
		
		software.getPermissions().clear();
		
		SoftwareDao.persist(software);
		
		
//		ApplicationManager.addEvent(software, 
//                SoftwareEventType.PERMISSION_REVOKE, 
//                "Permissions were revoked for all users by " + userResponsible, 
//                userResponsible);
		
	}

	/**
	 * Returns the permission the given {@code principal} has for the {@link #software} 
	 * based on the scope, user, group permissions. No {@link RemoteSytem} {@link SystemRole}s 
	 * are taken into consideration.
	 * 
	 * @param principal
	 * @return  the assigned {@link SoftwarePermission} or a {@link SoftwarePermission} with {@link PermissionType#NONE} if no permission is assigned for the user.
	 * @throws SoftwarePermissionException
	 */
	public SoftwarePermission getEffectivePermissionForPrincipal(String principal) 
	throws SoftwarePermissionException 
	{
		if (StringUtils.isEmpty(principal)) {
			return new SoftwarePermission(principal, PermissionType.NONE);
		}
		else if (AuthorizationHelper.isTenantAdmin(principal)) {
			return new SoftwarePermission(principal, PermissionType.ALL);
		}
		else {
			SoftwarePermission pricipalPermission = _getUserOrGroupRoleForUserOnApp(principal);
			
			return pricipalPermission;
		}
	}
	
	/**
	 * Returns the {@link SoftwarePermission} assigned to a given user or  
	 * an implicit permission if the {@link Software} is public.
	 * @param principal
	 * @return the assigned {@link SoftwarePermission} or a {@link SoftwarePermission} with {@link PermissionType#NONE} if no permission is assigned for the user.
	 */
	protected SoftwarePermission _getUserOrGroupRoleForUserOnApp(String principal) {
		
		SoftwarePermission userPermission = null;
		
		// no role for empty users
		if (StringUtils.isEmpty(principal)) {
			userPermission = new SoftwarePermission(principal, PermissionType.NONE);
		}
		else {
			// lookup the user permissions in the db
			userPermission = SoftwarePermissionDao.getUserSoftwarePermissions(principal, software.getId());
			
			// owners have their role implicitly
			if (userPermission == null) {
				// all users have PermissionType.READ_EXECUTE unless explicitly revoked 
				if (software.isPubliclyAvailable()) {
					userPermission = new SoftwarePermission(principal, PermissionType.READ_EXECUTE);
				}
				// owners have all permissions to their apps until published.
				else if (StringUtils.equals(principal, software.getOwner())) {
					userPermission = new SoftwarePermission(principal, PermissionType.ALL);
				}
				// private apps must be granted explicitly
				else {
					userPermission = new SoftwarePermission(principal, PermissionType.NONE);
				}
			}
			else {
				// public apps honor standard private app permissions
			}
		}
		
		return userPermission;
	}

}
