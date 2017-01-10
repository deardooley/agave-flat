/**
 * 
 */
package org.iplantc.service.jobs.managers.launchers;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.managers.launchers.parsers.RemoteJobIdParser;
import org.iplantc.service.jobs.managers.launchers.parsers.RemoteJobIdParserFactory;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.util.Slug;
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
			submissionClient = getExecutionSystem().getRemoteSubmissionClient(getJob().getInternalUsername());
			
			remoteExecutionDataClient = getExecutionSystem().getRemoteDataClient(getJob().getInternalUsername());
			
			// Get the remote work directory for the log file
			String remoteWorkPath = remoteExecutionDataClient.resolvePath(getJob().getWorkPath());
						
			// Resolve the startupScript and generate the command to run it and log the response to the
			// remoteWorkPath + "/.agave.log" file
			String startupScriptCommand = getStartupScriptCommand(remoteWorkPath);
						
			// command to cd to the remoteWorkPath
			String cdCommand = " cd " + remoteWorkPath;
			
			// ensure the wrapper template has execute permissions
			String chmodCommand = " chmod +x " + batchScriptName + " > /dev/null ";
			
			// get the logfile name so we can redirect output properly
			String logFileBaseName = Slug.toSlug(getJob().getName());
				
			// command to submit the resolved wrapper template (*.ipcexe) script to the 
			// scheduler. This forks the command in a child process redirecting stderr 
			// to the logFileBaseName + ".err" and stdout to logFileBaseName + ".out".
			// The process id of the invoked wrapper script is captured and quickly 
			// evaluated to see if the script failed immediately. If so, the error 
			// response is echoed back to the service. Otherwise, the process id is
			// echoed to stdout for the RemoteJobIdParser to extract and associate with
			// the job record.
			String submitCommand = String.format(" sh -c './%s 2> %s.err 1> %s.out & " + 
												 " export AGAVE_PID=$! && " +
												 " if [ -n \"$(ps -o comm= -p $AGAVE_PID)\" ] || [ -e %s.pid ]; then " + 
												 	" echo $AGAVE_PID; " + 
												 " else " + 
												 	" cat %s.err; " + 
												 " fi'",
					batchScriptName,
					logFileBaseName,
					logFileBaseName,
					logFileBaseName,
					logFileBaseName);
			
			// run the aggregate command on the remote system
			String submissionResponse = submissionClient.runCommand(
					startupScriptCommand + " ; " + cdCommand + " && " + chmodCommand + " && " + submitCommand);
					
			if (StringUtils.isBlank(submissionResponse)) 
			{
				// retry the remote command once just in case it was a flicker
				submissionResponse = submissionClient.runCommand(
						startupScriptCommand + " ; " + cdCommand + " &&  " + chmodCommand + " && " + submitCommand);
				
				// blank response means the job didn't go in...twice. Fail the attempt
				if (StringUtils.isBlank(submissionResponse)) 
					throw new JobException("Failed to submit cli job. " + submissionResponse);
			}
			
			// parse the response from the remote command invocation to get the localJobId
			// by which we'll reference the job during monitoring, etc.
			RemoteJobIdParser jobIdParser = 
					new RemoteJobIdParserFactory().getInstance(getExecutionSystem().getScheduler());
			
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
}