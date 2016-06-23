/**
 * 
 */
package org.iplantc.service.apps.queue.actions;

import static org.iplantc.service.apps.model.JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE;
import static org.iplantc.service.apps.model.JSONTestDataUtil.TEST_OWNER;
import static org.iplantc.service.apps.model.JSONTestDataUtil.TEST_SOFTWARE_SYSTEM_FILE;
import static org.iplantc.service.apps.model.JSONTestDataUtil.TEST_STORAGE_SYSTEM_FILE;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.iplantc.service.apps.dao.AbstractDaoTest;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.queue.actions.WorkerAction;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.enumerations.SystemStatusType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Catchall setup class for {@link WorkerAction} test classes
 * @author dooley
 *
 */
public abstract class AbstractWorkerActionTest extends AbstractDaoTest {
    
    
    protected void initSystems() throws Exception
    {
        clearSystems();
        
        privateExecutionSystem = ExecutionSystem.fromJSON(jtd.getTestDataObject(TEST_EXECUTION_SYSTEM_FILE));
        privateExecutionSystem.setOwner(TEST_OWNER);
        systemDao.persist(privateExecutionSystem);
        
        privateStorageSystem = StorageSystem.fromJSON(jtd.getTestDataObject(TEST_STORAGE_SYSTEM_FILE));
        privateStorageSystem.setOwner(TEST_OWNER);
        privateStorageSystem.getUsersUsingAsDefault().add(TEST_OWNER);
        systemDao.persist(privateStorageSystem);
    }
    
    protected Software createSoftware(ExecutionSystem executionSystem, StorageSystem storageSystem, 
            String name, String version, boolean publiclyAvailable, String owner) 
    throws JSONException, IOException 
    {
        JSONObject json = jtd.getTestDataObject(TEST_SOFTWARE_SYSTEM_FILE);
        software = Software.fromJSON(json, TEST_OWNER);
        software.setExecutionSystem(executionSystem);
        software.setStorageSystem(storageSystem);
        software.setName(name);
        software.setVersion(version);
        software.setPubliclyAvailable(publiclyAvailable);
        software.setOwner(owner);
        return software;
    }
    
    protected Software createExecutionSystem(boolean isPubliclyAvailable, boolean isGlobalDefault, 
            SystemStatusType status, boolean isAvailable, boolean owner)
    throws JSONException, IOException 
    {
        JSONObject json = jtd.getTestDataObject(TEST_SOFTWARE_SYSTEM_FILE);
        software = Software.fromJSON(json, TEST_OWNER);
        software.setExecutionSystem(privateExecutionSystem);
        software.setOwner(TEST_OWNER);
        software.setVersion("1.0.1");
        software.setChecksum("abc12345");
        return software;
    }
    
    protected void stageRemoteSoftwareAssets(Software software) throws Exception 
    {
        RemoteSystem storageSystem = software.getStorageSystem();
        RemoteDataClient storageDataClient = null;
        try 
        {
            storageDataClient = storageSystem.getRemoteDataClient();
            storageDataClient.authenticate();
            if (!storageDataClient.doesExist(software.getDeploymentPath())) {
                storageDataClient.mkdirs(FilenameUtils.getPath(software.getDeploymentPath()));
                storageDataClient.put(SOFTWARE_SYSTEM_TEMPLATE_DIR + File.separator + software.getUniqueName(), FilenameUtils.getPath(software.getDeploymentPath()));
            }
            else
            {
                for (File localSoftwareAssetPath: new File(SOFTWARE_SYSTEM_TEMPLATE_DIR + File.separator + software.getUniqueName()).listFiles()) {
                    if (!storageDataClient.doesExist(software.getDeploymentPath() + File.separator + localSoftwareAssetPath.getName())) {
                        storageDataClient.put(localSoftwareAssetPath.getAbsolutePath(), FilenameUtils.getPath(software.getDeploymentPath()) + File.separator + localSoftwareAssetPath.getName());
                    }
                }
            }
        }
        finally {
            try {storageDataClient.disconnect();} catch (Exception e) {}
        }
        
    }
    
    protected void deleteRemoteSoftwareAssets() throws Exception 
    {
        RemoteSystem storageSystem = software.getStorageSystem();
        RemoteDataClient storageDataClient = null;
        try 
        {
            storageDataClient = storageSystem.getRemoteDataClient();
            storageDataClient.authenticate();
            storageDataClient.delete(software.getDeploymentPath());
        }
        finally {
            try {storageDataClient.disconnect();} catch (Exception e) {}
        }
        
    }

}
