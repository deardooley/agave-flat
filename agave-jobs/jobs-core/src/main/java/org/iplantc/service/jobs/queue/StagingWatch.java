package org.iplantc.service.jobs.queue;

import java.nio.channels.ClosedByInterruptException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.StaleObjectStateException;
import org.hibernate.UnresolvableObjectException;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobDependencyException;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.QuotaViolationException;
import org.iplantc.service.jobs.exceptions.SchedulerException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.managers.JobQuotaCheck;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.queue.actions.StagingAction;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.model.enumerations.StorageProtocolType;
import org.joda.time.DateTime;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionException;

/**
 * Class to pull a job from the db queue and attempt to stage in any input
 * files specified as urls using the iPlant IO service.
 * 
 * @author dooley
 * 
 */
@DisallowConcurrentExecution
public class StagingWatch extends AbstractJobWatch 
{
	private static final Logger	log	= Logger.getLogger(StagingWatch.class);
	
	public StagingWatch() {}
	
	public StagingWatch(boolean allowFailure) {
        super(allowFailure);
    }
	
	/* (non-Javadoc)
     * @see org.iplantc.service.jobs.queue.WorkerWatch#selectNextAvailableJob()
     */
    @Override
    public String selectNextAvailableJob() throws JobException, SchedulerException {
        
        return JobDao.getNextQueuedJobUuid(JobStatusType.PENDING, 
                TenancyHelper.getDedicatedTenantIdForThisService(),
                org.iplantc.service.common.Settings.getDedicatedUsernamesFromServiceProperties(),
                org.iplantc.service.common.Settings.getDedicatedSystemIdsFromServiceProperties());
    }

