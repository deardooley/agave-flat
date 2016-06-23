package org.iplantc.service.notification.dao;

import static org.iplantc.service.notification.TestDataHelper.NOTIFICATION_CREATOR;
import static org.iplantc.service.notification.TestDataHelper.NOTIFICATION_STRANGER;
import static org.iplantc.service.notification.TestDataHelper.TEST_EMAIL_NOTIFICATION;
import static org.iplantc.service.notification.TestDataHelper.TEST_WEBHOOK_NOTIFICATION;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import org.codehaus.plexus.util.StringUtils;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.notification.AbstractNotificationTest;
import org.iplantc.service.notification.TestDataHelper;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.notification.model.enumerations.NotificationCallbackProviderType;
import org.iplantc.service.notification.model.enumerations.NotificationStatusType;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(singleThreaded=true)
public class NotificationDaoTest extends AbstractNotificationTest 
{
	private int totalActiveValidSpecificNotifications = 7;// totalActiveValidSpecificNotificationsForUser + totalActiveValidSpecificNotificationsForStranger;
	private int totalActiveValidSpecificNotificationsForUser = 6;
	private int totalActiveValidSpecificNotificationsForStranger = 1;
	
	private int totalActiveValidWildcardUuidNotifications = 2;//totalActiveValidWildcardUuidNotificationsForUser + totalActiveValidWildcardUuidNotificationsForStranger;
	private int totalActiveValidWildcardUuidNotificationsForUser = 1;
	private int totalActiveValidWildcardUuidNotificationsForStranger = 1;
	
	private int totalActiveDecoySpecificNotifications = 2;//totalActiveDecoySpecificNotificationsForUser + totalActiveDecoySpecificNotificationsForStranger;
	private int totalActiveDecoySpecificNotificationsForUser = 1;
	private int totalActiveDecoySpecificNotificationsForStranger = 1;
	
	// active notification rollup
	private int totalActiveValidNotifications = 9;//totalActiveValidSpecificNotifications + totalActiveValidWildcardUuidNotifications;
	private int totalActiveValidNotificationsForUser = 7;//totalActiveValidSpecificNotificationsForUser + totalActiveValidWildcardUuidNotificationsForUser;
	private int totalActiveValidNotificationsForStranger = 2;//totalActiveValidSpecificNotificationsForStranger + totalActiveValidWildcardUuidNotificationsForStranger;
	
	private int totalActiveDecoyNotifications = 4;//totalActiveValidWildcardUuidNotifications + totalActiveDecoySpecificNotifications;
	private int totalActiveDecoyNotificationsForUser = 2;//totalActiveValidWildcardUuidNotificationsForUser + totalActiveDecoySpecificNotificationsForUser;
	private int totalActiveDecoyNotificationsForStranger = 2;//totalActiveValidWildcardUuidNotificationsForStranger + totalActiveDecoySpecificNotificationsForStranger;
	
	private int totalActiveNotifications = 11;//totalActiveNotificationsForUser + totalActiveNotificationsForStranger;
	private int totalActiveNotificationsForUser = 8;//totalActiveValidSpecificNotificationsForUser + totalActiveValidWildcardUuidNotificationsForUser + totalActiveDecoySpecificNotificationsForStranger;
	private int totalActiveNotificationsForStranger = 3;//totalActiveValidSpecificNotificationsForStranger + totalActiveValidWildcardUuidNotificationsForStranger + totalActiveDecoySpecificNotificationsForStranger;
	
	// expired notifications
	private int totalPastSpecificNotifications = 3;//totalPastSpecificNotificationsForUser + totalPastSpecificNotificationsForStranger;
	private int totalPastSpecificNotificationsForUser = 2;
	private int totalPastSpecificNotificationsForStranger = 1;
	
	private int totalPastWildcardNotifications = 2;//totalPastWildcardNotificationsForUser + totalPastWildcardNotificationsForStranger;
	private int totalPastWildcardNotificationsForUser = 1;
	private int totalPastWildcardNotificationsForStranger = 1;
	
