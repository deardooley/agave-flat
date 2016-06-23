/**
 *
 */
package org.iplantc.service.transfer.operations;

import org.apache.log4j.Logger;
import org.iplantc.service.transfer.RemoteDataClient;
import org.testng.annotations.Test;

/**
 * Coverage tests for {@link RemoteDataClient#getURLForPath}
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"geturlforpath"})
public class GetURLForPathOperationTest extends BaseRemoteDataClientOperationTest {

    public GetURLForPathOperationTest(String systemJsonFilePath,
            TransferOperationAfterMethod afterMethodTeardown,
            TransferOperationBeforeMethod beforeMethodSetup,
            ForbiddenPathProvider forbiddenPathProvider)
    {
        super(systemJsonFilePath, afterMethodTeardown, beforeMethodSetup, forbiddenPathProvider);
    }

    private static final Logger log = Logger.getLogger(GetURLForPathOperationTest.class);

    public void getUrlForPath()
    {
        _getUrlForPath();
    }

    protected void _getUrlForPath() {}

}
