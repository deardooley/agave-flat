/**
 * 
 */
package org.iplantc.service.notification.providers.sms.clients;

import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.NotificationAttemptResponse;
import org.iplantc.service.notification.providers.NotificationAttemptProvider;

/**
 * Interface for all SMS notfiication clients.
 * @author dooley
 *
 */
public interface SmsClient extends NotificationAttemptProvider {

	/**
	 * Sends a SMS notification with the event subject to the phone number
	 * registered with the notifications API.
	 * 
	 * @return the respose from the remote sms service
	 * @throws NotificationException
	 */
	public NotificationAttemptResponse publish()
	throws NotificationException;
	
	abstract String getSupportedCallbackProviderType();
}
