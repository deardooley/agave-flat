package org.iplantc.service.transfer.ftp;

import java.io.IOException;

import org.ietf.jgss.GSSCredential;
import org.iplantc.service.transfer.AbstractPathResolutionTests;
import org.iplantc.service.transfer.RemoteDataClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;

@Test(groups= {"ftp","path-resolution","broken", "integration"})
public class FTPPathResolutionTests extends AbstractPathResolutionTests
{
	@Override
	protected JSONObject getSystemJson() throws JSONException, IOException {
		return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "ftp.example.com.json");
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
		return new FTP(storageConfig.getHost(), 
				storageConfig.getPort(), 
				storageConfig.getDefaultAuthConfig().getUsername(),
				storageConfig.getDefaultAuthConfig().getPassword(), 
				rootDir,  
				homeDir);
	}
}
