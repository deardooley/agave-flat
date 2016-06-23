/**
 * 
 */
package org.iplantc.service.transfer.sftp.operations;

import org.iplantc.service.transfer.BaseTransferTestCase;
import org.iplantc.service.transfer.operations.DefaultForbiddenPathProvider;
import org.iplantc.service.transfer.operations.ForbiddenPathProvider;
import org.iplantc.service.transfer.operations.TransferOperationTestFactory;

/**
 * @author dooley
 *
 */
public class SftpPasswordTransferOperationTestFactory extends TransferOperationTestFactory {

    /* (non-Javadoc)
     * @see org.iplantc.service.transfer.operations.TransferOperationTestFactory#getSystemJsonFilePath()
     */
    @Override
    public String getSystemJsonFilePath() {
        return BaseTransferTestCase.STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "sftp-password.example.com.json";
    }

    @Override
    public ForbiddenPathProvider getForbiddenPathProvider() {
        return new DefaultForbiddenPathProvider();
    }

}
