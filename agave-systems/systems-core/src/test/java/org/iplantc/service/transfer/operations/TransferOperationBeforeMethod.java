/**
 * 
 */
package org.iplantc.service.transfer.operations;

import org.iplantc.service.transfer.RemoteDataClient;
import org.testng.annotations.BeforeMethod;

/**
 * Implements {@link BeforeMethod} behavoir for a {@link BaseRemoteDataClientOperationTest}
 * @author dooley
 *
 */
public interface TransferOperationBeforeMethod {
    
    /**
     * Implements any cleanup and data initialization for each {@link BaseRemoteDataClientOperationTest} 
     * @param client the {@link RemoteDataClient} used in the test.
     * @throws Exception
     */
    public void setup(RemoteDataClient client) throws Exception;
}
