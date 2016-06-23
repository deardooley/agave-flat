package org.iplantc.service.io.manager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.exceptions.LogicalFileException;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.enumerations.FileEventType;
import org.iplantc.service.io.model.enumerations.StagingTaskStatus;
import org.iplantc.service.io.model.enumerations.TransformTaskStatus;
import org.iplantc.service.io.util.ServiceUtils;
import org.iplantc.service.notification.dao.NotificationDao;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.transfer.RemoteDataClient;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class LogicalFileManager 
{
	private String agavePath;
	private String internalUsername;
	private RemoteSystem system;
	private RemoteDataClient client;
	private boolean authenticated;
	
	public LogicalFileManager(RemoteSystem system, String agavePath)
	{
		this.system = system;
		this.agavePath = agavePath;
	}
	
	public LogicalFileManager(RemoteSystem system, String agavePath, String internalUsername)
	{
		this.system = system;
		this.agavePath = agavePath;
		this.internalUsername = internalUsername;
	}
	
	/**
	 * Has this RemoteDataClient been authenticated yet.
	 * 
	 * @return true if it has, false otherwise
	 */
	public boolean isAuthenticated() {
		return authenticated;
	}
	
	/**
	 * Null safe get of the RemoteDataClient. If it does not exist,
	 * it is created and authenticated. This will only happen once
	 * during the life of a LogicalFileManager instance.
	 * 
	 * @return pre-authenticated RemoteDataClient 
	 * @throws Exception
	 */
	private RemoteDataClient getClient() throws Exception 
	{
		if (client == null) {
			client = system.getRemoteDataClient(internalUsername);
		}
		
		if (!isAuthenticated()) {
			client.authenticate();
		}
		
		return client;
	}
	
	/**
	 * Whether a LogicalFile entry exists for this path and system.
	 * This does not check the remote system, only the persisted reference.
	 *   
	 * @return true if it exists, false otherwise
	 */
	public boolean logicalFileDoesExist()
	{
		return getLogicalFile() == null;
	}
	
	/**
	 * Returns a LogicalFile for this system and path if it exists. False 
	 * otherwise.
	 * 
	 * @return LogicalFile if the system path is known. False otherwise
	 */
	public LogicalFile getLogicalFile() 
	{
		try 
		{
			String resolvedPath = getClient().resolvePath(agavePath);
			
			return LogicalFileDao.findBySystemAndPath(system, resolvedPath);
		} 
		catch (FileNotFoundException e) {
			return null;
		}
		catch(Exception e) {
			return null;
		}
	}
	
	/**
	 * Creates a logical file from the current system, path, and the passed in owner and format.
	 * Status will be set to STAGING_COMPLETE and the name of the file/folder in the path will be
	 * the logical file name.
	 *  
	 * @param owner
	 * @param nativeFormat
	 * @return LogicalFile
	 * @throws LogicalFileException if the path is invalid or cannot be resolved to an absolute path
	 */
	public LogicalFile createLogicalFile(String owner, String nativeFormat) throws LogicalFileException 
	{
		try 
		{
			LogicalFile logicalFile = new LogicalFile(owner, 
													system, 
													null, 
													getClient().resolvePath(agavePath), 
													FilenameUtils.getName(agavePath), 
													StagingTaskStatus.STAGING_COMPLETED.name(), 
													nativeFormat);
			
			logicalFile.setInternalUsername(internalUsername);
			
			return logicalFile;
		} 
		catch (FileNotFoundException e) {
			throw new LogicalFileException("Invalid path " + agavePath);
		}
		catch(Exception e) {
			throw new LogicalFileException("Failed to resolve path to file on " + system.getSystemId());
		}
	}

	/**
	 * Retrieves the 
	 * @param system
	 * @param client
	 * @param path
	 * @return
	 */
	public static LogicalFile lookupLogicalFileByAgavePathAndSystem(RemoteSystem system, RemoteDataClient client, String path)
	{
		try 
		{
			String resolvedPath = client.resolvePath(path);
			
			return LogicalFileDao.findBySystemAndPath(system, resolvedPath);
		} 
		catch (FileNotFoundException e) {
			return null;
		}
		catch(Exception e) {
			return null;
		}
	}
	
//	public static String getAgavePath(RemoteDataClient client, LogicalFile logicalFile) 
//	throws LogicalFileException
//	{
//		try 
//		{
//			String actualRegisteredSystemRootDirectory = client.resolvePath("/");
//			String actualRegisteredSystemHomeDirectory = client.resolvePath("");
//			
//			String agaveAbsolutePath = logicalFile.getPath();
//			if (StringUtils.startsWith(logicalFile.getPath(), actualRegisteredSystemHomeDirectory)) {
//				agaveAbsolutePath = StringUtils.removeStart(agaveAbsolutePath, actualRegisteredSystemHomeDirectory);
//			} else if (StringUtils.startsWith(logicalFile.getPath(), actualRegisteredSystemRootDirectory)) {
//				agaveAbsolutePath = StringUtils.removeStart(agaveAbsolutePath, actualRegisteredSystemRootDirectory);
//			} else {
//				if (StringUtils.equalsIgnoreCase(logicalFile.getNativeFormat(), LogicalFile.DIRECTORY)) {
//					throw new LogicalFileException("This directory is no longer "
//							+ "visible because the system homeDir and or rootDir have changed.");
//				} else {
//					throw new LogicalFileException("This directory is no longer "
//							+ "visible because the system homeDir and or rootDir have changed.");
//				}
//			}
//			return agaveAbsolutePath;
//		} 
//		catch (FileNotFoundException e) {
//			return null;
//		}
//		catch (LogicalFileException e) {
//			throw e;
//		}
//		catch(Exception e) {
//			throw new LogicalFileException("Unable to resolve the logical file path " + 
//					logicalFile.getPath() + " to the system path on " + 
//					logicalFile.getSystem().getSystemId(), e);
//		}
//	}
	
//	public static void mkdirs(RemoteSystem srcSystem, RemoteDataClient srcClient, String srcPath) 
//	{
//		
//	}
	
	/**
	 * Validates and adds {@link Notifications} given in a serialized JSON object to a logical file. 
	 * The serialized value may be a single object or an array of objects. Objects must conform
	 * to the the same JSON Notification request format as would be passed to the Notifications API.
	 * The {@ode associationUuid} field can be omitted as it will be set to the value of
	 * {@link LogicalFile#getUuid()}.
	 * 
	 * @param logicalFile the logical file to which the notification will be associated
	 * @param sNotification a json serialized {@link Notification} object or array
	 * @return the registered {@link Notifications}.
	 * @throws NotificationException
	 */
	public static List<Notification> addUploadNotifications(LogicalFile logicalFile, String createdBy, String sNotification) 
	throws NotificationException 
	{
		List<Notification> notifications = new ArrayList<Notification>();
		try {
//			if (ServiceUtils.isEmailAddress(sNotification) 
//					|| ServiceUtils.isValidURL(sNotification) 
//					|| ServiceUtils.isValidPhoneNumber(sNotification)) {
//				notifications = addDefaultUploadNotifications(logicalFile, createdBy, sNotification);
//			}
			if (StringUtils.startsWithAny(sNotification,  new String[]{ "{", "[" })) {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode json = mapper.readTree(sNotification);
				
				// if they passed in a single string, we assume it was a URL and attempt to register the default set of upload
				// notifications to this URL. This provides legacy support when a simple callbackURL was given in the 
				// multipart upload request.
				if (json.isTextual()) {
					notifications = addDefaultUploadNotifications(logicalFile, createdBy, json.asText());
				}
				else if (json.isArray()) {
					notifications = addUploadNotifications(logicalFile, createdBy, (ArrayNode)json);
				} else {
					notifications.add(addUploadNotification(logicalFile, createdBy, (ObjectNode)json));
				}
			}
			else {
				notifications = addDefaultUploadNotifications(logicalFile, createdBy, sNotification);
			}
			return notifications;
		} catch (NotificationException e) {
			throw e;
		} catch (IOException e) {
			throw new NotificationException("Unable to process JSON notification object", e);
		} 
	}
	
	public static List<Notification> addDefaultUploadNotifications(LogicalFile logicalFile, String createdBy, String callbackUrl) 
	throws NotificationException 
	{
		List<Notification> notifications = new ArrayList<Notification>();
		try {
	
			FileEventType[] defaultUploadNotificationEvents = { 
					FileEventType.STAGING_COMPLETED,
					FileEventType.STAGING_FAILED,
					FileEventType.UPLOAD,
					FileEventType.TRANSFORMING_FAILED,
					FileEventType.TRANSFORMING_COMPLETED };
			List<String> validEvents = new ArrayList<String>();
			
			for (FileEventType eventType: defaultUploadNotificationEvents) {
				Notification notification = new Notification(logicalFile.getUuid(), createdBy, eventType.name(), callbackUrl, false);
				ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		        Validator validator = factory.getValidator();
		        
	        	Set<ConstraintViolation<Notification>> violations = validator.validate(notification);
	        	if (violations.isEmpty()) {
	        		notifications.add(notification);
	        		validEvents.add(eventType.name());
	        	} else {
	        		throw new NotificationException(violations.iterator().next().getMessage()); 
	        	}
			}
			NotificationDao dao = new NotificationDao();
			return dao.addUniqueNotificationForEvents(logicalFile.getUuid(), createdBy, validEvents, callbackUrl, false);
			
		} 
		catch (NotificationException e) {
			throw e; 
		}
		catch (Exception e) {
			throw new NotificationException("Unable to process default upload notifications for " + logicalFile.getPublicLink(), e);
		}
	}
	
	/**
	 * Validates and adds an {@link ArrayNode} of {@link Notification}s represented as {@link ObjectNode} 
	 * to a logical file. The {@link ObjectNode} must conform to the the same {@link Notification} request 
	 * format as would be passed to the Notifications API. The {@ode associationUuid} field can be omitted 
	 * as it will be set to the value of {@link LogicalFile#getUuid()}.
	 * 
	 * @param logicalFile the logical file to which the notification will be associated
	 * @param jsonNotifications an {@link ArrayNode} of json serialized {@link Notification} objects to add 
	 * @return the registered {@link Notifications}.
	 * @throws NotificationException
	 * Adds a single {@link Notification} for a given logical file. 
	 * @return
	 */
	public static List<Notification> addUploadNotifications(LogicalFile logicalFile, String createdBy, ArrayNode jsonNotifications) 
	{
		List<Notification> notifications = new ArrayList<Notification>();
		
		try {
			for (Iterator<JsonNode> iter = jsonNotifications.iterator(); iter.hasNext(); ) {
				JsonNode jNotification = iter.next();
				if (jNotification.isObject()) {
					Notification notification = addUploadNotification(logicalFile, createdBy, (ObjectNode)jNotification);
					notifications.add(notification);
				} else {
					throw new NotificationException("Failed to parse notification description. Notifications "
							+ "should be provided as an array of notification objects.");
				}
			}
		} catch (Exception e) {
			
		}
		return notifications;
	}
	
	/**
	 * Adds a single {@link Notification} for a given logical file.
	 * 
	 * @param logicalFile
	 * @param json
	 * @return
	 * @throws NotificationException
	 */
	public static Notification addUploadNotification(LogicalFile logicalFile, String createdBy, ObjectNode json) 
	throws NotificationException
	{	
		NotificationDao dao = new NotificationDao();
		json.put("associatedUuid", logicalFile.getUuid());
		Notification notification = Notification.fromJSON(json);
		notification.setOwner(createdBy);
		dao.persist(notification);
		return notification;
		
	}

	/**
	 * Creates a single notification from the given values. The returned
	 * notification has been persisted.
	 * 
	 * @param logicalFile
	 * @param eventName
	 * @param callbackUrl
	 * @param persistent
	 * @param createdBy
	 * @return
	 * @throws NotificationException 
	 */
	public static Notification addNotification(LogicalFile logicalFile,
			FileEventType eventType, String callbackUrl, boolean persistent,
			String createdBy) throws NotificationException {
		NotificationDao dao = new NotificationDao();
		Notification notification = new Notification(logicalFile.getUuid(), createdBy, eventType.name(), callbackUrl, persistent);
		dao.persist(notification);
		return notification;
	}
}
