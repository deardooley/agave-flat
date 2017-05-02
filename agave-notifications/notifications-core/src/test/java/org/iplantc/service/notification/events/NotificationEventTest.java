package org.iplantc.service.notification.events;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.notification.AbstractNotificationTest;
import org.iplantc.service.notification.TestDataHelper;
import org.iplantc.service.notification.dao.FailedNotificationAttemptQueue;
import org.iplantc.service.notification.dao.NotificationDao;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.iplantc.service.notification.model.enumerations.NotificationStatusType;
import org.iplantc.service.notification.model.enumerations.RetryStrategyType;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups={"integration"})
public class NotificationEventTest extends AbstractNotificationTest {

	@BeforeClass
	public void beforeClass()
	{
		try
		{
			dataHelper = TestDataHelper.getInstance();
			
			HibernateUtil.getConfiguration();
			
			dao = new NotificationDao();
			
			clearDeadLetterQueue();
		}
		catch (Exception e)
		{	
			e.printStackTrace();
		}
	}

	private void clearDeadLetterQueue() {
//		MongoClient mongoClient = null;
//		MongoCredential credential = null;
//		DB db = null;
//		try {
//			// default admin u/p for the test container
//			credential = MongoCredential.createCredential(
//	                "admin", Settings.FAILED_NOTIFICATION_DB_SCHEME, "changeit".toCharArray());
//			
//			mongoClient = new MongoClient(new ServerAddress(Settings.FAILED_NOTIFICATION_DB_HOST, Settings.FAILED_NOTIFICATION_DB_PORT), Arrays.asList(credential));
//	    	
//			db = mongoClient.getDB(Settings.FAILED_NOTIFICATION_DB_SCHEME);
//			
//			for (String collection: db.getCollectionNames()) {
//				if (Arrays.asList("metadata","schema").contains(collection)) {
//					db.getCollection(collection).drop();
//				}
//			}
//		}
//		catch (Exception e) {
//			Assert.fail("Failed to clean out dead letter queues before tests run ",e);
//		}
//		finally {
//			try { mongoClient.close(); } catch (Exception e) {}
//		}
	}

	@AfterClass
	public void afterClass() throws NotificationException, IOException
	{
		clearNotifications();
		clearDeadLetterQueue();
	}
	
	@DataProvider(name = "fireProvider")
	private Object[][] fireProvider() throws Exception
	{
		Notification validEmail = createEmailNotification();
		Notification validURL = createWebhookNotification();
		Notification validPort = createWebhookNotification();
		validPort.setCallbackUrl(requestBin.toString().replace("requestb.in", "requestb.in:80") + TEST_URL_QUERY);
	
		
		return new Object[][] {
			{ validEmail, "Valid email address failed to send", true },
			{ validURL, "Valid url address failed to send", true },
			{ validPort, "Valid url with port failed to send", true },
		};
	}

	@Test(enabled=true, dataProvider = "fireProvider")
	public void fire(Notification notification, String errorMessage, boolean shouldSucceed) throws Exception
	{
		try {
			dao.persist(notification);
			
			NotificationAttempt attempt = NotificationMessageProcessor.process(notification, "SENT", TEST_USER, notification.getAssociatedUuid(), notification.toJSON());
			Assert.assertNotNull(attempt, "No attempt returned from processing notification");
			
			Assert.assertEquals(attempt.isSuccess(), shouldSucceed, "Unexpcted result from notification attempt");
			Notification updatedNotification = dao.findByUuidAcrossTenants(notification.getUuid());
			
			Assert.assertEquals(shouldSucceed, updatedNotification.getStatus() == NotificationStatusType.COMPLETE, errorMessage);
			
//			if (shouldSucceed) {
//				Assert.assertTrue(notification.isSuccess(), "Notification outcome was false when it should be true.");
//				Assert.assertNotNull(notification.getLastSent(), "Notification last sent time was not updated.");
//				Assert.assertEquals(notification.getResponseCode(), new Integer(200), "Notification last sent time was not updated.");
//			} else {
//				Assert.assertFalse(notification.isSuccess(), "Notification outcome was true when it should be false.");
//				Assert.assertNotNull(notification.getLastSent(), "Notification last sent time was not updated on failure.");
//				Assert.assertNotEquals(notification.getResponseCode(), new Integer(200), "Notification last sent time was not set to a failure code on failure.");
//			}
		}
		catch (NotificationException e) {
			Assert.fail(errorMessage, e);
		}
		finally {
			try { dao.delete(notification); } catch (Exception e) {}
		}
	}
	
