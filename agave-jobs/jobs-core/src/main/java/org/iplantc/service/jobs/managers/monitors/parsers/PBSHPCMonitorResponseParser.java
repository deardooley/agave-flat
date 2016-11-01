package org.iplantc.service.jobs.managers.monitors.parsers;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitorResponseParsingException;
import org.iplantc.service.jobs.exceptions.RemoteJobUnrecoverableStateException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.joda.time.DateTime;

public class PBSHPCMonitorResponseParser implements JobMonitorResponseParser {
	
	private static final Logger log = Logger
			.getLogger(PBSHPCMonitorResponseParser.class);
	
	public enum PBSStatusType {
		B("Array job has at least one subjob running."),
        E("Job is exiting after having run."),
        F("Job is finished."),
        H("Job is held."),
        M("Job was moved to another server."),
        Q("Job is queued."),
        R("Job is running."),
        S("Job is suspended."),
        T("Job is being moved to new location."),
        U("Cycle-harvesting job is suspended due to keyboard activity."),
        W("Job is waiting for its submitter-assigned start time to be reached.");
		
		private String description;
		
		private PBSStatusType(String description) {
			this.setDescription(description);
		}
		/**
		 * @return the description
		 */
		public String getDescription() {
			return description;
		}
		/**
		 * @param description the description to set
		 */
		public void setDescription(String description) {
			this.description = description;
		}
		
		/**
		 * Parses a single character job status from a PBS qstat line
		 * and returns whether the status is active (ie. not E or F)
		 * @param status
		 * @return false if the status is E or F. True otherwise.
		 * @throws IllegalArgumentException if the {@code satus} value is empty or not a known PBSStatusType enumerated value.
		 */
		public static boolean isActiveStatus(String status) {
			if (StringUtils.isEmpty(status)) {
				return false;
			}
			else {
				PBSStatusType statusType = PBSStatusType.valueOf(status.toUpperCase());
				if (statusType == E || statusType == F) {
					return false;
				}
				else {
					return true;
				}
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.managers.monitors.parsers.JobMonitorResponseParser#isJobRunning(java.lang.String)
	 */
	@Override
	public boolean isJobRunning(String result) throws RemoteJobMonitorResponseParsingException, RemoteJobUnrecoverableStateException
	{
		if (StringUtils.isEmpty(result)) {
			return false;
//			throw new RemoteJobMonitorEmptyResponseException("Empty response received from job status check on the remote system");
		} 
		else if (result.toLowerCase().contains("unknown")
				|| result.toLowerCase().contains("error") 
				|| result.toLowerCase().contains("not ")) {
			return false;
		}
		else {
			
			List<String> lines = Arrays.asList(StringUtils.stripToEmpty(result).split("[\\r\\n]+"));
			
			for (String line: lines) {
				List<String> tokens = Arrays.asList(StringUtils.split(line));
				
				// output from {@code qstat -w | grep <job_id> } should be similar to 
				// <pre>Job ID                         Username        Queue           Jobname         SessID   NDS  TSK   Memory Time  S Time</pre>
				if (tokens.size() == 11) {
					try {
						return PBSStatusType.isActiveStatus(tokens.get(9)); 
					}
					catch (Throwable e) {
						throw new RemoteJobMonitorResponseParsingException("Unexpected fiels in the response from the scheduler: " + result);
					}
				}
				// in case the response is customized, we check for the next to last token, which 
				// is the standard location
				else {
					for (int i=tokens.size()-1; i >= 0; i--) {
						if (tokens.get(i).matches("^([a-zA-Z]{1})$")) {
							try {
								return PBSStatusType.isActiveStatus(tokens.get(9)); 
							}
							catch (Throwable e) {
								throw new RemoteJobMonitorResponseParsingException("Unexpected fiels in the response from the scheduler: " + result);
							}
						}
					}
					throw new RemoteJobMonitorResponseParsingException("Unable to obtain job status in the response from the scheduler: " + result);
				}
			}
			
			return false;
		}
		
//		throw new RemoteJobMonitorResponseParsingException("No response from server when checking for process id");
	}

}
