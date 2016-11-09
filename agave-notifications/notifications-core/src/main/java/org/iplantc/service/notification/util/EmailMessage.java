package org.iplantc.service.notification.util;

import java.util.Map;

import org.apache.log4j.Logger;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.providers.email.EmailClient;
import org.iplantc.service.notification.providers.email.EmailClientFactory;

/**
 * Simple email class using the JavaMail API to send an email in both
 * HTML and plain text format.
 * 
 * @author Rion Dooley < dooley [at] tacc [dot] utexas [dot] edu >
 */
public class EmailMessage {
    
    public static Logger log = Logger.getLogger(EmailMessage.class.getName());
    
    /**
     * Synchronously sends a multipart email in both html and plaintext format 
     * using an {@link EmailClient} determined by the service settings. Supports
     * addition of custom headers using the conventions of the email service
     * provider.
     * 
     * @param recipientName Full name of recipient (ex. John Smith)
     * @param recipientAddress email address of recipient
     * @param subject of the email
     * @param body of the email in plain text format.
     * @param custom headers to add to the email
     * @throws NotificationException
     */
    public static void send(String recipientName, String recipientAddress, String subject, String body, String htmlBody, Map<String, String> customHeaders) 
    throws NotificationException 
    {
        EmailClient client = EmailClientFactory.getInstance(Settings.EMAIL_PROVIDER);
        client.send(recipientName, recipientAddress, subject, body, htmlBody);
    }
    
    /**
     * Synchronously sends a multipart email in both html and plaintext format 
     * using an {@link EmailClient} determined by the service settings.
     * 
     * @param recipientName Full name of recipient (ex. John Smith)
     * @param recipientAddress email address of recipient
     * @param subject of the email
     * @param body of the email in plain text format.
     * @throws NotificationException
     */
    public static void send(String recipientName, String recipientAddress, String subject, String body, String htmlBody) 
    throws NotificationException 
    {
        EmailClient client = EmailClientFactory.getInstance(Settings.EMAIL_PROVIDER);
        client.send(recipientName, recipientAddress, subject, body, htmlBody);
    }
}