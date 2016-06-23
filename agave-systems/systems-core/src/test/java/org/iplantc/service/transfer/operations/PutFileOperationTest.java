/**
 *
 */
package org.iplantc.service.transfer.operations;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Coverage tests for {@link RemoteDataClient#put} on a file.
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"putfile", "upload"})//, dependsOnGroups={"mkdir"}
public class PutFileOperationTest extends BaseRemoteDataClientOperationTest {

    public PutFileOperationTest(String systemJsonFilePath,
            TransferOperationAfterMethod afterMethodTeardown,
            TransferOperationBeforeMethod beforeMethodSetup,
            ForbiddenPathProvider forbiddenPathProvider)
    {
        super(systemJsonFilePath, afterMethodTeardown, beforeMethodSetup, forbiddenPathProvider);
    }

    private static final Logger log = Logger.getLogger(PutFileOperationTest.class);

    @DataProvider(name="putFileProvider", parallel=false)
    protected Object[][] putFileProvider()
    {
        // expected result of copying LOCAL_BINARY_FILE to the remote system given the following dest
        // paths
        return new Object[][] {
              // remote dest path,  expected result path,  exception?, message
            { LOCAL_TXT_FILE_NAME, LOCAL_TXT_FILE_NAME,    false,      "put local file to remote destination with different name should result in file with new name on remote system." },
            { "",                  LOCAL_BINARY_FILE_NAME, false,      "put local file to empty(home) directory name should result in file with same name on remote system" },
        };
    }


    @Test(dataProvider="putFileProvider")
    public void putFile(String remotePath, String expectedRemoteFilename, boolean shouldThrowException, String message)
    {
        _putFile(remotePath, expectedRemoteFilename, shouldThrowException, message);
    }

    protected void _putFile(String remotePath, String expectedRemoteFilename, boolean shouldThrowException, String message)
    {
        try
        {
            getClient().put(LOCAL_BINARY_FILE, remotePath);

            Assert.assertTrue(getClient().doesExist(FilenameUtils.getName(expectedRemoteFilename)),
                    "Expected destination " + expectedRemoteFilename + " does not exist after put file");
        }
        catch (Exception e)
        {
            Assert.fail("Put of file or folder should not throw exception.", e);
        }
    }

    @Test(dataProvider="putFileProvider", dependsOnMethods={"putFile"})
    public void putFileOutsideHome(String remoteFilename, String expectedRemoteFilename, boolean shouldThrowException, String message)
    {
        _putFileOutsideHome(remoteFilename, expectedRemoteFilename, shouldThrowException, message);
    }

    protected void _putFileOutsideHome(String remoteFilename, String expectedRemoteFilename, boolean shouldThrowException, String message)
    {
        try
        {
            String remoteDir = "put/File/Outside/Home";
            getClient().mkdirs(remoteDir);

            String remoteFilePath = remoteDir + File.separator + remoteFilename;

            getClient().put(LOCAL_BINARY_FILE, remoteFilePath);

            Assert.assertTrue(getClient().doesExist(remoteDir + File.separator + FilenameUtils.getName(expectedRemoteFilename)),
                    "Expected destination " + remoteDir + File.separator + expectedRemoteFilename + " does not exist after put file");
        }
        catch (Exception e)
        {
            Assert.fail("Put of file or folder should not throw exception.", e);
        }
    }

    @Test(dependsOnMethods={"putFileOutsideHome"})
    public void putFileOverwritesExistingFile()
    {
        _putFileOverwritesExistingFile();
    }

    protected void _putFileOverwritesExistingFile()
    {
        String remoteName = UUID.randomUUID().toString();
        try
        {

            getClient().put(LOCAL_BINARY_FILE, remoteName);
            Assert.assertTrue(getClient().doesExist(remoteName),
                    "Failed to put file prior to overwrite test.");

            getClient().put(LOCAL_BINARY_FILE, remoteName);
            Assert.assertTrue(getClient().doesExist(remoteName),
                    "Failed to put file prior to overwrite test.");
        }
        catch (Exception e)
        {
            Assert.fail("Overwriting file should not throw exception.", e);
        }
        finally {
            try {getClient().delete(remoteName);} catch (Exception e) {}
        }
    }

    @Test(dependsOnMethods={"putFileOverwritesExistingFile"})
    public void putFileWithoutRemotePermissionThrowsRemoteDataException()
    {
        _putFileWithoutRemotePermissionThrowsRemoteDataException();
    }

    protected void _putFileWithoutRemotePermissionThrowsRemoteDataException()
    {
        try
        {
            getClient().put(LOCAL_BINARY_FILE, getForbiddenDirectoryPath(false));
            Assert.fail("Writing to file or folder without permission "
                    + "on the remote system should throw RemoteDataException");
        }
        catch (IOException | RemoteDataException e)
        {
            Assert.assertTrue(true);
        }
    }

    @Test(dependsOnMethods={"putFileWithoutRemotePermissionThrowsRemoteDataException"})
    public void putFileFailsToMissingDestinationPath()
    {
        _putFileFailsToMissingDestinationPath();
    }

    protected void _putFileFailsToMissingDestinationPath()
    {
        try
        {
            getClient().put(LOCAL_BINARY_FILE, MISSING_DIRECTORY);

            Assert.fail("Put local file to a remote path that does not exist should throw FileNotFoundException.");
        }
        catch (FileNotFoundException e) {
            Assert.assertTrue(true);
        }
        catch (Exception e)
        {
            Assert.fail("Put local file to a remote path that does not exist should throw FileNotFoundException.");
        }
    }

    @Test(dependsOnMethods={"putFileFailsToMissingDestinationPath"})
    public void putFailsForMissingLocalFilePath()
    {
        _putFailsForMissingLocalFilePath();
    }

    protected void _putFailsForMissingLocalFilePath()
    {
        try
        {
            getClient().put(MISSING_FILE, "");

            Assert.fail("Put on missing local file should throw FileNotFoundException.");
        }
        catch (FileNotFoundException e) {
            Assert.assertTrue(true);
        }
        catch (Exception e)
        {
            Assert.fail("Put on missing local file should throw FileNotFoundException.");
        }
    }
}