	private int totalPastDecoySpecificNotifications = 2;//totalPastDecoyNotificationsForUser + totalPastDecoyNotificationsForStranger;
	private int totalPastDecoySpecificNotificationsForUser = 1;
	private int totalPastDecoySpecificNotificationsForStranger = 1;
	
	
	// expired notification rollups
	private int totalPastValidNotifications = 5;//totalPastSpecificNotifications + totalPastWildcardNotifications;
	private int totalPastValidNotificationsForUser = 3;//totalPastSpecificNotificationsForUser + totalPastWildcardNotificationsForUser;
	private int totalPastValidNotificationsForStranger = 2;//totalPastSpecificNotificationsForStranger + totalPastWildcardNotificationsForStranger;

	private int totalPastDecoyNotifications = 4;//totalPastDecoySpecificNotifications + totalPastWildcardNotifications;
	private int totalPastDecoyNotificationsForUser = 2;//totalPastWildcardNotificationsForUser + totalPastDecoySpecificNotificationsForUser;
	private int totalPastDecoyNotificationsForStranger = 2;//totalPastWildcardNotificationsForStranger + totalPastDecoySpecificNotificationsForStranger;
	
	private int totalPastNotifications = 7;//totalPastSpecificNotifications + totalPastWildcardNotifications + totalPastDecoySpecificNotifications;
	
	// overall rollups
	private int totalNotifications = 18;//totalActiveNotifications + totalPastNotifications;
	
	private void loadTestData() 
	{
		try 
		{	
			addNotifications(totalActiveValidSpecificNotificationsForUser, NotificationStatusType.ACTIVE, SPECIFIC_ASSOCIATED_UUID, false, NotificationCallbackProviderType.EMAIL);
			addNotifications(totalActiveValidSpecificNotificationsForStranger, NotificationStatusType.ACTIVE, SPECIFIC_ASSOCIATED_UUID, true, NotificationCallbackProviderType.EMAIL);
			addNotifications(totalActiveValidWildcardUuidNotificationsForUser, NotificationStatusType.ACTIVE, WILDCARD_ASSOCIATED_UUID, false, NotificationCallbackProviderType.EMAIL);
			addNotifications(totalActiveValidWildcardUuidNotificationsForStranger, NotificationStatusType.ACTIVE, WILDCARD_ASSOCIATED_UUID, true, NotificationCallbackProviderType.EMAIL);
			addNotifications(totalActiveDecoySpecificNotificationsForUser, NotificationStatusType.ACTIVE, DECOY_ASSOCIATED_UUID, false, NotificationCallbackProviderType.EMAIL);
			addNotifications(totalActiveDecoySpecificNotificationsForStranger, NotificationStatusType.ACTIVE, DECOY_ASSOCIATED_UUID, true, NotificationCallbackProviderType.EMAIL);
			
			addNotifications(totalPastSpecificNotificationsForUser, NotificationStatusType.INACTIVE, SPECIFIC_ASSOCIATED_UUID, false, NotificationCallbackProviderType.EMAIL);
			addNotifications(totalPastSpecificNotificationsForStranger, NotificationStatusType.INACTIVE, SPECIFIC_ASSOCIATED_UUID, true, NotificationCallbackProviderType.EMAIL);
			addNotifications(totalPastWildcardNotificationsForUser, NotificationStatusType.INACTIVE, WILDCARD_ASSOCIATED_UUID, false, NotificationCallbackProviderType.EMAIL);
			addNotifications(totalPastWildcardNotificationsForStranger, NotificationStatusType.INACTIVE, WILDCARD_ASSOCIATED_UUID, true, NotificationCallbackProviderType.EMAIL);
			addNotifications(totalPastDecoySpecificNotificationsForUser, NotificationStatusType.INACTIVE, DECOY_ASSOCIATED_UUID, false, NotificationCallbackProviderType.EMAIL);
			addNotifications(totalPastDecoySpecificNotificationsForStranger, NotificationStatusType.INACTIVE, DECOY_ASSOCIATED_UUID, true, NotificationCallbackProviderType.EMAIL);
		} 
		catch (Exception e) {
			Assert.fail("Failed to retrive notification for uuid", e);
		}
	}
	
