package org.iplantc.service.notification.queue;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.Date;

import org.iplantc.service.common.queue.GenericSchedulingPlugin;
import org.iplantc.service.notification.Settings;
import org.joda.time.DateTime;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.testng.log4testng.Logger;

/**
 * Class to initialize worker tasks to process notification events.
 * This class is called by a servlet filter on startup so it will 
 * begin running even if no service is called.
 * 
 * @author dooley
 *
 */
public class NewNotificationMessageSchedulingPlugin extends GenericSchedulingPlugin
{
	private static final Logger log = Logger.getLogger(NewNotificationMessageSchedulingPlugin.class);
	/**
	 * 
	 */
	public NewNotificationMessageSchedulingPlugin() {
		super();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void start()
	{
	    // don't even register the jobs if the queue is disabled
        if (org.iplantc.service.common.Settings.isDrainingQueuesEnabled()) {
            log.debug("Skipping " + getPluginGroup().toLowerCase() + " job and triggers due to queue draining.");
            try {
	        	this.scheduler.shutdown(false);
	        } catch (SchedulerException e) {
	        	log.error("Error caught shutting down scheduler during " + getPluginGroup().toLowerCase() + " plugin startup", e);
	        }
	        return;
        }
        
		try {
			log.debug("Initializaing " + getPluginGroup().toLowerCase() + " job...");
			
			int totalTasks = getTaskCount();
			
			for (int i = 0; i < totalTasks; i++)
			{
				log.debug("Setting trigger " + i + " for " + getPluginGroup().toLowerCase() + " job");
				
				Date startTime = new DateTime().plusSeconds(5 + (2*(i+1))).toDate();
				
				JobDetail jobDetail = newJob(getJobClass())
						.withIdentity(getPluginGroup().toLowerCase() + "-job" + i, getPluginGroup())
						.build();
				
				Trigger trigger = newTrigger()
					    .withIdentity("trigger" + i, getPluginGroup())
					    .startAt(startTime)
					    .withSchedule(simpleSchedule()
					    		.withIntervalInMilliseconds(500)
					    		.withMisfireHandlingInstructionNextWithRemainingCount()
					    		.repeatForever())
					    .withPriority(5)
					    .forJob(jobDetail)
					    .build();
				
				scheduler.scheduleJob(jobDetail, trigger);
			}
			
			log.debug("Finished initializing " + getPluginGroup().toLowerCase() + " job with " + 
					getTaskCount() + " triggers.");
			
		} catch (SchedulerException e) {
			log.error("Error initializing " + getPluginGroup().toLowerCase() + " job.", e);
		} 
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	protected Class getJobClass()
	{
		return NewNotificationQueueProcessor.class;
	}

	@Override
	protected String getPluginGroup()
	{
		return "NewNotificationMessage";
	}

	@Override
	protected int getTaskCount()
	{
        try {
            getClass().getClassLoader().loadClass("org.iplantc.service.notification.Settings");
            return Settings.MAX_NOTIFICATION_TASKS;
        } catch (ClassNotFoundException e) {
            return 0;
        }
	}
}
