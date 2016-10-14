package org.iplantc.service.transfer.events;

import org.apache.log4j.Logger;
import org.iplantc.service.common.exceptions.EntityEventPersistenceException;
import org.iplantc.service.common.exceptions.EntityEventProcessingException;
import org.iplantc.service.notification.managers.NotificationManager;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.transfer.dao.TransferTaskEventDao;
import org.iplantc.service.transfer.model.TransferTask;
import org.iplantc.service.transfer.model.TransferTaskEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Handles sending and propagation of events on {@link TransferTask} objects.
 * 
 * TODO: Make this class the default mechanism for adding events
 * TODO: Refactor this as an async factory using vert.x
 *
 * @author dooley
 * @param <ScheduledTransfer>
 *
 */
public class TransferTaskEventProcessor {
	private static final Logger log = Logger.getLogger(TransferTaskEventProcessor.class);
	private ObjectMapper mapper = new ObjectMapper();
	
	public TransferTaskEventProcessor(){}
	
	/**
	 * Generates notification events for {@link TransferTask}.
	 * 
	 * @param transferTask the {@link TransferTask} on which the even is triggered
	 * @param createdBy the user who caused this event
	 * @return the {@link TransferTaskEvent} with the association to the {@link TransferTask}
	 */
	public void processTransferTaskEvent(TransferTask transferTask, TransferTaskEvent event) {

		
		try {
			if (transferTask == null) {
				throw new EntityEventProcessingException("Valid transfer task must be provided to process event.");
			}
			
			event.setEntity(transferTask.getUuid());
			
			String sJson = null;
			try {
				ObjectNode json = mapper.createObjectNode();
			    json.set("transferTask", mapper.readTree(transferTask.toJSON()));
//			    ArrayNode systems = mapper.createArrayNode()
//			    		.add(mapper.readTree(sourceSystem.toJSON()))
//			    		.add(mapper.readTree(destSystem.toJSON()));
//			    json.set("system", systems);
			    sJson = json.toString();
			} catch (Throwable e) {
			    log.error(String.format("Failed to serialize transfer "
			            + "%s to json for %s event notification", 
			            transferTask.getUuid(), event.getStatus()), e);
			}
			
			// fire event on transfer
			processNotification(transferTask.getUuid(), event.getStatus(), event.getCreatedBy(), sJson);

			// We fire delegated transfer chec events on the system being transfered.
//			processNotification(sourceSystem.getUuid(), "TRANSFER_" + event.getStatus(), event.getCreatedBy(), sJson);
//			processNotification(destSystem.getUuid(), "TRANSFER_" + event.getStatus(), event.getCreatedBy(), sJson);
//			
			try {
				new TransferTaskEventDao().persist(event);
			}
			catch (EntityEventPersistenceException e) {
				log.error(String.format("Failed to persist %s transfer task event "
			            + "to history for %s", 
			            event.getStatus(), transferTask.getUuid()), e);
			}
		}
		catch (EntityEventProcessingException e) {
			log.error(e.getMessage());
		}
	}
	
	/**
	 * Publishes a {@link TransferEvent} event to the  
	 * {@link RemoteSystem} on which the {@link ScheduledTransfer} resides.
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
