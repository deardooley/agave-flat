package org.iplantc.service.systems.events;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.exceptions.EntityEventPersistenceException;
import org.iplantc.service.common.exceptions.EntityEventProcessingException;
import org.iplantc.service.notification.managers.NotificationManager;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.RemoteSystemEventProcessingException;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.RemoteSystemEvent;
import org.iplantc.service.systems.model.SystemRole;
import org.iplantc.service.systems.model.enumerations.RoleType;
import org.iplantc.service.systems.model.enumerations.SystemEventType;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.internal.filter.ValueNode.JsonNode;

/**
 * Handles sending and propagation of events on {@link RemoteSystem} objects.
 * Currently it only sends notification events and does not persist the 
 * {@link RemoteSystemEvent} with the {@link RemoteSystem} due to the hibernate
 * managed relationship.
 * 
 * TODO: Refactor this as an async factory using vert.x
 *
 * @author dooley
 *
 */
public class RemoteSystemEventProcessor {
	private static final Logger log = Logger.getLogger(RemoteSystemEventProcessor.class);
	
	private ObjectMapper mapper = new ObjectMapper();
	SystemDao dao;
	SystemHistoryEventDao entityEventDao;
	
	public RemoteSystemEventProcessor(){
		entityEventDao = new SystemHistoryEventDao();
		dao = new SystemDao();
	}
	
	/**
	 * @return the dao
	 */
	public SystemDao getDao() {
		return dao;
	}

	/**
	 * @param dao the dao to set
	 */
	public void setDao(SystemDao dao) {
		this.dao = dao;
	}
	
	/**
	 * @return the entityEventDao
	 */
	public SystemHistoryEventDao getEntityEventDao() {
		return entityEventDao;
	}

	/**
	 * @param dao the historyDao to set
	 */
	public void setEntityEventDao(SystemHistoryEventDao entityEventDao) {
		this.entityEventDao = entityEventDao;
	}
	
	/**
	 * Generates notification events for content-related changes to a 
	 * {@link RemoteSystem}. This could be a CRUD operation or a change
	 * to the credentials.
	 * 
	 * @param system
	 * @param event
	 * @return the {@link SystemHistoryEvent} updated with the association to the {@link RemoteSystem}
	 */
	public SystemHistoryEvent processSystemUpdateEvent(RemoteSystem system, SystemEventType eventType, String createdBy) {
		
		SystemHistoryEvent event = null;
		
		try {
			if (system == null) {
				throw new EntityEventProcessingException("Valid system must be provided to process event.");
			}
			
			if (eventType == null) {
				throw new EntityEventProcessingException("Valid system event type must be provided to process event.");
			}
			
			String json;
			try {
			    json = mapper.createObjectNode()
			    		.set("system", mapper.readTree(system.toJSON()))
			    		.toString();
			} catch (Throwable e) {
			    log.error(String.format("Failed to serialize system "
			            + "%s to json for %s event notification", 
			            system.getUuid(), eventType.name()), e);
			    json = null;
			}
			
			event = new SystemHistoryEvent(system.getUuid(), eventType, eventType.getDescription(), createdBy);
			entityEventDao.persist(event);
			
			fireNotification(system.getUuid(), event.getStatus(), event.getCreatedBy(), json);
		}
		catch (EntityEventProcessingException e) {
			log.error(e.getMessage());
		}
		catch (EntityEventPersistenceException e) {
			log.error("Failed to persist history event for system " + system.getUuid(), e);
		}
		
		return event;	
	}
	
