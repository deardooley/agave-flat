/**
 * 
 */
package org.iplantc.service.notification.providers.realtime.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Bean for wrapping realtime messages and handling serialization
 * @author dooley
 *
 */
public class ChannelMessageBody {
    
    /**
     * The textual name of the event thrown.
     */
    public String event;
    
    /**
     * The identity of the principal to whom the event creation is
     * attributed 
     */
    public String owner;
    
    /**
     * The uuid of the resource from which the event originiated 
     */
    private String notificationId;
    
    /**
     * The uuid of the resource from which the event originiated 
     */
    public String source;
    
    /**
     * The actual content of the message.
     */
    @JsonProperty("message")
    public Object content;

    /**
     * @param event
     * @param owner
     * @param source
     * @param content
     */
    public ChannelMessageBody(String event, String owner, String notificationId, String source, JsonNode content) {
        super();
        this.event = event;
        this.owner = owner;
        this.notificationId = notificationId;
        this.source = source;
        this.content = content;
    }

    /**
     * @return the event
     */
    public synchronized String getEvent() {
        return event;
    }

    /**
     * @param event the event to set
     */
    public synchronized void setEvent(String event) {
        this.event = event;
    }

    /**
     * @return the owner
     */
    public synchronized String getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public synchronized void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * @return the source
     */
    public synchronized String getSource() {
        return source;
    }

    /**
     * @param source the source to set
     */
    public synchronized void setSource(String source) {
        this.source = source;
    }

    /**
     * @return the content
     */
    public synchronized Object getContent() {
        return content;
    }

    /**
     * @param content the content to set
     */
    public synchronized void setContent(Object content) {
        this.content = content;
    }

	/**
	 * @return the notificationId
	 */
	public String getNotificationId() {
		return notificationId;
	}

	/**
	 * @param notificationId the notificationId to set
	 */
	public void setNotificationId(String notificationId) {
		this.notificationId = notificationId;
	}
}
