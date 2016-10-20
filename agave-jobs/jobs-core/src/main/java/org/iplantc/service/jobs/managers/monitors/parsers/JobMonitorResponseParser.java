package org.iplantc.service.jobs.managers.monitors.parsers;

import org.iplantc.service.jobs.exceptions.RemoteJobMonitorResponseParsingException;

public interface JobMonitorResponseParser {

	/**
	 * Parses response from a remote job status check command and 
	 * determines whether it is running or otherwise.
	 * @param output
	 * @return
	 * @throws RemoteJobMonitorResponseParsingException
	 */
	public boolean isJobRunning(String output)
			throws RemoteJobMonitorResponseParsingException;

}
