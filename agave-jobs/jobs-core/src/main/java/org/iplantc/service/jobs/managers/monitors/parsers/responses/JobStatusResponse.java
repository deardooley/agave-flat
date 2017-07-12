package org.iplantc.service.jobs.managers.monitors.parsers.responses;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitorCommandSyntaxException;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitorEmptyResponseException;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitorResponseParsingException;

/**
 * Bean to structure monitoring results from remote schedulers.
 * @author dooley
 *
 */
public abstract class JobStatusResponse {
	protected String jobId;
	protected String status;
	protected String exitCode;

	public JobStatusResponse(){}

	/**
	 * Parses a remote command 
	 * @param schedulerResponse
	 * @throws RemoteJobMonitorEmptyResponseException
	 * @throws RemoteJobMonitorResponseParsingException
	 * @throws RemoteJobMonitorCommandSyntaxException 
	 */
	public JobStatusResponse(String schedulerResponse)
	throws RemoteJobMonitorEmptyResponseException, RemoteJobMonitorResponseParsingException, 
			RemoteJobMonitorCommandSyntaxException 
	{
		List<String> tokens = parseResponse(schedulerResponse);
		setJobId(tokens.get(0));
		setStatus(tokens.get(1));
		setExitCode(tokens.get(2));
	}

	/**
	 * The request made was for
	 * {@code sacct -p -o 'JOBID,State,ExitCode' -n -j <job_id>}. That means the
	 * response should come back in a pipe-delimited string as
	 * {@code <job_id>|<state>|<exit_code>|}. It is sufficient to split the
	 * string and look at the second term
	 * 
	 * @param schedulerResponse
	 *            the raw response from the scheduler. Should be in
	 *            {@code <job_id>|<state>|<exit_code>|} format.
	 * @return list of tokens in the response.
	 * @throws RemoteJobMonitorResponseParsingException
	 * @throws RemoteJobMonitorEmptyResponseException
	 * @throws RemoteJobMonitorCommandSyntaxException 
	 */
	public abstract List<String> parseResponse(String schedulerResponse)
	throws RemoteJobMonitorEmptyResponseException,
			RemoteJobMonitorResponseParsingException, RemoteJobMonitorCommandSyntaxException;

	/**
	 * @return the jobId
	 */
	public String getJobId() {
		return jobId;
	}

	/**
	 * @param jobId
	 *            the jobId to set
	 */
	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @param status
	 *            the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * @return the exitCode
	 */
	public String getExitCode() {
		return exitCode;
	}

	/**
	 * @param exitCode
	 *            the exitCode to set
	 */
	public void setExitCode(String exitCode) {
		this.exitCode = exitCode;
	}
	
	/**
	 * @return true if the response was blank, ie. null, empty, or only spaces
	 */
	public boolean isBlankResponse() {
		return StringUtils.isBlank(getStatus());
	}
	
	/**
	 * Checks the value of {@link #exitCode} for a success status. Depending
	 * on the scheduler, this could be a delimited value representing the
	 * value of the current and parent task (ie. 0.0), or a simple integer 
	 * value from a process exit code.
	 * 
	 * @return true if the remote process exited with code 0, 1 otherwise
	 */
	public boolean isSuccessExitCode() {
		return (NumberUtils.isNumber(getExitCode()) && 
				NumberUtils.toInt(getExitCode()) == 0);
	}
}