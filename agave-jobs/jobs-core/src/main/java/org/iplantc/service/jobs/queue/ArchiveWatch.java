package org.iplantc.service.jobs.queue;

import java.nio.channels.ClosedByInterruptException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.UnresolvableObjectException;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.common.messaging.MessageQueueClient;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.SchedulerException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.queue.actions.ArchiveAction;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.exceptions.SystemUnknownException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.systems.model.enumerations.StorageProtocolType;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;
import org.joda.time.DateTime;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionException;

/**
 * Class to pull a job from the db queue and attempt to archive its data.
 *
 * @author dooley
 *
 */
@DisallowConcurrentExecution
public class ArchiveWatch extends AbstractJobWatch
{
    private static final Logger log = Logger.getLogger(ArchiveWatch.class);

	public ArchiveWatch() {}

	public ArchiveWatch(boolean allowFailure) {
	    super(allowFailure);
	}

	private MessageQueueClient messageClient = null;

	/* (non-Javadoc)
     * @see org.iplantc.service.jobs.queue.WorkerWatch#selectNextAvailableJob()
     */
    @Override
    public String selectNextAvailableJob() throws JobException, SchedulerException {

        // Job.status should be set to JobStatusType.CLEANING_UP upon completion. Once
        // it hits that state, the ArchiveWatch should see the job, if it has
        // Job.archive set to true, it should be archived and the Job.status set to
        // ARCHIVING_FINISHED or ARCHIVING_FAILED depending on the outcome. Otherwise,
        return JobDao.getFairButRandomJobUuidForNextArchivingTask(
                TenancyHelper.getDedicatedTenantIdForThisService(),
                org.iplantc.service.common.Settings.getDedicatedUsernamesFromServiceProperties(),
                org.iplantc.service.common.Settings.getDedicatedSystemIdsFromServiceProperties());
    }

