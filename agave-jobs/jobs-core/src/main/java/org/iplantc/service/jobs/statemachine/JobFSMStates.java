package org.iplantc.service.jobs.statemachine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.statemachine.actions.RollbackAction;
import org.statefulj.fsm.model.State;
import org.statefulj.fsm.model.impl.StateImpl;

/** This class defines all possible states (i.e., statuses) that a job can be in
 * and all valid state transitions.
 * 
 * JobFSMTest tests every possible state transition.  If the set of states changes,
 * then that test program should be changed accordingly.
 * 
 * @author rcardone
 *
 */
public final class JobFSMStates
{
    /* ********************************************************************** */
    /*                                 Fields                                 */
    /* ********************************************************************** */
    // All states known to the state machine.
    public static final State<JobFSMStatefulEntity> Paused = 
            new StateImpl<JobFSMStatefulEntity>(JobStatusType.PAUSED.name());
    
    public static final State<JobFSMStatefulEntity> Pending = 
            new StateImpl<JobFSMStatefulEntity>(JobStatusType.PENDING.name());
    public static final State<JobFSMStatefulEntity> ProcessingInputs = 
            new StateImpl<JobFSMStatefulEntity>(JobStatusType.PROCESSING_INPUTS.name());
    public static final State<JobFSMStatefulEntity> StagingInputs = 
            new StateImpl<JobFSMStatefulEntity>(JobStatusType.STAGING_INPUTS.name());
    public static final State<JobFSMStatefulEntity> Staged = 
            new StateImpl<JobFSMStatefulEntity>(JobStatusType.STAGED.name());

    public static final State<JobFSMStatefulEntity> StagingJob = 
            new StateImpl<JobFSMStatefulEntity>(JobStatusType.STAGING_JOB.name());
    public static final State<JobFSMStatefulEntity> Submitting = 
            new StateImpl<JobFSMStatefulEntity>(JobStatusType.SUBMITTING.name());
    public static final State<JobFSMStatefulEntity> Queued = 
            new StateImpl<JobFSMStatefulEntity>(JobStatusType.QUEUED.name());
    public static final State<JobFSMStatefulEntity> Running = 
            new StateImpl<JobFSMStatefulEntity>(JobStatusType.RUNNING.name());
    
    public static final State<JobFSMStatefulEntity> CleaningUp = 
            new StateImpl<JobFSMStatefulEntity>(JobStatusType.CLEANING_UP.name());
    public static final State<JobFSMStatefulEntity> Archiving = 
            new StateImpl<JobFSMStatefulEntity>(JobStatusType.ARCHIVING.name());
    public static final State<JobFSMStatefulEntity> ArchivingFinished = 
            new StateImpl<JobFSMStatefulEntity>(JobStatusType.ARCHIVING_FINISHED.name());
    public static final State<JobFSMStatefulEntity> ArchivingFailed = 
            new StateImpl<JobFSMStatefulEntity>(JobStatusType.ARCHIVING_FAILED.name());
    
    public static final State<JobFSMStatefulEntity> Finished = 
            new StateImpl<JobFSMStatefulEntity>(JobStatusType.FINISHED.name());
    public static final State<JobFSMStatefulEntity> Killed = 
            new StateImpl<JobFSMStatefulEntity>(JobStatusType.KILLED.name());
    public static final State<JobFSMStatefulEntity> Stopped = 
            new StateImpl<JobFSMStatefulEntity>(JobStatusType.STOPPED.name());
    public static final State<JobFSMStatefulEntity> Failed = 
            new StateImpl<JobFSMStatefulEntity>(JobStatusType.FAILED.name());
    
    // Create the unmodifiable list of all above defined states.
    private static final List<State<JobFSMStatefulEntity>> _states = createStateList(); 
    
