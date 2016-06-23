package org.iplantc.service.notification.providers.realtime.clients;

import org.apache.log4j.Logger;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.model.NotificationAttemptResponse;
import org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType;
import org.iplantc.service.notification.providers.realtime.model.RealtimeMessageItems;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Client utility class to push messages to a info logger.
 * @author dooley
 *
 */
public class LoggingRealtimeClient extends AbstractRealtimeClient {
    
    private static final Logger log = Logger.getLogger(LoggingRealtimeClient.class);
    
    private String event;
    private Notification notification;
    
    public LoggingRealtimeClient(NotificationAttempt attempt) {
        super(attempt);
    }
    
    /* (non-Javadoc)
	 * @see org.iplantc.service.notification.http.clients.WebhookClient#getSupportedCallbackProviderType()
	 */
	@Override
	public String getSupportedCallbackProviderType() {
		return "pushpin " + NotificationCallbackProviderType.REALTIME.name().toLowerCase();
	}
    
    /* (non-Javadoc)
	 * @see org.iplantc.service.notification.realtime.clients.RealtimeClient#publish(org.iplantc.service.notification.realtime.model.RealtimeMessageItems, java.lang.String)
	 */
    @Override
	public NotificationAttemptResponse publish() throws NotificationException {
    	NotificationAttemptResponse response = null;
        try 
        {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode json = mapper.createObjectNode();
            json.put("body", getFilteredContent(attempt.getContent()));
            json.set("headers", mapper.createObjectNode()
            			.put("Content-Type", "application/json")
            			.put("User-Agent", "Agave-Hookbot/"+ org.iplantc.service.common.Settings.getContainerId())
            			.put("X-Agave-Delivery", attempt.getUuid())
            			.put("X-Agave-Notification", notification.getUuid()));
            
            log.info(mapper.writer(new DefaultPrettyPrinter()).writeValueAsString(json));
            
            log.debug("[" + attempt.getUuid() + "] Successfully sent " + event + " notification realtime message to " + getClass().getSimpleName());
            
            response = new NotificationAttemptResponse(200, "200 ok");
        }
        catch(Exception e) {
            
            response = new NotificationAttemptResponse(500, "Failed to send " + event + " notification email to " + getClass().getSimpleName() +
                    " due to internal server error.");
            log.error("[" + attempt.getUuid() + "] " + response.getMessage(), e);
        }
        
        return response;
    }
}
