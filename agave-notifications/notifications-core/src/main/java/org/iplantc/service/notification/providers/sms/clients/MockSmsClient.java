/**
 * 
 */
package org.iplantc.service.notification.providers.sms.clients;

import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.model.NotificationAttemptResponse;
import org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType;
import org.iplantc.service.notification.providers.sms.enumeration.SmsProviderType;

/**
 * @author dooley
 *
 */
public class MockSmsClient extends AbstractSmsClient {
	
	public MockSmsClient(NotificationAttempt attempt) {
		super(attempt);
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.sms.clients.SmsClient#publish(org.iplantc.service.notification.model.Notification, java.lang.String)
	 */
	@Override
	public NotificationAttemptResponse publish() throws NotificationException 
	{
		// Swallow notification and return 200-ok
		return new NotificationAttemptResponse(200, "200 ok");
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.sms.clients.SmsClient#getSupportedCallbackProviderType()
	 */
	@Override
	public String getSupportedCallbackProviderType() {
		return SmsProviderType.NONE.name().toLowerCase() + NotificationCallbackProviderType.SMS.name().toLowerCase();
	}

}
