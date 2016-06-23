/**
 * 
 */
package org.iplantc.service.jobs.queue.actions;

import java.nio.channels.ClosedByInterruptException;

import org.iplantc.service.jobs.exceptions.JobDependencyException;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.exceptions.SystemUnknownException;
import org.iplantc.service.transfer.URLCopy;

/**
 * @author dooley
 *
 */
public interface WorkerAction {

    /**
     * This method performs the actual task described by
     * this action. That may include staging data, invoking
     * processes, etc.
     * 
     * @param job
     * @return 
     * @throws SystemUnavailableException
     * @throws SystemUnknownException
     * @throws JobException
     * @throws JobDependencyException 
     */
    public void run() 
    throws SystemUnavailableException, SystemUnknownException, JobException, ClosedByInterruptException, JobDependencyException;
    
    public boolean isStopped();

    /**
     * @param stopped the stopped to set
     */
    public void setStopped(boolean stopped);

    /**
     * @return the job
     */
    public Job getJob();

    /**
     * @param job the job to set
     */
    public void setJob(Job job);

    /**
     * @return the urlCopy
     */
    public URLCopy getUrlCopy();

    /**
     * @param urlCopy the urlCopy to set
     */
    public void setUrlCopy(URLCopy urlCopy);

    /**
     * Throws an exception if {@link #isStopped()} returns true
     * @throws ClosedByInterruptException
     */
    void checkStopped() throws ClosedByInterruptException;
    
}
