package org.iplantc.service.io.queue;

import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.iplantc.service.io.BaseTestCase;
import org.iplantc.service.io.Settings;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.dao.QueueTaskDao;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.StagingTask;
import org.iplantc.service.io.model.enumerations.StagingTaskStatus;
import org.iplantc.service.io.model.enumerations.TransformTaskStatus;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.SystemRole;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.systems.model.enumerations.RoleType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(singleThreaded=true)
public class StagingJobTest extends BaseTestCase {
	
	private StagingJob stagingJob;
	private LogicalFile file;
	private StagingTask task;
	private RemoteDataClient remoteClient;
	private StorageSystem defaultStorageSystem;
	private StorageSystem sftpSystem;
	private StorageSystem gridftpSystem;
	private StorageSystem irodsSystem;
	private StorageSystem ftpSystem;
	private StorageSystem s3System;
	
	private SystemDao systemDao = new SystemDao();
	
	@BeforeClass
	@Override
	protected void beforeClass() throws Exception {
		super.beforeClass();
		defaultStorageSystem = initSystem("sftp", true, true);
		sftpSystem = initSystem("sftp", false, false);
		gridftpSystem = initSystem("gridftp", false, false);
		ftpSystem = initSystem("ftp", false, false);
		s3System = initSystem("s3", false, false);
		irodsSystem = initSystem("irods", false, false);
	}
	
	private StorageSystem initSystem(String protocol, boolean setDefault, boolean setPublic) 
	throws Exception 
	{
	    String systemId = String.format("%s%s%s", protocol, setPublic ? "-public" : "", setDefault ? "-default": "" );
		StorageSystem system = (StorageSystem) getNewInstanceOfRemoteSystem(RemoteSystemType.STORAGE,  StringUtils.lowerCase(protocol), systemId);
		system.setPubliclyAvailable(setPublic);
		system.setGlobalDefault(setDefault);
		system.setOwner(SYSTEM_OWNER);
		String testHomeDir = (StringUtils.isEmpty(system.getStorageConfig().getHomeDir()) ? "" : system.getStorageConfig().getHomeDir());
		system.getStorageConfig().setHomeDir( testHomeDir + "/" + this.getClass().getSimpleName() + "-staging");
		System.out.println("Saving test system " + system.toString());
		systemDao.persist(system);
		system.addRole(new SystemRole(SYSTEM_OWNER, RoleType.ADMIN));
		system.addRole(new SystemRole(SYSTEM_SHARE_USER, RoleType.ADMIN));
		system.addRole(new SystemRole(SYSTEM_UNSHARED_USER, RoleType.ADMIN));
//		system.addRole(new SystemRole(system.getStorageConfig().getDefaultAuthConfig().getUsername(), RoleType.ADMIN));
		system.addRole(new SystemRole(Settings.COMMUNITY_USERNAME, RoleType.ADMIN));
		systemDao.persist(system);
		return system;
	}
	
	@BeforeMethod
	protected void setUp() throws Exception 
	{
		stagingJob = new StagingJob();
	}
	
	@AfterMethod
	protected void tearDown() throws Exception {
		
		try { 
			// delete the remote file after each test so we don't get a false positive 
			remoteClient.delete(file.getAgaveRelativePathFromAbsolutePath());
			remoteClient.disconnect();
		} catch (Exception e) {}
		finally {
			try { clearLogicalFiles(); } catch (Exception e) {}
		}
	}
	
	@AfterMethod
	protected void afterMethod() 
	{
	    try { clearLogicalFiles(); } catch (Exception e) {}	
	}
	
	private StagingTask createStagingTaskForUrl(URI sourceUri, RemoteSystem destSystem, String destPath) 
	throws FileNotFoundException, Exception 
	{
		file = new LogicalFile(username, destSystem, sourceUri, destSystem.getRemoteDataClient().resolvePath(destPath));
		file.setStatus(StagingTaskStatus.STAGING_QUEUED);
		file.setOwner(SYSTEM_OWNER);
		
		LogicalFileDao.persist(file);
		
		task = new StagingTask(file, SYSTEM_OWNER);
		task.setRetryCount(Settings.MAX_STAGING_RETRIES);
		QueueTaskDao.persist(task);
		
		return task;
	} 
	
	@Test
	public void testStageNextFileQueueEmpty() 
	{
		// no task is in the queue
		try {
			stagingJob.execute(null);
		} catch (Exception e) {
			Assert.fail("No queued files should return without exception", e);
		}
	}
	
