/**
 * 
 */
package org.iplantc.service.notification.events;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.model.Notification;
import org.joda.time.DateTime;

/**
 * @author dooley
 *
 */
public class PostItNotificationEvent extends AbstractEventFilter {

	private static final Logger log = Logger.getLogger(PostItNotificationEvent.class);
	
	/**
	 * @param notification
	 */
	public PostItNotificationEvent(AgaveUUID associatedUuid, Notification notification, String event, String owner)
	{
		super(associatedUuid, notification, event, owner);
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getEmailBody()
	 */
	@Override
	public String getEmailBody()
	{
		String body = null;
		if (StringUtils.equalsIgnoreCase(event, "created")) {
			body = "PostIt ${NONCE} was created.\n\n" + 
					"Token: ${NONCE}\n" +
					"Created: ${CREATED}\n" +
					"Renewed: ${RENEWED}\n" + 
					"Expires: ${EXPIRES}\n" +
					"Target URL: ${TARGET_URL}\n" +
					"Target method: ${TARGET_METHOD}\n" +
					"PostIt URL: ${POSTIT}\n" +
					"Remaining uses: ${REMAINING_USES}\n";
		} else if (StringUtils.equalsIgnoreCase(event, "deleted")) {
			body = "PostIt ${NONCE} was deleted";
		} else if (StringUtils.equalsIgnoreCase(event, "expired")) {
			body = "PostIt ${NONCE} expired";
		} else if (StringUtils.equalsIgnoreCase(event, "redeemed")) {
			body = "PostIt ${NONCE} was redeemed";
		} else {
			body = "PostIt ${NONCE} recieved a(n) ${EVENT} event";
		}
		
		return resolveMacros(body, false);
	}
	
	/* (non-Javadoc)
     * @see org.iplantc.service.notification.events.EventFilter#getHtmlEmailBody()
     */
    @Override
    public String getHtmlEmailBody()
    {
        String body = null;
        if (StringUtils.equalsIgnoreCase(event, "created")) {
            body = "<p>PostIt ${NONCE} was created with the following configuration:</p><br>" + 
                    "<p><strong>Token:</strong> ${NONCE}<br>" +
                    "<strong>Created:</strong> ${CREATED}<br>" +
                    "<strong>Renewed:</strong> ${RENEWED}<br>" + 
                    "<strong>Expires:</strong> ${EXPIRES}<br>" +
                    "<strong>Target URL:</strong> ${TARGET_URL}<br>" +
                    "<strong>Target method:</strong> ${TARGET_METHOD}<br>" +
                    "<strong>PostIt URL:</strong> <a href=\"${POSTIT}\">${POSTIT}</a><br>" +
                    "<strong>Remaining uses:</strong> ${REMAINING_USES}</p>";
        } else if (StringUtils.equalsIgnoreCase(event, "deleted")) {
            body = "<p>PostIt ${NONCE} was deleted.</p>";
        } else if (StringUtils.equalsIgnoreCase(event, "expired")) {
            body = "<p>PostIt ${NONCE} expired.</p>";
        } else if (StringUtils.equalsIgnoreCase(event, "redeemed")) {
            body = "<p>PostIt ${NONCE} was redeemed.</p>";
        } else {
            body = "<p>PostIt ${NONCE} recieved a(n) ${EVENT} event.</p>";
        }
        
        return resolveMacros(body, false);
    }

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getEmailSubject()
	 */
	@Override
	public String getEmailSubject()
	{
		String subject = null;
		if (StringUtils.equalsIgnoreCase(event, "created")) {
			subject = "PostIt ${NONCE} was created";
		} else if (StringUtils.equalsIgnoreCase(event, "deleted")) {
			subject = "PostIt ${NONCE} was deleted";
		} else if (StringUtils.equalsIgnoreCase(event, "expired")) {
			subject = "PostIt ${NONCE} expired";
		} else if (StringUtils.equalsIgnoreCase(event, "redeemed")) {
			subject = "PostIt ${NONCE} was redeemed";
		} else {
			subject = "PostIt ${NONCE} recieved a(n) ${EVENT} event";
		}
		
		return resolveMacros(subject, false);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.AbstractNotificationEvent#resolveMacros(java.lang.String, boolean)
	 */
	@Override
	public String resolveMacros(String body, boolean urlEncode)
	{
		try 
		{
			Map<String, Object> jobFieldMap = getJobRow("postits", associatedUuid.toString());
			
			body = StringUtils.replace(body, "${UUID}", associatedUuid.toString());
			body = StringUtils.replace(body, "${EVENT}", event);
			body = StringUtils.replace(body, "${NONCE}", (String)jobFieldMap.get("postit_key"));
			body = StringUtils.replace(body, "${CREATED}", new DateTime(jobFieldMap.get("created_at")).toString());
			body = StringUtils.replace(body, "${RENEWED}", new DateTime(jobFieldMap.get("renewed_at")).toString());
			body = StringUtils.replace(body, "${EXPIRES}", new DateTime(jobFieldMap.get("expires_at")).toString());
			body = StringUtils.replace(body, "${TARGET_URL}", (String)jobFieldMap.get("target_url"));
			body = StringUtils.replace(body, "${TARGET_METHOD}", (String)jobFieldMap.get("target_method"));
			body = StringUtils.replace(body, "${REMAINING_USES}", (String)jobFieldMap.get("remaining_uses"));
			body = StringUtils.replace(body, "${POSTIT}", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_POSTIT_SERVICE) + jobFieldMap.get("postit_key"));
			
			return body;
		}
		catch (Exception e) {
			log.error("Failed to create notification body", e);
			return "The status of postit with uuid " + associatedUuid.toString() +
					" has changed to " + event;
		}
	}

}