	@BeforeClass
	protected void beforeClass()
	{
		try
		{
			dataHelper = TestDataHelper.getInstance();
			
			HibernateUtil.getConfiguration();
			
			dao = new NotificationDao();
		}
		catch (Exception e)
		{	
			e.printStackTrace();
		}
	}

	@AfterClass
	protected void afterClass() throws NotificationException
	{
		clearNotifications();
	}
	
//	@BeforeMethod
//	public void beforeMethod() throws NotificationException
//	{
//		clearNotifications();
//	}

	@AfterMethod
	protected void afterMethod() throws NotificationException
	{
		clearNotifications();
	}

	@DataProvider(name="persistProvider")
	protected Object[][] persistProvider() throws Exception
	{
		return new Object[][] { 
				{ createEmailNotification(), "Failed to persist notification", false },
				{ createWebhookNotification(), "Failed to persist notification", false }
		};
	}
	
	@Test(dataProvider="persistProvider")
	public void persist(Notification n, String errorMessage, boolean shouldThrowException)
	{
		try 
		{
			dao.persist(n);
			Assert.assertNotNull(n.getId(), "Failed to generate an notification ID.");
		} 
		catch(Exception e) 
		{
			if (!shouldThrowException) {
				Assert.fail(errorMessage, e);
			}
		}
	}
	
	@Test(dependsOnMethods={"persist"})
	public void getAll()
	{
		try 
		{
			loadTestData();
			
			List<Notification> notifs = dao.getAll();
			
			Assert.assertFalse(notifs.isEmpty(), "None of the " + totalNotifications + " test notifications found.");
			Assert.assertEquals(notifs.size(), totalNotifications, "Wrong number of notifications found. " +
					totalNotifications + " saved, " + notifs.size() + " found.");
		} 
		catch(Exception e) 
		{
			Assert.fail("Failed to retrieve notifications from the db.", e);
		}
	}
	
	@DataProvider(name="findByUuidProvider")
	protected Object[][] findByUuidProvider() throws Exception
	{
		return new Object[][] { 
				{ createEmailNotification(), "Failed to find notification by uuid", false },
				{ createWebhookNotification(), "Failed to find notification by uuid", false }
		};
	}
	
	@Test(dataProvider="findByUuidProvider", dependsOnMethods={"getAll"})
	public void findByUuid(Notification n, String errorMessage, boolean shouldThrowException)
	{
		try 
		{
			dao.persist(n);
			Assert.assertNotNull(n.getId(), "Failed to generate an notification ID.");
			
			String notifUuid = n.getUuid();
			
			n = dao.findByUuid(notifUuid);
			
			Assert.assertNotNull(n, "Notifiation was not found in db.");
		} 
		catch(Exception e) 
		{
			if (!shouldThrowException) {
				Assert.fail(errorMessage, e);
			}
		}
	}
	
