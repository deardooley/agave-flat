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
public class ZombieCleanupTriggerListener implements TriggerListener {

	private static Logger log = Logger.getLogger(ZombieCleanupTriggerListener.class);
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
		log.debug("Zombie cleanup trigger " + trigger.getKey() + " for job " + trigger.getJobKey() + 
				" misfired. Next fire time: " + trigger.getNextFireTime().toString());
		
	}

	/* (non-Javadoc)
	 * @see org.quartz.TriggerListener#triggerComplete(org.quartz.Trigger, org.quartz.JobExecutionContext, org.quartz.Trigger.CompletedExecutionInstruction)
	 */
	@Override
	public void triggerComplete(Trigger trigger, JobExecutionContext context,
			CompletedExecutionInstruction triggerInstructionCode) {
		
		if (context.getResult() == null || StringUtils.isBlank(context.getResult().toString())) {
			log.debug("Zombie cleanup job " + trigger.getJobKey() + " completed running after " + 
					context.getJobRunTime() + " milliseconds with 0 jobs affected");
		}
		else {
			String[] uuids = StringUtils.split(context.getResult().toString(), ",");
			log.debug("Zombie cleanup job " + trigger.getJobKey() + " completed running after " + 
					context.getJobRunTime() + " milliseconds with " + (uuids == null ? 0 : uuids.length)  + " jobs affected");
			log.info("Zombie cleanup job " + trigger.getJobKey() + " rolled back the following jobs: " + context.getResult() );
		}
        
	}

}
