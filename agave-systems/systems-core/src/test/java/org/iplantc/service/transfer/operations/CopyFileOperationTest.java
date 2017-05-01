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
 * Coverage tests for {@link RemoteDataClient#copy} on a file
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"copyfile","integration"})
public class CopyFileOperationTest extends BaseRemoteDataClientOperationTest {

    public CopyFileOperationTest(String systemJsonFilePath,
            TransferOperationAfterMethod afterMethodTeardown,
            TransferOperationBeforeMethod beforeMethodSetup,
            ForbiddenPathProvider forbiddenPathProvider)
    {
        super(systemJsonFilePath, afterMethodTeardown, beforeMethodSetup, forbiddenPathProvider);
    }

    private static final Logger log = Logger.getLogger(CopyFileOperationTest.class);

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

    public void copyFile()
    {
        _copyFile();
    }

    protected void _copyFile()
    {
        String remoteSrc = LOCAL_BINARY_FILE_NAME;
        String remoteDest = LOCAL_BINARY_FILE_NAME + ".copy";

        try
        {
            getClient().put(LOCAL_BINARY_FILE, "");
            Assert.assertTrue(getClient().doesExist(remoteSrc), "Failed to upload source file");

            getClient().copy(remoteSrc, remoteDest);

            Assert.assertTrue(getClient().doesExist(remoteDest), "Copy operation failed from " +
                    remoteSrc + " to " + remoteDest);
        }
        catch (Exception e) {
            Assert.fail("Copy operation failed from " +
                remoteSrc + " to " + remoteDest, e);
        }
    }

    @Test(dependsOnMethods={"copyFile"})
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
