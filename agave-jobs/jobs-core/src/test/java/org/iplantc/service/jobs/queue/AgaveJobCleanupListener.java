/**
 * 
 */
package org.iplantc.service.jobs.queue;

import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.SchedulerException;

/**
 * @author dooley
 *
 */
public class AgaveJobCleanupListener implements JobListener {

	private static Logger log = Logger.getLogger(AgaveJobCleanupListener.class);
	
	/* (non-Javadoc)
	 * @see org.quartz.JobListener#getName()
	 */
	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	/* (non-Javadoc)
	 * @see org.quartz.JobListener#jobToBeExecuted(org.quartz.JobExecutionContext)
	 */
	@Override
	public void jobToBeExecuted(JobExecutionContext context) {
		// ignore this bit. we only care about cleaning up the job.
		log.debug("About to execution " + context.getJobDetail().getKey().toString());
	}

	/* (non-Javadoc)
	 * @see org.quartz.JobListener#jobExecutionVetoed(org.quartz.JobExecutionContext)
	 */
	@Override
	public void jobExecutionVetoed(JobExecutionContext context) {
		// ignore this bit. we only care about cleaning up the job.
		log.debug("Just vetoed execution of " + context.getJobDetail().getKey().toString());
	}

	/* (non-Javadoc)
	 * @see org.quartz.JobListener#jobWasExecuted(org.quartz.JobExecutionContext, org.quartz.JobExecutionException)
	 */
	@Override
	public void jobWasExecuted(JobExecutionContext context,
			JobExecutionException jobException) {
		
		try {
			log.debug("Quartz job completed running. " + context.getScheduler().getMetaData().getSummary());
			
			
			log.debug("Attempting to delete quartz job for agave uuid: " + context.getMergedJobDataMap().getString("uuid"));
			
			if (context.getScheduler().checkExists(context.getJobDetail().getKey())) {
				log.debug("Deleting quartz job for agave uuid: " + context.getMergedJobDataMap().getString("uuid"));
				context.getScheduler().deleteJob(context.getJobDetail().getKey());
				log.debug("Successfully deleted quartz job for agave uuid: " + context.getMergedJobDataMap().getString("uuid"));
			} else {
				log.debug("No quartz job found for agave uuid: " + context.getMergedJobDataMap().getString("uuid"));
			}
			
			log.debug("Completed Quartz job and trigger deleted. " + context.getScheduler().getMetaData().getSummary());
			
		
//		Scheduler sched;
//		try {
//			sched = new StdSchedulerFactory().getScheduler("AgaveConsumerJobScheduler");
//			
//			log.debug("Job completed running. " + sched.getMetaData().getSummary());
//			
//			
//			log.debug("Attempting to delete quartz job for agave uuid: " + context.getMergedJobDataMap().getString("uuid"));
//			
//			if (context.getScheduler().checkExists(context.getJobDetail().getKey())) {
//				log.debug("Deleting quartz job for agave uuid: " + context.getMergedJobDataMap().getString("uuid"));
//				context.getScheduler().deleteJob(context.getJobDetail().getKey());
//				log.debug("Successfully deleted quartz job for agave uuid: " + context.getMergedJobDataMap().getString("uuid"));
//			} else {
//				log.debug("No quartz job found for agave uuid: " + context.getMergedJobDataMap().getString("uuid"));
//			}
//			
//			log.debug("Job completed running. " + sched.getMetaData().getSummary());
//			
//			
//			// clean up trigger
//			log.debug("Attempting to delete quartz job for agave uuid: " + context.getMergedJobDataMap().getString("uuid"));
//			
//			log.debug("Job completed running. " + sched.getMetaData().getSummary());
//			if (context.getScheduler().checkExists(context.getTrigger().getKey())) {
//				log.debug("Deleting quartz trigger for agave uuid: " + trigger.getKey().getName());
//				context.getScheduler().(context.getTrigger().getKey());
//				log.debug("Successfully deleted quartz trigger for agave uuid: " + trigger.getKey().getName());
//			} else {
//				log.debug("No quartz job found for agave uuid: " + trigger.getKey().getName());
//			}
//			
//			log.debug("Job completed running. " + sched.getMetaData().getSummary());
			
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
