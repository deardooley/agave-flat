package org.iplantc.service.transfer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.RemoteCredentialException;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups={"broken","integration"})
public abstract class AbstractPathSanitizationTest extends BaseTransferTestCase {
    
    private static final Logger log = Logger.getLogger(AbstractPathSanitizationTest.class);
    
    public static String SPECIAL_CHARS = " _-!@#$%^*()+[]{}:."; // excluding "&" due to a bug in irods
    
    protected ThreadLocal<RemoteDataClient> threadClient = new ThreadLocal<RemoteDataClient>();
    
    /**
     * Returns a {@link JSONObject} representing the system to test.
     * 
     * @return 
     * @throws JSONException
     * @throws IOException
     */
    protected abstract JSONObject getSystemJson() throws JSONException, IOException;
    
    @BeforeClass(alwaysRun=true)
    protected void beforeSubclass() throws Exception {
        super.beforeClass();
        
        JSONObject json = getSystemJson();
        json.remove("id");
        json.put("id", this.getClass().getSimpleName());
        system = (StorageSystem)StorageSystem.fromJSON(json);
        system.setOwner(SYSTEM_USER);
        String homeDir = system.getStorageConfig().getHomeDir();
        homeDir = StringUtils.isEmpty(homeDir) ? "" : homeDir;
        system.getStorageConfig().setHomeDir( homeDir + "/" + getClass().getSimpleName());
        storageConfig = system.getStorageConfig();
        salt = system.getSystemId() + storageConfig.getHost() + 
                storageConfig.getDefaultAuthConfig().getUsername();
        
        SystemDao dao = Mockito.mock(SystemDao.class);
        Mockito.when(dao.findBySystemId(Mockito.anyString()))
            .thenReturn(system);
        
        getClient().authenticate();
        if (getClient().doesExist("")) {
            getClient().delete("..");
        }
        
        getClient().mkdirs("");
    }
    
    @AfterClass(alwaysRun=true)
    protected void afterClass() throws Exception {
        try
        {
            getClient().authenticate();
            // remove test directory
            getClient().delete("..");
            Assert.assertFalse(getClient().doesExist(""), "Failed to clean up home directory " + getClient().resolvePath("") + "after test.");
        } 
        catch (Exception e) {
            Assert.fail("Failed to clean up test home directory " + getClient().resolvePath("") + " after test method.", e);
        }
        finally {
            try { getClient().disconnect(); } catch (Exception e) {}
        }
    }
    
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
    
    
    @DataProvider(parallel=false)
    protected Object[][] mkDirSanitizesSingleSpecialCharacterProvider() throws Exception {
        List<Object[]> tests = new ArrayList<Object[]>();
        for (char c: SPECIAL_CHARS.toCharArray()) {
            String sc = String.valueOf(c); 
            if (c == ' ' || c == '.') {
                continue;
            } else {
                tests.add(new Object[] { sc, true, "Directory name with single special character '" + sc + "' should be created" });
            }
            tests.add(new Object[] { sc + "leading", true, "Directory name with leading single special character '" + sc + "' should be created" });
            tests.add(new Object[] { "trailing" + sc, true, "Directory name with trailing single special character '" + sc + "' should be created" });
            tests.add(new Object[] { sc + "bookend" + sc, true, "Directory name with leading and trailing single special character '" + sc + "' should be created" });
            tests.add(new Object[] { "sand" + sc + "wich", true, "Directory name with singleinternal special character '" + sc + "' should be created" });
        }
        
        return tests.toArray(new Object[][] {});
    }
    
    @DataProvider(parallel=false)
    protected Object[][] mkDirSanitizesRepeatedSpecialCharacterProvider() throws Exception {
        List<Object[]> tests = new ArrayList<Object[]>();
        for (char c: SPECIAL_CHARS.toCharArray()) {
            String chars = String.valueOf(c) + String.valueOf(c);
            if (c == ' ' || c == '.') {
                continue;
            } else {
                tests.add(new Object[] { chars, true, "Directory name with only repeated special character '" + chars + "' should be created" });
            }
            tests.add(new Object[] { chars + "leading", true, "Directory name with repeated repeated special character '" + chars + "' should be created" });
            tests.add(new Object[] { "trailing" + chars, true, "Directory name with trailing repeated special character '" + chars + "' should be created" });
            tests.add(new Object[] { chars + "bookend" + chars, true, "Directory name with leading and trailing repeated special character '" + chars + "' should be created" });
            tests.add(new Object[] { "sand" + chars + "wich", true, "Directory name with repeated internal special character '" + chars + "' should be created" });
        }
        
        return tests.toArray(new Object[][] {});
    }
    
