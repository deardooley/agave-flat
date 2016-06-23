package org.iplantc.service.transfer;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.ietf.jgss.GSSCredential;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.transfer.gridftp.GridFTP;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(singleThreaded=true, groups= {"unit", "path-resolution"})
public abstract class AbstractPathResolutionTests extends BaseTransferTestCase 
{
	protected abstract JSONObject getSystemJson() throws JSONException, IOException;
	
	@BeforeClass
    public void beforeClass() throws Exception {
    	super.beforeClass();
    	
    	JSONObject json = getSystemJson();
    	system = (StorageSystem)StorageSystem.fromJSON(json);
    	system.setOwner(SYSTEM_USER);
    	system.getStorageConfig().setHomeDir(system.getStorageConfig() + "/unittests-" + System.currentTimeMillis());
        storageConfig = system.getStorageConfig();
        String oldSalt = system.getSystemId() + storageConfig.getHost() + 
        		storageConfig.getDefaultAuthConfig().getUsername();
        
        salt = this.getClass().getSimpleName() + 
				system.getStorageConfig().getHost() + 
    			system.getStorageConfig().getDefaultAuthConfig().getUsername();
		
        if (!StringUtils.isEmpty(system.getStorageConfig().getDefaultAuthConfig().getPassword())) {
        	system.getStorageConfig().getDefaultAuthConfig().setPassword(system.getStorageConfig().getDefaultAuthConfig().getClearTextPassword(oldSalt));
        	system.getStorageConfig().getDefaultAuthConfig().encryptCurrentPassword(salt);
        }
        
        if (!StringUtils.isEmpty(system.getStorageConfig().getDefaultAuthConfig().getPublicKey())) {
        	system.getStorageConfig().getDefaultAuthConfig().setPublicKey(system.getStorageConfig().getDefaultAuthConfig().getClearTextPublicKey(oldSalt));
        	system.getStorageConfig().getDefaultAuthConfig().encryptCurrentPublicKey(salt);
        }
        
        if (!StringUtils.isEmpty(system.getStorageConfig().getDefaultAuthConfig().getPrivateKey())) {
        	system.getStorageConfig().getDefaultAuthConfig().setPrivateKey(system.getStorageConfig().getDefaultAuthConfig().getClearTextPrivateKey(oldSalt));
        	system.getStorageConfig().getDefaultAuthConfig().encryptCurrentPrivateKey(salt);
        }
        
        if (!StringUtils.isEmpty(system.getStorageConfig().getDefaultAuthConfig().getCredential())) {
        	system.getStorageConfig().getDefaultAuthConfig().encryptCurrentCredential(salt);
        }
		
		system.setSystemId("serializedCredentialTest");
		
		SystemDao dao = Mockito.mock(SystemDao.class);
        Mockito.when(dao.findBySystemId(Mockito.anyString()))
            .thenReturn(system);
    }
    
    @AfterClass
    public void afterClass() throws Exception 
    {
    	clearSystems();
    }
    
    protected abstract RemoteDataClient createRemoteDataClient(String rootDir, String homeDir)  throws Exception;
    
