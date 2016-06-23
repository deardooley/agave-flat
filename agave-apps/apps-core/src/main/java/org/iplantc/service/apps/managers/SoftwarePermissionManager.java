package org.iplantc.service.apps.managers;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.apps.Settings;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.dao.SoftwarePermissionDao;
import org.iplantc.service.apps.exceptions.SoftwareException;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwarePermission;
import org.iplantc.service.apps.util.ServiceUtils;
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
}