	/**
	 * Generates notification events for permission-related changes to a 
	 * {@link RemoteSystem}. Permission changes are not propagated to the associated
	 * resources.
	 * 
	 * @param system
	 * @param permission
	 * @param event
	 * @return the {@link RemoteSystemEvent}
	 * 
	 * @throws RemoteSystemEventProcessingException
	 */
	public SystemHistoryEvent processPermissionEvent(RemoteSystem system, SystemRole systemRole, String createdBy) 
	{
		SystemHistoryEvent event = null;
		
		try {
			if (system == null) {
				throw new EntityEventProcessingException("Validsysteme must be provided to process event.");
			}
			
			if (systemRole == null) {
				throw new EntityEventProcessingException("Valid system role must be provided to process event.");
			}
			
			if (StringUtils.isEmpty(createdBy)) {
				throw new EntityEventProcessingException("Valid username must be provided to process event.");
			}
			
			SystemEventType eventType = null;
			String eventMessage = null;
			if (systemRole.getRole() == RoleType.NONE) { 
				eventType = SystemEventType.ROLES_REVOKE;
				eventMessage = String.format("User %s had their roles revoked by %s",
						systemRole.getUsername(),
						createdBy);
			} else {
				eventType = SystemEventType.ROLES_GRANT;
				eventMessage = String.format("User %s was granted the role of %s by %s", 
						systemRole.getUsername(), 
						systemRole.getRole().name(),
						createdBy);
			}
			
			ObjectNode json = null;
			try {
			    json = mapper.createObjectNode();
		    	json.set("system", mapper.readTree(system.toJSON()));
			    json.set("role", systemRole.toJsonNode(system));
			} catch (Throwable e) {
			    log.error(String.format("Failed to serialize system "
			            + "%s to json for %s event notification", 
			            system.getUuid(), eventType.name()), e);
			    json = null;
			}
			
			event = new SystemHistoryEvent(system.getUuid(), 
					  eventType, 
					  eventMessage,
					  createdBy);
			entityEventDao.persist(event);
	
			fireNotification(system.getUuid(), eventType.name(), createdBy, json);
			
			// send update notifications to each software item on the system
			doSoftwareEvents(system, eventType.name(), systemRole.getUsername(), json);
			
			// TODO: send event to the user
			// We do not fire delegated system permission events on associated uuids
		}
		catch (EntityEventProcessingException e) {
			log.error(e.getMessage());
		}
		catch (EntityEventPersistenceException e) {
			log.error("Failed to persist history event for system " + system.getUuid(), e);
		}
		
		return event;
	}
	
	/**
	 * Propagates change of public/private scope to associated objects and 
	 * writes an event to the system history. The events thrown here
	 * are rather broad since taking a system offline may impact active
	 * transfers, jobs, apps, monitors, scheduled tasks, files, etc. 
	 *
	 * @param system
	 * @param eventType
	 * @param createdBy
	 * @return
	 */
	public SystemHistoryEvent processPublishEvent(RemoteSystem system, SystemEventType eventType, String createdBy) {
		SystemHistoryEvent event = null;
		
		try {
			if (system == null) {
				throw new EntityEventProcessingException("Valid system must be provided to process event.");
			}
			
			if (eventType == null) {
				throw new EntityEventProcessingException("Valid system event type must be provided to process event.");
			}
			
			ObjectNode json = null;
			try {
			    json = mapper.createObjectNode();
			    json.set("system", mapper.readTree(system.toJSON()));
			} catch (Throwable e) {
			    log.error(String.format("Failed to serialize system "
			            + "%s to json for %s event notification", 
			            system.getUuid(), eventType.name()), e);
			    json = null;
			}
			
			event = new SystemHistoryEvent(system.getUuid(), eventType, eventType.getDescription(), createdBy);
			entityEventDao.persist(event);
			
			// process the primary object event
			fireNotification(system.getUuid(), eventType.name(), createdBy, json.toString());
			
			// process software tied to the system. we don't take action here. we simply give users the info they need
			// to decide what they want their policy to be. 
			// TODO: a separate process should disable public apps when the system is unpublished
			doSoftwareEvents(system, eventType.name(), createdBy, json);
			
			// process the content change events tied to the status field changing. Queues should know when a system goes down
//			processBatchQueueEvents(system, systemStatus, createdBy);
			
			// process jobs tied to the system. 
//			doJobEvents(system, systemStatus, createdBy, json);
			
			// process transfers tied to the system. 
//			doTransferEvents(system, systemStatus, createdBy, json);
			
		}
		catch (EntityEventProcessingException e) {
			log.error(e.getMessage());
		}
		catch (EntityEventPersistenceException e) {
			log.error("Failed to persist history event for system " + system.getUuid(), e);
		}
		
		return event;
	}

