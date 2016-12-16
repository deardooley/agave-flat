package org.iplantc.service.jobs.statemachine;

import org.apache.log4j.Logger;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.statefulj.fsm.model.State;
import org.statefulj.persistence.memory.MemoryPersisterImpl;

/** This class provides the main external interface to JobFSM 
 * so that calling code does not have to know anything about
 * the state machine.
 * 
 * @author rcardone
 */
public final class JobFSMUtils
{
    /* ********************************************************************** */
    /*                                Constants                               */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(JobFSMUtils.class);
    
    /* ********************************************************************** */
    /*                                 Fields                                 */
    /* ********************************************************************** */
    // In-memory persister for state machine.
    private static MemoryPersisterImpl<JobFSMStatefulEntity> _persister;
    
    // State machine.
    private static JobFSM<JobFSMStatefulEntity> _jsm;
    
    // Stateful entity object that holds state variable.
    private static JobFSMStatefulEntity _entity;
    
    /* ********************************************************************** */
    /*                              Initializer                               */
    /* ********************************************************************** */
    static {initFSM();}

    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* hasTransition:                                                         */
    /* ---------------------------------------------------------------------- */
    /** Validate that a transition from the current state to the proposed new
     * state has been defined.  We can only process one check at a time since
     * we use a single FSM for all validation.
     * 
     * All runtime exceptions thrown by lower level routines are captured and
     * returned as a false result.  IllegalArgumentExceptions may represent a 
     * logic error in a new version of the code.  Null parameters are tolerated 
     * and also cause a false result.
     * 
     * @param fromStatus the current job status
     * @param newState the proposed new job status
     * @return true if the transition is legal, false otherwise
     */
    public static synchronized boolean hasTransition(JobStatusType fromStatus, 
                                                     JobStatusType toStatus)
    {
        // Garbage in, garbage out.
        if (fromStatus == null || toStatus == null) return false;
        
        // Return false on all exceptions.
        try {
            // Reset the current state in the state machine.
            // Throws an IllegalArgumentException on unknown statuses.
            _persister.setCurrent(_entity, getJobFSMState(fromStatus));
        
            // Allow runtime exceptions to bleed through.
            // Throws an IllegalArgumentException on unknown statuses.
            String eventName = getJobFSMEvent(toStatus).name();
        
            // Attempt the transition to the new state.
            // Throws an IllegalStateException for undefined transitions.
            _jsm.onEvent(_entity, eventName);
        }
        catch (Exception e){return false;}
        
        // We found a transition from "fromStatus" to "toStatus".
        return true;
    }
    
    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* getJobFSMState:                                                        */
    /* ---------------------------------------------------------------------- */
    /** Return the state defined for the specified job status.
     * 
     * @param status the job status whose state is being retrieved
     * @return the state associated with the job status
     */
    private static State<JobFSMStatefulEntity> getJobFSMState(JobStatusType status)
    {
        // Only translate the statuses that have associated states.
        switch (status)
        {
            case PAUSED:                return JobFSMStates.Paused;
            case PENDING:               return JobFSMStates.Pending;
            case PROCESSING_INPUTS:     return JobFSMStates.ProcessingInputs;
            case STAGING_INPUTS:        return JobFSMStates.StagingInputs;
            case STAGED:                return JobFSMStates.Staged;
            case STAGING_JOB:           return JobFSMStates.StagingJob;
            case SUBMITTING:            return JobFSMStates.Submitting;
            case QUEUED:                return JobFSMStates.Queued;
            case RUNNING:               return JobFSMStates.Running;
            case CLEANING_UP:           return JobFSMStates.CleaningUp;
            case ARCHIVING:             return JobFSMStates.Archiving;
            case ARCHIVING_FINISHED:    return JobFSMStates.ArchivingFinished;
            case ARCHIVING_FAILED:      return JobFSMStates.ArchivingFailed;
            case FINISHED:              return JobFSMStates.Finished;
            case KILLED:                return JobFSMStates.Killed;
            case STOPPED:               return JobFSMStates.Stopped;
            case FAILED:                return JobFSMStates.Failed;
            default:                    break;
        }
        
        // This should never happen, but there are statuses that are not FSM states.
        String msg = "Cannot associate any JobFSM state with job status " + status.name() + ".";
        _log.error(msg);
        throw new IllegalArgumentException(msg);
    }
    
    /* ---------------------------------------------------------------------- */
    /* getJobFSMEvent:                                                        */
    /* ---------------------------------------------------------------------- */
    /** Return the event defined for the target job status.
     * 
     * @param status the job status whose event is being retrieved
     * @return the event associated with the job status
     */
    private static JobFSMEvents getJobFSMEvent(JobStatusType status)
    {
        // Only translate the statuses that have associated states.
        switch (status)
        {
            case PAUSED:                return JobFSMEvents.TO_PAUSED;
            case PENDING:               return JobFSMEvents.TO_PENDING;
            case PROCESSING_INPUTS:     return JobFSMEvents.TO_PROCESSING_INPUTS;
            case STAGING_INPUTS:        return JobFSMEvents.TO_STAGING_INPUTS;
            case STAGED:                return JobFSMEvents.TO_STAGED;
            case STAGING_JOB:           return JobFSMEvents.TO_STAGING_JOB;
            case SUBMITTING:            return JobFSMEvents.TO_SUBMITTING;
            case QUEUED:                return JobFSMEvents.TO_QUEUED;
            case RUNNING:               return JobFSMEvents.TO_RUNNING;
            case CLEANING_UP:           return JobFSMEvents.TO_CLEANING_UP;
            case ARCHIVING:             return JobFSMEvents.TO_ARCHIVING;
            case ARCHIVING_FINISHED:    return JobFSMEvents.TO_ARCHIVING_FINISHED;
            case ARCHIVING_FAILED:      return JobFSMEvents.TO_ARCHIVING_FAILED;
            case FINISHED:              return JobFSMEvents.TO_FINISHED;
            case KILLED:                return JobFSMEvents.TO_KILLED;
            case STOPPED:               return JobFSMEvents.TO_STOPPED;
            case FAILED:                return JobFSMEvents.TO_FAILED;
            default:                    break;
        }
    
        // This should never happen, but there are statuses that are not FSM states.
        String msg = "Cannot associate any JobFSM event with job status " + status.name() + ".";
        _log.error(msg);
        throw new IllegalArgumentException(msg);
    }
    
    /* ---------------------------------------------------------------------- */
    /* initFSM:                                                               */
    /* ---------------------------------------------------------------------- */
    /** Initialize all static fields. */
    private static void initFSM()
    {
        // Create the memory-based persister.
        _persister = new MemoryPersisterImpl<JobFSMStatefulEntity>(
                                            JobFSMStates.getStates(),   
                                            JobFSMStates.Pending);  // Start State        

        // Create the Jobs FSM.
        boolean strict = true;
        _jsm = new JobFSM<JobFSMStatefulEntity>("JobFSMUtils", _persister, strict);
        
        // Create the entity
        _entity = new JobFSMStatefulEntity();
    }
}
