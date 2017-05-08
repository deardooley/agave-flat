package org.iplantc.service.transfer.azure;

import java.io.IOException;

import org.iplantc.service.transfer.AbstractPathResolutionTests;
import org.iplantc.service.transfer.RemoteDataClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;

@Test(groups= {"azure","path-resolution", "integration"})
public class AzurePathResolutionTests extends AbstractPathResolutionTests
{
	@Override
	protected JSONObject getSystemJson() throws JSONException, IOException {
		return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "azure.example.com.json");
	}

	@Test(dataProvider="resolvePathProvider")
	@Override
	public void resolvePath(RemoteDataClient client, String beforePath, String resolvedPath, boolean shouldThrowException, String message) 
	{
		super.abstractResolvePath(client, beforePath, resolvedPath, shouldThrowException, message);
	}

	@Override
	protected RemoteDataClient createRemoteDataClient(String rootDir, String homeDir)
	throws Exception 
	{
		return new AzureJcloud(
				storageConfig.getDefaultAuthConfig().getPublicKey(), 
				storageConfig.getDefaultAuthConfig().getPrivateKey(), 
				rootDir,  
				homeDir,
				"testing",
				AzureJcloud.MEMORY_STORAGE_PROVIDER);
	}
}
