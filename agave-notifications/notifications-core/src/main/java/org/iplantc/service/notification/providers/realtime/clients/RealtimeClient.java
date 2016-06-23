package org.iplantc.service.notification.providers.realtime.clients;

import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.NotificationAttemptResponse;
import org.iplantc.service.notification.providers.NotificationAttemptProvider;
import org.iplantc.service.notification.providers.realtime.model.ChannelMessage;
import org.iplantc.service.notification.providers.realtime.model.RealtimeMessageItems;

public interface RealtimeClient extends NotificationAttemptProvider{

	/**
	 * Pushes the {@link RealtimeMessageItems} from the implementing object instance to the pushpin destination
	 * channels configured in each {@link ChannelMessage}
	 * 
	 * @param items wrapper object containing the list of {@link ChannelMessage}s to send 
	 * @param attemptUuid the unique identifier for this attempt
	 * @return the HTTP status code from the call to pushpin.
	 * @throws NotificationException 
	 */
	public abstract NotificationAttemptResponse publish() throws NotificationException;

}