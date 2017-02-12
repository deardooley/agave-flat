package org.iplantc.service.jobs.phases.workers;

import java.nio.channels.ClosedByInterruptException;

import org.iplantc.service.jobs.exceptions.JobFinishedException;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;

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
     *      checkStopped(false, null)
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
     * Interrupts can happen in two ways.  First, an administrative command may 
     * directly interrupt a worker thread using Thread.interrupt().  In this case,  
     * a ClosedByInterruptException exception is thrown and the INTERRUPTED thread
     * is responsible for leaving its job (if it has one) in a consistent state. 
     * If the caller passes in a non-null newStatus parameter, this method will
     * attempt to transition to that status.  Failed transitions will be logged
     * but not surfaced to the caller.
     * 
     * Second, a user-initiated interrupt may have been received by the topic thread.
     * In this case, the INTERRUPTING thread is responsible for setting the job
     * status to some finished state before queuing the interrupt message.  A
     * JobFinishedException exception is thrown to indicate to the worker thread
     * that the job status has been updated.
     * 
     * @param logException true causes this method to log before throwing an exception
     * @param newStatus the status to transition to on ClosedByInterruptException exceptions
     * @throws ClosedByInterruptException when the worker thread has been interrupted
     * @throws JobFinishedException when the job has transitioned to a finished state
     */
    void checkStopped(boolean logException, JobStatusType newStatus) 
     throws ClosedByInterruptException, JobFinishedException;
    
    /* ---------------------------------------------------------------------- */
    /* isJobExecutionSuspended:                                               */
    /* ---------------------------------------------------------------------- */
    /** Determine if the job was explicitly stopped using the topic interrupt
     * mechanism.  If so, we set a sticky flag so that subsequent checks can
     * quickly discover that the job was moved into a stopped state.
     * 
     * @return true if the job is in a finished state; false otherwise.
     */
    boolean isJobExecutionSuspended();
}
