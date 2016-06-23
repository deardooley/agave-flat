/**
 * 
 */
package org.iplantc.service.notification.providers;

import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.model.NotificationAttemptResponse;

/**
 * Common interface required by all notification protocol providers.
 * @author dooley
 *
 */
public interface NotificationAttemptProvider {

	/**
	 * Processes the {@link NotificationAttempt} provided by the 
	 * implementing class instance and returns a {@link NotificationAttemptResponse} 
	 * with the response of the call.
	 * @return
	 * @throws NotificationException
	 */
	public NotificationAttemptResponse publish() throws NotificationException;
}
