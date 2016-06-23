/**
 * 
 */
package org.iplantc.service.notification.providers.sms;

import static org.iplantc.service.notification.providers.sms.enumeration.SmsProviderType.LOG;
import static org.iplantc.service.notification.providers.sms.enumeration.SmsProviderType.NONE;
import static org.iplantc.service.notification.providers.sms.enumeration.SmsProviderType.TWILIO;

import org.apache.commons.lang.NotImplementedException;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.providers.realtime.clients.RealtimeClient;
import org.iplantc.service.notification.providers.sms.clients.LoggingSmsClient;
import org.iplantc.service.notification.providers.sms.clients.MockSmsClient;
import org.iplantc.service.notification.providers.sms.clients.SmsClient;
import org.iplantc.service.notification.providers.sms.clients.TwilioSmsClient;
import org.iplantc.service.notification.providers.sms.enumeration.SmsProviderType;

/**
 * Factory class obtain a SMS messaging client based
 * on the service configurations.
 * 
 * @author dooley
 *
 */
public class SmsClientFactory {

    /**
     * Creates a new {@link RealtimeClient} for the {@link NotificationAttempt}.
     * 
     * @param the {@link NotificationAttempt} being made buy the returned client
     * @param 
     * @return an {@link SmsClient} which supports the given {@code provider}.
     * @throws NotImplementedException if no client can be found.
     */
    public static SmsClient getInstance(NotificationAttempt attempt, SmsProviderType provider)
    throws NotImplementedException
    {
        if (TWILIO == provider) {
            return new TwilioSmsClient(attempt, Settings.TWILIO_ACCOUNT_SID, Settings.TWILIO_AUTH_TOKEN, Settings.TWILIO_PHONE_NUMBER);
        } else if (LOG == provider) {
            return new LoggingSmsClient(attempt);
        } else if (NONE == provider) {
            return new MockSmsClient(attempt);
        } else {
            throw new NotImplementedException();
        }
    }
}
