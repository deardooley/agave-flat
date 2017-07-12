package org.iplantc.service.jobs.managers.monitors.parsers;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitorCommandSyntaxException;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitorResponseParsingException;
import org.iplantc.service.jobs.exceptions.RemoteJobUnrecoverableStateException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.joda.time.DateTime;

public class GridEngineMonitorResponseParser implements JobMonitorResponseParser {
	
	private static final Logger log = Logger
			.getLogger(GridEngineMonitorResponseParser.class);
	
	public enum GridEngineStatusType {
		DELETED("d", "Job has been manually deleted by the qdel command."),  
		ERROR("E", "Job could not be started due to one or more job properties."),
        HOLD("h", "Job is either not currently eligible for execution due to a hold "
    		+ "state assigned to it via qhold, qalter, or the qsub -h option, "
    		+ "or the job is waiting for completion of the jobs to which job "
    		+ "dependencies have been assigned to the job via the -hold_jid "
    		+ "or -hold_jid-ad options of qsub or qalter."), 
        RUNNING("r","Job is about to be executed or is already executing."), 
        RESTARTED("R", "Job has been restarted. This can be caused by a job migration or "
    		+ "because of one of the reasons described in the -r section of "
    		+ "the qsub command."), 
        SUSPENDED_USER("s","Job was running, but has been manually suspended by the qmod command."), 
        SUSPENDED_HOST("S", "Job was running, but is currently suspended because the queue containing the job is suspended."),
        TRANSFERING("t", "Job is being transferred to the compute nodes for execution."), 
        THRESHOLD("T", "Job exceeded at least one suspend threshold of the corresponding queue. The job has been suspended as a consequence"),
        WAITING("w", "Job is waiting for a slot to "),
        EQW("eqw", "Job started but there was an unrecoverable error. Job will remain in this unrecoverable state until manually cleaned up.");
		
        private String description;
		private String code; 
		