	@DataProvider
	private Object[][] testStageNextURIProvider(Method m) throws Exception
	{
		
		URI httpUri = new URI("http://agaveapi.co/wp-content/themes/agave/images/favicon.ico");
		URI httpsUri = new URI("https://avatars0.githubusercontent.com/u/785202");
		URI httpPortUri = new URI("http://docker.example.com:10080/public/test_upload.bin");
		URI httpsPortUri = new URI("https://docker.example.com:10443/public/test_upload.bin");
		URI httpsQueryUri = new URI("https://docker.example.com:10443/public/test_upload.bin?t=now");
		URI httpBasicUri = new URI("http://testuser:testuser@docker.example.com:10080/private/test_upload.bin");
		
		URI httpNoPathUri = new URI("http://docker.example.com:10080");
		URI httpEmptyPathUri = new URI("http://docker.example.com:10080/");
		URI fileNotFoundUri = new URI("http://docker.example.com:10080/" + MISSING_FILE);
		URI httpMissingPasswordBasicUri = new URI("http://testuser@docker.example.com:10080/private/test_upload.bin");
		URI httpInvalidPasswordBasicUri = new URI("http://testuser:testotheruser@docker.example.com:10080/private/test_upload.bin");
		
		List<Object[]> testCases = new ArrayList<Object[]>();
		
		for (URI uri: Arrays.asList(httpUri, httpsUri, httpPortUri, httpsPortUri, httpsQueryUri, httpBasicUri)) 
		{
		    for (StorageSystem system: Arrays.asList(defaultStorageSystem, gridftpSystem, sftpSystem, ftpSystem, s3System, irodsSystem)) {
		        testCases.add(new Object[]{ uri, 
		                                    system, 
		                                    "", 
		                                    true, 
		                                    FilenameUtils.getName(uri.getPath()), 
                        		            String.format("Retrieving valid uri from %s to %s %s %s system should succeed",
                        		                    uri.toString(),
                        		                    system.isGlobalDefault() ? " default " : "",
                                                    system.getStorageConfig().getProtocol().name(),
                                                    system.getType().name()) });
		        
		        testCases.add(new Object[]{ fileNotFoundUri, 
		                                    system, 
		                                    "", 
		                                    false, 
		                                    FilenameUtils.getName(fileNotFoundUri.getPath()), 
		                                    String.format("Staging 404 at %s to default sotrage system should fail.",
		                                            fileNotFoundUri.toString())});
		        
		        testCases.add(new Object[]{ httpMissingPasswordBasicUri, 
		                                    defaultStorageSystem, 
		                                    "", 
		                                    false, 
                    		                FilenameUtils.getName(httpMissingPasswordBasicUri.getPath()), 
                    		                String.format("Missing basic password copying file at %s to default sotrage system should fail.",
                    		                        httpMissingPasswordBasicUri.toString())});
		        
		        testCases.add(new Object[]{ httpInvalidPasswordBasicUri, 
		                                    defaultStorageSystem, 
		                                    "", 
		                                    false, 
                    		                FilenameUtils.getName(httpInvalidPasswordBasicUri.getPath()), 
                    		                String.format("Bad basic password copying file at %s to default sotrage system should fail.",
                    		                httpInvalidPasswordBasicUri.toString())});
		        
		        testCases.add(new Object[]{ httpNoPathUri, 
		                                    defaultStorageSystem, 
		                                    "", 
		                                    false, 
		                                    FilenameUtils.getName(httpNoPathUri.getPath()), 
	                                        String.format("No source path found copying file at %s to default sotrage system should fail.",
	                                                httpNoPathUri.toString())});
		        testCases.add(new Object[]{ httpEmptyPathUri, 
		                                    defaultStorageSystem, 
		                                    "", 
		                                    false, 
		                                    FilenameUtils.getName(httpEmptyPathUri.getPath()), 
		                                    String.format("No source path found copying file at %s to default sotrage system should fail.",
		                                            httpEmptyPathUri.toString())});
		        
	        }
		        
//	        response[i++] = new Object[]{ uri, defaultStorageSystem, "", true, FilenameUtils.getName(uri.getPath()), "Retrieving valid uri from " + uri.toString() + " to default sotrage system should succeed." };
//			response[i++] = new Object[]{ uri, gridftpSystem, "", true, FilenameUtils.getName(uri.getPath()), "Retrieving valid uri from " + uri.toString() + " to gridftp should succeed." };
//			response[i++] = new Object[]{ uri, sftpSystem, "", true, FilenameUtils.getName(uri.getPath()), "Retrieving valid uri from " + uri.toString() + " to sftp should succeed." };
//			response[i++] = new Object[]{ uri, irodsSystem, "", true, FilenameUtils.getName(uri.getPath()), "Staging valid uri from " + uri.toString() + " to irods should succeed." };
		}
		
//		response[i++] = new Object[]{ fileNotFoundUri, defaultStorageSystem, "", false, FilenameUtils.getName(fileNotFoundUri.getPath()), "Staging 404 at " + fileNotFoundUri.toString() + " to default sotrage system should fail." };
//		response[i++] = new Object[]{ fileNotFoundUri, gridftpSystem, "", false, FilenameUtils.getName(fileNotFoundUri.getPath()), "Staging 404 at " + fileNotFoundUri.toString() + " to gridftp should fail." };
//		response[i++] = new Object[]{ fileNotFoundUri, sftpSystem, "", false, FilenameUtils.getName(fileNotFoundUri.getPath()), "Staging 404 at " + fileNotFoundUri.toString() + " to sftp should fail." };
//		response[i++] = new Object[]{ fileNotFoundUri, irodsSystem, "", false, FilenameUtils.getName(fileNotFoundUri.getPath()), "Staging 404 at " + fileNotFoundUri.toString() + " to irods should fail." };
		
//		response[i++] = new Object[]{ httpMissingPasswordBasicUri, defaultStorageSystem, "", false, FilenameUtils.getName(httpMissingPasswordBasicUri.getPath()), "Missing basic password copying file at " + httpMissingPasswordBasicUri.toString() + " to default sotrage system should fail." };
//		response[i++] = new Object[]{ httpMissingPasswordBasicUri, gridftpSystem, "", false, FilenameUtils.getName(httpMissingPasswordBasicUri.getPath()), "Missing basic password copying file at " + httpMissingPasswordBasicUri.toString() + " to gridftp should fail." };
//		response[i++] = new Object[]{ httpMissingPasswordBasicUri, sftpSystem, "", false, FilenameUtils.getName(httpMissingPasswordBasicUri.getPath()), "Missing basic password copying file at " + httpMissingPasswordBasicUri.toString() + " to sftp should fail." };
//		response[i++] = new Object[]{ httpMissingPasswordBasicUri, irodsSystem, "", false, FilenameUtils.getName(httpMissingPasswordBasicUri.getPath()), "Missing basic password copying file at " + httpMissingPasswordBasicUri.toString() + " to irods should fail." };
//		
//		response[i++] = new Object[]{ httpInvalidPasswordBasicUri, defaultStorageSystem, "", false, FilenameUtils.getName(httpInvalidPasswordBasicUri.getPath()), "Bad basic password copying file at " + httpInvalidPasswordBasicUri.toString() + " to default sotrage system should fail." };
//		response[i++] = new Object[]{ httpInvalidPasswordBasicUri, gridftpSystem, "", false, FilenameUtils.getName(httpInvalidPasswordBasicUri.getPath()), "Bad basic password copying file at " + httpInvalidPasswordBasicUri.toString() + " to gridftp should fail." };
//		response[i++] = new Object[]{ httpInvalidPasswordBasicUri, sftpSystem, "", false, FilenameUtils.getName(httpInvalidPasswordBasicUri.getPath()), "Bad basic password copying file at " + httpInvalidPasswordBasicUri.toString() + " to sftp should fail." };
//		response[i++] = new Object[]{ httpInvalidPasswordBasicUri, irodsSystem, "", false, FilenameUtils.getName(httpInvalidPasswordBasicUri.getPath()), "Bad basic password copying file at " + httpInvalidPasswordBasicUri.toString() + " to irods should fail." };
//		
//		response[i++] = new Object[]{ httpNoPathUri, defaultStorageSystem, "", false, FilenameUtils.getName(httpNoPathUri.getPath()), "No source path found copying file at " + httpNoPathUri.toString() + " to default sotrage system should fail." };
//		response[i++] = new Object[]{ httpNoPathUri, gridftpSystem, "", false, FilenameUtils.getName(httpNoPathUri.getPath()), "No source path found copying file at " + httpNoPathUri.toString() + " to gridftp should fail." };
//		response[i++] = new Object[]{ httpNoPathUri, sftpSystem, "", false, FilenameUtils.getName(httpNoPathUri.getPath()), "No source path found copying file at " + httpNoPathUri.toString() + " to sftp should fail." };
//		response[i++] = new Object[]{ httpNoPathUri, irodsSystem, "", false, FilenameUtils.getName(httpNoPathUri.getPath()), "No source path found copying file at " + httpNoPathUri.toString() + " to irods should fail." };
//		response[i++] = new Object[]{ httpNoPathUri, ftpSystem, "", false, FilenameUtils.getName(httpNoPathUri.getPath()), "No source path found copying file at " + httpNoPathUri.toString() + " to irods should fail." };
//		response[i++] = new Object[]{ httpNoPathUri, s3System, "", false, FilenameUtils.getName(httpNoPathUri.getPath()), "No source path found copying file at " + httpNoPathUri.toString() + " to s3 should fail." };
//		
//		response[i++] = new Object[]{ httpEmptyPathUri, defaultStorageSystem, "", false, FilenameUtils.getName(httpEmptyPathUri.getPath()), "No source path found copying file at " + httpEmptyPathUri.toString() + " to default sotrage system should fail." };
//		response[i++] = new Object[]{ httpEmptyPathUri, gridftpSystem, "", false, FilenameUtils.getName(httpEmptyPathUri.getPath()), "No source path found copying file at " + httpEmptyPathUri.toString() + " to gridftp should fail." };
//		response[i++] = new Object[]{ httpEmptyPathUri, sftpSystem, "", false, FilenameUtils.getName(httpEmptyPathUri.getPath()), "No source path found copying file at " + httpEmptyPathUri.toString() + " to sftp should fail." };
//		response[i++] = new Object[]{ httpEmptyPathUri, irodsSystem, "", false, FilenameUtils.getName(httpEmptyPathUri.getPath()), "No source path found copying file at " + httpEmptyPathUri.toString() + " to irods should fail." };
//		response[i++] = new Object[]{ httpEmptyPathUri, ftpSystem, "", false, FilenameUtils.getName(httpEmptyPathUri.getPath()), "No source path found copying file at " + httpEmptyPathUri.toString() + " to ftp should fail." };
//		response[i++] = new Object[]{ httpEmptyPathUri, s3System, "", false, FilenameUtils.getName(httpEmptyPathUri.getPath()), "No source path found copying file at " + httpEmptyPathUri.toString() + " to s3 should fail." };
		
		return testCases.toArray(new Object[][] {});
	}
	
