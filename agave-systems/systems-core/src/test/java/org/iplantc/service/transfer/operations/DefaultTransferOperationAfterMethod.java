/**
 * 
 */
package org.iplantc.service.transfer.operations;

import org.iplantc.service.transfer.RemoteDataClient;

/**
 * @author dooley
 *
 */
public class DefaultTransferOperationAfterMethod implements TransferOperationAfterMethod {

    /**
     * 
     */
    public DefaultTransferOperationAfterMethod() {
    }

    /* (non-Javadoc)
     * @see org.iplantc.service.transfer.operations.TransferOperationAfterMethod#afterMethod(org.iplantc.service.transfer.RemoteDataClient)
     */
    @Override
    public void teardown(RemoteDataClient client) {
    }

}
