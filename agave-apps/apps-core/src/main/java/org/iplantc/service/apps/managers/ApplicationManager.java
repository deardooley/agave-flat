/**
 * 
 */
package org.iplantc.service.apps.managers;

import static org.iplantc.service.apps.exceptions.SoftwareResourceException.CLIENT_ERROR_BAD_REQUEST;
import static org.iplantc.service.apps.exceptions.SoftwareResourceException.CLIENT_ERROR_FORBIDDEN;
import static org.iplantc.service.apps.exceptions.SoftwareResourceException.CLIENT_ERROR_NOT_FOUND;
import static org.iplantc.service.apps.exceptions.SoftwareResourceException.SERVER_ERROR_INTERNAL;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.iplantc.service.apps.Settings;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.exceptions.SoftwareException;
import org.iplantc.service.apps.exceptions.SoftwareResourceException;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwarePermission;
import org.iplantc.service.apps.model.enumerations.SoftwareEventType;
import org.iplantc.service.apps.queue.actions.PublishAction;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.permissions.PermissionManager;
import org.iplantc.service.notification.dao.NotificationDao;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.systems.exceptions.RemoteCredentialException;
import org.iplantc.service.systems.exceptions.SystemUnknownException;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Handles management tasks for {@link Software} objects.
 * 
 * @author dooley
 * 
 */
public class ApplicationManager 
{
	private static final Logger	log	= Logger.getLogger(ApplicationManager.class.getName());

	/**
	 * No-args constructor for testing
	 */
	public ApplicationManager() {}
	
	public static Software getApplication(String appName)
	{
		Software software = SoftwareDao.get(appName);

		return software;
	}

	public static List<Software> getApplications()
	{

		return SoftwareDao.getAll();
	}

	public static List<Software> getPublicApplications()
	{

		return SoftwareDao.getAllPublic();
	}

	public static List<Software> getSharedApplications()
	{

		return SoftwareDao.getAllPrivate();
	}
	
