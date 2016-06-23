package org.iplantc.service.notification.providers.http.clients;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType;
import org.iplantc.service.notification.providers.http.clients.WebhookClient;

import com.fasterxml.jackson.databind.ObjectMapper;
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
		ObjectNode json = mapper.createObjectNode()
				.put("text", content)
				.put("username", Settings.SLACK_WEBHOOK_USERNAME);
		if (StringUtils.isEmpty(Settings.SLACK_WEBHOOK_ICON_EMOJI)) {
			json.put("icon_emoji", Settings.SLACK_WEBHOOK_ICON_URL);
		} else {
			json.put("icon_url", Settings.SLACK_WEBHOOK_ICON_EMOJI);
		}
		
		return json.toString();
	}
}
