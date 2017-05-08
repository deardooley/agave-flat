package org.iplantc.service.notification.providers.realtime.clients;

import java.io.IOException;
import java.sql.Timestamp;

import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.notification.AbstractNotificationTest;
import org.iplantc.service.notification.TestDataHelper;
import org.iplantc.service.notification.dao.NotificationDao;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.model.NotificationAttemptResponse;
import org.json.JSONException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Test(groups={"integration"})
public class PushpinRealtimeClientTest extends AbstractNotificationTest {

	@BeforeClass
	public void beforeClass() {
		try {
			dataHelper = TestDataHelper.getInstance();

			HibernateUtil.getConfiguration();

			dao = new NotificationDao();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@AfterClass
	public void afterClass() throws NotificationException, IOException {
		clearNotifications();
	}

	@Test
	public void publish() throws NotificationException, JSONException,
			IOException {
		
		Notification notification = createRealtimeNotification(null);
		
		JsonNode json = new ObjectMapper().createObjectNode().put("foo", "bar");
		NotificationAttempt attempt = new NotificationAttempt(notification.getUuid(), 
				notification.getCallbackUrl(),
				notification.getOwner(), notification.getAssociatedUuid(), 
				"RUNNING", json.toString(), new Timestamp(System.currentTimeMillis()));

		RealtimeClient client = new PushpinRealtimeClient(attempt);
		NotificationAttemptResponse response = client.publish();
		
		Assert.assertEquals(response.getCode(), 200, "Publishing message to realtime pushpin server should return 200");

	}
}