	@Test(dataProvider="testStageNextURIProvider", dependsOnMethods={"testStageNextFileQueueEmpty"})
	public void testStageNextURI(URI sourceUri, RemoteSystem destSystem, String destPath, boolean shouldSucceed, String expectedPath, String message) 
	{
		RemoteDataClient destClient = null;
		try 
		{
			// verify the file is there
			destClient = destSystem.getRemoteDataClient();
			destClient.authenticate();
			if (!destClient.doesExist("")) {
				destClient.mkdirs("");
			}
			
			StagingTask task = createStagingTaskForUrl(sourceUri, destSystem, destPath);
			
			stagingJob.setQueueTask(task);
			
			stagingJob.doExecute();
			
			file = LogicalFileDao.findById(file.getId());
			
			if (shouldSucceed) 
			{
				Assert.assertEquals(file.getStatus(), TransformTaskStatus.TRANSFORMING_COMPLETED.name(), message );
				
				Assert.assertTrue(destClient.doesExist(expectedPath), 
						"Staged file is not present on the remote system.");
				
				Assert.assertEquals(destClient.resolvePath(expectedPath), file.getPath(), 
						"Expected path of " + expectedPath + " differed from the relative logical file path of " + file.getAgaveRelativePathFromAbsolutePath());
				
				Assert.assertTrue(destClient.isFile(expectedPath), 
						"Staged file is present, but not a file on the remote system.");
			}
			else 
			{
				Assert.assertEquals(file.getStatus(), StagingTaskStatus.STAGING_FAILED.name(), message );
			}
		} 
		catch (Exception e) {
			Assert.fail("File staging should not throw an exception", e);
		}
		finally {
			try { destClient.delete(""); } catch (Exception e) {}
			try { destClient.disconnect(); } catch (Exception e) {}
		}
	}
	
