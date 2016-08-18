package org.iplantc.service.jobs.managers.monitors;

import java.nio.channels.ClosedByInterruptException;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.StaleObjectStateException;
import org.hibernate.UnresolvableObjectException;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitoringException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.remote.RemoteSubmissionClient;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.enumerations.LoginProtocolType;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;

/**
 * Monitors a batch job by querying the remote scheduler for the local job id. 
 * Some work is probably needed to ascertain false negatives and use the
 * job history information available on some systems.
 * 
 * @author dooley
 *
 */
public class HPCMonitor extends AbstractJobMonitor {
	private static final Logger log = Logger.getLogger(HPCMonitor.class);
	
	public HPCMonitor(Job job)
	{
		super(job);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.managers.monitors.AbstractJobMonitor#monitor()
	 */
	@Override
	public Job monitor() throws RemoteJobMonitoringException, SystemUnavailableException, ClosedByInterruptException
	{
		RemoteSubmissionClient remoteSubmissionClient = null;
		
		try 
		{
			if (job != null)
			{
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
					return this.job;
				}
				else // otherwise, throw it in remotely
				{
					// increment the number of checks and lastupdated timestamp
				    
					this.job.setLastUpdated(new Date());
		        	this.job.setStatusChecks(job.getStatusChecks() + 1);
		        	this.job = JobManager.updateStatus(job, job.getStatus(), job.getErrorMessage());
//		        	
//		        	try { 
//		        	    JobDao.persist(job); 
//	        	    } catch (JobException e) { 
//	        	        throw e; 
//	        	    }
		        	
					ExecutionSystem system = JobManager.getJobExecutionSystem(job);
					
					checkStopped();
                    
                    remoteSubmissionClient = getRemoteSubmissionClient();
					
					String queryCommand = system.getScheduler().getBatchQueryCommand() + " " + job.getLocalJobId();
					
					String result = null;
					try
					{
						result = remoteSubmissionClient.runCommand(queryCommand);
					}
					catch (Throwable e) {
					    this.job = JobManager.updateStatus(job, job.getStatus());
						throw new RemoteJobMonitoringException("Failed to run job status query on " + system.getSystemId(), e);
					}
					
					try
					{
						if (StringUtils.isEmpty(result) || result.toLowerCase().contains("unknown") 
								|| result.toLowerCase().contains("error") || result.toLowerCase().contains("not ")) 
						{
							// job not found.
						    log.debug("Job " + job.getUuid() + " no longer present on " + job.getSystem() + 
						            " as local job id " + job.getLocalJobId() + ". Updating status to CLEANING_UP.");
						    Date logDate = new Date();// fetchEndDateFromLogFiles();
                            job.setEndTime(logDate);
							this.job = JobManager.updateStatus(job, JobStatusType.CLEANING_UP, 
									"Job completion detected by batch scheduler monitor.");
							
				            if (!job.isArchiveOutput()) {
				                log.debug("Job " + job.getUuid() + " will skip archiving at user request.");
				                this.job = JobManager.updateStatus(job, JobStatusType.FINISHED, "Job completed. Skipping archiving at user request.");
				                log.debug("Job " + job.getUuid() + " finished.");
				            }
				            else {
				            	createArchiveTask(job);
				            }
						}
						else if (java.util.Arrays.asList(StringUtils.split(result)).contains("Eqw")) 
						{
							// job not found.
						    log.debug("Job " + job.getUuid() + " was found in an unrecoverable state on " + job.getSystem() + 
                                    " as local job id " + job.getLocalJobId() + ". Updating status to FAILED.");
                            this.job = JobManager.updateStatus(job, JobStatusType.FAILED, 
						            "Job failed to move out of system queue.");
							
//							JobDao.persist(job);
						} 
						else if (!job.isRunning())
						{	
						    log.debug("Job " + job.getUuid() + " was found in a RUNNING state on " + job.getSystem() + 
                                    " as local job id " + job.getLocalJobId() + ". Updating status to RUNNING.");
                            
						    this.job = JobManager.updateStatus(job, JobStatusType.RUNNING, 
									"Job status change to running detected by batch scheduler monitor.");
						}
						else
						{
						    log.debug("Job " + job.getUuid() + " is still " + job.getStatus().name() + 
                                    " as local job id " + job.getLocalJobId() + " on " + job.getSystem());
                            
						    this.job = JobManager.updateStatus(job, job.getStatus(), job.getErrorMessage());
						}
					}
					catch (Exception e) {
						log.error("Failed to updated job " + job.getUuid() + " status to " + job.getStatus(), e);
					}
				}
			}
			return this.job;
		}
		catch (ClosedByInterruptException e) {
            throw e;
        }
        catch (StaleObjectStateException | UnresolvableObjectException e) {
            throw e;
        }
        catch (RemoteJobMonitoringException | SystemUnavailableException e) {
            throw e;
        }
        catch (Throwable e) {
			throw new RemoteJobMonitoringException("Failed to query status of job " + job.getUuid(), e);
		}
		finally {
			try { remoteSubmissionClient.close(); } catch (Exception e) {}
		}
	}

}