	/**
	 * Propagates change of status events to associated objects and 
	 * writes an event to the system history. The events thrown here
	 * are rather broad since taking a system offline may impact active
	 * transfers, jobs, apps, monitors, scheduled tasks, files, etc. 
	 * 
	 * @param system
	 * @param oldStatus
	 * @param newStatus
	 * @param createdBy
	 */
	public SystemHistoryEvent processStatusChangeEvent(RemoteSystem system, SystemStatusType oldStatus, SystemStatusType newStatus, String createdBy) {
		
		SystemHistoryEvent event = null;
		
		try {
			if (system == null) {
				throw new EntityEventProcessingException("Valid system must be provided to process event.");
			}
			
			if (oldStatus == null) {
				throw new EntityEventProcessingException("Prior system status must be provided to process event.");
			}
			
			if (newStatus == null) {
				throw new EntityEventProcessingException("New system status must be provided to process event.");
			}
			
			ObjectNode json = null;
			try {
			    json = mapper.createObjectNode();
			    json.set("system", mapper.readTree(system.toJSON()));
			} catch (Throwable e) {
			    log.error(String.format("Failed to serialize system "
			            + "%s to json for %s event notification", 
			            system.getUuid(), SystemEventType.STATUS_UPDATE.name()), e);
			    json = null;
			}
			
			event = new SystemHistoryEvent(system.getUuid(), 
										  SystemEventType.STATUS_UPDATE, 
										  String.format("System status changed from %s to %s", oldStatus.name(), newStatus.name()), 
										  createdBy);
			entityEventDao.persist(event);
			
			// process the primary object event
			fireNotification(system.getUuid(), SystemEventType.STATUS_UPDATE.name(), createdBy, json);
			
			// process software tied to the system. we don't take action here. we simply give users the info they need
			// to decide what they want their policy to be
			doSoftwareEvents(system, SystemEventType.STATUS_UPDATE.name(), createdBy, json);
			
			
			// process the content change events tied to the status field changing. Queues should know when a system goes down
//			processBatchQueueEvents(system, systemStatus, createdBy);
			
			// process jobs tied to the system. 
//			doJobEvents(system, systemStatus, createdBy, json);
			
			// process transfers tied to the system. 
//			doTransferEvents(system, systemStatus, createdBy, json);
			
		}
		catch (EntityEventProcessingException e) {
			log.error(e.getMessage());
		}
		catch (EntityEventPersistenceException e) {
			log.error("Failed to persist history event for system " + system.getUuid(), e);
		}
		
		return event;
	}
	
	/**
	 * Looks up {@link Software} for a given {@link RemoteSystem#getSystemId()} and
	 * processes system events for all apps.
	 * 
	 * @param system
	 * @param eventName
	 * @param createdBy
	 * @param jsonNode
	 * @return
	 */
	public int doSoftwareEvents(RemoteSystem system, String eventName, String createdBy, ObjectNode jsonNode) {
		int sentEvents = 0;
		try {
			if (jsonNode == null) {
				jsonNode = mapper.createObjectNode();
			}
			for (String uuid: dao.getUserOwnedAppsForSystemId(createdBy, system.getId())) {
				fireNotification(uuid, "SYSTEM_" + eventName, createdBy, 
						(ObjectNode)jsonNode.deepCopy().set("app", mapper.createObjectNode().put("uuid", uuid)));
				sentEvents++;
			}
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return sentEvents;
	}
	
	/**
	 * Writes one or more notification events to the queue for publication to all 
	 * subscribers of those events on this {@link RemoteSystem}.
	 * 
	 * @param associatedUuid
	 * @param json
	 * @return the number of events sent.
	 */
	protected int fireNotification(String associatedUuid, String eventStatus, String createdBy, ObjectNode json) {
		return fireNotification(associatedUuid, eventStatus, createdBy, json == null ? null : json.toString());
//		try {
//            return NotificationManager.process(associatedUuid, eventStatus, createdBy, json == null ? null : json.toString());
//        }
//        catch (Throwable e) {
//            log.error(String.format("Failed to send content system change notification "
//                    + "to %s to on a %s event", 
//                    associatedUuid, eventStatus), e);
//            return 0;
//        }
	}
	
	/**
	 * Writes one or more notification events to the queue for publication to all 
	 * subscribers of those events on this {@link RemoteSystem}.
	 * 
	 * @param associatedUuid
	 * @param json
	 * @return the number of events sent.
	 */
	private int fireNotification(String associatedUuid, String eventStatus, String createdBy, String json) {
		try {
            return NotificationManager.process(associatedUuid, eventStatus, createdBy, json == null ? null : json.toString());
        }
        catch (Throwable e) {
            log.error(String.format("Failed to send content system change notification "
                    + "to %s to on a %s event", 
                    associatedUuid, eventStatus), e);
            return 0;
        }
	}

}
