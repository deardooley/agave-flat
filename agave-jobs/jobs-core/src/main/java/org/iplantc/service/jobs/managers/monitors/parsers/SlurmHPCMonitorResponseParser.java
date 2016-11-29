package org.iplantc.service.jobs.managers.monitors.parsers;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.jobs.exceptions.RemoteJobFailureDetectedException;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitorEmptyResponseException;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitorResponseParsingException;
import org.iplantc.service.jobs.exceptions.RemoteJobUnknownStateException;
import org.iplantc.service.jobs.exceptions.RemoteJobUnrecoverableStateException;
import org.iplantc.service.jobs.managers.monitors.parsers.responses.SlurmJobStatusResponse;

public class SlurmHPCMonitorResponseParser implements JobMonitorResponseParser {
	
	private static final Logger log = Logger
			.getLogger(SlurmHPCMonitorResponseParser.class);
	
	@Override
	public boolean isJobRunning(String remoteServerRawResponse) 
	throws RemoteJobMonitorEmptyResponseException, RemoteJobMonitorResponseParsingException, RemoteJobUnrecoverableStateException
	{
		SlurmJobStatusResponse statusResponse = new SlurmJobStatusResponse(remoteServerRawResponse);
		
		// if the state info is missing, job isn't running
		if (StringUtils.isBlank(statusResponse.getStatus())) {
			return false;
		}
		else if (statusResponse.getStatus().toLowerCase().contains("eqw")) {
			throw new RemoteJobUnrecoverableStateException();
		}
		else if (StringUtils.equalsIgnoreCase(statusResponse.getStatus(), "completed")) {
			return false;
		}
		else if (StringUtils.equalsIgnoreCase(statusResponse.getStatus(), "failed")) {
			throw new RemoteJobFailureDetectedException("Exit code was " + statusResponse.getExitCode());
		}
		else if (StringUtils.equalsIgnoreCase(statusResponse.getStatus(), "resizing") ||
				StringUtils.equalsIgnoreCase(statusResponse.getStatus(), "running") || 
				StringUtils.equalsIgnoreCase(statusResponse.getStatus(), "pending")) {
			return true;
		}
		else {
			throw new RemoteJobUnknownStateException(statusResponse.getStatus(), "Detected job in an unknown state ");
		}
	}
}
