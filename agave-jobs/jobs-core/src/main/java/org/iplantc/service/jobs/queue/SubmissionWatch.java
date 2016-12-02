package org.iplantc.service.jobs.queue;

import java.nio.channels.ClosedByInterruptException;

import org.apache.log4j.Logger;
import org.hibernate.StaleObjectStateException;
import org.hibernate.UnresolvableObjectException;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.QuotaViolationException;
import org.iplantc.service.jobs.exceptions.SchedulerException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.managers.JobQuotaCheck;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.queue.actions.SubmissionAction;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.model.enumerations.LoginProtocolType;
import org.joda.time.DateTime;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionException;

/**
 * Class to pull a job from the db queue and attempt to submit it to iplant
 * resources using one of the appropriate execution factory instances.
 * 
 * @author dooley
 * 
 */
@DisallowConcurrentExecution
public class SubmissionWatch extends AbstractJobWatch
{
	private static final Logger	log	= Logger.getLogger(SubmissionWatch.class);
	
	public SubmissionWatch() {}

	public SubmissionWatch(boolean allowFailure) {
        super(allowFailure);
    }
	
	public void doExecute() throws JobExecutionException
	{
		// pull the oldest job with JobStatusType.PENDING from the db and submit
		// it to the remote scheduler.
		try
		{
	    	// verify the user is within quota to run the job before staging the data.
			try 
			{
				JobQuotaCheck quotaValidator = new JobQuotaCheck(job);
				quotaValidator.check();
			} 
			catch (QuotaViolationException e) 
			{
				try
				{
					log.debug("Remote execution of job " + job.getUuid() + " is current paused due to quota restrictions. " + e.getMessage());
					this.job = JobManager.updateStatus(job, JobStatusType.STAGED, 
						"Remote execution of job " + job.getUuid() + " is current paused due to quota restrictions. " + 
						e.getMessage() + ". This job will resume staging once one or more current jobs complete.");
				}
				catch (Throwable e1) {
					log.error("Failed to update job " + job.getUuid() + " status to STAGED");
				}	
				throw new JobExecutionException(e);
			}
			catch (SystemUnavailableException e) 
			{
				try
				{
				    log.debug("One or more dependent systems for job " + getJob().getUuid() 
				            + " is currently unavailable. " + e.getMessage());
				    this.job = JobManager.updateStatus(job, JobStatusType.STAGED, 
						"Remote execution of job " + job.getUuid() + " is current paused waiting for " + job.getSystem() + 
						"to become available. If the system becomes available again within 7 days, this job " + 
						"will resume staging. After 7 days it will be killed.");
				}
				catch (Throwable e1) {
					log.error("Failed to update job " + job.getUuid() + " status to STAGED");
				}	
				throw new JobExecutionException(e);
			}
			catch (StaleObjectStateException e) {
				log.debug("Just avoided a job submission race condition for job " + job.getUuid());
				throw new JobExecutionException(e);
			}
			catch (Throwable e) 
			{
				try
				{
					log.error("Failed to verify user quota for job " + job.getUuid() + 
							". Job will be returned to queue and retried later.", e);
					job.setRetries(job.getRetries()+1);
					this.job = JobManager.updateStatus(job, JobStatusType.STAGED, 
							"Failed to verify user quota for job " + job.getUuid() + 
							". Job will be returned to queue and retried later.");
				}
				catch (Throwable e1) {
					log.error("Failed to update job " + job.getUuid() + " status to FAILED");
				}
				throw new JobExecutionException(e);
			}
			
			// kill jobs past their max lifetime
			DateTime jobSubmissionDeadline = new DateTime(job.getSubmitTime());
			
			if (jobSubmissionDeadline.plusDays(14).isBeforeNow()) 
			{
				log.debug("Terminating job " + job.getUuid() + 
						" after 42 days of running without a status update.");
				this.job = JobManager.updateStatus(job, JobStatusType.KILLED, 
					"Killing job after 42 days of running without a status update");
				this.job = JobManager.updateStatus(job, JobStatusType.FAILED, 
						"Job did not complete with 42 days. Job cancelled.");
				return;
			} 
			
			// if it's a condor job, then it has to be submitted from a condor node.
			// we check to see if this is a submit node. if not, we pass.
			Software software = SoftwareDao.getSoftwareByUniqueName(job.getSoftwareName());
			
			// if the execution system login config is local, then we cannot submit
			// jobs to this system remotely. In this case, a worker will be running
			// dedicated to that system and will submitting jobs locally. All workers
			// other that this will should pass on accepting this job.
			if (software.getExecutionSystem().getLoginConfig().getProtocol().equals(LoginProtocolType.LOCAL) && 
					!Settings.LOCAL_SYSTEM_ID.equals(job.getSystem()))
			{
				return;
			}
			else // otherwise, throw it in remotely
			{
				// mark the job as submitting so no other process claims it
				// note: we should have jpa optimistic locking enabled, so
				// no race conditions should exist at this point.
			    this.job = JobManager.updateStatus(job, JobStatusType.SUBMITTING, 
						"Preparing job for submission.");
				
				if (isStopped()) {
                    throw new ClosedByInterruptException();
                }
                
				setWorkerAction(new SubmissionAction(getJob(), null));
				
				try {
				    // wrap this in a try/catch so we can update the local reference to the 
				    // job before it hist
				    getWorkerAction().run();
				} catch (Exception e) {
				    throw e;
				}
				finally {
				    this.job = getWorkerAction().getJob();
				}
                
                if (!isStopped() || this.job.getStatus() == JobStatusType.QUEUED || 
                        this.job.getStatus() == JobStatusType.RUNNING)
                {       
                    getJob().setRetries(0);
                    JobDao.persist(this.job);
                }
			}
		}
		catch (JobExecutionException e) {
		    throw e;
		}
		catch (ClosedByInterruptException e) {
            log.debug("Submission task for job " + job.getUuid() + " aborted due to interrupt by worker process.");
            
            try {
                job = JobManager.updateStatus(getJob(), JobStatusType.STAGED, 
                    "Job submission aborted due to worker shutdown. Job will be resubmitted automatically.");
                JobDao.persist(job);
            } catch (UnresolvableObjectException | JobException e1) {
                log.error("Failed to roll back job status when archive task was interrupted.", e);
            }
            throw new JobExecutionException("Submission task for job " + job.getUuid() + " aborted due to interrupt by worker process.");
        }
        catch (StaleObjectStateException | UnresolvableObjectException e) {
			log.debug("Job " + job.getUuid() + " already being processed by another submission thread. Ignoring.");
			throw new JobExecutionException("Job " + job.getUuid() + " already being processed by another submission thread. Ignoring.");
		}
		catch (Throwable e)
		{
			if (e.getCause() instanceof StaleObjectStateException) {
				log.debug("Just avoided a job submission staging race condition for job " + job.getUuid());
				throw new JobExecutionException("Job " + job.getUuid() + " already being processed by another thread. Ignoring.");
			}
			else if (job == null)
			{
				log.error("Failed to retrieve job information from db", e);
				throw new JobExecutionException(e);
			}
			else
			{
				try
				{
					log.error("Failed to submit job " + job.getUuid(), e);
					this.job = JobManager.updateStatus(job, JobStatusType.FAILED,
							"Failed to submit job " + job.getUuid() + " due to internal errors");
				}
				catch (Exception e1)
				{
					log.error("Failed to update job " + job.getUuid() + " status to failed");
				}
				throw new JobExecutionException(e);
			}
		}
		finally {
		    setTaskComplete(true);
            try { HibernateUtil.flush(); } catch (Exception e) {}//e.printStackTrace();};
            try { HibernateUtil.commitTransaction(); } catch (Exception e) {}//e.printStackTrace();};
            try { HibernateUtil.disconnectSession(); } catch (Exception e) {}//e.printStackTrace();};
		}
	}
	
	/* (non-Javadoc)
     * @see org.iplantc.service.jobs.queue.AbstractJobWatch#rollbackStatus()
     */
    @Override
    protected void rollbackStatus()
    {
        try 
        {
            if (getJob().getStatus() != JobStatusType.QUEUED && getJob().getStatus() != JobStatusType.RUNNING) 
            {
                JobManager.updateStatus(getJob(), JobStatusType.STAGED, 
                        "Job submission reset due to worker shutdown. Staging will resume in another worker automatically.");
            }
        } catch (Throwable e) {
            log.error("Failed to roll back status of job " + 
                    getJob().getUuid() + " to STAGED upon worker failure.", e);
        }
    }
    
    @Override
    public String selectNextAvailableJob() throws JobException, SchedulerException {
        
        return JobDao.getNextQueuedJobUuid(JobStatusType.STAGED, 
                TenancyHelper.getDedicatedTenantIdForThisService(),
                org.iplantc.service.common.Settings.getDedicatedUsernamesFromServiceProperties(),
                org.iplantc.service.common.Settings.getDedicatedSystemIdsFromServiceProperties());
    }
}
