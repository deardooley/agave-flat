/**
 * 
 */
package org.iplantc.service.notification.providers.email.clients;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.log4j.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.providers.email.EmailClient;

import com.sun.mail.smtp.SMTPTransport;

/**
 * Email client to send mail using the locally installed Postfix server.
 * This is generally safe in standard web server environments, but very
 * likely to fail in container environments.
 * @author dooley
 *
 */
public class LocalEmailClient implements EmailClient {

    public static Logger log = Logger.getLogger(LocalEmailClient.class);
            
    protected Map<String, String> customHeaders = new HashMap<String, String>();
    
    public static void main(String[] args) throws Exception{
        new SMTPEmailClient().send("Rion", 
                "agaveapi@gmail.com", 
                "Test email via smtp", 
                "This is a simple smtp email fom the java client.",
                "<p>This is a simple SMTP email fom the <a href=\"http://java.com\">Java</a> client.</p>");
    }
    
    /* (non-Javadoc)
     * @see org.iplantc.service.notification.email.EmailClient#send(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void send(String recipientName, String recipientAddress, String subject, String body, String htmlBody)
    throws NotificationException 
    {
        
        if (StringUtils.isEmpty(recipientAddress)) {
            throw new NotificationException("Email recipient address cannot be null.");
        }
        
        if (StringUtils.isEmpty(body)) {
            throw new NotificationException("Email body cannot be null.");
        }
        
        if (StringUtils.isEmpty(htmlBody)) {
            htmlBody = "<p><pre>" + body + "</pre></p>";
        }
        
        if (StringUtils.isEmpty(subject)) {
            throw new NotificationException("Email subject cannot be null.");
        }
        
        Session session = null;
        
        Properties props = new Properties();
        
        try {
            props.put("mail.smtp.host", "localhost");
            props.put("mail.smtp.auth", "false");
            
            session = Session.getInstance(props);
            
            session.setDebug(true);
            
            MimeMessage message = createMessageObject(session, 
                    subject, body, htmlBody, recipientName, recipientAddress);
            
            // add custom headers if present
            if (!getCustomHeaders().isEmpty()) {
                for (Entry<String,String> entry: getCustomHeaders().entrySet()) {
                    message.addHeader(entry.getKey(), entry.getValue());
                }
            }
            
            SMTPTransport transport = (SMTPTransport)session.getTransport("smtp");
            
            transport.connect("localhost", Settings.SMTP_FROM_ADDRESS, Settings.SMTP_FROM_NAME);
            
            transport.sendMessage(message, message.getRecipients(Message.RecipientType.TO));
            
        } 
        catch (NoSuchProviderException e) {
            throw new NotificationException("Failed to send email message due to unknown email provider.", e);
        } 
        catch (Throwable e) {
            throw new NotificationException("Failed to send email message due to internal error.", e);
        }
    }
   
    private static MimeMessage createMessageObject(Session session, 
                                                    String subject, 
                                                    String body,
                                                    String htmlBody,
                                                    String name,
                                                    String address) 
    throws Exception {
        
        MimeMessage message = new MimeMessage(session);
        
        Multipart multipart = new MimeMultipart("alternative");

        BodyPart part1 = new MimeBodyPart();
        part1.setText(body);

        BodyPart part2 = new MimeBodyPart();
        part2.setContent(htmlBody, "text/html");

        multipart.addBodyPart(part1);
        multipart.addBodyPart(part2);

        message.setContent(multipart);
        message.setSubject(subject);
        
        Address fromAddress = new InternetAddress(
                Settings.SMTP_FROM_ADDRESS, Settings.SMTP_FROM_NAME);
        
        Address toAddress = new InternetAddress(address, name);
        
        message.setFrom(fromAddress);
        
        message.setRecipient(Message.RecipientType.TO, toAddress);
        
        return message;
    }

    /**
     * @return the customHeaders
     */
    public synchronized Map<String, String> getCustomHeaders() {
        return customHeaders;
    }

    /**
     * @param customHeaders the customHeaders to set
     */
    public synchronized void setCustomHeaders(Map<String, String> customHeaders) {
        this.customHeaders = customHeaders;
    }
}