	@Test(dependsOnMethods={"findByUuid"})
	public void getActiveNotificationsForUuid()
	{	
		try 
		{
			loadTestData();
			
			// check we can pull notifications for primary uuid 
			List<Notification> notifs = dao.getActiveNotificationsForUuid(SPECIFIC_ASSOCIATED_UUID);
			
			Assert.assertFalse(notifs.isEmpty(), "None of the " + totalActiveValidNotifications + 
					" active test notifications for " + SPECIFIC_ASSOCIATED_UUID + " found.");
			Assert.assertEquals(notifs.size(), totalActiveValidNotifications, "Wrong number of notifications found for " +
					SPECIFIC_ASSOCIATED_UUID + ". " + totalActiveValidNotifications + " saved, " + notifs.size() + " found.");
			
			int specificAssociatedUuidFound = 0;
			int wildcardAssociatedUuidFound = 0;
			for (Notification n: notifs) {
				if (StringUtils.equalsIgnoreCase(n.getAssociatedUuid(), "*")) {
					wildcardAssociatedUuidFound++;
				} else { 
					specificAssociatedUuidFound++;
				}
			}
			
			Assert.assertEquals(specificAssociatedUuidFound, totalActiveValidSpecificNotifications, "Wrong number of notifications with a specific UUID found for " +
					SPECIFIC_ASSOCIATED_UUID + ". " + totalActiveValidSpecificNotifications + " saved, " + SPECIFIC_ASSOCIATED_UUID + " found.");
			
			Assert.assertEquals(wildcardAssociatedUuidFound, totalActiveValidWildcardUuidNotifications, "Wrong number of notifications with a wildcard UUID found for " +
					SPECIFIC_ASSOCIATED_UUID + ". " + totalActiveValidWildcardUuidNotifications + " saved, " + wildcardAssociatedUuidFound + " found.");
			
			// check we can pull notifications for decoys uuid as well
			notifs = dao.getActiveNotificationsForUuid(DECOY_ASSOCIATED_UUID);
			
			Assert.assertFalse(notifs.isEmpty(), "None of the " + totalActiveDecoyNotifications + 
					" active test notifications for " + DECOY_ASSOCIATED_UUID + " found.");
			
			Assert.assertEquals(notifs.size(), totalActiveDecoyNotifications, "Wrong number of decoy notifications found for " +
					DECOY_ASSOCIATED_UUID + ". " + totalActiveDecoyNotifications + " saved, " + notifs.size() + " found.");
			
			specificAssociatedUuidFound = 0;
			wildcardAssociatedUuidFound = 0;
			for (Notification n: notifs) {
				if (StringUtils.equalsIgnoreCase(n.getAssociatedUuid(), "*")) {
					wildcardAssociatedUuidFound++;
				} else { 
					specificAssociatedUuidFound++;
				}
			}
			
			Assert.assertEquals(specificAssociatedUuidFound, totalActiveDecoySpecificNotifications, "Wrong number of decoy notifications with a specific UUID found for " +
					DECOY_ASSOCIATED_UUID + ". " + totalActiveDecoySpecificNotifications + " saved, " + notifs.size() + " found.");
			
			Assert.assertEquals(wildcardAssociatedUuidFound, totalActiveValidWildcardUuidNotifications, "Wrong number of notifications with a wildcard UUID found for " +
					SPECIFIC_ASSOCIATED_UUID + ". " + totalActiveValidWildcardUuidNotifications + " saved, " + wildcardAssociatedUuidFound + " found.");
		} 
		catch (Exception e) {
			Assert.fail("Failed to retrive active notifications for uuid", e);
		}
	}

