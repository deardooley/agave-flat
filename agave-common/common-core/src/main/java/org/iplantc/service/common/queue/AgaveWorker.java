/**
 * 
 */
package org.iplantc.service.common.queue;

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.discovery.ServiceCapability;
import org.iplantc.service.common.discovery.ServiceCapabilityConfiguration;
import org.iplantc.service.common.exceptions.TaskSchedulerException;
import org.iplantc.service.common.schedulers.AgaveTaskScheduler;
import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;

/**
 * Abstract class for all wrapper implementations. Provides loading of s
 * @author dooley
 *
 */
public abstract class AgaveWorker implements InterruptableJob {

    private static final Logger log = Logger.getLogger(AgaveWorker.class);
    
    private JobExecutionContext context;
    private String taskId; 
    
    private Set<ServiceCapability> capabilities;
    
    public AgaveWorker() {
        capabilities = ServiceCapabilityConfiguration.getInstance().getLocalCapabilities();
    }

    /* (non-Javadoc)
     * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException
    {
        // set the dynamically created context on this instance.
        this.setContext(context);
        
        // if this instance has been set to drain, ignore any further work
        // until the block is removed.
        if (org.iplantc.service.common.Settings.isDrainingQueuesEnabled()) {
            log.debug("Queues are currently set to drain. Skipping job processing check...");
            return;
        }
        
        try 
        {
            AgaveTaskScheduler taskScheduler = getAgaveTaskScheduler();
            String taskId = taskScheduler.getNextTaskId(capabilities);
            if (StringUtils.isNotEmpty(taskId)) {
                doExecute(taskId);
            } else {
                log.error("No task found matching worker task " + taskId + 
                        " with capabilities [" + StringUtils.join(capabilities, ',') + "]. "
                        + "Abandoning this task without further action.");
            }
        }
        catch (TaskSchedulerException e) {
            log.info(e.getMessage() + ". Task will be retried " + context.getRefireCount() + " more times.");    
            JobExecutionException e2 =     
                new JobExecutionException(e);    
            // this job will refire immediately    
            e2.refireImmediately();    
            throw e2;   
        }
        catch (Throwable e) {    
            log.error("Fatal error processing job " + taskId + ". Job will not be retried.", e);    
            JobExecutionException e2 =     
                new JobExecutionException(e);
            // Quartz will automatically unschedule    
            // all triggers associated with this job    
            // so that it does not run again    
            e2.setUnscheduleAllTriggers(false);    
            throw e2;    
        }
    }

    /* (non-Javadoc)
     * @see org.quartz.InterruptableJob#interrupt()
     */
    @Override
    public void interrupt() throws UnableToInterruptJobException {}
    
    /**
     * Contains the actual worker logic in the implementing subclass.
     */
    public abstract void doExecute(String resourceId) throws Exception;

    /**
     * Returns an instance of an {@link AgaveTaskScheduler} that can be used
     * to obtain the next available task for this worker. Different {@link LongRunningQuartzJob}s 
     * can utilize different {@link AgaveTaskScheduler} instances depending on
     * the scheduling algorithm they wish to implement. For example, data 
     * transfer could use a FIFO algorithm while job submission may use a 
     * FairShare algorithm.
     * 
     * @param {@Link List} of {@link ServiceCapabilityImpl} representing the capabilities of this job
     * @return {@link AgaveTaskScheduler} appropriate for this type of long-running task.
     */
    public abstract AgaveTaskScheduler getAgaveTaskScheduler();
    
    /**
     * @return the context
     */
    public JobExecutionContext getContext()
    {
        return context;
    }

    /**
     * @param context the context to set
     */
    public void setContext(JobExecutionContext context)
    {
        this.context = context;
    }

}
