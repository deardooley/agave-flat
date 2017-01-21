package org.iplantc.service.remote.ssh;

import java.io.IOException;

import org.codehaus.plexus.util.StringUtils;
import org.iplantc.service.remote.AbstractRemoteSubmissionClientTest;
import org.iplantc.service.systems.model.JSONTestDataUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

public class MaverickSSHSubmissionClientTest extends
		AbstractRemoteSubmissionClientTest {

	@Test
	public void canAuthentication() {
		try {
			Assert.assertTrue(getClient().canAuthentication(), "Authentication should succeed on testbed system.");
		} 
		catch (Throwable t) {
			Assert.fail("No exceptions should be thrown on authentication.", t);
		}
	}

	@Test(dependsOnMethods={"canAuthentication"})
	public void runCommand() {
		try {
			String response = getClient().runCommand("whoami");
			Assert.assertEquals(StringUtils.trim(response), 
					system.getLoginConfig().getDefaultAuthConfig().getUsername(), 
					"Response from whoami on remote system should be the login config default auth config username");
		}
		catch (Exception e) {
			Assert.fail("Running whoami should not throw exception.", e);
		}
	}

	@Override
	protected JSONObject getSystemJson() throws JSONException, IOException {
		return jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE);
	}
}
