package org.iplantc.service.notification.providers.realtime.clients;

import java.net.URL;
import java.util.Arrays;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.providers.http.clients.AbstractWebhookClient;
import org.iplantc.service.notification.providers.realtime.model.ChannelMessage;
import org.iplantc.service.notification.providers.realtime.model.ChannelMessageBody;
import org.iplantc.service.notification.providers.realtime.model.RealtimeMessageItems;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Abstract class to handle parsing of {@link NotificationAttempt#getContent()} into 
 * a message suitable to send to the realtime server.
 * @author dooley
 *
 */
public abstract class AbstractRealtimeClient extends AbstractWebhookClient implements RealtimeClient {

	private static final Logger log = Logger.getLogger(AbstractRealtimeClient.class);
    
	public AbstractRealtimeClient(NotificationAttempt attempt) {
        super(attempt);
    }
    
    /**
	 * Filters the content into a JSON formatted incoming webhook body
	 * suitable for posting to Slack. 
	 * 
	 * @see https://api.slack.com/incoming-webhooks
	 * @param content the original {@link NotificationAttempt#getContent()}
	 * @return
	 */
	@Override
	public String getFilteredContent(String content) throws NotificationException{
		try {
			RealtimeMessageItems items = getMessageItemsForAttempt();
			return new ObjectMapper().writeValueAsString(items);
		} catch (JsonProcessingException e) {
			return attempt.getContent();
		}
	}
	
    /**
     * Turns the {@link NotificationAttempt#getContent()} into a message suitable
     * to send to the realtime server.
     * @return
     * @throws NotificationException
     */
    protected RealtimeMessageItems getMessageItemsForAttempt() throws NotificationException {
    	try {
	        URL callbackUrl = new URL(attempt.getCallbackUrl());
            
	        ObjectMapper mapper = new ObjectMapper();
    	    
    	    ChannelMessageBody channelMessageBody = new ChannelMessageBody(attempt.getEventName(), 
    	                                                    attempt.getOwner(), 
    	                                                    attempt.getAssociatedUuid(),
    	                                                    mapper.readTree(attempt.getContent()));
                    
            ChannelMessage ownerChannelMessage = new ChannelMessage(
            		attempt.getTenantId() + "/" + attempt.getOwner(), 
                    channelMessageBody);
            
            RealtimeMessageItems items = new RealtimeMessageItems(Arrays.asList(
                    ownerChannelMessage));
//            ChannelMessage resourceChannelMessage = new ChannelMessage(
//            		attempt.getTenantId() + "/" + attempt.getAssociatedUuid(), 
//                    channelMessageBody);
//            
//            RealtimeMessageItems items = new RealtimeMessageItems(Arrays.asList(
//                    ownerChannelMessage, resourceChannelMessage)); 
                 
            try 
            {
                String userProvidedChannelName = callbackUrl.getPath();
                
                if (StringUtils.isNotEmpty(userProvidedChannelName) &&
                        !StringUtils.equals(callbackUrl.getPath(), "/")) 
                {
                    items.getItems().add(new ChannelMessage(
                    		attempt.getTenantId() + "/" + attempt.getOwner() + userProvidedChannelName, 
                            channelMessageBody));
                }
            } catch (Exception e) {
                log.error("[" + attempt.getUuid() + "] Failed to add " + attempt.getEventName() + 
                		" notification realtime message to " + attempt.getCallbackUrl(), e);
            }
            
            return items;
            
	    }
	    catch (NotImplementedException e) {
			throw e;
		}
	    catch (Exception e) {
	    	throw new NotificationException("Error publishing message to realtime channels", e);
	    }
    }
}
