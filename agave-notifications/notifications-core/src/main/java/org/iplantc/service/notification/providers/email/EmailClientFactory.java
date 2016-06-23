/**
 * 
 */
package org.iplantc.service.notification.providers.email;

import org.apache.commons.lang.NotImplementedException;
import org.iplantc.service.notification.providers.email.clients.LocalEmailClient;
import org.iplantc.service.notification.providers.email.clients.LoggingEmailClient;
import org.iplantc.service.notification.providers.email.clients.SMTPEmailClient;
import org.iplantc.service.notification.providers.email.clients.SendGridEmailClient;
import org.iplantc.service.notification.providers.email.enumeration.EmailProviderType;

/**
 * Facotry class to use the email client specified in the settings.
 * 
 * @author dooley
 *
 */
public class EmailClientFactory {

    /**
     * Creates a new {@link EmailClient} based on the {@code provider} 
     * value passed in. Note that authentication will not happen until 
     * the email is sent, so there is no guarantee that the clients are
     * usable off the bat, just that they can be obtained.
     * 
     * @param provider a valid {@link EmailProviderType}
     * @return an {@link EmailClient} which supports the given {@code provider}.
     * @throws NotImplementedException if no client can be found.
     */
    public static EmailClient getInstance(EmailProviderType provider)
    throws NotImplementedException
    {
        if (EmailProviderType.SENDGRID == provider) {
            return new SendGridEmailClient();
        } else if (EmailProviderType.SMTP == provider) {
            return new SMTPEmailClient();
        } else if (EmailProviderType.LOCAL== provider) {
            return new LocalEmailClient();
        } else if (EmailProviderType.LOG== provider) {
            return new LoggingEmailClient();
        } else {
            throw new NotImplementedException();
        }
    }
}