	@DataProvider
	private Object[][] testStageNextAgaveFileProvider(Method m) throws Exception
	{
		String localFilename = FilenameUtils.getName(LOCAL_BINARY_FILE);
		List<Object[]> testCases = new ArrayList<Object[]>();
        
        for (StorageSystem srcSystem: Arrays.asList(defaultStorageSystem, gridftpSystem, sftpSystem, ftpSystem, s3System, irodsSystem)) {
                
            for (StorageSystem destSystem: Arrays.asList(defaultStorageSystem, gridftpSystem, sftpSystem, ftpSystem, s3System, irodsSystem)) {
                
                if (!srcSystem.equals(destSystem)) {
                    // copy file to home directory
                    testCases.add(new Object[] { srcSystem, destSystem, localFilename, "", localFilename });
                    testCases.add(new Object[] { srcSystem, destSystem, localFilename, localFilename, localFilename });
                }
            }
        }
        return testCases.toArray(new Object[][] {});
		
//		return new Object[][] {
//				// copy file to home directory
//				{ gridftpSystem, defaultStorageSystem, localFilename, "", localFilename },
//				{ gridftpSystem, sftpSystem, localFilename, "", localFilename },
//				{ gridftpSystem, irodsSystem, localFilename, "", localFilename },
//
//				{ sftpSystem, irodsSystem, localFilename, "", localFilename },
//				{ sftpSystem, gridftpSystem, localFilename, "", localFilename },
//				
//				{ irodsSystem, defaultStorageSystem, localFilename, "", localFilename },
//				{ irodsSystem, sftpSystem, localFilename, "", localFilename },
//				{ irodsSystem, gridftpSystem, localFilename, "", localFilename },
//				
//				// copy file to named file in remote home directory that does not exist
//				{ gridftpSystem, defaultStorageSystem, localFilename, localFilename, localFilename },
//				{ gridftpSystem, sftpSystem, localFilename, localFilename, localFilename },
//				{ gridftpSystem, irodsSystem, localFilename, localFilename, localFilename },
//
//				{ sftpSystem, irodsSystem, localFilename, localFilename, localFilename },
//				{ sftpSystem, gridftpSystem, localFilename, localFilename, localFilename },
//				
//				{ irodsSystem, defaultStorageSystem, localFilename, localFilename, localFilename },
//				{ irodsSystem, sftpSystem, localFilename, localFilename, localFilename },
//				{ irodsSystem, gridftpSystem, localFilename, localFilename, localFilename },
//		};
	}
	
	@Test(dataProvider="testStageNextAgaveFileProvider", dependsOnMethods={"testStageNextURI"})
	public void testStageNextFile(RemoteSystem sourceSystem, RemoteSystem destSystem, String srcPath, String destPath, String expectedPath) 
	{
		RemoteDataClient srcClient = null;
		RemoteDataClient destClient = null;
		try 
		{
			destClient = destSystem.getRemoteDataClient();
			destClient.authenticate();
			
			if (destClient.doesExist(expectedPath)) {
				destClient.delete(expectedPath);
			}
			
			if (!destClient.doesExist("")) {
				destClient.mkdirs("");
			}
			
			srcClient = sourceSystem.getRemoteDataClient();
			srcClient.authenticate();
			srcClient.mkdirs("");
			srcClient.put(LOCAL_BINARY_FILE, "");
			
			StagingTask task = createStagingTaskForUrl(new URI("agave://" + sourceSystem.getSystemId() + "/" + srcPath), destSystem, destPath);
			
			stagingJob.setQueueTask(task);
            
            stagingJob.doExecute();
            
			
			file = LogicalFileDao.findById(file.getId());
			
			Assert.assertEquals(file.getStatus(), TransformTaskStatus.TRANSFORMING_COMPLETED.name(),
					"Logical file status was not TRANSFORMING_COMPLETED" );
			
			Assert.assertTrue(destClient.doesExist(expectedPath), 
					"Staged file is not present on the remote system.");
			
			Assert.assertEquals(expectedPath, file.getAgaveRelativePathFromAbsolutePath(), 
					"Expected path of " + expectedPath + " differed from the relative logical file path of " + file.getAgaveRelativePathFromAbsolutePath());
			
			Assert.assertTrue(destClient.isFile(expectedPath), 
					"Staged file is present, but not a file on the remote system.");
		} 
		catch (Exception e) {
			Assert.fail("File staging should not throw an exception", e);
		} 
		finally {
			try { srcClient.delete(""); } catch (Exception e) {}
			try { srcClient.disconnect(); } catch (Exception e) {}
			try { destClient.delete(""); } catch (Exception e) {}
			try { destClient.disconnect(); } catch (Exception e) {}
		}
	}
	
