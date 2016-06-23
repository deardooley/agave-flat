/**
 *
 */
package org.iplantc.service.transfer.operations;

import java.io.FileNotFoundException;

import org.apache.log4j.Logger;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Coverage tests for {@link RemoteDataClient#copy} on a directory
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"copydirectory"})
public class CopyDirectoryOperationTest extends BaseRemoteDataClientOperationTest {

    private static final Logger log = Logger.getLogger(CopyDirectoryOperationTest.class);

    public CopyDirectoryOperationTest(String systemJsonFilePath,
            TransferOperationAfterMethod afterMethodTeardown,
            TransferOperationBeforeMethod beforeMethodSetup,
            ForbiddenPathProvider forbiddenPathProvider)
    {
        super(systemJsonFilePath, afterMethodTeardown, beforeMethodSetup, forbiddenPathProvider);
    }

    @DataProvider(name = "copyIgnoreSlashesProvider", parallel=false)
    protected Object[][] copyIgnoreSlashesProvider() throws Exception {
        return new Object[][] {
                { "foo", "bar", false, "bar", false, "foo => bar = bar when bar !exists" },
                { "foo/", "bar", false, "bar", false, "foo/ => bar = bar when bar !exists" },
                { "foo", "bar/", false, "bar", false, "foo => bar/ = bar/foo when bar !exists" },
                { "foo/", "bar/", false, "bar", false, "foo/ => bar/ = bar when bar !exists" },

                { "foo", "bar", true, "bar", false, "foo => bar = bar/foo when bar exists" },
                { "foo/", "bar", true, "bar", false, "foo/ => bar = bar when bar exists" },
                { "foo", "bar/", true, "bar", false, "foo => bar/ = bar/foo when bar exists" },
                { "foo/", "bar/", true, "bar", false, "foo/ => bar/ = bar when bar exists" }
        };
    }

    @Test(dataProvider="copyIgnoreSlashesProvider")
    public void copyDir(String src, String dest, boolean createDest, String expectedPath, boolean shouldThrowException, String message)
    {
        _copyDir(src, dest, createDest, expectedPath, shouldThrowException, message);
    }

    protected void _copyDir(String src, String dest, boolean createDest, String expectedPath, boolean shouldThrowException, String message)
    {
        try
        {
            getClient().mkdir(src);
            Assert.assertTrue(getClient().doesExist(src), "Failed to create source directory");

            if (createDest) {
                getClient().mkdir(dest);
                Assert.assertTrue(getClient().doesExist(dest), "Failed to create dest directory");
            }

            getClient().copy(src, dest);

            Assert.assertTrue(getClient().doesExist(expectedPath), message);
        }
        catch (Exception e) {
            if (!shouldThrowException) {
                Assert.fail(message, e);
            }
        }
    }

    @Test(dependsOnMethods={"copyDir"})
    public void copyThrowsRemoteDataExceptionToRestrictedDest()
    {
        _copyThrowsRemoteDataExceptionToRestrictedDest();
    }

    protected void _copyThrowsRemoteDataExceptionToRestrictedDest()
    {
        try
        {
            getClient().mkdir("foo");
            getClient().copy("foo", getForbiddenDirectoryPath(true));

            Assert.fail("copy a remote path to a dest path the user does not have "
                    + "permission to access should throw RemoteDataException.");
           }
        catch (RemoteDataException e) {
            Assert.assertTrue(true);
        }
        catch (Exception e) {
            Assert.fail("copy a remote path to a dest path the user does not have "
                    + "permission to access should throw RemoteDataException.", e);
        }
    }

    @Test(dependsOnMethods={"copyThrowsRemoteDataExceptionToRestrictedDest"})
    public void copyThrowsFileNotFoundExceptionOnMissingDestPath()
    {
        _copyThrowsFileNotFoundExceptionOnMissingDestPath();

    }

    protected void _copyThrowsFileNotFoundExceptionOnMissingDestPath()
    {
        try
        {
            getClient().mkdir("foo");
            getClient().copy("foo", MISSING_DIRECTORY);

            Assert.fail("copy a remote source path to a dest path that does not exist should throw FileNotFoundException.");
        }
        catch (FileNotFoundException e) {
            Assert.assertTrue(true);
        }
        catch (Exception e) {
            Assert.fail("copy a remote source path to a dest path that does not exist should throw FileNotFoundException.", e);
        }
    }

    @Test(dependsOnMethods={"copyThrowsFileNotFoundExceptionOnMissingDestPath"})
    public void copyThrowsFileNotFoundExceptionOnMissingSourcePath()
    {
        _copyThrowsFileNotFoundExceptionOnMissingSourcePath();
    }

    protected void _copyThrowsFileNotFoundExceptionOnMissingSourcePath()
    {
        try
        {
            getClient().copy(MISSING_DIRECTORY, "foo");

            Assert.fail("copy a remote source path that does not exist should throw FileNotFoundException.");
        }
        catch (FileNotFoundException e) {
            Assert.assertTrue(true);
        }
        catch (Exception e) {
            Assert.fail("copy a remote source path that does not exist should throw FileNotFoundException.", e);
        }
    }

    @Test(dependsOnMethods= {"copyThrowsFileNotFoundExceptionOnMissingSourcePath"})
    public void copyThrowsRemoteDataExceptionToRestrictedSource()
    {
        _copyThrowsRemoteDataExceptionToRestrictedSource();
    }

    protected void _copyThrowsRemoteDataExceptionToRestrictedSource() {
        try
        {
            getClient().copy(MISSING_DIRECTORY, "foo");

            Assert.fail("copy a remote source path that does not exist should throw FileNotFoundException.");
        }
        catch (FileNotFoundException e) {
            Assert.assertTrue(true);
        }
        catch (Exception e) {
            Assert.fail("copy a remote source path that does not exist should throw FileNotFoundException.", e);
        }
    }

}
