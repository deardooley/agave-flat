package org.iplantc.service.jobs.statemachine;

/** The events recognized by the Jobs FSM.
 * 
 * @author rcardone
 */
public enum JobFSMEvents 
{
    // Events with the format T0_<status> 
    // request a change to the target status
    TO_PAUSED,
    
    TO_PENDING,
    TO_PROCESSING_INPUTS,
    TO_STAGING_INPUTS,
    TO_STAGED,
    
    TO_STAGING_JOB,
    TO_SUBMITTING,
    TO_QUEUED,
    TO_RUNNING,
    
    TO_CLEANING_UP,
    TO_ARCHIVING,
    TO_ARCHIVING_FINISHED,
    TO_ARCHIVING_FAILED,
    
    TO_FINISHED,
    TO_KILLED,
    TO_STOPPED,
    TO_FAILED,
    
    TO_ROLLINGBACK
}
