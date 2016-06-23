package org.iplantc.service.tags.managers;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import net.sf.cglib.core.CollectionUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.auth.AuthorizationHelper;
import org.iplantc.service.common.clients.AgaveProfileServiceClient;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.tags.events.TagEventProcessor;
import org.iplantc.service.tags.dao.PermissionDao;
import org.iplantc.service.tags.exceptions.PermissionValidationException;
import org.iplantc.service.tags.exceptions.TagEventProcessingException;
import org.iplantc.service.tags.exceptions.TagPermissionException;
import org.iplantc.service.tags.exceptions.UnknownIdentityException;
import org.iplantc.service.tags.model.Tag;
import org.iplantc.service.tags.model.TagEvent;
import org.iplantc.service.tags.model.TagPermission;
import org.iplantc.service.tags.model.enumerations.PermissionType;
import org.iplantc.service.tags.model.enumerations.TagEventType;

/**
 * Handles permission operations on an entity.
 * @author dooley
 *
 */
public class TagPermissionManager {
	
	private static final Logger log = Logger.getLogger(TagPermissionManager.class);
	private Tag tag;
	
	public TagPermissionManager(Tag tag) {
		this.tag = tag;
	}
	
	/**
	 * Applies the given permission by updating an existing permission or
	 * 
	 * @param permission
	 * @return
	 * @throws TagPermissionException
	 * @throws PermissionValidationException 
	 */
	public TagPermission setPermission(TagPermission permission) 
	throws TagPermissionException, PermissionValidationException 
	{
		TagPermission currentPermission = getUserPermission(permission.getUsername());
		
		if (permission.getPermission() == PermissionType.NONE) {
			removeAllPermissionForUser(permission.getUsername());
			return permission;
		}
		else if (AuthorizationHelper.isTenantAdmin(permission.getUsername()) 
				|| isOwner(permission.getUsername())) {
			return currentPermission;
		}
		else {
			currentPermission.setPermission(permission.getPermission());
			currentPermission.setLastUpdated(new Date());
			
			PermissionDao.persist(currentPermission);
			
			TagEventProcessor eventProcessor = new TagEventProcessor();
			try {
				eventProcessor.processPermissionEvent(tag, currentPermission, 
						new TagEvent(tag.getUuid(), 
								TagEventType.PERMISSION_GRANT, 
								"Write permission granted to user " + currentPermission.getUsername() + 
									" by user " + TenancyHelper.getCurrentEndUser(),
								TenancyHelper.getCurrentEndUser()));
			} catch (TagEventProcessingException e) {
				log.error("Failed to send " + currentPermission.getPermission().name().toLowerCase() + 
						" permission grant event for tag " + tag.getUuid(), e);
			}
			
			return currentPermission;
			
		}
		
	}
	
	/**
	 * Returns all {@link TagPermission}s for a given {@link Tag}. This will include
	 * the implied owner and admin permissions as well.
	 * 
	 * @param username
	 * @return
	 * @throws TagPermissionException
	 */
	public List<TagPermission> getAllPermissions(String username) 
	throws TagPermissionException
	{
		List<TagPermission> pems = new ArrayList<TagPermission>();
		
		if (tag == null) {
			throw new TagPermissionException("No tag found for permission validation");
		}
		else if (StringUtils.isEmpty(username)) { 
			throw new TagPermissionException("No username provided.");
		} 
		else {
			if (StringUtils.equals(username, tag.getOwner())) {
				pems.add(new TagPermission(tag, username, PermissionType.ALL));
			} 
			else if (AuthorizationHelper.isTenantAdmin(username)) {
				pems.add(new TagPermission(null, username, PermissionType.ALL));
			}
			
			pems.addAll(PermissionDao.getEntityPermissions(tag.getUuid()));
		}
		
		return pems;
	}
	
	/**
	 * Gets the resolved permission for a user. This includes resolution of 
	 * implicit and owner permissions.
	 * 
	 * @param username
	 * @return
	 * @throws TagPermissionException
	 * @throws PermissionValidationException
	 */
	public TagPermission getUserPermission(String username) 
	throws TagPermissionException, PermissionValidationException
	{
		if (tag == null) {
			throw new PermissionValidationException("No tag found for permission validation");
		}
		else if (StringUtils.isEmpty(username)) { 
			throw new PermissionValidationException("No username provided.");
		} 
		else if (AuthorizationHelper.isTenantAdmin(username) || 
					StringUtils.equals(username, tag.getOwner())) {
			return new TagPermission(tag, username, PermissionType.ALL);
		}
		else {
			List<TagPermission> pems = PermissionDao.getEntityPermissions(tag.getUuid());
			for (TagPermission pem: pems) {
				if (StringUtils.equalsIgnoreCase(pem.getUsername(), username)) {
					return pem;
				} 
			}
			return new TagPermission(tag, username, PermissionType.NONE);
		}
	}
	
