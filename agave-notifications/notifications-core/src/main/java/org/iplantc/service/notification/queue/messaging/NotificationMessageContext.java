package org.iplantc.service.notification.queue.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Simple bean to hold notification specific message context.
 * 
 * @author dooley
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationMessageContext
{
	private String event;
	private String customData;
	private String associatedUuid;
	
	
	/**
	 * Default no-args constructor for use by Jackson
	 * during message deserialization 
	 */
	public NotificationMessageContext() {}
	
	/**
	 * @param uuid
	 * @param owner
	 * @param tenant
	 */
	public NotificationMessageContext(String event, String associatedUuid)
	{
		this(event, null, associatedUuid);
		
	}
	
	/**
	 * @param uuid
	 * @param owner
	 * @param tenant
	 */
	public NotificationMessageContext(String event, String customData, String associatedUuid)
	{
		this.event = event;
		this.setCustomData(customData);
		this.associatedUuid = associatedUuid;
	}

	/**
	 * @return the event
	 */
	public String getEvent()
	{
		return event;
	}

	/**
	 * @param event the event to set
	 */
	public void setEvent(String event)
	{
		this.event = event;
	}

	/**
     * @return the customData
     */
    public String getCustomData() {
        return customData;
    }

    /**
     * @param customData the customData to set
     */
    public void setCustomData(String customData) {
        this.customData = customData;
    }

    /**
	 * @return the associatedUuid
	 */
	public String getAssociatedUuid()
	{
		return associatedUuid;
	}

	/**
	 * @param associatedUuid the associatedUuid to set
	 */
	public void setAssociatedUuid(String associatedUuid)
	{
		this.associatedUuid = associatedUuid;
	}
}