package org.iplantc.service.io.model;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.io.BaseTestCase;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageConfig;
import org.iplantc.service.systems.model.StorageSystem;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LogicalFileTest extends BaseTestCase {

	@BeforeClass
	@Override
	protected void beforeClass() throws Exception {
		super.beforeClass();

		clearLogicalFiles();
		initSystems();
	}

	@AfterClass
	@Override
	protected void afterClass() throws Exception {
		clearLogicalFiles();
		clearSystems();
	}

	@DataProvider
	public Object[][] getAgaveRelativePathFromAbsolutePathProvider() {
		return new Object[][] { 
			{ "/", "/", "/foo/bar/baz", "/foo/bar/baz" },
			{ "/", "/foo", "/foo/bar/baz", "/foo/bar/baz" },
			{ "/", "/foo/bar/baz", "/foo/bar/baz", "/foo/bar/baz" },
		};
	}

	@Test(dataProvider = "getAgaveRelativePathFromAbsolutePathProvider", groups={"broken"})
	public void getAgaveRelativePathFromAbsolutePath(String rootDir,
			String homeDir, String path, String expectedUrl) {
		
		StorageConfig config = new StorageConfig();
		config.setRootDir(rootDir);
		config.setHomeDir(homeDir);

		RemoteSystem system = mock(StorageSystem.class);
		when(system.getSystemId()).thenReturn("lftest.example.com");
		when(system.getStorageConfig()).thenReturn(config);

		LogicalFile logicalFile = new LogicalFile(SYSTEM_OWNER, system, path);
		Assert.assertEquals(logicalFile.getPublicLink(), expectedUrl,
				"Expected URL path does not match expected url path.");
	}

	@Test(groups={"notReady"})
	public void getMetadataLink() {
		throw new RuntimeException("Test not implemented");
	}

	@Test(groups={"notReady"})
	public void getOwnerLink() {
		throw new RuntimeException("Test not implemented");
	}

	@Test(groups={"notReady"})
	public void getPublicLink() {
		throw new RuntimeException("Test not implemented");
	}

	@Test(groups={"notReady"})
	public void getSourceUri() {
		throw new RuntimeException("Test not implemented");
	}
}
