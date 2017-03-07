package org.iplantc.service.transfer;

import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.RemoteCredentialException;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.model.TransferTask;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.*;

@Test(singleThreaded = true, groups = { "transfer", "irods.filesystem.init",
		"broken" })
public class RemoteTransferListenerTest extends BaseTransferTestCase {

	protected File tmpFile = null;
	protected File tempDir = null;
	protected ThreadLocal<RemoteDataClient> threadClient = new ThreadLocal<RemoteDataClient>();

	private static final Logger log = Logger
			.getLogger(DefaultRemoteDataClientTest.class);

	// protected abstract JSONObject getSystemJson() throws JSONException,
	// IOException;
	protected JSONObject getSystemJson() throws JSONException, IOException {
		return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/"
				+ "sftp.example.com.json");
	}

	/**
	 * Gets getClient() from current thread
	 * 
	 * @return
	 * @throws RemoteCredentialException
	 * @throws RemoteDataException
	 */
	protected RemoteDataClient getClient() {
		RemoteDataClient client;
		try {
			if (threadClient.get() == null) {
				client = system.getRemoteDataClient();
				client.updateSystemRoots(client.getRootDir(), system
						.getStorageConfig().getHomeDir()
						+ "/thread-"
						+ Thread.currentThread().getId());
				threadClient.set(client);
			}
		} catch (RemoteDataException | RemoteCredentialException e) {
			Assert.fail("Failed to get client", e);
		}

		return threadClient.get();
	}

	protected String getLocalDownloadDir() {
		return LOCAL_DOWNLOAD_DIR + Thread.currentThread().getId();
	}

	@BeforeClass(alwaysRun = true)
	protected void beforeSubclass() throws Exception {
		super.beforeClass();

		JSONObject json = getSystemJson();
		json.remove("id");
		json.put("id", this.getClass().getSimpleName());
		system = (StorageSystem) StorageSystem.fromJSON(json);
		system.setOwner(SYSTEM_USER);
		String homeDir = system.getStorageConfig().getHomeDir();
		homeDir = StringUtils.isEmpty(homeDir) ? "" : homeDir;
		system.getStorageConfig().setHomeDir(
				homeDir + "/" + getClass().getSimpleName());
		storageConfig = system.getStorageConfig();
		salt = system.getSystemId() + storageConfig.getHost()
				+ storageConfig.getDefaultAuthConfig().getUsername();
		SystemDao dao = new SystemDao();
		if (dao.findBySystemId(system.getSystemId()) == null) {
			dao.persist(system);
		}
	}

	@AfterClass(alwaysRun = true)
	protected void afterClass() throws Exception {
		try {
			FileUtils.deleteQuietly(new File(getLocalDownloadDir()));
			FileUtils.deleteQuietly(tmpFile);
			FileUtils.deleteQuietly(tmpFile);
			clearSystems();
		} finally {
			try {
				getClient().disconnect();
			} catch (Exception e) {
			}
		}

		try {
			getClient().authenticate();
			// remove test directory
			getClient().delete("..");
			Assert.assertFalse(getClient().doesExist(""),
					"Failed to clean up home directory after test.");
		} catch (Exception e) {
			Assert.fail("Failed to clean up test home directory "
					+ getClient().resolvePath("") + " after test method.", e);
		} finally {
			try {
				getClient().disconnect();
			} catch (Exception e) {
			}
		}
	}

	@BeforeMethod(alwaysRun = true)
	protected void beforeMethod() throws Exception {
		FileUtils.deleteQuietly(new File(getLocalDownloadDir()));
		FileUtils.deleteQuietly(tmpFile);

		try {
			// auth client and ensure test directory is present
			getClient().authenticate();
			if (getClient().doesExist("")) {
				getClient().delete("");
			}

			getClient().mkdirs("");

			if (!getClient().isDirectory("")) {
				Assert.fail("System home directory " + client.resolvePath("")
						+ " exists, but is not a directory.");
			}
		} catch (IOException e) {
			throw e;
		} catch (RemoteDataException e) {
			throw e;
		} catch (Exception e) {
			Assert.fail("Failed to create home directory "
					+ (client == null ? "" : client.resolvePath(""))
					+ " before test method.", e);
		}
	}

	@AfterMethod(alwaysRun = true)
	protected void afterMethod() throws Exception {
	}

	@Test
	public void cancel() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void completed() throws Exception {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void failed() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getOverallStatusCallback() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getTransferTask() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void isCancelled() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void markerArrived() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void overallStatusCallback() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void perfMarkerArrived() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void progressed() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void restartMarkerArrived() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void setTransferTask() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void skipped() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void started() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void statusCallback() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void transferAsksWhetherToForceOperation() {
		throw new RuntimeException("Test not implemented");
	}
}
