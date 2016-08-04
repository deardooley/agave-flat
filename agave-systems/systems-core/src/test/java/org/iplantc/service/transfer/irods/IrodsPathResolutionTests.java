package org.iplantc.service.transfer.irods;

import java.io.IOException;

import org.iplantc.service.transfer.AbstractPathResolutionTests;
import org.iplantc.service.transfer.RemoteDataClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;

@Test(groups= {"irods.path-resolution", "broken"})
public class IrodsPathResolutionTests extends AbstractPathResolutionTests
{
	@Override
	protected JSONObject getSystemJson() throws JSONException, IOException {
		return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "irods.example.com.json");
	}

	@Test(dataProvider="resolvePathProvider")
	@Override
	public void resolvePath(RemoteDataClient client, String beforePath,
			String resolvedPath, boolean shouldThrowException, String message) {
		super.abstractResolvePath(client, beforePath, resolvedPath, shouldThrowException, message);
	}

	@Override
	protected RemoteDataClient createRemoteDataClient(String rootDir, String homeDir) 
	throws Exception 
	{
		return new IRODS(storageConfig.getHost(), 
				storageConfig.getPort(), 
				storageConfig.getDefaultAuthConfig().getUsername(), 
				storageConfig.getDefaultAuthConfig().getPassword(), 
				storageConfig.getResource(), 
				storageConfig.getZone(), 
				rootDir, 
				storageConfig.getDefaultAuthConfig().getInternalUsername(), 
				homeDir, 
				null);
	}
}
