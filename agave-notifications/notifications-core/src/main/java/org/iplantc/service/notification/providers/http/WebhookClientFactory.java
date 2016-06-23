/**
 * 
 */
package org.iplantc.service.notification.providers.http;

import static org.iplantc.service.notification.providers.http.enumeration.WebhookProviderType.AGAVE;
import static org.iplantc.service.notification.providers.http.enumeration.WebhookProviderType.HTTP;
import static org.iplantc.service.notification.providers.http.enumeration.WebhookProviderType.SLACK;

import org.apache.commons.lang.NotImplementedException;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.providers.http.clients.AgaveWebhookClient;
import org.iplantc.service.notification.providers.http.clients.HttpWebhookClient;
import org.iplantc.service.notification.providers.http.clients.SlackWebhookClient;
import org.iplantc.service.notification.providers.http.clients.WebhookClient;
import org.iplantc.service.notification.providers.http.enumeration.WebhookProviderType;

/**
 * Factory class obtain a realtime messaging client based
 * on the service configurations.
 * 
 * @author dooley
 *
 */
public class WebhookClientFactory {

    /**
     * Creates a new {@link WebhookClient} for the {@link NotificationAttempt}.
     * 
     * @param the {@link NotificationAttempt} being made buy the returned client
     * @return an {@link WebhookClient} which supports the given {@code provider}.
     * @throws NotImplementedException if no client can be found.
     */
    public static WebhookClient getInstance(NotificationAttempt attempt, WebhookProviderType provider)
    throws NotImplementedException
    {
    	if (SLACK == provider) {
            return new SlackWebhookClient(attempt);
        } 
    	else if (HTTP == provider) {
            return new HttpWebhookClient(attempt);
        } 
    	else if (AGAVE == provider) {
            return new AgaveWebhookClient(attempt);
        } 
    	else {
            throw new NotImplementedException();
        }
    }
}