    /* ********************************************************************** */
    /*                             Initializers                               */
    /* ********************************************************************** */
    static {
        // Create all state transitions.
        initializeTransitions();
    }
    
    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* getStates:                                                             */
    /* ---------------------------------------------------------------------- */
    /** Return the statically defined unmodifiable list of states.
     * 
     * @return the unmodifiable list of all states
     */
    public static List<State<JobFSMStatefulEntity>> getStates(){return _states;}
    
    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* createStateList:                                                       */
    /* ---------------------------------------------------------------------- */
    /** Create an unmodifiable list of all states known to the state machine.
     * The HEARTBEAT status is not defined as a state since jobs are never
     * assigned that status (though events are generated with that status).
     * 
     * @return a new unmodifiable list of all states
     */
    private static List<State<JobFSMStatefulEntity>> createStateList()
    {
        // Don't forget to update the initial capacity to match the number of states.
        List<State<JobFSMStatefulEntity>> states = new ArrayList<State<JobFSMStatefulEntity>>(17);
        states.add(Paused);
        states.add(Pending);
        states.add(ProcessingInputs);
        states.add(StagingInputs);
        states.add(Staged);
        states.add(StagingJob);
        states.add(Submitting);
        states.add(Queued);
        states.add(Running);
        states.add(CleaningUp);
        states.add(Archiving);
        states.add(ArchivingFinished);
        states.add(ArchivingFailed);
        states.add(Finished);
        states.add(Killed);
        states.add(Stopped);
        states.add(Failed);
        return Collections.unmodifiableList(states);
    }
    
