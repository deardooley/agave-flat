/**
 * 
 */
package org.iplantc.service.io.notifications;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.clients.RequestBin;
import org.iplantc.service.common.exceptions.MessagingException;
import org.iplantc.service.io.BaseTestCase;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.exceptions.LogicalFileException;
import org.iplantc.service.io.manager.LogicalFileManager;
import org.iplantc.service.io.model.FileEvent;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.enumerations.FileEventType;
import org.iplantc.service.io.model.enumerations.StagingTaskStatus;
import org.iplantc.service.io.model.enumerations.TransformTaskStatus;
import org.iplantc.service.notification.dao.NotificationDao;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.notification.model.enumerations.NotificationStatusType;
import org.iplantc.service.notification.providers.email.enumeration.EmailProviderType;
import org.iplantc.service.notification.queue.NewNotificationQueueProcessor;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.KeyMatcher;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surftools.BeanstalkClientImpl.ClientImpl;

/**
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"notifications","files"})
public class LogicalFileNotificationTest extends BaseTestCase
{
	private static final Logger log = Logger.getLogger(LogicalFileNotificationTest.class);
	
	private static String TEST_NOTIFICATION_EMAIL = "foo@example.com";
	private String TEST_NOTIFICATION_URL;
	private org.iplantc.service.io.model.JSONTestDataUtil util;
	private NotificationDao notificationDao = new NotificationDao();
	private ObjectMapper mapper = new ObjectMapper();
	private Scheduler sched;
	private SimpleTrigger trigger;
	private RequestBin requestBin;
	
	final AtomicBoolean notificationProcessed = new AtomicBoolean(false);
    
	@BeforeClass
	@Override
	protected void beforeClass() throws Exception {
		super.beforeClass();
		
		org.iplantc.service.notification.Settings.EMAIL_PROVIDER = EmailProviderType.LOG;
		
		clearLogicalFiles();
		initSystems();
		
		drainQueue();
		TEST_NOTIFICATION_URL = createRequestBin();
		
		startTestNotificationScheduler();
	}

	@AfterClass
	@Override
    protected void afterClass() throws Exception {
		clearLogicalFiles();
		clearSystems();
		
		drainQueue();
		sched.clear();
		sched.shutdown();
	}
	
	@BeforeMethod
	protected void beforeMethod(Method m) throws Exception {
	    clearLogicalFiles();
	    
		sched.clear();
		startNotificationQueue(m.getName());
	}
	
	@AfterMethod
	protected void afterMethod(Method m) throws Exception {
	    drainQueue();
	}
	
	private void startNotificationQueue(String name) throws SchedulerException {
		
		JobDetail jobDetail = newJob(NewNotificationQueueProcessor.class)
			.withIdentity("test-" + name)
			.build();
		
		trigger = (SimpleTrigger)newTrigger()
				.withIdentity("trigger-LogicalFileNotificationTest")
				.startNow()
				.withSchedule(simpleSchedule()
		            .withIntervalInMilliseconds(500)
		            .repeatForever()
		            .withMisfireHandlingInstructionNextWithExistingCount())
				.build();
		
		sched.scheduleJob(jobDetail, trigger);
		
		sched.getListenerManager().addJobListener(
            new JobListener() {

                @Override
                public String getName() {
                    return getClass().getSimpleName() + " Unit Test Listener";
                }

                @Override
                public void jobToBeExecuted(JobExecutionContext context) {
                    log.debug("working on a new notification event");                        
                }

                @Override
                public void jobExecutionVetoed(JobExecutionContext context) {
                    // no idea here
                }

                @Override
                public void jobWasExecuted(JobExecutionContext context, JobExecutionException e) {
                    notificationProcessed.set(true);
                }
                
            }, KeyMatcher.keyEquals(jobDetail.getKey())
        );
	}
	
	protected String createRequestBin() throws IOException, RemoteDataException {
	    requestBin = RequestBin.getInstance();
		return requestBin.toString() + "?status=${EVENT}&logicalFileid=${UUID}";
	}

	/**
	 * Flushes the messaging tube of any and all existing logicalFiles.
	 * @param queueName
	 */
	protected void drainQueue() 
	{
		ClientImpl client = null;
	
		try {
			// drain the message queue
			client = new ClientImpl(Settings.MESSAGING_SERVICE_HOST,
					Settings.MESSAGING_SERVICE_PORT);
			client.watch(Settings.NOTIFICATION_QUEUE);
			client.useTube(Settings.NOTIFICATION_QUEUE);
			client.kick(Integer.MAX_VALUE);
			
			com.surftools.BeanstalkClient.Job beanstalkJob = null;
			do {
				try {
					beanstalkJob = client.peekReady();
					if (beanstalkJob != null)
						client.delete(beanstalkJob.getJobId());
				} catch (Throwable e) {
					e.printStackTrace();
				}
			} while (beanstalkJob != null);
			do {
				try {
					beanstalkJob = client.peekBuried();
					if (beanstalkJob != null)
						client.delete(beanstalkJob.getJobId());
				} catch (Throwable e) {
					e.printStackTrace();
				}
			} while (beanstalkJob != null);
			do {
				try {
					beanstalkJob = client.peekDelayed();
					
					if (beanstalkJob != null)
						client.delete(beanstalkJob.getJobId());
				} catch (Throwable e) {
					e.printStackTrace();
				}
			} while (beanstalkJob != null);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		finally {
			try { client.ignore(Settings.NOTIFICATION_QUEUE); } catch (Throwable e) {}
			try { client.close(); } catch (Throwable e) {}
			client = null;
		}
	}
	
	/**
	 * Counts number of messages in the queue.
	 * 
	 * @param queueName
	 * @return int totoal message count
	 */
	public int getMessageCount(String queueName) throws MessagingException
	{
		ClientImpl client = null;
		
		try {
			// drain the message queue
			client = new ClientImpl(Settings.MESSAGING_SERVICE_HOST,
					Settings.MESSAGING_SERVICE_PORT);
			client.watch(queueName);
			client.useTube(queueName);
			Map<String,String> stats = client.statsTube(queueName);
			String totalLogicalFiles = stats.get("current-logicalFiles-ready");
			if (NumberUtils.isNumber(totalLogicalFiles)) {
				return NumberUtils.toInt(totalLogicalFiles);
			} else {
				throw new MessagingException("Failed to find total logicalFile count for queue " + queueName);
			}
		} catch (MessagingException e) {
			throw e;
		} catch (Throwable e) {
			throw new MessagingException("Failed to read logicalFiles from queue " + queueName, e);
		}
		finally {
			try { client.ignore(Settings.NOTIFICATION_QUEUE); } catch (Throwable e) {}
			try { client.close(); } catch (Throwable e) {}
			client = null;
		}
	}
	
	/**
	 * Creates a bare bones LogicalFile object.
	 * @return LogicalFile with minimal set of attributes.
	 * @throws LogicalFileProcessingException
	 */
	private LogicalFile createLogicalFile() throws LogicalFileException
	{
	    LogicalFile file = null;
		try 
		{
		    StorageSystem storageSystem = new SystemManager().getDefaultStorageSystem();
		    file = new LogicalFile(username, storageSystem, URI.create("agave://" + storageSystem.getSystemId() + "/" + SOURCE_FILENAME), storageSystem.getRemoteDataClient().resolvePath(SOURCE_FILENAME + ".copy"));
	        file.setStatus(StagingTaskStatus.STAGING_QUEUED);
	        file.setOwner(SYSTEM_OWNER);
	        
	        LogicalFileDao.persist(file);
	        
//	        task = new StagingTask(file);
//	        task.setRetryCount(Settings.MAX_STAGING_RETRIES);
//	        QueueTaskDao.persist(task);
//	        
            LogicalFileDao.persist(file);    
		} 
		catch (Exception e) {
			Assert.fail("Failed to create LogicalFile object", e);
		}
		
		return file;
	}
	
	/**
     * Fetches the custom quartz scheduler needed to manage notification processing.
     * @throws SchedulerException
     */
    private void startTestNotificationScheduler() throws SchedulerException {
        StdSchedulerFactory schedulerFactory = new StdSchedulerFactory();
        Properties props = new Properties();
        props.put("org.quartz.scheduler.instanceName", "AgaveNotificationScheduler");
        props.put("org.quartz.threadPool.threadCount", "1");
        props.put("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
        props.put("org.quartz.plugin.shutdownhook.class", "org.quartz.plugins.management.ShutdownHookPlugin");
        props.put("org.quartz.plugin.shutdownhook.cleanShutdown", "true");
        props.put("org.quartz.scheduler.skipUpdateCheck","true");
        schedulerFactory.initialize(props);
        
        sched = schedulerFactory.getScheduler();
        sched.start();
    }
    
    protected void createIndividualNotifications(LogicalFile logicalFile, String notificationUri, boolean persistent) 
    throws NotificationException 
    {
        Set<FileEventType> statuses = new HashSet<FileEventType>();
        statuses.addAll(Arrays.asList(FileEventType.getFileStagingEvents()));
        statuses.addAll(Arrays.asList(FileEventType.getFileTransformEvents()));
        
        for(FileEventType statusEventName: statuses) {
        	LogicalFileManager.addNotification(logicalFile, statusEventName, notificationUri, true, logicalFile.getOwner());
        }
        LogicalFileDao.persist(logicalFile);
    }
    
    protected void createSingleNotification(LogicalFile logicalFile, FileEventType eventType, String url, boolean persistent) 
    throws NotificationException 
    {
        LogicalFileManager.addNotification(logicalFile, eventType, url, persistent, logicalFile.getOwner());
    }

    @DataProvider(name = "logicalFileNotificationStatusProvider")
    protected Object[][] logicalFileNotificationStatusProvider() {
        ArrayList<Object[]> testCases = new ArrayList<Object[]>();
        Set<FileEventType> statuses = new HashSet<FileEventType>();
        statuses.addAll(Arrays.asList(FileEventType.getFileStagingEvents()));
        statuses.addAll(Arrays.asList(FileEventType.getFileTransformEvents()));
       
        for(FileEventType eventType: statuses) { 
            testCases.add(new Object[] { eventType });
        }
//        
//        for(TransformTaskStatus status: TransformTaskStatus.values()) { 
//            if (status != TransformTaskStatus.PREPROCESSING) {
//                testCases.add(new Object[] { status.name() });
//            }
//        }
        
        return testCases.toArray(new Object[][] {});
    }
    
	@Test(dataProvider="logicalFileNotificationStatusProvider")
	public void testLogicalFileIndividualEmailNotificationProcessed(FileEventType testStatus) throws Exception
	{
	    genericLogicalFileSpecificNotificationTest(testStatus, TEST_NOTIFICATION_EMAIL, true);
	}
	
	@Test(dataProvider="logicalFileNotificationStatusProvider", dependsOnMethods={"testLogicalFileIndividualEmailNotificationProcessed"})
    public void testLogicalFileWildcardEmailNotificationProcessed(FileEventType testStatus) throws Exception
    {
	    genericLogicalFileSpecificNotificationTest(testStatus, TEST_NOTIFICATION_EMAIL, true);
    }
	
	@Test(dataProvider="logicalFileNotificationStatusProvider", dependsOnMethods={"testLogicalFileWildcardEmailNotificationProcessed"})
    public void testLogicalFileIndividualWebhookNotificationProcessed(FileEventType testStatus) throws Exception
    {
        genericLogicalFileWildcardNotificationTest(testStatus, TEST_NOTIFICATION_EMAIL, true);
    }
    
    @Test(dataProvider="logicalFileNotificationStatusProvider", dependsOnMethods={"testLogicalFileIndividualWebhookNotificationProcessed"})
    public void testLogicalFileWildcardWebhookNotificationProcessed(FileEventType testStatus) throws Exception
    {
        genericLogicalFileWildcardNotificationTest(testStatus, TEST_NOTIFICATION_EMAIL, true);
    }
	
	/**
	 * Performs the actual notification invocation and check with notifications
	 * individually specified.
	 * 
	 * @param testStatus
	 * @param notificationUri
	 * @param persistent
	 * @throws Exception
	 */
	protected void genericLogicalFileSpecificNotificationTest(FileEventType eventType, String notificationUri, boolean persistent) 
	throws Exception
    {
        LogicalFile logicalFile = createLogicalFile();
		
        createSingleNotification(logicalFile, eventType, notificationUri, persistent);
		
		processNotification(logicalFile, eventType);
    }
	
	/**
     * Performs the actual notification invocation and check with an individual wildcard notification
     * subscribed.
     * 
     * @param testStatus
     * @param notificationUri
     * @param persistent
     * @throws Exception
     */
    protected void genericLogicalFileWildcardNotificationTest(FileEventType testStatus, String notificationUri, boolean persistent) 
    throws Exception
    {
        LogicalFile logicalFile = createLogicalFile();
        
        createIndividualNotifications(logicalFile, notificationUri, persistent);
        
        processNotification(logicalFile, testStatus);
    }
	
	/**
	 * Executes a nofication event processing.
	 * 
	 * @param logicalFile
	 * @param testStatus
	 * @throws Exception
	 */
	private void processNotification(LogicalFile logicalFile, FileEventType eventType)
	throws Exception
    {
		notificationProcessed.set(false);
		
	    logicalFile.setStatus(eventType.name());
	    LogicalFileDao.persist(logicalFile);
	    
	    // update logicalFile status
		logicalFile.addContentEvent(new FileEvent(
				eventType, 
                "Logical file " + logicalFile.getPublicLink() + " recieved " + eventType.name() + " event.", 
                null));
		LogicalFileDao.persist(logicalFile);
        
		// force the queue listener to fire. This should pull the logicalFile message off the queue and notifiy us
		//queueListener.execute(null);
		int i = 0;
		while(!notificationProcessed.get() && i < 5) {
		    Thread.sleep(1000);
		    i++;
		}
		
		List<Notification> notifications = notificationDao.getActiveForAssociatedUuidAndEvent(logicalFile.getUuid(), eventType.name());
		
		Assert.assertFalse(notifications.isEmpty(), "No notifications found for status " + eventType.name());
		
		Assert.assertEquals(notifications.size(), 1, "Wrong number of notifications returned for status " + eventType.name() + " ");
		
		Notification n = notifications.get(0);
		System.out.println(n.getLastUpdated());
//		Assert.assertNotNull(n.getLastSent(), "Message for status " + testStatus + " was attempted.");
		
		Assert.assertTrue(n.getStatus() == NotificationStatusType.COMPLETE, "Message for status " + eventType.name() + " was successfully sent.");
	}

}
