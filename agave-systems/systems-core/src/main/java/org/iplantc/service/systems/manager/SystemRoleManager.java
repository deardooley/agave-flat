package org.iplantc.service.systems.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.auth.AuthorizationHelper;
import org.iplantc.service.common.clients.AgaveProfileServiceClient;
import org.iplantc.service.common.clients.beans.Profile;
import org.iplantc.service.common.exceptions.NotificationException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.util.HTMLizer;
import org.iplantc.service.notification.managers.NotificationManager;
import org.iplantc.service.notification.util.EmailMessage;
import org.iplantc.service.systems.Settings;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.events.RemoteSystemEventProcessor;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.exceptions.SystemRoleException;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.SystemRole;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.systems.model.enumerations.RoleType;
import org.iplantc.service.systems.model.enumerations.SystemEventType;
import org.iplantc.service.systems.util.ServiceUtils;
import org.json.JSONException;

/**
 * Handles adding and removing roles for a user on a system.
 * 
 * @author dooley
 *
 */
public class SystemRoleManager {
	private static final Logger	log	= Logger.getLogger(SystemRoleManager.class);
	
	private RemoteSystem system;
	private RemoteSystemEventProcessor eventProcessor;
	
	/**
	 * Default constructor to create an role manager on a system.
	 * 
	 * @param system
	 * @throws SystemException
	 */
	public SystemRoleManager(RemoteSystem system) throws SystemException
	{
		if (system == null) { 
			throw new SystemException("RemoteSystem cannot be null"); 
		}
		this.system = system;
		this.eventProcessor = new RemoteSystemEventProcessor();
	}
	
	/**
	 * Returns the effective {@link SystemRole} for a given principal. This 
	 * method should be used for both 
	 * @param principal
	 * @return
	 * @throws SystemException
	 */
	public SystemRole getEffectiveRoleForPrincipal(String principal) throws SystemException {
		
//		if (StringUtils.isEmpty(principal)) {
//			return new SystemRole(principal, RoleType.NONE);
//		}
//		else if (principal.equals(system.getOwner()))
//		{
//			return new SystemRole(principal, RoleType.OWNER);
//		}
//		else if (ServiceUtils.isAdmin(principal))
//		{
//			return new SystemRole(principal, RoleType.ADMIN);
//		}
//		else
//		{
//			SystemRole worldRole = new SystemRole(Settings.WORLD_USER_USERNAME, RoleType.NONE);
////			SystemRole publicRole = new SystemRole(Settings.PUBLIC_USER_USERNAME, RoleType.NONE);
//			for(SystemRole role: system.getRoles()) {
//				if(role.getUsername().equals(principal)) {
//					if (role.getRole() == RoleType.PUBLISHER && system.getType() == RemoteSystemType.STORAGE) {
//						return new SystemRole(principal, RoleType.USER);
//					} else {
//						return role;
//					}
//				} else if (role.getUsername().equals(Settings.WORLD_USER_USERNAME)) {
//					worldRole = role;
////				} else if (role.getUsername().equals(Settings.PUBLIC_USER_USERNAME)) {
////					publicRole = role;
//				}
//			}
//
//			if ( system.isPubliclyAvailable())
//			{
//				if (system.getType() != RemoteSystemType.EXECUTION && worldRole.canRead())
//				{
//					return new SystemRole(principal, RoleType.GUEST);
//				}
////				else if (worldRole.canRead() || publicRole.canRead())
////				{
////					if (worldRole.getRole().intVal() >= publicRole.getRole().intVal()) {
////						return worldRole;
////					} else {
////						return publicRole;
////					}
////				}
////				else if (worldRole.canRead())
////				{
////					return worldRole;
////				}
//				else
//				{
//					return new SystemRole(principal, RoleType.USER);
//				}
//			}
//			else
//			{
//				return new SystemRole(principal, RoleType.NONE);
//			}
//		}
//		
//		
		if (StringUtils.isEmpty(principal)) {
			return new SystemRole(principal, RoleType.NONE);
		}
		else if (AuthorizationHelper.isTenantAdmin(principal)) {
			return new SystemRole(principal, RoleType.ADMIN);
		}
		else {
			
			SystemRole pricipalRole = _getUserOrGroupRoleForUserOnSystem(principal);
			
			// user had not explicit permissions to the system defined
			if (pricipalRole == null) {
				
				// assign implied roles if the system is public
				if (system.isPubliclyAvailable())
				{
					// user roles get squashed when a system is published, and 
					// RoleType.USER is granted everyone unless it's an execution system,
					// then they get RoleType.GUEST (readonly) access.
					SystemRole impliedRole = null;
					SystemRole worldRole = _getUserOrGroupRoleForUserOnSystem(Settings.WORLD_USER_USERNAME);
					if (system.getType() != RemoteSystemType.EXECUTION && 
							worldRole != null && worldRole.canRead())
					{
						impliedRole = new SystemRole(principal, RoleType.GUEST);
					}
					else {
						impliedRole = new SystemRole(principal, RoleType.USER);
					}
					
					return impliedRole;
					
//					return pricipalRole.getRole().intVal() >= impliedRole.getRole().intVal() ? pricipalRole : impliedRole;
				}
				else {
					return  new SystemRole(principal, RoleType.NONE);
				}
			}
			// admin and publisher roles apply regardless of system scope. we 
			// explicitly check the role rather than using the RoleType.can* 
			// method because we don't want the system owner to get greenlighted
			// on public systems.
			else if (pricipalRole.getRole() == RoleType.ADMIN || pricipalRole.getRole() == RoleType.PUBLISHER) {
				return pricipalRole;
			}
			else {
				return pricipalRole;
			}
		}
	}
	