	/* (non-Javadoc)
     * @see org.iplantc.service.jobs.queue.WorkerWatch#doExecute()
     */
	@Override
    public void doExecute() throws JobExecutionException
	{
		// pull the oldest job with JobStatusType.CLEANING_UP from the db
		try
		{
			if (!getJob().isArchiveOutput())
			{
				log.debug("Job " + getJob().getUuid() + " completed. Skipping archiving at user request.");
				this.job = JobManager.updateStatus(job, JobStatusType.FINISHED,
						"Job completed. Skipping archiving at user request.");
			}
			else
			{
				// we will only retry for 7 days after the job should have stopped running
				DateTime jobExpirationDate = new DateTime(getJob().calculateExpirationDate());

				if (jobExpirationDate.plusDays(7).isBeforeNow())
				{
					log.debug("Terminating job " + getJob().getUuid() + " after 7 days trying to archive output.");
					this.job = JobManager.updateStatus(job, JobStatusType.KILLED,
						"Removing job from queue after 7 days attempting to archive output.");
					this.job = JobManager.updateStatus(job, JobStatusType.FAILED,
							"Unable to archive outputs for job after 7 days. Job cancelled.");
					return;
				}

				ExecutionSystem executionSystem = null;
				try {
					
					executionSystem = (ExecutionSystem)new SystemDao().findUserSystemBySystemId(job.getOwner(), job.getSystem(), RemoteSystemType.EXECUTION);
					
					if (executionSystem == null) {
						throw new SystemUnknownException("Archiving failed due to the job "
								+ "execution system, " + job.getSystem() + ", having been deleted. No further "
								+ "action can be taken for this job. The job will be terminated immediately. ");
					}
					else if (!executionSystem.isAvailable()) {
						throw new SystemUnavailableException();
					}
					else if (executionSystem.getStatus() != SystemStatusType.UP) {
						throw new SystemUnavailableException("Job execution system " + job.getSystem() + 
								" is not currently available. " + executionSystem.getStatus().getExpression());
					}
				}	
				catch (SystemUnknownException e) {
					try
					{
						log.error("Failed to archive outputs for job " + this.job.getUuid(), e);
						this.job = JobManager.updateStatus(this.job, JobStatusType.ARCHIVING_FAILED, e.getMessage());
						this.job = JobManager.updateStatus(this.job, JobStatusType.FAILED, 
								"Job failed while archiving outputs due to missing execution system.");
					}
					catch (Exception e1) {
						log.error("Failed to update job " + this.job.getUuid() + " status to FAILED");
					}
					throw new JobExecutionException(e);
				}
				catch (SystemUnavailableException e) {
					try
					{
						if (!StringUtils.contains(job.getErrorMessage(), "paused waiting")) {
            				log.debug("Archiving skipped for job " + getJob().getUuid() + ". Execution system " +
            						executionSystem.getSystemId() + " is currently unavailable.");
            			}
            			this.job = JobManager.updateStatus(job, JobStatusType.CLEANING_UP,
							"Archiving is current paused waiting for the execution system " + executionSystem.getSystemId() +
							" to become available. If the system becomes available again within 7 days, this job " +
							"will resume archiving. After 7 days it will be killed.");
					}
					catch (Throwable e1) {
						log.error("Failed to update job " + this.job.getUuid() + " status to PENDING");
					}	
					throw new JobExecutionException(e);
				}

            	// if the execution system for this job has a local storage config,
            	// all other transfer workers will pass on it.
                if (!StringUtils.equals(Settings.LOCAL_SYSTEM_ID, getJob().getSystem()) &&
                		executionSystem.getStorageConfig().getProtocol().equals(StorageProtocolType.LOCAL))
                {
                    return;
                }
                else
                {
					if (job.getArchiveSystem() != null && (!job.getArchiveSystem().isAvailable() || !job.getArchiveSystem().getStatus().equals(SystemStatusType.UP)))
            		{
            			if (!StringUtils.contains(getJob().getErrorMessage(), "paused waiting")) {
            				log.debug("Archiving skipped for job " + getJob().getUuid() + ". Archive system " +
            						job.getArchiveSystem().getSystemId() + " is currently unavailable. ");
            			}
            			this.job = JobManager.updateStatus(getJob(), JobStatusType.CLEANING_UP,
							"Archiving is current paused waiting for the archival system " + job.getArchiveSystem().getSystemId() +
							" to become available. If the system becomes available again within 7 days, this job " +
							"will resume archiving. After 7 days it will be killed.");
						return;
            		}

            		// mark the job as submitting so no other process claims it
                	try {
                	    this.job = JobManager.updateStatus(getJob(), JobStatusType.ARCHIVING, "Beginning to archive output.");
                	}
                	catch (Throwable e) {
                	    log.debug("Job " + job.getUuid() + " already being processed by another thread. Ignoring.");
                	    return;
                	}

					int attempts = 0;
        			boolean archived = false;

        			// attempt to stage the job several times
        			while (!archived && attempts <= Settings.MAX_SUBMISSION_RETRIES)
        			{
        			    if (stopped.get()) break;

        				attempts++;

        				this.job.setRetries(attempts-1);

        				log.debug("Attempt " + attempts + " to archive job " + getJob().getUuid() + " output");

    					// mark the job as submitting so no other process claims it
						this.job = JobManager.updateStatus(this.job, JobStatusType.ARCHIVING,
						        "Attempt " + attempts + " to archive job output");

						JobDao.persist(getJob());
        				try
						{
        				    if (isStopped()) {
        	                    throw new ClosedByInterruptException();
        	                }

        				    setWorkerAction(new ArchiveAction(getJob()));

        				    try {
        				        getWorkerAction().run();
        				    } catch (Throwable t) {
        				        setJob(getWorkerAction().getJob());
        				        throw t;
        				    }

            				if (!isStopped() || getJob().getStatus() == JobStatusType.ARCHIVING_FINISHED
            				        || getJob().getStatus() == JobStatusType.ARCHIVING_FAILED)
            				{
            				    archived = true;
                				log.debug("Finished archiving job " + getJob().getUuid() + " output");
                				this.job = JobManager.updateStatus(getJob(), JobStatusType.FINISHED);
            				}
						}
        				catch (ClosedByInterruptException e) {
        				    throw e;
        				}
        				catch (SystemUnknownException e)
        				{
        					try
							{
        					    this.job = JobDao.getById(this.job.getId());
								log.error("System for job " + getJob().getUuid() + " is currently unknown. ", e);
								this.job = JobManager.updateStatus(job, JobStatusType.ARCHIVING_FAILED, e.getMessage());

								log.error("Job " + getJob().getUuid() + " completed, but failed to archive output.");
								this.job = JobManager.updateStatus(job, JobStatusType.FINISHED,
										"Job completed, but failed to archive output.");
							}
							catch (Exception e1) {
								log.error("Failed to update job " + getJob().getUuid() + " status to FINISHED");
							}
							break;
        				}
        				catch (SystemUnavailableException e)
						{
							try
							{
							    this.job = JobDao.getById(this.job.getId());
								log.debug("System for job " + getJob().getUuid() + " is currently unavailable. " + e.getMessage());
								this.job = JobManager.updateStatus(job, JobStatusType.CLEANING_UP,
									"Job output archiving is current paused waiting for a system containing " +
									"input data to become available. If the system becomes available again within 7 days, this job " +
									"will resume staging. After 7 days it will be killed.");
							}
							catch (Exception e1) {
								log.error("Failed to update job " + getJob().getUuid() + " status to CLEANING_UP");
							}
							break;
						}
						catch (JobException e)
        				{
						    this.job = JobDao.getById(this.job.getId());

							if (attempts >= Settings.MAX_SUBMISSION_RETRIES )
							{
								log.error("Failed to archive job " + getJob().getUuid() +
										" output after " + attempts + " attempts.", e);
								this.job =JobManager.updateStatus(job, JobStatusType.CLEANING_UP, "Attempt "
										+ attempts + " failed to archive job output. " + e.getMessage());

								log.error("Unable to archive output for job " + getJob().getUuid() +
										" after " + attempts + " attempts.");
								this.job =JobManager.updateStatus(job, JobStatusType.ARCHIVING_FAILED,
										"Unable to archive outputs for job" +
										" after " + attempts + " attempts.");

								log.error("Job " + getJob().getUuid() + " completed, but failed to archive output.");
								this.job =JobManager.updateStatus(job, JobStatusType.FINISHED,
										"Job completed, but failed to archive output.");

								break;
							}
							else
							{
							    try {
							        this.job = JobManager.updateStatus(job, JobStatusType.CLEANING_UP, "Attempt "
							                + attempts + " failed to archive job output.");
							    } catch (Exception e1) {
							        log.error("Attempt " + attempts + " for job " + getJob().getUuid()
							                + " failed to archive output.", e);
							    }
							}
						}
        			}
                }
			}
		}
		catch (ClosedByInterruptException e) {
		    log.debug("Archive task for job " + job.getUuid() + " aborted due to interrupt by worker process.");
		    
		    try {
                job = JobManager.updateStatus(getJob(), JobStatusType.CLEANING_UP, 
                    "Job archiving reset due to worker shutdown. Archiving will resume in another worker automatically.");
                JobDao.persist(job);
            } catch (UnresolvableObjectException | JobException e1) {
                log.error("Failed to roll back job status when archive task was interrupted.", e);
            }
		    throw new JobExecutionException("Staging task for job " + job.getUuid() + " aborted due to interrupt by worker process.");
		}
		catch (StaleObjectStateException | UnresolvableObjectException e) {
		    log.debug("Job " + job.getUuid() + " already being processed by another archiving thread. Ignoring.");
		}
		catch (HibernateException e) {
			log.error("Failed to retrieve job information from db", e);
		}
		catch (Throwable e)
		{
			if (e.getCause() instanceof StaleObjectStateException) {
			    log.debug("Job " + job.getUuid() + "already being processed by another thread. Ignoring.");
			}
			else {
				String message = "Failed to archive job " + getJob().getUuid() + " " + e.getMessage();
				log.error("Failed to archive output for job " + getJob().getUuid(), e);

				try {
				    this.job = JobDao.getById(this.job.getId());
				    this.job =JobManager.updateStatus(job, JobStatusType.ARCHIVING_FAILED, message);
				    this.job =JobManager.updateStatus(job, JobStatusType.FINISHED, "Job completed, but failed to archive.");
				} catch (Exception e1) {}
			}
//			throw new JobExecutionException(e);
		}
		finally {
		    taskComplete.set(true);
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
        try {

            if (getJob().getStatus() != JobStatusType.FINISHED)
            {
                job = JobDao.getById(getJob().getId());
                job.setStatus(JobStatusType.CLEANING_UP,
                        "Job archiving reset due to worker shutdown. Archiving will resume in another worker automatically.");
                JobDao.persist(job);
            }
            else {
                log.error("Job " + job.getUuid() + " is already FINISHED. Skipping roll back of job status to CLEANING_UP.");
            }
        } catch (Throwable e) {
            log.error("Failed to roll back status of job " +
                    job.getUuid() + " to CLEANING_UP upon worker failure.", e);
        }
    }
}
