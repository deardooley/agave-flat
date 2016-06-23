/**
 * 
 */
package org.iplantc.service.notification.providers.email.clients;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.providers.email.EmailClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Email client used for testing to write emails to log output rather than
 * send them.
 * 
 * @author dooley
 *
 */
public class LoggingEmailClient implements EmailClient {

    public static Logger log = Logger.getLogger(LoggingEmailClient.class);
    
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
        
        ObjectMapper mapper = new ObjectMapper();
        
        try {
            ObjectNode json = new ObjectMapper().createObjectNode();
            ObjectNode to = mapper.createObjectNode()
                    .put("name", recipientName)
                    .put("address", recipientAddress);
            ObjectNode from = mapper.createObjectNode()
                    .put("name", Settings.SMTP_FROM_NAME)
                    .put("address", Settings.SMTP_FROM_ADDRESS);
            
         // add custom headers if present
            ObjectNode headers = mapper.createObjectNode();
            if (!getCustomHeaders().isEmpty()) {
                for (Entry<String,String> entry: getCustomHeaders().entrySet()) {
                    headers.put(entry.getKey(), entry.getValue());
                }
            }
            
            json.put("headers", headers);
            json.put("to", to);
            json.put("from", from);
            json.put("subject", subject)
                .put("body", body)
                .put("htmlBody", htmlBody);
            
            log.debug(json.toString());
        } 
        catch (Exception e) {
            throw new NotificationException("Failed to send notification to log file", e);
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
