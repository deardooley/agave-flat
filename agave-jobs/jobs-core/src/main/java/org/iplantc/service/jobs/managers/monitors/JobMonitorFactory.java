/**
 * 
 */
package org.iplantc.service.jobs.managers.monitors;

import org.iplantc.service.jobs.exceptions.RemoteJobMonitoringException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.phases.workers.IPhaseWorker;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.enumerations.ExecutionType;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;

/**
 * Factory class to get monitoring class for different execution
 * system types.
 * 
 * @author dooley
 *
 */
public class JobMonitorFactory {

	public static JobMonitor getInstance(Job job, IPhaseWorker worker) 
	throws RemoteJobMonitoringException, SystemUnavailableException
	{
		ExecutionSystem executionSystem = JobManager.getJobExecutionSystem(job);
	    
		if (executionSystem == null || !executionSystem.isAvailable()) {
            throw new SystemUnavailableException("Job execution system " 
                    + job.getSystem() + " is not currently available");
        } 
        // if the system is in down time or otherwise unavailable...
        else if (executionSystem.getStatus() != SystemStatusType.UP)
        {
            throw new SystemUnavailableException("Job execution system " 
                    + executionSystem.getSystemId() + " is currently " 
                    + executionSystem.getStatus());
        }
		else
		{
	        ExecutionType executionType = executionSystem.getExecutionType();
	        
			if (executionType == ExecutionType.CONDOR) {
				return new CondorJobMonitor(job, worker);
			} else if (executionType == ExecutionType.HPC) {
				return new HPCMonitor(job, worker);
			} else if (executionType == ExecutionType.CLI) {
				return new ProcessMonitor(job, worker);
			} else {
				throw new RemoteJobMonitoringException("Unable to monitor unknown execution type.");
			}
		}
	}

}
