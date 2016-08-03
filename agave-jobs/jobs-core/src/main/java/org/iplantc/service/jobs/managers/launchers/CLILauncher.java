/**
 * 
 */
package org.iplantc.service.jobs.managers.launchers;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.managers.parsers.RemoteJobIdParser;
import org.iplantc.service.jobs.managers.parsers.RemoteJobIdParserFactory;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.util.Slug;
import org.iplantc.service.systems.model.enumerations.RemoteShell;
import org.iplantc.service.transfer.RemoteDataClient;

/**
 * Class to fork a background task on a remote linux system. The process
 * id will be stored as the Job.localJobId for querying by the monitoring
 * queue.
 *  
 * @author dooley
 * 
 */
public class CLILauncher extends HPCLauncher 
{
	private static final Logger log = Logger.getLogger(CLILauncher.class);
	
	/**
	 * Creates an instance of a JobLauncher capable of submitting jobs to
	 * Atmosphere VMs.
	 */
	public CLILauncher(Job job)
	{
		super(job);
	}

	@Override
	protected String submitJobToQueue() throws JobException
	{
		RemoteDataClient remoteExecutionDataClient = null;
		try
		{
			submissionClient = executionSystem.getRemoteSubmissionClient(job.getInternalUsername());
			remoteExecutionDataClient = executionSystem.getRemoteDataClient(job.getInternalUsername());
			String cdCommand = null;
			if (!StringUtils.isEmpty(executionSystem.getStartupScript())) {
				cdCommand = "source " + executionSystem.getStartupScript() + " ; ";		
			}
			
			// enter the job directory explicitly
			cdCommand = "cd " + remoteExecutionDataClient.resolvePath(job.getWorkPath());
			
			// get the logfile name so we can redirect output properly
			String logFileBaseName = Slug.toSlug(job.getName());
			
			// grant the file write permissions just in case it doens't have them
			String chmodCommand = " chmod +x " + batchScriptName + " > /dev/null ";
			
			// submit the script and echo the pid or dump the output for debugging
			String submitCommand = " sh -c './"+ batchScriptName + 
					" 2> " + logFileBaseName + ".err 1> " + logFileBaseName + ".out & export AGAVE_PID=$! " +
					" && if [[ -n \"$(ps -o comm= -p $AGAVE_PID)\" ]] || [[ -e " + logFileBaseName + ".pid ]]; then echo $AGAVE_PID; else cat " + logFileBaseName + ".err; fi'";
			
			String submissionResponse = submissionClient.runCommand(
					cdCommand + " && " + chmodCommand + " && " + submitCommand);
					
			if (StringUtils.isEmpty(submissionResponse.trim())) 
			{
				// retry once just in case it was a flickr
				submissionResponse = submissionClient.runCommand(
						cdCommand + " &&  " + chmodCommand + " && " + submitCommand);
				
				if (!ServiceUtils.isValid(submissionResponse.trim())) 
					throw new JobException("Failed to submit cli job. " + submissionResponse);
			}
			
			RemoteJobIdParser jobIdParser = 
					new RemoteJobIdParserFactory().getInstance(executionSystem.getScheduler());
			
			return jobIdParser.getJobId(submissionResponse);
		}
		catch (JobException e) {
			throw e;
		}
		catch (Exception e)
		{
			throw new JobException("Failed to submit job to remote system", e);
		}
		finally
		{
			try { submissionClient.close(); } catch (Exception e){}
			try { remoteExecutionDataClient.disconnect(); } catch (Exception e) {}
		}
	}
	
	@SuppressWarnings("unused")
	private RemoteShell findRemoteShell() throws Exception
	{
	    log.debug("Fetching " + job.getSystem() + " remote shell for job " + job.getUuid());
		String submissionResponse = submissionClient.runCommand("ps -o comm= -p $$");
		submissionResponse = StringUtils.trimToNull(submissionResponse);
		submissionResponse = StringUtils.remove(submissionResponse, "-");
		if (StringUtils.isEmpty(submissionResponse)) {
			return RemoteShell.BASH;
		} 
		
		try {
			return RemoteShell.valueOf(submissionResponse.toUpperCase());
		} catch (Exception e) {
			log.error("Unrecognized remote shell: " +  submissionResponse + ". Using BASH instead.");
			return RemoteShell.BASH;
		}
	}
}