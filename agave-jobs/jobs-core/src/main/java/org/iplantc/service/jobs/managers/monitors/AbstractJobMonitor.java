/**
 * 
 */
package org.iplantc.service.jobs.managers.monitors;

import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitoringException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.scripts.SubmitScript;
import org.iplantc.service.jobs.model.scripts.SubmitScriptFactory;
import org.iplantc.service.remote.RemoteSubmissionClient;
import org.iplantc.service.systems.exceptions.RemoteCredentialException;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteFileInfo;
import org.iplantc.service.transfer.exceptions.AuthenticationException;
import org.iplantc.service.transfer.exceptions.RemoteDataException;

/**
 * Abstract class to structure individual job monitoring.
 * 
 * @author dooley
 *
 */
public abstract class AbstractJobMonitor implements JobMonitor {

    private static final Logger log = Logger.getLogger(AbstractJobMonitor.class);
    
    private AtomicBoolean stopped = new AtomicBoolean(false);
    
    protected Job job;
	
	public AbstractJobMonitor(Job job) {
		this.job = job;
	}
	
	/* (non-Javadoc)
     * @see org.iplantc.service.jobs.managers.launchers.JobLauncher#isStopped()
     */
    @Override
    public synchronized boolean isStopped() {
        return stopped.get();
    }

    /* (non-Javadoc)
     * @see org.iplantc.service.jobs.managers.launchers.JobLauncher#setStopped(boolean)
     */
    @Override
    public synchronized void setStopped(boolean stopped) {
        this.stopped.set(stopped);
    }
    
    /* (non-Javadoc)
     * @see org.iplantc.service.jobs.managers.monitors.JobMonitor#getJob()
     */
    @Override
    public synchronized Job getJob() {
        return this.job;
    }
    
    
    
    /* (non-Javadoc)
     * @see org.iplantc.service.jobs.managers.launchers.JobLauncher#checkStopped()
     */
    @Override
    public void checkStopped() throws ClosedByInterruptException {
        if (isStopped()) {
            throw new ClosedByInterruptException();
        }
    }
    
    /* (non-Javadoc)
	 * @see org.iplantc.service.jobs.managers.monitors.JobMonitor#getRemoteSubmissionClient()
	 */
	@Override
	public RemoteSubmissionClient getRemoteSubmissionClient() 
	throws Exception 
	{
		ExecutionSystem system = JobManager.getJobExecutionSystem(job);
		return system.getRemoteSubmissionClient(job.getInternalUsername());
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.managers.monitors.JobMonitor#getRemoteDataClient()
	 */
	@Override
	public RemoteDataClient getAuthenticatedRemoteDataClient() 
	throws RemoteDataException, IOException, AuthenticationException, 
		SystemUnavailableException, RemoteCredentialException 
	{	
		RemoteDataClient remoteDataClient = JobManager.getJobExecutionSystem(job).getRemoteDataClient(job.getInternalUsername());
        remoteDataClient.authenticate();
        return remoteDataClient;
	}

	/**
     * Pulls end date from remote log files in the job directory, if present.
     * 
     * @return
     */
    protected Date fetchEndDateFromLogFiles()
    {
        RemoteDataClient remoteDataClient = null;
        RemoteFileInfo stdOutFileInfo;
        RemoteFileInfo stdErrFileInfo;
        Date errDate = null;
        Date outDate = null;
        Date logDate = new Date();
        try 
        {   log.debug("Attempting to fetch completion time for job " + job.getUuid() + " from logfile timestamps");
            remoteDataClient = getAuthenticatedRemoteDataClient();
            
            // get the output filenames from the SubmitScript for the job.
            SubmitScript script = SubmitScriptFactory.getScript(job);
            String stdOut = job.getWorkPath() + "/" + script.getStandardOutputFile();
            String stdErr = job.getWorkPath() + "/" + script.getStandardErrorFile();
            
            if (remoteDataClient.doesExist(stdErr)) {
                stdErrFileInfo = remoteDataClient.getFileInfo(stdErr);
                errDate = stdErrFileInfo.getLastModified();
            } 
            
            if (remoteDataClient.doesExist(stdOut)) {
                stdOutFileInfo = remoteDataClient.getFileInfo(stdOut);
                outDate = stdOutFileInfo.getLastModified();
            }
        } catch (Throwable e) {
            log.error("Failed to retrieve completion timestamp for job " + job.getUuid() + 
                    " from logfile timestamps.", e);
            
        }
        finally {
        	try { remoteDataClient.disconnect(); } catch (Exception e) {}
        	remoteDataClient = null;
        }
            
        if (errDate != null && outDate != null) {
            if (errDate.compareTo(outDate) >= 0) {
                logDate = errDate;
            } else {
                logDate = outDate;
            }
        } else if (errDate != null) {
            logDate = errDate;
        } else if (outDate != null) {
            logDate = outDate;
        }
        
        if (job.getStartTime() != null && logDate.after(job.getStartTime())) {
            return logDate;
        } else {
            return new Date();
        }
    }
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.managers.monitors.JobMonitor#getStatus()
	 */
	@Override
	public abstract Job monitor() throws RemoteJobMonitoringException, SystemUnavailableException, ClosedByInterruptException;
}