	@DataProvider(name = "fireFailsOnBadWebhookProvider")
	private Object[][] fireFailsOnBadWebhookProvider() throws Exception
	{
		List<Object[]> testCases = new ArrayList<Object[]>();
		
		Notification timeoutUrl = createWebhookNotification();
		timeoutUrl.setCallbackUrl("http://httpbin.org/delay/45");
		testCases.add(new Object[]{ timeoutUrl, "URL that times out should fail within 30 seconds.", true });
		
		for (int i=300; i<527; i++) {
			Notification errorCallbackUrl = createWebhookNotification();
			errorCallbackUrl.setCallbackUrl("http://httpbin.org/status/" + i);
			
			testCases.add(new Object[] { errorCallbackUrl, "Webhook response of " + i + " should fail immediately", true });
		}
		
		return testCases.toArray(new Object[][]{});
	}
	
	@Test(enabled=true, dataProvider = "fireFailsOnBadWebhookProvider")
	public void fireFailsOnBadWebhook(Notification notification, String errorMessage, boolean shouldThrowException) throws Exception
	{
		try 
		{
			dao.persist(notification);
			Date originalLastUpdated = notification.getLastUpdated();
			NotificationMessageProcessor.process(notification, "SENT", TEST_USER, notification.getAssociatedUuid(), notification.toJSON());
			
			Assert.assertNotEquals(shouldThrowException, errorMessage);
			
			Assert.assertFalse(notification.isSuccess(), "Notification outcome was true when it should be false.");
			Assert.assertNotNull(notification.getLastUpdated().after(originalLastUpdated), "Notification last sent time was not updated on failure.");
		}
		catch (NotificationException e) {
			if (!shouldThrowException) {
				Assert.fail(errorMessage, e);
			}
		}
		finally {
			try { dao.delete(notification); } catch (Exception e) {
				Assert.fail("Failed to delete test notification", e);
			}
		}
	}
	
