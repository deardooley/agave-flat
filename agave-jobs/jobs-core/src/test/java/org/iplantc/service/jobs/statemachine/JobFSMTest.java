package org.iplantc.service.jobs.statemachine;

import org.statefulj.fsm.model.State;
import org.statefulj.persistence.memory.MemoryPersisterImpl;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/** This class test all possible state transitions for every state, resulting
 * in a grand total of 289 (17 * 17) transitions attempts.  For transitions that 
 * are expected to succeed, their new state is checked.  For transitions that
 * are expected to fail because they are not defined, the expected exception
 * is caught.
 * 
 * Whenever a new state is added a new test method should be implemented and 
 * all existing test methods should be modified to include the new state as
 * a transition target.
 * 
 * @author rcardone
 *
 */
public class JobFSMTest
{
    /* ********************************************************************** */
    /*                                 Fields                                 */
    /* ********************************************************************** */
    // In-memory persister for state machine.
    private MemoryPersisterImpl<JobFSMStatefulEntity> _persister;
    
    // State machine.
    private JobFSM<JobFSMStatefulEntity> _jsm;
    
    // Stateful entity object that holds state variable.
    private JobFSMStatefulEntity _entity;
    
    // Flag used to confirm that an invalid transistion was attempted.
    private boolean _exceptionThrown;
    
    /* ********************************************************************** */
    /*                            Set Up / Tear Down                          */
    /* ********************************************************************** */    
    /* ---------------------------------------------------------------------- */
    /* setup:                                                                 */
    /* ---------------------------------------------------------------------- */
    @BeforeSuite
    private void setup()
    {
        // Create the memory-based persister.
        _persister = new MemoryPersisterImpl<JobFSMStatefulEntity>(
                                            JobFSMStates.getStates(),   
                                            JobFSMStates.Pending);  // Start State        

        // Create the Jobs FSM.
        boolean strict = true;
        _jsm = new JobFSM<JobFSMStatefulEntity>("Test JobFSM", _persister, strict);
        
        // Create the entity
        _entity = new JobFSMStatefulEntity();
    }
    
    /* ---------------------------------------------------------------------- */
    /* setupMethod:                                                           */
    /* ---------------------------------------------------------------------- */
    @BeforeMethod
    private void setupMethod()
    {
        _exceptionThrown = false;
    }
    
