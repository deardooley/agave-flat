package org.iplantc.service.jobs.managers.monitors;

import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;

import org.iplantc.service.jobs.exceptions.RemoteJobMonitoringException;
import org.iplantc.service.jobs.managers.launchers.JobLauncher;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.remote.RemoteSubmissionClient;
import org.iplantc.service.systems.exceptions.RemoteCredentialException;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.exceptions.AuthenticationException;
import org.iplantc.service.transfer.exceptions.RemoteDataException;

public interface JobMonitor {
    
    /**
     * Checks whether this {@link JobLauncher} has been stopped. 
     * @return true if it has been stopped, false otherwise
     */
    public boolean isStopped();
    
    /**
     * Stops the submission task asynchronously.
     * 
     * @param stopped
     */
    public void setStopped(boolean stopped);
    
    /**
     * Checks whether this launcher has been stopped and if so, 
     * throws a {@link ClosedByInterruptException}
     * 
     * @throws ClosedByInterruptException
     */
    void checkStopped() throws ClosedByInterruptException;
   
	/**
	 * Checks the status of the job on the remote system and updates
	 * the job record as needed.
	 * 
	 * @throws RemoteJobMonitoringException
	 * @throws SystemUnavailableException
	 */
	public Job monitor() 
	throws RemoteJobMonitoringException, SystemUnavailableException, ClosedByInterruptException;
	
	/**
	 * Threadsafe getter of the job passed to the {@link JobMonitor}.
     * @return
     */
	public Job getJob();
	
	/**
	 * Creates a new {@link RemoteSubmissionClient} for the {@link Job} 
	 * {@link ExecutionSystem}.
	 * @return an active {@link RemoteSubmissionClient}
	 * @throws SystemUnavailableException if the system if offline or unknown
	 * @throws Exception if an error occured connecting to the remote {@link ExecutionSystem}
	 */
	public RemoteSubmissionClient getRemoteSubmissionClient() 
	throws Exception;
	
	
	/**
	 * Creates a preauthenticated {@link RemoteDataClient} to the {@link Job} 
	 * {@link ExecutionSystem}. 
	 * @return an active {@link RemoteDataClient}
	 * @throws RemoteDataException
	 * @throws IOException
	 * @throws AuthenticationException
	 * @throws SystemUnavailableException
	 * @throws RemoteCredentialException
	 */
	public RemoteDataClient getAuthenticatedRemoteDataClient()
	throws RemoteDataException, IOException, AuthenticationException, 
		SystemUnavailableException, RemoteCredentialException;

}