/**
 * 
 */
package org.iplantc.service.apps.queue.actions;

import java.nio.channels.ClosedByInterruptException;

import org.iplantc.service.common.exceptions.DependencyException;
import org.iplantc.service.common.exceptions.DomainException;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.exceptions.SystemUnknownException;
import org.iplantc.service.transfer.URLCopy;

/**
 * @author dooley
 *
 */
public interface WorkerAction<T> {

    /**
     * This method performs the actual task described by
     * this action. That may include staging data, invoking
     * processes, etc.
     * 
     * @param job
     * @return 
     * @throws SystemUnavailableException
     * @throws SystemUnknownException
     * @throws PermissionException 
     * @throws JobException
     * @throws JobDependencyException 
     */
    public void run() 
    throws SystemUnavailableException, SystemUnknownException, ClosedByInterruptException, DomainException, DependencyException, PermissionException;
    
    public boolean isStopped();

    /**
     * @param stopped the stopped to set
     */
    public void setStopped(boolean stopped);

    /**
     * @return the entity upon which the worker is acting
     */
    public T getEntity();

    /**
     * @param entity the the entity upon which the worker is acting
     */
    public void setEntity(T entity);

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
