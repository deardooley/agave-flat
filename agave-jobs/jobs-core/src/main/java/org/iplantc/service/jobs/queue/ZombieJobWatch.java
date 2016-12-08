package org.iplantc.service.jobs.queue;

import static org.iplantc.service.jobs.model.enumerations.JobStatusType.ARCHIVING;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.CLEANING_UP;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.PENDING;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.STAGED;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.isFinished;

import java.math.BigInteger;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.UnresolvableObjectException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobDependencyException;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobEvent;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.notification.managers.NotificationManager;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.transfer.dao.TransferTaskDao;
import org.iplantc.service.transfer.model.enumerations.TransferTaskEventType;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * This is a reaper task designed to clean up any {@link Job} 
 * who has an unresponsive transfer for more than 15 minutes 
 * or an intermediate status for more than an hour.  
 * 
 * @author dooley
 */
@DisallowConcurrentExecution
public class ZombieJobWatch implements org.quartz.Job 
{
	private static final Logger	log	= Logger.getLogger(ZombieJobWatch.class);
	
	public ZombieJobWatch() {}
	
	@Override
	public void execute(JobExecutionContext context)
			throws JobExecutionException
	{
		try 
		{
			doExecute();
		}
		catch(JobExecutionException e) {
			throw e;
		}
		catch (Throwable e) 
		{
			log.error("Unexpected error during reaping of zombie jobs", e);
		}
	}
	
	public void doExecute() throws JobExecutionException
	{
		try
		{
			if (org.iplantc.service.common.Settings.isDrainingQueuesEnabled()) {
				log.debug("Queue draining has been enabled. Skipping zombie reaping task." );
				return;
			}
			
			List<BigInteger> zombieJobIds = JobDao.findZombieJobs(TenancyHelper.getDedicatedTenantIdForThisService());
			
			for (BigInteger jobId: zombieJobIds) 
            {
				Job job = JobDao.getById(jobId.longValue());
				
				// this is a new thread and thus has no tenant info loaded. we set it up
				// here so things like app and system lookups will stay local to the 
				// tenant
				TenancyHelper.setCurrentTenantId(job.getTenantId());
				TenancyHelper.setCurrentEndUser(job.getOwner());
				
				rollbackJob(job, job.getOwner()); 
            }
		}
		catch (JobException | JobDependencyException e) {
			log.error(e);
		}
		catch (Throwable e)
		{
			log.error("Failed to resolve one or more zombie tasks. "
					+ "Reaping will resume shortly.", e);
		} 
	}

