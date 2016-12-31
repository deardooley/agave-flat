package org.iplantc.service.jobs.managers.monitors.parsers;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.jobs.exceptions.RemoteJobFailureDetectedException;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitorEmptyResponseException;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitorResponseParsingException;
import org.iplantc.service.jobs.exceptions.RemoteJobUnknownStateException;
import org.iplantc.service.jobs.exceptions.RemoteJobUnrecoverableStateException;
import org.iplantc.service.jobs.managers.monitors.parsers.PBSHPCMonitorResponseParser.PBSStatusType;
import org.iplantc.service.jobs.managers.monitors.parsers.responses.SlurmJobStatusResponse;

public class SlurmHPCMonitorResponseParser implements JobMonitorResponseParser {
	
	private static final Logger log = Logger
			.getLogger(SlurmHPCMonitorResponseParser.class);
	
	public enum SlurmStatusType {
		
		BOOT_FAIL("BF","Job terminated due to launch failure, typically due to a hardware failure "
				+ "(e.g. unable to boot the node or block and the job can not be requeued)."),
		
		CANCELLED("CA","Job was explicitly cancelled by the user or system administrator. The job may or may not have been initiated."),
		COMPLETED("CD","Job has terminated all processes on all nodes with an exit code of zero."),
		CONFIGURING("CF","Job has been allocated resources, but are waiting for them to become ready for use (e.g. booting)."),
		COMPLETING("CG","Job is in the process of completing. Some processes on some nodes may still be active."),
		DEADLINE("DL","Job missed its deadline."),
		EQW("EQW", "Job started but there was an unrecoverable error. Job will remain in this unrecoverable state until manually cleaned up."),
		FAILED("F","Job terminated with non-zero exit code or other failure condition."),
		NODE_FAIL("NF","Job terminated due to failure of one or more allocated nodes."),
		PENDING("PD","Job is awaiting resource allocation. Note for a job to be selected in this "
				+ "state it must have 'EligibleTime' in the requested time interval or different from "
				+ "'Unknown'. The 'EligibleTime' is displayed by the 'scontrol show job' command. "
				+ "For example jobs submitted with the '--hold' option will have 'EligibleTime=Unknown' "
				+ "as they are pending indefinitely."),
		PREEMPTED("PR","Job terminated due to preemption."),
		RUNNING("R","Job currently has an allocation."),
		RESIZING("RS","Job is about to change size."),
		SUSPENDED("S","Job has an allocation, but execution has been suspended."),
		TIMEOUT("TO","Job terminated upon reaching its time limit.");

		
		private String description;
		private String code; 
		
		private SlurmStatusType(String code, String description) {
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
		 * Returns whether this {@link SlurmStatusType} is active. This includes
		 * suspended jobs and jobs in transition states
		 * @param status
		 * @return false if the status is E or F. True otherwise.
		 * @throws IllegalArgumentException if the {@code satus} value is empty or not a known PBSStatusType enumerated value.
		 */
		public boolean isActiveStatus() {
			boolean activeStatus = false;
			switch (this) {
				case PENDING:
				case CANCELLED:
				case DEADLINE:
				case FAILED:
				case NODE_FAIL:
				case TIMEOUT:
				case COMPLETED:
				case PREEMPTED:
					activeStatus = false;
				break;
				
			default:
				// anything else we assume it's still active.
			}
			return activeStatus;
		}
		
		/**
		 * Parses a job status string from a SLURM sacct line
		 * and returns whether the status is active. This includes
		 * suspended jobs and jobs in transition states
		 * @param status
		 * @return false if the status not in a terminal or failed state. True otherwise.
		 * @throws IllegalArgumentException if the {@code satus} value is empty or not a known PBSStatusType enumerated value.
		 */
		public static boolean isActiveStatus(String status) {
			if (StringUtils.isBlank(status)) {
				return false;
			}
			else {
				SlurmStatusType statusType = SlurmStatusType.valueOf(status.toUpperCase());
				return statusType.isActiveStatus();
			}
		}
		
		/**
		 * Returns whether this {@link SlurmStatusType} is a failure state. 
		 * This includes suspended jobs and jobs in transition states
		 * @param status
		 * @return false if the status is E or F. True otherwise.
		 * @throws IllegalArgumentException if the {@code status} value is empty or not a known PBSStatusType enumerated value.
		 */
		public boolean isFailureStatus() {
			boolean hasfailedStatus = false;
			switch (this) {
				case BOOT_FAIL:
				case CANCELLED:
				case DEADLINE:
				case FAILED:
				case NODE_FAIL:
				case TIMEOUT:
				case PREEMPTED:
					hasfailedStatus = true;
				break;
			default:
				// anything else we assume it's still active.
			}
			return hasfailedStatus;
		}
		
		/**
		 * Returns whether this {@link SlurmStatusType} is a paused state. 
		 * This includes suspended jobs.
		 * @param status
		 * @return false if the status is E or F. True otherwise.
		 * @throws IllegalArgumentException if the {@code status} value is empty or not a known PBSStatusType enumerated value.
		 */
		public boolean isPausedStatus() {
			return this == SUSPENDED;
		}
		
		/**
		 * Parses a job status string from a SLURM sacct line
		 * and returns whether the status is a failed, but not completed
		 * status (ie. not COMPLETED or PENDING)
		 * @param status
		 * @return false if the status is an unsuccessful terminal state. True otherwise.
		 * @throws IllegalArgumentException if the {@code satus} value is empty or not a known PBSStatusType enumerated value.
		 */
		public static boolean isFailureStatus(String status) {
			if (StringUtils.isBlank(status)) {
				return false;
			}
			else {
				SlurmStatusType statusType = SlurmStatusType.valueOf(status.toUpperCase());
				return statusType.isFailureStatus();
			}
		}	
	}
	
