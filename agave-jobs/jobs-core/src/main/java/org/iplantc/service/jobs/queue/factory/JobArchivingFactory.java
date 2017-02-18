package org.iplantc.service.jobs.queue.factory;


import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.log4j.Logger;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.queue.ArchiveWatch;
import org.iplantc.service.jobs.queue.WorkerWatch;
import org.quartz.spi.JobFactory;

/**
 * Provides an implementation of the {@link JobFactory} interface
 * to maintain a priority queue of archive jobs across the jvm.
 * 
 * @author dooley
 *
 * @param <T>
 */
public class JobArchivingFactory extends AbstractJobProducerFactory {
    
    private final Logger log = Logger.getLogger(JobArchivingFactory.class);
    
    public JobArchivingFactory() {}

    @Override
	protected ConcurrentLinkedDeque<String> getTaskQueue() {
		return archivingJobTaskQueue;
	}

	@Override
	protected WorkerWatch getJobInstance() {
		return new ArchiveWatch();
	}
	
	@Override
	protected int getMaxTasks() {
		return Settings.MAX_ARCHIVE_TASKS;
	}
}