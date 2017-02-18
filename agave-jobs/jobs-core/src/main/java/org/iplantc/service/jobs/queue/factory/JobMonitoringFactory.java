package org.iplantc.service.jobs.queue.factory;


import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.log4j.Logger;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.queue.MonitoringWatch;
import org.iplantc.service.jobs.queue.WorkerWatch;
import org.quartz.spi.JobFactory;

/**
 * Provides an implementation of the {@link JobFactory} interface
 * to maintain a priority queue of monitoring jobs across the jvm.
 * 
 * @author dooley
 *
 * @param <T>
 */
public class JobMonitoringFactory extends AbstractJobProducerFactory {
    
    private final Logger log = Logger.getLogger(JobMonitoringFactory.class);
    
    public JobMonitoringFactory() {}

    @Override
	protected ConcurrentLinkedDeque<String> getTaskQueue() {
		return monitoringJobTaskQueue;
	}

	@Override
	protected WorkerWatch getJobInstance() {
		return new MonitoringWatch();
	}
	
	@Override
	protected int getMaxTasks() {
		return Settings.MAX_MONITORING_TASKS;
	}
}