	/**
	 * Returns the {@link SystemRole} assigned to a given user. No implicit permission is returned 
	 * save ownership.
	 * 
	 * @param principal
	 * @return
	 */
	protected SystemRole _getUserOrGroupRoleForUserOnSystem(String principal) {
		
		// no role for empty users
		if (StringUtils.isEmpty(principal)) {
			return new SystemRole(principal, RoleType.NONE);
		}
		// owners of private systems have their role implicitly
		else if (!system.isPubliclyAvailable() && StringUtils.equals(principal, system.getOwner())) {
			return new SystemRole(principal, RoleType.OWNER);
		}
		
		// create a restricted role by default.
		SystemRole effectiveRole = null;
		
		// iterate through all roles...we could look this up directly, but they
		// are eager laoded with the sytem, so we already took the hit.
		for (SystemRole role: system.getRoles()) {
			if (role.getUsername().equals(principal)) {
				// fix any misplaced execution system roles that might have been applied to 
				// a storage system.
				if (role.getRole() == RoleType.PUBLISHER && system.getType() == RemoteSystemType.STORAGE) {
					effectiveRole = new SystemRole(principal, RoleType.USER);
				} 
				else {
					effectiveRole = role;
				}
				break;
			} 
//			// If a world role has been set, grant this to the user if they don't 
//			// have one of their own. This will be overridden by an actual user
//			// role if it comes up later.
//			else if (role.getUsername().equals(Settings.WORLD_USER_USERNAME)) {
//				if (effectiveRole == null) {
//					effectiveRole = new SystemRole(principal, role.getRole());
//				}
//			}
		}
		
		return effectiveRole;// == null ? new SystemRole(principal, RoleType.NONE) : effectiveRole;
	}

