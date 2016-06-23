package org.iplantc.service.io.manager;

import java.util.List;

import org.hibernate.HibernateException;
//import org.iplantc.service.io.BaseTestCase;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.model.LogicalFile;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LogicalFileManagerTest //extends BaseTestCase
{
//	@Test(dependsOnMethods={"testFindParent"})
//	public void testMoveOvewritesExistingLogicalFileOnOverwrite()
//	{
//		try {
//			List<LogicalFile> srcFiles = initTestFiles(basePath);
//			List<LogicalFile> destFiles = initTestFiles(otherPath);
//			
//			LogicalFile srcFile = null;
//			LogicalFile destFile = null;
//			for (int i=0; i<srcFiles.size(); i++) {
//				if (srcFiles.get(i).isDirectory()) continue;
//				srcFile = srcFiles.get(i);
//				destFile = destFiles.get(i);
//				break;
//			}
//			LogicalFileDao.findNonOverlappingChildren(srcFile.getPath(), srcFile.getSystem().getId(), destFile.getPath(), destFile.getSystem().getId());
//			
//			
//			for (LogicalFile file: srcFiles) {
//				if (!file.getPath().equals(basePath + "/folder/foo.dat")) {
//					LogicalFile sourceFile = LogicalFileDao.findBySystemAndPath(system, file.getPath());
//					Assert.assertNotNull(sourceFile, "Logical files for " + file.getPath() + " should not have been deleted in the movement of " + basePath + "/folder/foo.dat");
//				} else {
//					LogicalFile deletedSourceFile = LogicalFileDao.findBySystemAndPath(system, file.getPath());
//					Assert.assertNull(deletedSourceFile, "Old logical file for " + file.getPath() + " should be deleted after overwriting an existing logicalFile");
//				}
//			}
//			
//			for (LogicalFile file: destFiles) {
//				LogicalFile destFile = LogicalFileDao.findBySystemAndPath(system, file.getPath());
//				Assert.assertEquals(file, destFile, "Destination file for " + file.getPath() + " should be updated after moving another logicalFile over it");
//			}
//			
//		} catch (HibernateException e) {
//			Assert.fail("Moving logical file tree should not throw exception", e);
//		}
//	}
//	
//	@Test(dependsOnMethods={"testFindParent"})
//	public void testMoveOvewritesExistingLogicalDirectoryOnOverwrite()
//	{
//		try {
//			List<LogicalFile> srcFiles = initTestFiles(basePath);
//			List<LogicalFile> destFiles = initTestFiles(otherPath);
//			
//			LogicalFileDao.moveSubtreePath(basePath + "/folder/subfolder", system.getId(), otherPath + "/folder/subfolder", system.getId());
//			
//			for (LogicalFile file: srcFiles) {
//				if (file.getPath().startsWith(basePath + "/folder/subfolder")) {
//					LogicalFile deletedSourceFile = LogicalFileDao.findBySystemAndPath(system, file.getPath());
//					Assert.assertNull(deletedSourceFile, "Old logical file for " + file.getPath() + " should be deleted after overwriting an existing logicalFile");					
//				} else {
//					LogicalFile sourceFile = LogicalFileDao.findBySystemAndPath(system, file.getPath());
//					Assert.assertNull(sourceFile, "Logical files for " + file.getPath() + " should not have been deleted in the movement of " + basePath + "/folder/foo.dat");
//				}
//			}
//			
//			for (LogicalFile file: destFiles) {
//				LogicalFile destFile = LogicalFileDao.findBySystemAndPath(system, file.getPath());
//				Assert.assertEquals(file, destFile, "Destination file for " + file.getPath() + " should be updated after moving another logicalFile over it");
//			}
//			
//		} catch (HibernateException e) {
//			Assert.fail("Moving logical file tree should not throw exception", e);
//		}
//	}
//	
//	@Test(dependsOnMethods={"testFindParent"})
//	public void testMoveSubtreePathSubfolderOverwrite()
//	{
//		try {
//			List<LogicalFile> srcFiles = initTestFiles(basePath);
//			List<LogicalFile> destFiles = initTestFiles(otherPath);
//			
//			LogicalFileDao.moveSubtreePath(basePath + "/folder/subfolder", system.getId(), otherPath + "/folder/subfolder", system.getId());
//			
//			for (LogicalFile file: srcFiles) {
//				if (file.getPath().startsWith(basePath + "/folder/subfolder")) {
//					LogicalFile deletedSourceFile = LogicalFileDao.findBySystemAndPath(system, file.getPath());
//					Assert.assertNull(deletedSourceFile, "Old logical file for " + file.getPath() + " should be deleted after overwriting an existing logicalFile");					
//				} else {
//					LogicalFile sourceFile = LogicalFileDao.findBySystemAndPath(system, file.getPath());
//					Assert.assertNull(sourceFile, "Logical files for " + file.getPath() + " should not have been deleted in the movement of " + basePath + "/folder/foo.dat");
//				}
//			}
//			
//			for (LogicalFile file: destFiles) {
//				LogicalFile destFile = LogicalFileDao.findBySystemAndPath(system, file.getPath());
//				Assert.assertEquals(file, destFile, "Destination file for " + file.getPath() + " should be updated after moving another logicalFile over it");
//			}
//			
//		} catch (HibernateException e) {
//			Assert.fail("Moving logical file tree should not throw exception", e);
//		}
//	}
}
