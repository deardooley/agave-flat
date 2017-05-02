/**
 *
 */
package org.iplantc.service.transfer.operations;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Coverage tests for downloading single files from a
 * remote system.
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"getdirectory", "download", "integration"})
public class GetDirectoryOperationTest extends BaseRemoteDataClientOperationTest {

    public GetDirectoryOperationTest(String systemJsonFilePath,
            TransferOperationAfterMethod afterMethodTeardown,
            TransferOperationBeforeMethod beforeMethodSetup,
            ForbiddenPathProvider forbiddenPathProvider)
    {
        super(systemJsonFilePath, afterMethodTeardown, beforeMethodSetup, forbiddenPathProvider);
    }

    private static final Logger log = Logger.getLogger(GetDirectoryOperationTest.class);

    @DataProvider(name="getDirectoryRetrievesToCorrectLocationProvider", parallel=false)
    protected Object[][] getDirectoryRetrievesToCorrectLocationProvider()
    {
        String localPath = UUID.randomUUID().toString();
        return new Object[][] {
            { localPath + "-1", true, localPath + "-1/" + LOCAL_DIR_NAME, "Downloading to existing path creates new folder in path." },
            { localPath + "-2/new_get_path", false, localPath + "-2/new_get_path" , "Downloading to non-existing target directory path downloads directory as named path." },
        };
    }

    public void getDirectoryThrowsExceptionWhenDownloadingFolderToNonExistentLocalPath()
    {
        _getDirectoryThrowsExceptionWhenDownloadingFolderToNonExistentLocalPath();
    }

    protected void _getDirectoryThrowsExceptionWhenDownloadingFolderToNonExistentLocalPath()
    {
        try
        {
            FileUtils.getFile(getLocalDownloadDir()).delete();

            getClient().put(LOCAL_DIR, "");

            getClient().get(LOCAL_DIR_NAME, getLocalDownloadDir() + File.separator + MISSING_DIRECTORY);
            Assert.fail("Getting remote folder to a local directory that does not exist should throw FileNotFoundException.");
        }
        catch (FileNotFoundException e) {
            Assert.assertTrue(true);
        }
        catch (Exception e) {
            Assert.fail("Getting remote folder to a local directory that does not exist should throw FileNotFoundException.", e);
        }
    }

    @Test(dependsOnMethods={"getDirectoryThrowsExceptionWhenDownloadingFolderToNonExistentLocalPath"})
    public void getThrowsExceptionOnMissingRemotePath()
    {
        _getThrowsExceptionOnMissingRemotePath();
    }

    protected void _getThrowsExceptionOnMissingRemotePath()
    {
        try
        {
            getClient().get(MISSING_DIRECTORY, getLocalDownloadDir());
            Assert.fail("Get on unknown remote path should throw FileNotFoundException.");
        }
        catch (FileNotFoundException e) {
            Assert.assertTrue(true);
        }
        catch (Exception e) {
            Assert.fail("Get on unknown remote path should throw FileNotFoundException", e);
        }
    }

    @Test(dependsOnMethods={"getThrowsExceptionOnMissingRemotePath"})
    public void getThrowsExceptionWhenDownloadingFolderToLocalFilePath()
    {
        _getThrowsExceptionWhenDownloadingFolderToLocalFilePath();
    }

    protected void _getThrowsExceptionWhenDownloadingFolderToLocalFilePath()
    {
        try
        {
            FileUtils.getFile(getLocalDownloadDir()).mkdirs();

            // copy the file so it's present to be overwritten without endangering our test data
            FileUtils.copyFileToDirectory(new File(LOCAL_BINARY_FILE), new File(getLocalDownloadDir()));

            getClient().put(LOCAL_DIR, "");
            getClient().get(LOCAL_DIR_NAME,
                    getLocalDownloadDir() + File.separator + LOCAL_BINARY_FILE_NAME);
            Assert.fail("Getting remote folder to a local file path should throw RemoteDataException.");
        }
        catch (IOException e) {
            Assert.fail("Getting remote folder to a local file path should not throw IOException.", e);
        }
        catch (RemoteDataException e) {
            Assert.assertTrue(true);
        }
    }

    @Test(dataProvider="getDirectoryRetrievesToCorrectLocationProvider", dependsOnMethods={"getThrowsExceptionWhenDownloadingFolderToLocalFilePath"})
    public void getDirectoryRetrievesToCorrectLocation(String localdir, boolean createTestDownloadFolder, String expectedDownloadPath, String message)
    {
        _getDirectoryRetrievesToCorrectLocation(localdir, createTestDownloadFolder, expectedDownloadPath, message);
    }

    protected void _getDirectoryRetrievesToCorrectLocation(String localdir, boolean createTestDownloadFolder, String expectedDownloadPath, String message)
    {
        String remotePutPath = null;
        File testDownloadPath = FileUtils.getFile(FileUtils.getTempDirectory() + "/" + localdir);
        File testExpectedDownloadPath = FileUtils.getFile(FileUtils.getTempDirectory() + "/" + expectedDownloadPath);
        try
        {
            if (createTestDownloadFolder) {
                testDownloadPath.mkdirs();
            } else {
                testDownloadPath.getParentFile().mkdirs();
            }

            remotePutPath = LOCAL_DIR_NAME;

            getClient().put(LOCAL_DIR, "");

            Assert.assertTrue(getClient().doesExist(remotePutPath), "Data not found on remote system after put.");

            getClient().get(remotePutPath, testDownloadPath.getAbsolutePath());
            Assert.assertTrue(testExpectedDownloadPath.exists(), message);

            for(File localFile: FileUtils.getFile(LOCAL_DIR).listFiles()) {
                if (!localFile.getName().equals(".") && !localFile.getName().equals(".."))
                    Assert.assertTrue(new File(testExpectedDownloadPath, localFile.getName()).exists(),
                            "Data not found on local system after get.");
            }
        }
        catch (Exception e) {
            Assert.fail("get should not throw unexpected exception", e);
        }
        finally {
            FileUtils.deleteQuietly(testDownloadPath);
            FileUtils.deleteQuietly(testExpectedDownloadPath);
        }
    }


}
