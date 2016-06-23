/**
 * 
 */
package org.iplantc.service.transfer.s3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.iplantc.service.transfer.AbstractRemoteDataClientTest;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"s3.filesystem"})
public class S3RemoteDataClientTest extends AbstractRemoteDataClientTest 
{
	protected String containerName;
	
	@Override
	@BeforeClass
    protected void beforeSubclass() throws Exception {
    	super.beforeSubclass();
    	containerName = system.getStorageConfig().getContainerName();
	}
    
	@BeforeMethod
    public void beforeMethod() throws Exception 
    {
    	FileUtils.deleteQuietly(new File(getLocalDownloadDir()));
        
    	try
    	{	
    		if (((S3Jcloud)getClient()).getBlobStore().containerExists(containerName)) {
    		    try {
    		        getClient().delete("");
    		    } catch (FileNotFoundException e) {}
    		}
    		getClient().mkdirs("");
    	} 
    	catch (RemoteDataException e) {
    		throw e;
    	}
    	catch (Throwable e) {
    		Assert.fail("Failed to setup before test method.", e);
    	}
    }
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.AbstractRemoteDataClientTest#afterMethod()
	 */
	@Override
	@AfterMethod
	public void afterMethod() throws Exception
	{
	    // just disabling the parent behavior
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.AbstractRemoteDataClientTest#getSystemJson()
	 */
	@Override
	protected JSONObject getSystemJson() throws JSONException, IOException {
		return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "s3.example.com.json");
	}
	
	@Override
    protected void _isPermissionMirroringRequired()
	{
		Assert.assertFalse(getClient().isPermissionMirroringRequired(), 
				"S3 permission mirroring should not be enabled.");
		
	}
	
    @Override
	protected void _isThirdPartyTransferSupported()
	{
		Assert.assertFalse(getClient().isThirdPartyTransferSupported());
	}

	@Override
	protected String getForbiddenDirectoryPath(boolean shouldExist) throws RemoteDataException {
		if (shouldExist) {
			throw new RemoteDataException("Bypassing test for s3 forbidden file/folder");
		} else {
			throw new RemoteDataException("Bypassing test for S3 missing file/folder");
		}
	}
	
	@Override
	protected void _copyThrowsRemoteDataExceptionToRestrictedSource()
    {
        try 
        {
            getClient().copy(MISSING_DIRECTORY, "foo");
               
            Assert.fail("copy a remote source path that does not exist should throw FileNotFoundException.");
        }
        catch (FileNotFoundException e) {
            Assert.assertTrue(true);
        }
        catch (Exception e) {
            Assert.fail("copy a remote source path that does not exist should throw FileNotFoundException.", e);
        }
    }
}