	@DataProvider(name = "resolvePathProvider", parallel=true)
    public Object[][] resolvePathProvider() throws Exception 
    {	
		
		RemoteDataClient noRootNoHome = createRemoteDataClient("",  "");
    	RemoteDataClient absoluteRootNoHome = createRemoteDataClient("/root",  "");
    	RemoteDataClient relateveRootNoHome = createRemoteDataClient("root",  "");
    	RemoteDataClient noRootAbsoluteHome = createRemoteDataClient("",  "/home");
    	RemoteDataClient noRootRelativeHome = createRemoteDataClient("",  "home");
    	RemoteDataClient absoluteRootRelativeHome = createRemoteDataClient("/root",  "home");
    	RemoteDataClient relativeRootAbsoluteHome = createRemoteDataClient("root",  "/home");
    	RemoteDataClient absoluteRootAbsoluteHome = createRemoteDataClient("/root",  "/home");

    	return new Object[][] {
    			{ noRootNoHome, "dooley", "/dooley", false, "no root no home defaults to /, so /dooley" },
        		{ noRootNoHome, "/dooley", "/dooley", false, "no root no home all paths return unchanged" },
        		{ noRootNoHome, "../", "../", true, "noRootNoHome relative paths outside of rootDir should throw exception" },
        		{ noRootNoHome, "../root", "../root", true, "noRootNoHome relative path outside of rootDir should throw exception" },
        		
        		{ absoluteRootNoHome, "dooley", "/root/dooley", false, "absoluteRootNoHome all paths return unchanged" },
        		{ absoluteRootNoHome, "/dooley", "/root/dooley", false, "absoluteRootNoHome all paths return unchanged" },
        		{ absoluteRootNoHome, "..", "", true, "absoluteRootNoHome relative path outside of rootDir should throw exception" },
        		{ absoluteRootNoHome, "../", "", true, "absoluteRootNoHome relative path outside of rootDir should throw exception" },
        		{ absoluteRootNoHome, "/..", "", true, "absoluteRootNoHome absolute path outside of rootDir should throw exception" },
        		{ absoluteRootNoHome, "/../", "", true, "absoluteRootNoHome absolute path outside of rootDir should throw exception" },
        		{ absoluteRootNoHome, "/../root", "/root", false, "absoluteRootNoHome absolute path inside of rootDir return valid path" },
        		{ absoluteRootNoHome, "../root", "/root", false, "absoluteRootNoHome relative path inside of rootDir return valid path" },
        		{ absoluteRootNoHome, "/../root/../", "", true,"absoluteRootNoHome absolute path outside of rootDir should throw exception" },
        		{ absoluteRootNoHome, "../root/../", "", true,"absoluteRootNoHome relative path outside of rootDir should throw exception" },
        		
        		{ relateveRootNoHome, "dooley", "root/dooley", false, "relative root no home all paths return unchanged" },
        		{ relateveRootNoHome, "/dooley", "root/dooley", false, "relative root no home all paths return unchanged" },
        		{ relateveRootNoHome, "..", "", true, "relateveRootNoHome relative path outside of rootDir should throw exception" },
        		{ relateveRootNoHome, "../", "", true, "relateveRootNoHome relative path outside of rootDir should throw exception" },
        		{ relateveRootNoHome, "/..", "", true, "relateveRootNoHome absolute path outside of rootDir should throw exception" },
        		{ relateveRootNoHome, "/../", "", true, "relateveRootNoHome absolute path outside of rootDir should throw exception" },
        		{ relateveRootNoHome, "/../root", "root", false, "relateveRootNoHome absolute path inside of rootDir return valid path" },
        		{ relateveRootNoHome, "../root", "root", false, "relateveRootNoHome relative path inside of rootDir return valid path" },
        		{ relateveRootNoHome, "/../root/../", "", true,"relateveRootNoHome absolute path outside of rootDir should throw exception" },
        		{ relateveRootNoHome, "../root/../", "", true,"relateveRootNoHome relative path outside of rootDir should throw exception" },
        		
        		{ noRootAbsoluteHome, "dooley", "/home/dooley", false, "no root absolute home all paths return unchanged" },
        		{ noRootAbsoluteHome, "/dooley", "/dooley", false, "no root absolute home all paths return unchanged" },
        		{ noRootAbsoluteHome, "..", "/", false, "noRootAbsoluteHome relative path outside of rootDir should resolve" },
        		{ noRootAbsoluteHome, "../", "/", false, "noRootAbsoluteHome relative path outside of rootDir should resolve" },
        		{ noRootAbsoluteHome, "/..", "/", true, "noRootAbsoluteHome absolute path outside of rootDir should throw exception" },
        		{ noRootAbsoluteHome, "/../", "/", true, "noRootAbsoluteHome absolute path outside of rootDir should throw exception" },

        		{ noRootAbsoluteHome, "/../root", "/root", true, "noRootAbsoluteHome absolute path outside of rootDir should throw exception" },
        		{ noRootAbsoluteHome, "../root", "/root", false, "noRootAbsoluteHome relative path inside of rootDir should resolve" },
        		{ noRootAbsoluteHome, "/../root/../", "/", true,"noRootAbsoluteHome absolute path outside of rootDir should throw exception" },
        		{ noRootAbsoluteHome, "../root/../", "/", false,"noRootAbsoluteHome relative path outside of rootDir should resolve" },
        		
        		{ noRootRelativeHome, "dooley", "/home/dooley", false, "no root relative home all paths return unchanged" },
        		{ noRootRelativeHome, "/dooley", "/dooley", false, "no root relative home all paths return unchanged" },
                // relative path should resolve to "" with a relative Home of "home/"
        		{ noRootRelativeHome, "..", "/", false, "noRootRelativeHome relative path outside of rootDir should return unchanged" },
                    // relative path should resolve to "" with a relative Home of "home/"
        		{ noRootRelativeHome, "../", "/", false, "noRootRelativeHome relative path outside of rootDir should return unchanged" },
        		{ noRootRelativeHome, "/..", "/", true, "noRootRelativeHome absolute path outside of rootDir should throw exception" },
        		{ noRootRelativeHome, "/../", "/", true, "noRootRelativeHome absolute path outside of rootDir should throw excpetion" },

        		{ noRootRelativeHome, "/../root", "/root", true, "noRootRelativeHome absolute path outside of rootDir should throw exception" },
                // "home/../root" should resolve to "root"
        		{ noRootRelativeHome, "../root", "/root", false, "noRootRelativeHome relative path inside of rootDir should resolve" },
        		{ noRootRelativeHome, "/../root/../", "/", true,"noRootRelativeHome absolute path outside of rootDir should throw exception" },
                // // "home/../root/.." should resolve to ""
        		{ noRootRelativeHome, "../root/../", "/", false,"noRootRelativeHome relative path outside of rootDir should resolve" },
        		
        		{ absoluteRootRelativeHome, "dooley", "/root/home/dooley", false, "absolute root relative home all paths return unchanged" },
        		{ absoluteRootRelativeHome, "/dooley", "/root/dooley", false, "absolute root relative home all paths return unchanged" },
        		{ absoluteRootRelativeHome, "..", "/root/", false, "absoluteRootRelativeHome relative path outside to rootDir should return unchanged" },
        		{ absoluteRootRelativeHome, "../", "/root/", false, "absoluteRootRelativeHome relative path outside to rootDir should return unchanged" },
        		{ absoluteRootRelativeHome, "/..", "/", true, "absoluteRootRelativeHome absolute path outside of rootDir throw exception" },
        		{ absoluteRootRelativeHome, "/../", "/", true, "absoluteRootRelativeHome absolute path outside of rootDir throw exception" },
        		{ absoluteRootRelativeHome, "/../root", "/root", false, "absoluteRootRelativeHome absolute path inside of rootDir should resolve" },
        		{ absoluteRootRelativeHome, "../root", "/root/root", false, "absoluteRootRelativeHome relative path inside of rootDir should resolve" },
        		{ absoluteRootRelativeHome, "/../root/../", "/", true,"absoluteRootRelativeHome absolute path outside of rootDir should throw exception" },
        		{ absoluteRootRelativeHome, "../root/../", "/root/", false,"absoluteRootRelativeHome relative path inside of rootDir should resolve" },
        		
        		{ relativeRootAbsoluteHome, "dooley", "root/home/dooley", false, "relative root absolute home all paths return unchanged" },
        		{ relativeRootAbsoluteHome, "/dooley", "root/dooley", false, "relative root absolute home all paths return unchanged" },
        		{ relativeRootAbsoluteHome, "..", "root/", false, "relativeRootAbsoluteHome relative path outside to rootDir should return unchanged" },
        		{ relativeRootAbsoluteHome, "../", "root/", false, "relativeRootAbsoluteHome relative path outside to rootDir should return unchanged" },
        		{ relativeRootAbsoluteHome, "/..", "", true, "relativeRootAbsoluteHome absolute path outside of rootDir throw exception" },
        		{ relativeRootAbsoluteHome, "/../", "", true, "relativeRootAbsoluteHome absolute path outside of rootDir throw exception" },
        		{ relativeRootAbsoluteHome, "/../root", "root", false, "relativeRootAbsoluteHome absolute path inside of rootDir should resolve" },
        		{ relativeRootAbsoluteHome, "../root", "root/root", false, "relativeRootAbsoluteHome relative path inside of rootDir should resolve" },
        		{ relativeRootAbsoluteHome, "/../root/../", "", true,"relativeRootAbsoluteHome absolute path outside of rootDir should throw exception" },
        		{ relativeRootAbsoluteHome, "../root/../", "root/", false,"relativeRootAbsoluteHome relative path inside of rootDir should resolve" },
        		
        		{ absoluteRootAbsoluteHome, "dooley", "/root/home/dooley", false, "absolute root absolute home all paths return unchanged" },
        		{ absoluteRootAbsoluteHome, "/dooley", "/root/dooley", false, "absolute root absolute home all paths return unchanged" },
        		{ absoluteRootAbsoluteHome, "..", "/root/", false, "absoluteRootAbsoluteHome relative path outside to rootDir should return unchanged" },
        		{ absoluteRootAbsoluteHome, "../", "/root/", false, "absoluteRootAbsoluteHome relative path outside to rootDir should return unchanged" },
        		{ absoluteRootAbsoluteHome, "/..", "/", true, "absoluteRootAbsoluteHome absolute path outside of rootDir throw exception" },
        		{ absoluteRootAbsoluteHome, "/../", "/", true, "absoluteRootAbsoluteHome absolute path outside of rootDir throw exception" },
        		{ absoluteRootAbsoluteHome, "/../root", "/root", false, "absoluteRootAbsoluteHome absolute path inside of rootDir should resolve" },
        		{ absoluteRootAbsoluteHome, "../root", "/root/root", false, "absoluteRootAbsoluteHome relative path inside of rootDir should resolve" },
        		{ absoluteRootAbsoluteHome, "/../root/../", "/", true,"absoluteRootAbsoluteHome absolute path outside of rootDir should throw exception" },
        		{ absoluteRootAbsoluteHome, "../root/../", "/root/", false,"absoluteRootAbsoluteHome relative path inside of rootDir should resolve" },
    	};
    }
	
	/**
	 * Tests whether the given path resolves correctly against the virutal root and home for a given protocol's remote data client.
	 * 
	 * @param client
	 * @param beforePath
	 * @param resolvedPath
	 * @param shouldThrowException
	 * @param message
	 */
	protected void abstractResolvePath(RemoteDataClient client, String beforePath, String resolvedPath, boolean shouldThrowException, String message)
	{
//		System.out.println("Running resolvePath: " + client + ", " + beforePath + ", " + resolvedPath + ", " + shouldThrowException + ", " + message);
    	boolean actuallyThrewException = false;
    	
		try 
    	{
    		String afterPath = client.resolvePath(beforePath);
    		Assert.assertEquals(afterPath, resolvedPath, 
    				"Resolved path " + afterPath + " did not match the expected resolved path of " + resolvedPath);
    	} 
    	catch (Exception e) {
    		actuallyThrewException = true;
        	if (!shouldThrowException) e.printStackTrace();
        }
    	
    	Assert.assertEquals(actuallyThrewException, shouldThrowException, message);
	}
	
	public abstract void resolvePath(RemoteDataClient client, String beforePath, String resolvedPath, boolean shouldThrowException, String message);
	

}
