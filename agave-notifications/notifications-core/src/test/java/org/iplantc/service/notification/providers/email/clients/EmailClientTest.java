package org.iplantc.service.notification.providers.email.clients;

import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.providers.email.EmailClient;
import org.iplantc.service.notification.providers.email.EmailClientFactory;
import org.iplantc.service.notification.providers.email.enumeration.EmailProviderType;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class EmailClientTest {

    private static final String TO_NAME = "Unit Test User";
    private static final String TO_ADDRESS = Settings.SMTP_FROM_ADDRESS;
    private static final String SUBJECT = "This is a test message body";
    private static final String BODY = "This is a test message body\nwhich spans multiple lines. It should show up perfectly fine in the body of the text.";
    private static final String HTMLBODY = "<h3>Pay Attention</h3><p>This is a <em>test</em> <i>HTML</i>message body.</p><p>It should display with markup in the resulting email.</p>";

    @DataProvider
    public Object[][] sendProvider() {
        return new Object[][] {
                {TO_NAME, TO_ADDRESS, SUBJECT, BODY, null,     false,  "Null htmlBody should be allowed."},
                {TO_NAME, TO_ADDRESS, SUBJECT, null, HTMLBODY, true,  "Null body should throw exception."},
                {TO_NAME, TO_ADDRESS, null,    BODY, HTMLBODY, true,  "Null subject should throw exception."},
                {TO_NAME, null,       SUBJECT, BODY, HTMLBODY, true,  "Null to address should throw exception."},
                {null,    TO_ADDRESS, SUBJECT, BODY, HTMLBODY, false, "Null to name should be allowed."},
                
                {"",      TO_ADDRESS, SUBJECT, BODY, HTMLBODY, false, "Empty to name should be allowed."},
                {TO_NAME, "",         SUBJECT, BODY, HTMLBODY, true,  "Empty to address should throw exception."},
                {TO_NAME, TO_ADDRESS, "",      BODY, HTMLBODY, true, "Empty subject should be allowed."},
                {TO_NAME, TO_ADDRESS, SUBJECT, "",   HTMLBODY, true, "Empty body should be allowed."},
                
                {TO_NAME, TO_ADDRESS, SUBJECT, BODY, HTMLBODY, false, "Valid email values should result in sent email."},
        };
    }

    @Test(dataProvider="sendProvider")
    public void sendLogEmailClient(String recipientName, String recipientAddress, String subject, String body, String htmlBody, boolean shouldThrowException, String message) 
    {
        doSend(EmailProviderType.LOG, 
                recipientName, 
                recipientAddress, 
                subject, 
                body, htmlBody,
                shouldThrowException,
                message);
    }
    
    @Test(dataProvider="sendProvider")
    public void sendSMTPEmailClient(String recipientName, String recipientAddress, String subject, String body, String htmlBody, boolean shouldThrowException, String message) 
    {
        doSend(EmailProviderType.SMTP, 
                recipientName, 
                recipientAddress, 
                subject, 
                body, htmlBody,
                shouldThrowException,
                message);
    }
    
    @Test(dataProvider="sendProvider")
    public void sendLocalEmailClient(String recipientName, String recipientAddress, String subject, String body, String htmlBody, boolean shouldThrowException, String message) 
    {
        doSend(EmailProviderType.LOCAL, 
                recipientName, 
                recipientAddress, 
                subject, 
                body, htmlBody,
                shouldThrowException,
                message);
    }
    
    @Test(dataProvider="sendProvider")
    public void sendSendgridEmailClient(String recipientName, String recipientAddress, String subject, String body, String htmlBody, boolean shouldThrowException, String message) 
    {
        doSend(EmailProviderType.SENDGRID, 
                recipientName, 
                recipientAddress, 
                subject, 
                body, htmlBody,
                shouldThrowException,
                message);
    }
    
    public void doSend(EmailProviderType provider, 
            String recipientName, String recipientAddress, 
            String subject, String body, String htmlBody, boolean shouldThrowException, 
            String message)
    {
        EmailClient client = EmailClientFactory.getInstance(provider);
        
        try {
            client.send(recipientName, recipientAddress, subject, body, htmlBody);
            Assert.assertFalse(shouldThrowException, message);
        } catch (NotificationException e) {
            if (!shouldThrowException)
                Assert.fail(message, e);
        }
    }
}
