/**
 * 
 */
package org.iplantc.service.notification.events;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.notification.model.Notification;

/**
 * @author dooley
 *
 */
public class ProfileNotificationEvent extends AbstractEventFilter {

	private static final Logger log = Logger.getLogger(ProfileNotificationEvent.class);
	
	/**
	 * @param notification
	 */
	public ProfileNotificationEvent(AgaveUUID associatedUuid, Notification notification, String event, String owner)
	{
		super(associatedUuid, notification, event, owner);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getEmailBody()
	 */
	@Override
	public String getEmailBody()
	{
		String body = "Username: ${USERNAME}\n" +
//				"Email: ${EMAIL}\n" +
				"UUID: ${UUID}\n";// +
//				"First name: ${FIRST_NAME}\n" +
//				"Last name: ${LAST_NAME}\n" +
//				"Position: ${POSITION}\n" +
//				"Institution: ${INSTITUTION}\n" +
//				"Phone: ${PHONE}\n" +
//				"Fax: ${FAX}\n" +
//				"Research Area: ${RESEARCH_AREA}\n" +
//				"Department: ${DEPARTMENT}\n" +
//				"City: ${CITY}\n" +
//				"State: ${STATE}\n" +
//				"Country: ${COUNTRY}\n" + 
//				"Gender: ${GENDER}\n";
		
		if (StringUtils.equalsIgnoreCase(notification.getEvent(), "created") || 
				StringUtils.equalsIgnoreCase(notification.getEvent(), "profile_created")) {
			body = "A new account was created for user: ${USERNAME}";
		}
		else if (StringUtils.equalsIgnoreCase(notification.getEvent(), "udpated") || 
				StringUtils.equalsIgnoreCase(notification.getEvent(), "profile_udpated")) {
			body = "The profile of user ${USERNAME} was updated";
		}
		else if (StringUtils.equalsIgnoreCase(notification.getEvent(), "deleted") || 
				StringUtils.equalsIgnoreCase(notification.getEvent(), "profile_deleted")) {
			body = "User ${USERNAME} was deleted.\n\n";
		}
		else if (StringUtils.equalsIgnoreCase(notification.getEvent(), "account_activated")) {
			body = "The account of user ${USERNAME} was activated.\n\n";
		}
		else if (StringUtils.equalsIgnoreCase(notification.getEvent(), "account_deactivated")) {
			body = "The account of user ${USERNAME} was deactivated.\n\n";
		}
		else if (StringUtils.equalsIgnoreCase(notification.getEvent(), "role_granted") ||  
				StringUtils.equalsIgnoreCase(notification.getEvent(), "profile_role_granted")) {
			body = "User ${USERNAME} was was granted a new role.\n\n";
		}
		else if (StringUtils.equalsIgnoreCase(notification.getEvent(), "role_revoked") || 
				StringUtils.equalsIgnoreCase(notification.getEvent(), "profile_role_revoked")) {
			body = "User ${USERNAME} had a role revoked.\n\n";
		}
		else if (StringUtils.equalsIgnoreCase(notification.getEvent(), "quota_exceeded")) {
			body = "User ${USERNAME} has exceeded their quota.\n\n";
		}
		else
		{
			body = "User ${USERNAME} experienced a(n) ${EVENT} event.";
		}
		
		return resolveMacros(body, false);
	}

	/* (non-Javadoc)
     * @see org.iplantc.service.notification.events.EventFilter#getHtmlEmailBody()
     */
    @Override
    public String getHtmlEmailBody()
    {
        String body = "<p><strong>Username:</strong> ${USERNAME}<br>" +
//              "<strong>Email: ${EMAIL}<br>" +
                "<strong>UUID:</strong> ${UUID}<br>";// +
//              "<strong>First name:</strong> ${FIRST_NAME}<br>" +
//              "<strong>Last name:</strong> ${LAST_NAME}<br>" +
//              "<strong>Position:</strong> ${POSITION}<br>" +
//              "vInstitution:</strong> ${INSTITUTION}<br>" +
//              "<strong>Phone:</strong> ${PHONE}<br>" +
//              "<strong>Fax:</strong> ${FAX}<br>" +
//              "<strong>Research Area:</strong> ${RESEARCH_AREA}<br>" +
//              "<strong>Department:</strong> ${DEPARTMENT}<br>" +
//              "<strong>City:</strong> ${CITY}<br>" +
//              "<strong>State:</strong> ${STATE}<br>" +
//              "<strong>Country:</strong> ${COUNTRY}<br>" + 
//              "<strong>Gender:</strong> ${GENDER}<br>";
        
        if (StringUtils.equalsIgnoreCase(notification.getEvent(), "created") || 
                StringUtils.equalsIgnoreCase(notification.getEvent(), "profile_created")) {
            body = "<p>A new account was created for user: ${USERNAME}</p>";
        }
        else if (StringUtils.equalsIgnoreCase(notification.getEvent(), "udpated") || 
                StringUtils.equalsIgnoreCase(notification.getEvent(), "profile_udpated")) {
            body = "<p>The profile of user ${USERNAME} was updated</p>";
        }
        else if (StringUtils.equalsIgnoreCase(notification.getEvent(), "deleted") || 
                StringUtils.equalsIgnoreCase(notification.getEvent(), "profile_deleted")) {
            body = "<p>User ${USERNAME} was deleted.</p>";
        }
        else if (StringUtils.equalsIgnoreCase(notification.getEvent(), "account_activated")) {
            body = "<p>The account of user ${USERNAME} was activated.</p>";
        }
        else if (StringUtils.equalsIgnoreCase(notification.getEvent(), "account_deactivated")) {
            body = "<p>The account of user ${USERNAME} was deactivated.</p>";
        }
        else if (StringUtils.equalsIgnoreCase(notification.getEvent(), "role_granted") ||  
                StringUtils.equalsIgnoreCase(notification.getEvent(), "profile_role_granted")) {
            body = "<p>User ${USERNAME} was was granted a new role.</p>";
        }
        else if (StringUtils.equalsIgnoreCase(notification.getEvent(), "role_revoked") || 
                StringUtils.equalsIgnoreCase(notification.getEvent(), "profile_role_revoked")) {
            body = "<p>User ${USERNAME} had a role revoked.</p>";
        }
        else if (StringUtils.equalsIgnoreCase(notification.getEvent(), "quota_exceeded")) {
            body = "<p>User ${USERNAME} has exceeded their quota.</p>";
        }
        else
        {
            body = "<p>User ${USERNAME} experienced a(n) ${EVENT} event.</p>";
        }
        
        return resolveMacros(body, false);
    }
    
	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getEmailSubject()
	 */
	@Override
	public String getEmailSubject()
	{
		return resolveMacros("Profile ${USERNAME} was ${EVENT}", false);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.AbstractNotificationEvent#resolveMacros(java.lang.String, boolean)
	 */
	@Override
	public String resolveMacros(String body, boolean urlEncode)
	{
		try 
		{
			body = StringUtils.replace(body, "${UUID}", associatedUuid.toString());
			body = StringUtils.replace(body, "${EVENT}", event);
			
			String[] uuidTokens = StringUtils.split(associatedUuid.toString(), "-");
			if (uuidTokens.length == 4) {
				body = StringUtils.replace(body, "${USERNAME}", uuidTokens[1]);
			} else {
				body = StringUtils.replace(body, "${USERNAME}", "");
			}
//			body = StringUtils.replace(body, "${EMAIL}", (String)jobFieldMap.get("email"));
//			body = StringUtils.replace(body, "${FIRST_NAME}", (String)jobFieldMap.get("first_name"));
//			body = StringUtils.replace(body, "${LAST_NAME}", (String)jobFieldMap.get("last_name"));
//			body = StringUtils.replace(body, "${POSITION}", (String)jobFieldMap.get("position"));
//			body = StringUtils.replace(body, "${INSTITUTION}", (String)jobFieldMap.get("institution"));
//			body = StringUtils.replace(body, "${PHONE}", (String)jobFieldMap.get("phone"));
//			body = StringUtils.replace(body, "${FAX}", (String)jobFieldMap.get("fax"));
//			body = StringUtils.replace(body, "${RESEARCH_AREA}", (String)jobFieldMap.get("research_area"));
//			body = StringUtils.replace(body, "${DEPARTMENT}", (String)jobFieldMap.get("department"));
//			body = StringUtils.replace(body, "${CITY}", (String)jobFieldMap.get("city"));
//			body = StringUtils.replace(body, "${STATE}", (String)jobFieldMap.get("state"));
//			body = StringUtils.replace(body, "${COUNTRY}", (String)jobFieldMap.get("country"));
//			body = StringUtils.replace(body, "${GENDER}", ((Integer)jobFieldMap.get("gender")).toString());
//			body = StringUtils.replace(body, "${LAST_UPDATED}", new DateTime(jobFieldMap.get("last_updated")).toString());
			
			return body;
		}
		catch (Exception e) {
			log.error("Failed to create notification body", e);
			return "The status of internal user " + associatedUuid.toString() +
					" has changed to " + associatedUuid.toString();
		}
	}

}
