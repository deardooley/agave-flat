package org.iplantc.service.jobs.statemachine;

/** Define stateful entities as those that have state.
 * 
 * @author rcardone
 */
public interface IJobFSMStatefulEntity
{
    String getState();
}
