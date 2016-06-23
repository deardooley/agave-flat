package org.iplantc.service.monitor.queue;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.Date;

import org.iplantc.service.common.queue.GenericSchedulingPlugin;
import org.iplantc.service.monitor.Settings;
import org.joda.time.DateTime;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.testng.log4testng.Logger;

/**
 * Class to initialize worker tasks to process monitor events.
 * This class is called by a servlet filter on startup so it will 
 * begin running even if no service is called.
 * 
 * @author dooley
 *
 */
public class MonitorQueueSchedulingPlugin extends GenericSchedulingPlugin 
{
	private static final Logger log = Logger.getLogger(MonitorQueueSchedulingPlugin.class);
	
	/**
	 * 
	 */
	public MonitorQueueSchedulingPlugin() {
		super();
	}
	
	/**
     * <p>
     * Called during creation of the <code>Scheduler</code> in order to give
     * the <code>SchedulerPlugin</code> a chance to initialize.
     * </p>
     * 
     * @throws org.quartz.SchedulerConfigException
     *           if there is an error initializing.
     */
	@Override
	public void initialize(String name, Scheduler scheduler)
	throws SchedulerException
	{
		this.scheduler = scheduler;
		 // turn off the scheduler so we can release the threads if the queues are disabled
		 // in this service or worker.
		 if (scheduler != null && org.iplantc.service.common.Settings.isDrainingQueuesEnabled()) {
		     scheduler.shutdown(false);
		 }
	}
	
	@Override
	public void start()
	{
	 // don't even register the jobs if the queue is disabled
        if (org.iplantc.service.common.Settings.isDrainingQueuesEnabled()) {
            log.debug("Skipping " + getPluginGroup().toLowerCase() + " job and triggers due to queue draining.");
            try {
	        	this.scheduler.shutdown(false);
	        } catch (SchedulerException e) {
	        	log.error("Error caught shutting down scheduler during plugin startup", e);
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
				
				JobDetail jobDetail = newJob(MonitorQueueListener.class)
						.withIdentity(getPluginGroup().toLowerCase() + "-job" + i, getPluginGroup())
						.build();
				
				Trigger trigger = newTrigger()
					    .withIdentity("trigger" + i, getPluginGroup())
					    .startAt(startTime)
					    .withSchedule(simpleSchedule()
					    		.withIntervalInSeconds(1)
//					    		.withMisfireHandlingInstructionNowWithExistingCount()
					    		.repeatForever())
					    .withPriority(i+5)
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

	@Override
	protected Class<?> getJobClass()
	{
		return MonitorQueueListener.class;
	}

	@Override
	protected String getPluginGroup()
	{
		return "MonitorQueue";
	}

	@Override
	protected int getTaskCount()
	{
        try {
            getClass().getClassLoader().loadClass("org.iplantc.service.monitor.Settings");
            return Settings.MAX_MONITOR_TASKS;
        } catch (ClassNotFoundException e) {
            return 0;
        }
	}
}
