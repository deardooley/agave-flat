/**
 *
 */
package org.iplantc.service.transfer.operations;

import java.io.FileNotFoundException;

import org.apache.log4j.Logger;
import org.iplantc.service.transfer.RemoteDataClient;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Coverage tests for {@link RemoteDataClient#doesExist()}.
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"isdirectory"})
public class IsDirectoryOperationTest extends BaseRemoteDataClientOperationTest {

    private static final Logger log = Logger.getLogger(IsDirectoryOperationTest.class);

    public IsDirectoryOperationTest(String systemJsonFilePath,
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

    public void isDirectoryTrueForDirectory()
    {
        _isDirectoryTrueForDirectory();
    }

    protected void _isDirectoryTrueForDirectory()
    {

        try
        {
            Assert.assertTrue(getClient().isDirectory("/"),
                    "root should exist and return true.");

            Assert.assertTrue(getClient().isDirectory(null),
                    "null remoteDirectory to get should resolve to the home directory and return true.");

            Assert.assertTrue(getClient().isDirectory(""),
                    "empty remoteDirectory to get should resolve to the home directory and return true.");

            Assert.assertTrue(getClient().mkdir("." + LOCAL_DIR_NAME),
                    "Failed to create unique shadow directory to test isDirectory");

            Assert.assertTrue(getClient().isDirectory("." + LOCAL_DIR_NAME),
                    "isDirecotry should return true for shadow directories.");
        }
        catch (Exception e) {
            Assert.fail("isDirectory should not throw unexpected exceptions", e);
        }
        finally {
            try {
                getClient().delete("." + LOCAL_DIR_NAME);
            } catch (Exception e) {
                Assert.fail("Failed to clean up remote shadow directory after test");
            }
        }
    }

    @Test(dependsOnMethods={"isDirectoryTrueForDirectory"})
    public void isDirectoryFalseForFile()
    {
        _isDirectoryFalseForFile();
    }

    protected void _isDirectoryFalseForFile()
    {
        try
        {
            getClient().put(LOCAL_TXT_FILE, "");
            Assert.assertFalse(getClient().isDirectory(LOCAL_TXT_FILE_NAME),
                    "isDirectory should return false for standard file");

            getClient().put(LOCAL_TXT_FILE, "." + LOCAL_TXT_FILE_NAME);
            Assert.assertFalse(getClient().isDirectory("." + LOCAL_TXT_FILE_NAME),
                    "isDirectory should return false for shadow file");
        }
        catch (Exception e) {
            Assert.fail("isDirectory should not throw unexpected exceptions", e);
        }
        finally {
            try {
                getClient().delete(LOCAL_TXT_FILE_NAME);
                getClient().delete("." + LOCAL_TXT_FILE_NAME);
            } catch (Exception e) {
                Assert.fail("Failed to clean up remote files after test");
            }
        }
    }

    @Test(dependsOnMethods={"isDirectoryFalseForFile"})
    public void isDirectoryThrowsExceptionForMissingPath()
    {
        _isDirectoryThrowsExceptionForMissingPath();
    }

    protected void _isDirectoryThrowsExceptionForMissingPath()
    {
        try {
            getClient().isDirectory(MISSING_DIRECTORY);
            Assert.fail("Non-existent folder should throw exception");
        } catch (FileNotFoundException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.fail("Non-existent folder should throw exception", e);
        }
    }
}
