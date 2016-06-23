package org.iplantc.service.jobs.model.enumerations;

import static org.iplantc.service.jobs.model.enumerations.JobStatusType.PROCESSING_INPUTS;

import java.util.Arrays;

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
	
	HEARTBEAT("Job heartbeat received");
	
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
	 * The job state that logically comes directly before the 
	 * current state. Some states such as {@link PAUSED}, 
	 * {@link KILLED}, {@link STOPPED}, {@link FINISHED}, and
	 * {@link FAILED} do not have a well-defined predecessor
	 * so they fall back to {@link PENDING}.
	 * 
	 * Note that this method does not pull from the actual 
	 * job history, but rather represents the backward
	 * progression of a job in an ideal situation.
	 *   
	 * @return job state logically before this one.
	 */
	public JobStatusType previousState()
	{
		if (this == PENDING || this == PROCESSING_INPUTS) {
			return PENDING;
		} else if (this == STAGING_INPUTS) {
			return PROCESSING_INPUTS; 
		} else if (this == STAGED) {
			return STAGING_INPUTS;
		} else if (this == STAGING_JOB) {
			return STAGED;
		} else if (this == SUBMITTING) {
			return STAGING_JOB;
		} else if (this == QUEUED) {
			return SUBMITTING;
		} else if (this == RUNNING) {
			return QUEUED;
		} else if (this == CLEANING_UP) {
			return RUNNING;
		} else if (this == ARCHIVING) {
			return CLEANING_UP;
		} else if (this == ARCHIVING_FAILED || this == ARCHIVING_FINISHED) {
			return ARCHIVING;
		} else { // PAUSED, KILLED, STOPPED, FINISHED, FAILED
			return PENDING;
		}
	}
	
	/**
	 * Returns an array of the valid state transitions from the current one.
	 * @return array of valid states or an empty set if no valid states can be reached from the current one.
	 */
	public JobStatusType[] getNextValidStates()
    {
        if (this == PENDING) {
            return new JobStatusType[] {PENDING, PROCESSING_INPUTS, STOPPED, PAUSED, FAILED};
        } 
        else if (this == PROCESSING_INPUTS) {
            return new JobStatusType[] {PROCESSING_INPUTS, STAGING_INPUTS, STOPPED, PAUSED, FAILED};
        } 
        else if (this == STAGING_INPUTS) {
            return new JobStatusType[] {STAGING_INPUTS, STAGED, STOPPED, PAUSED, FAILED}; 
        } 
        else if (this == STAGED) {
            return new JobStatusType[] {STAGED, SUBMITTING, STOPPED, PAUSED, FAILED};
        } 
        else if (this == STAGING_JOB) {
            return new JobStatusType[] {STAGING_JOB, SUBMITTING, STOPPED, PAUSED, FAILED};
        } 
        else if (this == SUBMITTING) {
            return new JobStatusType[] {SUBMITTING, QUEUED, RUNNING, KILLED, STOPPED, PAUSED, FAILED, HEARTBEAT};
        } 
        else if (this == QUEUED) {
            return new JobStatusType[] {QUEUED, RUNNING, CLEANING_UP, KILLED, STOPPED, PAUSED, FAILED, HEARTBEAT};
        } 
        else if (this == RUNNING) {
            return new JobStatusType[] {RUNNING, CLEANING_UP, KILLED, STOPPED, PAUSED, FAILED, HEARTBEAT};
        } 
        else if (this == CLEANING_UP) {
            return new JobStatusType[] {CLEANING_UP, ARCHIVING, FINISHED, KILLED, STOPPED, PAUSED, FAILED};
        } 
        else if (this == ARCHIVING) {
            return new JobStatusType[] {ARCHIVING, ARCHIVING_FAILED, ARCHIVING_FINISHED, KILLED, STOPPED, PAUSED, FAILED};
        } 
        else if (this == ARCHIVING_FAILED) {
            return new JobStatusType[] {ARCHIVING_FAILED, FAILED};
        } 
        else if (this == ARCHIVING_FINISHED) {
            return new JobStatusType[] {ARCHIVING_FINISHED, FAILED, FINISHED};
        } 
        else if (this == PAUSED) {
            return JobStatusType.values();
        } 
        // dummy value that never actually gets saved.
        else if (this == HEARTBEAT) { 
            return JobStatusType.values();
        } 
        //  KILLED, STOPPED, FINISHED, FAILED
        else {
            return new JobStatusType[] {};
        }
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