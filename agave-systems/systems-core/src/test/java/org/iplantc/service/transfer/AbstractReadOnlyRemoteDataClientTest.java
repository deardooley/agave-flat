package org.iplantc.service.transfer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.RemoteCredentialException;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
//import org.iplantc.service.transfer.dao.TransferTaskDao;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
//import org.iplantc.service.transfer.model.TransferTask;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(singleThreaded=true, groups= {"transfer", "irods.filesystem.init"})
public abstract class AbstractReadOnlyRemoteDataClientTest extends BaseTransferTestCase 
{
    protected File tmpFile = null;
    protected File tempDir = null;
    protected ThreadLocal<RemoteDataClient> threadClient = new ThreadLocal<RemoteDataClient>();
    
    private static final Logger log = Logger.getLogger(AbstractReadOnlyRemoteDataClientTest.class);
    
    protected abstract JSONObject getSystemJson() throws JSONException, IOException;
    
    /**
     * Gets getClient() from current thread
     * @return
     * @throws RemoteCredentialException 
     * @throws RemoteDataException 
     */
    protected RemoteDataClient getClient() 
    {
        RemoteDataClient client;
        try {
            if (threadClient.get() == null) {
                client = system.getRemoteDataClient();
                client.updateSystemRoots(client.getRootDir(), system.getStorageConfig().getHomeDir() + "/thread-" + Thread.currentThread().getId());
                threadClient.set(client);
            } 
        } catch (RemoteDataException | RemoteCredentialException e) {
            Assert.fail("Failed to get client", e);
        }
        
        return threadClient.get();
    }
    
    protected String getLocalDownloadDir() {
        return LOCAL_DOWNLOAD_DIR + Thread.currentThread().getId();
    }
    
    @BeforeClass(alwaysRun=true)
    protected void beforeSubclass() throws Exception {
        super.beforeClass();
        
        JSONObject json = getSystemJson();
        json.remove("id");
        json.put("id", this.getClass().getSimpleName());
        system = (StorageSystem)StorageSystem.fromJSON(json);
        system.setOwner(SYSTEM_USER);
        storageConfig = system.getStorageConfig();
        salt = system.getSystemId() + storageConfig.getHost() + 
                storageConfig.getDefaultAuthConfig().getUsername();
        
        SystemDao dao = Mockito.mock(SystemDao.class);
        Mockito.when(dao.findBySystemId(Mockito.anyString()))
            .thenReturn(system);
    }
    
    @AfterClass(alwaysRun=true)
    protected void afterClass() throws Exception {
        try 
        {
            FileUtils.deleteQuietly(new File(getLocalDownloadDir()));
            FileUtils.deleteQuietly(tmpFile);
            FileUtils.deleteQuietly(tmpFile);
        }
        finally {
            try { getClient().disconnect(); } catch (Exception e) {}
        }
    }

    @BeforeMethod(alwaysRun=true)
    protected void beforeMethod() throws Exception 
    {
        FileUtils.deleteQuietly(new File(getLocalDownloadDir()));
        FileUtils.deleteQuietly(tmpFile);
        
        // auth client just in case
        getClient().authenticate();
    }

    @AfterMethod(alwaysRun=true)
    protected void afterMethod() throws Exception
    {  
    }

    @DataProvider(name="getDirectoryRetrievesToCorrectLocationProvider", parallel=false)
    protected Object[][] getDirectoryRetrievesToCorrectLocationProvider()
    {
        String localPath = UUID.randomUUID().toString();
        return new Object[][] {
            { localPath + "-1", true, localPath + "-1/" + LOCAL_DIR_NAME, "Downloading to existing path creates new folder in path." },
            { localPath + "-2/new_get_path", false, localPath + "-2/new_get_path" , "Downloading to non-existing target directory path downloads directory as named path." },
        };
    }
    
    @DataProvider(name="getFileRetrievesToCorrectLocationProvider")
    protected Object[][] getFileRetrievesToCorrectLocationProvider()
    {
        return new Object[][] {
            { getLocalDownloadDir(), getLocalDownloadDir() + "/" + LOCAL_BINARY_FILE_NAME, "Downloading to existing path creates new file in path." },
            { getLocalDownloadDir() + "/" + LOCAL_BINARY_FILE_NAME, getLocalDownloadDir() + "/" + LOCAL_BINARY_FILE_NAME, "Downloading to explicit file path where no file exists creates the file." },
        };
    }
    
    @DataProvider(name="doesExistProvider")
    protected Object[][] doesExistProvider()
    { 
        return new Object[][] {
            { null, true, "null path should resolve to home and not throw an exception." },
            { "", true, "Home directory should exist." },
            { MISSING_DIRECTORY, false, "Missing directory should not return true from doesExist." },
        };
    }
    
    
    @Test(groups= {"proxy"})
    public void isThirdPartyTransferSupported() 
    {
        _isThirdPartyTransferSupported();
    }
    
