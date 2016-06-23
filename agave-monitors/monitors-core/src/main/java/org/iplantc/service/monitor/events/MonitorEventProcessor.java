package org.iplantc.service.monitor.events;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.exceptions.EntityEventPersistenceException;
import org.iplantc.service.common.exceptions.EntityEventProcessingException;
import org.iplantc.service.monitor.model.Monitor;
import org.iplantc.service.monitor.model.MonitorCheck;
import org.iplantc.service.monitor.model.enumeration.MonitorEventType;
import org.iplantc.service.notification.managers.NotificationManager;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.RemoteSystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Handles sending and propagation of events on {@link Monitor} and {@link MonitorCheck} objects.
 * Currently it only sends notification events and does not persist the 
 * {@link MontiorEvent} with the {@link Montior} due to the hibernate
 * managed relationship.
 * 
 * TODO: Make this class the default mechansim for adding events
 * TODO: Refactor this as an async factory using vert.x
 *
 * @author dooley
 *
 */
public class MonitorEventProcessor {
	private static final Logger log = Logger.getLogger(MonitorEventProcessor.class);
	private ObjectMapper mapper = new ObjectMapper();
	
	public MonitorEventProcessor(){}
	
	/**
	 * Generates notification events for content-related changes to a 
	 * {@link Monitor}. This could be a CRUD operation or a change
	 * to the credentials.
	 * 
	 * @param monitor the monitor on which the even is triggered
	 * @param eventType the event being triggered
	 * @param createdBy the user who caused this event
	 * @return the {@link MontiorEvent} updated with the association to the {@link Monitor}
	 */
	public DomainEntityEvent processContentEvent(Monitor monitor, MonitorEventType eventType, String createdBy) {
		
		DomainEntityEvent event = null;
		
		try {
			if (monitor == null) {
				throw new EntityEventProcessingException("Valid monitor must be provided to process event.");
			}
			
			if (eventType == null) {
				throw new EntityEventProcessingException("Valid monitor event type must be provided to process event.");
			}
			
			if (StringUtils.isEmpty(createdBy)) {
				throw new EntityEventProcessingException("Valid monitor creator username must be provided to process event.");
			}
			
			String sJson = null;
			try {
				ObjectNode json = mapper.createObjectNode();
			    json.set("monitor", mapper.readTree(monitor.toJSON()));
			    json.set("system", mapper.readTree(monitor.getSystem().toJSON()));
			    sJson = json.toString();
			} catch (Throwable e) {
			    log.error(String.format("Failed to serialize monitor "
			            + "%s to json for %s event notification", 
			            monitor.getUuid(), eventType.name()), e);
			}
			
			// fire event on monitor
			processNotification(monitor.getUuid(), eventType.name(), createdBy, sJson);
			
			// We fire delegated monitor chec events on the system being monitored.
			processNotification(monitor.getSystem().getUuid(), "MONITOR_" + eventType.name(), createdBy, sJson);
			
			event = new DomainEntityEvent(monitor.getUuid(), eventType, eventType.getDescription() + " by " + createdBy, createdBy);
			
			try {
				new DomainEntityEventDao().persist(event);
			}
			catch (EntityEventPersistenceException e) {
				log.error(String.format("Failed to persist %s monitor event "
			            + "to history for %s", 
			            eventType.name(), monitor.getUuid()), e);
			}
		}
		catch (EntityEventProcessingException e) {
			log.error(e.getMessage());
		}
		
		return event;
	}
	
