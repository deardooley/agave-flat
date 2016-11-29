package org.iplantc.service.jobs.managers.monitors.parsers.responses;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
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
	 */
	public JobStatusResponse(String schedulerResponse)
			throws RemoteJobMonitorEmptyResponseException,
			RemoteJobMonitorResponseParsingException {
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
	 */
	public abstract List<String> parseResponse(String schedulerResponse)
			throws RemoteJobMonitorEmptyResponseException,
			RemoteJobMonitorResponseParsingException;

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

}