/**
 * 
 */
package org.iplantc.service.jobs.queue;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import org.apache.log4j.Logger;
import org.iplantc.service.common.queue.GenericSchedulingPlugin;
import org.iplantc.service.jobs.Settings;
import org.joda.time.DateTime;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

/**
 * Class to initialize worker tasks to clean up zombie
 * jobs across the platform. This class is called by a servlet filter
 * on startup so it will begin running even if no service
 * is called.
 * 
 * @author dooley
 *
 */
public class ZombieJobSchedulingPlugin extends GenericSchedulingPlugin 
{
	private static final Logger log = Logger.getLogger(ZombieJobSchedulingPlugin.class);
	
	/**
	 * 
	 */
	public ZombieJobSchedulingPlugin() {
		super();
	}
	
	@Override
	public void start()
	{
	    // don't even register the jobs if the queue is disabled
	    if (Settings.isDrainingQueuesEnabled()) {
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
			
			// start a block of worker processes to pull pre-staged file references
	        // from the db and apply the appropriate transforms to them.
	        for (int i = 0; i < totalTasks; i++)
	        {
	            log.debug("Setting job and trigger " + i + " for " + getPluginGroup().toLowerCase() + " job group");
                
	            JobDetail customJobDetail = newJob(ZombieJobWatch.class)
                        .withIdentity(getPluginGroup().toLowerCase() + "-job-"+i, getPluginGroup())
                        .requestRecovery(true)
                        .storeDurably()
                        .build();
	            
	            log.debug("Zombie cleanup task will run every 5 minutes.");
	            
	            Trigger trigger = newTrigger()
	                    .withIdentity(getPluginGroup().toLowerCase() + "-trigger"+i, getPluginGroup())
	                    .startAt(new DateTime().plusSeconds(5+i).toDate())
	                    .withSchedule(simpleSchedule()
	                            .withMisfireHandlingInstructionNextWithExistingCount()
	                            .withIntervalInMinutes(5)
	                            .repeatForever())
	                    .forJob(customJobDetail)
	                    .withPriority(5)
	                    .build();
	            
	            scheduler.scheduleJob(customJobDetail, trigger);
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
		return ZombieJobWatch.class;
	}

	@Override
	protected String getPluginGroup()
	{
		return "ZombieCleanup";
	}

	@Override
	protected int getTaskCount()
	{
        try {
            getClass().getClassLoader().loadClass("org.iplantc.service.jobs.Settings");
            return Settings.ENABLE_ZOMBIE_CLEANUP ? 1 : 0;
        } catch (ClassNotFoundException e) {
            return 0;
        }
	}

	

}