	/**
	 * Assigns a specifc role to a user on a system.
	 * 
	 * @param principal the principal whose role will be removed.
	 * @param roleType the {@link RoleType} to assign to the principal
	 * @param createdBy the user issuing the role removal
	 * @throws SystemException
	 * @throws SystemRoleException is attempting to assign {@link Settings#WORLD_USER_USERNAME} or {@link Settings#PUBLIC_USER_USERNAME} without tenant admin permissions.
	 */
	public void setRole(String principal, RoleType type, String createdBy)
	throws SystemException, SystemRoleException
	{
		if (StringUtils.isEmpty(principal) || StringUtils.equals(principal, "null")) { 
			throw new SystemException("Invalid username"); 
		}

		// ignore ownership unless the system is public, then the owner/creator won't
		// automatically have admin privilieges.
		if (!system.isPubliclyAvailable() && system.getOwner().equals(principal))
			return;
		
		if (type == RoleType.PUBLISHER && system.getType() == RemoteSystemType.STORAGE) {
			throw new SystemException("Cannot set PUBLISHER role on storage systems."); 
		}
		
		if (!AuthorizationHelper.isTenantAdmin(createdBy) && 
				(principal.equals(Settings.WORLD_USER_USERNAME) || principal.equals(Settings.PUBLIC_USER_USERNAME))) {
			throw new SystemRoleException("Only tenant admins may assign roles to public and world groups");
		}
		
		// if a null RoleType is given, then treat is as a "clear" request. 
		if (type == null) {
			removeRole(principal, createdBy);
		}
		// assigning a NONE role is an explicit assignment 
		else {
			
			SystemRole currentRole = getEffectiveRoleForPrincipal(principal);
			
			SystemDao dao = new SystemDao();
			
			if (currentRole == null)
			{ 
			    SystemRole newRole = new SystemRole(principal, type);
				system.getRoles().add(newRole);
				
				this.eventProcessor.processPermissionEvent(system, newRole, createdBy);
			} 
			else 
			{
				if (type.equals(RoleType.NONE)) {
					boolean found = false;
					for (SystemRole userRole: system.getRoles()) {
						if (StringUtils.equals(userRole.getUsername(), principal)) {
							userRole.setRole(RoleType.NONE);
							userRole.setLastUpdated(new Date());
						}
					}
					if (!found) {
						system.getRoles().add(new SystemRole(principal, RoleType.NONE));
					}
					
					this.eventProcessor.processPermissionEvent(system, new SystemRole(principal, RoleType.NONE), createdBy);
					
	//				// now disable all apps for this system that were registered by 
	//				// the user who just had access revoked
	//				for (String appId: new SystemDao().getUserOwnedAppsForSystemId(currentRole.getUsername(), system.getId())) {
	////					if (app.getOwner().equals(currentRole.getUsername())) {
	////						app.setAvailable(false);
	////						app.setLastUpdated(new Date());
	////						SoftwareDao.persist(app);
	////						appIds.add(app.getUniqueName());
	//						try {
	//							json = "{\"system\":" + system.toJSON() + ",\"app\":{\"uuid\":\"" + appId + "\"}";
	//							NotificationManager.process(appId, "SYSTEM_DISABLE", currentRole.getUsername(), json);
	//						} catch (JSONException e) {
	//							log.error("Failed to send system_disable event to app " + appId, e);
	//						}	
	////					}
	//				}
					
					// send the notifications in bulk
	//				sendApplicationDisabledMessage(currentRole.getUsername(), appIds, system);
				} 
				else {
					
					system.getRoles().remove(currentRole);
					SystemRole newRole = new SystemRole(principal, type);
					system.addRole(newRole);
					
					this.eventProcessor.processPermissionEvent(system, newRole, createdBy);
	//				String json;
	//				try {
	//					json = "{\"system\":" + system.toJSON() + ",\"role\":" + newRole.toJSON(system) + "}";
	//					NotificationManager.process(system.getUuid(), SystemEventType.ROLES_GRANT.name(), 
	//					        TenancyHelper.getCurrentEndUser(), json);
	//				} catch (JSONException e) {
	//					log.error("Failed to send role grant event for system " + system.getUuid(), e);
	//				}
	//				
				}
			}
			
			dao.merge(system);
		}
	}
	
