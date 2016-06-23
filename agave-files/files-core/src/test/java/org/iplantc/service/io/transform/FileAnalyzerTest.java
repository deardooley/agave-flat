package org.iplantc.service.io.transform;

import java.io.File;

import org.iplantc.service.data.transform.FileAnalyzer;
import org.iplantc.service.io.BaseTestCase;
import org.iplantc.service.io.Settings;
import org.iplantc.service.io.exceptions.RemoteCopyException;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.enumerations.StagingTaskStatus;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.enumerations.StorageProtocolType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * This test doesn't do anything useful at the moment.
 * 
 * @author dooley
 *
 */
public class FileAnalyzerTest extends BaseTestCase 
{
	private LogicalFile logicalFile;
	private RemoteSystem defaultStorageSystem;
	private RemoteDataClient remoteClient; 
	
	@BeforeMethod
	protected void setUp() throws Exception {
		Settings.TRANSFORMS_FOLDER_PATH = testFileAnalysisDirectory.getAbsolutePath();
		
		defaultStorageSystem = new SystemManager().getUserDefaultStorageSystem(username);
		remoteClient = defaultStorageSystem.getRemoteDataClient();
		remoteClient.authenticate();
	}
	
	@AfterMethod
	protected void tearDown() throws Exception {
		try {
			// delete the remote file after each test so we don't get a false positive 
			remoteClient.delete("/" + username + "/test");
		} catch (Exception e) {}
	}

	//@Test
	public void testFindTransform() throws RemoteCopyException {
		
		for (File testFile: testFileAnalysisDirectory.listFiles()) {
			try {
				
				stageInputFile(testFile);
				
				FileAnalyzer analyzer = new FileAnalyzer(remoteClient, logicalFile.getAgaveRelativePathFromAbsolutePath());
				AssertJUnit.assertNotNull("File analyzer found no matching transform", analyzer.findTransform());
				
				// delete the remote file after each test so we don't get a false positive 
				if (logicalFile.getSystem().getStorageConfig().getProtocol().equals(StorageProtocolType.IRODS)) {
					remoteClient.delete(logicalFile.getAgaveRelativePathFromAbsolutePath());
				}
			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail("Analyzing transform for " + testFile.getAbsolutePath() 
						+ " should not throw an exception");
			}
		}
	}
	
	private void stageInputFile(File localFile) throws Exception 
	{
		String irodsPath = username + "/test/" + localFile.getName(); 
		
		try {
			// copy test file to remote machine
			remoteClient.put(localFile.getAbsolutePath(), irodsPath);
			AssertJUnit.assertTrue("File not staged into iRODS",remoteClient.doesExist(irodsPath));
			
			logicalFile = new LogicalFile(username, defaultStorageSystem, localFile.toURI(), destPath);
			logicalFile.setPath(remoteClient.resolvePath(irodsPath));
			logicalFile.setStatus(StagingTaskStatus.STAGING_COMPLETED);
			
		} catch (Exception e) {
			throw new RemoteCopyException("Failed to stage " 
					+ localFile.getAbsolutePath() + " to IRODS for the test");
		}
	} 

}
