package org.iplantc.service.jobs.phases.workers;

import java.nio.channels.ClosedByInterruptException;

import org.iplantc.service.jobs.exceptions.JobFinishedException;

/** Interface that exposes worker interrupt checking routines.
 * 
 * @author rcardone
 *
 */
public interface IPhaseWorker
{
    /* ---------------------------------------------------------------------- */
    /* checkStopped:                                                          */
    /* ---------------------------------------------------------------------- */
    /** This method is called at convenient points during execution in which
     * exception processing will not leave the system in an inconsistent state.
     * This call is equivalent to calling:
     * 
     *      checkStopped(false)
     * 
     * @throws ClosedByInterruptException when the worker thread has been interrupted
     * @throws JobFinishedException when the job has transitioned to a finished state
     */
     void checkStopped() 
      throws ClosedByInterruptException, JobFinishedException;
    
    /* ---------------------------------------------------------------------- */
    /* checkStopped:                                                          */
    /* ---------------------------------------------------------------------- */
    /** This method is called at convenient points during execution in which
     * exception processing will not leave the system in an inconsistent state.
     * The logging flag causes debug records to be written to the log when an
     * exception is going to be thrown.
     * 
     * @param logException true causes this method to log before throwing an exception
     * @throws ClosedByInterruptException when the worker thread has been interrupted
     * @throws JobFinishedException when the job has transitioned to a finished state
     */
    void checkStopped(boolean logException) 
     throws ClosedByInterruptException, JobFinishedException;
    
    /* ---------------------------------------------------------------------- */
    /* isJobStopped:                                                          */
    /* ---------------------------------------------------------------------- */
    /** Determine if the job was explicitly stopped using the topic interrupt
     * mechanism.  If so, we set a sticky flag so that subsequent checks can
     * quickly discover that the job was moved into a stopped state.
     * 
     * @return true if the job is in a finished state; false otherwise.
     */
    boolean isJobStopped();
}
