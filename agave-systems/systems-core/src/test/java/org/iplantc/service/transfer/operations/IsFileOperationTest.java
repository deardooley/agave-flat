/**
 *
 */
package org.iplantc.service.transfer.operations;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.log4j.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.iplantc.service.transfer.RemoteDataClient;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.io.Files;

/**
 * Coverage tests for {@link RemoteDataClient#doesExist()}.
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"isfile"})
public class IsFileOperationTest extends BaseRemoteDataClientOperationTest {

    private static final Logger log = Logger.getLogger(IsFileOperationTest.class);

    public IsFileOperationTest(String systemJsonFilePath,
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

    public void isFileFalseForDirectory()
    {
        _isFileFalseForDirectory();
    }

    protected void _isFileFalseForDirectory()
    {
        try
        {
            Assert.assertFalse(getClient().isFile("/"),
                    "root should exist and return false.");

            Assert.assertFalse(getClient().isFile(null),
                    "null remoteDirectory to get should resolve to the home directory and return false.");

            Assert.assertFalse(getClient().isFile(""),
                    "empty remoteDirectory to get should resolve to the home directory and return false.");

            getClient().mkdir("." + LOCAL_DIR_NAME);

            Assert.assertFalse(getClient().isFile("." + LOCAL_DIR_NAME),
                    "isFile should return false for shadow directories");
        }
        catch (Exception e) {
            Assert.fail("isFile should not throw unexpected exceptions", e);
        }
        finally {
            try {
                getClient().delete("." + LOCAL_DIR_NAME);
            } catch (Exception e) {
                Assert.fail("Failed to clean up remote shadow directory after test");
            }
        }
    }

    @Test(dependsOnMethods={"isFileFalseForDirectory"})
    public void isFileTrueForFile()
    {
        _isFileTrueForFile();
    }

    protected void _isFileTrueForFile()
    {
        File tmpFile = null;
        try
        {
            getClient().put(LOCAL_TXT_FILE, "");
            Assert.assertTrue(getClient().isFile(LOCAL_TXT_FILE_NAME),
                    "isFile should return true for standard file");

            tmpFile = new File(Files.createTempDir(), "." + LOCAL_TXT_FILE_NAME);
            FileUtils.copyFile(new File(LOCAL_TXT_FILE), tmpFile);

            getClient().put(tmpFile.getAbsolutePath(), "");
            Assert.assertTrue(getClient().isFile("." + LOCAL_TXT_FILE_NAME),
                    "isFile should return true for shadow file");
        }
        catch (Exception e) {
            Assert.fail("isFile should not throw unexpected exceptions", e);
        }
        finally {
            try {
                if (tmpFile != null) FileUtils.deleteDirectory(tmpFile.getParentFile());

                getClient().delete(LOCAL_TXT_FILE_NAME);
                getClient().delete("." + LOCAL_TXT_FILE_NAME);
            } catch (Exception e) {
                Assert.fail("Failed to clean up remote files after test", e);
            }
        }
    }

    @Test(dependsOnMethods={"isFileTrueForFile"})
    public void isFileThrowsExceptionForMissingPath()
    {
        _isFileThrowsExceptionForMissingPath();
    }

    protected void _isFileThrowsExceptionForMissingPath()
    {
        try {
            getClient().isFile(MISSING_FILE);
            Assert.fail("Non-existent file should throw exception");
        } catch (FileNotFoundException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.fail("Non-existent file should throw exception", e);
        }
    }

}