	/**
	 * Rolls back a {@link Job} status to the last stable status prior to the 
	 * current one. This is akin to release a job back to the queue from which 
	 * it was previously taken. This is helpful in situations where a job is 
	 * stuck due to a failed worker or other abandoned process which left the 
	 * job is a zombie state from which it would not otherwise recover.
	 * 
	 * @param job the job that will be rolled back
	 * @param callingUsername the principal requesting the rollback
	 * @throws JobException if the job cannot be rolled back due to invalid status
	 * @throws JobDependencyException 
	 */
	public Job rollbackJob(Job job, String callingUsername) 
	throws JobException, JobDependencyException 
	{	
		try 
		{	
			JobStatusType rollbackJobStatus = job.getStatus().rollbackState();
			
			RemoteSystem executionSystem = new SystemDao().findBySystemId(job.getSystem());
			
			if (executionSystem == null) 
			{
				if (job.getStatus() == ARCHIVING) 
				{
				    job = JobManager.updateStatus(job, JobStatusType.ARCHIVING_FAILED, 
		    				"Execution system is no longer present.");
				    job = JobManager.updateStatus(job, JobStatusType.FINISHED, 
		    				"Job completed, but failed to archive.");
				}
				else
				{
				    job = JobManager.updateStatus(job, JobStatusType.FAILED, 
							"Job failed. Execution system is no longer present.");
				}
				
				log.debug("Zombie reaper task is setting status of job " + job.getUuid() + 
						" to " + job.getStatus() + " because the execution system " + 
						job.getSystem() + " is no longer present.");
			}
			else if (job.getArchiveSystem() == null) 
			{
				if (job.getStatus() == ARCHIVING) 
				{
				    job = JobManager.updateStatus(job, JobStatusType.ARCHIVING_FAILED, 
		    				"Archive system is no longer present.");
					job = JobManager.updateStatus(job, JobStatusType.FINISHED, 
		    				"Job completed, but failed to archive.");
		    	} 
				else
				{
				    job = JobManager.updateStatus(job, JobStatusType.FAILED, 
							"Job failed. Archive system is no longer present.");
				}
				
				log.debug("Zombie reaper task is setting status of job " + job.getUuid() + 
						" to " + job.getStatus() + " because the archive system "
						+ "is no longer present.");
			}
			
			cancelCurrentTransfers(job, callingUsername);
		    
			if (!isFinished(job.getStatus()))
			{	
				log.debug("Zombie reaper task is rolling back status of job " + job.getUuid() + 
						" from " + job.getStatus() + " to " + rollbackJobStatus);
				
				if (rollbackJobStatus == CLEANING_UP) 
				{
		        	if (executionSystem.isAvailable()) 
		        	{	
		            	// roll back the status so it will be picked back up
		        		if (job.getArchiveSystem().isAvailable())
		            	{
		        		    job = JobManager.updateStatus(job, CLEANING_UP, "Archiving task for this job was "
									+ "found in a zombie state. Job will be rolled back to the previous state and "
									+ "archiving to " + job.getArchiveSystem().getSystemId()  + " will resume.");
		            	}
		        		else
		        		{	
		        		    job = JobManager.updateStatus(job, CLEANING_UP, "Archiving task for this job was "
									+ "found in a zombie state. Job will be rolled back to the previous state and "
		            				+ "archiving will resume when the " + job.getArchiveSystem().getSystemId() 
		            				+ " becomes available.");
		        		}
		        	}
		        	else
		        	{
		        	    job = JobManager.updateStatus(job, JobStatusType.CLEANING_UP, "Archiving task for this job was "
								+ "found in a zombie state. Job will be rolled back to the previous state and "
		        				+ "archiving will resume when " + job.getArchiveSystem().getSystemId() 
		        				+ " the execution system becomes available.");
		        	}
				}
				else if (rollbackJobStatus == STAGED)
				{
					if (executionSystem.isAvailable()) 
		        	{
					    job = JobManager.updateStatus(job, STAGED, "Submission task for this job was "
								+ "found in a zombie state. Job will be rolled back to the previous state and "
								+ "submission to " + job.getSystem() + " will resume.");
		        	}
					else
					{
					    job = JobManager.updateStatus(job, STAGED, "Submission task for this job was "
								+ "found in a zombie state. Job will be rolled back to the previous state and "
		        				+ "submission to " + job.getSystem() + " will resume when the system becomes "
		        				+ "available.");
					}
				}
				else if (rollbackJobStatus == PENDING)
				{
					if (executionSystem.isAvailable()) 
		        	{
					    job = JobManager.updateStatus(job, PENDING, "Input data staging for this job was "
								+ "found in a zombie state. Job will be rolled back to the previous state and "
								+ "input staging to " + job.getSystem() + " will resume.");
		        	}
					else
					{
					    job = JobManager.updateStatus(job, PENDING, "Input data staging for this job was "
								+ "found in a zombie state. Job will be rolled back to the previous state and "
		        				+ "input staging to " + job.getSystem() + " will resume when the system becomes "
		        				+ "available.");
					}
				}
				else 
				{
					if (executionSystem.isAvailable()) 
		        	{
					    job = JobManager.updateStatus(job, rollbackJobStatus, 
		    					StringUtils.capitalize(job.getStatus().name())
		    					+ " for this job was found in a zombie state. Job will be rolled back "
		    					+ "to the previous state and and resume.");
		        	}
					else
					{
						job = JobManager.updateStatus(job, rollbackJobStatus, 
								StringUtils.capitalize(job.getStatus().name())
		    					+ " for this job was found in a zombie state. Job will be rolled back "
								+ "to the previous state and and resume when "
		    					+ job.getSystem() + " becomes available.");
					}
				}	
			}
			else {
				throw new JobDependencyException("Unabled to roll back status of inactive jobs.");
			}
		}
		catch (JobDependencyException e) {
			throw e;
		}
		catch (JobException e) {
			throw e;
		}
		catch (UnresolvableObjectException e) {
			throw new JobException("Just avoided a job archive race condition");
		}
		catch (StaleObjectStateException e) {
			throw new JobException("Just avoided a job archive race condition");
		}
		catch (HibernateException e) {
			throw new JobException("Failed to retrieve job information from db", e);
		}
		catch (Throwable e)
		{
			throw new JobException("Failed to roll back job " + job.getUuid() + " to previous state", e);
		}
		
		return job;
	}
	
	/**
	 * Cancel all transfer tasks for this job prior to rolling
	 * back the status so there won't be new transfers
	 * started before the status is updated.
	 * 
	 * @param job the job for which to cancel transfers
	 * @param callingUsername the principal canceling transfers for the job
	 */
	private void cancelCurrentTransfers(Job job, String callingUsername) 
	{	
		// iterate over all job events
    	for (JobEvent event: job.getEvents()) 
		{
    		// wherever a transfer task is found for an event, cancel it. This will 
    		// issue a single SQL update query to set {@link TransferTask#status} to 
    		// {@link TransferStatusType#CANCELLED}
			if (event.getTransferTask() != null) 
			{
				try 
				{ 
					log.debug("Zombie reaper task is cancelling transfer task " 
							+ event.getTransferTask().getUuid() + " for job " + job.getUuid());
        			
					TransferTaskDao.cancelAllRelatedTransfers(event.getTransferTask().getId());
					
					NotificationManager.process(event.getTransferTask().getUuid(), TransferTaskEventType.CANCELLED.name(), callingUsername, event.getTransferTask().toJSON());
					
				} catch (Throwable e) {
					log.error("Failed to cancel transfer task " + 
							event.getTransferTask().getUuid() + " associated with job " + job.getUuid(), e);
				}
			}
		}
	}		
}
