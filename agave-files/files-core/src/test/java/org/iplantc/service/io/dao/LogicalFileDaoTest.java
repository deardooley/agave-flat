package org.iplantc.service.io.dao;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.hibernate.HibernateException;
import org.iplantc.service.io.BaseTestCase;
import org.iplantc.service.io.model.JSONTestDataUtil;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.enumerations.StagingTaskStatus;
import org.iplantc.service.io.model.enumerations.TransformTaskStatus;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups={"broken"})
public class LogicalFileDaoTest extends BaseTestCase 
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
	}
	
	@BeforeMethod
	protected void setUp() throws Exception 
	{	
		file = new LogicalFile(username, system, httpUri, destPath);
		file.setStatus(StagingTaskStatus.STAGING_QUEUED);
		file.setUuid(file.getPath());
		
		
		parent = new LogicalFile(username, system, httpUri, FilenameUtils.getFullPathNoEndSeparator(destPath));
		parent.setStatus(TransformTaskStatus.TRANSFORMING_COMPLETED);
		parent.setNativeFormat(LogicalFile.DIRECTORY);
		parent.setUuid(parent.getPath());
		
		sibling = new LogicalFile(username, system, httpUri, parent.getPath() + "/sibling.dat");
		sibling.setStatus(StagingTaskStatus.STAGING_QUEUED);
		sibling.setUuid(sibling.getPath());
		
		parentParent = new LogicalFile(username, system, httpUri, FilenameUtils.getFullPathNoEndSeparator(parent.getPath()));
		parentParent.setStatus(TransformTaskStatus.TRANSFORMING_COMPLETED);
		parentParent.setNativeFormat(LogicalFile.DIRECTORY);
		parentParent.setUuid(parentParent.getPath());
		
		uncle = new LogicalFile(username, system, httpUri, parentParent.getPath() + "/sibling");
		uncle.setStatus(StagingTaskStatus.STAGING_QUEUED);
		uncle.setNativeFormat(LogicalFile.DIRECTORY);
		uncle.setUuid(uncle.getPath());
		
		cousin = new LogicalFile(username, system, httpUri, uncle.getPath() + "/cousin.dat");
		cousin.setStatus(StagingTaskStatus.STAGING_QUEUED);
		cousin.setUuid(cousin.getPath());
		
		parentParentParent = new LogicalFile(username, system, httpUri, FilenameUtils.getFullPathNoEndSeparator(parentParent.getPath()));
		parentParentParent.setStatus(TransformTaskStatus.TRANSFORMING_COMPLETED);
		parentParentParent.setNativeFormat(LogicalFile.DIRECTORY);
		parentParentParent.setUuid(parentParentParent.getPath());
		
		rootParent = new LogicalFile(username, system, httpUri, "/");
		rootParent.setStatus(TransformTaskStatus.TRANSFORMING_COMPLETED);
		rootParent.setNativeFormat(LogicalFile.DIRECTORY);
		rootParent.setUuid(rootParent.getPath());
		
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

	@Test
	public void testPersistNull() {
		try {
			LogicalFileDao.save(null);
			AssertJUnit.fail("null file should throw an exception");
		} catch (HibernateException e) {
			// null file should throw an exception
		}
	}
	
	@Test
	public void testPersist() {
		try {
			LogicalFileDao.persist(file);
			Assert.assertNotNull(file.getId(), "Failed to persist logical file");
		} catch (HibernateException e) {
			Assert.fail("Persisting logical file should not thorw exception", e);
		}
	}

	@Test(dependsOnMethods={"testPersist"})
	public void testFindByIdInvalid() {
		try {
			LogicalFile f = LogicalFileDao.findById(-1);
			AssertJUnit.assertNull("Failed to retrieve file by id", f);
		} catch (HibernateException e) {
			Assert.fail("Retrieving file by invalid id should not throw an exception", e);
		}
	}
	
	@Test(dependsOnMethods={"testFindByIdInvalid"})
	public void testFindById() {
		try {
			LogicalFileDao.save(file);
			Assert.assertNotNull(file.getId(), "Failed to save the file");
			
			LogicalFile f = LogicalFileDao.findById(file.getId());
			Assert.assertNotNull(f, "Failed to retrieve file by id");
		} catch (HibernateException e) {
			Assert.fail("Retrieving file by id should not throw an exception", e);
		}
	}

	@Test(dependsOnMethods={"testFindById"})
	public void testFindBySystemPath() {
		try {
			LogicalFileDao.save(file);
			LogicalFile f = LogicalFileDao.findBySystemAndPath(file.getSystem(), file.getPath());
			AssertJUnit.assertNotNull("Failed to retrieve file by url", f);
		} catch (HibernateException e) {
			Assert.fail("Retrieving file by valid url should not throw an exception", e);
		}
	}
	
	@Test(dependsOnMethods={"testFindBySystemPath"})
	public void testFindBySystemAndNullPath() {
		try {
			LogicalFileDao.save(file);
			LogicalFileDao.findBySystemAndPath(file.getSystem(), null);
			Assert.fail("Null path should throw exception");
		} catch (HibernateException e) {
			Assert.assertTrue(true);
		}
	}
	
	@Test(dependsOnMethods={"testFindBySystemAndNullPath"})
	public void testFindByNullSystemAndPath() {
		try {
			LogicalFileDao.save(file);
			LogicalFileDao.findBySystemAndPath(null, file.getPath());
			Assert.fail("Null system should throw exception");
		} catch (HibernateException e) {
			Assert.assertTrue(true);
		}
	}
	
	@Test(dependsOnMethods={"testFindByNullSystemAndPath"})
	public void testFindByNullSystemAndNullPath() {
		try {
			LogicalFileDao.save(file);
			LogicalFileDao.findBySystemAndPath(null, null);
			Assert.fail("Null system and path should throw exception");
		} catch (HibernateException e) {
			Assert.assertTrue(true);
		}
	}
	
	@Test(dependsOnMethods={"testFindByNullSystemAndNullPath"})
	public void testFindParent() {
		try {
			LogicalFileDao.save(parent);
			LogicalFileDao.save(file);
			
			LogicalFile foundParent = LogicalFileDao.findParent(file);
			Assert.assertEquals(parent, foundParent, "Parent of file not found");
		} catch (HibernateException e) {
			Assert.fail("Looking for existing parent should not throw exception", e);
		}
	}
	
	@Test(dependsOnMethods={"testFindParent"})
	public void testFindParentMissing() {
		try {
			LogicalFileDao.save(file);
			
			LogicalFile foundParent = LogicalFileDao.findParent(file);
			Assert.assertNull(foundParent, "No parent should return null parent value");
		} catch (HibernateException e) {
			Assert.fail("Looking for existing parent should not throw exception", e);
		}
	}
	
	@Test(dependsOnMethods={"testFindParentMissing"})
	public void testFindParentReturnsFirstParent() {
		try {
			
			LogicalFileDao.save(rootParent);
			LogicalFileDao.save(parent);
			LogicalFileDao.save(file);
			
			LogicalFile foundParent = LogicalFileDao.findParent(file);
			Assert.assertEquals(parent, foundParent, "findParent should return first parent");
		} catch (HibernateException e) {
			Assert.fail("Looking for existing parent should not throw exception", e);
		}
	}
	
	@Test(dependsOnMethods={"testFindParentReturnsFirstParent"})
	public void testFindClosestParentNullSystem() {
		try {
			LogicalFileDao.findClosestParent(null, "/");
			Assert.fail("Null system and path should throw exception");
		} catch (HibernateException e) {
			Assert.assertTrue(true);
		}
	}
	
	@Test(dependsOnMethods={"testFindClosestParentNullSystem"})
	public void testFindClosestParentNullPath() {
		try {
			LogicalFileDao.findClosestParent(system, null);
			Assert.fail("Null path should throw exception");
		} catch (HibernateException e) {
			Assert.assertTrue(true);
		}
	}
	
	@Test(dependsOnMethods={"testFindClosestParentNullPath"})
	public void testFindClosestParentNullSystemNullPath() {
		try {
			LogicalFileDao.findClosestParent(null, null);
			Assert.fail("Null system and path should throw exception");
		} catch (HibernateException e) {
			Assert.assertTrue(true);
		}
	}
	
	@Test(dependsOnMethods={"testFindClosestParentNullSystemNullPath"})
	public void testFindClosestParentReturnsImmediateParent() {
		try {
			LogicalFileDao.save(parent);
			LogicalFileDao.save(file);
			
			LogicalFile foundParent = LogicalFileDao.findClosestParent(file.getSystem(), file.getPath());
			Assert.assertEquals(parent, foundParent, "findClosestParent "
					+ "should return immediate parent " + parent.getPath() + " when present");
		} catch (HibernateException e) {
			Assert.fail("Find closest parent should not throw exception", e);
		}
	}
	
	@Test(dependsOnMethods={"testFindClosestParentReturnsImmediateParent"})
	public void testFindClosestParentReturnsRootParentWhenNoIntermediate() {
		try {
			LogicalFileDao.save(rootParent);
			LogicalFileDao.save(file);
			
			LogicalFile foundParent = LogicalFileDao.findClosestParent(file.getSystem(), file.getPath());
			Assert.assertEquals(rootParent, foundParent, "findClosestParent "
					+ "should return root parent " + rootParent.getPath() + 
					" when no other parents are present");
		} catch (HibernateException e) {
			Assert.fail("Find closest parent should not throw exception", e);
		}
	}
	
	@Test(dependsOnMethods={"testFindClosestParentReturnsRootParentWhenNoIntermediate"})
	public void testFindClosestParentReturnsNullWhenNoIntermediateAndNoRootParent() {
		try {
			LogicalFileDao.save(file);
			
			LogicalFile foundParent = LogicalFileDao.findClosestParent(file.getSystem(), file.getPath());
			Assert.assertNull(foundParent, "findClosestParent "
					+ "should return null when no parent or root known.");
		} catch (HibernateException e) {
			Assert.fail("Find closest parent should not throw exception", e);
		}
	}
	
	@Test(dependsOnMethods={"testFindClosestParentReturnsNullWhenNoIntermediateAndNoRootParent"})
	public void testFindClosestParentReturnsImmediateParentWhenImmediateAndMultipleParentsExist() {
		try {
			LogicalFileDao.save(parentParent);
			LogicalFileDao.save(parent);
			LogicalFileDao.save(file);
			
			LogicalFile foundParent = LogicalFileDao.findClosestParent(file.getSystem(), file.getPath());
			Assert.assertEquals(foundParent, parent, "findClosestParent "
					+ "should return immediate parent when more than one parent is known.");
		} catch (HibernateException e) {
			Assert.fail("Find closest parent should not throw exception", e);
		}
	}
	
	@Test(dependsOnMethods={"testFindClosestParentReturnsImmediateParentWhenImmediateAndMultipleParentsExist"})
	public void testFindClosestParentReturnsFirstParentWhenMultipleParentsExist() {
		try {
			LogicalFileDao.save(parentParentParent);
			LogicalFileDao.save(parentParent);
			LogicalFileDao.save(file);
			
			LogicalFile foundParent = LogicalFileDao.findClosestParent(file.getSystem(), file.getPath());
			Assert.assertEquals(foundParent, parentParent, "findClosestParent "
					+ "should return first parent when more than one parent is known.");
		} catch (HibernateException e) {
			Assert.fail("Find closest parent should not throw exception", e);
		}
	}
	
	@Test(dependsOnMethods={"testFindClosestParentReturnsFirstParentWhenMultipleParentsExist"})
	public void testFindClosestParentReturnsFirstParentWhenMultipleParentsAndRootExist() {
		try {
			LogicalFileDao.save(rootParent);
			LogicalFileDao.save(parentParentParent);
			LogicalFileDao.save(file);
			
			LogicalFile foundParent = LogicalFileDao.findClosestParent(file.getSystem(), file.getPath());
			Assert.assertEquals(foundParent, parentParentParent, "findClosestParent "
					+ "should return first parent when more than one parent is known.");
		} catch (HibernateException e) {
			Assert.fail("Find closest parent should not throw exception", e);
		}
	}
	
	@Test(dependsOnMethods={"testFindClosestParentReturnsFirstParentWhenMultipleParentsAndRootExist"})
	public void testFindClosestParentReturnsNullWithoutParents() {
		try {
			LogicalFileDao.save(file);
			
			LogicalFile foundParent = LogicalFileDao.findClosestParent(file.getSystem(), file.getPath());
			Assert.assertNull(foundParent, "findClosestParent "
					+ "should return null when no parents are present");
		} catch (HibernateException e) {
			Assert.fail("Find closest parent should throw exception", e);
		}
	}
	
	@Test(dependsOnMethods={"testFindParentReturnsFirstParent"})
	public void testFindChildrenOfFolder()
	{
		try 
		{
			List<LogicalFile> srcFiles = initTestFiles(basePath);
			
			LogicalFile srcFile = null;
			for (int i=0; i<srcFiles.size(); i++) {
				if (!srcFiles.get(i).getPath().equals(basePath)) continue;
				srcFile = srcFiles.get(i);
				break;
			}
			
			List<LogicalFile> children = LogicalFileDao.findChildren(basePath, system.getId());
			Assert.assertFalse(children.contains(srcFile), "Parent folder should not be returned when looking for children");
			Assert.assertEquals(children.size(), srcFiles.size()-1, "Number of children returned for " + basePath + " is incorrect");
			
			for (LogicalFile file: srcFiles.subList(1, srcFiles.size() -1)) {
				Assert.assertTrue(children.contains(file), "Logical file for " + file.getPath() + " was not returned as a child of " + basePath);
			}
			
		} catch (HibernateException e) {
			Assert.fail("Finding children of " + basePath, e);
		}
	}
	
	@Test(dependsOnMethods={"testFindChildrenOfFolder"})
	public void testFindChildrenOfFile()
	{
		try 
		{
			List<LogicalFile> srcFiles = initTestFiles(basePath);
			
			LogicalFile srcFile = null;
			for (int i=0; i<srcFiles.size(); i++) {
				if (srcFiles.get(i).isDirectory()) continue;
				srcFile = srcFiles.get(i);
				break;
			}
			
			List<LogicalFile> children = LogicalFileDao.findChildren(srcFile.getPath(), system.getId());
			Assert.assertTrue(children.isEmpty(), "No children should be returned for a file " + srcFiles.get(1).getPath());
			
		} catch (HibernateException e) {
			Assert.fail("Finding children of " + basePath, e);
		}
	}
	
	@Test(dependsOnMethods={"testFindChildrenOfFile"})
	public void testFindChildrenOfEmptyFolder()
	{
		try 
		{
			List<LogicalFile> srcFiles = initTestFiles(basePath);
			
			String emptyfoldername = "emptyfolder" + System.currentTimeMillis();
			LogicalFile emptyFolder = new LogicalFile(username, system, null, basePath + "/" + emptyfoldername, emptyfoldername, "PROCESSING", LogicalFile.DIRECTORY);
			LogicalFileDao.persist(emptyFolder);
			
			List<LogicalFile> children = LogicalFileDao.findChildren(emptyFolder.getPath(), system.getId());
			Assert.assertTrue(children.isEmpty(),
					"No children should be returned for an empty folder " + srcFiles.get(srcFiles.size() - 1).getPath());
			
		} catch (HibernateException e) {
			Assert.fail("Finding children of " + basePath, e);
		}
	}
	
	
	
	@Test(dependsOnMethods={"testFindChildrenOfEmptyFolder"})
	public void testFindNonOverlappingChildrenEmptyOnIdenticalFolder()
	{
		try {
			List<LogicalFile> srcFiles = initTestFiles(basePath);
			List<LogicalFile> destFiles = initTestFiles(otherPath);
			
			LogicalFile srcFile = null;
			LogicalFile destFile = null;
			for (int i=0; i<srcFiles.size(); i++) {
				if (!srcFiles.get(i).isDirectory()) continue;
				srcFile = srcFiles.get(i);
				destFile = destFiles.get(i);
				break;
			}
			List<LogicalFile> overlappingChildren = 
					LogicalFileDao.findNonOverlappingChildren(srcFile.getPath(), 
															srcFile.getSystem().getId(), 
															destFile.getPath(), 
															destFile.getSystem().getId());
			
			Assert.assertTrue(overlappingChildren.isEmpty(),
					"No children should be returned when copying a folder with an identical tree");
			
//			Assert.assertEquals(overlappingChildren.size(), srcFiles.size(), "Copying an identical tree should return all children of " + basePath);
//			
//			for (LogicalFile file: srcFiles) {
//				Assert.assertTrue(overlappingChildren.contains(file), "Logical files for child " + file.getPath() + " was not returned as an overlapping child of " + basePath);
//			}
			
		} catch (HibernateException e) {
			Assert.fail("Finding non-overlapping children should not throw exception", e);
		}
	}
	
	@Test(dependsOnMethods={"testFindNonOverlappingChildrenEmptyOnIdenticalFolder"})
	public void testFindNonOverlappingChildrenEmptyOnIdenticalFfile()
	{
		try {
			List<LogicalFile> srcFiles = initTestFiles(basePath);
			List<LogicalFile> destFiles = initTestFiles(otherPath);
			
			LogicalFile srcFile = null;
			LogicalFile destFile = null;
			for (int i=0; i<srcFiles.size(); i++) {
				if (srcFiles.get(i).isDirectory()) continue;
				srcFile = srcFiles.get(i);
				destFile = destFiles.get(i);
				break;
			}
			List<LogicalFile> overlappingChildren = 
					LogicalFileDao.findNonOverlappingChildren(srcFile.getPath(), 
															srcFile.getSystem().getId(), 
															destFile.getPath(), 
															destFile.getSystem().getId());
			
			Assert.assertTrue(overlappingChildren.isEmpty(),
					"No children should be returned when copying a file");
			
		} catch (HibernateException e) {
			Assert.fail("Finding non-overlapping children should not throw exception", e);
		}
	}
	
	@Test(dependsOnMethods={"testFindNonOverlappingChildrenEmptyOnIdenticalFfile"})
	public void testFindNonOverlappingChildrenReturnsAllChildrenOnFullCopy()
	{
		try {
			List<LogicalFile> srcFiles = initTestFiles(basePath);
			List<LogicalFile> destFiles = initTestFiles(otherPath);
			
			List<LogicalFile> overlappingChildren = 
					LogicalFileDao.findNonOverlappingChildren(basePath, 
															system.getId(), 
															otherPath + "/bingo", 
															system.getId());
			
			for (int i=0; i<srcFiles.size(); i++) {
				if (srcFiles.get(i).getPath().equals(basePath)) {
					continue;
				}
 				Assert.assertTrue(overlappingChildren.contains(srcFiles.get(i)), "Logical files for child " + 
 						srcFiles.get(i).getPath() + " was not returned as a non overlapping child of " + basePath);
			}
			
		} catch (HibernateException e) {
			Assert.fail("Finding non-overlapping children should not throw exception", e);
		}
	}
	
	@Test(dependsOnMethods={"testFindNonOverlappingChildrenEmptyOnIdenticalFfile"})
	public void testFindNonOverlappingChildrenReturnsOnlyOverlappingChildrenOnFullCopy()
	{
		try {
			List<LogicalFile> srcFiles = initTestFiles(basePath);
			List<LogicalFile> destFiles = initTestFiles(otherPath);
			LogicalFile overlappingChild = new LogicalFile(username, 
					system, null, otherPath + "/folder/subfolder/foo.dat", "foo.dat", "PROCESSING", LogicalFile.RAW);
			LogicalFileDao.persist(overlappingChild);
			
			List<LogicalFile> overlappingChildren = 
					LogicalFileDao.findNonOverlappingChildren(basePath, 
															system.getId(), 
															otherPath + "/folder/subfolder", 
															system.getId());
			
			Assert.assertFalse(overlappingChildren.contains(overlappingChild), 
					"Overlapping file " + overlappingChild.getPath() + " should not be returned when copying from " + 
					basePath + " to " + otherPath + "/folder/subfolder");
			
			for (LogicalFile file: srcFiles) 
			{
				if (file.getName().equals(overlappingChild.getName())) continue;
				if (file.getPath().equals(basePath)) continue;
				
				Assert.assertTrue(overlappingChildren.contains(file), "Logical files for child " + 
						file.getPath() + " was not returned as a non overlapping child of " + basePath);
			}
			
		} catch (HibernateException e) {
			Assert.fail("Moving logical file tree should not throw exception", e);
		}
	}
	
	@Test(dependsOnMethods={"testFindNonOverlappingChildrenReturnsOnlyOverlappingChildrenOnFullCopy"})
	public void testFindParentReturnsClosestParent() {
		try {
			
			LogicalFileDao.save(rootParent);
			LogicalFileDao.save(file);
			
			LogicalFile foundParent = LogicalFileDao.findParent(file);
			Assert.assertNull(foundParent, "No parent should be returned if the direct parent folder is not known");
		} catch (HibernateException e) {
			Assert.fail("Looking for existing parent should not throw exception", e);
		}
	}
	
	@Test(dependsOnMethods={"testFindParentReturnsClosestParent"})
	public void testFindParentNullArgumentThrowsException() {
		try {
			LogicalFileDao.findParent(null);
			Assert.fail("Null argument to findParent should throw exception");
		} catch (HibernateException e) {
			
		}
	}

	@Test(dependsOnMethods={"testFindParentNullArgumentThrowsException"})
	public void testFindByOwnerNull() {
		try {
			LogicalFileDao.findByOwner(null);
			Assert.fail("null username should throw an exception");
		} catch (HibernateException e) {
			// null username should throw an exception
		}
	}
	
	@Test(dependsOnMethods={"testFindByOwnerNull"})
	public void testFindByOwnerEmpty() {
		try {
			LogicalFileDao.findByOwner("");
			Assert.fail("Empty username should throw an exception");
		} catch (HibernateException e) {
			// empty username should throw an exception
		}
	}
	
	@Test(dependsOnMethods={"testFindByOwnerEmpty"})
	public void testFindByOwner() {
		try {
			LogicalFileDao.save(file);
			List<LogicalFile> files = LogicalFileDao.findByOwner(file.getOwner());
			AssertJUnit.assertNotNull("Failed to retrieve inputs for " + file.getOwner(), files);
			AssertJUnit.assertFalse("No files retrieved for " + file.getOwner(), files.isEmpty());
		} catch (HibernateException e) {
			Assert.fail("Retrieving files for " + file.getOwner() + " should not throw an exception", e);
		}
	}

	@Test(dependsOnMethods={"testFindByOwner"})
	public void testUpdateTransferStatusLogicalFileNullString() {
		try {
			LogicalFileDao.updateTransferStatus((LogicalFile)null, StagingTaskStatus.STAGING_COMPLETED, SYSTEM_OWNER);
			Assert.fail("Empty file should throw an exception");
		} catch (HibernateException e) {
			// empty file should throw an exception
		}
	}
	
//	@Test(dependsOnMethods={"testPersist", "testFindById"})
//	public void testUpdateTransferStatusLogicalFileStringNull() {
//		try {
//			LogicalFileDao.updateTransferStatus(file, (String)null);
//			Assert.fail("null status should throw an exception");
//		} catch (HibernateException e) {
//			// null status should throw an exception
//		}
//	}
//	
//	@Test(dependsOnMethods={"testPersist", "testFindById"})
//	public void testUpdateTransferStatusLogicalFileStringEmpty() {
//		try {
//			LogicalFileDao.updateTransferStatus(file,"");
//			Assert.fail("Empty status should throw an exception");
//		} catch (HibernateException e) {
//			// empty status should throw an exception
//		}
//	}
	
	@Test(dependsOnMethods={"testUpdateTransferStatusLogicalFileNullString"})
	public void testUpdateTransferStatusLogicalFileString() {
		try {
			LogicalFileDao.updateTransferStatus(file, StagingTaskStatus.STAGING_COMPLETED, file.getOwner());
			LogicalFile f = LogicalFileDao.findById(file.getId());
			AssertJUnit.assertTrue("Status failed to updated", f.getStatus().equals(StagingTaskStatus.STAGING_COMPLETED.name()));
		} catch (HibernateException e) {
			Assert.fail("Failed to update status", e);
		}
	}

	@Test(dependsOnMethods={"testUpdateTransferStatusLogicalFileString"})
	public void testUpdateTransferStatusStringNullString() {
		try {
			LogicalFileDao.updateTransferStatus((LogicalFile)null, StagingTaskStatus.STAGING_FAILED, SYSTEM_OWNER);
			Assert.fail("null url should throw an exception");
		} catch (HibernateException e) {
			// null url should throw an exception
		}
	}
	
	@Test(dependsOnMethods={"testUpdateTransferStatusStringNullString"})
	public void testUpdateTransferStatusStringEmptyString() {
		try {
			LogicalFileDao.updateTransferStatus((RemoteSystem)null, "", StagingTaskStatus.STAGING_FAILED.name());
			Assert.fail("Empty url should throw an exception");
		} catch (HibernateException e) {
			// empty url should throw an exception
		}
	}
	
	@Test(dependsOnMethods={"testUpdateTransferStatusStringEmptyString"})
	public void testUpdateTransferStatusStringStringNull() {
		try {
			LogicalFileDao.updateTransferStatus(file.getSystem(), file.getPath(), null);
			Assert.fail("null status should throw an exception");
		} catch (HibernateException e) {
			// null status should throw an exception
		}
	}
	
	@Test(dependsOnMethods={"testUpdateTransferStatusStringStringNull"})
	public void testUpdateTransferStatusStringStringEmpty() {
		try {
			LogicalFileDao.updateTransferStatus(file.getSystem(), file.getPath(),"");
			Assert.fail("Empty status should throw an exception");
		} catch (HibernateException e) {
			// empty status should throw an exception
		}
	}

	@Test(dependsOnMethods={"testUpdateTransferStatusStringStringEmpty"})
	public void testUpdateTransferStatusStringString() {
		try {
			LogicalFileDao.save(file);
			LogicalFileDao.updateTransferStatus(file.getSystem(), file.getPath(),StagingTaskStatus.STAGING_FAILED.name());
			LogicalFile f = LogicalFileDao.findById(file.getId());
			AssertJUnit.assertTrue("Status failed to updated", f.getStatus().equals(StagingTaskStatus.STAGING_FAILED.name()));
		} catch (HibernateException e) {
			Assert.fail("Failed to update status", e);
		}
	}
	
	@Test(dependsOnMethods={"testUpdateTransferStatusStringString"})
	public void testRemoveNull() {
		try {
			LogicalFileDao.remove(null);
			Assert.fail("null file should throw an exception");
		} catch (HibernateException e) {
			// null file should throw an exception
		}
	}
	
	@Test(dependsOnMethods={"testRemoveNull"})
	public void testRemove() {
		try {
			LogicalFileDao.save(file);
			Assert.assertNotNull(file.getId(), "Failed to save logical file");
			Long id = file.getId();
			
			LogicalFileDao.remove(file);
			Assert.assertNull(LogicalFileDao.findById(id), "Failed to delete logical file");
		} catch (HibernateException e) {
			Assert.fail("File deletion should not throw an exception", e);
		}
	}
	
	@Test(dependsOnMethods={"testRemove"})
	public void testDeleteSubtreePathDoesNotDeleteFile()
	{
		try {
			LogicalFileDao.save(rootParent);
			LogicalFileDao.save(parentParentParent);
			LogicalFileDao.save(parentParent);
			LogicalFileDao.save(parent);
			LogicalFileDao.save(file);
			
			LogicalFileDao.deleteSubtreePath(file.getPath(), file.getSystem().getId());
			
			LogicalFile foundFile = LogicalFileDao.findById(file.getId());
			
			Assert.assertEquals(foundFile, file,"deleteSubtreePath "
					+ "should not delete the logical file at hte given path.");
		} catch (HibernateException e) {
			Assert.fail("Find closest parent should not throw exception", e);
		}
	}
	
	@Test(dependsOnMethods={"testRemove"})
	public void testDeleteSubtreePathDoesDeletesChildFiles()
	{
		try {
			LogicalFileDao.save(rootParent);
			LogicalFileDao.save(parentParentParent);
			LogicalFileDao.save(parentParent);
			LogicalFileDao.save(parent);
			LogicalFileDao.save(file);
			
			LogicalFileDao.deleteSubtreePath(parent.getPath(), parent.getSystem().getId());
			
			LogicalFile foundFile = LogicalFileDao.findById(file.getId());
			
			Assert.assertNull(foundFile,"deleteSubtreePath "
					+ "should delete files under the given path.");
		} catch (HibernateException e) {
			Assert.fail("Find closest parent should not throw exception", e);
		}
	}
	
	@Test(dependsOnMethods={"testRemove"})
	public void testDeleteSubtreePathDoesDeletesChildDirectories()
	{
		try {
			LogicalFileDao.save(rootParent);
			LogicalFileDao.save(parentParentParent);
			LogicalFileDao.save(parentParent);
			LogicalFileDao.save(parent);
			LogicalFileDao.save(uncle);
			LogicalFileDao.save(cousin);
			LogicalFileDao.save(sibling);
			LogicalFileDao.save(file);
			
			LogicalFileDao.deleteSubtreePath(parentParent.getPath(), parentParent.getSystem().getId());
			
			Assert.assertNull(LogicalFileDao.findById(sibling.getId()),
					"deleteSubtreePath should delete multiple directory hierarchies and files under the given path.");
			
			Assert.assertNull(LogicalFileDao.findById(cousin.getId()),
					"deleteSubtreePath should delete multiple directory hierarchies and files under the given path.");
			
			Assert.assertNull(LogicalFileDao.findById(uncle.getId()),
					"deleteSubtreePath should delete multiple directory hierarchies and files under the given path.");
			
			Assert.assertNull(LogicalFileDao.findById(parent.getId()),
					"deleteSubtreePath should delete multiple directory hierarchies and files under the given path.");
			
			Assert.assertNull(LogicalFileDao.findById(file.getId()),
					"deleteSubtreePath should delete multiple directory hierarchies and files under the given path.");

		} catch (HibernateException e) {
			Assert.fail("Find closest parent should not throw exception", e);
		}
	}
	
	@Test(dependsOnMethods={"testRemove"})
	public void testDeleteSubtreePathDoesNotDeletesSiblingDirectories()
	{
		try {
			LogicalFileDao.save(rootParent);
			LogicalFileDao.save(parentParentParent);
			LogicalFileDao.save(parentParent);
			LogicalFileDao.save(parent);
			LogicalFileDao.save(uncle);
			LogicalFileDao.save(cousin);
			LogicalFileDao.save(sibling);
			LogicalFileDao.save(file);
			
			LogicalFileDao.deleteSubtreePath(parent.getPath(), parent.getSystem().getId());
			
			Assert.assertNotNull(LogicalFileDao.findById(parent.getId()),
					"deleteSubtreePath should not delete the given path.");
			
			
			Assert.assertNull(LogicalFileDao.findById(sibling.getId()),
					"deleteSubtreePath should delete multiple directory hierarchies and files under the given path.");
			
			Assert.assertNull(LogicalFileDao.findById(file.getId()),
					"deleteSubtreePath should delete multiple directory hierarchies and files under the given path.");

			
			Assert.assertNotNull(LogicalFileDao.findById(uncle.getId()),
					"deleteSubtreePath should not delete sibling to the given path.");
			
			Assert.assertNotNull(LogicalFileDao.findById(cousin.getId()),
					"deleteSubtreePath should not delete children of sibling to the given path.");
			
			
		} catch (HibernateException e) {
			Assert.fail("Find closest parent should not throw exception", e);
		}
	}
	
	@Test(dependsOnMethods={"testRemove"})
	public void testDeleteSubtreePathDoesDeletesEntireTree()
	{
		try {
			LogicalFileDao.save(rootParent);
			LogicalFileDao.save(parentParentParent);
			LogicalFileDao.save(parentParent);
			LogicalFileDao.save(parent);
			LogicalFileDao.save(uncle);
			LogicalFileDao.save(sibling);
			LogicalFileDao.save(cousin);
			LogicalFileDao.save(file);
			
			LogicalFileDao.deleteSubtreePath(rootParent.getPath(), rootParent.getSystem().getId());
			
			Assert.assertEquals(LogicalFileDao.findById(rootParent.getId()), rootParent,
					"deleteSubtreePath should not delete the given path.");
			
			Assert.assertNull(LogicalFileDao.findById(parentParentParent.getId()),
					"deleteSubtreePath should delete multiple directory hierarchies under the given path.");
			
			Assert.assertNull(LogicalFileDao.findById(parentParent.getId()),
					"deleteSubtreePath should delete multiple directory hierarchies under the given path.");
			
			Assert.assertNull(LogicalFileDao.findById(parent.getId()),
					"deleteSubtreePath should delete multiple directory hierarchies under the given path.");
			
			Assert.assertNull(LogicalFileDao.findById(file.getId()),
					"deleteSubtreePath should delete multiple directory hierarchies and files under the given path.");
			
			Assert.assertNull(LogicalFileDao.findById(sibling.getId()),
					"deleteSubtreePath should delete multiple directory hierarchies and files under the given path.");
			
			Assert.assertNull(LogicalFileDao.findById(cousin.getId()),
					"deleteSubtreePath should delete multiple directory hierarchies and files under the given path.");
			
			Assert.assertNull(LogicalFileDao.findById(uncle.getId()),
					"deleteSubtreePath should delete multiple directory hierarchies and files under the given path.");
			
		} catch (HibernateException e) {
			Assert.fail("Find closest parent should not throw exception", e);
		}
	}
	
	@Test(dependsOnMethods={"testDeleteSubtreePathDoesDeletesEntireTree"})
	public void testFindParentsAbsoluteRootReturnsNoParents() 
	{
		try 
		{
			rootParent.setPath("/");
			LogicalFileDao.save(rootParent);
			LogicalFileDao.save(parentParentParent);
			LogicalFileDao.save(parentParent);
			LogicalFileDao.save(parent);
			LogicalFileDao.save(uncle);
			LogicalFileDao.save(sibling);
			LogicalFileDao.save(cousin);
			LogicalFileDao.save(file);
			
			
			List<LogicalFile> parents = LogicalFileDao.findParents(rootParent.getSystem(), rootParent.getPath());
			
			Assert.assertTrue(parents.isEmpty(), "No parents should be returned for /");
		} catch (HibernateException e) {
			Assert.fail("Looking for existing parent should not throw exception", e);
		}
	}
	
	@Test(dependsOnMethods={"testDeleteSubtreePathDoesDeletesEntireTree"})
	public void testFindParentsRootDirectoryReturnsNoParents() 
	{
		try 
		{
			LogicalFileDao.save(rootParent);
			LogicalFileDao.save(parentParentParent);
			LogicalFileDao.save(parentParent);
			LogicalFileDao.save(parent);
			LogicalFileDao.save(uncle);
			LogicalFileDao.save(sibling);
			LogicalFileDao.save(cousin);
			LogicalFileDao.save(file);
			
			List<LogicalFile> parents = LogicalFileDao.findParents(rootParent.getSystem(), rootParent.getPath());
			
			Assert.assertTrue(parents.isEmpty(), "No parents should be returned for root parent");
		} catch (HibernateException e) {
			Assert.fail("Looking for existing parent should not throw exception", e);
		}
	}
	
	@Test(dependsOnMethods={"testDeleteSubtreePathDoesDeletesEntireTree"})
	public void testFindParents() {
		try {
			LogicalFileDao.save(rootParent);
			LogicalFileDao.save(parentParentParent);
			LogicalFileDao.save(parentParent);
			LogicalFileDao.save(parent);
			LogicalFileDao.save(uncle);
			LogicalFileDao.save(sibling);
			LogicalFileDao.save(cousin);
			LogicalFileDao.save(file);
			
			List<LogicalFile> parents = LogicalFileDao.findParents(file.getSystem(), file.getPath());
			
			Assert.assertFalse(parents.contains(file), "findParents should not be included the given path");
			
			Assert.assertTrue(parents.contains(parent), "findParents should included parent of the given path");
			Assert.assertTrue(parents.contains(parentParent), "findParents should included all parents of the given path");
			Assert.assertTrue(parents.contains(parentParentParent), "findParents should included all parents of the given path");
			Assert.assertTrue(parents.contains(rootParent), "findParents should included the root parent of the given path");
			
			Assert.assertFalse(parents.contains(sibling), "findParents should not included sibling");
			Assert.assertFalse(parents.contains(cousin), "findParents should not included children of parent siblings");
			Assert.assertFalse(parents.contains(uncle), "findParents should not included siblings of parents");
			
			Assert.assertTrue(parents.contains(parent), "findParents should included parent of the given path");
			
		} catch (HibernateException e) {
			Assert.fail("Looking for existing parent should not throw exception", e);
		}
	}
	
	@Test(dependsOnMethods={"testDeleteSubtreePathDoesDeletesEntireTree"})
	public void testFindParentsNoSiblingResults() 
	{
		try 
		{
			LogicalFileDao.save(rootParent);
			LogicalFileDao.save(parentParentParent);
			LogicalFileDao.save(parentParent);
			LogicalFileDao.save(parent);
			LogicalFileDao.save(uncle);
			LogicalFileDao.save(sibling);
			LogicalFileDao.save(cousin);
			LogicalFileDao.save(file);
			
			List<LogicalFile> parents = LogicalFileDao.findParents(cousin.getSystem(), cousin.getPath());
			
			Assert.assertFalse(parents.contains(cousin), "findParents should not be included the given path");
			
			Assert.assertTrue(parents.contains(uncle), "findParents should included parent of the given path");
			Assert.assertTrue(parents.contains(parentParent), "findParents should included all parents of the given path");
			Assert.assertTrue(parents.contains(parentParentParent), "findParents should included all parents of the given path");
			Assert.assertTrue(parents.contains(rootParent), "findParents should included the root parent of the given path");
			
			Assert.assertFalse(parents.contains(file), "findParents should not included children of parent siblings");
			Assert.assertFalse(parents.contains(parent), "findParents should not included parent siblings of the given path");
			
		} catch (HibernateException e) {
			Assert.fail("Looking for existing parent should not throw exception", e);
		}
	}
	
	@Test(dependsOnMethods={"testDeleteSubtreePathDoesDeletesEntireTree"})
	public void testFindParentsEmptyResultWithNoParents() 
	{
		try 
		{
			LogicalFileDao.save(file);
			
			List<LogicalFile> parents = LogicalFileDao.findParents(file.getSystem(), file.getPath());
			
			Assert.assertTrue(parents.isEmpty(), "findParents should not return results when there are no parents");
			
		} catch (HibernateException e) {
			Assert.fail("Looking for existing parent should not throw exception", e);
		}
	}
	
	@Test(dependsOnMethods={"testDeleteSubtreePathDoesDeletesEntireTree"})
	public void testFindParentsEmptyResultWithNoEntires() 
	{
		try 
		{
			List<LogicalFile> parents = LogicalFileDao.findParents(file.getSystem(), file.getPath());
			
			Assert.assertTrue(parents.isEmpty(), "findParents should not return results when there are no no entries");
			
		} catch (HibernateException e) {
			Assert.fail("Looking for existing parent should not throw exception", e);
		}
	}
	
	private List<LogicalFile> initTestFiles(String path) {
		
		List<LogicalFile> srcFiles = new ArrayList<LogicalFile>();
		srcFiles.add(new LogicalFile(username, system, null, path, "folder", "PROCESSING", LogicalFile.DIRECTORY));
		srcFiles.add(new LogicalFile(username, system, null, path + "/folder", "folder", "PROCESSING", LogicalFile.DIRECTORY));
		srcFiles.add(new LogicalFile(username, system, null, path + "/folder/foo.dat", "foo.dat", "PROCESSING", LogicalFile.RAW));
		srcFiles.add(new LogicalFile(username, system, null, path + "/folder/bar.dat", "bar.dat", "PROCESSING", LogicalFile.RAW));
		srcFiles.add(new LogicalFile(username, system, null, path + "/folder/subfolder", "subfolder", "PROCESSING", LogicalFile.DIRECTORY));
		srcFiles.add(new LogicalFile(username, system, null, path + "/folder/subfolder/alpha.txt", "alpha.txt", "PROCESSING", LogicalFile.RAW));
		srcFiles.add(new LogicalFile(username, system, null, path + "/file.dat", "file.dat", "PROCESSING", LogicalFile.RAW));
		srcFiles.add(new LogicalFile(username, system, null, path + "/emptyfolder", "emptyfolder", "PROCESSING", LogicalFile.DIRECTORY));
		
		for (LogicalFile file: srcFiles) {
			file.setStatus(StagingTaskStatus.STAGING_QUEUED);
			file.setUuid(file.getPath());
			LogicalFileDao.save(file);
		}
		
		return srcFiles;
	}
}
