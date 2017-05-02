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
 * Coverage tests for {@link RemoteDataClient#doesExist()}.
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"delete","integration"})
public class DeleteOperationTest extends BaseRemoteDataClientOperationTest {

    private static final Logger log = Logger.getLogger(DeleteOperationTest.class);

    public DeleteOperationTest(String systemJsonFilePath,
            TransferOperationAfterMethod afterMethodTeardown,
            TransferOperationBeforeMethod beforeMethodSetup,
            ForbiddenPathProvider forbiddenPathProvider)
    {
        super(systemJsonFilePath, afterMethodTeardown, beforeMethodSetup, forbiddenPathProvider);
    }
    
    @DataProvider
    protected Object[][] deleteFileProvider() {
        return new Object[][] {
                { LOCAL_TXT_FILE_NAME, false, "Delete should delete standard files." },
                { "." + LOCAL_TXT_FILE_NAME, false, "Delete should delete shadow files." },
                { "foo " + LOCAL_TXT_FILE_NAME, false, "Delete should delete files with spaces in the name." }
        };
    }
    
    @DataProvider
    protected Object[][] deleteDirectoryProvider() {
        return new Object[][] {
                { LOCAL_DIR_NAME, false, "Delete should delete empty standard directories." },
                { "." + LOCAL_DIR_NAME, false, "Delete should delete empty shadow directories." },
                { "foo " + LOCAL_DIR_NAME, false, "Delete should delete directories with spaces in the name." },
        };
    }

    @Test(dataProvider="deleteFileProvider")
    public void deleteFile(String remoteFileName, boolean shouldThrowException, String message)
    {
        _deleteFile(remoteFileName, shouldThrowException, message);
    }

    protected void _deleteFile(String remoteFileName, boolean shouldThrowException, String message)
    {
        try
        {
            getClient().put(LOCAL_TXT_FILE, remoteFileName);
            
            Assert.assertTrue(getClient().doesExist(remoteFileName),
                    "File " + remoteFileName + " not found on remote system after put.");
            
            getClient().delete(remoteFileName);
            
            Assert.assertFalse(getClient().doesExist(LOCAL_TXT_FILE_NAME),
                    "File " + LOCAL_TXT_FILE_NAME + " not deleted from remote system.");
        }
        catch (Exception e) {
            Assert.fail("Failed to delete file or folder", e);
        }
    }
    
    @Test(dependsOnMethods={"deleteFile"})
    public void deleteDirectoryWithFileContent()
    {
        _deleteDirectoryWithFileContent();
    }

    protected void _deleteDirectoryWithFileContent()
    {
        try
        {
            getClient().put(LOCAL_DIR, "");
            
            Assert.assertTrue(getClient().doesExist(LOCAL_DIR_NAME + "/" + LOCAL_TXT_FILE_NAME),
                    "Directory " + LOCAL_DIR_NAME + " not found on remote system after put.");
            
            getClient().delete(LOCAL_DIR_NAME);
            
            Assert.assertFalse(getClient().doesExist(LOCAL_DIR_NAME),
                    "Directory " + LOCAL_DIR_NAME + " not deleted from remote system.");
        }
        catch (Exception e) {
            Assert.fail("Failed to delete file or folder", e);
        }
    }
    
    @Test(dataProvider="deleteDirectoryProvider", dependsOnMethods= {"deleteDirectoryWithFileContent"})
    public void deleteDirectoryWithEmptyDirectoryContent(String localDirectory, boolean shouldThrowException, String message)
    {
        _deleteDirectoryWithEmptyDirectoryContent(localDirectory, shouldThrowException, message);
    }

    protected void _deleteDirectoryWithEmptyDirectoryContent(String remoteDirectory, boolean shouldThrowException, String message)
    {
        try
        {
            Assert.assertTrue(getClient().mkdirs(remoteDirectory),
                    "Directory " + remoteDirectory + " not created during test setup.");
            
            getClient().delete(remoteDirectory);
            
            Assert.assertFalse(getClient().doesExist(LOCAL_DIR_NAME), message);
        }
        catch (Exception e) {
            Assert.fail(message, e);
        }
    }
    
    @Test(dependsOnMethods= {"deleteDirectoryWithEmptyDirectoryContent"})
    public void deleteEmptyDirectory()
    {
        _deleteEmptyDirectory();
    }

    protected void _deleteEmptyDirectory()
    {
        try
        {
            Assert.assertTrue(getClient().mkdirs(LOCAL_DIR_NAME),
                    "Directory " + LOCAL_DIR_NAME + " not created during test setup.");
            
            getClient().delete(LOCAL_DIR_NAME);
            
            Assert.assertFalse(getClient().doesExist(LOCAL_DIR_NAME), "Deleting an empty directory should not fail");
        }
        catch (Exception e) {
            Assert.fail("Deleting an empty directory should not throw an exception", e);
        }
    }

    @Test(dependsOnMethods={"deleteEmptyDirectory"})
    public void deleteFailsOnMissingDirectory()
    {
        _deleteFailsOnMissingDirectory();
    }

    protected void _deleteFailsOnMissingDirectory()
    {
        try
        {
            getClient().delete(MISSING_DIRECTORY);
            Assert.fail("delete should throw exception on missing directory");
        }
        catch (FileNotFoundException e) {
            Assert.assertTrue(true);
        }
        catch (Exception e) {
            Assert.fail("Incorrect error thorwn when delete missing directory.", e);
        }
    }

    @Test(dependsOnMethods={"deleteFailsOnMissingDirectory"})
    public void deleteThrowsExceptionWhenNoPermission()
    {
        _deleteThrowsExceptionWhenNoPermission();
    }

    protected void _deleteThrowsExceptionWhenNoPermission()
    {
        try
        {
            getClient().delete(getForbiddenDirectoryPath(true));
            Assert.fail("delete should throw RemoteDataException on no permissions");
        }
        catch (RemoteDataException e) {
            Assert.assertTrue(true);
        }
        catch (Exception e) {
            Assert.fail("delete should throw RemoteDataException on no permissions");
        }
    }
    
    @Test(dependsOnMethods={"deleteThrowsExceptionWhenNoPermission"})
    public void deleteThrowsRemoteDataExceptionWhenNoPermissionOnMissingDirectory()
    {
        _deleteThrowsRemoteDataExceptionWhenNoPermissionOnMissingDirectory();
    }

    protected void _deleteThrowsRemoteDataExceptionWhenNoPermissionOnMissingDirectory()
    {
        try
        {
            getClient().delete(getForbiddenDirectoryPath(true));
            Assert.fail("delete should throw RemoteDataException on missing forbiddden path");
        }
        catch (RemoteDataException e) {
            
        }
        catch (Exception e) {
            Assert.fail("delete should throw RemoteDataException on missing forbiddden path");
        }
    }

}
