package org.iplantc.service.notification.providers.email.clients;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.plexus.util.StringUtils;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.providers.email.EmailClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sendgrid.SendGrid;
import com.sendgrid.SendGridException;

/**
 * Client class to send emails using the SendGrid HTTP API. This is helpful
 * in container environments where there is no local mail server and port
 * blocking may prevent mail from otherwise being sent.
 * 
 * @author dooley
 *
 */
public class SendGridEmailClient implements EmailClient {

    protected Map<String, String> customHeaders = new HashMap<String, String>();
    
    public static void main(String[] args) throws Exception{
        new SendGridEmailClient().send("Rion", 
                "agaveapi@gmail.com", 
                "Test email via smtp", 
                "This is a simple smtp email fom the java client.",
                "<p>This is a simple SMTP email fom the <a href=\"http://java.com\">Java</a> client.</p>");
    }
    
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
        
        SendGrid sendgrid = new SendGrid(Settings.SMTP_AUTH_USER, Settings.SMTP_AUTH_PWD);

        SendGrid.Email email = new SendGrid.Email();
        if (StringUtils.isEmpty(recipientName)) {
            email.addTo(recipientAddress);
        } else {
            email.addTo(recipientAddress, recipientName);
        }
        email.setFrom(Settings.SMTP_FROM_ADDRESS);
        email.setFromName(Settings.SMTP_FROM_NAME);
        email.setSubject(subject);
        email.setHtml(htmlBody);
        email.setText(body);
        
        // add custom headers if present
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonHeader = mapper.createObjectNode();
        if (!getCustomHeaders().isEmpty()) {
            for (Entry<String,String> entry: getCustomHeaders().entrySet()) {
                jsonHeader.put(entry.getKey(), entry.getValue());
            }
        }
        
        email.addHeader("X-SMTPAPI", jsonHeader.toString());
            
//        email.setTemplateId(Settings.SENDGRID_TEMPLATE_ID);

        try {
          SendGrid.Response response = sendgrid.send(email);
          
          if (response == null) {
              throw new NotificationException("Failed to send notification message. Unable to connect to remote service.");
          }
          else if (!response.getStatus()) {
              throw new NotificationException("Failed to send notification message." + response.getMessage());
          }
        }
        catch (SendGridException e) {
            throw new NotificationException("Failed to send notification due to upstream erorr from mail server.", e);
        }
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
