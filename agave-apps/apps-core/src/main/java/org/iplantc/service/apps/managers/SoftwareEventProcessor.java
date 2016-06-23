/**
 * 
 */
package org.iplantc.service.apps.managers;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.dao.SoftwareEventDao;
import org.iplantc.service.apps.exceptions.SoftwareEventPersistenceException;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwareEvent;
import org.iplantc.service.apps.model.SoftwarePermission;
import org.iplantc.service.apps.model.enumerations.SoftwareEventType;
import org.iplantc.service.common.exceptions.EntityEventProcessingException;
import org.iplantc.service.notification.managers.NotificationManager;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.transfer.model.enumerations.PermissionType;
import org.json.JSONException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Handles sending and propagation of events on {@link Software} objects.
 * 
 * @author dooley
 *
 */
public class SoftwareEventProcessor {
	private static final Logger log = Logger.getLogger(SoftwareEventProcessor.class);
	
	private ObjectMapper mapper = new ObjectMapper();
	private SoftwareEventDao entityEventDao;
	private SoftwareDao dao;
	
	public SoftwareEventProcessor(){
		setEntityEventDao(new SoftwareEventDao());
		setDao(new SoftwareDao());
	}
	
	/**
	 * @return the dao
	 */
	public SoftwareDao getDao() {
		return new SoftwareDao();
	}

	/**
	 * @param dao the dao to set
	 */
	public void setDao(SoftwareDao dao) {
		this.dao = dao;
	}
	
	/**
	 * @return the entityEventDao
	 */
	public SoftwareEventDao getEntityEventDao() {
		return entityEventDao;
	}

	/**
	 * @param dao the historyDao to set
	 */
	public void setEntityEventDao(SoftwareEventDao entityEventDao) {
		this.entityEventDao = entityEventDao;
	}
	
	/**
	 * Generates notification events for content-related changes to a 
	 * {@link RemoteSystem}. This could be a CRUD operation or a change
	 * to the credentials.
	 * 
	 * @param software
	 * @param event
	 * @return the {@link SoftwareEvent} updated with the association to the {@link Software}
	 */
	public SoftwareEvent processSoftwareContentEvent(Software software, SoftwareEventType eventType, String description, String createdBy) {
		
		SoftwareEvent event = null;
		
		try {
			if (software == null) {
				throw new EntityEventProcessingException("Valid app must be provided to process event.");
			}
			
			if (eventType == null) {
				throw new EntityEventProcessingException("Valid app event type must be provided to process event.");
			}
			
			ObjectNode json = mapper.createObjectNode();
			try {
			    json.put("app", mapper.readTree(software.toJSON()));
			} catch (Throwable e) {
			    log.error(String.format("Failed to serialize app "
			            + "%s to json for %s event notification", 
			            software.getUniqueName(), eventType.name()), e);
			}
			
			event = new SoftwareEvent(software, eventType, description, createdBy);
			entityEventDao.persist(event);
			
			fireNotification(software.getUuid(), event.getStatus(), event.getCreatedBy(), json);
			
			if (eventType.isStatusEvent()) {
				doSystemEvents(software, event.getStatus(), event.getCreatedBy(), json);
			}
		}
		catch (EntityEventProcessingException e) {
			log.error(e.getMessage());
		}
		catch (SoftwareEventPersistenceException e) {
			log.error("Failed to persist history event for software " + software.getUuid(), e);
		}
		
		return event;	
	}
	
