/**
 * 
 */
package org.iplantc.service.transfer.irods;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.iplantc.service.systems.model.JSONTestDataUtil;
import org.iplantc.service.transfer.AbstractRemoteDataClientTest;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.irods.jargon.core.connection.AuthScheme;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author dooley
 *
 */
@Test(singleThreaded=false, groups= {"transfer", "irods.filesystem", "irods.auth.password", "irods.version.3", "broken","integration"})
public class IrodsPasswordRemoteDataClientTest extends AbstractRemoteDataClientTest {

	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.AbstractRemoteDataClientTest#getSystemJson()
	 */
	@Override
	protected JSONObject getSystemJson() throws JSONException, IOException {
		return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "irods-password.example.com.json");
	}
	
	@Override
	@AfterMethod
	protected void afterMethod() throws Exception
    {
		
    }
	
	@Override 
	@BeforeMethod(alwaysRun=true)
    protected void beforeMethod() throws Exception {
		
	}
	
	@BeforeMethod(alwaysRun=true)
    protected void beforeMethod(java.lang.reflect.Method m) throws Exception 
    {
    	System.out.println("\n" + Thread.currentThread().getName() + Thread.currentThread().getId() + " Beginning test method " + m.getName());
    	super.beforeMethod();
    }
	
	@AfterMethod
	protected void afterMethod(java.lang.reflect.Method m) throws Exception
    {   
	    getClient().disconnect();
	    System.out.println(Thread.currentThread().getName() + Thread.currentThread().getId() + " Exiting test method " + m.getName() + "\n\n");
    }
	
	@Override
    protected String getForbiddenDirectoryPath(boolean shouldExist) {
        if (shouldExist) {
            return "/testotheruser";
        } else {
            return "/testotheruser/youdonothaveaccess";
        }
    }
    
	@Override
	protected void _isPermissionMirroringRequired()
	{
		Assert.assertEquals(system.getStorageConfig().isMirrorPermissions(), 
				getClient().isPermissionMirroringRequired(), 
				"IRODS permission mirroring should be enabled.");
	}
	
	@Override
	protected void _copyThrowsRemoteDataExceptionToRestrictedSource()
    {   
	    IRODS irods = null;
	    try 
        {
	        // if the forbidden directory is empty, irods will allow the copy...dumb
	        irods = new IRODS(system.getStorageConfig().getHost(), 
                    system.getStorageConfig().getPort(), 
                    JSONTestDataUtil.TEST_UNSHARED_OWNER,
                    JSONTestDataUtil.TEST_UNSHARED_OWNER,
                    system.getStorageConfig().getResource(),
                    system.getStorageConfig().getZone(),
                    system.getStorageConfig().getRootDir(),
                    null,
                    system.getStorageConfig().getHomeDir(),
                    AuthScheme.STANDARD);
	        
	        irods.authenticate();
	        irods.put(LOCAL_BINARY_FILE, getForbiddenDirectoryPath(true));
	        Assert.assertTrue(irods.doesExist(getForbiddenDirectoryPath(true) + "/" + FilenameUtils.getName(LOCAL_BINARY_FILE)),
	                "Failed to copy the forbidden file to the remote directory. Copy will result in a false positive.");
        }
	    catch (Exception e) {
            Assert.fail("Failed to copy the forbidden file to the remote directory. Copy will result in a false positive.", e);
        }
	    
	    try {    
            // we need to copy a 
            getClient().copy(getForbiddenDirectoryPath(true), "foo");
           
            Assert.fail("copy a restricted remote path to a dest path should throw RemoteDataException.");
        }
        catch (RemoteDataException | FileNotFoundException e) {
            Assert.assertTrue(true);
        }
        catch (Exception e) {
            Assert.fail("copy a restricted remote path to a dest path should throw RemoteDataException or FileNotFoundException.", e);
        }
	    finally {
	        try { irods.delete(getForbiddenDirectoryPath(true) + "/" + FilenameUtils.getName(LOCAL_BINARY_FILE)); } catch (Exception e) {};
	    }
    }
}