	/**
	 * Validates a {@link JSONObject} representing a {@link Software} registration request. If an 
	 * {@code existingSoftware} exists, then the {@link JSONObject} is validated for correctness 
	 * and merged with the existing {@link Software} object as an update. Otherwise a new {@link Software}
	 * entry is created.
	 *  
	 * @param existingSoftware
	 * @param json
	 * @param username
	 * @return
	 * @throws SoftwareResourceException
	 */
	public Software processSoftware(Software existingSoftware, JSONObject json, String username) 
	throws SoftwareResourceException
	{
		String owner = (existingSoftware == null ? username : existingSoftware.getOwner());
		
		try 
		{
			// parse and validate the json
	        Software newSoftware = Software.fromJSON(json, owner);
	        newSoftware.setOwner(owner);
	        
	        if (!newSoftware.getExecutionSystem().getUserRole(username).canPublish()) {
	        	throw new SoftwareResourceException(CLIENT_ERROR_FORBIDDEN,
	        			"User does not have permission to publish applications on execution system \"" + 
	        					newSoftware.getExecutionSystem().getSystemId() + "\"");
	        }
	        
	        if (existingSoftware != null && 
	        		!newSoftware.getUniqueName().equalsIgnoreCase(existingSoftware.getUniqueName())) 
	        {
	        	if (SoftwareDao.getSoftwareByUniqueName(newSoftware.getUniqueName()) != null) {
	        		throw new SoftwareResourceException(CLIENT_ERROR_BAD_REQUEST,
						"Uploaded application description does not match target uri. You uploaded an " +
						"application description for \"" + newSoftware.getUniqueName() + "\" to the uri " +
						"for \"" + existingSoftware.getUniqueName() + "\". Please either register a new " +
						"application or post your update to " + 
						Settings.IPLANT_JOB_SERVICE + "apps/" + newSoftware.getUniqueName() +
						" to update that app.");
	        	} else {
	        		throw new SoftwareResourceException(CLIENT_ERROR_BAD_REQUEST,
						"Uploaded application description does not match target uri. You uploaded an " +
						"application description for \"" + newSoftware.getUniqueName() + "\" to the uri " +
						"for \"" + existingSoftware.getUniqueName() + "\". Please post your application " +
						"description to " + Settings.IPLANT_JOB_SERVICE + "apps to create a new app.");
	        	}
	        }
	        
	        if (ApplicationManager.userCanPublish(username, newSoftware)) 
	        {	
	        	validateSoftwareDependencies(username, newSoftware);
	        	
	        } else {
	        	throw new SoftwareResourceException(CLIENT_ERROR_FORBIDDEN,
						"Permission denied. You do not have permission to register applications");
	        }
	        
	        
			// why update? we should just blow them away and add the new ones. 
			// they can't upload a partial description...
	        if (existingSoftware != null) 
            {
	            newSoftware.setUuid(existingSoftware.getUuid());
	            newSoftware.setCreated(existingSoftware.getCreated());
	            newSoftware.setLastUpdated(new Date());
				newSoftware.setRevisionCount(existingSoftware.getRevisionCount() + 1);
				for (SoftwarePermission pem: existingSoftware.getPermissions()) {
					SoftwarePermission newPem = pem.clone();
					newPem.setSoftware(newSoftware);
					newSoftware.getPermissions().add(newPem);
				}
            }
			
			return newSoftware;
		} 
		catch (JSONException e) 
		{
			throw new SoftwareResourceException(CLIENT_ERROR_BAD_REQUEST,
					"Invalid json description: " + e.getMessage(), e);
        } 
		catch (FileNotFoundException e) 
		{
        	throw new SoftwareResourceException(CLIENT_ERROR_NOT_FOUND, e.getMessage(), e);
        }
        catch (SoftwareException | SoftwareResourceException e) 
        {
			throw e;
		} 
		catch (RemoteDataException e) 
		{
			throw new SoftwareResourceException(SERVER_ERROR_INTERNAL, 
					"Failed to verify application dependencies: " + e.getMessage(), e);
		} 
		catch (Throwable e) 
		{
			throw new SoftwareResourceException(SERVER_ERROR_INTERNAL,
					"Failed to update application: " + e.getMessage(), e);
		}
	}

