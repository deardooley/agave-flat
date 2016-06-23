/**
 * 
 */
package org.iplantc.service.notification.providers.http.clients;

import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.model.NotificationAttemptResponse;
import org.iplantc.service.notification.providers.NotificationAttemptProvider;

/**
 * @author dooley
 *
 */
public interface WebhookClient extends NotificationAttemptProvider {

	/**
	 * Makes a HTTP POST request to {@link NotificationAttempt#getCallbackUrl()} with the 
	 * a {@code Content-Type: application/json} and body comprised of the 
	 * {@link NotificationAttempt#getContent()}. If the {@link NotificationAttempt#getCallbackUrl()}
	 * contains authorization informaiton, HTTP Basic auth is attempted with the
	 * given credentials.
	 * 
	 * @param attempt the attempt to make
	 * @return contains the http response code and interpreted message from the response. 
	 * @throws NotificationException
	 */
	@Override
	public abstract NotificationAttemptResponse publish() throws NotificationException;
}
