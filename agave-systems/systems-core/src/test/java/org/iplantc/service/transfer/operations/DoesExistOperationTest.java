/**
 *
 */
package org.iplantc.service.transfer.operations;

import org.apache.log4j.Logger;
import org.iplantc.service.transfer.RemoteDataClient;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Coverage tests for {@link RemoteDataClient#doesExist()}.
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"doesexist","integration"})
public class DoesExistOperationTest extends BaseRemoteDataClientOperationTest {

    private static final Logger log = Logger.getLogger(DoesExistOperationTest.class);

    public DoesExistOperationTest(String systemJsonFilePath,
            TransferOperationAfterMethod afterMethodTeardown,
            TransferOperationBeforeMethod beforeMethodSetup,
            ForbiddenPathProvider forbiddenPathProvider)
    {
        super(systemJsonFilePath, afterMethodTeardown, beforeMethodSetup, forbiddenPathProvider);
    }

    @BeforeClass
    @Override
    protected void beforeClass() throws Exception
    {
        super.beforeClass();

        getClient().authenticate();
        Assert.assertTrue(getClient().mkdirs(""),
                "Failed to create home directory.");
    }

    @AfterClass
    @Override
    protected void afterClass() throws Exception
    {
        try
        {
            // clearSystems();
        }
        catch (Exception e) {}

        try
        {
            // remove test directory
            getClient().delete("..");
            Assert.assertFalse(getClient().doesExist(""), "Failed to clean up home directory after test.");
        }
        catch (Exception e) {
            Assert.fail("Failed to clean up test home directory " + getClient().resolvePath("") + " after test method.", e);
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

    @DataProvider(name="doesExistProvider")
    protected Object[][] doesExistProvider()
    {
        return new Object[][] {
            { null, true, "null path should resolve to home and not throw an exception." },
//            { "", true, "Home directory should exist." },
//            { "..", true, system.getStorageConfig().getHomeDir()  + "/.. should exist." },
//            { "../", true, system.getStorageConfig().getHomeDir() + "/../../ should exist." },
//            { "../../..", false, system.getStorageConfig().getHomeDir() + "/../../.. should return false from doesExist." },
//            { "/", true, "Root directory should exist." },
//            { "/..", false, "/.. should return false from doesExist." },
//            { MISSING_DIRECTORY, false, "Missing directory should return false from doesExist." },
//            { MISSING_FILE, false, "Missing file should return false from doesExist." },
        };
    }

    @Test(dataProvider="doesExistProvider")
    public void doesExist(String remotedir, boolean shouldExist, String message)
    {
        _doesExist(remotedir, shouldExist, message);
    }

    protected void _doesExist(String remotedir, boolean shouldExist, String message)
    {
        try {
            boolean doesExist = getClient().doesExist(remotedir);
            Assert.assertEquals(doesExist, shouldExist, message);
        }
        catch (Exception e) {
            Assert.fail("Failed to query for existence of remote path " + remotedir, e);
        }
    }


}
