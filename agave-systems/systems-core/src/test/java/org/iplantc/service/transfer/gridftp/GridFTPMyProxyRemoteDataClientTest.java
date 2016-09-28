/**
 * 
 */
package org.iplantc.service.transfer.gridftp;

import java.io.IOException;

import org.globus.ftp.exception.ServerException;
import org.ietf.jgss.GSSCredential;
import org.iplantc.service.systems.exceptions.RemoteCredentialException;
import org.iplantc.service.systems.model.StorageConfig;
import org.iplantc.service.transfer.AbstractRemoteDataClientTest;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * @author dooley
 *
 */
@Test(groups= {"gridftp","filesystem","broken"})
public class GridFTPMyProxyRemoteDataClientTest extends AbstractRemoteDataClientTest {

	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.AbstractRemoteDataClientTest#getSystemJson()
	 */
	@Override
	protected JSONObject getSystemJson() throws JSONException, IOException {
		return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "gridftp.example.com.json");
	}
	
	@Override
	protected RemoteDataClient getClient() 
    {
        RemoteDataClient client;
        try {
            if (threadClient.get() == null) {
                StorageConfig storage = system.getStorageConfig();
                GSSCredential credential = (GSSCredential)storage.getDefaultAuthConfig().retrieveCredential(salt);
                
                client = new GridFTP(storage.getHost(), storage.getPort(), storage.getDefaultAuthConfig().getUsername(), credential, storage.getRootDir(), storage.getHomeDir());
                client.updateSystemRoots(client.getRootDir(), system.getStorageConfig().getHomeDir() + "/thread-" + Thread.currentThread().getId());
                threadClient.set(client);
            } 
        } catch (RemoteCredentialException | ServerException | IOException e) {
            Assert.fail("Failed to get client", e);
        }
        
        return threadClient.get();
    }
	
	@AfterMethod
    public void afterMethod() throws Exception
    {	
		super.afterMethod();
		// seems to be a leak in the library somewhere...
    	getClient().disconnect();
    }
	
	@Override
	public void _isThirdPartyTransferSupported()
	{
		Assert.assertTrue(getClient().isThirdPartyTransferSupported(),
		        "Third party transfers should be supported by gridftp protocol.");
	}
	
	@Override
	protected String getForbiddenDirectoryPath(boolean shouldExist) {
		if (shouldExist) {
			return "/root";
		} else {
			return "/root/helloworld";
		}
	}
}
