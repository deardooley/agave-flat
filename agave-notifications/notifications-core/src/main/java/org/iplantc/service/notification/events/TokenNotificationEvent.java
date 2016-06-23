/**
 * 
 */
package org.iplantc.service.notification.events;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.notification.model.Notification;
import org.joda.time.DateTime;

/**
 * @author dooley
 *
 */
public class TokenNotificationEvent extends AbstractEventFilter {

	private static final Logger log = Logger.getLogger(TokenNotificationEvent.class);
	
	/**
	 * @param notification
	 */
	public TokenNotificationEvent(AgaveUUID associatedUuid, Notification notification, String event, String owner)
	{
		super(associatedUuid, notification, event, owner);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getEmailBody()
	 */
	@Override
	public String getEmailBody()
	{
		return "Token ${UUID} changed status to ${EVENT}. \n\n" + 
				"Token: ${TOKEN}\n" +
				"Created: ${CREATED}\n" +
				"Renewed: ${RENEWED}\n" + 
				"Expires: ${EXPIRES}\n" +
				"Username: ${USERNAME}\n" +
				"Remaining uses: ${REMAINING_USES}\n";
	}
	
	/* (non-Javadoc)
     * @see org.iplantc.service.notification.events.EventFilter#getHtmlEmailBody()
     */
    @Override
    public String getHtmlEmailBody()
    {
        return "<p>Token ${UUID} changed status to ${EVENT}:</p><br>" + 
                "<p><strong>Token:</strong> ${TOKEN}<br>" +
                "<strong>Created:</strong> ${CREATED}<br>" +
                "<strong>Renewed:</strong> ${RENEWED}<br>" + 
                "<strong>Expires:</strong> ${EXPIRES}<br>" +
                "<strong>Username:</strong> ${USERNAME}<br>" +
                "<strong>Remaining uses:</strong> ${REMAINING_USES}</p>";
    }

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getEmailSubject()
	 */
	@Override
	public String getEmailSubject()
	{
		return resolveMacros("Token ${UUID} changed status to ${EVENT}", false);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.AbstractNotificationEvent#resolveMacros(java.lang.String, boolean)
	 */
	@Override
	public String resolveMacros(String body, boolean urlEncode)
	{
		try 
		{
			Map<String, Object> jobFieldMap = getJobRow("authentication_tokens", associatedUuid.toString());
			
			body = StringUtils.replace(body, "${UUID}", associatedUuid.toString());
			body = StringUtils.replace(body, "${EVENT}", event);
			body = StringUtils.replace(body, "${TOKEN}", (String)jobFieldMap.get("token"));
			body = StringUtils.replace(body, "${CREATED}", new DateTime(jobFieldMap.get("created_at")).toString());
			body = StringUtils.replace(body, "${RENEWED}", new DateTime(jobFieldMap.get("renewed_at")).toString());
			body = StringUtils.replace(body, "${EXPIRES}", new DateTime(jobFieldMap.get("expires_at")).toString());
			body = StringUtils.replace(body, "${USERNAME}", (String)jobFieldMap.get("username"));
			body = StringUtils.replace(body, "${REMAINING_USES}", (String)jobFieldMap.get("remaining_uses"));
			
			return body;
		}
		catch (Exception e) {
			log.error("Failed to create notification body", e);
			return "The status of token with uuid " + associatedUuid.toString() +
					" has changed to " + event;
		}
	}

}
