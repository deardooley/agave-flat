package org.iplantc.service.notification.providers.email;

import java.util.HashMap;
import java.util.Map;

import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.model.NotificationAttemptResponse;
import org.iplantc.service.notification.providers.NotificationAttemptProvider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EmailNotificationAttemptProvider implements NotificationAttemptProvider {
	
	private NotificationAttempt attempt;
	
	public EmailNotificationAttemptProvider(NotificationAttempt attempt) {
		this.attempt = attempt;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.providers.NotificationAttemptProvider#publish()
	 */
	@Override
	public NotificationAttemptResponse publish() throws NotificationException {
		NotificationAttemptResponse response = new NotificationAttemptResponse();
		
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode json = mapper.readTree(attempt.getContent());
			
			EmailClient client = EmailClientFactory.getInstance(Settings.EMAIL_PROVIDER);
			client.setCustomHeaders(getCustomHeaders());
			client.send(null, attempt.getCallbackUrl(), getAttemptEmailSubject(json), getAttemptEmailBody(json), getAttemptEmailHtmlBody(json));
			response.setCode(200);
			response.setMessage("Ok");
		} 
		catch (Exception e) {
			response.setCode(500);
			response.setMessage(e.getMessage());
		}
		
		return response;
	}
	
	/**
	 * Returns html body value from deserialized {@link NotificationAttempt#getContent()} 
	 * JSON object.
	 * @param json the deserialized {@link NotificationAttempt#getContent()} value
	 * @return
	 */
	private String getAttemptEmailHtmlBody(JsonNode json) {
		String htmlBody = "";
		
		if (json != null) {
			htmlBody = json.has("htmlBody") ? json.get("htmlBody").asText() : "";
		}
		
		return htmlBody;
	}
	
	/**
	 * Returns plain text body value from deserialized {@link NotificationAttempt#getContent()} 
	 * JSON object.
	 * @param json the deserialized {@link NotificationAttempt#getContent()} value
	 * @return
	 */
	private String getAttemptEmailBody(JsonNode json) {
		String body = "";
		
		if (json != null) {
			body = json.has("body") ? json.get("body").asText() : "";
		}
		
		return body;
	}


	/**
	 * Returns email subject from deserialized {@link NotificationAttempt#getContent()} 
	 * JSON object.
	 * @param json the deserialized {@link NotificationAttempt#getContent()} value
	 * @return
	 */
	private String getAttemptEmailSubject(JsonNode json) {
		String subject = "";
		
		if (json != null) {
			subject = json.has("subject") ? json.get("subject").asText() : "";
		}
		
		return subject;
	}

	/**
	 * Returns custom headers injected into all notification callbacks. This is 
	 * dropped on all not HTTP based email clients.
	 * @return
	 */
	protected Map<String, String> getCustomHeaders() {
		Map<String, String> customHeaders = new HashMap<String, String>();
		customHeaders.put("User-Agent", "Agave-Hookbot/"+ org.iplantc.service.common.Settings.getContainerId());
		customHeaders.put("X-Agave-Delivery", attempt.getUuid());
		customHeaders.put("X-Agave-Notification", attempt.getNotificationId());
        
		return customHeaders;
	}

}
