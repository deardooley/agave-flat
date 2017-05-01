package org.iplantc.service.transfer.operations;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.RemoteCredentialException;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.transfer.BaseTransferTestCase;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Test(groups={"integration"})
public class BaseRemoteDataClientOperationTest extends BaseTransferTestCase{
    
    protected String systemJsonFilePath = null;
    protected ObjectMapper mapper = new ObjectMapper();
    protected TransferOperationAfterMethod afterMethodTeardown = null;
    protected TransferOperationBeforeMethod beforeMethodSetup = null;
    protected ForbiddenPathProvider forbiddenPathProvider = null;
    
    protected ThreadLocal<RemoteDataClient> threadClient = new ThreadLocal<RemoteDataClient>();
    
    public BaseRemoteDataClientOperationTest(String systemJsonFilePath, 
                                            TransferOperationAfterMethod afterMethodTeardown,
                                            TransferOperationBeforeMethod beforeMethodSetup,
                                            ForbiddenPathProvider forbiddenPathProvider) 
    {
        this.systemJsonFilePath = systemJsonFilePath;
        this.afterMethodTeardown = afterMethodTeardown;
        this.beforeMethodSetup = beforeMethodSetup;
        this.forbiddenPathProvider = forbiddenPathProvider;
    }
    
    protected ObjectNode readObjectNode() throws JSONException, IOException {
        return (ObjectNode)mapper.readTree(new File(systemJsonFilePath));
    }
    
    protected JSONObject getSystemJson() throws JSONException, IOException {
        return jtd.getTestDataObject(systemJsonFilePath);
    }
    
    @BeforeClass
    protected void beforeClass() throws Exception {
        super.beforeClass();
        
        ObjectNode json = readObjectNode();
        json.remove("id");
        json.put("id", this.getClass().getSimpleName());
        system = (StorageSystem)StorageSystem.fromJSON(new JSONObject(json.toString()));
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
    
    @AfterClass
    protected void afterClass() throws Exception {
        try 
        {
            FileUtils.deleteQuietly(new File(getLocalDownloadDir()));
//            clearSystems();
        }
        finally {
            try { getClient().disconnect(); } catch (Exception e) {}
        }   
        
        try
        {
            getClient().authenticate();
            // remove test directory
            getClient().delete("..");
            Assert.assertFalse(getClient().doesExist(""), "Failed to clean up home directory after test.");
        } 
        catch (Exception e) {
            Assert.fail("Failed to clean up test home directory " + getClient().resolvePath("") + " after test method.", e);
        }
        finally {
            try { getClient().disconnect(); } catch (Exception e) {}
        }
    }
    
    @BeforeMethod
    protected void beforeMethod() throws Exception 
    {
        beforeMethodSetup.setup(getClient());
    }
    
    @AfterMethod
    protected void afterMethod() throws Exception 
    {
        afterMethodTeardown.teardown(getClient());
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
    
    /**
     * Since there won't be a universally forbidden path, we delegate
     * the decision to each system and protocol test. Specifying 
     * shouldExist allows you to separate permission exceptions from
     * file not found exceptions.
     * 
     * @param shouldExist. Whether the path should exist. 
     * @return 
     * @throws RemoteDataException 
     */
    protected String getForbiddenDirectoryPath(boolean shouldExist) throws RemoteDataException {
        return forbiddenPathProvider.getDirectoryPath(shouldExist);
        
//        if (shouldExist) {
//            return "/";
//        } else {
//            return "/some/silly/root/directory/path/that/should/under/no/circumstances/exist/abracadabra";
//        }
    }
    
    /**
     * Since there won't be a universally forbidden path, we delegate
     * the decision to each system and protocol test. Specifying 
     * shouldExist allows you to separate permission exceptions from
     * file not found exceptions.
     * 
     * @param shouldExist. Whether the path should exist. 
     * @return 
     * @throws RemoteDataException 
     */
    protected String getForbiddenFilePath(boolean shouldExist) throws RemoteDataException {
        return forbiddenPathProvider.getFilePath(shouldExist);
    }
    
    
    
    protected String getLocalDownloadDir() {
        return LOCAL_DOWNLOAD_DIR + Thread.currentThread().getId();
    }
}