    /* ********************************************************************** */
    /*                              Test Methods                              */
    /* ********************************************************************** */    
    /* ---------------------------------------------------------------------- */
    /* fromPaused:                                                            */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void fromPaused()
    {   
        // ----- Legal transitions.
        _persister.setCurrent(_entity, JobFSMStates.Paused);
        State<JobFSMStatefulEntity> state = _jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Archiving.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Paused);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FAILED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.ArchivingFailed.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Paused);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FINISHED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.ArchivingFinished.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Paused);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_CLEANING_UP.name());
        Assert.assertEquals(state.getName(), JobFSMStates.CleaningUp.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Paused);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_FAILED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Failed.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Paused);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_FINISHED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Finished.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Paused);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_KILLED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Killed.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Paused);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_PAUSED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Paused.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Paused);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_PENDING.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Pending.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Paused);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_PROCESSING_INPUTS.name());
        Assert.assertEquals(state.getName(), JobFSMStates.ProcessingInputs.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Paused);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_QUEUED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Queued.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Paused);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_RUNNING.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Running.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Paused);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_STAGED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Staged.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Paused);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_INPUTS.name());
        Assert.assertEquals(state.getName(), JobFSMStates.StagingInputs.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Paused);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_JOB.name());
        Assert.assertEquals(state.getName(), JobFSMStates.StagingJob.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Paused);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_STOPPED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Stopped.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Paused);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_SUBMITTING.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Submitting.getName(), 
                            "Transitioned to the wrong state!");
    }
   
    /* ---------------------------------------------------------------------- */
    /* fromPending:                                                           */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void fromPending()
    {   
        // ----- Legal transitions.
        _persister.setCurrent(_entity, JobFSMStates.Pending);
        State<JobFSMStatefulEntity> state = _jsm.onEvent(_entity, JobFSMEvents.TO_FAILED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Failed.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Pending);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_PAUSED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Paused.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Pending);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_PENDING.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Pending.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Pending);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_PROCESSING_INPUTS.name());
        Assert.assertEquals(state.getName(), JobFSMStates.ProcessingInputs.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Pending);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_STAGED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Staged.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Pending);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_STOPPED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Stopped.getName(), 
                            "Transitioned to the wrong state!");
        
        // ----- Illegal transitions.
        // Should be the last time we reset this.
        _persister.setCurrent(_entity, JobFSMStates.Pending);
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING.name());}
            catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FAILED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING_FAILED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FINISHED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING_FINISHED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_CLEANING_UP.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_CLEANING_UP.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_FINISHED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_FINISHED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_KILLED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_KILLED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_QUEUED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_QUEUED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_RUNNING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_RUNNING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_INPUTS.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_INPUTS.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_JOB.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_JOB.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_SUBMITTING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_SUBMITTING.name()); 
    }

    /* ---------------------------------------------------------------------- */
    /* fromProcessingInputs:                                                  */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void fromProcessingInputs()
    {   
        // ----- Legal transitions.
        _persister.setCurrent(_entity, JobFSMStates.ProcessingInputs);
        State<JobFSMStatefulEntity> state = _jsm.onEvent(_entity, JobFSMEvents.TO_FAILED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Failed.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.ProcessingInputs);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_PAUSED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Paused.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.ProcessingInputs);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_PROCESSING_INPUTS.name());
        Assert.assertEquals(state.getName(), JobFSMStates.ProcessingInputs.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.ProcessingInputs);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_INPUTS.name());
        Assert.assertEquals(state.getName(), JobFSMStates.StagingInputs.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.ProcessingInputs);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_STOPPED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Stopped.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.ProcessingInputs);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_PENDING.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Pending.getName(), 
                            "Transitioned to the wrong state!");
        
        // ----- Illegal transitions.
        // Should be the last time we reset this.
        _persister.setCurrent(_entity, JobFSMStates.ProcessingInputs);
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING.name());}
            catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FAILED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING_FAILED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FINISHED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING_FINISHED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_CLEANING_UP.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_CLEANING_UP.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_FINISHED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_FINISHED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_KILLED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_KILLED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_QUEUED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_QUEUED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_RUNNING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_RUNNING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_JOB.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_JOB.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_SUBMITTING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_SUBMITTING.name()); 
    }

    /* ---------------------------------------------------------------------- */
    /* fromStagingInputs:                                                     */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void fromStagingInputs()
    {   
        // ----- Legal transitions.
        _persister.setCurrent(_entity, JobFSMStates.StagingInputs);
        State<JobFSMStatefulEntity> state = _jsm.onEvent(_entity, JobFSMEvents.TO_FAILED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Failed.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.StagingInputs);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_PAUSED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Paused.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.StagingInputs);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_STAGED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Staged.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.StagingInputs);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_INPUTS.name());
        Assert.assertEquals(state.getName(), JobFSMStates.StagingInputs.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.StagingInputs);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_STOPPED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Stopped.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.StagingInputs);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_PENDING.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Pending.getName(), 
                            "Transitioned to the wrong state!");
        
        // ----- Illegal transitions.
        // Should be the last time we reset this.
        _persister.setCurrent(_entity, JobFSMStates.StagingInputs);
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING.name());}
            catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FAILED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING_FAILED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FINISHED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING_FINISHED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_CLEANING_UP.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_CLEANING_UP.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_FINISHED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_FINISHED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_KILLED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_KILLED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PROCESSING_INPUTS.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PROCESSING_INPUTS.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_QUEUED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_QUEUED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_RUNNING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_RUNNING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_JOB.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_JOB.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_SUBMITTING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_SUBMITTING.name()); 
    }

    /* ---------------------------------------------------------------------- */
    /* fromStaged:                                                            */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void fromStaged()
    {   
        // ----- Legal transitions.
        _persister.setCurrent(_entity, JobFSMStates.Staged);
        State<JobFSMStatefulEntity> state = _jsm.onEvent(_entity, JobFSMEvents.TO_FAILED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Failed.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Staged);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_PAUSED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Paused.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Staged);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_STAGED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Staged.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Staged);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_STOPPED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Stopped.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Staged);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_SUBMITTING.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Submitting.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Staged);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_PENDING.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Pending.getName(), 
                            "Transitioned to the wrong state!");
        
        // ----- Illegal transitions.
        // Should be the last time we reset this.
        _persister.setCurrent(_entity, JobFSMStates.Staged);
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING.name());}
            catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FAILED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING_FAILED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FINISHED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING_FINISHED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_CLEANING_UP.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_CLEANING_UP.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_FINISHED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_FINISHED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_KILLED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_KILLED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PROCESSING_INPUTS.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PROCESSING_INPUTS.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_INPUTS.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_INPUTS.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_JOB.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_JOB.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_QUEUED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_QUEUED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_RUNNING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_RUNNING.name()); 
    }

    /* ---------------------------------------------------------------------- */
    /* fromStagingJob:                                                        */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void fromStagingJob()
    {   
        // ----- Legal transitions.
        _persister.setCurrent(_entity, JobFSMStates.StagingJob);
        State<JobFSMStatefulEntity> state = _jsm.onEvent(_entity, JobFSMEvents.TO_FAILED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Failed.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.StagingJob);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_PAUSED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Paused.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.StagingJob);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_JOB.name());
        Assert.assertEquals(state.getName(), JobFSMStates.StagingJob.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.StagingJob);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_STOPPED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Stopped.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.StagingJob);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_SUBMITTING.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Submitting.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.StagingJob);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_STAGED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Staged.getName(), 
                            "Transitioned to the wrong state!");
        
        // ----- Illegal transitions.
        // Should be the last time we reset this.
        _persister.setCurrent(_entity, JobFSMStates.StagingJob);
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING.name());}
            catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FAILED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING_FAILED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FINISHED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING_FINISHED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_CLEANING_UP.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_CLEANING_UP.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_FINISHED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_FINISHED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_KILLED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_KILLED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PENDING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_JOB.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PROCESSING_INPUTS.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PROCESSING_INPUTS.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_INPUTS.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_INPUTS.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_QUEUED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_QUEUED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_RUNNING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_RUNNING.name()); 
    }

    /* ---------------------------------------------------------------------- */
    /* fromSubmitting:                                                        */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void fromSubmitting()
    {   
        // ----- Legal transitions.
        _persister.setCurrent(_entity, JobFSMStates.Submitting);
        State<JobFSMStatefulEntity> state = _jsm.onEvent(_entity, JobFSMEvents.TO_FAILED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Failed.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Submitting);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_PAUSED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Paused.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Submitting);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_KILLED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Killed.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Submitting);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_QUEUED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Queued.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Submitting);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_RUNNING.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Running.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Submitting);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_STOPPED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Stopped.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Submitting);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_SUBMITTING.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Submitting.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Submitting);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_STAGED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Staged.getName(), 
                            "Transitioned to the wrong state!");
        
        // ----- Illegal transitions.
        // Should be the last time we reset this.
        _persister.setCurrent(_entity, JobFSMStates.Submitting);
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING.name());}
            catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FAILED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING_FAILED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FINISHED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING_FINISHED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_CLEANING_UP.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_CLEANING_UP.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_FINISHED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_FINISHED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PENDING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PENDING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PROCESSING_INPUTS.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PROCESSING_INPUTS.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_INPUTS.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_INPUTS.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_JOB.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_JOB.name()); 
    }

    /* ---------------------------------------------------------------------- */
    /* fromQueued:                                                            */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void fromQueued()
    {   
        // ----- Legal transitions.
        _persister.setCurrent(_entity, JobFSMStates.Queued);
        State<JobFSMStatefulEntity> state = _jsm.onEvent(_entity, JobFSMEvents.TO_FAILED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Failed.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Queued);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_PAUSED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Paused.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Queued);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_KILLED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Killed.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Queued);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_QUEUED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Queued.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Queued);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_RUNNING.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Running.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Queued);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_STOPPED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Stopped.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Queued);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_CLEANING_UP.name());
        Assert.assertEquals(state.getName(), JobFSMStates.CleaningUp.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Queued);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_STAGED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Staged.getName(), 
                            "Transitioned to the wrong state!");
        
        // ----- Illegal transitions.
        // Should be the last time we reset this.
        _persister.setCurrent(_entity, JobFSMStates.Queued);
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING.name());}
            catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FAILED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING_FAILED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FINISHED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING_FINISHED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_SUBMITTING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_SUBMITTING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_FINISHED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_FINISHED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PENDING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PENDING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PROCESSING_INPUTS.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PROCESSING_INPUTS.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_INPUTS.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_INPUTS.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_JOB.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_JOB.name()); 
   }

    /* ---------------------------------------------------------------------- */
    /* fromRunning:                                                           */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void fromRunning()
    {   
        // ----- Legal transitions.
        _persister.setCurrent(_entity, JobFSMStates.Running);
        State<JobFSMStatefulEntity> state = _jsm.onEvent(_entity, JobFSMEvents.TO_FAILED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Failed.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Running);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_PAUSED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Paused.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Running);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_KILLED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Killed.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Running);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_RUNNING.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Running.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Running);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_STOPPED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Stopped.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Running);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_CLEANING_UP.name());
        Assert.assertEquals(state.getName(), JobFSMStates.CleaningUp.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Running);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_STAGED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Staged.getName(), 
                            "Transitioned to the wrong state!");
        
        // ----- Illegal transitions.
        // Should be the last time we reset this.
        _persister.setCurrent(_entity, JobFSMStates.Running);
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING.name());}
            catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FAILED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING_FAILED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FINISHED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING_FINISHED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_SUBMITTING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_SUBMITTING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_FINISHED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_FINISHED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PENDING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PENDING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PROCESSING_INPUTS.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PROCESSING_INPUTS.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_INPUTS.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_INPUTS.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_JOB.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_JOB.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_QUEUED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_QUEUED.name()); 
    }

    /* ---------------------------------------------------------------------- */
    /* fromCleaningUp:                                                        */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void fromCleaningUp()
    {   
        // ----- Legal transitions.
        _persister.setCurrent(_entity, JobFSMStates.CleaningUp);
        State<JobFSMStatefulEntity> state = _jsm.onEvent(_entity, JobFSMEvents.TO_FAILED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Failed.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.CleaningUp);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_PAUSED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Paused.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.CleaningUp);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_KILLED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Killed.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.CleaningUp);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_FINISHED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Finished.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.CleaningUp);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Archiving.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.CleaningUp);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_STOPPED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Stopped.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.CleaningUp);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_CLEANING_UP.name());
        Assert.assertEquals(state.getName(), JobFSMStates.CleaningUp.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.CleaningUp);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_STAGED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Staged.getName(), 
                            "Transitioned to the wrong state!");
        
        // ----- Illegal transitions.
        // Should be the last time we reset this.
        _persister.setCurrent(_entity, JobFSMStates.CleaningUp);
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FAILED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING_FAILED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FINISHED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING_FINISHED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_SUBMITTING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_SUBMITTING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PENDING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PENDING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PROCESSING_INPUTS.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PROCESSING_INPUTS.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_INPUTS.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_INPUTS.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_JOB.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_JOB.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_QUEUED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_QUEUED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_RUNNING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_RUNNING.name()); 
    }

    /* ---------------------------------------------------------------------- */
    /* fromArchiving:                                                         */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void fromArchiving()
    {   
        // ----- Legal transitions.
        _persister.setCurrent(_entity, JobFSMStates.Archiving);
        State<JobFSMStatefulEntity> state = _jsm.onEvent(_entity, JobFSMEvents.TO_FAILED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Failed.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Archiving);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_PAUSED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Paused.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Archiving);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_KILLED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Killed.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Archiving);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Archiving.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Archiving);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FAILED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.ArchivingFailed.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Archiving);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FINISHED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.ArchivingFinished.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Archiving);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_STOPPED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Stopped.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.Archiving);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_CLEANING_UP.name());
        Assert.assertEquals(state.getName(), JobFSMStates.CleaningUp.getName(), 
                            "Transitioned to the wrong state!");
        
        // ----- Illegal transitions.
        // Should be the last time we reset this.
        _persister.setCurrent(_entity, JobFSMStates.Archiving);
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_SUBMITTING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_SUBMITTING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PENDING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PENDING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_FINISHED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_FINISHED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PROCESSING_INPUTS.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PROCESSING_INPUTS.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_INPUTS.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_INPUTS.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_JOB.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_JOB.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_QUEUED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_QUEUED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_RUNNING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_RUNNING.name()); 
    }

    /* ---------------------------------------------------------------------- */
    /* fromArchivingFailed:                                                   */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void fromArchivingFailed()
    {   
        // ----- Legal transitions.
        _persister.setCurrent(_entity, JobFSMStates.ArchivingFailed);
        State<JobFSMStatefulEntity> state = _jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FAILED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.ArchivingFailed.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.ArchivingFailed);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_FAILED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Failed.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.ArchivingFailed);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_CLEANING_UP.name());
        Assert.assertEquals(state.getName(), JobFSMStates.CleaningUp.getName(), 
                            "Transitioned to the wrong state!");
        
        // ----- Illegal transitions.
        // Should be the last time we reset this.
        _persister.setCurrent(_entity, JobFSMStates.ArchivingFailed);

        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FINISHED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING_FINISHED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_FINISHED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_FINISHED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_KILLED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_KILLED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PAUSED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PAUSED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PENDING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PENDING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PROCESSING_INPUTS.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PROCESSING_INPUTS.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_QUEUED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_QUEUED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_RUNNING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_RUNNING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_INPUTS.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_INPUTS.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_JOB.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_JOB.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STOPPED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STOPPED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_SUBMITTING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_SUBMITTING.name()); 
    }
    
    /* ---------------------------------------------------------------------- */
    /* fromArchivingFinished:                                                 */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void fromArchivingFinished()
    {   
        // ----- Legal transitions.
        _persister.setCurrent(_entity, JobFSMStates.ArchivingFinished);
        State<JobFSMStatefulEntity> state = _jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FINISHED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.ArchivingFinished.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.ArchivingFinished);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_FAILED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Failed.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.ArchivingFinished);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_FINISHED.name());
        Assert.assertEquals(state.getName(), JobFSMStates.Finished.getName(), 
                            "Transitioned to the wrong state!");
        
        _persister.setCurrent(_entity, JobFSMStates.ArchivingFinished);
        state = _jsm.onEvent(_entity, JobFSMEvents.TO_CLEANING_UP.name());
        Assert.assertEquals(state.getName(), JobFSMStates.CleaningUp.getName(), 
                            "Transitioned to the wrong state!");
        
        // ----- Illegal transitions.
        // Should be the last time we reset this.
        _persister.setCurrent(_entity, JobFSMStates.ArchivingFinished);

        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FAILED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING_FAILED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_KILLED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_KILLED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PAUSED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PAUSED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PENDING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PENDING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PROCESSING_INPUTS.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PROCESSING_INPUTS.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_QUEUED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_QUEUED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_RUNNING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_RUNNING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_INPUTS.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_INPUTS.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_JOB.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_JOB.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STOPPED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STOPPED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_SUBMITTING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_SUBMITTING.name()); 
    }
    
    /* ---------------------------------------------------------------------- */
    /* fromFailed:                                                            */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void fromFailed()
    {   
        // ----- Illegal transitions.
        // Should be the last time we reset this.
        _persister.setCurrent(_entity, JobFSMStates.Failed);

        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FAILED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING_FAILED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FINISHED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING_FINISHED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_CLEANING_UP.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_CLEANING_UP.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_FAILED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_FAILED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_FINISHED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_FINISHED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_KILLED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_KILLED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PAUSED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PAUSED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PROCESSING_INPUTS.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PROCESSING_INPUTS.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_QUEUED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_QUEUED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_RUNNING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_RUNNING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_INPUTS.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_INPUTS.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_JOB.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_JOB.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STOPPED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STOPPED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_SUBMITTING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_SUBMITTING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PENDING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PENDING.name()); 
    }

    /* ---------------------------------------------------------------------- */
    /* fromFinished:                                                          */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void fromFinished()
    {   
        // ----- Illegal transitions.
        // Should be the last time we reset this.
        _persister.setCurrent(_entity, JobFSMStates.Finished);

        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FAILED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING_FAILED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FINISHED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING_FINISHED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_CLEANING_UP.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_CLEANING_UP.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_FAILED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_FAILED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_FINISHED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_FINISHED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_KILLED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_KILLED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PAUSED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PAUSED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PROCESSING_INPUTS.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PROCESSING_INPUTS.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_QUEUED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_QUEUED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_RUNNING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_RUNNING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_INPUTS.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_INPUTS.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_JOB.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_JOB.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STOPPED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STOPPED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_SUBMITTING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_SUBMITTING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PENDING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PENDING.name()); 
    }
    
    /* ---------------------------------------------------------------------- */
    /* fromKilled:                                                            */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void fromKilled()
    {   
        // ----- Illegal transitions.
        // Should be the last time we reset this.
        _persister.setCurrent(_entity, JobFSMStates.Killed);

        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FAILED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING_FAILED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FINISHED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING_FINISHED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_CLEANING_UP.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_CLEANING_UP.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_FAILED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_FAILED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_FINISHED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_FINISHED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_KILLED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_KILLED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PAUSED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PAUSED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PROCESSING_INPUTS.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PROCESSING_INPUTS.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_QUEUED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_QUEUED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_RUNNING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_RUNNING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_INPUTS.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_INPUTS.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_JOB.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_JOB.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STOPPED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STOPPED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_SUBMITTING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_SUBMITTING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PENDING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PENDING.name()); 
    }
    
    /* ---------------------------------------------------------------------- */
    /* fromStopped:                                                           */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void fromStopped()
    {   
        // ----- Illegal transitions.
        // Should be the last time we reset this.
        _persister.setCurrent(_entity, JobFSMStates.Stopped);

        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FAILED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING_FAILED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_ARCHIVING_FINISHED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_ARCHIVING_FINISHED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_CLEANING_UP.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_CLEANING_UP.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_FAILED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_FAILED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_FINISHED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_FINISHED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_KILLED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_KILLED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PAUSED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PAUSED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PROCESSING_INPUTS.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PROCESSING_INPUTS.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_QUEUED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_QUEUED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_RUNNING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_RUNNING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_INPUTS.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_INPUTS.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STAGING_JOB.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STAGING_JOB.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_STOPPED.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_STOPPED.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_SUBMITTING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_SUBMITTING.name()); 
        
        _exceptionThrown = false;
        try {_jsm.onEvent(_entity, JobFSMEvents.TO_PENDING.name());}
        catch (IllegalStateException e){_exceptionThrown = true;}
        Assert.assertTrue(_exceptionThrown, "Illegal transition " + JobFSMEvents.TO_PENDING.name()); 
    }

}
