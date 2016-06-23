/**
 * 
 */
package org.iplantc.service.transfer.operations;

import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.exceptions.RemoteDataException;

/**
 * Represents a forbidden path generation class for use
 * in unit testing
 * @author dooley
 *
 */
public interface ForbiddenPathProvider {

    /**
     * Path to a file on a given {@link RemoteSystem} which should be 
     * inaccessible for a given {@link RemoteDataClient}.
     * 
     * @param shouldExist true if a valid, though forbidden, system path should be returned. False otherwise.
     * @return
     */
    public String getFilePath(boolean shouldExist) throws RemoteDataException;
    
    /**
     * Path to a directory on a given {@link RemoteSystem} which should be 
     * inaccessible for a given {@link RemoteDataClient}.
     * 
     * @param shouldExist true if a valid, though forbidden, system path should be returned. False otherwise.
     * @return
     */
    public String getDirectoryPath(boolean shouldExist) throws RemoteDataException;
}