	/**
	 * Checks that the app dependencies are present on the remote system as 
	 * part of the validation process.
	 * 
	 * @param username
	 * @param newSoftware
	 * @return true unless dependencies are not supported for the {@link Software#getExecutionType()}
	 * @throws RemoteDataException
	 * @throws RemoteCredentialException
	 * @throws IOException
	 * @throws SoftwareResourceException
	 * @throws PermissionException
	 * @throws FileNotFoundException
	 */
	protected boolean validateSoftwareDependencies(String username, Software newSoftware) 
	throws RemoteDataException, RemoteCredentialException, IOException, SoftwareResourceException,
			PermissionException, FileNotFoundException 
	{
		RemoteDataClient remoteDataClient = null;
		try {
			remoteDataClient = newSoftware.getStorageSystem().getRemoteDataClient();
			remoteDataClient.authenticate();
			
			String checkPath = newSoftware.getDeploymentPath();
			LogicalFile logicalFile = null;
			PermissionManager pm = null;
			
			try { logicalFile=LogicalFileDao.findBySystemAndPath(newSoftware.getStorageSystem(), checkPath); } catch(Exception e) {}
			
			pm = new PermissionManager(newSoftware.getStorageSystem(), remoteDataClient, logicalFile, username);
			
			if (!remoteDataClient.doesExist(checkPath)) {
				throw new SoftwareResourceException(CLIENT_ERROR_BAD_REQUEST,
						"Invalid deploymentPath value. " + checkPath + 
						" does not exist on deploymentSystem " + newSoftware.getStorageSystem().getSystemId() + 
						". Please specify a valid deploymentPath.");
			}
			else if (!pm.canRead(remoteDataClient.resolvePath(checkPath))) {
				throw new SoftwareResourceException(CLIENT_ERROR_FORBIDDEN,
						"Permission denied. You do not have permission to access the deployment path " + checkPath);
			}
			
			checkPath = newSoftware.getDeploymentPath() + "/" + newSoftware.getExecutablePath();
			
			try { logicalFile=LogicalFileDao.findBySystemAndPath(newSoftware.getStorageSystem(), checkPath); } catch(Exception e) {}
			
			pm = new PermissionManager(newSoftware.getStorageSystem(), remoteDataClient, logicalFile, username);
			
			if (!remoteDataClient.doesExist(checkPath)) {
				throw new SoftwareResourceException(CLIENT_ERROR_BAD_REQUEST,
						"Invalid templatePath value. " + checkPath + 
						" does not exist on the deploymentSystem " + newSoftware.getStorageSystem().getSystemId() + 
						". Please specify a valid templatePath.");
			}
			else if (!pm.canRead(remoteDataClient.resolvePath(checkPath))) {
				throw new SoftwareResourceException(CLIENT_ERROR_FORBIDDEN,
						"Permission denied. You do not have permission to access the wrapper script " + checkPath);
			}
			
			checkPath = newSoftware.getDeploymentPath() + "/" + newSoftware.getTestPath();
			
			try { logicalFile=LogicalFileDao.findBySystemAndPath(newSoftware.getStorageSystem(), checkPath); } catch(Exception e) {}
			
			pm = new PermissionManager(newSoftware.getStorageSystem(), remoteDataClient, logicalFile, username);
			
			if (!remoteDataClient.doesExist(checkPath)) {
				throw new SoftwareResourceException(CLIENT_ERROR_BAD_REQUEST,
						"Invalid testPath value. " + checkPath + 
						" does not exist on the deploymentSystem " + newSoftware.getStorageSystem().getSystemId() + 
						". Please specify a valid testPath.");
			}
			else if (!pm.canRead(remoteDataClient.resolvePath(checkPath))) {
				throw new SoftwareResourceException(CLIENT_ERROR_FORBIDDEN,
						"Permission denied. You do not have permission to access the test script " + checkPath);
			}
			
			return true;
		}
		finally {
			try { remoteDataClient.disconnect();} catch (Exception e) {}
		}
	}

	/**
	 * Makes a private app public by snappshotting, compressing, and checksumming the deploymentPath
	 * onto the tenant default public storage system.
	 * 
	 * @param software
	 * @returns public software object
	 * @throws SoftwareException
	 */
	public static Software makePublic(Software software, String apiUsername) throws SoftwareException
	{
		PublishAction publishAction = new PublishAction(software, apiUsername);
		try {
		    publishAction.run();
		
		    return publishAction.getPublishedSoftware();
		} catch (Exception e) {
		    throw new SoftwareException("Failed to publish app.", e);
		}
	}
	