    protected void _isThirdPartyTransferSupported() {
        Assert.assertFalse(getClient().isThirdPartyTransferSupported(),
                "Third party transfer should not be supported by default in most protocols.");
    }
    
    @Test(dataProvider="doesExistProvider", dependsOnGroups= {"proxy"})
    public void doesExist(String remotedir, boolean shouldExist, String message)
    {
//        _doesExist(remotedir, shouldExist, message);
    }
    
    protected void _doesExist(String remotedir, boolean shouldExist, String message)
    {
        try {
            boolean doesExist = getClient().doesExist(remotedir);
            Assert.assertEquals(doesExist, shouldExist, message);
        } 
        catch (Exception e) {
            Assert.fail("Failed to query for existence of remote path " + remotedir, e);
        }
    }
   
    @Test//(dependsOnMethods= {"doesExist"})
    public void length()
    {
        _length();
    }
    
    protected void _length()
    {
        try 
        {
            Assert.assertTrue(getClient().doesExist(LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME), 
                    "Data not found on remote system.");
            Assert.assertTrue(new File(LOCAL_BINARY_FILE).length() == getClient().length(LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME),
                    "remote length does not match local length.");
        } 
        catch (Exception e) {
            Assert.fail("Failed to retrieve length of remote file", e);
        }
    }
    
    @Test(groups={"is"}, dependsOnMethods= {"length"})
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
            
