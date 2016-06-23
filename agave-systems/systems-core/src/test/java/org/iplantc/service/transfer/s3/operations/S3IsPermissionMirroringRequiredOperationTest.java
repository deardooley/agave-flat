/**
 * 
 */
package org.iplantc.service.transfer.s3.operations;

import org.apache.log4j.Logger;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.operations.ForbiddenPathProvider;
import org.iplantc.service.transfer.operations.IsPermissionMirroringRequiredOperationTest;
import org.iplantc.service.transfer.operations.TransferOperationAfterMethod;
import org.iplantc.service.transfer.operations.TransferOperationBeforeMethod;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Coverage tests for {@link RemoteDataClient#isPermissionMirroringRequired()}.
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"proxy"})
public class S3IsPermissionMirroringRequiredOperationTest extends IsPermissionMirroringRequiredOperationTest {
    
    public S3IsPermissionMirroringRequiredOperationTest(String systemJsonFilePath,
            TransferOperationAfterMethod afterMethodTeardown,
            TransferOperationBeforeMethod beforeMethodSetup,
            ForbiddenPathProvider forbiddenPathProvider)
    {
        super(systemJsonFilePath, afterMethodTeardown, beforeMethodSetup, forbiddenPathProvider);
    }

    private static final Logger log = Logger.getLogger(IsPermissionMirroringRequiredOperationTest.class);

    protected void _isPermissionMirroringRequired() {
        Assert.assertFalse(getClient().isPermissionMirroringRequired(), 
                "S3 permission mirroring should not be enabled.");
    }
    
}