	/**
	 * Checks for user ownership of the entity
	 * @param username
	 * @return
	 * @throws PermissionValidationException
	 */
	public boolean isOwner(String username) throws PermissionValidationException 
	{
		if (tag == null) {
			throw new PermissionValidationException("No tag found for permission validation");
		}
		else if (StringUtils.isEmpty(username)) { 
			throw new PermissionValidationException("No username provided.");
		} 
		else {
			return StringUtils.equals(tag.getOwner(), username);
		}
	}
	
	/**
	 * Checks for user ability to edit the entity
	 * @param username
	 * @return
	 * @throws TagPermissionException
	 * @throws PermissionValidationException
	 */
	public boolean canWrite(String username) 
	throws PermissionValidationException, TagPermissionException 
	{
		if (tag == null) {
			throw new PermissionValidationException("No tag found for permission validation");
		}
		else if (StringUtils.isEmpty(username)) { 
			throw new PermissionValidationException("No username provided.");
		} 
		else {
			return getUserPermission(username).getPermission().canWrite();
		}
	}
	
	/**
	 * Checks for user ability to edit the entity
	 * @param username
	 * @return
	 * @throws TagPermissionException
	 * @throws PermissionValidationException 
	 */
	public boolean addWrite(String username) 
	throws TagPermissionException, PermissionValidationException 
	{
		if (tag == null) {
			throw new PermissionValidationException("No tag found for permission validation");
		}
		else if (StringUtils.isEmpty(username)) { 
			throw new PermissionValidationException("No username provided.");
		} 
		else if (AuthorizationHelper.isTenantAdmin(username) || isOwner(username)) {
			return false;
		} 
		else if (!isValidPrincipal(username)) {
			throw new PermissionValidationException("No user found matching " + username);
		}
		else {
			TagPermission currentPem = getUserPermission(username);
			
			if (currentPem.getPermission().canWrite()) {
				// not much to do here. carry on
			} 
			else {
				// if they have an existing permissions, updated the timestamp
				currentPem.setPermission(currentPem.getPermission().add(PermissionType.WRITE));
				currentPem.setLastUpdated(new Timestamp(System.currentTimeMillis()));
				
				PermissionDao.persist(currentPem);
				
				TagEventProcessor eventProcessor = new TagEventProcessor();
				try {
					eventProcessor.processPermissionEvent(tag, currentPem, 
							new TagEvent(tag.getUuid(), 
									TagEventType.PERMISSION_GRANT, 
									"Write permission granted to user " + username + " by user " + TenancyHelper.getCurrentEndUser(),
									TenancyHelper.getCurrentEndUser()));
				} catch (TagEventProcessingException e) {
					log.error("Failed to send write permission grant event for tag " + tag.getUuid(), e);
				}
			}
			return true;
		}
	}
	
	/**
	 * Checks for user ability to edit the entity
	 * @param username
	 * @return
	 * @throws TagPermissionException
	 * @throws PermissionValidationException
	 */
	public boolean removeWrite(String username) 
	throws TagPermissionException, PermissionValidationException
	{
		if (tag == null) {
			throw new PermissionValidationException("No tag found for permission validation");
		}
		else if (StringUtils.isEmpty(username)) { 
			throw new PermissionValidationException("No username provided.");
		} 
		else if (AuthorizationHelper.isTenantAdmin(username) || isOwner(username)) {
			return false;
		} 
		else {
			TagPermission currentPem = getUserPermission(username);
			
			if (!currentPem.getPermission().canWrite()) {
				// not much to do here. carry on
			} 
			else {
				// if they have an existing permissions, updated the timestamp
				PermissionType newPermissionType = currentPem.getPermission().subtract(PermissionType.WRITE);
				if (currentPem.getPermission() == PermissionType.NONE) {
					PermissionDao.delete(currentPem);
				} 
				else {
					currentPem.setPermission(newPermissionType);
					currentPem.setLastUpdated(new Timestamp(System.currentTimeMillis()));
					PermissionDao.persist(currentPem);
				}
				
				TagEventProcessor eventProcessor = new TagEventProcessor();
				try {
					eventProcessor.processPermissionEvent(tag, currentPem, 
							new TagEvent(tag.getUuid(), 
									TagEventType.PERMISSION_REVOKE, 
									"Write permission revoked to user " + username + " by user " + TenancyHelper.getCurrentEndUser(),
									TenancyHelper.getCurrentEndUser()));
				} catch (TagEventProcessingException e) {
					log.error("Failed to send write permission revoke event for tag " + tag.getUuid(), e);
				}
			}
			return true;
		}
	}
	
