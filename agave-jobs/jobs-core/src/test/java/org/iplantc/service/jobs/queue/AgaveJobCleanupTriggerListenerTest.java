package org.iplantc.service.jobs.queue;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.ScheduleBuilder;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SchedulerListener;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.TriggerKey;
import org.quartz.TriggerListener;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.EverythingMatcher;
import org.quartz.impl.matchers.KeyMatcher;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AgaveJobCleanupTriggerListenerTest {
	private static Logger log = Logger
			.getLogger(AgaveJobCleanupTriggerListenerTest.class);
	
	AtomicBoolean jobFinished = new AtomicBoolean(false);
	
	@BeforeMethod
	protected void beforeMethod() {
		jobFinished.set(false);
	}
	
	@Test
	public void triggerCompleteDeletesJob() throws SchedulerException,
			InterruptedException {
		// Verify quartz jobs are deleted after firing.

		Scheduler sched = null;
		try {
			
			SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory(); 
			sched = schedFact.getScheduler(); 
			
//			sched.getListenerManager().addTriggerListener(
//	                new TriggerListener() {
//
//	                    @Override
//	                    public String getName() {
//	                        return "Unit Test Trigger Listener";
//	                    }
//
//						@Override
//						public void triggerFired(Trigger trigger,
//								JobExecutionContext context) {
//							log.debug("working on a new job");  
//						}
//
//						@Override
//						public boolean vetoJobExecution(Trigger trigger,
//								JobExecutionContext context) {
//							return false;
//						}
//
//						@Override
//						public void triggerMisfired(Trigger trigger) {
//							log.debug("ignoring misfire event");  
//						}
//
//						@Override
//						public void triggerComplete(
//								Trigger trigger,
//								JobExecutionContext context,
//								CompletedExecutionInstruction triggerInstructionCode) {
//							jobFinished.set(true);
//						}
//	                    
//	                }, EverythingMatcher.allTriggers()
//	            );
//			
//			sched.getListenerManager().addJobListener(
//					new AgaveJobCleanupListener(), EverythingMatcher.allJobs());
//			
//			
			String jobUuid = "12345";

			JobDetail jobDetail = org.quartz.JobBuilder.newJob(StagingWatch.class)
					.usingJobData("uuid", jobUuid)
					.withIdentity(jobUuid, "demoWorkers")
					.build();

			SimpleTrigger trigger = (SimpleTrigger) newTrigger()
					.withIdentity(jobUuid, "demoWorkers")
					.build();
			
			sched.scheduleJob(jobDetail, trigger);
			sched.start();
			
			Thread.sleep(3000);
			
//			String summary = null;
//			while (sched.getMetaData().getNumberOfJobsExecuted() == 0) {
//				if (!StringUtils.equals(summary,  sched.getMetaData().getSummary())) {
//					System.out.println(sched.getMetaData().getSummary());
//					summary = sched.getMetaData().getSummary();
//				}
//				//System.out.println(sched.getJobDetail(jobDetail.getKey()));
//			};
			
			Assert.assertEquals(sched.getMetaData().getNumberOfJobsExecuted(), 1,
					"Incorrect number of jobs executed.");

			Assert.assertFalse(sched.checkExists(jobDetail.getKey()),
					"Job should be deleted immediately after it fires.");

			Assert.assertFalse(sched.checkExists(trigger.getKey()),
					"Trigger should be deleted immediately after it fires.");
		} finally {
			if (sched != null) {
				try {
					sched.clear();
					sched.shutdown(false);
				} catch (Exception e) {
					log.error(
							"Failed to shtudown and clear scheduler after test.",
							e);
				}
			}
		}

	}

}
