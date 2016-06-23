package org.iplantc.service.notification.events;

import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.notification.dao.NotificationDao;
import org.iplantc.service.notification.model.Notification;


public interface EventFilter {

	/**
	 * @return the notification
	 */
	public abstract Notification getNotification();

	/**
	 * @param notification the notification to set
	 */
	public abstract void setNotification(Notification notification);

	/**
	 * @return the dao
	 */
	public abstract NotificationDao getDao();

	/**
	 * @param dao the dao to set
	 */
	public abstract void setDao(NotificationDao dao);

	/**
	 * @return the event
	 */
	public abstract String getEvent();

	/**
	 * @param event the event to set
	 */
	public abstract void setEvent(String event);

	/**
	 * @return the owner
	 */
	public abstract String getOwner();

	/**
	 * @param owner the owner to set
	 */
	public abstract void setOwner(String owner);

	/**
	 * @return the associatedUuid
	 */
	public abstract AgaveUUID getAssociatedUuid();

	/**
	 * @param associatedUuid the associatedUuid to set
	 */
	public abstract void setAssociatedUuid(AgaveUUID associatedUuid);

	/**
	 * @return the responseCode
	 */
	public abstract int getResponseCode();

	/**
	 * @param responseCode the responseCode to set
	 */
	public abstract void setResponseCode(int responseCode);

	public abstract String resolveMacros(String text, boolean urlEncode);

	/**
	 * @return the customNotificationMessageContextData
	 */
	public abstract String getCustomNotificationMessageContextData();

	/**
	 * @param customNotificationMessageContextData the customNotificationMessageContextData to set
	 */
	public abstract void setCustomNotificationMessageContextData(
			String customNotificationMessageContextData);

	/**
	 * Creates an appropriate email subject for the given event. Notification event macros
	 * are resolved properly prior to returning.
	 * @return
	 */
	public abstract String getEmailSubject();
	
	/**
	 * Creates an appropriate email plain text body for the given event. Notification event macros
	 * are resolved properly prior to returning.
	 * @return
	 */
	public abstract String getEmailBody();

	
	/**
	 * Handles default conversion of {@link NotificationEvent#getEmailBody()} into HTML 
	 * for templates who have not implemented the method by wrapping in a 
	 * <pre>&lt;div&gt;&lt;pre&gt;&lt;/pre&gt;%lt;/div&gt;</pre> 
	 * @return
	 */
	public abstract String getHtmlEmailBody();
	
}
