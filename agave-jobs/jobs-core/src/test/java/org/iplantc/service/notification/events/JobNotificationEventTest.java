package org.iplantc.service.notification.events;

import java.io.IOException;
import java.lang.reflect.Method;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.exceptions.SoftwareException;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.jobs.dao.AbstractDaoTest;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.notification.dao.NotificationDao;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Tests the processing of job event messages by the notifications api
 * @author dooley
 *
 */
public class JobNotificationEventTest extends AbstractDaoTest {

	protected NotificationDao ndao = new NotificationDao();
	
	@Override 
	@BeforeClass
	public void beforeClass() throws Exception {
		super.beforeClass();
		SoftwareDao.persist(software);
	}

	@AfterClass
	public void afterClass() throws Exception {
		clearJobs();
		clearSoftware();
		clearSystems();
		clearNotifications();
	}
	
	@BeforeMethod
	public void beforeMethod(Method m) throws Exception {
		clearJobs();
		clearNotifications();
	}

	@AfterClass
	public void clearNotifications() throws NotificationException
	{
		Session session = null;
		try
		{
			HibernateUtil.beginTransaction();
			session = HibernateUtil.getSession();
			session.clear();
			HibernateUtil.disableAllFilters();
            session.createQuery("delete Notification").executeUpdate();
			session.flush();
		}
		catch (HibernateException ex) {
			throw new SoftwareException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Throwable e) {}
		}
		
	}
	
	protected Notification createNotification(Job job) 
	throws NotificationException, IOException
	{
		Notification notification = new Notification(job.getUuid(), 
													job.getOwner(), 
													job.getStatus().name(), 
													"http://httpbin.agaveapi.co/post", 
													true);
		
		ndao.persist(notification);
		
		return notification;
	}

	protected Notification createJobSoftwareNotification(Job job, Software software) 
	throws NotificationException, IOException
	{
		Notification notification = new Notification(software.getUuid(), 
													job.getOwner(), 
													"JOBS_" + job.getStatus().name(), 
													"http://httpbin.agaveapi.co/post", 
													true);
		
		ndao.persist(notification);
		
		return notification;
	}
	
	
	protected Notification createJobSystemNotification(Job job, ExecutionSystem system) 
	throws NotificationException, IOException
	{
		Notification notification = new Notification(system.getUuid(), 
													job.getOwner(), 
													"JOB_" + job.getStatus().name(), 
													"http://httpbin.agaveapi.co/post", 
													true);
		
		ndao.persist(notification);
		
		return notification;
	}
	@DataProvider
	protected Object[][] processJobStatusEventsProvider() throws Exception {
		JobStatusType[] allStatuses = JobStatusType.values();
		
		Object[][] testData = new Object[allStatuses.length][1];
		
		
		for (int i=0; i<allStatuses.length; i++) {
			testData[i][0] = allStatuses[i];
		}
		return testData;
	}
	
	/**
	 * Test that job status events properly process their notification template
	 * variables.
	 * 
	 * @throws Exception
	 */
	@Test(dataProvider="processJobStatusEventsProvider")
	public void processJobStatusEvents(JobStatusType status) 
	throws Exception 
	{
		Job job = createJob(status);
		
		Notification notification = createNotification(job);
		
		JobNotificationEvent event = (JobNotificationEvent) EventFilterFactory.getInstance(new AgaveUUID(job.getUuid()), notification, status.name(), job.getOwner());
		event.setCustomNotificationMessageContextData(job.toJSON());
		
		String resolvedTemplate = event.getEmailBody();
		Assert.assertFalse(resolvedTemplate.contains("${"), "No template variables should be found in the plain text email template body");
		Assert.assertTrue(resolvedTemplate.contains("Execution System: " + job.getSystem()), "Execution system should be resolved in email body.");
		
		resolvedTemplate = event.getHtmlEmailBody();
		Assert.assertFalse(resolvedTemplate.contains("${"), "No template variables should be found in the html email template body");
		Assert.assertTrue(resolvedTemplate.contains("<strong>Execution System:</strong> " + job.getSystem()), "Execution system should be resolved in email body.");
		
		resolvedTemplate = event.getEmailSubject();
		Assert.assertFalse(resolvedTemplate.contains("${"), "No template variables should be found in the email subject");
	}
	
	@DataProvider
	protected Object[][] processJobAssociationEventsProvider() {
		return new Object[][] {
				{ JobStatusType.PENDING },
				{ JobStatusType.RUNNING },
				{ JobStatusType.FAILED },
				{ JobStatusType.STOPPED },
				{ JobStatusType.PAUSED },
				{ JobStatusType.QUEUED },
				{ JobStatusType.FINISHED }
		};
	}
	
	/**
	 * Test that job status events on software resources properly process their notification template
	 * variables .
	 * 
	 * @throws Exception
	 */
	@Test(dataProvider="processJobAssociationEventsProvider")
	public void processJobSoftwareEvents(JobStatusType status) 
	throws Exception 
	{
		Job job = createJob(status);
		
		Notification notification = createJobSoftwareNotification(job, software);
		
		SoftwareNotificationEvent event = (SoftwareNotificationEvent) EventFilterFactory.getInstance(new AgaveUUID(notification.getAssociatedUuid()), notification, notification.getEvent(), job.getOwner());
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode jsonContent = mapper.createObjectNode();
		jsonContent.put("software", mapper.readTree(software.toJSON()));
		jsonContent.put("job", mapper.readTree(job.toJSON()));
		event.setCustomNotificationMessageContextData(jsonContent.toString());
		
		String resolvedTemplate = event.getEmailBody();
		Assert.assertFalse(resolvedTemplate.contains("${"), "No template variables should be found in the plain text email template body");
//		Assert.assertTrue(resolvedTemplate.contains("Execution System: " + job.getSystem()), "Execution system should be resolved in email body.");
		System.out.println(resolvedTemplate);
		
		resolvedTemplate = event.getHtmlEmailBody();
		Assert.assertFalse(resolvedTemplate.contains("${"), "No template variables should be found in the html email template body");
//		Assert.assertTrue(resolvedTemplate.contains("<strong>Execution System:</strong> " + job.getSystem()), "Execution system should be resolved in email body.");
		System.out.println(resolvedTemplate);
		
		resolvedTemplate = event.getEmailSubject();
//		Assert.assertFalse(resolvedTemplate.contains("${"), "No template variables should be found in the email subject");
		System.out.println(resolvedTemplate);
	}
	
	/**
	 * Test that job status events on software resources properly process their notification template
	 * variables .
	 * 
	 * @throws Exception
	 */
	@Test(dataProvider="processJobAssociationEventsProvider")
	public void processJobSystemEvents(JobStatusType status) 
	throws Exception 
	{
		Job job = createJob(status);
		
		Notification notification = createJobSystemNotification(job, software.getExecutionSystem());
		
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode json = mapper.createObjectNode();
		json.put("job", mapper.readTree(job.toJSON()));
		json.put("system", mapper.readTree(software.getExecutionSystem().toJSON()));
		
		SystemNotificationEvent event = (SystemNotificationEvent) EventFilterFactory.getInstance(new AgaveUUID(notification.getAssociatedUuid()), notification, notification.getEvent(), job.getOwner());
		event.setCustomNotificationMessageContextData(json.toString());
		
		String resolvedTemplate = event.getEmailBody();
		Assert.assertFalse(resolvedTemplate.contains("${"), "No template variables should be found in the plain text email template body");
//		Assert.assertTrue(resolvedTemplate.contains("Execution System: " + job.getSystem()), "Execution system should be resolved in email body.");
		System.out.println(resolvedTemplate);
		
		resolvedTemplate = event.getHtmlEmailBody();
		Assert.assertFalse(resolvedTemplate.contains("${"), "No template variables should be found in the html email template body");
//		Assert.assertTrue(resolvedTemplate.contains("<strong>Execution System:</strong> " + job.getSystem()), "Execution system should be resolved in email body.");
		System.out.println(resolvedTemplate);
		
		resolvedTemplate = event.getEmailSubject();
		Assert.assertFalse(resolvedTemplate.contains("${"), "No template variables should be found in the email subject");
		System.out.println(resolvedTemplate);
	}
	
}
