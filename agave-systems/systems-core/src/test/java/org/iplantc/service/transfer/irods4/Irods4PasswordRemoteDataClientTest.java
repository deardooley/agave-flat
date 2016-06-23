/**
 * 
 */
package org.iplantc.service.transfer.irods4;

import java.io.IOException;

import org.iplantc.service.transfer.AbstractRemoteDataClientTest;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"irods.filesystem", "irods.auth.password", "disabled", "irods.version.4"})
public class Irods4PasswordRemoteDataClientTest extends AbstractRemoteDataClientTest {

	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.AbstractRemoteDataClientTest#getSystemJson()
	 */
	@Override
	protected JSONObject getSystemJson() throws JSONException, IOException {
		return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "irods4.example.com.json");
	}
	
    @Override
    protected String getForbiddenDirectoryPath(boolean shouldExist) {
        return "/someobscuremadeuplocation";
    }
    
    @AfterMethod
    @Override
    protected void afterMethod() throws Exception
    {   
        getClient().disconnect();
    }
    
    @Override
    protected void _isPermissionMirroringRequired()
    {
        Assert.assertEquals(system.getStorageConfig().isMirrorPermissions(), 
                getClient().isPermissionMirroringRequired(), 
                "IRODS permission mirroring should be enabled.");
    }
}