	/**
	 * Clears explicit permissions for a given user.
	 * 
	 * @param username the principal whose role will be removed.
	 * @param createdBy the user issuing the role removal
	 * @throws SystemException
	 * @throws SystemRoleException is attempting to assign {@link Settings#WORLD_USER_USERNAME} or {@link Settings#PUBLIC_USER_USERNAME} without tenant admin permissions.
	 */
	public void removeRole(String username, String createdBy)
	throws SystemException, SystemRoleException
	{
		if (StringUtils.isEmpty(username) || StringUtils.equals(username, "null")) { 
			throw new SystemException("Invalid username"); 
		}

		// ignore ownership unless the system is public, then the owner/creator won't
		// automatically have admin privilieges.
		if (!system.isPubliclyAvailable() && system.getOwner().equals(username))
			return;
		
		if (!AuthorizationHelper.isTenantAdmin(createdBy) && 
				(username.equals(Settings.WORLD_USER_USERNAME) || username.equals(Settings.PUBLIC_USER_USERNAME))) {
			throw new SystemRoleException("Only tenant admins may revoke roles to public and world groups");
		}
		
		SystemRoleManager manager = new SystemRoleManager(system);
		SystemRole currentRole = manager.getEffectiveRoleForPrincipal(username);
		
		SystemDao dao = new SystemDao();
		
		if (currentRole != null) {
			system.getRoles().remove(currentRole);
			this.eventProcessor.processPermissionEvent(system, new SystemRole(username, RoleType.NONE), createdBy);
				
//				String json;
//				try {
//					json = "{\"system\":" + system.toJSON() + ",\"role\":" + currentRole.toJSON(system) + "}";
//					NotificationManager.process(system.getUuid(), SystemEventType.ROLES_REVOKE.name(), 
//					        TenancyHelper.getCurrentEndUser(), json);
//				} catch (JSONException e) {
//					log.error("Failed to send role revoke event for system " + system.getUuid(), e);
//				}
				
//				// now disable all apps for this system that were registered by 
//				// the user who just had access revoked
//				for (String appId: new SystemDao().getUserOwnedAppsForSystemId(currentRole.getUsername(), system.getId())) {
////					if (app.getOwner().equals(currentRole.getUsername())) {
////						app.setAvailable(false);
////						app.setLastUpdated(new Date());
////						SoftwareDao.persist(app);
////						appIds.add(app.getUniqueName());
//						try {
//							json = "{\"system\":" + system.toJSON() + ",\"app\":{\"uuid\":\"" + appId + "\"}";
//							NotificationManager.process(appId, "SYSTEM_DISABLE", currentRole.getUsername(), json);
//						} catch (JSONException e) {
//							log.error("Failed to send system_disable event to app " + appId, e);
//						}	
////					}
//				}
				
				// send the notifications in bulk
//				sendApplicationDisabledMessage(currentRole.getUsername(), appIds, system);
//			}
		}
		
		dao.merge(system);
	}

