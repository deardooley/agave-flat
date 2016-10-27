package org.iplantc.service.notification.providers.http.clients;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType;
import org.iplantc.service.notification.providers.http.clients.WebhookClient;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SlackWebhookClient extends AbstractWebhookClient {
	
	private static final Logger	log	= Logger.getLogger(SlackWebhookClient.class);
	
	public SlackWebhookClient(NotificationAttempt attempt) {
		super(attempt);
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.providers.http.clients.AbstractWebhookClient#getSupportedCallbackProviderType()
	 */
	@Override
	public String getSupportedCallbackProviderType() {
		return NotificationCallbackProviderType.SLACK.name().toLowerCase();
	}
	
	/**
	 * Filters the content into a JSON formatted incoming webhook body
	 * suitable for posting to Slack. 
	 * 
	 * @see https://api.slack.com/incoming-webhooks
	 * @param content the original {@link NotificationAttempt#getContent()}
	 * @return
	 */
	@Override
	public String getFilteredContent(String content) {
		ObjectMapper mapper = new ObjectMapper();
        ObjectNode slackPayload = mapper.createObjectNode();
		
		try {
			ObjectNode jsonContent = (ObjectNode)mapper.readTree(content);
			boolean hasBody = jsonContent.hasNonNull("body");
			// markup the json response as a code block
			String body = hasBody ? "```" + jsonContent.get("body").asText() + "```" : null;
			String subject = jsonContent.get("subject").asText();
			
			slackPayload
				.put("username", Settings.SLACK_WEBHOOK_USERNAME)
				.put("mrkdwn", true);

			ObjectNode attachment = mapper.createObjectNode()
				.put("fallback", hasBody ? body : subject)
	            .put("color", jsonContent.get("color").asText())
	            .put("title", attempt.getEventName() + " Webhook Notification")
                .put("title_link", TenancyHelper.resolveURLToCurrentTenant(
                		Settings.IPLANT_NOTIFICATION_SERVICE + attempt.getNotificationId(), 
                		attempt.getTenantId()));	

			// enable markdown formatting on the content body
			attachment.putArray("mrkdwn_in").add("text").add("pretext");
			
			if (hasBody) {
				attachment.put("text", subject + "\n" + body);
			}
			else {
				attachment.put("text", subject);
			}

			slackPayload.putArray("attachments").add(attachment);

		}
		// if something goes wrong, just write a simple message
		catch (IOException e) {
			slackPayload.put("text", content)
						.put("username", Settings.SLACK_WEBHOOK_USERNAME)
						.put("mrkdwn", true);
			if (StringUtils.isEmpty(Settings.SLACK_WEBHOOK_ICON_EMOJI)) {
				slackPayload.put("icon_emoji", Settings.SLACK_WEBHOOK_ICON_EMOJI);
			} else {
				slackPayload.put("icon_url", Settings.SLACK_WEBHOOK_ICON_URL);
			}
		}
			
		return slackPayload.toString();
	}
}
