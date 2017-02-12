package org.iplantc.service.jobs.model.enumerations;

public enum JobStatusType
{
	PAUSED("Job execution paused by user"), 
	
	PENDING("Job accepted and queued for submission."), 
	PROCESSING_INPUTS("Identifying input files for staging"), 
	STAGING_INPUTS("Transferring job input data to execution system"), 
	STAGED("Job inputs staged to execution system"), 
	
	STAGING_JOB("Staging runtime assets to execution system"), 
	SUBMITTING("Preparing job for execution and staging assets to execution system"), 
	QUEUED("Job successfully placed into queue"), 
	RUNNING("Job started running"), 
	
	CLEANING_UP("Job completed execution"), 
	ARCHIVING("Transferring job output to archive system"), 
	ARCHIVING_FINISHED("Job archiving complete"), 
	ARCHIVING_FAILED("Job archiving failed"),
	
	FINISHED("Job complete"), 
	KILLED("Job execution killed at user request"), 
	STOPPED("Job execution intentionally stopped"), 
	FAILED("Job failed"), 
	
	HEARTBEAT("Job heartbeat received"),
    ROLLINGBACK("Rolling job back to a prior state");
	
	private final String description;
	
	JobStatusType(String description) {
		this.description = description;
	}
	
	public String getDescription() {
		return description;
	}
	
	public static boolean isRunning(JobStatusType status)
	{

		return ( status.equals(PENDING) || status.equals(STAGING_INPUTS)
				|| status.equals(STAGING_JOB) || status.equals(RUNNING)
				|| status.equals(PAUSED) || status.equals(QUEUED)
				|| status.equals(SUBMITTING)
				|| status.equals(PROCESSING_INPUTS) || status.equals(STAGED) 
				|| status.equals(CLEANING_UP) );
	}

	public static boolean isSubmitting(JobStatusType status)
	{
		return ( status.equals(STAGING_JOB) || 
				status.equals(SUBMITTING) || 
				status.equals(STAGED) );
	}
	
	public static boolean hasQueued(JobStatusType status)
	{
		return !( status.equals(PENDING) 
				|| status.equals(STAGING_INPUTS)
				|| status.equals(STAGING_JOB)  
				|| status.equals(SUBMITTING)  
				|| status.equals(STAGED) 
				|| status.equals(STAGING_JOB));
	}

	public static boolean isFinished(JobStatusType status)
	{
		return ( status.equals(FINISHED) || status.equals(KILLED)
				|| status.equals(FAILED) || status.equals(STOPPED));
	}
	
	public static boolean isArchived(JobStatusType status) 
	{
		 return (status.equals(ARCHIVING_FAILED) 
				 || status.equals(ARCHIVING_FINISHED));
	}
		
	public static boolean isFailed(JobStatusType status) 
	{
		 return (status.equals(ARCHIVING_FAILED) 
				|| status.equals(FAILED)
		 		|| status.equals(KILLED));
	}
	
	public static boolean isExecuting(JobStatusType status)
	{
		return ( status.equals(RUNNING) || 
				status.equals(PAUSED) || 
				status.equals(QUEUED) );
	}

	public static JobStatusType[] getActiveStatuses()
	{
		return new JobStatusType[]{ CLEANING_UP, ARCHIVING, RUNNING, PAUSED, QUEUED };
	}
	
	public static String getActiveStatusValues()
	{
		return String.format("'%s','%s','%s','%s','%s'",
				CLEANING_UP.name(), ARCHIVING.name(), RUNNING.name(), PAUSED.name(), QUEUED.name());
	}
	
	/**
	 * Returns the previous state in the lifecycle where 
	 * the job must be transformed. This essentially maps 
	 * the current job state to the state needed to place 
	 * it in the previous processing queue.
	 *  
	 * @return 
	 */
	public JobStatusType rollbackState()
	{
		// Begin again with staging inputs
		if (this == PENDING || this == PROCESSING_INPUTS || 
	        this == STAGING_INPUTS || this == STAGED) 
		{
			return PENDING;
		} 
		// Resubmit from current state
		else if (this == STAGING_JOB || this == SUBMITTING || 
				 this == QUEUED || this == RUNNING || 
				 this == CLEANING_UP)
		{
			return STAGED;
		} 
		// Rerun archiving tasks
		else if (this == ARCHIVING || this == ARCHIVING_FAILED || 
				 this == ARCHIVING_FINISHED) 
		{
			return CLEANING_UP;
		} 
		// Just rerun from the beginning
		// PAUSED, KILLED, STOPPED, FINISHED, FAILED
		else { 
			return PENDING;
		}
	}
	
	@Override
	public String toString() {
		return name();
	}
}