/**
 * 
 */
package org.iplantc.service.jobs.managers.monitors;

import java.nio.channels.ClosedByInterruptException;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.StaleObjectStateException;
import org.hibernate.UnresolvableObjectException;
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
 * Monitors a cli job by checking for the process id. This is nearly exactly the
 * same process as the HPCMonitor since the command to check id is abstracted in 
 * the SchedulerType enumeration class.
 * 
 * @author dooley
 *
 */
public class ProcessMonitor extends AbstractJobMonitor 
{
	private static final Logger log = Logger.getLogger(ProcessMonitor.class);
	
	public ProcessMonitor(Job job)
	{
		super(job);
	}

	@Override
	public Job monitor() throws RemoteJobMonitoringException, SystemUnavailableException, ClosedByInterruptException
	{
		RemoteSubmissionClient remoteSubmissionClient = null;
		
		try 
		{
			if (this.job != null)
			{
				// if it's a condor job, then it has to be submitted from a condor node.
				// we check to see if this is a submit node. if not, we pass.
			    ExecutionSystem executionSystem = JobManager.getJobExecutionSystem(this.job);
                
				// if the execution system login config is local, then we cannot submit
				// jobs to this system remotely. In this case, a worker will be running
				// dedicated to that system and will submitting jobs locally. All workers
				// other that this will should pass on accepting this job.
				if (executionSystem.getLoginConfig().getProtocol().equals(LoginProtocolType.LOCAL) && 
						!Settings.LOCAL_SYSTEM_ID.equals(this.job.getSystem()))
				{
					return this.job;
				}
				else // otherwise, throw it in remotely
				{
					// increment the number of checks and lastupdated timestamp
				    this.job.setLastUpdated(new Date());
					this.job.setStatusChecks(this.job.getStatusChecks() + 1);
		        	this.job = JobManager.updateStatus(this.job, this.job.getStatus(), this.job.getErrorMessage());
		        	
					ExecutionSystem system = JobManager.getJobExecutionSystem(job);
					
					checkStopped();
					
					remoteSubmissionClient = system.getRemoteSubmissionClient(job.getInternalUsername());
					
					String queryCommand = system.getScheduler().getBatchQueryCommand() + " " + job.getLocalJobId() ;
					
					String result = null;
					try 
					{
						result = remoteSubmissionClient.runCommand(queryCommand);
					}
					catch (Throwable e)
					{
					    this.job = JobManager.updateStatus(this.job, this.job.getStatus(), this.job.getErrorMessage());
						throw new RemoteJobMonitoringException("Failed to run job status check query on " + system.getSystemId(),e);
					}
					finally {
						remoteSubmissionClient.close();
					}
					
					try
					{
					    if (StringUtils.isEmpty(result) || result.toLowerCase().contains("unknown") 
								|| result.toLowerCase().contains("error") || result.toLowerCase().contains("not ")) {
							// we can acutally check the error log file for the timestamp to get 
						    // the endTime in leu of a callback
						    Date logDate = fetchEndDateFromLogFiles();
						    this.job.setEndTime(logDate);
							this.job = JobManager.updateStatus(this.job, JobStatusType.CLEANING_UP, "Job completion detected by process monitor.");

				            if (!this.job.isArchiveOutput()) {
				                log.debug("Job " + this.job.getUuid() + " will skip archiving at user request.");
				                this.job = JobManager.updateStatus(this.job, JobStatusType.FINISHED, "Job completed. Skipping archiving at user request.");
				                log.debug("Job " + this.job.getUuid() + " finished.");
				            }
				            else {
				            	createArchiveTask(job);
				            }
						} else {
						    if (!StringUtils.isBlank(result)) {
						        log.debug("Unrecognized response from status check for job " + this.job.getUuid() + ": " + result);
						    }
	                        this.job = JobManager.updateStatus(this.job, job.getStatus(), this.job.getErrorMessage());
						}
					}
					catch (Exception e) {
						log.error("Failed to updated job " + this.job.getUuid() + " status to " + this.job.getStatus(), e);
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
        catch (Throwable e) 
		{
			throw new RemoteJobMonitoringException("Failed to query status of job " + job.getUuid(), e);
		}
		finally {
			try { remoteSubmissionClient.close(); } catch (Exception e) {}
		}
	}

}