	/**
	 * Checks for user ability to read the entity
	 * @param username
	 * @return
	 * @throws TagPermissionException
	 */
	public boolean canRead(String username) throws TagPermissionException 
	{
		return getUserPermission(username).getPermission().canRead();
	}
	
	/**
	 * Checks for user ability to read the entity
	 * @param username
	 * @return
	 * @throws TagPermissionException
	 * @throws PermissionValidationException 
	 */
	public boolean addRead(String username) 
	throws TagPermissionException, UnknownIdentityException 
	{
		if (tag == null) {
			throw new PermissionValidationException("No tag found for permission validation");
		}
		else if (StringUtils.isEmpty(username)) { 
			throw new PermissionValidationException("No username provided.");
		} 
		else if (AuthorizationHelper.isTenantAdmin(username) || isOwner(username)) {
			return false;
		} 
		else if (!isValidPrincipal(username)) {
			throw new PermissionValidationException("No user found matching " + username);
		}
		else {
			TagPermission currentPem = getUserPermission(username);
			
			if (currentPem.getPermission().canRead()) {
				// not much to do here. carry on
			} 
			else {
				// if they have an existing permissions, updated the timestamp
				currentPem.setPermission(currentPem.getPermission().add(PermissionType.READ));
				currentPem.setLastUpdated(new Timestamp(System.currentTimeMillis()));
				
				PermissionDao.persist(currentPem);
				
				TagEventProcessor eventProcessor = new TagEventProcessor();
				try {
					eventProcessor.processPermissionEvent(tag, currentPem, 
							new TagEvent(tag.getUuid(), 
									TagEventType.PERMISSION_GRANT, 
									"Read permission granted to user " + username + " by user " + TenancyHelper.getCurrentEndUser(),
									TenancyHelper.getCurrentEndUser()));
				} catch (TagEventProcessingException e) {
					log.error("Failed to send read permission grant event for tag " + tag.getUuid(), e);
				}
			}
			return true;
		}
	}
	
	/**
	 * Checks for user ability to read the entity
	 * @param username
	 * @return
	 * @throws TagPermissionException
	 * @throws PermissionValidationException 
	 */
	public boolean removeRead(String username) 
	throws TagPermissionException, PermissionValidationException 
	{
		if (tag == null) {
			throw new PermissionValidationException("No tag found for permission validation");
		}
		else if (StringUtils.isEmpty(username)) { 
			throw new PermissionValidationException("No username provided.");
		} 
		else if (AuthorizationHelper.isTenantAdmin(username) || isOwner(username)) {
			return false;
		} 
		else {
			TagPermission currentPem = getUserPermission(username);
			
			if (!currentPem.getPermission().canRead()) {
				// not much to do here. carry on
			} 
			else {
				// if they have an existing permissions, updated the timestamp
				PermissionType newPermissionType = currentPem.getPermission().subtract(PermissionType.READ);
				if (currentPem.getPermission() == PermissionType.NONE) {
					PermissionDao.delete(currentPem);
				} 
				else {
					currentPem.setPermission(newPermissionType);
					currentPem.setLastUpdated(new Timestamp(System.currentTimeMillis()));
					PermissionDao.persist(currentPem);
				}
				
				TagEventProcessor eventProcessor = new TagEventProcessor();
				try {
					eventProcessor.processPermissionEvent(tag, currentPem, 
							new TagEvent(tag.getUuid(), 
									TagEventType.PERMISSION_REVOKE, 
									"Read permission revoked to user " + username + " by user " + TenancyHelper.getCurrentEndUser(),
									TenancyHelper.getCurrentEndUser()));
				} catch (TagEventProcessingException e) {
					log.error("Failed to send read permission revoke event for tag " + tag.getUuid(), e);
				}
			}
			return true;
		}
	}
	
	/**
	 * Checks for user ability to read and write the entity
	 * @param username
	 * @return
	 * @throws TagPermissionException
	 */
	public boolean canAll(String username) throws TagPermissionException 
	{
		PermissionType pem = getUserPermission(username).getPermission();
		return pem.canRead() && pem.canWrite();
	}
	
