package org.iplantc.service.jobs.phases.utils;

import java.util.List;

import org.apache.log4j.Logger;
import org.iplantc.service.jobs.dao.JobInterruptDao;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobInterrupt;
import org.iplantc.service.jobs.model.enumerations.JobInterruptType;

/** Collection of utilities related to interrupt messages writing to 
 * the scheduler topic.
 * 
 * @author rcardone
 *
 */
public class JobInterruptUtils
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(JobInterruptUtils.class);
    
    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* isJobInterrupted:                                                      */
    /* ---------------------------------------------------------------------- */
    /** See if the specified job has any outstanding interrupts.  If so, 
     * process each one.  If not, let job processing continue normally by 
     * returning false.  Return true if job processing should be immediately
     * discontinued.
     * 
     * @param job the target job
     * @return true if interrupts were processed and normal job execution should
     *          be aborted; false if normal job processing should continue.
     */
    public static boolean isJobInterrupted(Job job)
    {
        // Check if this job has any outstanding interrupts.
        List<JobInterrupt> interrupts = null;
        try {interrupts = JobInterruptDao.getInterrupts(job.getUuid(), job.getTenantId());}
        catch (Exception e) {
            String msg = "Worker " + Thread.currentThread().getName() +
                         " is unable to check for interrupts for job " + job.getUuid() +
                         " (" + job.getName() + ").";
            _log.error(msg, e);
            return false;
        }
        
        // The job has not been interrupted.
        if (interrupts.isEmpty()) return false;
        
        // Process all interrupts in creation order (i.e., list order).
        return processInterrupts(job, interrupts);
    }
    
    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* processInterrupts:                                                     */
    /* ---------------------------------------------------------------------- */
    /** Process a list of interrupts.  All interrupts have the effect of causing
     * the worker thread to immediately stop processing the specified job.  Some
     * interrupt put the job into a finished state, others reset the job to a
     * different status so that process can continue at a later time.
     * 
     * @param job the job that was interrupted
     * @param interrupts the ordered list of interrupts
     * @return true if job processing should be discontinued by the worker thread;
     *         false if the worker can continue processing the job
     */
    private static boolean processInterrupts(Job job, List<JobInterrupt> interrupts)
    {
        // Assume we won't abort normal job processing.
        boolean abortProcessing = false;
        
        // Process each interrupt.
        for (JobInterrupt interrupt : interrupts)
        {
            // We only process known asynchronous interrupt types and those should 
            // be the only ones passed in, but we double check anyway.
            JobInterruptType interruptType = interrupt.getInterruptType();
            if (interruptType != JobInterruptType.DELETE &&
                interruptType != JobInterruptType.PAUSE  &&
                interruptType != JobInterruptType.STOP )
            {
                _log.error("Invalid job interrupt type received: " + interruptType);
                continue;
            }
            
            // We abort processing this job on any recognized interrupt type.
            abortProcessing |= true;
            
            // Remove the interrupt.
            int rows = 0;
            try {rows = JobInterruptDao.deleteInterrupt(interrupt.getId(), interrupt.getTenantId());}
            catch (Exception e) {
                String msg = "Failed to delete interrupt with id = " + interrupt.getId() +
                             " and tenant id = " + interrupt.getTenantId() +
                             " for job " + job.getUuid() + " (" + job.getName() + ").";
                _log.error(msg, e);
                continue;
            }
            
            // We should have deleted the interrupt, but it possible that the
            // interrupt had expired and had already been deleted.
            if (rows != 1) {
                String msg = "Interrupt with id = " + interrupt.getId() +
                             " and tenant id = " + interrupt.getTenantId() +
                             " not found for deletion. No problem if already deleted.";
                _log.warn(msg);
            }
        }
        
        return abortProcessing;
    }
}