		private GridEngineStatusType(String code, String description) {
			this.setCode(code);
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
		 * @return the code
		 */
		public String getCode() {
			return code;
		}
		
		/**
		 * @param code the code to set
		 */
		public void setCode(String code) {
			this.code = code;
		}
		
		@Override
		public String toString() {
			return name() + " - " + getDescription();
		}
		
		/**
		 * Returns whether this {@link GridEngineStatusType} is active. This includes
		 * suspended jobs and jobs in transition states
		 * @param status
		 * @return false if the status is E or F. True otherwise.
		 * @throws IllegalArgumentException if the {@code satus} value is empty or not a known GridEngineStatusType enumerated value.
		 */
		public boolean isActiveStatus() {
			boolean activeStatus = false;
			switch (this) {
				case RUNNING:
				case RESTARTED:
				case WAITING:
				case TRANSFERING:
					activeStatus = true;
				break;
				
			default:
				// anything else we assume it's still active.
			}
			return activeStatus;
		}
		
		/**
		 * Parses a job status string from a GridEngine sacct line
		 * and returns whether the status is active. This includes
		 * suspended jobs and jobs in transition states
		 * @param status
		 * @return false if the status not in a terminal or failed state. True otherwise.
		 * @throws IllegalArgumentException if the {@code satus} value is empty or not a known GridEngineStatusType enumerated value.
		 */
		public static boolean isActiveStatus(String status) {
			if (StringUtils.isBlank(status)) {
				return false;
			}
			else {
				GridEngineStatusType statusType = GridEngineStatusType.valueOf(status.toUpperCase());
				return statusType.isActiveStatus();
			}
		}
		
		/**
		 * Returns whether this {@link GridEngineStatusType} is a failure state. 
		 * This includes suspended jobs and jobs in transition states
		 * @param status
		 * @return false if the status is E,d, or eqw. True otherwise.
		 * @throws IllegalArgumentException if the {@code status} value is empty or not a known GridEngineStatusType enumerated value.
		 */
		public boolean isFailureStatus() {
			boolean hasfailedStatus = false;
			switch (this) {
				case ERROR:
				case DELETED:
				case EQW:
					hasfailedStatus = true;
				break;
			default:
				// anything else we assume it's still active.
			}
			return hasfailedStatus;
		}
		
		/**
		 * Returns whether this {@link GridEngineStatusType} is a paused state. 
		 * This includes suspended jobs.
		 * @param status
		 * @return false if the status is E or F. True otherwise.
		 * @throws IllegalArgumentException if the {@code status} value is empty or not a known GridEngineStatusType enumerated value.
		 */
		public boolean isPausedStatus() {
			boolean hasPausedStatus = false;
			switch (this) {
				case THRESHOLD:
				case SUSPENDED_USER:
				case SUSPENDED_HOST:
				case HOLD:
					hasPausedStatus = true;
				break;
			default:
				// anything else we assume it's still active or dead.
			}
			return hasPausedStatus;
		}
		
		/**
		 * Parses a job status string from a GridEngine sacct line
		 * and returns whether the status is a failed, but not completed
		 * status (ie. not COMPLETED or PENDING)
		 * @param status
		 * @return false if the status is an unsuccessful terminal state. True otherwise.
		 * @throws IllegalArgumentException if the {@code satus} value is empty or not a known GridEngineStatusType enumerated value.
		 */
		public static boolean isFailureStatus(String status) {
			if (StringUtils.isBlank(status)) {
				return false;
			}
			else {
				GridEngineStatusType statusType = GridEngineStatusType.valueOf(status.toUpperCase());
				return statusType.isFailureStatus();
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.managers.monitors.parsers.JobMonitorResponseParser#isJobRunning(java.lang.String)
	 */
	@Override
	public boolean isJobRunning(String result) throws RemoteJobMonitorResponseParsingException, RemoteJobUnrecoverableStateException, RemoteJobMonitorCommandSyntaxException
	{
		Pattern invalidUsageResponse = Pattern.compile("^usage:\\s+");
		
		if (StringUtils.isEmpty(result)) {
			return false;
		}
		// Check for the beginning of usage info or invalid command syntax.  
		// This indicates an invalid command rather than a missing job
		else if (invalidUsageResponse.matcher(result).find() ||
				result.toLowerCase().contains("invalid option argument")) {
			throw new RemoteJobMonitorCommandSyntaxException("Unable to obtain job status in the response from the scheduler: " + result);
		}
		else if (result.toLowerCase().contains("unknown")
				|| result.toLowerCase().contains("error") 
				|| result.toLowerCase().contains("not ")) {
			return false;
		}
		else {
			
			List<String> lines = Arrays.asList(StringUtils.stripToEmpty(result).split("[\\r\\n]+"));
			
			for (String line: lines) {
				
				// check the response for the job id  
				List<String> tokens = Arrays.asList(StringUtils.split(line));
				
				// output from {@code qstat -w | grep <job_id> } should be similar to 
				// <pre>Job ID                         Username        Queue           Jobname         SessID   NDS  TSK   Memory Time  S Time</pre>
				if (tokens.size() == 11) {
					try {
						return GridEngineStatusType.isActiveStatus(tokens.get(9)); 
					}
					catch (Throwable e) {
						throw new RemoteJobMonitorResponseParsingException("Unexpected fields in the response from the scheduler: " + result);
					}
				}
				// in case the response is customized, we check for the next to last token, which 
				// is the standard location
				else {
					for (int i=tokens.size()-1; i >= 0; i--) {
						if (tokens.get(i).matches("^([a-zA-Z]{1})$")) {
							try {
								return GridEngineStatusType.isActiveStatus(tokens.get(9)); 
							}
							catch (Throwable e) {
								throw new RemoteJobMonitorResponseParsingException("Unexpected fields in the response from the scheduler: " + result);
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
