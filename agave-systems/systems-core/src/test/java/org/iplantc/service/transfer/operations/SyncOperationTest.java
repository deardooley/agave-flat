/**
 *
 */
package org.iplantc.service.transfer.operations;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.transfer.RemoteFileInfo;
//import org.iplantc.service.transfer.RemoteTransferListener;
import org.iplantc.service.transfer.Settings;
//import org.iplantc.service.transfer.dao.TransferTaskDao;
//import org.iplantc.service.transfer.model.TransferTask;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Coverage tests for directly uploading data from the local
 * file system to a {@link RemoteSystem}.
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"sync","upload", "integration"})//, dependsOnGroups= {"copy"}
public class SyncOperationTest extends BaseRemoteDataClientOperationTest {

    private static final Logger log = Logger.getLogger(SyncOperationTest.class);

    public SyncOperationTest(String systemJsonFilePath,
            TransferOperationAfterMethod afterMethodTeardown,
            TransferOperationBeforeMethod beforeMethodSetup,
            ForbiddenPathProvider forbiddenPathProvider)
    {
        super(systemJsonFilePath, afterMethodTeardown, beforeMethodSetup, forbiddenPathProvider);
    }

    public void syncToRemoteFile()
    {
        _syncToRemoteFile();
    }

    protected void _syncToRemoteFile()
    {
        File tmpFile = null;
        try
        {
            tmpFile = File.createTempFile("syncToRemoteFile", "txt");
            org.apache.commons.io.FileUtils.writeStringToFile(tmpFile, "This should be overwritten.");

//            TransferTask task = new TransferTask(
//                    tmpFile.toURI().toString(),
//                    getClient().getUriForPath(LOCAL_BINARY_FILE_NAME).toString(),
//                    SYSTEM_USER, null, null);
//
//            TransferTaskDao.persist(task);
//
//            getClient().syncToRemote(tmpFile.getAbsolutePath(), LOCAL_BINARY_FILE_NAME, new RemoteTransferListener(task));
            getClient().syncToRemote(tmpFile.getAbsolutePath(), LOCAL_BINARY_FILE_NAME, null);

            Assert.assertTrue(getClient().doesExist(LOCAL_BINARY_FILE_NAME),
                    "Remote file was not created during sync");

            Assert.assertEquals(getClient().length(LOCAL_BINARY_FILE_NAME), tmpFile.length(),
                    "Remote file was not copied in whole created during sync");

//            TransferTask task2 = new TransferTask(
//                    new File(LOCAL_BINARY_FILE).toURI().toString(),
//                    getClient().getUriForPath(LOCAL_BINARY_FILE_NAME).toString(),
//                    SYSTEM_USER, null, null);
//
//            TransferTaskDao.persist(task2);
//
//            getClient().syncToRemote(LOCAL_BINARY_FILE, LOCAL_BINARY_FILE_NAME, new RemoteTransferListener(task2));
            getClient().syncToRemote(LOCAL_BINARY_FILE, LOCAL_BINARY_FILE_NAME, null);

            Assert.assertNotEquals(getClient().length(LOCAL_BINARY_FILE_NAME), tmpFile.length(),
                    "Remote file size should not match temp file after syncing.");

            Assert.assertEquals(getClient().length(LOCAL_BINARY_FILE_NAME), new File(LOCAL_BINARY_FILE).length(),
                    "Remote file size should match local binary file after syncing.");
        }
        catch (Exception e)
        {
            Assert.fail("Put of file or folder should not throw exception.", e);
        }
    }

    @Test(dependsOnMethods={"syncToRemoteFile"})
    public void syncToRemoteFolderAddsMissingFiles()
    {
        _syncToRemoteFolderAddsMissingFiles();
    }

    protected void _syncToRemoteFolderAddsMissingFiles()
    {
        try
        {
            File localDir = new File(LOCAL_DIR);
            getClient().mkdir(localDir.getName());

            Assert.assertTrue(getClient().doesExist(localDir.getName()),
                    "Remote directory was not created. Test will fail");

//            TransferTask task = new TransferTask(
//                    localDir.toURI().toString(),
//                    getClient().getUriForPath(localDir.getName() + "/" + localDir.getName()).toString(),
//                    SYSTEM_USER, null, null);
//
//            TransferTaskDao.persist(task);
//
//            getClient().syncToRemote(LOCAL_DIR, "", new RemoteTransferListener(task));
            getClient().syncToRemote(LOCAL_DIR, "", null);
            Assert.assertFalse(getClient().doesExist(localDir.getName() + "/" + localDir.getName()),
                    "Local directory was synced as a subfolder instead of overwriting");

            List<RemoteFileInfo> remoteListing = getClient().ls(LOCAL_DIR_NAME);

            Assert.assertEquals(localDir.list().length, remoteListing.size(),
                    "Mismatch between file count locally and remote.");

            for (RemoteFileInfo fileInfo: remoteListing)
            {
                File localFile = new File(LOCAL_DIR, fileInfo.getName());

                Assert.assertEquals(fileInfo.getSize(), localFile.length(),
                        "Remote file is a different size than the local one.");
            }
        }
        catch (Exception e)
        {
            Assert.fail("Put of file or folder should not throw exception.", e);
        }
    }

