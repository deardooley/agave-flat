/**
 * 
 */
package org.iplantc.service.transfer.sftp;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.RemoteCredentialException;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.transfer.BaseTransferTestCase;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.sftp.MaverickSFTP;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"sftp","sanitization", "broken"})
public class SftpSshKeysKBIAuthTest extends BaseTransferTestCase {

private static final Logger log = Logger.getLogger(SftpSshKeysKBIAuthTest.class);
    
    protected ThreadLocal<RemoteDataClient> threadClient = new ThreadLocal<RemoteDataClient>();
    
    /**
     * Returns a {@link JSONObject} representing the system to test.
     * 
     * @return 
     * @throws JSONException
     * @throws IOException
     */
    protected JSONObject getSystemJson() throws JSONException, IOException {
    	return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "sftp-sshkeys-kbi.example.com.json");
    }
    
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
    }
    
    @AfterClass(alwaysRun=true)
    protected void afterClass() throws Exception {
//        try
//        {
//            getClient().authenticate();
//            // remove test directory
//            getClient().delete("..");
//            Assert.assertFalse(getClient().doesExist(""), "Failed to clean up home directory " + getClient().resolvePath("") + "after test.");
//        } 
//        catch (Exception e) {
//            Assert.fail("Failed to clean up test home directory " + getClient().resolvePath("") + " after test method.", e);
//        }
//        finally {
//            try { getClient().disconnect(); } catch (Exception e) {}
//        }
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
    
    @Test
    public void stat() 
	throws FileNotFoundException 
    {	
    	try {
    		MaverickSFTP client = (MaverickSFTP)getClient();
    		client.authenticate();
	    	client.getFileInfo("/");
	    	
	    	Assert.assertTrue(true, "Authentication should succeed wiht KBI auth");
    	}
    	catch (RemoteDataException e) {
    		Assert.fail("Authentication should succeed with KBI auth", e);
    	}
    	catch (IOException e) {
			Assert.fail("IO Exception should not be thrown an KBI auth", e);
		} 
    	finally {
    		
    	}
    }
}
