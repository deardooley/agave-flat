/**
 * 
 */
package org.iplantc.service.jobs.managers.killers;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.jobs.exceptions.JobTerminationException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.StartupScriptJobVariableType;
import org.iplantc.service.remote.RemoteSubmissionClient;
import org.iplantc.service.remote.exceptions.RemoteExecutionException;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.enumerations.SchedulerType;
import org.iplantc.service.systems.model.enumerations.StartupScriptSystemVariableType;

/**
 * Handles actual mechanics of killing a job. Implementing classes
 * simply provide the appropriate command to invoke.
 * 
 * @author dooley
 *
 */
public abstract class AbstractJobKiller implements JobKiller {
	
	private static final Logger log = Logger.getLogger(AbstractJobKiller.class);
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
			
			// Resolve the startupScript and generate the command to run it and log the response to the
			// "/.agave.log" file
			String startupScriptCommand = getStartupScriptCommand();
			
			// get the command to kill the remote job process
			String remoteJobKillCommand = getCommand();
			
			// run the aggregate command on the remote system
			String result = remoteSubmissionClient.runCommand(
					startupScriptCommand + " ; " + remoteJobKillCommand);
			
			// if the response was empty, the job could be done, but the scheduler could only 
			// recognize numeric job ids. Let's try again with just the numeric part
			if (StringUtils.isBlank(result)) {
				String altCommand = getAltCommand();
				if (StringUtils.isBlank(altCommand)) {
					log.debug("Empty response found when checking remote execution system of agave job " 
							+ job.getUuid() + " for local batch job id "+ job.getLocalJobId() 
							+ ". No numeric job id found in the batch job id for remtoe system. "
							+ "No further attempt will be made.");
				}
				else {
					log.debug("Empty response found when checking remote execution system of agave job " 
							+ job.getUuid() + " for local batch job id "+ job.getLocalJobId() 
							+ ". Attempting to recheck with just the numeric job id " + job.getNumericLocalJobId());
					result = remoteSubmissionClient.runCommand(startupScriptCommand + " ; " + altCommand);
				}
			}
			
			if (StringUtils.isEmpty(result) && getExecutionSystem().getScheduler() != SchedulerType.FORK) {
				throw new RemoteExecutionException(result); 
			}
			else {
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
     * @param startupScript
     * @return
     * @throws SystemUnavailableException
     */
    public String resolveStartupScriptMacros(String startupScript) 
	throws SystemUnavailableException 
	{
		if (StringUtils.isBlank(startupScript)) {
			return null;
		}
		else {
			String resolvedStartupScript = startupScript;
			for (StartupScriptSystemVariableType macro: StartupScriptSystemVariableType.values()) {
				resolvedStartupScript = StringUtils.replace(resolvedStartupScript, "${" + macro.name() + "}", macro.resolveForSystem(getExecutionSystem()));
			}
			
			for (StartupScriptJobVariableType macro: StartupScriptJobVariableType.values()) {
				resolvedStartupScript = StringUtils.replace(resolvedStartupScript, "${" + macro.name() + "}", macro.resolveForJob(getJob()));
			}
			
			return resolvedStartupScript;
		}
	}
	
	/**
	 * @return
	 * @throws SystemUnavailableException
	 */
	public String getStartupScriptCommand() throws SystemUnavailableException {
		String startupScriptCommand = "";
		if (!StringUtils.isEmpty(getExecutionSystem().getStartupScript())) {
			String resolvedstartupScript = resolveStartupScriptMacros(getExecutionSystem().getStartupScript());
			
			if (resolvedstartupScript != null) {
//				startupScriptCommand = String.format("echo $(source %s 2>&1) >> %s/.agave.log ; ",
//					resolvedstartupScript,
//					remoteDataClient.resolvePath(job.getWorkPath()));
				startupScriptCommand = String.format("echo $(source %s 2>&1) >> /dev/null ; ",
						resolvedstartupScript);
			}
		}
		return startupScriptCommand;
	}
	
	/**
     * Provides the actual command that should be invoked on the remote
     * system to kill the job.
     * 
     * @return 
     */
    protected abstract String getCommand();
    
    /**
     * Provides the actual command that should be invoked on the remote
     * system to kill the job using the shortened numeric remote local job id.
     * 
     * @return the same as {@link #getCommand()}, but with the {@link Job#getNumericLocalJobId()}, null if they are the same
     */
    protected abstract String getAltCommand();

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
