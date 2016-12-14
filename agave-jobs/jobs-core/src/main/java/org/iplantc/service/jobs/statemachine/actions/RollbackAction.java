package org.iplantc.service.jobs.statemachine.actions;

import org.apache.log4j.Logger;
import org.iplantc.service.jobs.statemachine.IJobFSMStatefulEntity;

public class RollbackAction<T extends IJobFSMStatefulEntity> 
 extends NoopAction<T> 
{
    
    /* ********************************************************************** */
    /*                                Constants                               */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(RollbackAction.class);

    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* Execute:                                                               */
    /* ---------------------------------------------------------------------- */
    /** Events may cause the current state to transition to a new state, but
     * the consequences of that transition are handled by the onEvent caller.
     * Thus, events merely validate whether a transition is legal from the 
     * current state.
     */
    @Override
    public void execute(T stateful, 
                        String event, 
                        Object ... args) 
    {
        super.execute(stateful, event, args);
    }   
}
