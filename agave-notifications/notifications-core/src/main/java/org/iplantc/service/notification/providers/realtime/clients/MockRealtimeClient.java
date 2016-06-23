/**
 * 
 */
package org.iplantc.service.notification.providers.realtime.clients;

import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.model.NotificationAttemptResponse;

/**
 * @author dooley
 *
 */
public class MockRealtimeClient extends AbstractRealtimeClient {
	
	/**
	 * Default no-args contructor will swallow notifications. 
	 */
	public MockRealtimeClient(NotificationAttempt attempt) {
		super(attempt);
	}

	@Override
	public NotificationAttemptResponse publish() throws NotificationException 
	{
		// Swallow notification and return 200-ok
		return new NotificationAttemptResponse(200, "200 ok");
	}

	@Override
	protected String getSupportedCallbackProviderType() {
		return "mock";
	}
	
	

}
