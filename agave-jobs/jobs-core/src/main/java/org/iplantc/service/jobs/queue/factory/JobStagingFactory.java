package org.iplantc.service.jobs.queue.factory;


import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.log4j.Logger;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.queue.StagingWatch;
import org.iplantc.service.jobs.queue.WorkerWatch;
import org.quartz.spi.JobFactory;

/**
 * Provides an implementation of the {@link JobFactory} interface
 * to maintain a priority queue of staging jobs across the jvm.
 * 
 * @author dooley
 *
 * @param <T>
 */
public class JobStagingFactory extends AbstractJobProducerFactory {
    
    private final Logger log = Logger.getLogger(JobStagingFactory.class);
    
    public JobStagingFactory() {}

    @Override
	protected ConcurrentLinkedDeque<String> getTaskQueue() {
		return stagingJobTaskQueue;
	}

	@Override
	protected WorkerWatch getJobInstance() {
		return new StagingWatch();
	}
	
	@Override
	protected int getMaxTasks() {
		return Settings.MAX_STAGING_TASKS;
	}
}