	/**
	 * Checks that the user has proper permissions on the underlying {@link ExecutionSystem} 
	 * and the given {@link Software} object to submit a job. Existence of the {@link Software#getDeploymentPath()} 
	 * is also verified. If missing, and the app is disabled and notifications are thrown.
	 * 
	 * @param software
	 * @param username
	 * @return
	 */
	public static boolean isInvokableByUser(Software software, String username) 
	throws SoftwareException
	{
		if (software == null) {
			throw new SoftwareException("No shared app found matching that name");
		} 
		else {
			if (!software.isAvailable())
			{
				throw new SoftwareException("App is not available for execution");
			}
			else 
			{
				
				RemoteDataClient dataClient = null;
			
				try {
					dataClient = software.getStorageSystem().getRemoteDataClient();
					dataClient.authenticate();
					String wrapperPath = null;
					if (software.isPubliclyAvailable()) {
						wrapperPath = software.getDeploymentPath();
					} else {
						wrapperPath = software.getDeploymentPath() + '/' + software.getExecutablePath();
					}
					
					if (dataClient.doesExist(wrapperPath))
					{
						SoftwarePermissionManager pemManager = new SoftwarePermissionManager(software);
						return pemManager.canExecute(username) && 
								software.getExecutionSystem().getUserRole(username).canUse();
					} 
					else 
					{
						log.error("Disabling application " + 
								software.getUniqueName() + "(" + software.getId() + ") due to missing wrapper script." );
						software.setAvailable(false);
						SoftwareDao.persist(software);
						
						String message = "Unable to locate app templatePath at " + 
								software.getDeploymentPath() + "/" + software.getExecutablePath() +
								" on system " + software.getStorageSystem().getSystemId() + 
								". App is not available for execution and will be disabled.";
						
						SoftwareEventProcessor eventProcessor = new SoftwareEventProcessor();
						eventProcessor.processSoftwareContentEvent(software, 
								SoftwareEventType.DISABLED,
								message,
								username);
						
						throw new SoftwareException(message);
					}
				}
				catch (SoftwareException e) {
					throw e;
				}
				catch (RemoteDataException e) {
				    throw new SoftwareException("Unable to locate application assets on remote system. "
				            + "Remote server responded with: " + e.getMessage(), e);
				}
				catch (Exception e)
				{
					throw new SoftwareException("Unable to locate this application for invocation", e);
				}
				finally {
					try { dataClient.disconnect(); } catch(Exception e) {};
				}
			}
		}
	}
	
	/**
	 * Returns true if the user has been granted write or above permissions on the
	 * {@link Software} object.
	 * 
	 * @param software
	 * @param username
	 * @return
	 */
	public static boolean isManageableByUser(Software software, String username)
	{
		if (software == null) {
			throw new SoftwareException("No shared software found matching that name");
		} 
		else {
			SoftwarePermissionManager pemManager = new SoftwarePermissionManager(software);
			return pemManager.canWrite(username);
		}
	}
	
	/**
	 * Checks permissions and availability of the {@link Software} for the given user. This
	 * check takes {@link Software#isPubliclyAvailable()} into account in the decison 
	 * making process.
	 * 
	 * @param software
	 * @param username
	 * @return
	 */
	public static boolean isVisibleByUser(Software software, String username)
	{
		if (software == null) {
			throw new SoftwareException("No shared software found matching that name");
		} 
		else {
			//String wrapperPath = software.getDeploymentPath() + '/' + software.getExecutablePath();
			
			if (ServiceUtils.isAdmin(username)) {
				return true;
			}
			else if (software.isPubliclyAvailable()) {
				return software.isAvailable();
			}
			else if (software.getOwner().equals(username)) {
				return true;
			} 
			else {
				SoftwarePermissionManager pemManager = new SoftwarePermissionManager(software);
				return pemManager.canRead(username);
			}
		}
	}
	
	/**
	 * Deletes a {@link Software} entity from the db. This should not be used, rather
	 * use a soft delete to preserve relationships and provenance.
	 * @param software
	 */
	public static void deleteApplication(Software software, String username)
	{	
		try {
			
			software.setAvailable(false);
			SoftwareDao.persist(software);
			
			SoftwareEventProcessor eventProcessor = new SoftwareEventProcessor();
			eventProcessor.processSoftwareContentEvent(software, 
					SoftwareEventType.DELETED,
					"App was deleted by user " + username, 
					username);
			
		} catch (Throwable e) {
			throw new SoftwareResourceException(500,
					"Unexpected error deleting app.", e);
		}
	}