	@Override
	public boolean isJobRunning(String remoteServerRawResponse) 
	throws RemoteJobMonitorEmptyResponseException, RemoteJobMonitorResponseParsingException, RemoteJobUnrecoverableStateException
	{
		SlurmJobStatusResponse statusResponse = new SlurmJobStatusResponse(remoteServerRawResponse);
		
		try {
			// if the state info is missing, job isn't running
			if (StringUtils.isBlank(statusResponse.getStatus())) {
				return false;
			}
			else {
				SlurmStatusType statusType = SlurmStatusType.valueOf(statusResponse.getStatus().toUpperCase());
				
				// if the job is in an unrecoverable state, throw the exception so the job is cleaned up
				if (statusType == SlurmStatusType.EQW) {
					throw new RemoteJobUnrecoverableStateException(statusType.getDescription());
				}
				// is it in a known active state?
				else if (SlurmStatusType.isActiveStatus(statusResponse.getStatus())) {
					// raise a RemoteJobUnknownStateException exception here because the job 
					// will remain in that state without being killed by our monitors.
					if (statusType == SlurmStatusType.SUSPENDED) {
						throw new RemoteJobUnknownStateException(statusResponse.getStatus(), statusType.getDescription());
					}
					else {
						return true;
					}
				}
				// if it's explicitly failed due to a runtime issue, denote that and throw an exception
				else if (statusType == SlurmStatusType.FAILED) {
					throw new RemoteJobFailureDetectedException(statusType.getDescription() + 
							". Exit code was " + statusResponse.getExitCode());
				}
				else if (SlurmStatusType.isFailureStatus(statusResponse.getStatus())) {
					throw new RemoteJobFailureDetectedException(statusType.getDescription());
				}
				else {
					return false;
				}	
			}
		}
		catch (IllegalArgumentException e) {
			throw new RemoteJobUnknownStateException(statusResponse.getStatus(), "Detected job in an unknown state ");
		}
//		
//		
//		
//		else if (StringUtils.equalsIgnoreCase(statusResponse.getStatus(), "completed")) {
//			return false;
//		}
//		else if (StringUtils.equalsIgnoreCase(statusResponse.getStatus(), "timeout")) {
//			return false;
//		}
//		else if (StringUtils.equalsIgnoreCase(statusResponse.getStatus(), "timeout")) {
//			return false;
//		}
//		else if (StringUtils.equalsIgnoreCase(statusResponse.getStatus(), "failed")) {
//			throw new RemoteJobFailureDetectedException("Exit code was " + statusResponse.getExitCode());
//		}
//		else if (StringUtils.equalsIgnoreCase(statusResponse.getStatus(), "resizing") ||
//				StringUtils.equalsIgnoreCase(statusResponse.getStatus(), "running") || 
//				StringUtils.equalsIgnoreCase(statusResponse.getStatus(), "pending")) {
//			return true;
//		}
//		else {
//			throw new RemoteJobUnknownStateException(statusResponse.getStatus(), "Detected job in an unknown state ");
//		}
	}
}
