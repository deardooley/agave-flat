/**
 * 
 */
package org.iplantc.service.io.manager.actions;

import java.nio.channels.ClosedByInterruptException;

import org.iplantc.service.common.exceptions.DependencyException;
import org.iplantc.service.common.exceptions.DomainException;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.exceptions.SystemUnknownException;
import org.iplantc.service.transfer.RemoteDataClient;

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
    throws SystemUnavailableException, SystemUnknownException, ClosedByInterruptException, 
    	   DomainException, DependencyException, PermissionException;
    
    public boolean isStopped();

    /**
     * @param stopped the stopped to set
     */
    public void setStopped(boolean stopped);

    /**
     * @return the entity upon which the worker is acting
     */
    public T getTask();

    /**
     * @param entity the the entity upon which the worker is acting
     */
    public void setTask(T entity);

    /**
     * @return the {@link RemoteDataClient} to the {@link DataTask#getSourceSystemId()}
     */
    public RemoteDataClient getSourceClient();

    /**
     * @param sourceClient the {@link RemoteDataClient} to the {@link DataTask#getSourceSystemId()}
     */
    public void setSourceClient(RemoteDataClient sourceClient);

    /**
     * Throws an exception if {@link #isStopped()} returns true
     * @throws ClosedByInterruptException
     */
    void checkStopped() throws ClosedByInterruptException;
    
}