	@Test(dependsOnMethods={"getActiveNotificationsForUuid"})
	public void getActiveUserNotifications()
	{
		try 
		{
			loadTestData();
			
			// check we can pull notifications for primary user 
			List<Notification> notifs = dao.getActiveUserNotifications(NOTIFICATION_CREATOR);
			
			Assert.assertFalse(notifs.isEmpty(), "None of the " + totalActiveValidNotifications + 
					" active test notifications for " + NOTIFICATION_CREATOR + " found.");
			Assert.assertEquals(notifs.size(), totalActiveNotificationsForUser, "Wrong number of notifications found for " +
					NOTIFICATION_CREATOR + ". " + totalActiveNotificationsForUser + " saved, " + notifs.size() + " found.");
			
			int specificAssociatedUuidFound = 0;
			int decoyAssociatedUuidFound = 0;
			int wildcardAssociatedUuidFound = 0;
			for (Notification n: notifs) {
				if (StringUtils.equalsIgnoreCase(n.getAssociatedUuid(), "*")) {
					wildcardAssociatedUuidFound++;
				} else if (StringUtils.equalsIgnoreCase(n.getAssociatedUuid(), SPECIFIC_ASSOCIATED_UUID)){ 
					specificAssociatedUuidFound++;
				} else {
					decoyAssociatedUuidFound++;
				}
			}
			
			Assert.assertEquals(specificAssociatedUuidFound, totalActiveValidSpecificNotificationsForUser, "Wrong number of notifications found for " +
					NOTIFICATION_CREATOR + ". " + totalActiveValidSpecificNotificationsForUser + " saved, " + specificAssociatedUuidFound + " found.");
			
			Assert.assertEquals(wildcardAssociatedUuidFound, totalActiveValidWildcardUuidNotificationsForUser, "Wrong number of notifications with a wildcard UUID found for " + 
					NOTIFICATION_CREATOR + ". " + totalActiveValidWildcardUuidNotificationsForUser + " saved, " + wildcardAssociatedUuidFound + " found.");
			
			Assert.assertEquals(decoyAssociatedUuidFound, totalActiveDecoySpecificNotificationsForUser, "Wrong number of decoy notifications found for " +
					NOTIFICATION_CREATOR + ". " + totalActiveDecoySpecificNotificationsForUser + " saved, " + decoyAssociatedUuidFound + " found.");
			
			
			
			// check we can pull notifications for other user as well
			notifs = dao.getActiveUserNotifications(NOTIFICATION_STRANGER);
			
			Assert.assertFalse(notifs.isEmpty(), "None of the " + totalActiveNotificationsForStranger + 
					" active test notifications for " + NOTIFICATION_STRANGER + " found.");
			Assert.assertEquals(notifs.size(), totalActiveNotificationsForStranger, "Wrong number of decoy notifications found for " +
					NOTIFICATION_STRANGER + ". " + totalActiveNotificationsForStranger + " saved, " + notifs.size() + " found.");
			
			specificAssociatedUuidFound = 0;
			decoyAssociatedUuidFound = 0;
			wildcardAssociatedUuidFound = 0;
			for (Notification n: notifs) {
				if (StringUtils.equalsIgnoreCase(n.getAssociatedUuid(), "*")) {
					wildcardAssociatedUuidFound++;
				} else if (StringUtils.equalsIgnoreCase(n.getAssociatedUuid(), SPECIFIC_ASSOCIATED_UUID)){ 
					specificAssociatedUuidFound++;
				} else {
					decoyAssociatedUuidFound++;
				}
			}
			
			Assert.assertEquals(specificAssociatedUuidFound, totalActiveValidSpecificNotificationsForStranger, "Wrong number of decoy notifications found for " +
					NOTIFICATION_STRANGER + ". " + totalActiveValidSpecificNotificationsForStranger + " saved, " + SPECIFIC_ASSOCIATED_UUID + " found.");
			
			Assert.assertEquals(wildcardAssociatedUuidFound, totalActiveValidWildcardUuidNotificationsForStranger, "Wrong number of decoy notifications with a wildcard UUID found for " +
					NOTIFICATION_STRANGER + ". " + totalActiveValidWildcardUuidNotificationsForStranger + " saved, " + wildcardAssociatedUuidFound + " found.");
			
			Assert.assertEquals(decoyAssociatedUuidFound, totalActiveDecoySpecificNotificationsForStranger, "Wrong number of decoy notifications found for " +
					NOTIFICATION_CREATOR + ". " + totalActiveDecoySpecificNotificationsForStranger + " saved, " + decoyAssociatedUuidFound + " found.");
			
			
		} 
		catch (Exception e) {
			Assert.fail("Failed to retrive active notifications for user", e);
		}
	}

