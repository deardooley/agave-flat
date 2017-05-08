package org.iplantc.service.notification;

import static org.iplantc.service.notification.TestDataHelper.NOTIFICATION_CREATOR;
import static org.iplantc.service.notification.TestDataHelper.NOTIFICATION_STRANGER;
import static org.iplantc.service.notification.TestDataHelper.TEST_EMAIL_NOTIFICATION;
import static org.iplantc.service.notification.TestDataHelper.TEST_WEBHOOK_NOTIFICATION;
import static org.iplantc.service.notification.TestDataHelper.TEST_REALTIME_NOTIFICATION;
import static org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType.AGAVE;
import static org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType.EMAIL;
import static org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType.REALTIME;
import static org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType.SLACK;
import static org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType.WEBHOOK;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.iplantc.service.common.clients.RequestBin;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.notification.dao.NotificationDao;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType;
import org.iplantc.service.notification.model.enumerations.NotificationStatusType;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups={"integration"})
public class AbstractNotificationTest {

	protected static final String TEST_USER = "ipcservices";
	protected static final String TEST_EMAIL = "dooley@tacc.utexas.edu";
	protected static final String TEST_URL = "http://requestb.in/11pbi6m1?username=${USERNAME}&status=${STATUS}";
	protected static final String TEST_URL_QUERY = "?username=${USERNAME}&status=${STATUS}";
	protected static final String SPECIFIC_ASSOCIATED_UUID = "abc1234-abc1234-abc1234-abc1234-011";
	protected static final String DECOY_ASSOCIATED_UUID = "def5678-def5678-def5678-def5678-011";
	protected static final String WILDCARD_ASSOCIATED_UUID = "*";
	
	protected NotificationDao dao = null;
	protected TestDataHelper dataHelper;
	protected RequestBin requestBin;
	protected Scheduler sched;
	protected SimpleTrigger trigger;
	
	protected void addNotifications(int instances, NotificationStatusType status, String associatedUuid, boolean stranger, NotificationCallbackProviderType type) 
	throws Exception
	{
		for (int i=0;i<instances;i++)
		{
			Notification n = createNotification(type);
			n.setStatus(status);
			n.setAssociatedUuid( associatedUuid );
			n.setOwner( stranger ? NOTIFICATION_STRANGER : NOTIFICATION_CREATOR );
			
			dao.persist(n);
			
			Assert.assertNotNull(n.getId(), "Failed to save notification " + i);
		}
	}
	
	protected Notification createNotification(NotificationCallbackProviderType type) throws NotificationException, IOException {
		
		if (type == EMAIL) {
			return createEmailNotification();
		} else if (type == WEBHOOK) {
			return createWebhookNotification();
		} else if (type == AGAVE) {
			return createAgaveWebhookNotification();
		} else if (type == SLACK) {
			return createSlackNotification();
		} else if (type == REALTIME) {
			return createRealtimeNotification("/");
		} else {
			return createInvalidNotification();
		}
		
	}
	
	protected Notification createInvalidNotification() throws NotificationException, IOException
	{
		Notification notification = Notification.fromJSON(dataHelper.getTestDataObject(TEST_EMAIL_NOTIFICATION));
		notification.setCallbackUrl("ftp://foo.example.com");
		notification.setOwner(NOTIFICATION_CREATOR);
		
		return notification;
	}
	
	protected Notification createEmailNotification() throws NotificationException, IOException
	{
		Notification notification = Notification.fromJSON(dataHelper.getTestDataObject(TEST_EMAIL_NOTIFICATION));
		notification.setOwner(NOTIFICATION_CREATOR);
		
		return notification;
	}
	
	protected Notification createWebhookNotification() throws NotificationException, IOException
	{
		if (requestBin == null) {
			requestBin = RequestBin.getInstance();
		}
		Notification notification = Notification.fromJSON(dataHelper.getTestDataObject(TEST_WEBHOOK_NOTIFICATION));
		notification.setOwner(NOTIFICATION_CREATOR);
		notification.setCallbackUrl(requestBin.toString() + TEST_URL_QUERY);
		
		return notification;
	}
	
	protected Notification createSlackNotification() throws NotificationException, IOException
	{
		if (requestBin == null) {
			requestBin = RequestBin.getInstance();
		}
		Notification notification = Notification.fromJSON(dataHelper.getTestDataObject(TEST_WEBHOOK_NOTIFICATION));
		notification.setOwner(NOTIFICATION_CREATOR);
		notification.setCallbackUrl("https://hooks.slack.com/services/TTTTTTTTT/BBBBBBBBB/1234567890123456789012345");
		
		return notification;
	}
	
	protected Notification createAgaveWebhookNotification() throws NotificationException, IOException
	{
		if (requestBin == null) {
			requestBin = RequestBin.getInstance();
		}
		
		Notification notification = Notification.fromJSON(dataHelper.getTestDataObject(TEST_WEBHOOK_NOTIFICATION));
		notification.setOwner(NOTIFICATION_CREATOR);
		
		return notification;
	}
	
	protected Notification createRealtimeNotification(String channel) throws NotificationException, IOException
	{
		Notification notification = Notification.fromJSON(dataHelper.getTestDataObject(TEST_REALTIME_NOTIFICATION));
		notification.setOwner(NOTIFICATION_CREATOR);
		String realtimeUrl = TenancyHelper.resolveURLToCurrentTenant("https://docker.example.com/realtime");
		if (!StringUtils.isEmpty(channel)) {
			notification.setCallbackUrl(realtimeUrl + "/" + channel);
		} else {
			notification.setCallbackUrl(realtimeUrl);
		}
		
		return notification;
	}
	
	protected Notification createRealtimeNotification() throws NotificationException, IOException
	{
		return createRealtimeNotification(null);
	}
	
	@SuppressWarnings("unchecked")
	protected void clearNotifications() throws NotificationException
	{
		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			
			List<Notification> notifications = session.createQuery("from Notification").list();
			for (Notification notification: notifications) {
				session.delete(notification);
			}
			
			session.flush();
		}
		catch (HibernateException ex)
		{
			throw new NotificationException(ex);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}
}