	/**
	 * Generates notification events for permission-related changes to a 
	 * {@link Software}. Permission changes are not propagated to the associated
	 * resources.
	 * 
	 * @param software
	 * @param permission
	 * @param event
	 * @return the {@link SoftwareEvent}
	 * 
	 * @throws SoftwareEventProcessingException
	 */
	public SoftwareEvent processPermissionEvent(Software software, SoftwarePermission softwarePermission, String createdBy) 
	{
		SoftwareEvent event = null;
		
		try {
			if (software == null) {
				throw new EntityEventProcessingException("Valid app must be provided to process event.");
			}
			
			if (StringUtils.isEmpty(createdBy)) {
				throw new EntityEventProcessingException("Valid username must be provided to process event.");
			}
			
			SoftwareEventType eventType = null;
			String eventMessage = null;
			
			if (softwarePermission == null) {
				eventType = SoftwareEventType.PERMISSION_REVOKE;
				eventMessage = "All permissions were cleared by "  + createdBy;
			}
			else if (softwarePermission.getPermission() == PermissionType.NONE) { 
				eventType = SoftwareEventType.PERMISSION_REVOKE;
				eventMessage = String.format("User %s had their permissions revoked by %s",
						softwarePermission.getUsername(),
						createdBy);
			} else {
				eventType = SoftwareEventType.PERMISSION_GRANT;
				eventMessage = String.format("User %s was granted the permission of %s by %s", 
						softwarePermission.getUsername(), 
						softwarePermission.getPermission().name(),
						createdBy);
			}
			
			ObjectNode json = null;
			try {
			    json = mapper.createObjectNode();
		    	json.put("app", mapper.readTree(software.toJSON()));
			    json.put("permission", mapper.readTree(softwarePermission.toJSON()));
			} catch (Throwable e) {
			    log.error(String.format("Failed to serialize app "
			            + "%s to json for %s event notification", 
			            software.getUuid(), eventType.name()), e);
			    json = null;
			}
			
			event = new SoftwareEvent(software, 
					  eventType, 
					  eventMessage,
					  createdBy);
			
			entityEventDao.persist(event);
	
			fireNotification(software.getUuid(), eventType.name(), createdBy, json);
			
			// alert the job in case the job owner just had their app permissions revoked
			doUserJobEvents(software, eventType.name(), softwarePermission.getUsername(), createdBy, json);
			
			// TODO: send event to the user
			// We do not fire delegated app permission events on associated uuids
		}
		catch (EntityEventProcessingException e) {
			log.error(e.getMessage());
		}
		catch (SoftwareEventPersistenceException e) {
			log.error("Failed to persist history event for app " + software.getUuid(), e);
		}
		
		return event;
	}
	
	/**
	 * Propagates change of public/private scope to associated objects and 
	 * writes an event to the relevant software histories. The events thrown here
	 * are rather broad since taking a app offline may impact active
	 * transfers, jobs, apps, monitors, scheduled tasks, files, etc. 
	 *
	 * @param privateSoftware
	 * @param eventType
	 * @param createdBy
	 * @return
	 */
	public SoftwareEvent processPublishEvent(Software privateSoftware, Software publishedSoftware, String createdBy) {
		SoftwareEvent event = null;
		
		try {
			if (privateSoftware == null) {
				throw new EntityEventProcessingException("Valid original app must be provided to process event.");
			}
			
			if (publishedSoftware == null) {
				throw new EntityEventProcessingException("Valid published app must be provided to process event.");
			}
			
			if (StringUtils.isEmpty(createdBy)) {
				throw new EntityEventProcessingException("Valid username must be provided to process event.");
			}
			
			// notify the old app
			String description = String.format("App was published by %s as %s. The published asset checksum is %s", 
					createdBy, publishedSoftware.getUniqueName(), publishedSoftware.getChecksum() == null ? "unavailable" : publishedSoftware.getChecksum());
    		
    		processSoftwareContentEvent(privateSoftware, SoftwareEventType.PUBLISHED, description, createdBy);
			
    		
    		// notify the newly created public app
    		description = String.format("App was created by %s as a result of publishing %s. The asset checksum is %s", 
    				createdBy, privateSoftware.getUniqueName(),  publishedSoftware.getChecksum() == null ? "unavailable" : publishedSoftware.getChecksum());
            
    		processSoftwareContentEvent(publishedSoftware, SoftwareEventType.CREATED, description, createdBy);
    		
    		// TODO: Throw events on the source and destination folders. Create LogicalFile records if not already present
    		
    		// notify all old versions of public app
    		String sPublishedAppJson = null;
			ObjectNode publishedAppJson = null;
    		try {
				publishedAppJson = (ObjectNode)mapper.createObjectNode().set("app", mapper.readTree(publishedSoftware.toJSON()));
				sPublishedAppJson = publishedAppJson.toString();
				
			} catch (IOException | JSONException e) {
				log.error(String.format("Failed to serialize app "
			            + "%s to json for %s event notification", 
			            publishedSoftware.getUniqueName(), SoftwareEventType.REPUBLISHED.name()), e);
			}
			
			// notify the target system of the published event as well as the created one
    		doSystemEvents(publishedSoftware, SoftwareEventType.PUBLISHED.name(), createdBy, publishedAppJson);
    		
    		List<String> appuuids = dao.getPreviousVersionsOfPublshedSoftware(publishedSoftware);
    		for (String previousPublicSoftwareVersionsUuid: appuuids) {
    			description = String.format("A new version of this app, %s, was created by %s as a result of publishing %s. The published asset checksum is %s", 
        				publishedSoftware.getUniqueName(), createdBy, privateSoftware.getUniqueName(), publishedSoftware.getChecksum() == null ? "unavailable" : publishedSoftware.getChecksum());
                    			
    			try {
	    			event = new SoftwareEvent(SoftwareEventType.REPUBLISHED.name(), description, createdBy);
	    			event.setSoftwareUuid(previousPublicSoftwareVersionsUuid);
	    			getEntityEventDao().persist(event);
    			}
    			catch (SoftwareEventPersistenceException e) {
    				log.error("Failed to save republished history event to " + previousPublicSoftwareVersionsUuid);
    			}
    			
    			fireNotification(previousPublicSoftwareVersionsUuid, event.getStatus(), event.getCreatedBy(), sPublishedAppJson);
    		}
		}
		catch (EntityEventProcessingException e) {
			log.error(e.getMessage());
		} 
		
		return event;
	}
	