	/**
	 * Generates notification events for forced {@link MonitorCheck} outside of the  
	 * existing {@link Monitor} schedule.
	 * 
	 * @param monitor the {@link Monitor} on which the even is triggered
	 * @param createdBy the user who caused this event
	 * @return the {@link MonitorEvent} with the association to the {@link Monitor}
	 */
	public void processForceCheckEvent(Monitor monitor, String createdBy) {

		try {
			if (monitor == null) {
				throw new EntityEventProcessingException("Valid monitor must be provided to process event.");
			}
			
			if (StringUtils.isEmpty(createdBy)) {
				throw new EntityEventProcessingException("Valid monitor creator username must be provided to process event.");
			}
			
			String sJson = null;
			try {
				ObjectNode json = mapper.createObjectNode();
			    json.set("monitor", mapper.readTree(monitor.toJSON()));
			    json.set("system", mapper.readTree(monitor.getSystem().toJSON()));
			    sJson = json.toString();
			} catch (Throwable e) {
			    log.error(String.format("Failed to serialize monitor "
			            + "%s to json for %s event notification", 
			            monitor.getUuid(), MonitorEventType.FORCED_CHECK_REQUESTED.name()), e);
			}
			
			// fire event on monitor
			processNotification(monitor.getUuid(), MonitorEventType.FORCED_CHECK_REQUESTED.name(), createdBy, sJson);

			// We fire delegated monitor chec events on the system being monitored.
			processNotification(monitor.getSystem().getUuid(), "MONITOR_" + MonitorEventType.FORCED_CHECK_REQUESTED.name(), createdBy, sJson);
		}
		catch (EntityEventProcessingException e) {
			log.error(e.getMessage());
		}
	}
	
	
	/**
	 * Generates notification events for each {@link MonitorCheck} run on schedule 
	 * {@link Monitor}.
	 * 
	 * @param monitor the {@link Monitor} on which the even is triggered
	 * @param check the {@link MonitorCheck} that just ran
	 * @param eventType the event being triggered
	 * @param createdBy the user who caused this event
	 * @return the {@link MonitorEvent} with the association to the {@link Monitor}
	 */
	public DomainEntityEvent processCheckEvent(Monitor monitor, MonitorCheck lastCheck, MonitorCheck currentCheck, String createdBy) {
		
		DomainEntityEvent event = null;
		
		try {
			if (monitor == null) {
				throw new EntityEventProcessingException("Valid monitor must be provided to process event.");
			}
			
			if (currentCheck == null) {
				throw new EntityEventProcessingException("Valid monitor check must be provided to process event.");
			}
			
			if (StringUtils.isEmpty(createdBy)) {
				throw new EntityEventProcessingException("Valid monitor check creator must be provided to process event.");
			}
			
			MonitorEventType eventType = MonitorEventType.valueOfCheckStatus(currentCheck.getResult());
			
			String sJson = null;
			try {
				ObjectNode json = mapper.createObjectNode();
			    json.set("monitor", mapper.readTree(monitor.toJSON()));
			    json.set("monitorcheck", mapper.readTree(currentCheck.toJSON()));
			    json.set("system", mapper.readTree(monitor.getSystem().toJSON()));
			    sJson = json.toString();
			} catch (Throwable e) {
			    log.error(String.format("Failed to serialize monitor "
			            + "%s to json for %s event notification", 
			            monitor.getUuid(), eventType.name()), e);
			}
			
			
			// fire event on monitor
			processNotification(monitor.getUuid(), eventType.name(), createdBy, sJson);
			
			
			// if the result of this check was different from the last, sent a MonitorEventType.RESULT_CHANGE event
			if (lastCheck != null && !currentCheck.getResult().equals(lastCheck.getResult())) {
				processNotification(monitor.getUuid(), MonitorEventType.RESULT_CHANGE.name(), createdBy, sJson);
				
				processNotification(monitor.getSystem().getUuid(), "MONITOR_" + eventType.name(), createdBy, sJson);
			}
			
			// multiple checks can run for a single monitor. if this check tips the scale and 
			// changes the overall monitor status, send a result
			if (currentCheck.getResult().getSystemStatus() != monitor.getSystem().getStatus()) {
				
				// monitor event
				processNotification(monitor.getUuid(), MonitorEventType.STATUS_CHANGE.name(), createdBy, sJson);
				
				// delegated system event
				processNotification(monitor.getSystem().getUuid(), "MONITOR_" + MonitorEventType.STATUS_CHANGE.name(), createdBy, sJson);
				
				// if the monitor is configured to update a system, then do so here. The SystemManager
				// can handle its own event propagation.
				if (monitor.isUpdateSystemStatus()) {
					SystemManager systemManager = new SystemManager();
					systemManager.updateSystemStatus(monitor.getSystem(), currentCheck.getResult().getSystemStatus(), monitor.getOwner());
				}
			}
			
			event = new DomainEntityEvent(monitor.getUuid(), eventType, eventType.getDescription() + " by " + createdBy, createdBy);
		}
		catch (EntityEventProcessingException e) {
			log.error(e.getMessage());
		}
		
		return event;
	}
	
//	/**
//	 * Generates notification events for granting and revoking {@link AgaveEntityPermission} 
//	 * on the given {@link AgaveEntity}. Permission change events are not propagated to the associated
//	 * resources.
//	 * 
//	 * @param monitor the {@link AgaveEntityEvent} on which the even is triggered
//	 * @param permission the {@link AgaveEntityPermission} just granted
//	 * @param eventType the event being triggered
//	 * @param createdBy the user who caused this event
//	 * @return the {@link AgaveEntityEvent} saved to the Monitor history.
//	 * 
//	 * @throws EntityEventProcessingException
//	 */
//	public DomainEntityEvent processPermissionEvent(Monitor monitor, MonitorPermission permission, MonitorEventType eventType, String createdBy) 
//	throws EntityEventProcessingException 
//	{
//		DomainEntityEvent event = null;
//		try {
//			if (monitor == null) {
//				throw new EntityEventProcessingException("Valid monitor must be provided to process event.");
//			}
//			
//			if (permission == null) {
//				throw new EntityEventProcessingException("Valid monitor permission must be provided to process event.");
//			}
//			
//			ObjectNode json = null;
//			try {
//			    json = mapper.createObjectNode();
//		    	json.put("monitor", mapper.readTree(monitor.toJSON()));
//			    json.put("permission", mapper.valueToTree(permission));
//			} catch (Throwable e) {
//			    log.error(String.format("Failed to serialize monitor "
//			            + "%s to json for %s event notification", 
//			            monitor.getUuid(), permission.getPermission().name()), e);
//			    json = null;
//			}
//			
//	//		MonitorEventType eventType = (permission.getPermission() == MonitorEventType.NONE) ? 
//	//				MonitorEventType.PERMISSION_REVOKE : MonitorEventType.PERMISSION_GRANT;
//	//		
//			event = new DomainEntityEvent(monitor.getUuid(), eventType, eventType.getDescription() + " by createdBy", createdBy);
//			
//			try {
//				new DomainEntityEventDao().persist(event);
//			}
//			catch (EntityEventPersistenceException e) {
//				log.error(String.format("Failed to persist %s monitor event "
//			            + "to history for %s", 
//			            eventType.name(), monitor.getUuid()), e);
//			}
//			
//			processNotification(monitor.getUuid(), eventType.name(), createdBy, json.toString());
//			
//			// We do not fire delegated monitor permission events on associated uuids 
//		}
//		catch (EntityEventProcessingException e) {
//			log.error(e.getMessage());
//		}
//		
//		return event;
//	}
	
	
	/**
	 * Publishes a {@link MonitorEvent} event to the  
	 * {@link RemoteSystem} on which the {@link Monitor} resides.
	 * 
	 * @param associatedUuid
	 * @param json
	 * @return the number of messages published
	 */
	private int processNotification(String associatedUuid, String event, String createdBy, String sJson) {
		try {
            return NotificationManager.process(associatedUuid, event, createdBy, sJson);
        }
        catch (Throwable e) {
            log.error(String.format("Failed to send delegated event notification "
                    + "to %s to on a %s event", 
                    associatedUuid, event), e);
            return 0;
        }
	}
}
