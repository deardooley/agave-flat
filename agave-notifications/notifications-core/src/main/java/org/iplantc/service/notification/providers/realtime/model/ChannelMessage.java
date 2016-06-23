/**
 * 
 */
package org.iplantc.service.notification.providers.realtime.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Bean for wrapping individual {@link ChannelMessageBody} with the channel to which they should be written.
 * @author dooley
 *
 */
@JsonSerialize(using = ChannelMessageSerializer.class)
public class ChannelMessage {
    
	/**
	 * The protocol used to deliver the message. 
	 * Defaults to {@link ChannelMessageType#WS_MESSAGE} for 
	 * websocket delivery.
	 */
	@JsonIgnore
	private ChannelMessageType messageType = ChannelMessageType.JSON_OBJECT;
	
    /**
     * Channel to which the message should be written
     */
    private String channel;
    
    /**
     * The actual content of the message.
     */
    public ChannelMessageBody content;
    
    public ChannelMessage(String channel, ChannelMessageBody content) {
        setChannel(channel);
        setContent(content);
    }
    
    public ChannelMessage(String channel, ChannelMessageType messageType, ChannelMessageBody content) {
        setChannel(channel);
        setMessageType(messageType);
        setContent(content);
    }
    /**
     * @return the channel
     */
    public synchronized String getChannel() {
        return channel;
    }

    /**
     * @param channel the channel to set
     */
    public synchronized void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * @return the content
     */
    public synchronized ChannelMessageBody getContent() {
        return content;
    }

    /**
     * @param content the content to set
     */
    public synchronized void setContent(ChannelMessageBody content) {
        this.content = content;
    }
	public ChannelMessageType getMessageType() {
		return messageType;
	}
	public void setMessageType(ChannelMessageType messageType) {
		this.messageType = messageType;
	}
       
}
