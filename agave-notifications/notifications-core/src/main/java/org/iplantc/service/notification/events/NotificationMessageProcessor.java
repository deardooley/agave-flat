package org.iplantc.service.notification.events;

import static org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType.AGAVE;
import static org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType.EMAIL;
import static org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType.NONE;
import static org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType.REALTIME;
import static org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType.SLACK;
import static org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType.SMS;
import static org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType.WEBHOOK;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.sql.Timestamp;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.notification.exceptions.BadCallbackException;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.model.NotificationPolicy;
import org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Static utility class to convert the original event notification messages thrown from
 * the platform, filter them into a format suitable to publish to the appropriate subscribed
 * consumers. The result of this call will be a message written to the retry message queue,
 * the failed notification queue, or dropped entirely. Behavior depends on the {@link NotificationPolicy}
 * defined at subscription time.
 *  
 * @author dooley
 *
 */
public class NotificationMessageProcessor 
{
	private static final Logger	log	= Logger.getLogger(NotificationMessageProcessor.class);

	/**
	 * Converts a notification into a {@link NotificationAttempt} and attempts to send it. If
	 * unsuccessful, the returned {@link NotificationAttempt} will contain the next attempt time
	 * based on the {@link NotificationPolicy}. This will only throw an exception when the 
	 * {@link Notification} itself cannot be resolved to a valid event.
	 * 
	 * @param notification
	 * @param eventName
	 * @param owner
	 * @param associatedUuid
	 * @param customNotificationMessageContextData
	 * @return the result of attempting to deliver the notification
	 * @throws NotificationException
	 */
	public static NotificationAttempt process(Notification notification, String eventName, String owner, String associatedUuid, String customNotificationMessageContextData) 
	throws NotificationException
	{
		try 
		{
		    TenancyHelper.setCurrentEndUser(notification.getOwner());
            TenancyHelper.setCurrentTenantId(notification.getTenantId());
            
			// we need to resolve the actual target uuid corresponding to the event because the
			// notification may have a wildcard associatedUuid and we would not otherwise be
			// able to resolve the webhook template variables.
			String targetUuid = associatedUuid;
			if (StringUtils.isEmpty(associatedUuid) && !StringUtils.equalsIgnoreCase(notification.getAssociatedUuid(), "*")) {
				targetUuid = notification.getAssociatedUuid();
			}
			
			AgaveUUID uuid = new AgaveUUID(targetUuid);
			
			EventFilter event = 
					EventFilterFactory.getInstance(uuid, notification, eventName, owner);
			
			event.setCustomNotificationMessageContextData(customNotificationMessageContextData);
			
			
			NotificationAttempt attempt = createNotificationAttemptFromEvent(event);
			log.debug(String.format("Attempt [%s]: Beginning to process notification %s for %s event on %s entity with associatedUuid %s", 
					attempt.getAttemptNumber(),
					attempt.getNotificationId(),
					attempt.getEventName(),
					uuid.getResourceType().name(),
					attempt.getAssociatedUuid()));
			NotificationAttemptProcessor processor = new NotificationAttemptProcessor(attempt);
			
			processor.fire();
			
			log.debug(String.format("Attempt [%s]: Completed attempt at notification %s for %s event on %s entity with associatedUuid %s", 
					attempt.getAttemptNumber(),
					attempt.getNotificationId(),
					processor.getAttempt().getEventName(),
					uuid.getResourceType().name(),
					processor.getAttempt().getAssociatedUuid()));
			
			return processor.getAttempt();
		} 
		catch (UUIDException e) {
			throw new NotificationException("Could not identify associated resource from notification uuid. Trigger cannot be processed.");
		}
	}

	/**
	 * Creates a {@link NotificationAttempt} instance from an {@link AbstractEventFilter}. This also 
	 * @param event
	 * @return
	 */
	public static NotificationAttempt createNotificationAttemptFromEvent(EventFilter event) {

		NotificationAttempt attempt = new NotificationAttempt(event.getNotification().getUuid(), 
				event.resolveMacros(event.getNotification().getCallbackUrl(), true), 
				event.getOwner(), event.getAssociatedUuid().toString(), 
				event.getEvent(), "", new Timestamp(System.currentTimeMillis()));
		
		
		try {
			NotificationCallbackProviderType provider = NotificationCallbackProviderType.getInstanceForUri(event.getNotification().getCallbackUrl());
			
			ObjectMapper mapper = new ObjectMapper();
			if (provider == EMAIL) {
				// build a json object with the subject, body, and html body for the content
				ObjectNode json = mapper.createObjectNode()
						.put("subject", event.getEmailSubject())
						.put("body", event.getEmailBody())
						.put("htmlBody", event.getHtmlEmailBody());
				attempt.setContent(json.toString());
			} 
			else if (provider == SLACK || provider == SMS) {
				ObjectNode json = mapper.createObjectNode()
						.put("subject", event.getEmailSubject());
				
				// we'll create a formatted attachment if there is custom data to post to slack
				if (StringUtils.contains(event.getCustomNotificationMessageContextData(), "CUSTOM_USER_JOB_EVENT_NAME")) {
					try {
						// add content as a json object so we don't double escape
						json.put("body", mapper.writer(new DefaultPrettyPrinter()).writeValueAsString(mapper.readTree(event.getCustomNotificationMessageContextData())));
					} catch (IOException e) {
						json.put("body", event.getCustomNotificationMessageContextData());
					}
					// if the notification got here, it's all good.
					json.put("color", "good");
				}
				else {
					if (StringUtils.contains(event.getEvent(), "FAILED")) {
						json.put("color", "danger");
					}
					else if (StringUtils.contains(event.getEvent(), "RETRY")) {
						json.put("color", "warning");
					}
					else {
						json.put("color", "good");
					}
					// otherwise we'll just post a standard message
				}
				
				attempt.setContent(json.toString());
			}
//			else if (provider == RABBITMQ || provider == BEANSTALK || providprovider == JMS) {
//				
//			}
			else if (provider == WEBHOOK || provider == AGAVE || provider == REALTIME) {
				attempt.setContent(event.getCustomNotificationMessageContextData());
				try {
					URL callbackUrl = new URL(event.getNotification().getCallbackUrl());
					URI encodedCallbackUrl = new URI(callbackUrl.getProtocol(), 
											callbackUrl.getUserInfo(), 
											callbackUrl.getHost(), 
											callbackUrl.getPort(), 
											event.resolveMacros(callbackUrl.getPath(), false), 
											event.resolveMacros(callbackUrl.getQuery(), true), 
											callbackUrl.getRef());
					attempt.setCallbackUrl(encodedCallbackUrl.toString());
				} catch (Exception e) {
					// bad url, move on
				}
			} 
			else if (provider == NONE) {
				throw new BadCallbackException("No notification deliver provider found for for callbackUrl");
			}
			
			return attempt;
		}
		catch (BadCallbackException e) { 
			throw new NotImplementedException(e.getMessage(), e);
		}
	}
}