	/**
	 * Checks for user ability to read the entity
	 * @param username
	 * @return
	 * @throws TagPermissionException
	 * @throws PermissionValidationException 
	 */
	public boolean addAll(String username) 
	throws TagPermissionException, PermissionValidationException 
	{
		if (tag == null) {
			throw new TagPermissionException("No tag found for permission validation");
		}
		else if (StringUtils.isEmpty(username)) { 
			throw new PermissionValidationException("No username provided.");
		} 
		else if (AuthorizationHelper.isTenantAdmin(username) || isOwner(username)) {
			return false;
		} 
		else if (!isValidPrincipal(username)) {
			throw new PermissionValidationException("No user found matching " + username);
		}
		else {
			TagPermission currentPem = getUserPermission(username);
			
			if (currentPem.getPermission() == PermissionType.ALL) {
				// not much to do here. carry on
			} 
			else {
				// if they have an existing permissions, updated the timestamp
				if (currentPem.getPermission() != PermissionType.NONE) {
					currentPem.setLastUpdated(new Timestamp(System.currentTimeMillis()));
				}
				
				currentPem.setPermission(PermissionType.ALL);
				
				PermissionDao.persist(currentPem);
				
				TagEventProcessor eventProcessor = new TagEventProcessor();
				try {
					eventProcessor.processPermissionEvent(tag, currentPem, 
							new TagEvent(tag.getUuid(), 
									TagEventType.PERMISSION_GRANT, 
									"All permissions granted for user " + username + " by user " + TenancyHelper.getCurrentEndUser(),
									TenancyHelper.getCurrentEndUser()));
				} catch (TagEventProcessingException e) {
					log.error("Failed to send all permission revocation event for tag " + tag.getUuid(), e);
				}
			}
			return true;
		}
	}
	
	/**
	 * Removes all permissions for a user. If the named user is an admin or the 
	 * owner, permission are left in tact. Implied permission cannot change.
	 * 
	 * @param username
	 * @return true if removed, false otherwise.
	 * @throws TagPermissionException
	 * @throws PermissionValidationException 
	 */
	public boolean removeAllPermissionForUser(String username) 
	throws TagPermissionException, PermissionValidationException 
	{
		if (tag == null) {
			throw new PermissionValidationException("No tag found for permission validation");
		}
		else if (StringUtils.isEmpty(username)) { 
			throw new PermissionValidationException("No username provided.");
		} 
		else if (AuthorizationHelper.isTenantAdmin(username) || isOwner(username)) {
			return false;
		} 
		else {
			TagPermission currentPem = getUserPermission(username);
		
			if (currentPem.getPermission() != PermissionType.NONE) {
				PermissionDao.delete(currentPem);
				
				TagEventProcessor eventProcessor = new TagEventProcessor();
				try {
					eventProcessor.processPermissionEvent(tag, currentPem, 
							new TagEvent(tag.getUuid(), 
									TagEventType.PERMISSION_REVOKE, 
									"Permissions revoked for user " + username + " by user " + TenancyHelper.getCurrentEndUser(),
									TenancyHelper.getCurrentEndUser()));
				} catch (TagEventProcessingException e) {
					log.error("Failed to send all permission revocation event for tag " + tag.getUuid(), e);
				}
			}
			else {
				// NONE permission are not saved, so nothing needs to be done here.
			}
			return true;
		}
	}
	
	/**
	 * Removes all permissions granted on a tag. Implied permission cannot change.
	 * 
	 * @return true if removed, false otherwise.
	 * @throws TagPermissionException
	 */
	public boolean clearPermissions() throws TagPermissionException 
	{
		if (tag == null) {
			throw new TagPermissionException("No tag found for permission validation");
		}
		else {
			// iterate over the events so we can process the individual events
			for (TagPermission permission: PermissionDao.getEntityPermissions(tag.getUuid())) {
				
				PermissionDao.delete(permission);
				
				TagEventProcessor eventProcessor = new TagEventProcessor();
				try {
					eventProcessor.processPermissionEvent(tag, permission, 
							new TagEvent(tag.getUuid(), 
									TagEventType.PERMISSION_REVOKE, 
									"Permissions revoked for user " + permission.getUsername() + " by user " + TenancyHelper.getCurrentEndUser(),
									TenancyHelper.getCurrentEndUser()));
				} catch (TagEventProcessingException e) {
					log.error("Failed to send all permission revocation event for tag " + tag.getUuid(), e);
				}
			}
			
			return true;
		}
	}
	
	/**
	 * Calls client service to verify the given principal is valid.
	 * @param principal
	 * @return
	 * @throws Exception
	 */
	public boolean isValidPrincipal(String principal) throws PermissionValidationException {
		
		try {
			// validate the user they are giving permissions to exists
			AgaveProfileServiceClient authClient = 
					new AgaveProfileServiceClient(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE, tag.getTenantId()), 
												  Settings.COMMUNITY_PROXY_USERNAME, 
												  Settings.COMMUNITY_PROXY_PASSWORD);
	  
			return authClient.getUser(principal) == null;
		} 
		catch (Exception e) {
			throw new PermissionValidationException("Unable to verify identity of the principal", e);
		}
	}

}
