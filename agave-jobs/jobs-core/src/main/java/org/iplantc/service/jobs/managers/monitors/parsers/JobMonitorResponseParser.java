package org.iplantc.service.jobs.managers.monitors.parsers;

import org.iplantc.service.jobs.exceptions.RemoteJobMonitorEmptyResponseException;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitorResponseParsingException;
import org.iplantc.service.jobs.exceptions.RemoteJobUnrecoverableStateException;

public interface JobMonitorResponseParser {

	/**
	 * Parses response from a remote job status check command and 
	 * determines whether it is running or otherwise.
	 * @param output
	 * @return
	 * @throws RemoteJobMonitorResponseParsingException
	 * @throws RemoteJobUnrecoverableStateException 
	 * @throws RemoteJobMonitorEmptyResponseException 
	 */
	public boolean isJobRunning(String output)
			throws RemoteJobMonitorResponseParsingException, RemoteJobUnrecoverableStateException, RemoteJobMonitorEmptyResponseException;

}