	/**
	 * Raises events and records history of an app being cloned. This is
	 * useful for documenting the origin of apps and alerting users to 
	 * new versions and revisions. The events thrown here
	 * are rather broad since cloning an app may impact active
	 * transfers, jobs, other apps, monitors, scheduled tasks, files, etc. 
	 *
	 * @param privateSoftware
	 * @param publishedSoftware
	 * @param eventType
	 * @param createdBy
	 * @return
	 */
	public SoftwareEvent processCloneEvent(Software originalSoftware, Software clonedSoftware, String createdBy) {
		SoftwareEvent event = null;
		
		try {
			if (originalSoftware == null) {
				throw new EntityEventProcessingException("Valid original app must be provided to process event.");
			}
			
			if (clonedSoftware == null) {
				throw new EntityEventProcessingException("Valid published app must be provided to process event.");
			}
			
			if (StringUtils.isEmpty(createdBy)) {
				throw new EntityEventProcessingException("Valid username of must be provided to process event.");
			}
			
			// notify the old app
			String description = String.format("App was cloned by %s as %s", 
					createdBy, clonedSoftware.getUniqueName());
    		processSoftwareContentEvent(originalSoftware, SoftwareEventType.CLONED, description, createdBy);
			
    		
    		// notify the newly created public app
    		description = String.format("App was created by %s as a result of cloning %s", 
    				createdBy, originalSoftware.getUniqueName());
    		
    		processSoftwareContentEvent(clonedSoftware, SoftwareEventType.CREATED, description, createdBy);
    		
    		// notify all old versions of public app
    		ObjectNode clonedAppJson = null;
			try {
				clonedAppJson = (ObjectNode)mapper.createObjectNode().set("app", mapper.readTree(clonedSoftware.toJSON()));
			} catch (IOException | JSONException e) {
				log.error(String.format("Failed to serialize app "
			            + "%s to json for %s event notification", 
			            clonedSoftware.getUniqueName(), SoftwareEventType.CLONED.name()), e);
			}
    		
    		doSystemEvents(clonedSoftware, SoftwareEventType.CLONED.name(), createdBy, clonedAppJson);
    		
    		// TODO: Throw events on the source and destination folders. Create LogicalFile records if not already present
		}
		catch (EntityEventProcessingException e) {
			log.error(e.getMessage());
		}
		
		return event;
	}

