/**
 * 
 */
package org.iplantc.service.common.uuid;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.iplantc.service.common.exceptions.TenantException;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.UUIDEntityLookup;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.io.uuid.AbstractUUIDEntityLookupTest;
import org.iplantc.service.io.BaseTestCase;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.model.JSONTestDataUtil;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.RemoteCredentialException;
import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author dooley
 *
 */
@Test(groups={"integration"})
public class LogicalFileUUIDEntityLookupTest extends AbstractUUIDEntityLookupTest<LogicalFile> {
	
	@BeforeClass
	@Override
	protected void beforeClass() throws TenantException, SystemArgumentException, JSONException, IOException {
		super.beforeClass();
	}
	
	@AfterClass
	@Override
	protected void afterClass() {
		super.afterClass();
	}
	
	@AfterMethod
	@Override
	protected void afterMethod() {
		super.afterMethod();
	}
	
	@Override
	public UUIDType getEntityType() {
		return UUIDType.FILE;
	}

	@Override
	public LogicalFile createEntity() {
		
		return createLogicalFile(this.system);
	}
	
	protected LogicalFile createLogicalFile(RemoteSystem remoteSystem) {
		LogicalFile logicalFile = null;
		try {
			logicalFile = new LogicalFile(BaseTestCase.SYSTEM_OWNER, 
									remoteSystem, 
									"agave://" + remoteSystem.getSystemId() + "//tmp/test.dat", 
									remoteSystem.getRemoteDataClient().resolvePath(this.destPath),
									FilenameUtils.getName(this.destPath));
			LogicalFileDao.persist(logicalFile);
		} catch (FileNotFoundException | RemoteDataException | RemoteCredentialException e) {
			Assert.fail("Failed to create test entity", e);
		}
		
		return logicalFile;
	}

	@Override
	public String serializeEntityToJSON(LogicalFile testEntity) {
		
		String json = null;
		try {
			json = testEntity.toJSON();
		} catch (JSONException e) {
			Assert.fail("Failed to serialize " + getEntityType().name().toLowerCase() + " to JSON.", e);
		}
		
		return json;
	}

	@Override
	public String getEntityUuid(LogicalFile testEntity) {
		return testEntity.getUuid();
	}
	
	@Test(dependsOnMethods="getResourceUrl")
	public void getAgaveRelativePathFromAbsolutePath() {
		
		LogicalFile lf = createEntity();
		
		Assert.assertEquals(UUIDEntityLookup.getAgaveRelativePathFromAbsolutePath(
						lf.getPath(), 
						lf.getSystem().getStorageConfig().getRootDir(), 
						lf.getSystem().getStorageConfig().getHomeDir()),
						lf.getAgaveRelativePathFromAbsolutePath(),
				"Relative path calculated by UUIDEntityLookup should match that calculated by LogicalFile");
		
	}

//	/**
//	 * Generates a test case using the abstract methods implemented for this
//	 * class.
//	 * @return
//	 * @throws IOException
//	 */
//	@DataProvider
//	protected Object[][] resolveLogicalFileURLFromUUIDProvider() throws IOException {
//		LogicalFile testEntity = createEntity();
//		return new Object[][] { 
//				{ getEntityUuid(testEntity), getUrlFromEntityJson(serializeEntityToJSON(testEntity)) } };
//	}

	@Test(dependsOnMethods="getAgaveRelativePathFromAbsolutePath")
	public void resolveLogicalFileURLFromUUID() {
		LogicalFile testEntity = createEntity();
		try {
			String resolvedUrl = UUIDEntityLookup.resolveLogicalFileURLFromUUID(testEntity.getUuid());
			Assert.assertEquals(
					TenancyHelper.resolveURLToCurrentTenant(resolvedUrl, testEntity.getTenantId()),
					testEntity.getPublicLink(),
					"Relative path calculated by UUIDEntityLookup should match that calculated by LogicalFile");
		} catch (UUIDException e) {
			Assert.fail("Resolving logical file url from uuid should never throw exception", e);
		}
	}
	
	@Test(dependsOnMethods="getAgaveRelativePathFromAbsolutePath")
	public void resolveLogicalFileURLFromUUIDSystemHomeNull() {
		
		StorageSystem nullHomeSystem = null; 
		SystemDao dao = new SystemDao();
		
		try {
			JSONObject json = JSONTestDataUtil.getInstance().getTestDataObject(JSONTestDataUtil.TEST_STORAGE_SYSTEM_FILE);
			json.getJSONObject("storage").put("homeDir", (String)null);
			json.put("id", "null-home-file-uuid-resolution-test");
			nullHomeSystem = StorageSystem.fromJSON(json);
			nullHomeSystem.setOwner(BaseTestCase.SYSTEM_OWNER);
			nullHomeSystem.setAvailable(true);
			
			dao.persist(nullHomeSystem);
				
			LogicalFile testEntity = createLogicalFile(nullHomeSystem);
			
			try {
				String resolvedUrl = UUIDEntityLookup.resolveLogicalFileURLFromUUID(testEntity.getUuid());
				Assert.assertEquals(
					TenancyHelper.resolveURLToCurrentTenant(resolvedUrl, testEntity.getTenantId()),
						testEntity.getPublicLink(),
						"Relative path calculated by UUIDEntityLookup should match that calculated by LogicalFile");
			} catch (UUIDException e) {
				Assert.fail("Resolving logical file url from uuid should never throw exception", e);
			}
		}
		catch (JSONException | SystemArgumentException | IOException e) {
			Assert.fail("Failed to create test storage system with null home directory", e);
		}
		finally {
			try { dao.remove(nullHomeSystem); } catch (Exception e){};
			
		}
	}
	
	@Test(dependsOnMethods="resolveLogicalFileURLFromUUIDSystemHomeNull")
	public void resolveLogicalFileURLFromUUIDSystemHomeEmpty() {
		
		StorageSystem emtpyHomeSystem = null; 
		SystemDao dao = new SystemDao();
		
		try {
			JSONObject json = JSONTestDataUtil.getInstance().getTestDataObject(JSONTestDataUtil.TEST_STORAGE_SYSTEM_FILE);
			json.getJSONObject("storage").put("homeDir", "/");
			json.put("id", "empty-home-file-uuid-resolution-test");
			emtpyHomeSystem = StorageSystem.fromJSON(json);
			emtpyHomeSystem.setOwner(BaseTestCase.SYSTEM_OWNER);
			emtpyHomeSystem.setAvailable(true);
			
			dao.persist(emtpyHomeSystem);
				
			LogicalFile testEntity = createLogicalFile(emtpyHomeSystem);
			
			try {
				String resolvedUrl = UUIDEntityLookup.resolveLogicalFileURLFromUUID(testEntity.getUuid());
				Assert.assertEquals(
						TenancyHelper.resolveURLToCurrentTenant(resolvedUrl, testEntity.getTenantId()),
						testEntity.getPublicLink(),
						"Relative path calculated by UUIDEntityLookup should match that calculated by LogicalFile");
			} catch (UUIDException e) {
				Assert.fail("Resolving logical file url from uuid should never throw exception", e);
			}
		}
		catch (JSONException | SystemArgumentException | IOException e) {
			Assert.fail("Failed to create test storage system with empty home directory", e);
		}
		finally {
			try { dao.remove(emtpyHomeSystem); } catch (Exception e){};
			
		}
	}
}
