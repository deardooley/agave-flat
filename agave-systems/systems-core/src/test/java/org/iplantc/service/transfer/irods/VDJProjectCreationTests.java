/**
 * 
 */
package org.iplantc.service.transfer.irods;

import java.io.IOException;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.RemoteCredentialException;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.transfer.AbstractRemoteDataClientTest;
import org.iplantc.service.transfer.BaseTransferTestCase;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.model.enumerations.PermissionType;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * This is a test validating the project creation functionality needed
 * by the VDJ server. It runs through a handful of simple actions to verify
 * they are working. This should run in essentially constant time, but 
 * it tends to vary greatly in production. This test case looks into 
 * why that may be.
 * 
 * @author dooley
 *
 */
@Test(groups= {"irods.boutique", "broken"})
public class VDJProjectCreationTests extends BaseTransferTestCase
{
	private static final Logger log = Logger.getLogger(AbstractRemoteDataClientTest.class);
	private ThreadLocal<RemoteDataClient> threadClient = new ThreadLocal<RemoteDataClient>();
    
	protected JSONObject getSystemJson() throws JSONException, IOException {
        return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "irods-password.example.com.json");
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
    
    public String getLocalDownloadDir() {
        return LOCAL_DOWNLOAD_DIR + Thread.currentThread().getId();
    }
    
    @BeforeClass
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
        SystemDao dao = new SystemDao();
        if (dao.findBySystemId(system.getSystemId()) == null) {
            dao.persist(system);
        }
    }

    @BeforeMethod
    protected void beforeMethod() throws Exception 
    {
    	try { FileUtils.deleteDirectory(LOCAL_DOWNLOAD_DIR); } catch (IOException e1) {}
        
    	try
    	{	
    		// auth client and ensure test directory is present
    		if (client == null) {
    			client = system.getRemoteDataClient();
    		}
    		client.authenticate();
    		if (!client.doesExist("")) {
    			client.mkdirs("");
    		}
    		
    		if (!client.isDirectory("")) {
    			Assert.fail("System home directory " + client.resolvePath("") + " exists, but is not a directory.");
    		}
    	} 
    	catch (Exception e) {
    		Assert.fail("Failed to create home directory " + client.resolvePath("") + " before test method.", e);
    	}
    }

    @AfterMethod
    protected void afterMethod() throws Exception
    {	
    	try { FileUtils.deleteDirectory(LOCAL_DOWNLOAD_DIR); } catch (IOException e1) {}
        
    	try
    	{
    		// remove test directory
    		if (client.doesExist("")) {
    			client.delete("");
    			Assert.assertFalse(client.doesExist(""), "Failed to clean up home directory after test.");
    		}
    	} 
    	catch (Exception e) {
    		Assert.fail("Failed to clean up test home directory " + client.resolvePath("") + " after test method.");
    	}
    }

    @Test
    public void testVDJProjectCreation()
    {
    	long startTime = System.currentTimeMillis();
    			
    	try
    	{
    		String projectId = UUID.randomUUID().toString();
//    		1.) Create a directory called <projectId>
    		client.mkdirs(projectId);
//    		2.) Set permissions on this directory
    		client.setPermissionForUser(SHARED_SYSTEM_USER, projectId, PermissionType.ALL, true);
//    		3.) Create a directory called <projectId>/files
    		client.mkdirs(projectId + "/files");
//    		4.) Set permissions on this directory
    		client.setPermissionForUser(SHARED_SYSTEM_USER, projectId + "/files", PermissionType.ALL, true);
//    		5.) Create a directory called <projectId>/analyses
    		client.mkdirs(projectId + "/analyses");
//    		6.) Set permissions on this directory
    		client.setPermissionForUser(SHARED_SYSTEM_USER, projectId + "/analyses", PermissionType.ALL, true);
    		
    		long elapsedTime = System.currentTimeMillis() - startTime;
        	Assert.assertTrue(elapsedTime <= 5000, "Operations took longer than 5 seconds.");
        	
        	Assert.assertTrue(client.doesExist(projectId), "Project directory does not exist.");
        	Assert.assertTrue(client.hasReadPermission(projectId, SHARED_SYSTEM_USER), "Shared user does not have ALL permission on their project folder.");
        	Assert.assertTrue(client.doesExist(projectId + "/files"), "Project files directory does not exist.");
        	Assert.assertTrue(client.hasReadPermission(projectId + "/files", SHARED_SYSTEM_USER), "Shared user does not have ALL permission on their project files folder.");
        	Assert.assertTrue(client.doesExist(projectId + "/analyses"), "Project analyses directory does not exist.");
        	Assert.assertTrue(client.hasReadPermission(projectId + "/analyses", SHARED_SYSTEM_USER), "Shared user does not have ALL permission on their project analyses folder.");
        	
    	}
    	catch (Exception e) {
    		Assert.fail("Error encountered while running vdj setup commands.", e);
    	}
    	
    	long elapsedTime = System.currentTimeMillis() - startTime;
    	Assert.assertTrue(elapsedTime <= 5000, "Operations took longer than 5 seconds.");
    }
  

}
