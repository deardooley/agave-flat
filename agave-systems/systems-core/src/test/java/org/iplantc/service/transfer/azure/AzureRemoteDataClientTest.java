/**
 * 
 */
package org.iplantc.service.transfer.azure;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.transfer.AbstractRemoteDataClientTest;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author dooley
 *
 */

@Test(groups= {"azure","filesystem", "integration"})
public class AzureRemoteDataClientTest extends AbstractRemoteDataClientTest {

	@Override
	@BeforeClass
    public void beforeSubclass() throws Exception {
    	super.beforeClass();
    	
    	JSONObject json = getSystemJson();
    	json.remove("id");
    	json.put("id", this.getClass().getSimpleName());
		system = (StorageSystem)StorageSystem.fromJSON(json);
    	system.setOwner(SYSTEM_USER);
    	String homeDir = system.getStorageConfig().getHomeDir();
    	homeDir = StringUtils.isEmpty(homeDir) ? "" : homeDir;
    	system.getStorageConfig().setHomeDir( "/agave-data-unittests6");
        storageConfig = system.getStorageConfig();
        salt = system.getSystemId() + storageConfig.getHost() + 
        		storageConfig.getDefaultAuthConfig().getUsername();
        
        new SystemDao().persist(system);
    }
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.AbstractRemoteDataClientTest#getSystemJson()
	 */
	@Override
	protected JSONObject getSystemJson() throws JSONException, IOException {
		return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "azure.example.com.json");
	}

	@Override
	protected String getForbiddenDirectoryPath(boolean shouldExist) {
		if (shouldExist) {
			return "/";
		} else {
			return "/helloworld";
		}
	}
}