	@Test(dependsOnMethods={"getActiveUserNotifications"})
	public void getActiveUserNotificationsForUuid()
	{
		try 
		{
			loadTestData();
			
			// check we can pull notifications for primary user 
			List<Notification> notifs = dao.getActiveUserNotificationsForUuid(NOTIFICATION_CREATOR, SPECIFIC_ASSOCIATED_UUID);
			
			Assert.assertFalse(notifs.isEmpty(), "None of the " + totalActiveValidNotificationsForUser + 
					" active test notifications for " + NOTIFICATION_CREATOR + " found.");
			Assert.assertEquals(notifs.size(), totalActiveValidNotificationsForUser, "Wrong number of notifications found for " +
					NOTIFICATION_CREATOR + ". " + totalActiveValidNotificationsForUser + " saved, " + notifs.size() + " found.");
			
			int specificAssociatedUuidFound = 0;
			int decoyAssociatedUuidFound = 0;
			int wildcardAssociatedUuidFound = 0;
			for (Notification n: notifs) {
				if (StringUtils.equalsIgnoreCase(n.getAssociatedUuid(), "*")) {
					wildcardAssociatedUuidFound++;
				} else if (StringUtils.equalsIgnoreCase(n.getAssociatedUuid(), SPECIFIC_ASSOCIATED_UUID)){ 
					specificAssociatedUuidFound++;
				} else {
					decoyAssociatedUuidFound++;
				}
				Assert.assertEquals(NOTIFICATION_CREATOR, n.getOwner(), "Notifications from other users should not be returned when querying by user and uuid.");
			}
			
			Assert.assertEquals(specificAssociatedUuidFound, totalActiveValidSpecificNotificationsForUser, "Wrong number of notifications found for " +
					NOTIFICATION_CREATOR + ". " + totalActiveValidSpecificNotificationsForUser + " saved, " + specificAssociatedUuidFound + " found.");
			
			Assert.assertEquals(wildcardAssociatedUuidFound, totalActiveValidWildcardUuidNotificationsForUser, "Wrong number of notifications with a wildcard UUID found for " + 
					NOTIFICATION_CREATOR + ". " + totalActiveValidWildcardUuidNotificationsForUser + " saved, " + wildcardAssociatedUuidFound + " found.");
			
			Assert.assertEquals(decoyAssociatedUuidFound, 0, "Decoy notifications should not be returned when querying by specific user and uuid.");
			
			
			// check we can pull notifications for other user as well
			notifs = dao.getActiveUserNotificationsForUuid(NOTIFICATION_STRANGER, SPECIFIC_ASSOCIATED_UUID);
			
			Assert.assertFalse(notifs.isEmpty(), "None of the " + totalActiveDecoyNotificationsForUser + 
					" active test notifications for " + NOTIFICATION_STRANGER + " found.");
			Assert.assertEquals(notifs.size(), totalActiveDecoyNotificationsForUser, "Wrong number of decoy notifications found for " +
					NOTIFICATION_STRANGER + ". " + totalActiveDecoyNotificationsForUser + " saved, " + notifs.size() + " found.");
			
			specificAssociatedUuidFound = 0;
			decoyAssociatedUuidFound = 0;
			wildcardAssociatedUuidFound = 0;
			for (Notification n: notifs) {
				if (StringUtils.equalsIgnoreCase(n.getAssociatedUuid(), "*")) {
					wildcardAssociatedUuidFound++;
				} else if (StringUtils.equalsIgnoreCase(n.getAssociatedUuid(), SPECIFIC_ASSOCIATED_UUID)){ 
					specificAssociatedUuidFound++;
				} else {
					decoyAssociatedUuidFound++;
				}
				Assert.assertEquals(NOTIFICATION_STRANGER, n.getOwner(), "Notifications from primary user stranger should not be returned when querying by stranger and uuid.");
			}
			
			Assert.assertEquals(specificAssociatedUuidFound, totalActiveValidSpecificNotificationsForStranger, "Wrong number of notifications found for " +
					NOTIFICATION_STRANGER + ". " + totalActiveValidSpecificNotificationsForStranger + " saved, " + specificAssociatedUuidFound + " found.");
			
			Assert.assertEquals(wildcardAssociatedUuidFound, totalActiveValidWildcardUuidNotificationsForStranger, "Wrong number of notifications with a wildcard UUID found for " + 
					NOTIFICATION_STRANGER + ". " + totalActiveValidWildcardUuidNotificationsForStranger + " saved, " + wildcardAssociatedUuidFound + " found.");
			
			Assert.assertEquals(decoyAssociatedUuidFound, 0, "Decoy notifications should not be returned when querying by stranger and uuid.");
			
		} 
		catch (Exception e) {
			Assert.fail("Failed to retrive active notifications for user and uuid", e);
		}
	}

