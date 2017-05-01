/**
 * 
 */
package org.iplantc.service.transfer.local;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.NotImplementedException;
import org.iplantc.service.transfer.AbstractRemoteDataClientTest;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.util.MD5Checksum;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.io.Files;

/**
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"local.filesystem", "integration"})
public class LocalRemoteDataClientTest extends AbstractRemoteDataClientTest 
{
    @BeforeClass
    @Override
    protected void beforeSubclass() throws Exception {
        super.beforeSubclass();
        
        system.getStorageConfig().setRootDir("/");
        system.getStorageConfig().setHomeDir(Files.createTempDir().getAbsolutePath());
    }
    
	@BeforeMethod
	@Override
    protected void beforeMethod() throws Exception 
    {
    	FileUtils.deleteQuietly(new File(getLocalDownloadDir()));
        
    	try
    	{	
//    		getClient().authenticate();
    		
    		// ensure test directory is present and empty
            File homeDir = new File(getClient().resolvePath(""));
            if (homeDir.exists()) {
                FileUtils.deleteQuietly(homeDir);
            } 
            homeDir.mkdirs();
            
    	} 
    	catch (Throwable e) {
    		Assert.fail("Failed to setup before test method.", e);
    	}
    }
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.AbstractRemoteDataClientTest#getSystemJson()
	 */
	@Override
	protected JSONObject getSystemJson() throws JSONException, IOException {
		return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "local.example.com.json");
	}
	
	
	
	@Override
	protected void _isPermissionMirroringRequired()
	{
		Assert.assertFalse(getClient().isPermissionMirroringRequired(), 
				"Local permission mirroring should not be enabled.");
		
	}
	
	@Override
	protected String getForbiddenDirectoryPath(boolean shouldExist) throws RemoteDataException {
		if (shouldExist) {
		    if (org.iplantc.service.common.util.OSValidator.isMac()) {
		        return "/var/root";
		    } else {
		        return "/root";
		    }
		} else {
		    return "/" + MISSING_FILE;
		}
	}
	
	@Override
    public void _checksum()
    {
        try 
        {
            getClient().put(LOCAL_TXT_FILE, "");
            Assert.assertTrue(getClient().doesExist(LOCAL_TXT_FILE_NAME), 
                    "Data not found on remote system after put.");
        
            String md5 = getClient().checksum(LOCAL_TXT_FILE_NAME);
            Assert.assertNotNull(md5, "Local client should return checksum");
            Assert.assertEquals(md5, MD5Checksum.getMD5Checksum(new File(LOCAL_TXT_FILE)), "Checksums did not match.");
        } 
        catch (NotImplementedException e) {
            Assert.assertTrue(true);
        }
        catch (Exception e) {
            Assert.fail("Checksum should throw a NotImplementedException for SFTP", e);
        }
    }
}
