/**
 * 
 */
package org.iplantc.service.notification.events;

import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.notification.model.Notification;
import org.joda.time.DateTime;

/**
 * @author dooley
 *
 */
public class TransformNotificationEvent extends AbstractEventFilter {

	/**
	 * @param notification
	 */
	public TransformNotificationEvent(AgaveUUID associatedUuid, Notification notification, String event, String owner)
	{
		super(associatedUuid, notification, event, owner);
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getEmailBody()
	 */
	@Override
	public String getEmailBody()
	{
		String body = "Data transformation task ${UUID} received a(n) " + event +  
					" event at "+ new DateTime().toString() + " from " + owner + ".\n\n";
		
		return resolveMacros(body, false);
	}
	
	@Override
    public String getHtmlEmailBody()
    {
        return "<p>" + getEmailBody() + "</p>";
    }

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getEmailSubject()
	 */
	@Override
	public String getEmailSubject()
	{
		return resolveMacros("Transform ${UUID} received a(n) ${EVENT} event", false);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.AbstractNotificationEvent#resolveMacros(java.lang.String, boolean)
	 */
	@Override
	public String resolveMacros(String text, boolean urlEncode)
	{
		// TODO Auto-generated method stub
		return null;
	}

}
