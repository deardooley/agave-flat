/**
 *
 */
package org.iplantc.service.transfer.operations;

import java.io.FileNotFoundException;

import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.iplantc.service.transfer.RemoteDataClient;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Coverage tests for {@link RemoteDataClient#checksum}.
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"checksum","integration"})
public class ChecksumOperationTest extends BaseRemoteDataClientOperationTest {

    private static final Logger log = Logger.getLogger(ChecksumOperationTest.class);

    public ChecksumOperationTest(String systemJsonFilePath,
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

        getClient().mkdirs("");

        getClient().put(LOCAL_DIR, "");
        Assert.assertTrue(getClient().doesExist(LOCAL_DIR_NAME),
                "Data not found on remote system after put.");

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

    public void checksumDirectoryFails()
    {
        _checksumDirectoryFails();
    }

    protected void _checksumDirectoryFails()
    {
        try
        {
            getClient().checksum(LOCAL_DIR_NAME);
            Assert.fail("Checksum is not supported on directories.");
        }
        catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test(groups={"checksum"}, dependsOnMethods={"checksumDirectoryFails"})
    public void checksumMissingPathThrowsException()
    {
        _checksumMissingPathThrowsException();
    }

    protected void _checksumMissingPathThrowsException()
    {
        try
        {
            getClient().checksum(MISSING_DIRECTORY);
            Assert.fail("Checksum a missing path should throw a FileNotFoundException.");
        }
        catch (FileNotFoundException e) {
            Assert.assertTrue(true);
        }
        catch (Exception e) {
            Assert.fail("Checksum a missing path should throw a FileNotFoundException.");
        }
    }

    @Test(groups={"checksum"}, dependsOnMethods={"checksumMissingPathThrowsException"})
    public void checksum()
    {
        _checksum();
    }

    protected void _checksum()
    {
        try
        {
            getClient().checksum(LOCAL_DIR_NAME + "/" + LOCAL_TXT_FILE_NAME);
            Assert.fail("Checksum should throw a NotImplementedException unless overridden by a concrete test class");
        }
        catch (NotImplementedException e) {
            Assert.assertTrue(true);
        }
        catch (Exception e) {
            Assert.fail("Checksum should throw a NotImplementedException for SFTP", e);
        }
    }
}
