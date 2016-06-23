package org.iplantc.service.io.manager;

import org.apache.log4j.Logger;
import org.iplantc.service.io.dao.FileEventDao;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.exceptions.FileEventProcessingException;
import org.iplantc.service.io.exceptions.LogicalFileException;
import org.iplantc.service.io.model.FileEvent;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.enumerations.FileEventType;
import org.iplantc.service.notification.managers.NotificationManager;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.transfer.model.RemoteFilePermission;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Handles sending and propagation of events on {@link LogicalFile} objects.
 * Currently it only sends notification events and does not persist the 
 * {@link FileEvent} with the {@link LogicalFile} due to the hibernate
 * managed relationship.
 * 
 * TODO: Remove {@link LogicalFile#getEvents()} and the relationship.
 * TODO: Make this class the default mechansim for adding events
 * TODO: Refactor this as an async factory using vert.x
 *
 * @author dooley
 *
 */
public class FileEventProcessor {
	private static final Logger log = Logger.getLogger(FileEventProcessor.class);
	private ObjectMapper mapper = new ObjectMapper();
	
	public FileEventProcessor(){}
	
	/**
	 * Generates notification events for content-related changes to a 
	 * {@link LogicalFile}. This could be a file operation or a change
	 * to the contents of a directory.
	 * 
	 * @param logicalFile
	 * @param event
	 * @return the {@link FileEvent} updated with the association to the {@link LogicalFile}
	 */
	public FileEvent processContentEvent(LogicalFile logicalFile, FileEvent event) 
	throws FileEventProcessingException {
		
		if (logicalFile == null) {
			throw new FileEventProcessingException("Valid logical file must be provided to process event.");
		}
		
		if (event == null) {
			throw new FileEventProcessingException("Valid file event must be provided to process event.");
		}
		
		event.setLogicalFile(logicalFile);
		
		ObjectNode json = null;
		try {
		    json = (ObjectNode)mapper.createObjectNode().set("file", mapper.readTree(logicalFile.toJSON()));
		} catch (Throwable e) {
		    log.error(String.format("Failed to serialize logical file "
		            + "%s to json for %s event notification", 
		            logicalFile.getUuid(), event.getStatus()), e);
		    json = null;
		}
		log.debug(String.format("Processing %s event for %s, %s", 
				event.getStatus(), logicalFile.getUuid(), logicalFile.getPublicLink())); 
		
		NotificationManager.process(logicalFile.getUuid(), event.getStatus(), event.getCreatedBy(), json.toString());
		
		fireParentNotification(event, json);
		
		fireSystemNotification(event, json);
		
		return event;
	}
	
	/**
	 * Generates notification events for permission-related changes to a 
	 * {@link LogicalFile}. Permission changes are not propagated to the 
	 * parent folder, but they are to the system.
	 * 
	 * @param logicalFile
	 * @param permission
	 * @param event
	 * @return the {@link FileEvent} updated with the association to the {@link LogicalFile}
	 * 
	 * @throws FileEventProcessingException
	 */
	public FileEvent processPermissionEvent(LogicalFile logicalFile, RemoteFilePermission permission, FileEvent event) 
	throws FileEventProcessingException 
	{
		if (logicalFile == null) {
			throw new FileEventProcessingException("Valid logical file must be provided to process event.");
		}
		
		if (permission == null) {
			throw new FileEventProcessingException("Valid remote file permission must be provided to process event.");
		}
		
		if (event == null) {
			throw new FileEventProcessingException("Valid file event must be provided to process event.");
		}
		
		event.setLogicalFile(logicalFile);
		
		ObjectNode json = null;
		try {
		    json = mapper.createObjectNode();
	    	json.set("file", mapper.readTree(logicalFile.toJSON()));
		    json.set("permission", mapper.readTree(permission.toJSON(logicalFile.getAgaveRelativePathFromAbsolutePath(), logicalFile.getSystem().getSystemId())));
		} catch (Throwable e) {
		    log.error(String.format("Failed to serialize logical file "
		            + "%s to json for %s event notification", 
		            logicalFile.getUuid(), event.getStatus()), e);
		    json = null;
		}
		
		NotificationManager.process(logicalFile.getUuid(), event.getStatus(), event.getCreatedBy(), json.toString());
		
		fireSystemNotification(event, json);
		
		return event;
	}
	
	
	/**
	 * Publishes a FILE_CONTENT_CHANGE or FILE_PERMISSION_* event to the  
	 * system on which the {@link LogicalFile} resides. Only events where
	 * {@link FileEventType#isContentChangeEvent()} or {@link FileEventType#isPermissionChangeEvent()}
	 * are true get published.
	 * 
	 * @param event
	 * @param json
	 * @return
	 */
	private void fireSystemNotification(FileEvent event, ObjectNode json) {
		try {
            RemoteSystem system = event.getLogicalFile().getSystem();
            if (system != null) {
            	ObjectMapper mapper = new ObjectMapper();
            	json.set("system", mapper.readTree(system.toJSON()));
            	
            	if (FileEventType.isContentChangeEvent(event.getStatus()))
            		NotificationManager.process(system.getUuid(), "FILE_" + FileEventType.CONTENT_CHANGE.name(), event.getCreatedBy(), json.toString());
            	else if (FileEventType.isPermissionChangeEvent(event.getStatus())) {
            		NotificationManager.process(system.getUuid(), "FILE_" + event.getStatus().toUpperCase(), event.getCreatedBy(), json.toString());
            	}
            }
        }
        catch (Throwable e) {
            log.error(String.format("Failed to send content change notification "
                    + "to for logical file %s to parent on %s event", 
                    event.getLogicalFile().getUuid(), event.getStatus()), e);
        }
	}

	/**
	 * Publishes a CONTENT_CHANGE event to the parent of a {@link FileEvent#getLogicalFile()}.
	 * This only fires for events satisfying {@link FileEventType#isContentChangeEvent()}
	 * 
	 * @param event
	 * @param json
	 */
	private void fireParentNotification(FileEvent event, ObjectNode json) {
		// notify parent of the changed content
		if (FileEventType.isContentChangeEvent(event.getStatus()))
		{
            LogicalFile parent = null;
            try {
                parent = LogicalFileDao.findParent(event.getLogicalFile());
                if (parent != null) {
                    NotificationManager.process(parent.getUuid(), "CONTENT_CHANGE", event.getCreatedBy(), json.toString());
                }
            }
            catch (Throwable e) {
                log.error(String.format("Failed to send content change notification "
                        + "to for logical file %s to parent on %s event", 
                        event.getLogicalFile().getUuid(), event.getStatus()), e);
            }
		}
	}

	/**
	 * Processes and saves a content event. This delegates to {@link #processContentEvent(LogicalFile, FileEvent)}
	 * @param file
	 * @param event
	 * @throws FileEventProcessingException 
	 * @throws LogicalFileException 
	 */
	public static void processAndSaveContentEvent(LogicalFile file, FileEvent event) 
	throws LogicalFileException, FileEventProcessingException 
	{
		
		FileEventDao.persist(new FileEventProcessor().processContentEvent(file, event));
	}
	
	/**
	 * Processes and saves a permisison event. This delegates to {@link #processContentEvent(LogicalFile, FileEvent)}
	 * @param file
	 * @param permission
	 * @param event
	 * @throws FileEventProcessingException 
	 * @throws LogicalFileException 
	 */
	public static void processAndSavePermissionEvent(LogicalFile file, RemoteFilePermission permission, FileEvent event) 
	throws LogicalFileException, FileEventProcessingException 
	{
		FileEventDao.persist(new FileEventProcessor().processPermissionEvent(file, permission, event));
	}

}
