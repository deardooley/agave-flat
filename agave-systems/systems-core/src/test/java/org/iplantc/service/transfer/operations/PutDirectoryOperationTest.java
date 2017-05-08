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
@Test(singleThreaded=true, groups= {"putdirectory", "upload", "integration"})//, dependsOnGroups={"mkdir"}
public class PutDirectoryOperationTest extends BaseRemoteDataClientOperationTest {

    public PutDirectoryOperationTest(String systemJsonFilePath,
            TransferOperationAfterMethod afterMethodTeardown,
            TransferOperationBeforeMethod beforeMethodSetup,
            ForbiddenPathProvider forbiddenPathProvider)
    {
        super(systemJsonFilePath, afterMethodTeardown, beforeMethodSetup, forbiddenPathProvider);
    }

    private static final Logger log = Logger.getLogger(PutDirectoryOperationTest.class);

    @DataProvider(name="putFolderProvider", parallel=false)
    protected Object[][] putFolderProvider()
    {
        return new Object[][] {
            // remote dest path,   expected result path,   exception?, message
            { LOCAL_DIR_NAME,       LOCAL_DIR_NAME,         false,     "put local file to remote home directory explicitly setting the identical name should result in folder with same name on remote system." },
            { "somedir",            "somedir",              false,     "put local file to remote home directory explicitly setting a new name should result in folder with new name on remote system." },
            { "",                   LOCAL_DIR_NAME,         false,     "put local directory to empty (home) remote directory without setting a new name should result in folder with same name on remote system." },

        };
    }

    @Test(dataProvider="putFolderProvider")
    public void putFolderCreatesRemoteFolder(String remotePath, String expectedRemoteFilename, boolean shouldThrowException, String message)
    {
        _putFolderCreatesRemoteFolder(remotePath, expectedRemoteFilename, shouldThrowException, message);
    }

    protected void _putFolderCreatesRemoteFolder(String remotePath, String expectedRemoteFilename, boolean shouldThrowException, String message)
    {
        try
        {
            getClient().put(LOCAL_DIR, remotePath);

            Assert.assertTrue(getClient().doesExist(FilenameUtils.getName(expectedRemoteFilename)),
                    "Expected destination " + expectedRemoteFilename + " does not exist after put file");

            for(File child: new File(LOCAL_DIR).listFiles()) {
                Assert.assertTrue(getClient().doesExist(expectedRemoteFilename + "/" + child.getName()),
                        "Expected uploaded folder content " + expectedRemoteFilename + "/" + child.getName() +
                        " does not exist after put file");
            }
        }
        catch (Exception e)
        {
            Assert.fail("Put of file or folder should not throw exception.", e);
        }
    }

    @Test(dependsOnMethods={"putFolderCreatesRemoteFolder"})
    public void putFolderCreatesRemoteSubFolderWhenNamedRemotePathExists()
    {
        _putFolderCreatesRemoteSubFolderWhenNamedRemotePathExists();
    }

    protected void _putFolderCreatesRemoteSubFolderWhenNamedRemotePathExists()
    {
        try
        {
            getClient().mkdirs(LOCAL_DIR_NAME);

            getClient().put(LOCAL_DIR, LOCAL_DIR_NAME);

            Assert.assertTrue(getClient().doesExist(LOCAL_DIR_NAME + File.separator + LOCAL_DIR_NAME),
                    "Expected destination " + LOCAL_DIR_NAME + File.separator + LOCAL_DIR_NAME
                    + " does not exist after put file");
        }
        catch (Exception e)
        {
            Assert.fail("Put of file or folder should not throw exception.", e);
        }
    }

    @Test(dependsOnMethods={"putFolderCreatesRemoteSubFolderWhenNamedRemotePathExists"})
    public void putFolderMergesContentsWhenRemoteFolderExists()
    {
        _putFolderMergesContentsWhenRemoteFolderExists();
    }

    protected void _putFolderMergesContentsWhenRemoteFolderExists()
    {
        String remoteDir = UUID.randomUUID().toString();
        try
        {
            getClient().mkdirs(remoteDir + "foo/bar");
            Assert.assertTrue(getClient().doesExist(remoteDir + "foo/bar"),
                    "Failed to create test directory for put merge test.");

            getClient().put(LOCAL_BINARY_FILE, remoteDir + "foo/file.dat");
            Assert.assertTrue(getClient().doesExist(remoteDir + "foo/file.dat"),
                    "Failed to upload test file for put merge test.");

            getClient().put(LOCAL_DIR, remoteDir);

            Assert.assertTrue(getClient().doesExist(remoteDir + "foo/bar"),
                    "Remote directory was deleted during put of folder with non-overlapping file trees.");
            Assert.assertTrue(getClient().doesExist(remoteDir + "foo/file.dat"),
                    "Remote file was was deleted during put of folder with non-overlapping file trees.");
        }
        catch (Exception e)
        {
            Assert.fail("Put of file or folder should not throw exception.", e);
        }
    }