    @DataProvider(parallel=false)
    protected Object[][] mkDirSanitizesWhitespaceProvider() throws Exception {
        return new Object[][] {
                { " ", "Directory name with single whitespace character will resolve to an empty name and should throw exception" },
                { "  ", "Directory name with double whitespace character will resolve to an empty name and should throw exception" },
                { "   ", "Directory name with triple whitespace character will resolve to an empty name and should throw exception" },
        };
    }
    
//    @Test(dataProvider="mkDirSanitizesSingleSpecialCharacterProvider", priority=1)
//    public void mkDirSanitizesSingleSpecialCharacter(String filename, boolean shouldThrowException, String message)  throws FileNotFoundException
//    {
//        _mkDirSanitizationTest(filename, shouldThrowException, message);
//    }
//    
//    @Test(dataProvider="mkDirSanitizesSingleSpecialCharacterProvider", priority=2)
//    public void mkDirSanitizesSingleSpecialCharacterRelativePath(String filename, boolean shouldThrowException, String message)  throws FileNotFoundException
//    {
//        String relativePath = "relative_path_test/";
//        
//        _mkDirsSanitizationTest(relativePath + filename, shouldThrowException, message);
//    }
//    
//    @Test(dataProvider="mkDirSanitizesSingleSpecialCharacterProvider", priority=3)
//    public void mkDirSanitizesSingleSpecialCharacterAbsolutePath(String filename, boolean shouldThrowException, String message)  throws FileNotFoundException
//    {
//        String absolutePath = system.getStorageConfig().getHomeDir() + "/thread-" + Thread.currentThread().getId() +  "/" + "absolute_path_test/";
//        
//        _mkDirsSanitizationTest(absolutePath + filename, shouldThrowException, message);
//    }
//    
    @Test(dataProvider="mkDirSanitizesRepeatedSpecialCharacterProvider", priority=4)
    public void mkDirSanitizesRepeatedSpecialCharacter(String filename, boolean shouldSucceed, String message)  throws FileNotFoundException
    {
        _mkDirSanitizationTest(filename, shouldSucceed, message);
    }
//    
//    @Test(dataProvider="mkDirSanitizesRepeatedSpecialCharacterProvider", priority=5)
//    public void mkDirSanitizesRepeatedSpecialCharacterRelativePath(String filename, boolean shouldSucceed, String message)  throws FileNotFoundException
//    {
//        String relativePath = "relative_path_test/";
//        
//        _mkDirsSanitizationTest(relativePath + filename, shouldSucceed, message);
//    }
    
//    @Test(dataProvider="mkDirSanitizesRepeatedSpecialCharacterProvider", priority=6)
//    public void mkDirSanitizesRepeatedSpecialCharacterAbsolutePath(String filename, boolean shouldSucceed, String message)  throws FileNotFoundException
//    {
//        String absolutePath = system.getStorageConfig().getHomeDir() + "/thread-" + Thread.currentThread().getId() +  "/" + "absolute_path_test/";
//        
//        _mkDirsSanitizationTest(absolutePath + filename, shouldSucceed, message);
//    }
    
//    @Test(dataProvider="mkDirSanitizesWhitespaceProvider", priority=7)
//    public void mkDirSanitizesWhitespace(String filename, boolean shouldSucceed, String message)  throws FileNotFoundException
//    {
//        _mkDirSanitizationTest(filename, shouldSucceed, message);
//    }
//
//    @Test(dataProvider="mkDirSanitizesWhitespaceProvider", priority=8)
//    public void mkDirSanitizesWhitespace(String filename, String message)  throws IOException, RemoteDataException
//    {
////        getClient().mkdirs("");
//        _mkDirSanitizationTest(filename, false, message);
//    }
    
    protected void _mkDirSanitizationTest(String filename, boolean shouldSucceed, String message) throws FileNotFoundException
    {
        try {
            Assert.assertEquals(getClient().mkdir(filename), shouldSucceed);
            Assert.assertTrue(getClient().doesExist(filename), message);
        } 
        catch (Exception e) 
        {
            if (shouldSucceed) {
                Assert.fail(message, e);
            }
        }
        
        try { 
            getClient().delete(filename);
        } catch (Exception e) {
            Assert.fail("Unable to delete directory " + getClient().resolvePath(filename), e);
        }
    }
    
    protected void _mkDirsSanitizationTest(String filename, boolean shouldSucceed, String message) throws FileNotFoundException
    {
        try {
            Assert.assertEquals(getClient().mkdirs(filename), shouldSucceed, message);
            Assert.assertTrue(getClient().doesExist(filename), message);
        } 
        catch (Exception e) 
        {
            if (shouldSucceed)
                Assert.fail(message, e);
        }
        
        try { 
            getClient().delete(filename);
        } catch (Exception e) {
            Assert.fail("Unable to delete directory " + getClient().resolvePath(filename), e);
        }
     
    }
}
