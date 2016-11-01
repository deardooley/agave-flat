package org.iplantc.service.jobs.managers.monitors.parsers;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.jobs.exceptions.RemoteJobFailureDetectedException;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitorEmptyResponseException;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitorResponseParsingException;
import org.iplantc.service.jobs.exceptions.RemoteJobUnknownStateException;
import org.iplantc.service.jobs.exceptions.RemoteJobUnrecoverableStateException;

public class SlurmHPCMonitorResponseParser implements JobMonitorResponseParser {
	
	private static final Logger log = Logger
			.getLogger(SlurmHPCMonitorResponseParser.class);
	
	@Override
	public boolean isJobRunning(String result) throws RemoteJobMonitorEmptyResponseException, RemoteJobMonitorResponseParsingException, RemoteJobUnrecoverableStateException
	{
		// The request made was for {@code sacct -p -o 'JOBID,State,ExitCode' -n -j <job_id>}. That means 
		// the response should come back in a pipe-delimited string as {@code <job_id>|<state>|<exit_code>|}.
		// It is sufficient to split the string and look at the second term
		if (StringUtils.isEmpty(result)) {
			throw new RemoteJobMonitorEmptyResponseException("Empty response received from job status check on the remote system. This is likely caused by a ");
		}
		else if (!result.contains("|")) {
			throw new RemoteJobMonitorResponseParsingException("Unexpected fields in the response from the scheduler: " + result);
		}
		else {
			List<String> lines = Arrays.asList(StringUtils.stripToEmpty(result).split("[\\r\\n]+"));
			
			
			for (String line: lines) {
				List<String> tokens = Arrays.asList(StringUtils.split(line, "|"));
				
				if (tokens.size() != 3) {
					throw new RemoteJobMonitorResponseParsingException("Unexpected fields in the response from the scheduler: " + result);
				}
				// if the state info is missing, job isn't running
				else if (StringUtils.isEmpty(tokens.get(1))) {
					return false;
				}
				else if (tokens.get(1).toLowerCase().contains("eqw")) {
					throw new RemoteJobUnrecoverableStateException();
				}
				else if (StringUtils.equalsIgnoreCase(tokens.get(1), "completed")) {
					return false;
				}
				else if (StringUtils.equalsIgnoreCase(tokens.get(1), "failed")) {
					throw new RemoteJobFailureDetectedException("Exit code was " + tokens.get(2));
				}
				else if (StringUtils.equalsIgnoreCase(tokens.get(1), "resizing") ||
						StringUtils.equalsIgnoreCase(tokens.get(1), "running") || 
						StringUtils.equalsIgnoreCase(tokens.get(1), "pending")) {
					return true;
				}
				else {
					throw new RemoteJobUnknownStateException(tokens.get(1), "Detected job in an unknown state ");
				}
			}
			
			return false;
		}
	}
}