	@DataProvider
	private Object[][] testStageNextAgaveSourceFolderToDestFileFailsProvider(Method m) throws Exception
	{
		String localFilename = FilenameUtils.getName(LOCAL_BINARY_FILE);
		String localDirectory = FilenameUtils.getName(LOCAL_DIR);
		
		List<Object[]> testCases = new ArrayList<Object[]>();
        
        for (StorageSystem srcSystem: Arrays.asList(defaultStorageSystem, gridftpSystem, sftpSystem, ftpSystem, s3System, irodsSystem)) {
                
            for (StorageSystem destSystem: Arrays.asList(defaultStorageSystem, gridftpSystem, sftpSystem, ftpSystem, s3System, irodsSystem)) {
                
                if (!srcSystem.equals(destSystem)) {
                 // copy file to home directory
                    testCases.add(new Object[] { srcSystem, destSystem, localDirectory, localFilename });
                }
            }
        }
        return testCases.toArray(new Object[][] {});
//		return new Object[][] {
//				// copy file to home directory
//				{ gridftpSystem, defaultStorageSystem, localDirectory, localFilename },
//				{ gridftpSystem, sftpSystem, localDirectory, localFilename },
//				{ gridftpSystem, irodsSystem, localDirectory, localFilename },
//				{ gridftpSystem, gridftpSystem, localDirectory, localFilename },
//				
////				{ sftpSystem, defaultStorageSystem, localDirectory, localFilename },
////				{ sftpSystem, sftpSystem, localDirectory, localFilename },
//				{ sftpSystem, irodsSystem, localDirectory, localFilename },
//				{ sftpSystem, gridftpSystem, localDirectory, localFilename },
//				
//				{ irodsSystem, defaultStorageSystem, localDirectory, localFilename },
//				{ irodsSystem, sftpSystem, localDirectory, localFilename },
//				{ irodsSystem, irodsSystem, localDirectory, localFilename },
//				{ irodsSystem, gridftpSystem, localDirectory, localFilename },
//		};
	}
	
	@Test(dataProvider="testStageNextAgaveSourceFolderToDestFileFailsProvider", dependsOnMethods={"testStageNextFile"})
	public void testStageNextAgaveSourceFolderToDestFileFails(RemoteSystem sourceSystem, RemoteSystem destSystem, String srcPath, String destPath) 
	{
		RemoteDataClient srcClient = null;
		RemoteDataClient destClient = null;
		try 
		{
			// load the db with dummy records to stage a http-accessible file
			destClient = destSystem.getRemoteDataClient();
			destClient.authenticate();
			
			if (destClient.doesExist(destPath)) {
				destClient.delete(destPath);
			}
			
			if (!destClient.doesExist("")) {
				destClient.mkdirs("");
			}
			
			// stage file to destination Path to cause the failure
			destClient.put(LOCAL_BINARY_FILE, destPath);
			
			srcClient = sourceSystem.getRemoteDataClient();
			srcClient.authenticate();
			srcClient.mkdirs("");
			srcClient.put(LOCAL_DIR, "");
			
			StagingTask task = createStagingTaskForUrl(new URI("agave://" + sourceSystem.getSystemId() + "/" + srcPath), destSystem, destPath);
			
			stagingJob.setQueueTask(task);
			
			stagingJob.doExecute();
			
			file = LogicalFileDao.findById(file.getId());
			
			Assert.assertEquals(file.getStatus(), StagingTaskStatus.STAGING_FAILED.name(),
					"Logical file status was not STAGING_FAILED when staging a directory onto a file" );
		} 
		catch (Exception e) {
			Assert.fail("File staging should not throw an exception", e);
		} 
		finally {
			try { srcClient.delete(""); } catch (Exception e) {}
			try { srcClient.disconnect(); } catch (Exception e) {}
			try { destClient.delete(""); } catch (Exception e) {}
			try { destClient.disconnect(); } catch (Exception e) {}
		}
	}
	
