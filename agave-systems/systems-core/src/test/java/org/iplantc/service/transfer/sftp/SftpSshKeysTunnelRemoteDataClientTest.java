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
@Test(groups= {"sftp", "sftp-sshkeys-tunnel", "filesystem", "broken", "integration" })
public class SftpSshKeysTunnelRemoteDataClientTest extends SftpPasswordRemoteDataClientTest {

	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.AbstractRemoteDataClientTest#getSystemJson()
	 */
	@Override
	@Test(enabled=false)
	protected JSONObject getSystemJson() throws JSONException, IOException {
		return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "sftp-sshkeys-tunnel.example.com.json");
	}
}
