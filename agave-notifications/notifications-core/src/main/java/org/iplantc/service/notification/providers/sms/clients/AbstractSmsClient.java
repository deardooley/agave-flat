package org.iplantc.service.notification.providers.sms.clients;

import org.iplantc.service.notification.model.NotificationAttempt;

public abstract class AbstractSmsClient implements SmsClient {

	protected NotificationAttempt attempt;
	
	public AbstractSmsClient(NotificationAttempt attempt) {
		this.attempt = attempt;
	}
	 
}
