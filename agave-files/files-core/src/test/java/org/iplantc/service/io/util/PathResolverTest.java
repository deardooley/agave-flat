package org.iplantc.service.io.util;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups={"unit"})
public class PathResolverTest {

	@DataProvider(name="resovleProvider")
	public Object[][] resovleProvider()
	{
		String basePath = "/files/";
		String[] resourceTypes = { "listings", "media", "pems", "meta", "history" };
		int i=0;
		
		Object[][] testCases = new Object[resourceTypes.length*22][2];
		for (String resourceType: resourceTypes) {
			testCases[i++] = new Object[] { basePath + resourceType + "/", "" };
			testCases[i++] = new Object[] { basePath + resourceType + "//", "/" };
			testCases[i++] = new Object[] { basePath + resourceType + "//username", "/username" };
			testCases[i++] = new Object[] { basePath + resourceType + "/username", "username" };
			testCases[i++] = new Object[] { basePath + resourceType + "/username/", "username/" };
			testCases[i++] = new Object[] { basePath + resourceType + "/username/relative/path", "username/relative/path" };
			testCases[i++] = new Object[] { basePath + resourceType + "/username/relative/path/", "username/relative/path/" };
			testCases[i++] = new Object[] { basePath + resourceType + "/system/sftp.storage.example.com/", "" };
			testCases[i++] = new Object[] { basePath + resourceType + "/system/sftp.storage.example.com", "" };
			testCases[i++] = new Object[] { basePath + resourceType + "/system/sftp.storage.example.com//", "/" };
			testCases[i++] = new Object[] { basePath + resourceType + "/system/sftp.storage.example.com///", "//" };
			testCases[i++] = new Object[] { basePath + resourceType + "/system/sftp.storage.example.com////", "///" };
			testCases[i++] = new Object[] { basePath + resourceType + "/system/sftp.storage.example.com/////", "////" };
			testCases[i++] = new Object[] { basePath + resourceType + "/system/sftp.storage.example.com/..", ".." };
			testCases[i++] = new Object[] { basePath + resourceType + "/system/sftp.storage.example.com/../", "../" };
			testCases[i++] = new Object[] { basePath + resourceType + "/system/sftp.storage.example.com/../..", "../.." };
			testCases[i++] = new Object[] { basePath + resourceType + "/system/sftp.storage.example.com/../../", "../../" };
			testCases[i++] = new Object[] { basePath + resourceType + "/system/sftp.storage.example.com//username", "/username" };
			testCases[i++] = new Object[] { basePath + resourceType + "/system/sftp.storage.example.com/username", "username" };
			testCases[i++] = new Object[] { basePath + resourceType + "/system/sftp.storage.example.com/username/", "username/" };
			testCases[i++] = new Object[] { basePath + resourceType + "/system/sftp.storage.example.com/username/relative/path", "username/relative/path" };
			testCases[i++] = new Object[] { basePath + resourceType + "/system/sftp.storage.example.com/username/relative/path/", "username/relative/path/" };
		};
		
		return testCases;
	}
	
	@Test(dataProvider="resovleProvider")
	public void resolve(String originalPath, String expectedString) throws IOException
	{
		String resolvedPath = PathResolver.resolve("username", originalPath);
		Assert.assertEquals(resolvedPath, expectedString, "Resolved path does not match expected path.");
	}
}
