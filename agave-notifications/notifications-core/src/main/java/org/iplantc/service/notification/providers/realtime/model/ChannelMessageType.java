/**
 * 
 */
package org.iplantc.service.notification.providers.realtime.model;

/**
 * Supported realtime message delivery formats
 * @author dooley
 *
 */
public enum ChannelMessageType {

	WS_MESSAGE("ws-message", "content"),
	JSON_OBJECT("json-object", null),
	HTTP_STREAM("http-stream", "content"),
	HTTP_RESPONSE("http-response", "body");
	
	private String contentKey;
	private String contentField;
	
	private ChannelMessageType(String contentKey, String contentField) {
		this.setContentKey(contentKey);
		this.contentField = contentField;
	}

	public String getContentField() {
		return contentField;
	}

	public void setContentField(String contentField) {
		this.contentField = contentField;
	}

	public String getContentKey() {
		return contentKey;
	}

	public void setContentKey(String contentKey) {
		this.contentKey = contentKey;
	}
	
}