    /* ---------------------------------------------------------------------- */
    /* initializeTransitions:                                                 */
    /* ---------------------------------------------------------------------- */
    /** Define the transitions for all states.  All transitions are deterministic.
     * No state is designated as a terminal state since all potential candidate 
     * states can be rolled back.
     */
    private static void initializeTransitions()
    {
        /* Deterministic Transitions */
        //
        // When transitions are defined with an action, the result is:
        //
        //      stateA(eventA) -> stateB/actionA
        //
        // and without an action:
        //
        //      stateA(eventA) -> stateB/noop

        /* ================================================================== */
        /* Forward Processing Transitions                                     */
        /* ================================================================== */
        // Since we only use the state machine to validate that a transition is
        // legal, we don't need to specify actions on transitions.  When used
        // with the JobFSM, exceptions are thrown on undefined transitions.
        
        // ------ From Paused
        JobFSMStates.Paused.addTransition(JobFSMEvents.TO_ARCHIVING.name(), JobFSMStates.Archiving);     
        JobFSMStates.Paused.addTransition(JobFSMEvents.TO_ARCHIVING_FAILED.name(), JobFSMStates.ArchivingFailed);
        JobFSMStates.Paused.addTransition(JobFSMEvents.TO_ARCHIVING_FINISHED.name(), JobFSMStates.ArchivingFinished);
        JobFSMStates.Paused.addTransition(JobFSMEvents.TO_CLEANING_UP.name(), JobFSMStates.CleaningUp);
        JobFSMStates.Paused.addTransition(JobFSMEvents.TO_FAILED.name(), JobFSMStates.Failed);
        JobFSMStates.Paused.addTransition(JobFSMEvents.TO_FINISHED.name(), JobFSMStates.Finished);
        JobFSMStates.Paused.addTransition(JobFSMEvents.TO_KILLED.name(), JobFSMStates.Killed);
        JobFSMStates.Paused.addTransition(JobFSMEvents.TO_PAUSED.name(), JobFSMStates.Paused);
        JobFSMStates.Paused.addTransition(JobFSMEvents.TO_PENDING.name(), JobFSMStates.Pending);
        JobFSMStates.Paused.addTransition(JobFSMEvents.TO_PROCESSING_INPUTS.name(), JobFSMStates.ProcessingInputs);
        JobFSMStates.Paused.addTransition(JobFSMEvents.TO_QUEUED.name(), JobFSMStates.Queued);
        JobFSMStates.Paused.addTransition(JobFSMEvents.TO_RUNNING.name(), JobFSMStates.Running);
        JobFSMStates.Paused.addTransition(JobFSMEvents.TO_STAGED.name(), JobFSMStates.Staged);
        JobFSMStates.Paused.addTransition(JobFSMEvents.TO_STAGING_INPUTS.name(), JobFSMStates.StagingInputs);
        JobFSMStates.Paused.addTransition(JobFSMEvents.TO_STAGING_JOB.name(), JobFSMStates.StagingJob);
        JobFSMStates.Paused.addTransition(JobFSMEvents.TO_STOPPED.name(), JobFSMStates.Stopped);
        JobFSMStates.Paused.addTransition(JobFSMEvents.TO_SUBMITTING.name(), JobFSMStates.Submitting);
               
        // ------ From Pending
        JobFSMStates.Pending.addTransition(JobFSMEvents.TO_FAILED.name(), JobFSMStates.Failed);
        JobFSMStates.Pending.addTransition(JobFSMEvents.TO_PAUSED.name(), JobFSMStates.Paused);
        JobFSMStates.Pending.addTransition(JobFSMEvents.TO_PENDING.name(), JobFSMStates.Pending);
        JobFSMStates.Pending.addTransition(JobFSMEvents.TO_PROCESSING_INPUTS.name(), JobFSMStates.ProcessingInputs);
        JobFSMStates.Pending.addTransition(JobFSMEvents.TO_STOPPED.name(), JobFSMStates.Stopped);
               
        // ------ From ProcessingInputs
        JobFSMStates.ProcessingInputs.addTransition(JobFSMEvents.TO_FAILED.name(), JobFSMStates.Failed);
        JobFSMStates.ProcessingInputs.addTransition(JobFSMEvents.TO_PAUSED.name(), JobFSMStates.Paused);
        JobFSMStates.ProcessingInputs.addTransition(JobFSMEvents.TO_PROCESSING_INPUTS.name(), JobFSMStates.ProcessingInputs);
        JobFSMStates.ProcessingInputs.addTransition(JobFSMEvents.TO_STAGING_INPUTS.name(), JobFSMStates.StagingInputs);
        JobFSMStates.ProcessingInputs.addTransition(JobFSMEvents.TO_STOPPED.name(), JobFSMStates.Stopped);
        
        // ------ From StagingInputs
        JobFSMStates.StagingInputs.addTransition(JobFSMEvents.TO_FAILED.name(), JobFSMStates.Failed);
        JobFSMStates.StagingInputs.addTransition(JobFSMEvents.TO_PAUSED.name(), JobFSMStates.Paused);
        JobFSMStates.StagingInputs.addTransition(JobFSMEvents.TO_STAGED.name(), JobFSMStates.Staged);
        JobFSMStates.StagingInputs.addTransition(JobFSMEvents.TO_STAGING_INPUTS.name(), JobFSMStates.StagingInputs);
        JobFSMStates.StagingInputs.addTransition(JobFSMEvents.TO_STOPPED.name(), JobFSMStates.Stopped);
        
        // ------ From Staged
        JobFSMStates.Staged.addTransition(JobFSMEvents.TO_FAILED.name(), JobFSMStates.Failed);
        JobFSMStates.Staged.addTransition(JobFSMEvents.TO_PAUSED.name(), JobFSMStates.Paused);
        JobFSMStates.Staged.addTransition(JobFSMEvents.TO_STAGED.name(), JobFSMStates.Staged);
        JobFSMStates.Staged.addTransition(JobFSMEvents.TO_STOPPED.name(), JobFSMStates.Stopped);
        JobFSMStates.Staged.addTransition(JobFSMEvents.TO_SUBMITTING.name(), JobFSMStates.Submitting);
        
        // ------ From StagingJob
        JobFSMStates.StagingJob.addTransition(JobFSMEvents.TO_FAILED.name(), JobFSMStates.Failed);
        JobFSMStates.StagingJob.addTransition(JobFSMEvents.TO_PAUSED.name(), JobFSMStates.Paused);
        JobFSMStates.StagingJob.addTransition(JobFSMEvents.TO_STAGING_JOB.name(), JobFSMStates.StagingJob);
        JobFSMStates.StagingJob.addTransition(JobFSMEvents.TO_STOPPED.name(), JobFSMStates.Stopped);
        JobFSMStates.StagingJob.addTransition(JobFSMEvents.TO_SUBMITTING.name(), JobFSMStates.Submitting);
                       
        // ------ From Submitting
        JobFSMStates.Submitting.addTransition(JobFSMEvents.TO_FAILED.name(), JobFSMStates.Failed);
        JobFSMStates.Submitting.addTransition(JobFSMEvents.TO_KILLED.name(), JobFSMStates.Killed);
        JobFSMStates.Submitting.addTransition(JobFSMEvents.TO_PAUSED.name(), JobFSMStates.Paused);
        JobFSMStates.Submitting.addTransition(JobFSMEvents.TO_QUEUED.name(), JobFSMStates.Queued);
        JobFSMStates.Submitting.addTransition(JobFSMEvents.TO_RUNNING.name(), JobFSMStates.Running);
        JobFSMStates.Submitting.addTransition(JobFSMEvents.TO_STOPPED.name(), JobFSMStates.Stopped);
        JobFSMStates.Submitting.addTransition(JobFSMEvents.TO_SUBMITTING.name(), JobFSMStates.Submitting);
        
        // ------ From Queued
        JobFSMStates.Queued.addTransition(JobFSMEvents.TO_CLEANING_UP.name(), JobFSMStates.CleaningUp);
        JobFSMStates.Queued.addTransition(JobFSMEvents.TO_FAILED.name(), JobFSMStates.Failed);
        JobFSMStates.Queued.addTransition(JobFSMEvents.TO_KILLED.name(), JobFSMStates.Killed);
        JobFSMStates.Queued.addTransition(JobFSMEvents.TO_PAUSED.name(), JobFSMStates.Paused);
        JobFSMStates.Queued.addTransition(JobFSMEvents.TO_QUEUED.name(), JobFSMStates.Queued);
        JobFSMStates.Queued.addTransition(JobFSMEvents.TO_RUNNING.name(), JobFSMStates.Running);
        JobFSMStates.Queued.addTransition(JobFSMEvents.TO_STOPPED.name(), JobFSMStates.Stopped);
        
        // ------ From Running
        JobFSMStates.Running.addTransition(JobFSMEvents.TO_CLEANING_UP.name(), JobFSMStates.CleaningUp);
        JobFSMStates.Running.addTransition(JobFSMEvents.TO_FAILED.name(), JobFSMStates.Failed);
        JobFSMStates.Running.addTransition(JobFSMEvents.TO_KILLED.name(), JobFSMStates.Killed);
        JobFSMStates.Running.addTransition(JobFSMEvents.TO_PAUSED.name(), JobFSMStates.Paused);
        JobFSMStates.Running.addTransition(JobFSMEvents.TO_RUNNING.name(), JobFSMStates.Running);
        JobFSMStates.Running.addTransition(JobFSMEvents.TO_STOPPED.name(), JobFSMStates.Stopped);
             
        // ------ From CleaningUp
        JobFSMStates.CleaningUp.addTransition(JobFSMEvents.TO_ARCHIVING.name(), JobFSMStates.Archiving);     
        JobFSMStates.CleaningUp.addTransition(JobFSMEvents.TO_CLEANING_UP.name(), JobFSMStates.CleaningUp);
        JobFSMStates.CleaningUp.addTransition(JobFSMEvents.TO_FAILED.name(), JobFSMStates.Failed);
        JobFSMStates.CleaningUp.addTransition(JobFSMEvents.TO_FINISHED.name(), JobFSMStates.Finished);
        JobFSMStates.CleaningUp.addTransition(JobFSMEvents.TO_KILLED.name(), JobFSMStates.Killed);
        JobFSMStates.CleaningUp.addTransition(JobFSMEvents.TO_PAUSED.name(), JobFSMStates.Paused);
        JobFSMStates.CleaningUp.addTransition(JobFSMEvents.TO_STOPPED.name(), JobFSMStates.Stopped);
        
        // ------ From Archiving
        JobFSMStates.Archiving.addTransition(JobFSMEvents.TO_ARCHIVING.name(), JobFSMStates.Archiving);     
        JobFSMStates.Archiving.addTransition(JobFSMEvents.TO_ARCHIVING_FAILED.name(), JobFSMStates.ArchivingFailed);
        JobFSMStates.Archiving.addTransition(JobFSMEvents.TO_ARCHIVING_FINISHED.name(), JobFSMStates.ArchivingFinished);
        JobFSMStates.Archiving.addTransition(JobFSMEvents.TO_FAILED.name(), JobFSMStates.Failed);
        JobFSMStates.Archiving.addTransition(JobFSMEvents.TO_KILLED.name(), JobFSMStates.Killed);
        JobFSMStates.Archiving.addTransition(JobFSMEvents.TO_PAUSED.name(), JobFSMStates.Paused);
        JobFSMStates.Archiving.addTransition(JobFSMEvents.TO_STOPPED.name(), JobFSMStates.Stopped);
        
        // ------ From ArchivingFinished
        JobFSMStates.ArchivingFinished.addTransition(JobFSMEvents.TO_ARCHIVING_FINISHED.name(), JobFSMStates.ArchivingFinished);
        JobFSMStates.ArchivingFinished.addTransition(JobFSMEvents.TO_FAILED.name(), JobFSMStates.Failed);
        JobFSMStates.ArchivingFinished.addTransition(JobFSMEvents.TO_FINISHED.name(), JobFSMStates.Finished);
        
        // ------ From ArchivingFailed
        JobFSMStates.ArchivingFailed.addTransition(JobFSMEvents.TO_ARCHIVING_FAILED.name(), JobFSMStates.ArchivingFailed);
        JobFSMStates.ArchivingFailed.addTransition(JobFSMEvents.TO_FAILED.name(), JobFSMStates.Failed);

        // ------ From Finished, Killed, Stopped, Failed
        // none
               
        /* ================================================================== */
        /* Rollback Processing Transitions                                    */
        /* ================================================================== */
        // Create an action that just logs the rollback event when debugging.
        RollbackAction<JobFSMStatefulEntity> rollbackAction = new RollbackAction<JobFSMStatefulEntity>();
        
        // ------ To Pending
        JobFSMStates.ProcessingInputs.addTransition(JobFSMEvents.TO_PENDING.name(), JobFSMStates.Pending, rollbackAction);
        JobFSMStates.Staged.addTransition(JobFSMEvents.TO_PENDING.name(), JobFSMStates.Pending, rollbackAction);
        JobFSMStates.StagingInputs.addTransition(JobFSMEvents.TO_PENDING.name(), JobFSMStates.Pending, rollbackAction);
        JobFSMStates.Failed.addTransition(JobFSMEvents.TO_PENDING.name(), JobFSMStates.Pending, rollbackAction);
        JobFSMStates.Finished.addTransition(JobFSMEvents.TO_PENDING.name(), JobFSMStates.Pending, rollbackAction);
        JobFSMStates.Killed.addTransition(JobFSMEvents.TO_PENDING.name(), JobFSMStates.Pending, rollbackAction);
        JobFSMStates.Stopped.addTransition(JobFSMEvents.TO_PENDING.name(), JobFSMStates.Pending, rollbackAction);
        
        // ------ To Staged
        //JobFSMStates.Staged.addTransition(JobFSMEvents.TO_STAGED.name(), JobFSMStates.Staged);
        JobFSMStates.StagingJob.addTransition(JobFSMEvents.TO_STAGED.name(), JobFSMStates.Staged, rollbackAction);
        JobFSMStates.Submitting.addTransition(JobFSMEvents.TO_STAGED.name(), JobFSMStates.Staged, rollbackAction);
        JobFSMStates.Queued.addTransition(JobFSMEvents.TO_STAGED.name(), JobFSMStates.Staged, rollbackAction);
        JobFSMStates.Running.addTransition(JobFSMEvents.TO_STAGED.name(), JobFSMStates.Staged, rollbackAction);
        JobFSMStates.CleaningUp.addTransition(JobFSMEvents.TO_STAGED.name(), JobFSMStates.Staged, rollbackAction);
        
        // ------ To CleaningUp
        JobFSMStates.Archiving.addTransition(JobFSMEvents.TO_CLEANING_UP.name(), JobFSMStates.CleaningUp, rollbackAction);
        JobFSMStates.ArchivingFailed.addTransition(JobFSMEvents.TO_CLEANING_UP.name(), JobFSMStates.CleaningUp, rollbackAction);
        JobFSMStates.ArchivingFinished.addTransition(JobFSMEvents.TO_CLEANING_UP.name(), JobFSMStates.CleaningUp, rollbackAction);
    }
}
