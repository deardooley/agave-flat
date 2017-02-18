package org.iplantc.service.jobs.queue.factory;


import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.log4j.Logger;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.queue.SubmissionWatch;
import org.iplantc.service.jobs.queue.WorkerWatch;
import org.quartz.spi.JobFactory;

/**
 * Provides an implementation of the {@link JobFactory} interface
 * to maintain a priority queue of submission jobs across the jvm.
 * 
 * @author dooley
 *
 * @param <T>
 */
public class JobSubmissionFactory extends AbstractJobProducerFactory {
    
    private final Logger log = Logger.getLogger(JobSubmissionFactory.class);
    
    public JobSubmissionFactory() {}

    @Override
	protected ConcurrentLinkedDeque<String> getTaskQueue() {
		return submissionJobTaskQueue;
	}

	@Override
	protected WorkerWatch getJobInstance() {
		return new SubmissionWatch();
	}

	@Override
	protected int getMaxTasks() {
		return Settings.MAX_SUBMISSION_TASKS;
	}
}