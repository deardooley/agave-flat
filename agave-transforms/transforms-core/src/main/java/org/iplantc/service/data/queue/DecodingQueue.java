/**
 * 
 */
package org.iplantc.service.data.queue;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import org.iplantc.service.data.Settings;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Background process to process decoding tasks
 * @author dooley
 * @deprecated <code>DecodingSchedulingPlugin</code> is used instead as it initializes on startup rather than invocation.
 */
public class DecodingQueue 
{
	public static void init() throws SchedulerException
	{
		Scheduler sched = StdSchedulerFactory.getDefaultScheduler();
		
		sched.start();

		// start a block of worker processes to pull pre-staged file references
		// from the db and apply the appropriate transforms to them.
		for (int i = 0; i < Settings.MAX_TRANSFORM_TASKS; i++)
		{
			JobDetail jobDetail = newJob(DecodingJob.class)
					.withIdentity("job" + i, "Decoding")
					.build();
			
			Trigger trigger = newTrigger()
				    .withIdentity("trigger" + i, "Decoding")
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
