/**
 * 
 */
package org.iplantc.service.transfer.s3.operations;

import org.iplantc.service.transfer.BaseTransferTestCase;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.operations.ForbiddenPathProvider;
import org.iplantc.service.transfer.operations.TransferOperationTestFactory;

/**
 * @author dooley
 *
 */
public class S3TransferOperationTestFactory extends TransferOperationTestFactory {

    /* (non-Javadoc)
     * @see org.iplantc.service.transfer.operations.TransferOperationTestFactory#getSystemJsonFilePath()
     */
    @Override
    public String getSystemJsonFilePath() {
        return BaseTransferTestCase.STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "s3.example.com.json";
    }

    @Override
    public ForbiddenPathProvider getForbiddenPathProvider() {
        return new ForbiddenPathProvider() {

            @Override
            public String getFilePath(boolean shouldExist) 
            throws RemoteDataException 
            {
                if (shouldExist) {
                    throw new RemoteDataException("Bypassing test for s3 forbidden file");
                } else {
                    throw new RemoteDataException("Bypassing test for S3 missing file");
                }
            }

            @Override
            public String getDirectoryPath(boolean shouldExist)
            throws RemoteDataException 
            {
                if (shouldExist) {
                    throw new RemoteDataException("Bypassing test for s3 forbidden folder");
                } else {
                    throw new RemoteDataException("Bypassing test for S3 missing folder");
                }
            }
            
        };
    }

}
