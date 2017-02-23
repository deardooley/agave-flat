package org.iplantc.service.common.queue;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.Set;

import org.apache.log4j.Logger;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.discovery.ServiceCapability;
import org.iplantc.service.common.discovery.ServiceCapabilityConfiguration;
import org.iplantc.service.common.messaging.MessageQueueListener;
import org.joda.time.DateTime;
import org.quartz.InterruptableJob;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.spi.SchedulerPlugin;

/**
 * @author dooley
 *
 */
public abstract class GenericSchedulingPlugin implements SchedulerPlugin 
{
	@SuppressWarnings("unused")
	private String name;
    protected Scheduler scheduler;
    private final Logger log = Logger.getLogger(getClass());
    
	/**
	 * 
	 */
	public GenericSchedulingPlugin() {}

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
		 this.name = name;
		 this.scheduler = scheduler; 
	}

	@SuppressWarnings("unchecked")
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
                
	            JobDetail customJobDetail = newJob(getJobClass())
                        .withIdentity(getPluginGroup().toLowerCase() + "-job-"+i, getPluginGroup())
                        .requestRecovery(true)
                        .storeDurably()
                        .build();
	            
	            Trigger trigger = newTrigger()
	                    .withIdentity(getPluginGroup().toLowerCase() + "-trigger"+i, getPluginGroup())
	                    .startAt(new DateTime().plusSeconds(5+i).toDate())
	                    .withSchedule(simpleSchedule()
	                            .withMisfireHandlingInstructionNextWithExistingCount()
	                            .withIntervalInSeconds(5)
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

    /**
     * <p>
     * Called in order to inform the <code>SchedulerPlugin</code> that it
     * should free up all of it's resources because the scheduler is shutting
     * down.
     * </p>
     */
	@Override
	public void shutdown()
	{
		// nothing to do here. scheduler is already shutting down.
		try
		{
			log.debug("Shutting down " + getPluginGroup().toLowerCase() + " queue...");
			
			for (JobExecutionContext jobContext : scheduler.getCurrentlyExecutingJobs())
			{
				Job job = jobContext.getJobInstance();
				if (job instanceof MessageQueueListener) {
					((MessageQueueListener)job).stop();
				}
				else if (job instanceof InterruptableJob) {
					((InterruptableJob)job).interrupt();
				}
			}
		}
		catch (NullPointerException e) {
		    // happens when the scheduler wasn't initialized properly. Ususally due to us
		    // disabling it completely.
		}
		catch (SchedulerException e)
		{
			log.error("Failed to shut down queue properly.", e);
		}
	}
    
	@SuppressWarnings("rawtypes")
	protected abstract Class getJobClass();
	
	protected abstract String getPluginGroup();
	
	protected abstract int getTaskCount();
	
	protected Set<ServiceCapability> getCapabilities()
    {
        return ServiceCapabilityConfiguration.getInstance().getLocalCapabilities();
    }

}
