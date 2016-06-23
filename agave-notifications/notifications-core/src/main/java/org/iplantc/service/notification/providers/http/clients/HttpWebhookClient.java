/**
 * 
 */
package org.iplantc.service.notification.providers.http.clients;

import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType;

/**
 * @author dooley
 *
 */
public class HttpWebhookClient extends AbstractWebhookClient {

	/**
	 * @param attempt
	 */
	public HttpWebhookClient(NotificationAttempt attempt) {
		super(attempt);
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.providers.http.clients.AbstractWebhookClient#getSupportedCallbackProviderType()
	 */
	@Override
	protected String getSupportedCallbackProviderType() {
		return NotificationCallbackProviderType.WEBHOOK.name().toLowerCase();
	}

}