	/**
	 * Propagates change of status events to associated objects and 
	 * writes an event to the app history. The events thrown here
	 * are rather broad since taking an app offline may impact active
	 * jobs, systems, workflows, etc. 
	 * 
	 * @param software
	 * @param oldStatus
	 * @param newStatus
	 * @param createdBy
	 */
	public SoftwareEvent processStatusChangeEvent(Software software, SoftwareEventType oldStatus, SoftwareEventType newStatus, String createdBy) {
		
		SoftwareEvent event = null;
		
		try {
			if (software == null) {
				throw new EntityEventProcessingException("Valid app must be provided to process event.");
			}
			
			if (oldStatus == null) {
				throw new EntityEventProcessingException("Prior app status must be provided to process event.");
			}
			
			if (newStatus == null) {
				throw new EntityEventProcessingException("New app status must be provided to process event.");
			}
			
			ObjectNode json = null;
			try {
			    json = mapper.createObjectNode();
			    json.set("app", mapper.readTree(software.toJSON()));
			} catch (Throwable e) {
			    log.error(String.format("Failed to serialize app "
			            + "%s to json for %s event notification", 
			            software.getUuid(), newStatus.name()), e);
			    json = null;
			}
			
			event = new SoftwareEvent(software, 
									  newStatus,
									  String.format("App status changed from %s to %s", oldStatus.name(), newStatus.name()), 
									  createdBy);
			entityEventDao.persist(event);
			
			// process the primary object event
			fireNotification(software.getUuid(), newStatus.name(), createdBy, json);
			
			// process systems tied to the app. we don't take action here. we simply give users the info they need
			// to decide what they want their policy to be
			doSystemEvents(software, newStatus.name(), createdBy, json);
			
			doJobEvents(software, newStatus.name(), createdBy, json);
		}
		catch (EntityEventProcessingException e) {
			log.error(e.getMessage());
		}
		catch (SoftwareEventPersistenceException e) {
			log.error("Failed to persist history event for app " + software.getUuid(), e);
		}
		
		return event;
	}
	
	/**
	 * Looks up all active jobs for a given {@link Software#getUniqueName()} and
	 * triggers software events on them.
	 * 
	 * @param software
	 * @param eventName
	 * @param createdBy
	 * @param jsonNode
	 * @return
	 */
	public int doJobEvents(Software software, String eventName, String createdBy, ObjectNode jsonNode) {
		int sentEvents = 0;
		try {
			if (jsonNode == null) {
				jsonNode = mapper.createObjectNode();
			}
			for (String uuid: dao.getActiveJobsForSoftware(software)) {
				fireNotification(uuid, "APP_" + eventName, createdBy, 
						(ObjectNode)jsonNode.deepCopy().set("job", mapper.createObjectNode().put("id", uuid)));
				sentEvents++;
			}
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return sentEvents;
	}
	
	/**
	 * Looks up all active jobs for a given {@link Software#getUniqueName()} and
	 * triggers software events on them.
	 * 
	 * @param software
	 * @param eventName
	 * @param createdBy
	 * @param jsonNode
	 * @return
	 */
	public int doUserJobEvents(Software software, String eventName, String jobOwner, String createdBy, ObjectNode jsonNode) {
		int sentEvents = 0;
		try {
			if (jsonNode == null) {
				jsonNode = mapper.createObjectNode();
			}
			for (String uuid: dao.getActiveUserJobsForSoftware(software, jobOwner)) {
				fireNotification(uuid, "APP_" + eventName, createdBy, 
						(ObjectNode)jsonNode.deepCopy().set("job", mapper.createObjectNode().put("id", uuid)));
				sentEvents++;
			}
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return sentEvents;
	}
	
	/**
	 * Triggers events on the {@link ExecutionSystem} tied to this {@Software} object.
	 * 
	 * @param software
	 * @param eventName
	 * @param createdBy
	 * @param jsonNode
	 * @return
	 */
	public void doSystemEvents(Software software, String eventName, String createdBy, ObjectNode jsonNode) {
		try {
			if (jsonNode == null) {
				jsonNode = mapper.createObjectNode();
			}
			
			fireNotification(software.getExecutionSystem().getUuid(), "APP_" + eventName, createdBy,
					(ObjectNode)jsonNode.deepCopy().set("system", mapper.readTree(software.getExecutionSystem().toJSON())));
			
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
	
	/**
	 * Writes one or more notification events to the queue for publication to all 
	 * subscribers of those events on this {@link Software}.
	 * 
	 * @param associatedUuid
	 * @param json
	 * @return the number of events sent.
	 */
	protected int fireNotification(String associatedUuid, String eventStatus, String createdBy, ObjectNode json) {
		return fireNotification(associatedUuid, eventStatus, createdBy, json == null ? null : json.toString());
	}
	
	/**
	 * Writes one or more notification events to the queue for publication to all 
	 * subscribers of those events on this {@link Software}.
	 * 
	 * @param associatedUuid
	 * @param json
	 * @return the number of events sent.
	 */
	protected int fireNotification(String associatedUuid, String eventStatus, String createdBy, String json) {
		try {
            return NotificationManager.process(associatedUuid, eventStatus, createdBy, json == null ? null : json.toString());
        }
        catch (Throwable e) {
            log.error(String.format("Failed to send app notification "
                    + " to %s to on a %s event", 
                    associatedUuid, eventStatus), e);
            return 0;
        }
	}
}