    @Test(dependsOnMethods={"syncToRemoteFolderAddsMissingFiles"})
    public void syncToRemoteFolderLeavesExistingFilesUntouched()
    {
        _syncToRemoteFolderLeavesExistingFilesUntouched();
    }

    protected void _syncToRemoteFolderLeavesExistingFilesUntouched()
    {
        try
        {
            File localDir = new File(LOCAL_DIR);
            getClient().put(LOCAL_DIR, "");

            Assert.assertTrue(getClient().doesExist(localDir.getName()),
                    "Remote directory was not uploaded. Test will fail");

            Map<String, Date> lastUpdatedMap = new HashMap<String,Date>();
            List<RemoteFileInfo> remoteListing = getClient().ls(LOCAL_DIR_NAME);
            for (RemoteFileInfo fileInfo: remoteListing)
            {
                lastUpdatedMap.put(fileInfo.getName(), fileInfo.getLastModified());
            }

//            TransferTask task = new TransferTask(
//                    localDir.toURI().toString(),
//                    getClient().getUriForPath(localDir.getName() + "/" + localDir.getName()).toString(),
//                    SYSTEM_USER, null, null);
//
//            TransferTaskDao.persist(task);
//
//            getClient().syncToRemote(LOCAL_DIR, "", new RemoteTransferListener(task));
            getClient().syncToRemote(LOCAL_DIR, "", null);
            
            Assert.assertFalse(getClient().doesExist(localDir.getName() + "/" + localDir.getName()),
                    "Local directory was synced as a subfolder instead of overwriting");

            remoteListing = getClient().ls(LOCAL_DIR_NAME);

            Assert.assertEquals(localDir.list().length, remoteListing.size(),
                    "Mismatch between file count locally and remote.");

            for (RemoteFileInfo fileInfo: remoteListing)
            {
                File localFile = new File(LOCAL_DIR, fileInfo.getName());

                Assert.assertEquals(fileInfo.getSize(), localFile.length(),
                        "Remote file is a different size than the local one.");

                Assert.assertEquals(fileInfo.getLastModified(), lastUpdatedMap.get(fileInfo.getName()),
                        "Dates do not match up. File must have been overwritten.");
            }
        }
        catch (Exception e)
        {
            Assert.fail("Put of file or folder should not throw exception.", e);
        }
    }

    @Test(dependsOnMethods={"syncToRemoteFolderLeavesExistingFilesUntouched"})
    public void syncToRemoteFolderOverwritesFilesWithDeltas()
    {
        _syncToRemoteFolderOverwritesFilesWithDeltas();
    }

    protected void _syncToRemoteFolderOverwritesFilesWithDeltas()
    {
        File tmpFile = null;
        try
        {
            File localDir = new File(LOCAL_DIR);
            getClient().mkdir(localDir.getName());

            Assert.assertTrue(getClient().doesExist(localDir.getName()),
                    "Remote directory was not created. Test will fail");

            tmpFile = File.createTempFile("syncToRemoteFile", "txt");
            org.apache.commons.io.FileUtils.writeStringToFile(tmpFile, "This should be overwritten.");

            String remoteFileToOverwrightString = localDir.getName() + "/" + LOCAL_BINARY_FILE_NAME;
            getClient().put(tmpFile.getAbsolutePath(), remoteFileToOverwrightString);

            Assert.assertTrue(getClient().doesExist(remoteFileToOverwrightString),
                    "Remote file was not created during sync");

            Assert.assertEquals(getClient().length(remoteFileToOverwrightString), tmpFile.length(),
                    "Remote file was not copied in whole created during sync");

//            TransferTask task = new TransferTask(
//                    localDir.toURI().toString(),
//                    getClient().getUriForPath(localDir.getName() + "/" + localDir.getName()).toString(),
//                    SYSTEM_USER, null, null);
//
//            TransferTaskDao.persist(task);
//
//            getClient().syncToRemote(LOCAL_DIR, "", new RemoteTransferListener(task));
            getClient().syncToRemote(LOCAL_DIR, "", null);
            
            Assert.assertFalse(getClient().doesExist(localDir.getName() + "/" + localDir.getName()),
                    "Local directory was synced as a subfolder instead of overwriting");

            List<RemoteFileInfo> remoteListing = getClient().ls(LOCAL_DIR_NAME);
            File[] localListing = localDir.listFiles();

            Assert.assertEquals(localListing.length, remoteListing.size(),
                    "Mismatch between file count locally and remote.");

            for (RemoteFileInfo fileInfo: remoteListing)
            {
                File localFile = new File(LOCAL_DIR, fileInfo.getName());

                Assert.assertEquals(fileInfo.getSize(), localFile.length(),
                        "Remote file is a different size than the local one.");
            }
        }
        catch (Exception e)
        {
            Assert.fail("Put of file or folder should not throw exception.", e);
        }
    }