    @Test(dependsOnMethods={"putFolderMergesContentsWhenRemoteFolderExists"})
    public void putFolderWithoutRemotePermissionThrowsRemoteDataException()
    {
        _putFolderWithoutRemotePermissionThrowsRemoteDataException();
    }

    protected void _putFolderWithoutRemotePermissionThrowsRemoteDataException()
    {
        try
        {
            getClient().put(LOCAL_DIR, getForbiddenDirectoryPath(false));
            Assert.fail("Writing to file or folder without permission "
                    + "on the remote system should throw RemoteDataException");
        }
        catch (IOException | RemoteDataException e)
        {
            Assert.assertTrue(true);
        }
    }

    @Test(dataProvider="putFolderProvider", dependsOnMethods= {"putFolderWithoutRemotePermissionThrowsRemoteDataException"})
    public void putFolderOutsideHome(String remoteFilename, String expectedRemoteFilename, boolean shouldThrowException, String message)
    {
        _putFolderOutsideHome(remoteFilename, expectedRemoteFilename, shouldThrowException, message);
    }

    protected void _putFolderOutsideHome(String remoteFilename, String expectedRemoteFilename, boolean shouldThrowException, String message)
    {
        try
        {
            String remoteDir = "put/File/Outside/Home/";
            getClient().mkdirs(remoteDir);

            String remoteFilePath = remoteDir + remoteFilename;

            getClient().put(LOCAL_DIR, remoteFilePath);

            Assert.assertTrue(getClient().doesExist(remoteDir + FilenameUtils.getName(expectedRemoteFilename)),
                    "Expected destination " + remoteDir + FilenameUtils.getName(expectedRemoteFilename) + " does not exist after put file");
        }
        catch (Exception e)
        {
            Assert.fail("Put of file or folder should not throw exception.", e);
        }
    }

    @Test(dependsOnMethods={"putFolderOutsideHome"})
    public void putFolderFailsToMissingDestinationPath()
    {
        _putFolderFailsToMissingDestinationPath();
    }

    protected void _putFolderFailsToMissingDestinationPath()
    {
        try
        {
            getClient().put(LOCAL_DIR, MISSING_DIRECTORY);

            Assert.fail("Put folder to a remote directory that does not exist should throw FileNotFoundException.");
        }
        catch (FileNotFoundException e) {
            Assert.assertTrue(true);
        }
        catch (Exception e)
        {
            Assert.fail("Put folder to a local directory that does not exist should throw FileNotFoundException.");
        }
    }

    @Test(dependsOnMethods={"putFolderFailsToMissingDestinationPath"})
    public void putFailsForMissingLocalDirectoryPath()
    {
        _putFailsForMissingLocalDirectoryPath();
    }

    protected void _putFailsForMissingLocalDirectoryPath()
    {
        try
        {
            getClient().put(MISSING_DIRECTORY, "");

            Assert.fail("Put on missing local folder should throw FileNotFoundException.");
        }
        catch (FileNotFoundException e) {
            Assert.assertTrue(true);
        }
        catch (Exception e)
        {
            Assert.fail("Put on missing local folder should throw FileNotFoundException.");
        }
    }

    @Test(dependsOnMethods={"putFailsForMissingLocalDirectoryPath"})
    public void putFolderFailsToRemoteFilePath()
    {
        _putFolderFailsToRemoteFilePath();
    }

    protected void _putFolderFailsToRemoteFilePath()
    {
        try
        {
            getClient().put(LOCAL_BINARY_FILE, "");
            getClient().put(LOCAL_DIR, LOCAL_BINARY_FILE_NAME);

            Assert.fail("Put folder to a local directory that does not exist should throw FileNotFoundException.");
        }
        catch (IOException e) {
            Assert.fail("Put folder to path of remote file should throw RemoteDataException.", e);
        }
        catch (RemoteDataException e)
        {
            Assert.assertTrue(true);
        }
    }
}
