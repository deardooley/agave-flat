package org.iplantc.service.transfer.local;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.iplantc.service.transfer.AbstractPathResolutionTests;
import org.iplantc.service.transfer.RemoteDataClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.io.Files;
@Test(groups= {"local","path-resolution","integration"})
public class LocalPathResolutionTests extends AbstractPathResolutionTests
{
	@Override
	protected JSONObject getSystemJson() throws JSONException, IOException {
		return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "local.example.com.json");
	}
	
	@BeforeClass
	@Override
    public void beforeClass() throws Exception { 
	    super.beforeClass();
	    
	    system.getStorageConfig().setRootDir(Files.createTempDir().getAbsolutePath());
	}
	
	 @AfterClass
    public void afterClass() throws Exception 
    {
	     super.afterClass();
	     FileUtils.deleteQuietly(new File(system.getStorageConfig().getRootDir()));
	     
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
		return new Local(system, rootDir, homeDir);
	}
}