            try {
                getClient().isDirectory(MISSING_DIRECTORY);
                Assert.fail("Non-existent folder should throw exception");
            } 
            catch (Exception e) {
                Assert.assertTrue(true);
            }
        } 
        catch (Exception e) {
            Assert.fail("isDirectory should not throw unexpected exceptions", e);
        }
    }
    
    @Test(groups={"is"}, dependsOnMethods={"isDirectoryTrueForDirectory"})
    public void isDirectoryFalseForFile()
    {
        _isDirectoryFalseForFile();
    }
    
    protected abstract void _isDirectoryFalseForFile();
    
    @Test(groups={"is"}, dependsOnMethods={"isDirectoryFalseForFile"})
    public void isDirectorThrowsExceptionForMissingPath()
    {
        _isDirectorThrowsExceptionForMissingPath();
    }
    
    protected void _isDirectorThrowsExceptionForMissingPath()
    {
        try 
        {
            getClient().isDirectory(MISSING_DIRECTORY);
            Assert.fail("isDirectory should throw exception when checking a non-existent path");
        }
        catch (Exception e) {
            Assert.assertTrue(true);
        }
    }
    
    @Test(groups={"is"}, dependsOnMethods={"isDirectorThrowsExceptionForMissingPath"})
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
            
            try {
                getClient().isFile(MISSING_FILE);
                Assert.fail("Non-existent folder should throw exception");
            } catch (Exception e) {
                Assert.assertTrue(true);
            }
        } 
        catch (Exception e) {
            Assert.fail("isFile should not throw unexpected exceptions", e);
        }
    }
    
    @Test(groups={"is"}, dependsOnMethods={"isFileFalseForDirectory"})
    public void isFileTrueForFile()
    {
        _isFileTrueForFile();
    }
    
    protected abstract void _isFileTrueForFile();
    
    @Test(groups={"is"}, dependsOnMethods={"isFileTrueForFile"})
    public void isFileThrowsExceptionForMissingPath()
    {
        _isFileThrowsExceptionForMissingPath();
    }
    
    protected void _isFileThrowsExceptionForMissingPath()
    {
        try 
        {
            getClient().isFile(MISSING_FILE);
            Assert.fail("isFile should throw exception when checking a non-existent path");
        }
        catch (Exception e) {
            Assert.assertTrue(true);
        }
    }
  
    @Test(groups={"list"}, dependsOnGroups={"is"})
    public void ls()
    {
        _ls();
    }
    
    protected void _ls()
    {
        String remoteDir = null;
        try 
        {
            List<RemoteFileInfo> files = getClient().ls("");
            Assert.assertTrue(files.size() > 0, "Public bucket listing should return not return empty results");
            List<String> localFiles = Arrays.asList(new File(LOCAL_DIR).list());
            
            for (RemoteFileInfo file: files) 
            {
                if (file.getName().equals(".")) {
                    Assert.fail("Listing should not return current directory in response list.");
                }
                else if (file.getName().equals("..")) {
                    Assert.fail("Listing should not return parent directory in response list.");
                }
                else
                {
                    Assert.assertTrue(localFiles.contains(file.getName()), 
                            "Remote file is not present on local file system.");
                }
            }
        } 
        catch (Exception e) {
            Assert.fail("Failed to list contents of " + remoteDir, e);
        } 
    }
    
    @Test(groups={"list"}, dependsOnMethods={"ls"})
    public void lsFailsOnMissingDirectory()
    {
        _lsFailsOnMissingDirectory();
    }
    
    protected void _lsFailsOnMissingDirectory()
    {
        try 
        {
            getClient().ls(MISSING_DIRECTORY);
            Assert.fail("ls should throw exception on missing directory");
        } 
        catch (FileNotFoundException e) {
            Assert.assertTrue(true);
        }
        catch (Exception e) {
            Assert.fail("Incorrect error thorwn when listing missing directory.", e);
        } 
    }
    
    @Test(groups={"get", "download"}, dependsOnGroups={"list"})
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
    
    @Test(groups={"get", "download"}, dependsOnMethods={"getThrowsExceptionOnMissingRemotePath"})
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
    
    @Test(groups={"get", "download"}, dataProvider="getFileRetrievesToCorrectLocationProvider", dependsOnMethods={"getDirectoryRetrievesToCorrectLocation"})
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
    
    @DataProvider(name="getInputStreamProvider", parallel=false)
    protected Object[][] getInputStreamProvider()
    {
        return new Object[][] {
                { "", "", true, "empty localfile to get should throw exception." },
                { null, "", true, "null localfile to get should throw exception." },
                { "", null, true, "null remotedir to get should throw exception." },
                { LOCAL_TXT_FILE, MISSING_DIRECTORY, true, "get on missing remote file should throw exception." },
                { LOCAL_TXT_FILE, "", false, "get local file from remote home directory should succeed." },
        };
    }
    
    @Test(groups={"stream", "get"}, dataProvider="getInputStreamProvider", dependsOnGroups= {"download"})
    public void getInputStream(String localFile, String remotedir, boolean shouldThrowException, String message)
    {
        _getInputStream(localFile, remotedir, shouldThrowException, message);
    }
    
    protected void _getInputStream(String localFile, String remotedir, boolean shouldThrowException, String message)
    {
        boolean actuallyThrewException = false;
        InputStream in = null;
        BufferedOutputStream bout = null;
        String remotePutPath = null;
        try
        {
            getClient().put(localFile, remotedir);
            if (StringUtils.isEmpty(remotedir)) {
                remotePutPath = FilenameUtils.getName(localFile);
            } else {
                remotePutPath = remotedir + "/" + FilenameUtils.getName(localFile);
            }
            String localGetPath = getLocalDownloadDir() + "/" + FilenameUtils.getName(remotePutPath);
            Assert.assertTrue(getClient().doesExist(remotePutPath), "Data not found on remote system after put.");

            in = getClient().getInputStream(remotePutPath, true);
            File downloadfile = new File(localGetPath);
            if (!org.codehaus.plexus.util.FileUtils.fileExists(downloadfile.getParent())) {
                FileUtils.getFile(downloadfile.getParent()).mkdirs();
            }
            bout = new BufferedOutputStream(new FileOutputStream(downloadfile));

            int bufferSize = getClient().getMaxBufferSize();
            byte[] b = new byte[bufferSize];
            int len = 0;

            while ((len = in.read(b)) > -1) {
                bout.write(b, 0, len);
            }

            bout.flush();
            
            Assert.assertTrue(org.codehaus.plexus.util.FileUtils.fileExists(localGetPath), "Data not found on local system after get.");

        }
        catch (Exception e) {
            actuallyThrewException = true;
            if (!shouldThrowException) e.printStackTrace();
        }
        finally {
            try { in.close(); } catch (Exception e) {}
            try { bout.close(); } catch (Exception e) {}
        }

        Assert.assertEquals(actuallyThrewException, shouldThrowException, message);
    }
    
//    @Test(groups={"checksum"}, dependsOnMethods={"checksumMissingPathThrowsException"})
//    public void checksum()
//    {
//        _checksum();
//    }
//    
//    protected void _checksum()
//    {
//        try 
//        {
//            getClient().put(LOCAL_TXT_FILE, "");
//            Assert.assertTrue(getClient().doesExist(LOCAL_TXT_FILE_NAME), 
//                    "Data not found on remote system after put.");
//        
//            getClient().checksum(LOCAL_TXT_FILE_NAME);
//            Assert.fail("Checksum should throw a NotImplementedException unless overridden by a concrete test class");
//        } 
//        catch (NotImplementedException e) {
//            Assert.assertTrue(true);
//        }
//        catch (Exception e) {
//            Assert.fail("Checksum should throw a NotImplementedException for SFTP", e);
//        }
//    }
}
