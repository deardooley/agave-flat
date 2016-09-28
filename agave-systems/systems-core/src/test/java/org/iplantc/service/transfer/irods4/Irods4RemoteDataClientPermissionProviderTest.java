/**
 * 
 */
package org.iplantc.service.transfer.irods4;

import java.io.IOException;

import org.iplantc.service.transfer.irods.IrodsRemoteDataClientPermissionProviderTest;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;

/**
 * Test for all IRODS client permission implementations. This inherits nearly
 * all it's functionality from the parent class.
 * 
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"permissions.irods4.password"})
public class Irods4RemoteDataClientPermissionProviderTest extends IrodsRemoteDataClientPermissionProviderTest 
{
    /* (non-Javadoc)
     * @see org.iplantc.service.transfer.RemoteDataClientPermissionProviderTest#getSystemJson()
     */
    @Override
    protected JSONObject getSystemJson() throws JSONException, IOException {
        return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "irods4.example.com.json");
//    	return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "qairods.cyverse.org.json");
    }
}