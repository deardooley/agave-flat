package org.iplantc.service.jobs.managers;

import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.QuotaViolationException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.model.BatchQueue;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;

public class JobQuotaCheck implements QuotaCheck {
	
	private Job job;
	
	public JobQuotaCheck(Job job)
	{
		this.job = job;
	}
	
	@Override
	public void check() throws QuotaViolationException, JobException, SystemUnavailableException
	{
		ExecutionSystem system = (ExecutionSystem) new SystemDao().findBySystemId(job.getSystem());
		
		if (system == null || !system.isAvailable() || !system.getStatus().equals(SystemStatusType.UP)) {
			throw new SystemUnavailableException("Job execution system " + job.getSystem() + " is not available.");
		}
		
		// TODO: add check for batch queue on all hpc systems. this should always be false for them.
        BatchQueue jobQueue = system.getQueue(job.getBatchQueue());
        if ( jobQueue == null && system.getDefaultQueue() == null) {
            jobQueue = new BatchQueue("default", new Long(10), new Double(4));
        } 
        // todo end hack for queueless condor
//        if ( jobQueue == null ) {
//        	if (system.getDefaultQueue() == null && system.getScheduler() == SchedulerType.CONDOR) {
//        		jobQueue = new BatchQueue("default", new Long(10), new Double(4));
//        	} else {
//        		
//        	}
//        } 
        
		// verify the system quota first
        if (jobQueue.getMaxJobs() == -1) {
        	return;
        }
        // verify the system is not at capacity
        else if (system.getMaxSystemJobs() > 0 && JobDao.countActiveJobsOnSystem(job.getSystem()) >= system.getMaxSystemJobs()) 
		{
			throw new QuotaViolationException(job.getSystem() + " is currently at capacity for new jobs.");
		}
        // verify the system queue is not at capacity
        else if (jobQueue.getMaxJobs() > 0 && JobDao.countActiveJobsOnSystemQueue(job.getSystem(), job.getBatchQueue()) >= jobQueue.getMaxJobs())
        {
        	throw new QuotaViolationException("System " + system.getSystemId() + " is currently at maximum capacity for "
        			+ "concurrent active jobs.");
        }
        // verify the user is not at system capacity
        else if (system.getMaxSystemJobsPerUser() > 0 && JobDao.countActiveUserJobsOnSystem(job.getOwner(), job.getSystem()) >= system.getMaxSystemJobsPerUser())
        {
        	throw new QuotaViolationException("User " + job.getOwner() + " has reached their quota for "
        			+ "concurrent active jobs on " + system.getSystemId());
        }
        // verify the user is not at queue capacity
        else if (jobQueue.getMaxUserJobs() > 0 && JobDao.countActiveUserJobsOnSystemQueue(job.getOwner(), job.getSystem(), job.getBatchQueue()) >= jobQueue.getMaxUserJobs())
        {
        	throw new QuotaViolationException("User " + job.getOwner() + " has reached their quota for "
        			+ "concurrent active jobs on the " + jobQueue.getName() + " queue of " + system.getSystemId());
        }
	}
}
