/**
 *
 */
package org.iplantc.service.transfer.operations;

import org.apache.log4j.Logger;
import org.iplantc.service.transfer.RemoteDataClient;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Coverage tests for {@link RemoteDataClient#isThirdPartyTransferSupportedOperationTest()}.
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"proxy","integration"})
public class IsThirdPartyTransferSupportedOperationTest extends BaseRemoteDataClientOperationTest {

    public IsThirdPartyTransferSupportedOperationTest(String systemJsonFilePath,
            TransferOperationAfterMethod afterMethodTeardown,
            TransferOperationBeforeMethod beforeMethodSetup,
            ForbiddenPathProvider forbiddenPathProvider)
    {
        super(systemJsonFilePath, afterMethodTeardown, beforeMethodSetup, forbiddenPathProvider);
    }

    private static final Logger log = Logger.getLogger(IsThirdPartyTransferSupportedOperationTest.class);

    @AfterClass
    @Override
    protected void afterClass() throws Exception 
    {
        try {
//            clearSystems();
        }
        finally {
            try { getClient().disconnect(); } catch (Exception e) {}
        }
    }
    
    @BeforeMethod
    @Override
    protected void beforeMethod() throws Exception 
    {
        // no remote call is needed here
    }
    
    @AfterMethod
    @Override
    protected void afterMethod() throws Exception 
    {
     // no remote call is needed here
    }
    
    public void isThirdPartyTransferSupported()
    {
        _isThirdPartyTransferSupported();
    }

    protected void _isThirdPartyTransferSupported() {
        Assert.assertFalse(getClient().isThirdPartyTransferSupported(),
                "Third party transfer should not be supported by default in most protocols.");
    }

}
