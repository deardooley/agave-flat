/**
 *
 */
package org.iplantc.service.transfer.operations;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.transfer.RemoteDataClient;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.io.Files;

/**
 * Coverage tests for {@link RemoteDataClient#get} on files
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"getFile", "download", "integration"})
public class GetFileOperationTest extends BaseRemoteDataClientOperationTest {

    public GetFileOperationTest(String systemJsonFilePath,
            TransferOperationAfterMethod afterMethodTeardown,
            TransferOperationBeforeMethod beforeMethodSetup,
            ForbiddenPathProvider forbiddenPathProvider)
    {
        super(systemJsonFilePath, afterMethodTeardown, beforeMethodSetup, forbiddenPathProvider);
    }

    private static final Logger log = Logger.getLogger(GetFileOperationTest.class);

    @DataProvider(name="getFileRetrievesToCorrectLocationProvider")
    protected Object[][] getFileRetrievesToCorrectLocationProvider()
    {
        return new Object[][] {
            { getLocalDownloadDir(), getLocalDownloadDir() + "/" + LOCAL_BINARY_FILE_NAME, "Downloading to existing path creates new file in path." },
            { getLocalDownloadDir() + "/" + LOCAL_BINARY_FILE_NAME, getLocalDownloadDir() + "/" + LOCAL_BINARY_FILE_NAME, "Downloading to explicit file path where no file exists creates the file." },
        };
    }
    
    @Test
    public void getThrowsExceptionWhenDownloadingFileToNonExistentLocalPath()
    {
        _getThrowsExceptionWhenDownloadingFileToNonExistentLocalPath();
    }

    protected void _getThrowsExceptionWhenDownloadingFileToNonExistentLocalPath()
    {
        try
        {
            FileUtils.getFile(getLocalDownloadDir()).delete();

            getClient().put(LOCAL_BINARY_FILE, "");

            getClient().get(LOCAL_BINARY_FILE_NAME, getLocalDownloadDir() + File.separator + MISSING_DIRECTORY);
            Assert.fail("Getting remote file to a local directory that does not exist should throw FileNotFoundException.");
        }
        catch (FileNotFoundException e) {
            Assert.assertTrue(true);
        }
        catch (Exception e) {
            Assert.fail("Getting remote folder to a local directory that does not exist should throw FileNotFoundException.", e);
        }
    }

    @Test( dataProvider="getFileRetrievesToCorrectLocationProvider", dependsOnMethods={"getThrowsExceptionWhenDownloadingFileToNonExistentLocalPath"})
    public void getFileRetrievesToCorrectLocation(String localPath, String expectedDownloadPath, String message)
    {
        _getFileRetrievesToCorrectLocation(localPath, expectedDownloadPath, message);
    }

    protected void _getFileRetrievesToCorrectLocation(String localPath, String expectedDownloadPath, String message)
    {
        String remotePutPath = null;
        try
        {
            FileUtils.getFile(getLocalDownloadDir()).mkdirs();

            getClient().put(LOCAL_BINARY_FILE, "");
            remotePutPath = LOCAL_BINARY_FILE_NAME;
            Assert.assertTrue(getClient().doesExist(remotePutPath), "Data not found on remote system after put.");

            getClient().get(remotePutPath, localPath);
            Assert.assertTrue(FileUtils.getFile(expectedDownloadPath).exists(), message);
        }
        catch (Exception e) {
            Assert.fail("get should not throw unexpected exception", e);
        }
    }

    @Test( dependsOnMethods={"getFileRetrievesToCorrectLocation"})
    public void getFileOverwritesExistingFile()
    {
        _getFileOverwritesExistingFile();
    }

    protected void _getFileOverwritesExistingFile()
    {
        String remotePutPath = null;
        File downloadDir = null;
        try
        {
            downloadDir = Files.createTempDir();
            
            // copy the file so it's present to be overwritten without endangering our test data
            FileUtils.copyFileToDirectory(new File(LOCAL_BINARY_FILE), downloadDir);
            File downloadFile = new File(downloadDir, LOCAL_BINARY_FILE_NAME);

            remotePutPath = LOCAL_BINARY_FILE_NAME;
            getClient().put(LOCAL_BINARY_FILE, "");
            Assert.assertTrue(getClient().doesExist(remotePutPath), "Data not found on remote system after put.");

            getClient().get(remotePutPath, downloadFile.getAbsolutePath());
            Assert.assertTrue(
                    downloadFile.exists(),
                    "Getting remote file should overwrite local file if it exists.");
        }
        catch (Exception e) {
            Assert.fail("Overwriting local file on get should not throw unexpected exception", e);
        }
        finally {
            try {
                FileUtils.deleteDirectory(downloadDir);
            } catch (IOException e) {}
        }
    }
}