	@DataProvider(name="deleteProvider")
	protected Object[][] deleteProvider() throws Exception
	{
		return new Object[][] { 
				{ createEmailNotification(), "Failed to delete notification", false },
				{ createWebhookNotification(), "Failed to delete notification", false }
		};
	}

	@Test(dataProvider="deleteProvider", dependsOnMethods={"getActiveUserNotificationsForUuid"})
	public void delete(Notification n, String errorMessage, boolean shouldThrowException)
	{
		try 
		{
			dao.persist(n);
			Assert.assertNotNull(n.getId(), "Failed to generate an notification ID.");
			
			String notifUuid = n.getUuid();
			
			n = dao.findByUuid(notifUuid);
			
			Assert.assertNotNull(n, "Notifiation was not found in db.");
			
			dao.delete(n);
			
			n = dao.findByUuid(notifUuid);
			
			Assert.assertNull(n, "Notification was not found deleted from the db.");
		} 
		catch(Exception e) 
		{
			if (!shouldThrowException) {
				Assert.fail(errorMessage, e);
			}
		}
	}	
	
	@Test(dependsOnMethods={"delete"})
	public void getActiveNotificationsForUuidIgnoresTerminated()
	{
		try 
		{
			Notification n = createWebhookNotification();
			n.setStatus(NotificationStatusType.INACTIVE);
			n.setAssociatedUuid(new AgaveUUID(UUIDType.NOTIFICATION).getUniqueId());
			n.setEvent("*");
			dao.persist(n);
			
			Assert.assertNotNull(n.getId(), "Failed to generate a notification ID.");
			
			List<Notification> notifications = dao.getActiveNotificationsForUuid(n.getAssociatedUuid());
			
			Assert.assertTrue(notifications.isEmpty(), "Terminated notifiation was returned.");
		} 
		catch(Exception e) 
		{
			Assert.fail("Looking up valid notification should not throw exception", e);
		}
	}
	
	@Test(dependsOnMethods={"getActiveNotificationsForUuidIgnoresTerminated"})
	public void getActiveUserNotificationsForUuidIgnoresTerminated()
	{
		try 
		{
			Notification n = createWebhookNotification();
			n.setStatus(NotificationStatusType.INACTIVE);
			n.setAssociatedUuid(new AgaveUUID(UUIDType.NOTIFICATION).getUniqueId());
			n.setEvent("*");
			dao.persist(n);
			
			Assert.assertNotNull(n.getId(), "Failed to generate a notification ID.");
			
			List<Notification> notifications = dao.getActiveUserNotificationsForUuid(n.getOwner(), n.getAssociatedUuid());
			
			Assert.assertTrue(notifications.isEmpty(), "Terminated notifiation was returned.");
		} 
		catch(Exception e) 
		{
			Assert.fail("Looking up valid notification should not throw exception", e);
		}
	}
	
	@Test(dependsOnMethods={"getActiveUserNotificationsForUuidIgnoresTerminated"})
	public void getActiveUserNotificationsIgnoresTerminated()
	{
		try 
		{
			Notification n = createWebhookNotification();
			n.setStatus(NotificationStatusType.INACTIVE);
			n.setAssociatedUuid(new AgaveUUID(UUIDType.NOTIFICATION).getUniqueId());
			n.setEvent("*");
			dao.persist(n);
			
			Assert.assertNotNull(n.getId(), "Failed to generate a notification ID.");
			
			List<Notification> notifications = dao.getActiveUserNotifications(n.getOwner());
			
			Assert.assertTrue(notifications.isEmpty(), "Terminated notifiation was returned.");
		} 
		catch(Exception e) 
		{
			Assert.fail("Looking up valid notification should not throw exception", e);
		}
	}
	
}
