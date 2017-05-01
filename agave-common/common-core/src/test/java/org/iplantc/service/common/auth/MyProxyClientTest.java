package org.iplantc.service.common.auth;

import java.io.File;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.ietf.jgss.GSSCredential;
import org.iplantc.service.common.Settings;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.io.Files;

@Test(singleThreaded=true, groups = {"integration"} )
public class MyProxyClientTest {

	@BeforeMethod
	public void beforeTest() {
		
	}

	@Test
	public void getCommunityCredential() {
		try {
			GSSCredential cred = MyProxyClient.getCommunityCredential();
			Assert.assertNotNull(cred, "Credential should be returned from "
					+ Settings.TACC_MYPROXY_SERVER);
			Assert.assertTrue(cred.getRemainingLifetime() > 0,
					"Credential returned from " + Settings.TACC_MYPROXY_SERVER
							+ " is expired");
			Assert.assertEquals(cred.getClass().getName(),
					GlobusGSSCredentialImpl.class.getName());

			File trustrootDirectory = new File(MyProxy.getTrustRootPath());
			Assert.assertTrue(
					trustrootDirectory.exists(),
					"Trustroot directory "
							+ trustrootDirectory.getAbsolutePath()
							+ " was not created when the credential was retrieved.");

			Collection<File> trustrootFiles = FileUtils.listFiles(
					trustrootDirectory, new String[] { "0" }, true);
			Assert.assertFalse(trustrootFiles.isEmpty(),
					"No trustroots were retrieved from "
							+ Settings.TACC_MYPROXY_SERVER
							+ " with the credential");

		} catch (Throwable e) {
			Assert.fail("No exception should be thrown "
					+ "retrieving a community credential", e);
		}
	}

	@Test
	public void getCredential() {
		try {
			GSSCredential cred = MyProxyClient.getCredential(
					Settings.TACC_MYPROXY_SERVER, Settings.TACC_MYPROXY_PORT,
					Settings.COMMUNITY_PROXY_USERNAME,
					Settings.COMMUNITY_PROXY_PASSWORD, null);

			Assert.assertNotNull(cred, "Credential shoudl be returned from "
					+ Settings.TACC_MYPROXY_SERVER);
			Assert.assertTrue(cred.getRemainingLifetime() > 0,
					"Credential returned from " + Settings.TACC_MYPROXY_SERVER
							+ " is expired");
			Assert.assertEquals(cred.getClass().getName(),
					GlobusGSSCredentialImpl.class.getName());

			File trustrootDirectory = new File(MyProxy.getTrustRootPath());
			Assert.assertTrue(
					trustrootDirectory.exists(),
					"Trustroot directory "
							+ trustrootDirectory.getAbsolutePath()
							+ " was not created when the credential was retrieved.");

			Collection<File> trustrootFiles = FileUtils.listFiles(
					trustrootDirectory, new String[] { "0" }, true);
			Assert.assertFalse(trustrootFiles.isEmpty(),
					"No trustroots were retrieved from "
							+ Settings.TACC_MYPROXY_SERVER
							+ " with the credential");
		} catch (Throwable e) {
			Assert.fail(
					"No exception should be thrown retrieving a credential", e);
		}
	}

	@Test
	public void getCredentialAtCustomCACertPath() {
		try {
			File tempDir = Files.createTempDir();

			GSSCredential cred = MyProxyClient.getCredential(
					Settings.TACC_MYPROXY_SERVER, Settings.TACC_MYPROXY_PORT,
					Settings.COMMUNITY_PROXY_USERNAME,
					Settings.COMMUNITY_PROXY_PASSWORD, 
					tempDir.getAbsolutePath());

			Assert.assertNotNull(cred, "Credential should be returned from "
					+ Settings.TACC_MYPROXY_SERVER);
			Assert.assertTrue(cred.getRemainingLifetime() > 0,
					"Credential returned from " + Settings.TACC_MYPROXY_SERVER
							+ " is expired");
			Assert.assertEquals(cred.getClass().getName(),
					GlobusGSSCredentialImpl.class.getName());
			
			Assert.assertTrue(
					tempDir.exists(),
					"Trustroot directory "
							+ tempDir.getAbsolutePath()
							+ " was no longer present after retrieving the credentials.");

			Collection<File> trustrootFiles = FileUtils.listFiles(
					tempDir, new String[] { "0" }, true);
			Assert.assertFalse(trustrootFiles.isEmpty(),
					"No trustroots were retrieved from "
							+ Settings.TACC_MYPROXY_SERVER
							+ " with the credential");

		} catch (Throwable e) {
			Assert.fail(
					"No exception should be thrown retrieving a credential", e);
		}
	}

	@Test
	public void getUserCredential() {
		try {
			GSSCredential cred = MyProxyClient.getUserCredential(
					Settings.COMMUNITY_PROXY_USERNAME,
					Settings.COMMUNITY_PROXY_PASSWORD);

			Assert.assertNotNull(cred, "Credential shoudl be returned from "
					+ Settings.TACC_MYPROXY_SERVER);
			Assert.assertTrue(cred.getRemainingLifetime() > 0,
					"Credential returned from " + Settings.TACC_MYPROXY_SERVER
							+ " is expired");
			Assert.assertEquals(cred.getClass().getName(),
					GlobusGSSCredentialImpl.class.getName());

			File trustrootDirectory = new File(
					MyProxy.getTrustRootPath());
			Assert.assertTrue(
					trustrootDirectory.exists(),
					"Trustroot directory "
							+ trustrootDirectory.getAbsolutePath()
							+ " was not created when the credential was retrieved.");

			Collection<File> trustrootFiles = FileUtils.listFiles(
					trustrootDirectory, new String[] { "0" }, true);
			Assert.assertFalse(trustrootFiles.isEmpty(),
					"No trustroots were retrieved from "
							+ Settings.TACC_MYPROXY_SERVER
							+ " with the credential");
		} catch (Throwable e) {
			Assert.fail(
					"No exception should be thrown retrieving a user credential",
					e);
		}
	}
}