	@DataProvider
	private Object[][] testStageNextAgaveFolderProvider(Method m) throws Exception
	{
		String localDirectoryName = FilenameUtils.getName(LOCAL_DIR);
		
		List<Object[]> testCases = new ArrayList<Object[]>();
        
        for (StorageSystem srcSystem: Arrays.asList(defaultStorageSystem, gridftpSystem, sftpSystem, ftpSystem, s3System, irodsSystem)) {
                
            for (StorageSystem destSystem: Arrays.asList(defaultStorageSystem, gridftpSystem, sftpSystem, ftpSystem, s3System, irodsSystem)) {
                
                if (!srcSystem.equals(destSystem)) {
                    // copy file to home directory
                    testCases.add(new Object[] { srcSystem, destSystem, localDirectoryName, "", localDirectoryName });
                    
                    // copy file to named file in remote home directory that does not exist
                    testCases.add(new Object[] { srcSystem, destSystem, localDirectoryName, localDirectoryName, localDirectoryName });
                    
                }
            }
        }
        
        return testCases.toArray(new Object[][] {});
//       return new Object[][] { 
//                { gridftpSystem, defaultStorageSystem, localDirectoryName, "", localDirectoryName },
//				{ gridftpSystem, sftpSystem, localDirectoryName, "", localDirectoryName },
//				{ gridftpSystem, irodsSystem, localDirectoryName, "", localDirectoryName },
//				
//				{ sftpSystem, irodsSystem, localDirectoryName, "", localDirectoryName },
//				{ sftpSystem, gridftpSystem, localDirectoryName, "", localDirectoryName },
//				
//				{ irodsSystem, defaultStorageSystem, localDirectoryName, "", localDirectoryName },
//				{ irodsSystem, sftpSystem, localDirectoryName, "", localDirectoryName },
//				{ irodsSystem, gridftpSystem, localDirectoryName, "", localDirectoryName },
//				
//				// copy file to named file in remote home directory that does not exist
//				{ gridftpSystem, defaultStorageSystem, localDirectoryName, localDirectoryName, localDirectoryName },
//				{ gridftpSystem, sftpSystem, localDirectoryName, localDirectoryName, localDirectoryName },
//				{ gridftpSystem, irodsSystem, localDirectoryName, localDirectoryName, localDirectoryName },
//				
//				{ sftpSystem, irodsSystem, localDirectoryName, localDirectoryName, localDirectoryName },
//				{ sftpSystem, gridftpSystem, localDirectoryName, localDirectoryName, localDirectoryName },
//				
//				{ irodsSystem, defaultStorageSystem, localDirectoryName, localDirectoryName, localDirectoryName },
//				{ irodsSystem, sftpSystem, localDirectoryName, localDirectoryName, localDirectoryName },
//				{ irodsSystem, gridftpSystem, localDirectoryName, localDirectoryName, localDirectoryName },
//		};
	}
	
	@Test(dataProvider="testStageNextAgaveFolderProvider", dependsOnMethods={"testStageNextAgaveSourceFolderToDestFileFails"})
	public void testStageNextFolder(RemoteSystem sourceSystem, RemoteSystem destSystem, String srcPath, String destPath, String expectedPath) 
	{
		RemoteDataClient srcClient = null;
		RemoteDataClient destClient = null;
		try 
		{
			// load the db with dummy records to stage a http-accessible file
			destClient = destSystem.getRemoteDataClient();
			destClient.authenticate();
			
			if (destClient.doesExist(expectedPath)) {
				destClient.delete(expectedPath);
			}
			
			if (!destClient.doesExist("")) {
				destClient.mkdirs("");
			}
			
			srcClient = sourceSystem.getRemoteDataClient();
			srcClient.authenticate();
			srcClient.mkdirs("");
			srcClient.put(LOCAL_DIR, "");
			
			StagingTask task = createStagingTaskForUrl(new URI("agave://" + sourceSystem.getSystemId() + "/" + srcPath), destSystem, destPath);
			
            stagingJob.setQueueTask(task);
            
            stagingJob.doExecute();
			
			file = LogicalFileDao.findById(file.getId());
			
			Assert.assertEquals(file.getStatus(), TransformTaskStatus.TRANSFORMING_COMPLETED.name(),
					"Logical file status was not TRANSFORMING_COMPLETED" );
			
			Assert.assertTrue(destClient.doesExist(expectedPath), 
					"Staged file is not present on the remote system.");
			
			Assert.assertEquals(expectedPath, file.getAgaveRelativePathFromAbsolutePath(), 
					"Expected path of " + expectedPath + " differed from the relative logical file path of " + file.getAgaveRelativePathFromAbsolutePath());
			
			Assert.assertTrue(destClient.isDirectory(expectedPath), 
					"Staged file is present, but not a file on the remote system.");
		} 
		catch (Exception e) {
			Assert.fail("File staging should not throw an exception", e);
		} 
		finally {
			try { srcClient.delete(""); } catch (Exception e) {}
			try { srcClient.disconnect(); } catch (Exception e) {}
			try { destClient.delete(""); } catch (Exception e) {}
			try { destClient.disconnect(); } catch (Exception e) {}
		}
	}
	
	@DataProvider
	private Object[][] testStageNextAgaveFileSourcePathDoesNotExistProvider(Method m) throws Exception
	{
        List<Object[]> testCases = new ArrayList<Object[]>();
                
        for (StorageSystem srcSystem: Arrays.asList(defaultStorageSystem, gridftpSystem, sftpSystem, ftpSystem, s3System, irodsSystem)) {
                
            for (StorageSystem destSystem: Arrays.asList(defaultStorageSystem, gridftpSystem, sftpSystem, ftpSystem, s3System, irodsSystem)) {
                
                if (!srcSystem.equals(destSystem)) {
                    // copy file to home directory
                    testCases.add(new Object[] { srcSystem, destSystem,  MISSING_FILE, "", "Staging should fail when source path file does not exist" });
                    testCases.add(new Object[] { srcSystem, destSystem,  MISSING_DIRECTORY, "", "Staging should fail when source path dir does not exist" });
                }
            }
        }
        return testCases.toArray(new Object[][] {});
//		return new Object[][] {			
//			{ gridftpSystem, defaultStorageSystem, MISSING_FILE, "", "Staging should fail when source path does not exist" },
//			{ gridftpSystem, defaultStorageSystem, MISSING_DIRECTORY, "", "Staging should fail when source path does not exist" },
//			
//			{ irodsSystem, defaultStorageSystem, MISSING_FILE, "", "Staging should fail when source path does not exist" },
//			{ irodsSystem, defaultStorageSystem, MISSING_DIRECTORY, "", "Staging should fail when source path does not exist" },
//			
//			{ sftpSystem, defaultStorageSystem, MISSING_FILE, "", "Staging should fail when source path does not exist" },
//			{ sftpSystem, defaultStorageSystem, MISSING_DIRECTORY, "", "Staging should fail when source path does not exist" },
//			
//			{ ftpSystem, defaultStorageSystem, MISSING_FILE, "", "Staging should fail when source path does not exist" },
//            { ftpSystem, defaultStorageSystem, MISSING_DIRECTORY, "", "Staging should fail when source path does not exist" },
//			
//            { s3System, defaultStorageSystem, MISSING_FILE, "", "Staging should fail when source path does not exist" },
//            { s3System, defaultStorageSystem, MISSING_DIRECTORY, "", "Staging should fail when source path does not exist" },
//		};
	}
	
