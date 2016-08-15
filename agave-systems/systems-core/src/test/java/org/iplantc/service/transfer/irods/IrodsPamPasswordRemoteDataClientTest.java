/**
 * 
 */
package org.iplantc.service.transfer.irods;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;

/**
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"irods.filesystem", "irods.auth.pam", "irods.version.3", "broken"})
public class IrodsPamPasswordRemoteDataClientTest extends IrodsPasswordRemoteDataClientTest {

	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.AbstractRemoteDataClientTest#getSystemJson()
	 */
	@Override
	protected JSONObject getSystemJson() throws JSONException, IOException {
		return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "irods-pam.example.com.json");
	}
}
