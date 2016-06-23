/**
 * 
 */
package org.iplantc.service.notification.events;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.model.Notification;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author dooley
 *
 */
public class NotificationNotificationEvent extends AbstractEventFilter {

	private static final Logger log = Logger.getLogger(NotificationNotificationEvent.class);
	
	/**
	 * @param notification
	 */
	public NotificationNotificationEvent(AgaveUUID associatedUuid, Notification notification, String event, String owner)
	{
		super(associatedUuid, notification, event, owner);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getEmailBody()
	 */
	@Override
	public String getEmailBody()
	{
		String body =	"UUID: ${UUID}\n" +
				"Owner: ${OWNER}\n" +
				"Callback URL: ${URL}\n" +
				"Associated UUID: ${ASSOCIATED_ID}\n" +
				"Event: ${EVENT}\n" +
				"Status: ${STATUS}\n";		
		
		if (StringUtils.equalsIgnoreCase(event, "created")) {
			body = "The following notification was created: \n\n" + body;
		}
		else if (StringUtils.equalsIgnoreCase(event, "deleted")) {
			body = "The following notification was deleted.\n\n" + body;
		}
		else if (StringUtils.equalsIgnoreCase(event, "failed")) {
			body = "Delivery of notification ${UUID} failed for event ${EVENT}.\n\n${RAW_JSON}";
		}
		else
		{
			body = "The following notification received a(n) " + event +  
					" event. The current notification description is now: \n\n" + body;
		}
		
		return resolveMacros(body, false);
	}
	
	/* (non-Javadoc)
     * @see org.iplantc.service.notification.events.EventFilter#getHtmlEmailBody()
     */
    @Override
    public String getHtmlEmailBody()
    {
        String body = "<p><strong>UUID: ${UUID}<br>" +
                "<strong>Owner:</strong> ${OWNER}<br>" +
                "<strong>Callback URL:</strong> ${URL}<br>" +
                "<strong>Associated UUID:</strong> ${ASSOCIATED_ID}<br>" +
                "<strong>Event:</strong> ${EVENT}<br>" +
                "<strong>Status:</strong> ${STATUS}</p>";  
        
        if (StringUtils.equalsIgnoreCase(event, "sent")) {
            body = "<p>The following notification was sent:</p><br>" + body;
        }
        else if (StringUtils.equalsIgnoreCase(event, "deleted")) {
            body = "<p>The following notification was deleted.</p><br>" + body;
        }
        else if (StringUtils.equalsIgnoreCase(event, "failed")) {
            body = "<p>Delivery of notification ${UUID} failed for event ${EVENT}.</p>" + 
            		"<br><p><pre>${RAW_JSON}</pre></p>";
        }
        else
        {
            body = "<p>The following notification received a(n) " + event +  
                    " event. The current notification description is now: </p><br>" + body;
        }
        
        return resolveMacros(body, false);
    }

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getEmailSubject()
	 */
	@Override
	public String getEmailSubject()
	{
		return resolveMacros("Notification ${UUID} changed status to ${EVENT}", false);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.AbstractNotificationEvent#resolveMacros(java.lang.String, boolean)
	 */
	@Override
	public String resolveMacros(String body, boolean urlEncode)
	{
		try 
		{
			body = StringUtils.replace(body,"${UUID}", associatedUuid.toString());
			body = StringUtils.replace(body,"${EVENT}", event);
			body = StringUtils.replace(body,"${OWNER}", notification.getOwner());
			body = StringUtils.replace(body,"${USERNAME}", notification.getOwner());
			body = StringUtils.replace(body,"${URL}", notification.getCallbackUrl());
//			body = StringUtils.replace(body,"${ATTEMPTS}", "" + notification.getPolicy().getRetryLimit());
//			body = StringUtils.replace(body,"${RESPONSE_CODE}", "" + notification.getResponseCode());
			body = StringUtils.replace(body,"${STATUS}", notification.getStatus().name());
//			body = StringUtils.replace(body,"${LAST_SENT}", new DateTime(notification.getLastSent()).toString());
			body = StringUtils.replace(body,"${CREATED}", new DateTime(notification.getCreated()).toString());
//			body = StringUtils.replace(body,"${LAST_UPDATED}", new DateTime(notification.getLastUpdated()).toString());
			body = StringUtils.replace(body,"${ASSOCIATED_ID}", associatedUuid.toString());
			
			if (StringUtils.isNotEmpty(getCustomNotificationMessageContextData())) {
			    ObjectMapper mapper = new ObjectMapper();
			    try {
//			        JsonNode json = mapper.readTree(getCustomNotificationMessageContextData());
			        
			        body = StringUtils.replace(body, "${RAW_JSON}", mapper.writer().withDefaultPrettyPrinter().writeValueAsString(getCustomNotificationMessageContextData()));
			    } catch (Exception e) {
			        body = StringUtils.replace(body, "${RAW_JSON}", getCustomNotificationMessageContextData());
			    }
			}
			
			return body;
		}
		catch (Exception e) {
			log.error("Failed to create notification body", e);
			return "The status of notification " + associatedUuid.toString() +
					" has changed to " + event;
		}
	}

}
