package org.iplantc.service.transfer.sftp;

import java.io.IOException;

import org.iplantc.service.transfer.AbstractPathResolutionTests;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.sftp.MaverickSFTP;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;

@Test(groups= {"sftp","path-resolution","broken"})
public class SftpPathResolutionTests extends AbstractPathResolutionTests
{
	@Override
	protected JSONObject getSystemJson() throws JSONException, IOException {
		return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "sftp.example.com.json");
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
		return new MaverickSFTP(storageConfig.getHost(), 
				storageConfig.getPort(), 
				storageConfig.getDefaultAuthConfig().getUsername(), 
				storageConfig.getDefaultAuthConfig().getPassword(), 
				rootDir,  
				homeDir);
	}
}