	/**
	 * Removes all but the system owner roles on a system.
	 * 
	 * @param createdBy the username of the user clearing the roles
	 * @throws SystemException
	 */
	public void clearRoles(String createdBy) throws SystemException
	{   
	    List<SystemRole> currentRoles = system.getRoles();
		SystemRole[] deletedRoles = currentRoles.toArray(new SystemRole[] {});
		
		system.getRoles().clear();
		
		new SystemDao().persist(system);
		
		if (!system.isPubliclyAvailable()) 
		{
			for (SystemRole deletedRole: deletedRoles) {
				this.eventProcessor.processPermissionEvent(system, new SystemRole(deletedRole.getUsername(), RoleType.NONE), createdBy);
			}
			
//			// now disable all apps for this system that were registered by users
//			// who were granted permissions, but are now revoked.
//			List<Software> apps = SoftwareDao.getAllBySystemId(system.getSystemId());
//			Map<String, List<String>> ownerSoftwareMap = new HashMap<String, List<String>>();
//			for (Software app: apps) {
//				if (!app.getOwner().equals(system.getOwner())) {
//					app.setAvailable(false);
//					app.setLastUpdated(new Date());
//					SoftwareDao.persist(app);
//					NotificationManager.process(app.getUuid(), "DISABLE", app.getOwner());
//					if (ownerSoftwareMap.containsKey(app.getOwner())) {
//						ownerSoftwareMap.get(app.getOwner()).add(app.getUniqueName());
//					} else {
//						ownerSoftwareMap.put(app.getOwner(), Arrays.asList(app.getUniqueName()));
//					}
//				}
//			}
			
//			// send the notifications in bulk
//			for (String appOwner: ownerSoftwareMap.keySet()) {
//				sendApplicationDisabledMessage(appOwner, ownerSoftwareMap.get(appOwner), system);
//			}
		}
		
//		StringBuilder sb = new StringBuilder();
//		sb.append("[");
//		for (SystemRole role: roles) {
//		    if (sb.length() > 0) {
//		        sb.append("," + role.toJSON(system));
//		    } else {
//		        sb.append(role.toJSON(system));
//		    }
//		}
//		sb.append("]");
//		try {
//			String sysjsonn = "{\"system\":" + system.toJSON() + ",\"role\":";
//			for (SystemRole role: roles) {
//				String json = sysjsonn + role.toJSON(system) + "}";
//				NotificationManager.process(system.getUuid(), SystemEventType.ROLES_REVOKE.name(), 
//				        TenancyHelper.getCurrentEndUser(), json);
//			} 
//		} catch (JSONException e) {
//			log.error("Failed to send role revoke event for system " + system.getUuid(), e);
//		}
    
//		NotificationManager.process(system.getUuid(), SystemEventType.ROLES_REVOKE.name(), 
//		        TenancyHelper.getCurrentEndUser(), sb.toString());
	}
	
	/**
	 * Alerts an app publishers that their applications were disabled. 
	 * This occurs when a publisher role has been removed from them on a system.
	 * 
	 * @param appOwner
	 * @param appIds
	 * @param system
	 */
	public void sendApplicationDisabledMessage(String appOwner, List<String> appIds, RemoteSystem system) 
	{
		if (ServiceUtils.isValid(appIds)) return;
		
		AgaveProfileServiceClient profileClient = new AgaveProfileServiceClient(
				Settings.IPLANT_PROFILE_SERVICE + "profile/search/username/" + appOwner, 
				Settings.IRODS_USERNAME, 
				Settings.IRODS_PASSWORD);
		
		String fullname = "";
		Profile ownerProfile = null;
		try
		{
			for(Profile profile: profileClient.getUsers()) 
			{
				if (profile.getUsername().equals(appOwner)) {
					ownerProfile = profile;
					break;
				}
			}
			
			if (ownerProfile == null) {
				throw new NotificationException("User profile not found for " + appOwner);
			}
		}
		catch (Exception e)
		{
			log.error("Error looking up email address for " + appOwner + 
					" no notification email will be sent", e);
		}
		
		try 
		{	
			String subject = "Your access to \"" + system.getName() + "\" has been revoked.";
			
			String body = fullname + ",\n\n" +
					"This email is being sent to you as a courtesy by the iPlant Foundation API. " + 
					"Your access to " + system.getName() + " (" + system.getSystemId() + ") " +
					"has been revoked by the owner. As a result, the applications you " +
					"registered on this system have been disabled. The affected applications " +
					"are listed below.\n\n";
			for(String appId: appIds) {
				body += "\t" + appId + "\n";
			}
			body += "\nYou will still have access to the application data itself as long as " +
					"you have access to the storage system on which the application deployment " +
					"directory exists. Any jobs you and others have run using the disabled applications " +
					"will remain accessible through your personal job histories. The generated job " +
					"data will be available through the job service. If you have further questions, " +
					"please contact help@iplantcollaborative.org.";
			
			try {
				EmailMessage.send(fullname, ownerProfile.getEmail(), subject, body, HTMLizer.htmlize(body));
			} catch (Exception e) {
				log.error("Failed to send software deactivation notification to " + 
						appOwner + " at " + ownerProfile.getEmail(), e);
			}
		}
		catch (Exception e)
		{
			log.error("Error notifying " + appOwner + " of application deactivation.", e);
		}
	}
}
