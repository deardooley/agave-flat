/**
 * 
 */
package org.iplantc.service.transfer.s3;

import java.io.IOException;

import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.transfer.AbstractReadOnlyRemoteDataClientTest;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"s3.filesystem"})
public class S3PublicRemoteDataClientTest extends AbstractReadOnlyRemoteDataClientTest 
{
	protected String containerName;
	
	@Override
	@BeforeClass
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
//        SystemDao dao = new SystemDao();
//        if (dao.findBySystemId(system.getSystemId()) == null) {
//            dao.persist(system);
//        }
        SystemDao dao = Mockito.mock(SystemDao.class);
        Mockito.when(dao.findBySystemId(Mockito.anyString()))
            .thenReturn(system);
    	containerName = system.getStorageConfig().getContainerName();
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.AbstractRemoteDataClientTest#getSystemJson()
	 */
	@Override
	protected JSONObject getSystemJson() throws JSONException, IOException {
		return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "s3-public.example.com.json");
	}
	
	@Override
	protected void _isThirdPartyTransferSupported()
	{
		Assert.assertFalse(getClient().isThirdPartyTransferSupported());
	}

	protected void _isDirectoryFalseForFile()
    {
        try 
        {
            Assert.assertFalse(getClient().isDirectory(LOCAL_DIR_NAME + "/" + LOCAL_BINARY_FILE_NAME), 
                    "isDirectory should return false for file.");
        } 
        catch (Exception e) {
            Assert.fail("isDirectory should not throw unexpected exceptions", e);
        }
    }

    @Override
    protected void _isFileTrueForFile() {
    }
}
