/**
 * 
 */
package org.iplantc.service.notification.providers.realtime;

import static org.iplantc.service.notification.providers.realtime.enumeration.RealtimeProviderType.*;

import org.apache.commons.lang.NotImplementedException;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.providers.realtime.clients.FanoutRealtimeClient;
import org.iplantc.service.notification.providers.realtime.clients.LoggingRealtimeClient;
import org.iplantc.service.notification.providers.realtime.clients.PushpinRealtimeClient;
import org.iplantc.service.notification.providers.realtime.clients.RealtimeClient;
import org.iplantc.service.notification.providers.realtime.enumeration.RealtimeProviderType;
import org.iplantc.service.notification.util.ServiceUtils;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.notification.model.NotificationAttempt;

/**
 * Factory class obtain a realtime messaging client based
 * on the service configurations.
 * 
 * @author dooley
 *
 */
public class RealtimeClientFactory {

    /**
     * Creates a new {@link RealtimeClient} for the {@link NotificationAttempt}.
     * 
     * @param the {@link NotificationAttempt} being made buy the returned client
     * @return an {@link RealtimeClient} which supports the given {@code provider}.
     * @throws NotImplementedException if no client can be found.
     */
    public static RealtimeClient getInstance(NotificationAttempt attempt, RealtimeProviderType provider)
    throws NotImplementedException
    {
    	if (FANOUT == provider) {
            return new FanoutRealtimeClient(attempt);
        } 
    	else if (PUSHPIN == provider) {
            return new PushpinRealtimeClient(attempt);
        } 
    	else if (LOG == provider) {
            return new LoggingRealtimeClient(attempt);
        } 
    	else {
            throw new NotImplementedException();
        }
    }
}
