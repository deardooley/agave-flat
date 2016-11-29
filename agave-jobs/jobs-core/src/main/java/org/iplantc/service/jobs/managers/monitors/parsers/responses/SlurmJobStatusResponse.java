/**
 * 
 */
package org.iplantc.service.jobs.managers.monitors.parsers.responses;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitorEmptyResponseException;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitorResponseParsingException;

/**
 * @author dooley
 *
 */
public class SlurmJobStatusResponse extends JobStatusResponse {

	/**
	 * @param jobId
	 * @param status
	 * @param exitCode
	 */
	public SlurmJobStatusResponse(){}

	/**
	 * @param schedulerResponse
	 * @throws RemoteJobMonitorEmptyResponseException
	 * @throws RemoteJobMonitorResponseParsingException
	 */
	public SlurmJobStatusResponse(String schedulerResponse)
			throws RemoteJobMonitorEmptyResponseException,
			RemoteJobMonitorResponseParsingException {
		super(schedulerResponse);
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
	@Override
	public List<String> parseResponse(String schedulerResponse)
	throws RemoteJobMonitorEmptyResponseException, RemoteJobMonitorResponseParsingException 
	{
		if (StringUtils.isBlank(schedulerResponse)) {
			throw new RemoteJobMonitorEmptyResponseException(
					"Empty response received from job status check on the remote system. This is likely caused by a ");
		} else if (!schedulerResponse.contains("|")) {
			throw new RemoteJobMonitorResponseParsingException(
					"Unexpected fields in the response from the scheduler: "
							+ schedulerResponse);
		} else {
			List<String> lines = splitResponseByLine(schedulerResponse);
			
			for (String line : lines) {
				line = StringUtils.removeEnd(line, "|");
				
				List<String> tokens = Arrays.asList(StringUtils
						.splitByWholeSeparatorPreserveAllTokens(line, "|"));

				if (tokens.size() != 3) {
					throw new RemoteJobMonitorResponseParsingException(
							"Unexpected fields in the response from the scheduler: "
									+ schedulerResponse);
				} else {
					return tokens;
				}
			}

			return null;
		}
	}

	/**
	 * Splits the response by newline characters
	 * @param schedulerResponse
	 * @return
	 * @throws RemoteJobMonitorResponseParsingException
	 */
	protected List<String> splitResponseByLine(String schedulerResponse)
	throws RemoteJobMonitorResponseParsingException 
	{
		BufferedReader rdr = null;
		List<String> lines = new ArrayList<String>();
		try {
			rdr = new BufferedReader(new StringReader(schedulerResponse));
			
			for (String line = rdr.readLine(); line != null; line = rdr.readLine()) {
			    lines.add(StringUtils.trimToEmpty(line));
			}
		} 
		catch (Exception e) {
			throw new RemoteJobMonitorResponseParsingException(
					"Unable to parsethe response from the scheduler: "
							+ schedulerResponse);
		}
		finally {
			try {rdr.close();} catch (Throwable t) {}
		}
		
		return lines;
	}

}