    /* (non-Javadoc)
     * @see org.iplantc.service.jobs.queue.WorkerWatch#doExecute()
     */
    public void doExecute() throws JobExecutionException
	{
		try
		{
			// verify the user is within quota to run the job before staging the data.
		    // this should have been caught by the original job selection, but could change
		    // due to high concurrency. 
			try 
			{
				JobQuotaCheck quotaValidator = new JobQuotaCheck(this.job);
				quotaValidator.check();
			} 
			catch (QuotaViolationException e) 
			{
				try
				{
					log.debug("Input staging for job " + this.job.getUuid() + " is current paused due to quota restrictions. " + e.getMessage());
					this.job = JobManager.updateStatus(this.job, JobStatusType.PENDING, 
						"Input staging for job is current paused due to quota restrictions. " + 
						e.getMessage() + ". This job will resume staging once one or more current jobs complete.");
				}
				catch (Throwable e1) {
					log.error("Failed to update job " + this.job.getUuid() + " status to PENDING");
				}	
				throw new JobExecutionException(e);
			}
			catch (SystemUnavailableException e) 
			{
				try
				{
					log.debug("System for job " + this.job.getUuid() + " is currently unavailable. " + e.getMessage());
					this.job = JobManager.updateStatus(this.job, JobStatusType.PENDING, 
						"Input staging is current paused waiting for a system containing " + 
						"input data to become available. If the system becomes available " +
						"again within 7 days, this job " + 
						"will resume staging. After 7 days it will be killed.");
				}
				catch (Throwable e1) {
					log.error("Failed to update job " + this.job.getUuid() + " status to PENDING");
				}
				throw new JobExecutionException(e);
			}
			catch (Throwable e) 
			{
				try
				{
					log.error("Failed to stage inputs for job " + this.job.getUuid(), e);
					this.job = JobManager.updateStatus(this.job, JobStatusType.FAILED, e.getMessage());
				}
				catch (Throwable e1) {
					log.error("Failed to update job " + this.job.getUuid() + " status to FAILED");
				}
				throw new JobExecutionException(e);
			}
			
			Software software = SoftwareDao.getSoftwareByUniqueName(this.job.getSoftwareName());
            
        	// if the execution system for this job has a local storage config,
        	// all other transfer workers will pass on it.
            if (!StringUtils.equals(Settings.LOCAL_SYSTEM_ID, this.job.getSystem()) &&
            		software.getExecutionSystem().getStorageConfig().getProtocol().equals(StorageProtocolType.LOCAL)) 
            {
                return;
            }
            else
            {	
    			int attempts = 0;
    			boolean staged = false;
    			
    			// we will only retry for 7 days
				if (new DateTime(this.job.getCreated()).plusDays(7).isBeforeNow()) 
				{
				    this.job = JobManager.updateStatus(this.job, JobStatusType.KILLED, 
							"Removing job from queue after 7 days attempting to stage inputs.");
				    this.job = JobManager.updateStatus(this.job, JobStatusType.FAILED, 
							"Unable to stage inputs for job after 7 days. Job cancelled.");
					return;
				} 
				
				// if the job doesn't need to be staged, just move on with things.
    			if (JobManager.getJobInputMap(this.job).isEmpty()) 
				{
    			    this.job = JobManager.updateStatus(this.job, JobStatusType.STAGED, 
    			            "Skipping staging. No input data associated with this job.");
				} 
    			// otherwise, attempt to stage the job Settings.MAX_SUBMISSION_RETRIES times
				else 
				{
        			while (!staged && !isStopped() && attempts <= Settings.MAX_SUBMISSION_RETRIES)
        			{
        				attempts++;
        				
        				this.job.setRetries(attempts-1);
        				
        				log.debug("Attempt " + attempts + " to stage job " + this.job.getUuid() + " inputs");
        				
    					// mark the job as submitting so no other process claims it
        				this.job = JobManager.updateStatus(this.job, JobStatusType.PROCESSING_INPUTS, 
								"Attempt " + attempts + " to stage job inputs");
						
						try 
						{
						    if (isStopped()) {
                                throw new ClosedByInterruptException();
                            }
                            
                            setWorkerAction(new StagingAction(getJob(), null));
                            
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
                            
                            if (!isStopped() || getJob().getStatus() == JobStatusType.STAGED)
                            {       
							    staged = true;
								this.job.setRetries(0);
								JobDao.persist(this.job);
                            }
                            else {
                            	
                            }
						}
						catch (ClosedByInterruptException e) {
//                            log.debug("Staging task for job " + job.getUuid() + " aborted due to interrupt by worker process.");
                            throw e;
                        }
//	                        catch (QuotaViolationException e) 
//							{
//								try
//								{
//									log.debug("Input staging for job " + this.job.getUuid() + " is current paused due to quota restrictions. " + e.getMessage());
//									JobManager.updateStatus(this.job, JobStatusType.PENDING, 
//										"Input staging for job is current paused due to quota restrictions. " + 
//										e.getMessage() + ". This job will resume staging once one or more current jobs complete.");
//								}
//								catch (Throwable e1) {
//									log.error("Failed to update job " + this.job.getUuid() + " status to PENDING");
//								}	
//								break;
//							}
						catch (SystemUnavailableException e) 
						{
							try
							{
								log.debug("System for job " + this.job.getUuid() + " is currently unavailable. " + e.getMessage());
								this.job = JobManager.updateStatus(this.job, JobStatusType.PENDING, 
									"Input staging is current paused waiting for a system containing " + 
									"input data to become available. If the system becomes available " +
									"again within 7 days, this job " + 
									"will resume staging. After 7 days it will be killed.");
							}
							catch (Throwable e1) {
								log.error("Failed to update job " + this.job.getUuid() + " status to PENDING");
							}	
							throw new JobExecutionException(e);
						}
						catch (JobDependencyException e) 
						{
							try
							{
								log.error("Failed to stage inputs for job " + this.job.getUuid(), e);
								this.job = JobManager.updateStatus(this.job, JobStatusType.FAILED, e.getMessage());
							}
							catch (Exception e1) {
								log.error("Failed to update job " + this.job.getUuid() + " status to FAILED");
							}
							throw new JobExecutionException(e);
						}
						catch (JobException e) 
						{
							if (attempts >= Settings.MAX_SUBMISSION_RETRIES ) 
							{
								log.error("Failed to stage job " + this.job.getUuid() + 
										" inputs after " + attempts + " attempts.", e);
								this.job = JobManager.updateStatus(this.job, JobStatusType.STAGING_INPUTS, "Attempt " 
										+ attempts + " failed to stage job inputs. " + e.getMessage());
								try 
								{
								    this.job = JobManager.deleteStagedData(this.job);
								} 
								catch (Throwable e1)
								{
									log.error("Failed to remove remote work directory for job " + this.job.getUuid(), e1);
									this.job = JobManager.updateStatus(this.job, JobStatusType.FAILED, 
											"Failed to remove remote work directory.");
								}
								
								log.error("Unable to stage inputs for job " + this.job.getUuid() + 
										" after " + attempts + " attempts. Job cancelled.");
								this.job = JobManager.updateStatus(this.job, JobStatusType.FAILED, 
										"Unable to stage inputs for job" + 
										" after " + attempts + " attempts. Job cancelled.");
								
								throw new JobExecutionException(e);
							} 
							else {
								
								this.job = JobManager.updateStatus(this.job, JobStatusType.PENDING, "Attempt " 
										+ attempts + " failed to stage job inputs. " + e.getMessage());
							}
						}
						catch (Exception e) 
						{
							try
							{
								log.error("Failed to stage inputs for job " + this.job.getUuid(), e);
								this.job = JobManager.updateStatus(this.job, JobStatusType.FAILED, 
										"Failed to stage file due to unexpected error.");
							}
							catch (Exception e1) {
								log.error("Failed to update job " + this.job.getUuid() + " status to FAILED");
							}
							throw new JobExecutionException(e);
						}
    				}
				}
            }
		}
		catch (JobExecutionException e) {
		    throw e;
		}
		catch (ClosedByInterruptException e) {
            log.debug("Staging task for job " + job.getUuid() + " aborted due to interrupt by worker process.");
            
            try {
                job = JobManager.updateStatus(getJob(), JobStatusType.PENDING, 
                    "Job staging reset due to worker shutdown. Staging will resume in another worker automatically.");
                JobDao.persist(job);
            } catch (UnresolvableObjectException | JobException e1) {
                log.error("Failed to roll back job status when archive task was interrupted.", e);
            }
            throw new JobExecutionException("Staging task for job " + job.getUuid() + " aborted due to interrupt by worker process.");
        }
		catch (StaleObjectStateException | UnresolvableObjectException e) {
            log.debug("Job " + job.getUuid() + " already being processed by another staging thread. Ignoring.");
            throw new JobExecutionException("Job " + job.getUuid() + " already being processed by another staging thread. Ignoring.", e);
        }
		catch (Throwable e)
        {
            if (e.getCause() instanceof StaleObjectStateException) {
                log.debug("Job " + job.getUuid() + " already being processed by another staging thread. Ignoring.");
                throw new JobExecutionException("Job " + job.getUuid() + " already being processed by another staging thread. Ignoring.");
            }
            else 
            {
                String message = "Failed to stage input data for job " + getJob().getUuid();
                log.error(message, e);
                
                try {
                    this.job = JobManager.updateStatus(job, JobStatusType.FAILED, message);
                } catch (Exception e1) {}
                throw new JobExecutionException(e);
            }
        } 
        finally {
            taskComplete.set(true);
            try { HibernateUtil.flush(); } catch (Exception e) {}
            try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
            try { HibernateUtil.disconnectSession(); } catch (Exception e) {}
        }
	}
		
	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.queue.AbstractJobWatch#rollbackStatus()
	 */
	@Override
	protected void rollbackStatus()
    {
	    try { HibernateUtil.closeSession(); } catch (Exception e) {};
        try {
            
            if (getJob().getStatus() != JobStatusType.STAGED) 
            {
                job = JobDao.getById(getJob().getId());
                job.setStatus(JobStatusType.PENDING, 
                        "Job input staging reset due to worker shutdown. Staging will resume in another worker automatically.");
                JobDao.persist(job);
            }
        } catch (Throwable e) {
            log.error("Failed to roll back status of job " + 
                    job.getUuid() + " to PENDING upon worker failure.", e);
        }
    }
}