    @Test(dependsOnMethods={"syncToRemoteFolderOverwritesFilesWithDeltas"})
    public void syncToRemoteFolderReplacesOverlappingFilesAndFolders()
    {
        _syncToRemoteFolderReplacesOverlappingFilesAndFolders();
    }

    protected void _syncToRemoteFolderReplacesOverlappingFilesAndFolders()
    {
        try
        {
            File localDir = new File(LOCAL_DIR);
            getClient().mkdir(localDir.getName());

            Assert.assertTrue(getClient().doesExist(localDir.getName()),
                    "Remote directory was not created. Test will fail");

            String remoteFolderToOverwrightString = localDir.getName() + "/" + LOCAL_BINARY_FILE_NAME;
            getClient().mkdir(remoteFolderToOverwrightString);

            Assert.assertTrue(getClient().doesExist(remoteFolderToOverwrightString),
                    "Remote subdirectory was not created during sync");

//            TransferTask task = new TransferTask(
//                    localDir.toURI().toString(),
//                    getClient().getUriForPath(localDir.getName() + "/" + localDir.getName()).toString(),
//                    SYSTEM_USER, null, null);
//
//            TransferTaskDao.persist(task);
//
//            getClient().syncToRemote(LOCAL_DIR, "", new RemoteTransferListener(task));
            getClient().syncToRemote(LOCAL_DIR, "", null);
            
            List<RemoteFileInfo> remoteListing = getClient().ls(LOCAL_DIR_NAME);

            Assert.assertEquals(localDir.list().length, remoteListing.size(),
                    "Mismatch between file count locally and remote.");

            for (RemoteFileInfo fileInfo: remoteListing)
            {
                File localFile = new File(LOCAL_DIR, fileInfo.getName());

                Assert.assertEquals(fileInfo.getSize(), localFile.length(),
                        "Remote file is a different size than the local one.");

                Assert.assertEquals(fileInfo.isDirectory(), localFile.isDirectory(),
                        "Remote file is " + (localFile.isDirectory()? "not ": "") +
                        "a file like the local one. Sync should delete remote and replace with local.");
            }
        }
        catch (Exception e)
        {
            Assert.fail("Put of file or folder should not throw exception.", e);
        }
    }

    @Test(dependsOnMethods= {"syncToRemoteFolderReplacesOverlappingFilesAndFolders"})
    public void syncToRemoteSyncesSubfoldersRatherThanEmbeddingThem()
    {
        _syncToRemoteSyncesSubfoldersRatherThanEmbeddingThem();
    }

    protected void _syncToRemoteSyncesSubfoldersRatherThanEmbeddingThem()
    {
        File tmpFile = null;
        File tempDir = null;
        try
        {
            tmpFile = new File(tempDir, "temp.txt");
            org.apache.commons.io.FileUtils.writeStringToFile(tmpFile, "This should be overwritten.");

            String[] tempSubdirectories = {
                    "foo",
                    "foo/alpha",
                    "foo/alpha/gamma",
                    "bar",
                    "bar/beta",
            };

            String tempLocalDir = Settings.TEMP_DIRECTORY + "/deleteme-" + System.currentTimeMillis();
            tempDir = new File(tempLocalDir);
            tempDir.mkdirs();

            for (String path: tempSubdirectories) {
                FileUtils.copyFile(tmpFile, new File(tempLocalDir + "/" + path + "/file1.txt"));
                FileUtils.copyFile(tmpFile, new File(tempLocalDir + "/" + path + "/file2.txt"));
            }

            getClient().mkdirs(tempDir.getName() + "/foo/alpha/gamma");
            getClient().mkdirs(tempDir.getName() + "/bar/beta");

//            TransferTask task = new TransferTask(
//                    tempDir.toURI().toString(),
//                    getClient().getUriForPath(tempDir.getName() + "/" + tempDir.getName()).toString(),
//                    SYSTEM_USER, null, null);
//
//            TransferTaskDao.persist(task);
//
//            getClient().syncToRemote(tempLocalDir, "", new RemoteTransferListener(task));
            getClient().syncToRemote(tempLocalDir, "", null);
            
            Assert.assertFalse(getClient().doesExist(tempDir.getName() + "/" + tempDir.getName()),
                    "Syncing put the local dir into the remote directory instead of overwriting");

            for (String path: tempSubdirectories)
            {
                Assert.assertFalse(getClient().doesExist(tempDir.getName() + "/" + path + "/" + FilenameUtils.getName(path)),
                    "Syncing put the local subdirectory into the remote subdirectory instead of overwriting");
                Assert.assertTrue(getClient().doesExist(tempDir.getName() + "/" + path + "/file1.txt"),
                    "Failed to copy local file1.txt to remote folder in proper place.");
                Assert.assertTrue(getClient().doesExist(tempDir.getName() + "/" + path + "/file2.txt"),
                        "Failed to copy local file2.txt to remote folder in proper place.");
            }
        }
        catch (Exception e)
        {
            Assert.fail("Put of file or folder should not throw exception.", e);
        }
    }
}
