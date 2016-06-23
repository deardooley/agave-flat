package org.iplantc.service.notification.providers;

import static org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType.AGAVE;
import static org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType.EMAIL;
import static org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType.NONE;
import static org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType.REALTIME;
import static org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType.SLACK;
import static org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType.SMS;
import static org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType.WEBHOOK;

import org.apache.commons.lang.NotImplementedException;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.exceptions.BadCallbackException;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType;
import org.iplantc.service.notification.providers.email.EmailNotificationAttemptProvider;
import org.iplantc.service.notification.providers.http.clients.AgaveWebhookClient;
import org.iplantc.service.notification.providers.http.clients.HttpWebhookClient;
import org.iplantc.service.notification.providers.http.clients.SlackWebhookClient;
import org.iplantc.service.notification.providers.realtime.RealtimeClientFactory;
import org.iplantc.service.notification.providers.sms.SmsClientFactory;

public class NotificationAttemptProviderFactory {

	public static NotificationAttemptProvider getInstance(NotificationAttempt attempt) 
	throws NotImplementedException, NotificationException 
	{
		try {
			NotificationCallbackProviderType providerType = NotificationCallbackProviderType.getInstanceForUri(attempt.getCallbackUrl());
			NotificationAttemptProvider provider = null;
			
			if (providerType == EMAIL) {
				provider = new EmailNotificationAttemptProvider(attempt); 
			} else if (providerType == WEBHOOK) {
				provider = new HttpWebhookClient(attempt);
			} else if (providerType == SMS) {
				provider = SmsClientFactory.getInstance(attempt, Settings.SMS_PROVIDER);
			} else if (providerType == REALTIME) {
				provider = RealtimeClientFactory.getInstance(attempt, Settings.REALTIME_PROVIDER);
			} else if (providerType == SLACK) {
				provider = new SlackWebhookClient(attempt);
			} else if (providerType == AGAVE) {
				provider = new AgaveWebhookClient(attempt);
			} else if (providerType == NONE) {
				throw new NotImplementedException("Not supported protocol found for " + attempt.getCallbackUrl());
			}
			
			return provider;
		}
		catch (BadCallbackException | NotImplementedException e) {
			throw new NotImplementedException("Not supported protocol found for " + attempt.getCallbackUrl()); 
		}
	}
}