	@Test(dataProvider="testStageNextAgaveFileSourcePathDoesNotExistProvider", dependsOnMethods={"testStageNextFolder"})
	public void testStageNextAgaveFileSourcePathDoesNotExist(RemoteSystem sourceSystem, RemoteSystem destSystem, String srcPath, String destPath, String message) 
	{
		RemoteDataClient srcClient = null;
		RemoteDataClient destClient = null;
		try 
		{
			// load the db with dummy records to stage a http-accessible file
			destClient = destSystem.getRemoteDataClient();
			destClient.authenticate();
			
			if (!destClient.doesExist("")) {
				destClient.mkdirs("");
			}
			
			srcClient = sourceSystem.getRemoteDataClient();
			srcClient.authenticate();
			if (srcClient.doesExist(srcPath)) {
				srcClient.delete(srcPath);
			}
			
			StagingTask task = createStagingTaskForUrl(new URI("agave://" + sourceSystem.getSystemId() + "/" + srcPath), destSystem, destPath);
			
			stagingJob.setQueueTask(task);
            
            stagingJob.doExecute();
			
			file = LogicalFileDao.findById(file.getId());
			
			Assert.assertEquals(file.getStatus(), StagingTaskStatus.STAGING_FAILED.name(), message);
		} 
		catch (Exception e) {
			Assert.fail("File staging should not throw an exception", e);
		} 
		finally {
			try { srcClient.delete(""); } catch (Exception e) {}
			try { srcClient.disconnect(); } catch (Exception e) {}
			try { destClient.delete(""); } catch (Exception e) {}
			try { destClient.disconnect(); } catch (Exception e) {}
		}
	}
			
	@DataProvider
	private Object[][] testStageNextAgaveFileDestPathDoesNotExistProvider(Method m) throws Exception
	{
		String localFilename = FilenameUtils.getName(LOCAL_BINARY_FILE);
		
		List<Object[]> testCases = new ArrayList<Object[]>();
        
        for (StorageSystem srcSystem: Arrays.asList(defaultStorageSystem, gridftpSystem, sftpSystem, ftpSystem, s3System, irodsSystem)) {
            if (!srcSystem.equals(defaultStorageSystem)) {
                // copy file to home directory
                testCases.add(new Object[] { srcSystem, defaultStorageSystem, localFilename, MISSING_FILE, "", "Staging should fail when dest path file does not exist" });
                testCases.add(new Object[] { srcSystem, defaultStorageSystem, localFilename, MISSING_DIRECTORY, "", "Staging should fail when dest path dir does not exist" });
            }
        }
        
        return testCases.toArray(new Object[][] {});
		
//		return new Object[][] {		
//			{ gridftpSystem, defaultStorageSystem, localFilename, MISSING_FILE, "Staging should fail when dest path does not exist" },
//			{ gridftpSystem, defaultStorageSystem, localFilename, MISSING_DIRECTORY, "Staging should fail when dest path does not exist" },
//			{ irodsSystem, defaultStorageSystem, localFilename, MISSING_FILE, "Staging should fail when dest path does not exist" },
//			{ irodsSystem, defaultStorageSystem, localFilename, MISSING_DIRECTORY, "Staging should fail when dest path does not exist" },
//			{ sftpSystem, defaultStorageSystem, localFilename, MISSING_FILE, "Staging should fail when dest path does not exist" },
//			{ sftpSystem, defaultStorageSystem, localFilename, MISSING_DIRECTORY, "Staging should fail when dest path does not exist" },
//		};
	}
		
	@Test(dataProvider="testStageNextAgaveFileDestPathDoesNotExistProvider", dependsOnMethods={"testStageNextAgaveFileSourcePathDoesNotExist"})
	public void testStageNextAgaveFileDestPathDoesNotExist(RemoteSystem sourceSystem, RemoteSystem destSystem, String srcPath, String destPath, String message) 
	{
		RemoteDataClient srcClient = null;
		RemoteDataClient destClient = null;
		try 
		{
			// load the db with dummy records to stage a http-accessible file
			destClient = destSystem.getRemoteDataClient();
			destClient.authenticate();
			
			if (!destClient.doesExist("")) {
				destClient.mkdirs("");
			}
			
			srcClient = sourceSystem.getRemoteDataClient();
			srcClient.authenticate();
			srcClient.mkdirs("");
			srcClient.put(LOCAL_BINARY_FILE, "");
			
			StagingTask task = createStagingTaskForUrl(new URI("agave://" + sourceSystem.getSystemId() + "/" + srcPath), destSystem, destPath);
			
			stagingJob.setQueueTask(task);
            
            stagingJob.doExecute();
			
			file = LogicalFileDao.findById(file.getId());
			
			Assert.assertEquals(file.getStatus(), StagingTaskStatus.STAGING_FAILED.name(), message);
		} 
		catch (Exception e) {
			Assert.fail("File staging should not throw an exception", e);
		} 
		finally {
			try { srcClient.delete(""); } catch (Exception e) {}
			try { srcClient.disconnect(); } catch (Exception e) {}
			try { destClient.delete(""); } catch (Exception e) {}
			try { destClient.disconnect(); } catch (Exception e) {}
		}
	}
				
