/**
 * 
 */
package org.iplantc.service.jobs.queue;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import org.iplantc.service.jobs.Settings;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Background process to submit jobs to iPlant resources
 * 
 * @author dooley
 * @deprecated <code>JobStagingSchedulingPlugin</code> is used instead as it initializes on startup rather than invocation.
 */
public class StagingQueue {

	public static void init() throws SchedulerException
	{
		Scheduler sched = StdSchedulerFactory.getDefaultScheduler();
		
		sched.start();

		// start a block of worker processes to pull pre-staged file references
		// from the db and apply the appropriate transforms to them.
		for (int i = 0; i < Settings.MAX_STAGING_TASKS; i++)
		{
			JobDetail jobDetail = newJob(StagingWatch.class)
					.withIdentity("job" + i, "Staging")
					.build();
			
			Trigger trigger = newTrigger()
				    .withIdentity("trigger" + i, "Staging")
				    .startNow()
				    .withSchedule(simpleSchedule()
				            .withIntervalInSeconds(10)
				            .repeatForever())
				    .build();
			
			sched.scheduleJob(jobDetail, trigger);
			
			try { Thread.sleep(3000); } catch (InterruptedException e) {}
		}
	}

}
