/**
 * 
 */
package org.iplantc.service.jobs.model.enumerations;

/**
 * @author dooley
 *
 */
public enum JobEventType {
	PERMISSION_GRANT("Permission was added or updated"),
	PERMISSION_REVOKE("Permission was removed"),
	
	PAUSED("Job execution paused by user"), 
	
	PENDING("Job accepted and queued for submission."), 
	PROCESSING_INPUTS("Identifying input files for staging"), 
	STAGING_INPUTS("Transferring job input data to execution system"), 
	STAGED("Job inputs staged to execution system"), 
	
	STAGING_JOB("Staging runtime assets to execution system"), 
	SUBMITTING("Preparing job for execution and staging assets to execution system"), 
	QUEUED("Job successfully placed into queue"), 
	RUNNING("Job started running"), 
	
	EMPTY_STATUS_RESPONSE("An empty response was received from the remote execution system when querying for job status"),
	REMOTE_STATUS_CHANGE("The status of the job on the remote system was changed by an external process."),
	UNKNOWN_TERMINATION("The job experienced an unknown termination event and is no longer running on the remote system."),
	
	CLEANING_UP("Job completed execution"), 
	ARCHIVING("Transferring job output to archive system"), 
	ARCHIVING_FINISHED("Job archiving complete"), 
	ARCHIVING_FAILED("Job archiving failed"),
	
	FINISHED("Job complete"), 
	KILLED("Job execution killed at user request"), 
	STOPPED("Job execution intentionally stopped"), 
	FAILED("Job failed"), 
	
	HEARTBEAT("Job heartbeat received");
	
	private String description;
	
	private JobEventType(String description) {
		setDescription(description);
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
	private void setDescription(String description) {
		this.description = description;
	}
}