	@DataProvider
	private Object[][] testStageNextAgaveFileSourceNoPermissionProvider(Method m) throws Exception
	{
		return new Object[][] {		
				{ gridftpSystem, defaultStorageSystem, "/root", "", "Staging should fail when user does not have permission on source path" },
				{ irodsSystem, defaultStorageSystem, "/testotheruser", "", "Staging should fail when user does not have permission on source path" },
				{ sftpSystem, defaultStorageSystem, "/root", "", "Staging should fail when user does not have permission on source path" },
				{ ftpSystem, defaultStorageSystem, "/root", "", "Staging should fail when user does not have permission on source path" },
				{ s3System, defaultStorageSystem, "/", "", "Staging should fail when user does not have permission on source path" },
		};
	}	
	
	@Test(dataProvider="testStageNextAgaveFileSourceNoPermissionProvider", dependsOnMethods={"testStageNextAgaveFileDestPathDoesNotExist"})
	public void testStageNextAgaveFileSourceNoPermission(RemoteSystem sourceSystem, RemoteSystem destSystem, String srcPath, String destPath, String message) 
	{
		RemoteDataClient destClient = null;
		try 
		{
			// load the db with dummy records to stage a http-accessible file
			destClient = destSystem.getRemoteDataClient();
			destClient.authenticate();
			
			if (!destClient.doesExist("")) {
				destClient.mkdirs("");
			}
			
			StagingTask task = createStagingTaskForUrl(new URI("agave://" + sourceSystem.getSystemId() + "/" + srcPath), destSystem, destPath);
			
			stagingJob.setQueueTask(task);
            
            stagingJob.doExecute();
			
			file = LogicalFileDao.findById(file.getId());
			
			Assert.assertEquals(file.getStatus(), StagingTaskStatus.STAGING_FAILED.name(), message);
		} 
		catch (Exception e) {
			Assert.fail("File staging should not throw an exception", e);
		} 
		finally {
			try { destClient.delete(""); } catch (Exception e) {}
			try { destClient.disconnect(); } catch (Exception e) {}
		}
	}
	
	@DataProvider
	private Object[][] testStageNextAgaveFileDestNoPermissionProvider(Method m) throws Exception
	{
		String localFilename = FilenameUtils.getName(LOCAL_BINARY_FILE);
		List<Object[]> testCases = new ArrayList<Object[]>();
        
        for (StorageSystem srcSystem: Arrays.asList(defaultStorageSystem, gridftpSystem, sftpSystem, ftpSystem, s3System, irodsSystem)) {
            if (!srcSystem.equals(defaultStorageSystem)) {
                // copy file to home directory
                testCases.add(new Object[] { srcSystem, defaultStorageSystem, localFilename, "/root", "Staging should fail when user does not have permission on dest path" });
            }
        }
        
        return testCases.toArray(new Object[][] {});
//		return new Object[][] {		
//				{ gridftpSystem, defaultStorageSystem, localFilename, "/root", "Staging should fail when user does not have permission on dest path" },
//				{ irodsSystem, defaultStorageSystem, localFilename, "/root", "Staging should fail when user does not have permission on dest path" },
//				{ sftpSystem, defaultStorageSystem, localFilename, "/root", "Staging should fail when user does not have permission on dest path" },
//				
//		};
	}
	
	@Test(dataProvider="testStageNextAgaveFileDestNoPermissionProvider", dependsOnMethods={"testStageNextAgaveFileSourceNoPermission"})
	public void testStageNextAgaveFileDestNoPermission(RemoteSystem sourceSystem, RemoteSystem destSystem, String srcPath, String destPath, String message) 
	{
		RemoteDataClient srcClient = null;
		RemoteDataClient destClient = null;
		try 
		{
			// load the db with dummy records to stage a http-accessible file
			destClient = destSystem.getRemoteDataClient();
			destClient.authenticate();
			
			if (!destClient.doesExist("")) {
				destClient.mkdirs("");
			}
			
			srcClient = sourceSystem.getRemoteDataClient();
			srcClient.authenticate();
			srcClient.mkdirs("");
			srcClient.put(LOCAL_BINARY_FILE, "");
			
			StagingTask task = createStagingTaskForUrl(new URI("agave://" + sourceSystem.getSystemId() + "/" + srcPath), destSystem, destPath);
			
            stagingJob.setQueueTask(task);
            
            stagingJob.doExecute();
			
			file = LogicalFileDao.findById(file.getId());
			
			Assert.assertEquals(file.getStatus(), StagingTaskStatus.STAGING_FAILED.name(), message);
		} 
		catch (Exception e) {
			Assert.fail("File staging should not throw an exception", e);
		} 
		finally {
			try { srcClient.delete(""); } catch (Exception e) {}
			try { srcClient.disconnect(); } catch (Exception e) {}
			try { destClient.delete(""); } catch (Exception e) {}
			try { destClient.disconnect(); } catch (Exception e) {}
		}
	}
	
//	@Test(dependsOnMethods={"testStageNextAgaveFileDestNoPermission"})
//    public void testStageNextAgaveJobSourceNoPermissionProvider() throws Exception
//    {
//	    
//	    
//    }
	
	
	
}
