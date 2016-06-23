/**
 * 
 */
package org.iplantc.service.jobs.managers.killers;

import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.enumerations.ExecutionType;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;

/**
 * Factor to init job killers
 * 
 * @author dooley
 * 
 */
public class JobKillerFactory {

	public static JobKiller getInstance(Job job) throws JobException, SystemUnavailableException
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

	    // kill the condor job
		if (executionSystem.getExecutionType().equals(ExecutionType.CONDOR)) { 
			return new CondorKiller(job, executionSystem);
		}
		// or kill the batch job
		else if (executionSystem.getExecutionType().equals(ExecutionType.HPC)) {
		    return new HPCKiller(job, executionSystem);
        }
		// or try to kill the forked process
		else {
		    return new ProcessKiller(job, executionSystem);
		}
	}
}
