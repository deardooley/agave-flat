package org.iplantc.service.jobs.managers.monitors.parsers;

import org.iplantc.service.jobs.exceptions.RemoteJobMonitorCommandSyntaxException;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitorEmptyResponseException;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitorResponseParsingException;
import org.iplantc.service.jobs.exceptions.RemoteJobUnrecoverableStateException;

public interface JobMonitorResponseParser {

	/**
	 * Parses response from a remote job status check command and 
	 * determines whether it is running or otherwise.
	 * @param output
	 * @return
	 * @throws RemoteJobMonitorResponseParsingException if the response cannot be parsed or contains invalid formatting
	 * @throws RemoteJobUnrecoverableStateException  if the job is found in a terminal state from which recovery is not possible
	 * @throws RemoteJobMonitorEmptyResponseException if the remote scheduler returns no response
	 * @throws RemoteJobMonitorCommandSyntaxException if the remote scheduler complains of usage or syntax issues
	 */
	public boolean isJobRunning(String output)
			throws RemoteJobMonitorResponseParsingException, RemoteJobUnrecoverableStateException, 
			RemoteJobMonitorEmptyResponseException, RemoteJobMonitorCommandSyntaxException;

}
