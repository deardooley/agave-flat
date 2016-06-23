package org.iplantc.service.jobs.managers.killers;

import org.iplantc.service.jobs.exceptions.JobTerminationException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.remote.exceptions.RemoteExecutionException;

public interface JobKiller {

	/**
	 * Run the remote termination process.
	 * 
	 * @throws JobTerminationException
	 * @throws RemoteExecutionException
	 */
	public void attack() throws JobTerminationException, RemoteExecutionException;
	
	/**
	 * Returns a threadsafe reference to the job.
	 * @returns {@link Job} passed into the {@link JobKiller} originally
	 */
	public Job getJob();

}
