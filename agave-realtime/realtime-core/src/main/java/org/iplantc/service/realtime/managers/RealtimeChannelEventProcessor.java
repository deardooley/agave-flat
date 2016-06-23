/**
 * 
 */
package org.iplantc.service.realtime.managers;

import org.apache.log4j.Logger;
import org.iplantc.service.notification.managers.NotificationManager;
import org.iplantc.service.realtime.dao.RealtimeChannelEventDao;
import org.iplantc.service.realtime.exceptions.RealtimeChannelEventException;
import org.iplantc.service.realtime.model.RealtimeChannel;
import org.iplantc.service.realtime.model.RealtimeChannelEvent;
import org.iplantc.service.realtime.model.enumerations.RealtimeChannelEventType;

/**
 * @author dooley
 *
 */
public class RealtimeChannelEventProcessor {
	
	private static final Logger log = Logger.getLogger(RealtimeChannelEventProcessor.class);
	
	public static RealtimeChannelEvent process(RealtimeChannel realtimeChannel, 
			RealtimeChannelEventType eventType, String eventCreator) throws RealtimeChannelEventException {
		
		RealtimeChannelEvent event 
			= new RealtimeChannelEvent(realtimeChannel.getUuid(),
									   eventType, 
									   "Channel was " + eventType.name().toLowerCase() + " by " + eventCreator,
									   eventCreator);
		
		RealtimeChannelEventDao.persist(event);
		
//		MessageQueueClient client = null;
//		try {
//			client = MessageClientFactory.getMessageClient();
//			String queueName = StringUtils.replace(event.getTenantId(), ".", "_").toUpperCase();
//			client.push(queueName, queueName, event.toJSON());
//		} catch (MessagingException e) {
//			log.error("Failed to write realtime channel event " + event.getUuid() + " to the message queue.");
//		}
//		finally {
//			try { client.stop(); } catch (Exception e) {}
//		}
		
		// send notification event. This will also alert the realtime server
		// that the channel shoudl be closed and all clients unsubscribed.
		NotificationManager.process(event.getEntity(), 
				event.getStatus(), 
				event.getCreatedBy(), 
				realtimeChannel.toJSON());
		
		return event;
	}

}