	/**
	 * Verifies that the given user can publish the given Software. Requirements 
	 * are that the user must have RoleType.PUBLISH on the Software.executionSystem
	 * and RoleType.USER on the Software.storageSystem.
	 * 
	 * @param username
	 * @param newSoftware
	 * @return
	 * @throws SystemUnknownException 
	 */
	public static boolean userCanPublish(String username, Software newSoftware) throws SystemUnknownException
	{
		if (newSoftware.isPubliclyAvailable()) {
			return ServiceUtils.isAdmin(username); 
		} else if (newSoftware.getExecutionSystem() == null) {
		    throw new SystemUnknownException("No execution system given.");
		} else {
			return ServiceUtils.isAdmin(username) || (
					newSoftware.getExecutionSystem().getUserRole(username).canPublish());
		}
	}

	/**
	 * Deletes a software record and every resource associated with it.
	 * 
	 * @param software
	 */
	public static void eraseSoftware(Software software, String username) throws SoftwareResourceException
	{
		// hide jobs using this app
		try {
			String sql = "update jobs set visible = 0 where software_name = :softwareName and tenant_id = :tenantId";
			Session session = HibernateUtil.getSession();
			session.clear();
			session.createSQLQuery(sql)
				.setString("softwareName", software.getUniqueName())
				.setString("tenantId", software.getTenantId())
				.executeUpdate();
			
			SoftwareEventProcessor eventProcessor = new SoftwareEventProcessor();
			eventProcessor.processSoftwareContentEvent(software, 
					SoftwareEventType.DELETED,
					"App was deleted by user " + username, 
					username);
			
		} catch (Throwable e) {
			throw new SoftwareResourceException(400,
					"Failed to erase app. Unable to delete associated jobs.", e);
		}
		
		// delete notifications using this app
		NotificationDao ndao = new NotificationDao();
		try {
			List<Notification> appNotifications = ndao.getActiveNotificationsForUuid(software.getUuid());
			for (Notification n: appNotifications) 
			{
				ndao.delete(n);
			}
		} catch (Throwable e) {
			throw new SoftwareResourceException(400,
					"Failed to erase app. Unable to delete associated notifications.", e);
		}
		
		SoftwareDao.delete(software);
	}

	/**
	 * Pulls the software unique id out of the JSON object. If there is 
	 * any problem, the exception is swallowed and null returned.
	 * 
	 * @param json
	 * @return softwareUniqueId if present, null otherwise.
	 */
	public static String getSoftwareNameFromJSON(JSONObject json)
	{
		try {
			return String.format("%s-%s", 
								json.getString("name"),
								json.getString("version"));
		} catch (JSONException e) {
			return null;
		}
	}

	/**
	 * Enables an app from disabled status
	 * 
	 * @param software
	 * @param username
	 * @return
	 * @throws SoftwareException
	 */
	public static Software enableSoftware(Software software, String username)
	throws SoftwareException
	{	
		if (software == null) {
			throw new SoftwareException("No disabled software found matching the given id");
		} else if (software.isAvailable()) {
			return software;
		}
		
		software.setAvailable(true);
		SoftwareDao.persist(software);
		
		SoftwareEventProcessor eventProcessor = new SoftwareEventProcessor();
		eventProcessor.processSoftwareContentEvent(software, 
				SoftwareEventType.RESTORED,
				"App was restored by user " + username, 
				username);
		
		return software;
	}
	
	/**
	 * Disables an app from disabled status
	 * 
	 * @param software
	 * @param username
	 * @return
	 * @throws SoftwareException
	 */
	public static Software disableSoftware(Software software, String username)
	throws SoftwareException
	{	
		if (software == null) {
			throw new SoftwareException("No disabled software found matching the given id");
		} else if (!software.isAvailable()) {
			return software;
		}
		
		software.setAvailable(false);
		SoftwareDao.persist(software);
		
		SoftwareEventProcessor eventProcessor = new SoftwareEventProcessor();
		eventProcessor.processSoftwareContentEvent(software, 
				SoftwareEventType.DISABLED,
				"App was disabled by user " + username, 
				username);
		
		
		return software;
	}
}
