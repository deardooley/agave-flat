package org.iplantc.service.io.dao;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.iplantc.service.io.BaseTestCase;
import org.iplantc.service.io.model.JSONTestDataUtil;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.StorageSystem;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups={"integration"})
public class LogicalFileDaoCaseSensitivityTest extends BaseTestCase 
{
	private LogicalFile file;
	private LogicalFile sibling;
	private LogicalFile cousin;
	private LogicalFile parent;
	private LogicalFile uncle;
	private LogicalFile parentParent;
	private LogicalFile parentParentParent;
	private LogicalFile rootParent;
	private SystemDao systemDao = new SystemDao();
	private StorageSystem system;
	private String basePath;
	private String otherPath;
	
	@BeforeClass
	protected void beforeClass() throws Exception 
	{
		super.beforeClass();
		clearSystems();
		clearLogicalFiles();
		
		system = StorageSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_STORAGE_SYSTEM_FILE));
		system.setOwner(SYSTEM_OWNER);
		system.setPubliclyAvailable(true);
		system.setGlobalDefault(true);
		system.setAvailable(true);
		
		systemDao.persist(system);
		
		basePath = "/var/home/" + username + "/some";
		otherPath = "/var/home/" + username + "/other";
		
		file = new LogicalFile(username, system, httpUri.toString().toLowerCase(), destPath.toLowerCase(), "originalFilename");
	}
	
	@BeforeMethod
	protected void setUp() throws Exception 
	{	
		file = new LogicalFile(username, system, httpUri.toString().toLowerCase(), destPath.toLowerCase(), "originalFilename");
		
		//LogicalFileDao.persist(file);
	}
	
	@AfterMethod
	protected void tearDown() throws Exception
	{
		clearLogicalFiles();
	}
	
	@AfterClass
	protected void afterClass() throws Exception 
	{
		clearSystems();
		clearLogicalFiles();
	}

	@DataProvider
	protected Object[][] caseInsensitiveProvider() {
		return new Object[][] {
				{ file.getPath(), StringUtils.upperCase(file.getPath()) },
				{ file.getPath(), StringUtils.swapCase(file.getPath()) },
				{ StringUtils.upperCase(file.getPath()), file.getPath() },
				{ StringUtils.swapCase(file.getPath()), file.getPath() },
		};
	}

	@Test(dataProvider="caseInsensitiveProvider")
	public void testFindBySystemCaseInsensitivePath(String originalPath, String caseInsensitivePath) {
		try {
			file.setPath(originalPath);
			LogicalFileDao.save(file);
			
			LogicalFile caseInsensitiveFile = new LogicalFile(username, system, file.getSourceUri(), caseInsensitivePath, "originalFilename");
			LogicalFileDao.save(caseInsensitiveFile);
			
			LogicalFile f = LogicalFileDao.findBySystemAndPath(file.getSystem(), originalPath);
			Assert.assertNotNull(f, "Failed to retrieve file by system and path");
			Assert.assertEquals(f.getPath(), originalPath, "case senstivity should be honored in logical file lookups by system and path");
			
			LogicalFile f2 = LogicalFileDao.findBySystemAndPath(caseInsensitiveFile.getSystem(), caseInsensitivePath);
			Assert.assertNotNull(f2, "Failed to retrieve file by by system and path");
			Assert.assertEquals(f2.getPath(), caseInsensitivePath, "case senstivity should be honored in logical file lookups by system and path");
			
			Assert.assertNotEquals(f.getUuid(), f2.getUuid(), "files with the system and same path, but mismatched case should not result in the same rsponse from a query");
		} catch (HibernateException e) {
			Assert.fail("Retrieving file by valid source and path should not throw an exception", e);
		}
	}
	
	@DataProvider
	protected Object[][] caseInsensitiveSourceUriProvider() {
		return new Object[][] {
				{ file.getSourceUri(), StringUtils.upperCase(file.getSourceUri()) },
				{ file.getSourceUri(), StringUtils.swapCase(file.getSourceUri()) },
		};
	}

	@Test(dataProvider="caseInsensitiveSourceUriProvider", dependsOnMethods={"testFindBySystemCaseInsensitivePath"}, groups={"broken"})
	public void testFindByCaseInsensitiveSourceUri(String originalUri, String caseInsensitiveUri) {
		try {
			file.setSourceUri(originalUri);
			LogicalFileDao.save(file);
			
			LogicalFile caseInsensitiveFile = new LogicalFile(username, system, caseInsensitiveUri, file.getPath(), "originalFilename");
			LogicalFileDao.save(caseInsensitiveFile);
			
			LogicalFile f = LogicalFileDao.findBySourceUrl(originalUri);
			Assert.assertNotNull(f, "Failed to retrieve file by sourceUrl");
			Assert.assertEquals(f.getSourceUri(), originalUri, "case senstivity should be honored in logical file lookups by sourceUrl");
			
			LogicalFile f2 = LogicalFileDao.findBySourceUrl(caseInsensitiveUri);
			Assert.assertNotNull(f2, "Failed to retrieve file by getSourceUri");
			Assert.assertEquals(f2.getSourceUri(), caseInsensitiveUri, "case senstivity should be honored in logical file lookups by getSourceUri");
			
			Assert.assertNotEquals(f.getUuid(), f2.getUuid(), "files with the same getSourceUri, but mismatched case should not result in the same rsponse from a query");
		} catch (HibernateException e) {
			Assert.fail("Retrieving file by valid getSourceUri should not throw an exception", e);
		}
	}
	
	@DataProvider
	protected Object[][] caseInsensitiveOwnerProvider() {
		return new Object[][] {
				{ file.getOwner(), StringUtils.upperCase(file.getOwner()) },
				{ file.getOwner(), StringUtils.swapCase(file.getOwner()) },
		};
	}

	@Test(dataProvider="caseInsensitiveOwnerProvider", dependsOnMethods={"testFindByCaseInsensitiveSourceUri"}, groups={"broken"})
	public void testFindByCaseInsensitiveOwner(String originalOwner, String caseInsensitiveOwner) {
		try {
			file.setOwner(originalOwner);
			LogicalFileDao.save(file);
			
			LogicalFile caseInsensitiveFile = new LogicalFile(caseInsensitiveOwner, system, file.getSourceUri(), file.getPath(), "originalFilename");
			LogicalFileDao.save(caseInsensitiveFile);
			
			List<LogicalFile> f = LogicalFileDao.findByOwner(originalOwner);
			Assert.assertNotNull(f, "Failed to retrieve files by owner");
			Assert.assertEquals(f.size(), 1, "Failed to retrieve files by owner");
			Assert.assertEquals(f.get(0).getOwner(), originalOwner, "case senstivity should be honored in logical file lookups by owner");
			
			List<LogicalFile> f2 = LogicalFileDao.findByOwner(caseInsensitiveOwner);
			Assert.assertNotNull(f2, "Failed to retrieve file by caseInsensitiveOwner");
			Assert.assertEquals(f2.size(), 1, "Failed to retrieve files by caseInsensitiveOwner");
			Assert.assertEquals(f2.get(0).getOwner(), caseInsensitiveOwner, "case senstivity should be honored in logical file lookups by caseInsensitiveOwner");
			
			Assert.assertNotEquals(f.get(0).getUuid(), f2.get(0).getUuid(), "files with the same owner, but mismatched case should not result in the same rsponse from a query");
		} catch (HibernateException e) {
			Assert.fail("Retrieving file by valid owner should not throw an exception", e);
		}
	}
	
	
}
