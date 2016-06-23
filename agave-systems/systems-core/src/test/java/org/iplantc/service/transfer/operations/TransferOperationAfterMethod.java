/**
 * 
 */
package org.iplantc.service.transfer.operations;

import org.iplantc.service.transfer.RemoteDataClient;
import org.testng.annotations.AfterMethod;

/**
 * Implements {@link AfterMethod} behavoir for a {@ BaseRemoteDataClientOperationTest}
 * @author dooley
 *
 */
public interface TransferOperationAfterMethod {
    
    public void teardown(RemoteDataClient client);
}
