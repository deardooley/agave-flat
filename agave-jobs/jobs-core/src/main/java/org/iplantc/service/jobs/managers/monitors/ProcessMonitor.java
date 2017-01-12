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
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobFinishedException;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitorResponseParsingException;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitoringException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.managers.monitors.parsers.ForkJobMonitorResponseParser;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobUpdateParameters;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.phases.workers.IPhaseWorker;
import org.iplantc.service.remote.RemoteSubmissionClient;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.enumerations.LoginProtocolType;
import org.joda.time.DateTime;

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
	
	public ProcessMonitor(Job job, IPhaseWorker worker)
	{
		super(job, worker);
	}

	@Override
	public Job monitor() 
	   throws RemoteJobMonitoringException, SystemUnavailableException, 
	          ClosedByInterruptException, JobFinishedException
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
				    this.job.setLastUpdated(new DateTime().toDate());
					this.job.setStatusChecks(this.job.getStatusChecks() + 1);
					
					// Save changed fields along with status.
					JobUpdateParameters jobUpdateParameters = new JobUpdateParameters();
					jobUpdateParameters.setLastUpdated(this.job.getLastUpdated());
					jobUpdateParameters.setStatusChecks(this.job.getStatusChecks());
		        	this.job = JobManager.updateStatus(this.job, this.job.getStatus(), 
		        	                                   this.job.getErrorMessage(),
		        	                                   jobUpdateParameters);
		        	
					ExecutionSystem system = JobManager.getJobExecutionSystem(job);
					
					checkStopped();
					
					remoteSubmissionClient = system.getRemoteSubmissionClient(job.getInternalUsername());
					
					String startupScriptCommand = getStartupScriptCommand();
        			
					String queryCommand = startupScriptCommand + system.getScheduler().getBatchQueryCommand() + " " + job.getLocalJobId() ;
					
					String result = null;
					try 
					{
						if (log.isDebugEnabled())
						    log.debug("Forking command " + queryCommand + " on " + 
								remoteSubmissionClient.getHost() + ":" + remoteSubmissionClient.getPort() +
								" for job " + job.getUuid());
						
						result = remoteSubmissionClient.runCommand(queryCommand);
						
						if (log.isDebugEnabled())
						    log.debug("Response for job " + job.getUuid() + " monitoring command was: " + result);
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
							// don't check the remote time. the time zone isn't known
//						    Date logDate = fetchEndDateFromLogFiles();
					    	updateStatusOfFinishedJob();
						} else {
							ForkJobMonitorResponseParser parser = new ForkJobMonitorResponseParser();
							try {
								boolean running = parser.isJobRunning(result);
							
								if (!running) {
									updateStatusOfFinishedJob();
								}
								else {
								    if (log.isDebugEnabled())
								        log.debug("Job " + job.getUuid() + " is still " + job.getStatus().name() + 
		                                    " as local job id " + job.getLocalJobId() + " on " + job.getSystem());
									this.job = JobManager.updateStatus(this.job, job.getStatus(), this.job.getErrorMessage());
								}
						    }
							catch (RemoteJobMonitorResponseParsingException e) {
							    if (log.isDebugEnabled())
							        log.debug("Unrecognized response from status check for job " + this.job.getUuid() + ": " + result, e);
								this.job = JobManager.updateStatus(this.job, job.getStatus(), this.job.getErrorMessage());
							}
							
							
						}
					}
					catch (Exception e) {
						log.error("Failed to updated job " + this.job.getUuid() + " status to " + this.job.getStatus(), e);
					}
				}
			}
			
			return this.job;
		}
		catch (ClosedByInterruptException | JobFinishedException e) {
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

	

	/**
	 * @throws JobException
	 */
	protected void updateStatusOfFinishedJob() throws JobException {
		Date logDate = new DateTime().toDate();
		this.job.setEndTime(logDate);
		JobUpdateParameters jobUpdateParameters = new JobUpdateParameters();
		jobUpdateParameters.setEndTime(this.job.getEndTime());
		this.job = JobManager.updateStatus(this.job, JobStatusType.CLEANING_UP, 
		                                   "Job completion detected by process monitor.",
		                                   jobUpdateParameters);

		if (!this.job.isArchiveOutput()) {
		    if (log.isDebugEnabled()) 
		        log.debug("Job " + this.job.getUuid() + " will skip archiving at user request.");
		    this.job = JobManager.updateStatus(this.job, JobStatusType.FINISHED, 
		                                       "Job completed. Skipping archiving at user request.");
		    if (log.isDebugEnabled())
		        log.debug("Job " + this.job.getUuid() + " finished.");
		}
	}

}
