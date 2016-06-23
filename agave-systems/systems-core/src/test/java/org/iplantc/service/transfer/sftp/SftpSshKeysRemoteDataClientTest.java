/**
 * 
 */
package org.iplantc.service.transfer.sftp;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;

/**
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"sftp","sftp-sshkeys", "filesystem"})
public class SftpSshKeysRemoteDataClientTest extends SftpPasswordRemoteDataClientTest {

	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.AbstractRemoteDataClientTest#getSystemJson()
	 */
	@Override
	@Test(enabled=false)
	protected JSONObject getSystemJson() throws JSONException, IOException {
		return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "sftp-sshkeys.example.com.json");
	}
}
