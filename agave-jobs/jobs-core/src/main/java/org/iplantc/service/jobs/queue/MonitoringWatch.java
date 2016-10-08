package org.iplantc.service.jobs.queue;

import java.nio.channels.ClosedByInterruptException;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.UnresolvableObjectException;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.SchedulerException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.queue.actions.MonitoringAction;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.model.enumerations.StorageProtocolType;
import org.joda.time.DateTime;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionException;

/**
 * Class to watch job status on remote systems. This can also run 
 * locally.
 * 
 * TODO: Batch jobs on a particular system together for efficiency sake.
 * 
 * @author dooley
 * 
 */
//@DisallowConcurrentExecution
public class MonitoringWatch extends AbstractJobWatch 
{
	private static final Logger	log	= Logger.getLogger(MonitoringWatch.class);
	
	public MonitoringWatch() {}
	
	public MonitoringWatch(boolean allowFailure) {
        super(allowFailure);
    }
	
	/* (non-Javadoc)
     * @see org.iplantc.service.jobs.queue.WorkerWatch#selectNextAvailableJob()
     */
    @Override
    public String selectNextAvailableJob() throws JobException, SchedulerException {
        
        return JobDao.getNextExecutingJobUuid( 
                TenancyHelper.getDedicatedTenantIdForThisService(),
                org.iplantc.service.common.Settings.getDedicatedUsernamesFromServiceProperties(),
                org.iplantc.service.common.Settings.getDedicatedSystemIdsFromServiceProperties());
    }
	
	public void doExecute() throws JobExecutionException
	{
		// pull the oldest job with JobStatusType.CLEANING_UP from the db
		try
		{	
			
			if (job.getStatus().equals(JobStatusType.ARCHIVING) || 
			        job.getStatus().equals(JobStatusType.CLEANING_UP)) 
			{
				return;
			} 
			else
			{
				// kill jobs past their max lifetime
				if (job.getEndTime() != null) {
					
					this.job = JobManager.updateStatus(job, JobStatusType.FINISHED, 
							"Setting job " + job.getUuid() + " status to FINISHED due to previous completion event.");
					
					log.debug("Skipping watch on job " + job.getUuid() + 
							" due to previous completion. Setting job status to FINISHED due to previous completion event.");
					return;
				} 
				else if (job.calculateExpirationDate().before(new DateTime().toDate())) 
				{
					log.debug("Terminating job " + job.getUuid() + 
							" after for not completing prior to the expiration date " + 
							new DateTime(job.calculateExpirationDate()).toString());
					this.job = JobManager.updateStatus(job, JobStatusType.KILLED, 
							"Terminating job " + job.getUuid() + 
							" after for not completing prior to the expiration date " + 
							new DateTime(job.calculateExpirationDate()).toString());
					this.job = JobManager.updateStatus(job, JobStatusType.FAILED, 
							"Job " + job.getUuid() + " did not complete by " + new DateTime(job.calculateExpirationDate()).toString() +
							". Job cancelled.");
					return;
				} 
				
				// if the execution system for this job has a local storage config,
            	// all other transfer workers will pass on it.
				if (!StringUtils.equals(Settings.LOCAL_SYSTEM_ID, job.getSystem()) &&
				        JobManager.getJobExecutionSystem(job).getStorageConfig().getProtocol().equals(StorageProtocolType.LOCAL)) 
                {
                    return;
                } 
                else 
                {
                    this.job = JobManager.updateStatus(this.job,  this.job.getStatus(), this.job.getErrorMessage());
                    
                    setWorkerAction(new MonitoringAction(job));
                    
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
                }
			} 
		}
		catch (ClosedByInterruptException e) {
            log.debug("Monitoring task for job " + job.getUuid() + " aborted due to interrupt by worker process.");
            throw new JobExecutionException("Submission task for job " + job.getUuid() + " aborted due to interrupt by worker process.");
        }
        catch (StaleObjectStateException | UnresolvableObjectException e) {
//            log.error("Job " + job.getUuid() + " is already being processed by another monitoring thread. Ignoring.");
            throw new JobExecutionException("Job " + job.getUuid() + " already being processed by another thread. Ignoring.");
        }
		catch (SystemUnavailableException e) {
            String message = "Monitoring task for job " + job.getUuid() 
                    + ". Execution system " + job.getSystem() + " is currently unavailable. ";
            log.debug(message);
            try {
                this.job = JobManager.updateStatus(this.job,  this.job.getStatus(), message);
            } catch (JobException e1) {
                log.error("Failed to updated job " + this.job.getUuid() + " timestamp", e);
            }
            throw new JobExecutionException(e);
        }
        catch (HibernateException e) {
            log.error("Failed to retrieve job information from db", e);
            throw new JobExecutionException(e);
        }
        catch (Throwable e) {
            log.error("Monitoring task for job " + job.getUuid() + " failed.", e);
            throw new JobExecutionException(e);
		}
		finally {
            taskComplete.set(true);
            try { HibernateUtil.flush(); } catch (Exception e) {}//e.printStackTrace();};
            try { HibernateUtil.commitTransaction(); } catch (Exception e) {}//e.printStackTrace();};
            try { HibernateUtil.disconnectSession(); } catch (Exception e) {}//e.printStackTrace();};
        }
	}
	
	@Override
    protected void rollbackStatus()
    {
	    // nothing to do here. we just abandon the job check and move on
    }
}
