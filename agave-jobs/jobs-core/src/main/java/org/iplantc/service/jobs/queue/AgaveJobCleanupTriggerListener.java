/**
 * 
 */
package org.iplantc.service.jobs.queue;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.TriggerListener;

/**
 * @author dooley
 *
 */
public class AgaveJobCleanupTriggerListener implements TriggerListener {

	private static Logger log = Logger.getLogger(AgaveJobCleanupTriggerListener.class);
	/* (non-Javadoc)
	 * @see org.quartz.TriggerListener#getName()
	 */
	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	/* (non-Javadoc)
	 * @see org.quartz.TriggerListener#triggerFired(org.quartz.Trigger, org.quartz.JobExecutionContext)
	 */
	@Override
	public void triggerFired(Trigger trigger, JobExecutionContext context) {
//		log.debug("Quartz trigger " + trigger.getKey() + " for job " + trigger.getJobKey() + " fired.");
	}

	/* (non-Javadoc)
	 * @see org.quartz.TriggerListener#vetoJobExecution(org.quartz.Trigger, org.quartz.JobExecutionContext)
	 */
	@Override
	public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
		// let the job run. No reason to stop it now
		return false;
	}

	/* (non-Javadoc)
	 * @see org.quartz.TriggerListener#triggerMisfired(org.quartz.Trigger)
	 */
	@Override
	public void triggerMisfired(Trigger trigger) {
		log.debug("Quartz trigger " + trigger.getKey() + " for job " + trigger.getJobKey() + 
				" misfired. Next fire time: " + trigger.getNextFireTime().toString());
		
	}

	/* (non-Javadoc)
	 * @see org.quartz.TriggerListener#triggerComplete(org.quartz.Trigger, org.quartz.JobExecutionContext, org.quartz.Trigger.CompletedExecutionInstruction)
	 */
	@Override
	public void triggerComplete(Trigger trigger, JobExecutionContext context,
			CompletedExecutionInstruction triggerInstructionCode) {
		
		try {
			if (context.getMergedJobDataMap().containsKey("uuid") ) {
				log.debug("Quartz job " + trigger.getJobKey() + " completed running. " + context.getScheduler().getMetaData().getSummary());
				
				log.debug("Attempting to delete quartz job " + trigger.getJobKey() + " for agave uuid: " + context.getMergedJobDataMap().getString("uuid"));
				
				if (context.getScheduler().checkExists(trigger.getJobKey())) {
					log.debug("Deleting quartz job " + trigger.getJobKey() + " for agave uuid: " + context.getMergedJobDataMap().getString("uuid"));
					context.getScheduler().deleteJob(trigger.getJobKey());
					log.debug("Successfully deleted quartz job " + trigger.getJobKey() + " for agave uuid: " + context.getMergedJobDataMap().getString("uuid"));
					
					if (context.getScheduler().getTrigger(trigger.getKey()) == null) { 
						log.debug("Quartz trigger " + trigger.getKey() + " was removed");
					}
					else {
						log.debug("Quartz trigger " + trigger.getKey() + " was not removed. State is " + context.getScheduler().getTriggerState(trigger.getKey()).name());
					}
					
					log.debug("Successfully deleted quartz job " + trigger.getJobKey() + " for agave uuid: " + context.getMergedJobDataMap().getString("uuid"));
					
				} else {
					log.debug("No quartz job " + trigger.getKey() + " found for agave uuid: " + context.getMergedJobDataMap().getString("uuid"));
				}
				
				log.debug("Completed cleaning up quartz job " + trigger.getJobKey() + " for agave job " + context.getMergedJobDataMap().getString("uuid"));
			}
			else {
//				log.debug("Completed quartz job " + trigger.getJobKey() + " for job " + context.getMergedJobDataMap().getString("uuid"));
			}
			
//			// clean up trigger
//			log.debug("Attempting to delete quartz job for agave uuid: " + context.getMergedJobDataMap().getString("uuid"));
//			
//			log.debug("Job completed running. " + sched.getMetaData().getSummary());
//			if (sched.checkExists(trigger.getKey())) {
//				log.debug("Deleting quartz trigger for agave uuid: " + trigger.getKey().getName());
//				sched.getJobDetail(trigger.getJobKey());
//				
//				log.debug("Successfully deleted quartz trigger for agave uuid: " + trigger.getKey().getName());
//			} else {
//				log.debug("No quartz job found for agave uuid: " + trigger.getKey().getName());
//			}
//			
//			log.debug("Job completed running. " + sched.getMetaData().getSummary());
			
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			log.error("Failed to remove completed quartz job from scheduler.", e);
		}
        
	}

}
