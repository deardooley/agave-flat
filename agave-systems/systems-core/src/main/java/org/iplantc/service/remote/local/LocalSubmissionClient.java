package org.iplantc.service.remote.local;

import org.iplantc.service.remote.RemoteSubmissionClient;
import org.iplantc.service.remote.exceptions.RemoteExecutionException;

/**
 * Forks a command to the local system using ProcessBuilder.
 * @author dooley
 *
 */
public class LocalSubmissionClient implements RemoteSubmissionClient 
{
	private String hostname;
	
	public LocalSubmissionClient(String hostname) {
		this.hostname = hostname;
	}
	
	@Override
	public void close() {}
	
	public String runCommand(String command) throws Exception
	{	
		CmdLineProcessHandler processHandler = null;
		CmdLineProcessOutput processOutput = null;
		try
		{
			processHandler = new CmdLineProcessHandler();
	        int exitCode = processHandler.executeCommand(command);

	        if (exitCode != 0) {
	            throw new RemoteExecutionException("Job exited with error code " + exitCode + " please check your arguments and try again.");
	        }
	        
	        processOutput = processHandler.getProcessOutput();
	        
	        return processOutput.getOutString();
		} 
		catch (RemoteExecutionException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RemoteExecutionException("Failed to execute command on " + hostname, e);
		}
	}

	@Override
	public boolean canAuthentication()
	{
		return true;
	}

	@Override
	public String getHost() {
		return hostname;
	}

	@Override
	public int getPort() {
		return -1;
	}
}
