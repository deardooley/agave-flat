/**
 * 
 */
package org.iplantc.service.jobs.managers.killers;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.jobs.exceptions.JobTerminationException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.remote.RemoteSubmissionClient;
import org.iplantc.service.remote.exceptions.RemoteExecutionException;
import org.iplantc.service.systems.model.ExecutionSystem;

/**
 * Handles actual mechanics of killing a job. Implementing classes
 * simply provide the appropriate command to invoke.
 * 
 * @author dooley
 *
 */
public abstract class AbstractJobKiller implements JobKiller {

    private Job	job;
	private ExecutionSystem executionSystem;

	public AbstractJobKiller(Job job, ExecutionSystem executionSystem)
	{
	    this.job = job;
        this.executionSystem = executionSystem;
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.managers.killers.JobKiller#attack()
	 */
	@Override
	public void attack() throws JobTerminationException, RemoteExecutionException
	{
		RemoteSubmissionClient remoteSubmissionClient = null;
		
		try
		{
			remoteSubmissionClient = getExecutionSystem().getRemoteSubmissionClient(getJob().getInternalUsername());
			
			String result = remoteSubmissionClient.runCommand(getCommand());
			
			if (StringUtils.isEmpty(result)) 
			{ 
				throw new RemoteExecutionException(result); 
			}
			else
			{
			    String[] notFoundTerms = new String[] {"does not exist", "has deleted job", "couldn't find"};
			    for (String notfoundTerm: notFoundTerms) 
			    {
			        if (result.toLowerCase().contains(notfoundTerm)) {
			            throw new RemoteExecutionException(result);
			        }
			    }
			}
		}
		catch (RemoteExecutionException e) {
		    throw e;
		}
		catch (Throwable e)
		{
			throw new JobTerminationException("Failed to stop job " + getJob().getUuid() 
			        + " identified by id " + getJob().getLocalJobId() + " on " + getJob().getSystem(), e);
		}
		finally {
		    try { remoteSubmissionClient.close(); } catch (Throwable e) {}
		}
	}
	
	/**
     * Provides the actual command that should be invoked on the remote
     * system to kill the job.
     * 
     * @return 
     */
    protected abstract String getCommand();

    /**
     * @return the job
     */
    public synchronized Job getJob() {
        return job;
    }

    /**
     * @param job the job to set
     */
    public synchronized void setJob(Job job) {
        this.job = job;
    }

    /**
     * @return the executionSystem
     */
    public synchronized ExecutionSystem getExecutionSystem() {
        return executionSystem;
    }

    /**
     * @param executionSystem the executionSystem to set
     */
    public synchronized void setExecutionSystem(ExecutionSystem executionSystem) {
        this.executionSystem = executionSystem;
    }

}