	@Test(enabled=true, dependsOnMethods={"fireFailsOnBadWebhook"})
	public void failedNotificationAttemptWithSavePolicySaves() throws Exception {
		Notification notification = null;
	
		try {
			notification = createWebhookNotification();
			notification.setCallbackUrl("https://httpbin.org/status/500");
			notification.getPolicy().setRetryStrategyType(RetryStrategyType.NONE);
			notification.getPolicy().setSaveOnFailure(true);
			dao.persist(notification);
			
			// run the test
			NotificationMessageProcessor.process(notification, "SENT", TEST_USER, notification.getAssociatedUuid(), notification.toJSON());
			
			// it shoudl fail and not retry. resulting in a record in the dead letter queue named after the
			// notification uuid
			NotificationAttempt attempt = FailedNotificationAttemptQueue.getInstance().next(notification.getUuid());
			
			Assert.assertNotNull(attempt, "Result of failed webhook should be in failed queue when an exception is not thrown.");
			Assert.assertEquals(attempt.getAttemptNumber(), 1, "Notification attempt counter should be incremented to 1 even on failure.");
			Assert.assertEquals(attempt.getResponse().getCode(), 500, "Attempt response code should match the 500 from the httpbin response.");
		}
		catch (NotificationException e) {
			Assert.fail("No exception should be thrown processing a notification.", e);
		}
		finally {
			try { dao.delete(notification); } catch (Exception e) {
				Assert.fail("Failed to delete test notification", e);
			}
			
			try {
				// skipping cleanup since no notification is used twice, thus no bucket
				// duplication. after this we'll just blow away the container. much faster
//				FailedNotificationAttemptQueue.getInstance().removeAll(notification.getUuid());
			} catch (Exception e) {
				Assert.fail("Failed to delete test notification attempt collection", e);
			}
		}
	}
	
//	@Test
//	public void fireFailsOnBadWebhook() throws Exception
//	{
//		Notification notification = new Notification();
//		notification.setAssociatedUuid("2453452449026871781-242ac113-0001-007");
////		notification.setCallbackUrl("https://vdj-dev.tacc.utexas.edu/api/v1/notifications/jobs/2453452449026871781-242ac113-0001-007?status=PENDING&event=PENDING&error=Job%20accepted%20and%20queued%20for%20submission.&projectUuid=8032710074285756901-242ac11d-0001-012&jobName=notification_test_job");
////		notification.setCallbackUrl("https://agaveapi.co/?status=PENDING&event=PENDING&error=Job%20accepted%20and%20queued%20for%20submission.&projectUuid=8032710074285756901-242ac11d-0001-012&jobName=notification_test_job");
//		notification.setOwner(TEST_USER);
//		notification.setPersistent(true);
//		
////		NotificationEvent2 event = NotificationEventFactory.getInstance(new AgaveUUID(notification.getAssociatedUuid()), notification, "RUNNING", TEST_USER);
////		event.setCustomNotificationMessageContextData("{\"_links\": {\"app\": {\"href\": \"https://vdj-agave-api.tacc.utexas.edu/apps/v2/vdj_pipe-small-0.1.6u4\"},\"archiveData\": {\"href\": \"https://vdj-agave-api.tacc.utexas.edu/files/v2/listings/system/data.vdjserver.org//projects/8032710074285756901-242ac11d-0001-012/analyses/2016-03-03-21-39-26-60-my-job-3-mar-2016-3:39:37-pm\"},\"archiveSystem\": {\"href\": \"https://vdj-agave-api.tacc.utexas.edu/systems/v2/data.vdjserver.org\"},\"executionSystem\": {\"href\": \"https://vdj-agave-api.tacc.utexas.edu/systems/v2/vdj-exec-02.tacc.utexas.edu\"},\"history\": {\"href\": \"https://vdj-agave-api.tacc.utexas.edu/jobs/v2/2453452449026871781-242ac113-0001-007/history\"},\"metadata\": {\"href\": \"https://vdj-agave-api.tacc.utexas.edu/meta/v2/data/?q={\\\"associationIds\\\":\\\"2453452449026871781-242ac113-0001-007\\\"}\"},\"notifications\": {\"href\": \"https://vdj-agave-api.tacc.utexas.edu/notifications/v2/?associatedUuid=2453452449026871781-242ac113-0001-007\"},\"owner\": {\"href\": \"https://vdj-agave-api.tacc.utexas.edu/profiles/v2/vdj\"},\"permissions\": {\"href\": \"https://vdj-agave-api.tacc.utexas.edu/jobs/v2/2453452449026871781-242ac113-0001-007/pems\"},\"self\": {\"href\": \"https://vdj-agave-api.tacc.utexas.edu/jobs/v2/2453452449026871781-242ac113-0001-007\"}},\"appId\": \"vdj_pipe-small-0.1.6u4\",\"archive\": true,\"archivePath\": \"/projects/8032710074285756901-242ac11d-0001-012/analyses/2016-03-03-21-39-26-60-my-job-3-mar-2016-3:39:37-pm\",\"archiveSystem\": \"data.vdjserver.org\",\"batchQueue\": \"normal\",\"created\": \"2016-03-03T16:27:37.000-06:00\",\"endTime\": \"2016-03-03T16:28:42.000-06:00\",\"executionSystem\": \"vdj-exec-02.tacc.utexas.edu\",\"id\": \"2453452449026871781-242ac113-0001-007\",\"inputs\": {\"files\": \"agave://data.vdjserver.org//projects/8032710074285756901-242ac11d-0001-012/files/mid_pair_1.fastq\"},\"localId\": \"32285\",\"maxRunTime\": \"999:59:59\",\"memoryPerNode\": 1.0,\"name\": \"My Job 3-Mar-2016 3:39:37 pm\",\"nodeCount\": 1,\"outputPath\": \"/home/vdj/scratch/vdj/job-2453452449026871781-242ac113-0001-007-my-job-3-mar-2016-3-39-37-pm\",\"owner\": \"vdj\",\"parameters\": {\"help\": \"false\",\"json\": \"{\\\"base_path_input\\\":\\\"\\\",\\\"base_path_output\\\":\\\"\\\",\\\"summary_output_path\\\":\\\"summary.txt\\\",\\\"input\\\":[{\\\"sequence\\\":\\\"mid_pair_1.fastq\\\"}],\\\"steps\\\":[{\\\"find_shared\\\":{\\\"out_group_unique\\\":\\\"find-unique.fasta\\\",\\\"out_duplicates\\\":\\\"find-unique.duplicates.tsv\\\"}}]}\",\"outdir\": [\"./\"]},\"processorsPerNode\": 1,\"retries\": 0,\"startTime\": \"2016-03-03T16:28:25.000-06:00\",\"status\": \"FINISHED\",\"submitTime\": \"2016-03-03T16:28:24.000-06:00\"}");
////		event.postCallback();
//	}
}
