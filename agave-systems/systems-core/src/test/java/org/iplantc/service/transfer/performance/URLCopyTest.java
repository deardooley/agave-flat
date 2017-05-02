package org.iplantc.service.transfer.performance;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.util.FileUtils;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.systems.exceptions.RemoteCredentialException;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.enumerations.StorageProtocolType;
import org.iplantc.service.transfer.BaseTransferTestCase;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteDataClientFactory;
import org.iplantc.service.transfer.URLCopy;
import org.iplantc.service.transfer.dao.TransferTaskDao;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.exceptions.TransferException;
import org.iplantc.service.transfer.model.TransferTask;
import org.iplantc.service.transfer.model.enumerations.TransferStatusType;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups= {"transfer","performance","integration"})
public class URLCopyTest extends BaseTransferTestCase
{
	protected static String LOCAL_DIR = "src/test/resources/transfer";
	protected static String LOCAL_DOWNLOAD_DIR = "src/test/resources/download";
	protected static String LOCAL_TXT_FILE = "src/test/resources/transfer/test_upload.txt";
	protected static String LOCAL_BINARY_FILE = "src/test/resources/transfer/test_upload.bin";
	
	protected static String SOURCE_DIRNAME = "transfer";
	protected static String DEST_DIRNAME = "transfer.copy";
	protected static String SOURCE_FILENAME = "test_upload.bin";
	protected static String DEST_FILENAME = "test_upload.bin.copy";

	List<StorageProtocolType> testSrcTypes = new ArrayList<StorageProtocolType>();
	List<StorageProtocolType> testDestTypes = new ArrayList<StorageProtocolType>();
	
	@BeforeClass
	public void beforeClass() throws Exception
	{
		super.beforeClass();
		clearLogicalFiles();
		clearTransferTasks();
//		testSrcTypes.add(StorageProtocolType.GRIDFTP);
//		testSrcTypes.add(StorageProtocolType.S3);
		testSrcTypes.add(StorageProtocolType.SFTP);
//		testSrcTypes.add(StorageProtocolType.FTP);
//		testSrcTypes.add(StorageProtocolType.IRODS);
		testSrcTypes.add(StorageProtocolType.LOCAL);
		
//		testDestTypes.add(StorageProtocolType.GRIDFTP);
//		testDestTypes.add(StorageProtocolType.S3);
		testDestTypes.add(StorageProtocolType.SFTP);
//		testDestTypes.add(StorageProtocolType.FTP);
//		testDestTypes.add(StorageProtocolType.IRODS);
		testDestTypes.add(StorageProtocolType.LOCAL);
	}


	@AfterClass
	public void afterClass() throws Exception
	{
		clearLogicalFiles();
		clearTransferTasks();
	}
	
	@BeforeMethod
	public void beforeMethod() throws Exception
	{
		clearLogicalFiles();
	}

	@AfterMethod
	public void afterMethod() throws Exception
	{
		
	}
	

	private RemoteDataClient getRemoteDataClientFromSystemJson(StorageProtocolType type) throws Exception
	{
		return getRemoteDataClientFromSystemJson(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + 
				type.name().toLowerCase() + ".example.com.json");
	}
	
	private RemoteDataClient getRemoteDataClientFromSystemJson(String pathToSystemJson) throws Exception
	{	 
		JSONObject json = jtd.getTestDataObject(pathToSystemJson);
    	StorageSystem system = (StorageSystem)StorageSystem.fromJSON(json);
    	system.setOwner(SYSTEM_USER);
    	
    	RemoteDataClient client = system.getRemoteDataClient();
    	client.updateSystemRoots(client.getRootDir(), system.getStorageConfig().getHomeDir() + "/thread-" + Thread.currentThread().getId());
    	client.authenticate();
    	client.mkdirs("");
    	
    	return client;
	}
	
	@Test(dataProvider="copyProvider")
	public void copyUnsavedTransferTaskThrowsException(StorageProtocolType srcClientType, StorageProtocolType destClientType, String message, boolean shouldThrowException)
	throws Exception
	{
		RemoteDataClient srcClient = null;
		RemoteDataClient destClient = null;
		
		boolean actuallyThrewException = false;
		try 
		{
			srcClient = getRemoteDataClientFromSystemJson(srcClientType);
			srcClient.authenticate();
			
			destClient = getRemoteDataClientFromSystemJson(destClientType);
			destClient.authenticate();
			
			URLCopy urlCopy = new URLCopy(srcClient, destClient);
			
			urlCopy.copy(SOURCE_DIRNAME, DEST_DIRNAME, new TransferTask(SOURCE_DIRNAME, DEST_DIRNAME));
			
			Assert.fail("Unsaved transferTask should throw a TransferException");
			
//			destClient.authenticate();
//			Assert.assertTrue(destClient.doesExist(DEST_DIRNAME), 
//					"Failed to copy test file to dest system");
		}
		catch (TransferException e) 
		{
    		// this is expected behavior
        }
		catch (Exception e) {
			Assert.fail("Unsaved transferTask should throw a TransferException", e);
		}
		finally {
			try { srcClient.disconnect(); } catch (Exception e) {}
			try { destClient.disconnect(); } catch (Exception e) {}
		}
		
		Assert.assertEquals(actuallyThrewException, shouldThrowException, message);
	}

	@DataProvider(name = "httpImportProvider")
	protected Object[][] httpImportProvider() throws RemoteDataException, RemoteCredentialException, PermissionException
	{
		List<Object[]> testCases = new ArrayList<Object[]>();
		for(StorageProtocolType destType: testDestTypes) {
			testCases.add(new Object[] { URI.create("http://preview.agaveapi.co/wp-content/themes/agave/images/favicon.ico"), destType, "Public URL copy to " + destType.name() + " should succeed", false });
			testCases.add(new Object[] { URI.create("http://agaveapi.co/wp-content/themes/agave/images/favicon.ico"), destType, "Public 301 redirected URL copy to " + destType.name() + " should succeed", false });
			testCases.add(new Object[] { URI.create("https://avatars0.githubusercontent.com/u/785202"), destType, "Public HTTPS URL copy to " + destType.name() + " should succeed", false });
			testCases.add(new Object[] { URI.create("http://docker.example.com:10080/public/" + DEST_FILENAME), destType, "Public URL copy to " + destType.name() + " on alternative port should succeed", false });
			testCases.add(new Object[] { URI.create("https://docker.example.com:10443/public/" + DEST_FILENAME), destType, "Public HTTPS URL copy to " + destType.name() + " on alternative port should succeed", false });
			testCases.add(new Object[] { URI.create("https://docker.example.com:10443/public/" + DEST_FILENAME + "?t=now"), destType, "Public URL copy to " + destType.name() + " should succeed", false });
			testCases.add(new Object[] { URI.create("http://testuser:testuser@docker.example.com:10080/private/" + DEST_FILENAME), destType, "Public URL copy to " + destType.name() + " should succeed", false });
			
			testCases.add(new Object[] { URI.create("http://docker.example.com:10080"), destType, "Public URL copy of no path to " + destType.name() + " should fail", true });
			testCases.add(new Object[] { URI.create("http://docker.example.com:10080/"), destType, "Public URL copy of empty path to " + destType.name() + " should fail", true });
			testCases.add(new Object[] { URI.create("http://docker.example.com:10080/" + MISSING_FILE), destType, "Missing file URL " + destType.name() + " should fail", true });
			testCases.add(new Object[] { URI.create("http://testuser@docker.example.com:10080/private/test_upload.bin"), destType, "Protected URL missing password should fail auth and copy to " + destType.name() + " should fail", true });
			testCases.add(new Object[] { URI.create("http://testuser:testotheruser@docker.example.com:10080/private/test_upload.bin"), destType, "Protected URL with bad credentials should fail auth and copy to " + destType.name() + " should fail", true });
		}
		
		return new Object[][] {
			
		};
	}
	
	@Test(dataProvider="httpImportProvider", dependsOnMethods={"copyUnsavedTransferTaskThrowsException"}, enabled=true)
	public void copyFileHttpImport(URI httpUri, StorageProtocolType destClientType, String message, boolean shouldThrowException)
	throws Exception
	{
		RemoteDataClient srcClient = null;
		RemoteDataClient destClient = null;
		
		boolean actuallyThrewException = false;
		try 
		{
			srcClient = new RemoteDataClientFactory().getInstance(SYSTEM_USER, null, httpUri);
			srcClient.authenticate();
			
			destClient = getRemoteDataClientFromSystemJson(destClientType);
			destClient.authenticate();
			
			TransferTask task = new TransferTask(
					httpUri.toString(), 
					destClient.getUriForPath(DEST_FILENAME).toString(),
					SYSTEM_USER, null, null);
			
			TransferTaskDao.persist(task);
			
			URLCopy urlCopy = new URLCopy(srcClient, destClient);
			
			String sUrl = httpUri.toString();
			urlCopy.copy(sUrl.substring(sUrl.indexOf(httpUri.getPath())), DEST_FILENAME, task);
			
			destClient.authenticate();
			Assert.assertTrue(destClient.doesExist(DEST_FILENAME), 
					"Failed to copy test file to dest system");
			
			TransferTask savedTask = TransferTaskDao.getById(task.getId());
			
			Assert.assertEquals(savedTask.getBytesTransferred(),
					destClient.length(DEST_FILENAME), 
					"Transfer task bytes transferred was not updated by status listener.");
			Assert.assertEquals(savedTask.getTotalSize(),
					destClient.length(DEST_FILENAME ),
					"Transfer task total bytes was not updated by status listener.");
			Assert.assertEquals(savedTask.getStatus(),
					TransferStatusType.COMPLETED,
					"Transfer task status was not updated by status listener.");
		}
		catch (Exception e) 
		{
    		actuallyThrewException = true;
        	if (!shouldThrowException) e.printStackTrace();
        }
		finally {
			try { destClient.delete(""); } catch (Exception e) {}
			try { destClient.delete(DEST_FILENAME); } catch (Exception e) {}
		}
		
		Assert.assertEquals(actuallyThrewException, shouldThrowException, message);
	}
	
	@DataProvider(name = "ftpFileImportProvider")
	protected Object[][] ftpFileImportProvider() throws RemoteDataException, RemoteCredentialException, PermissionException
	{
		List<Object[]> testCases = new ArrayList<Object[]>();
		for (StorageProtocolType destType: testDestTypes) 
		{	
			testCases.add(new Object[] { URI.create("ftp://testuser:testuser@docker.example.com:10021/" + DEST_FILENAME), destType, true, "FTP URI with auth should succeed for relative file path" }); 
			testCases.add(new Object[] { URI.create("ftp://testuser:testuser@docker.example.com:10021//home/testuser/" + DEST_FILENAME), destType, true, "FTP URI with auth should succeed for absolute file path" }); 
			testCases.add(new Object[] { URI.create("ftp://testuser:testuser@docker.example.com:10021/" + DEST_DIRNAME), destType, true, "FTP URI with auth should succeed for relative directory path" });
			testCases.add(new Object[] { URI.create("ftp://testuser:testuser@docker.example.com:10021//home/testuser/" + DEST_DIRNAME), destType, true, "FTP URI with auth should succeed for absolute directory path" }); 
				
			testCases.add(new Object[] { URI.create("ftp://docker.example.com:10021/" + DEST_FILENAME), destType, true, "anonymous FTP URI should succeed for relative file path" });
			testCases.add(new Object[] { URI.create("ftp://docker.example.com:10021//home/testuser/" + DEST_FILENAME), destType, true, "anonymous FTP URI should succeed for absolute file path" });
			testCases.add(new Object[] { URI.create("ftp://docker.example.com:10021/" + DEST_DIRNAME), destType, true, "anonymous FTP URI should succeed for relative directory path" });
			testCases.add(new Object[] { URI.create("ftp://docker.example.com:10021//home/testuser/" + DEST_DIRNAME), destType, true, "anonymous FTP URI should succeed for absolute directory path" });
				 
			testCases.add(new Object[] { URI.create("ftp://testuser@docker.example.com:10080/" + DEST_FILENAME), destType, true, "FTP URI with missing password should fail" });
			testCases.add(new Object[] { URI.create("ftp://testuser:testotheruser@docker.example.com:10080/" + DEST_FILENAME), destType, true, "FTP URI with invalid password should fail" });
			testCases.add(new Object[] { URI.create("ftp://testuser:testuser@docker.example.com:10080/home/testotheruser/" + DEST_FILENAME), destType, true, "FTP URI with auth should fail on data not owned." });
		}
		
		return testCases.toArray(new Object[][]{});
	}
	
	@Test(dataProvider="ftpFileImportProvider", dependsOnMethods={"copyFileHttpImport"}, enabled=true)
	public void copyFileFtpImport(URI ftpUri, StorageProtocolType destClientType, boolean shouldThrowException, String message)
	throws Exception
	{
		RemoteDataClient srcClient = null;
		RemoteDataClient destClient = null;
		
		boolean actuallyThrewException = false;
		try 
		{
			srcClient = new RemoteDataClientFactory().getInstance(SYSTEM_USER, null, ftpUri);
			srcClient.authenticate();
			
			destClient = getRemoteDataClientFromSystemJson(destClientType);
			destClient.authenticate();
			
			srcClient.put(LOCAL_BINARY_FILE, "");
			Assert.assertTrue(srcClient.doesExist(SOURCE_FILENAME), 
					"Failed to copy test file to source system");
			
			TransferTask task = new TransferTask(
					ftpUri.toString(), 
					destClient.getUriForPath(DEST_FILENAME).toString(),
					SYSTEM_USER, null, null);
			
			TransferTaskDao.persist(task);
			
			URLCopy urlCopy = new URLCopy(srcClient, destClient);
			
			urlCopy.copy(ftpUri.getPath(), DEST_FILENAME, task);
			
			destClient.authenticate();
			Assert.assertTrue(destClient.doesExist(DEST_FILENAME), 
					"Failed to copy test file to dest system");
			
			TransferTask savedTask = TransferTaskDao.getById(task.getId());
			
			Assert.assertEquals(savedTask.getBytesTransferred(),
					destClient.length(DEST_FILENAME), 
					"Transfer task bytes transferred was not updated by status listener.");
			Assert.assertEquals(savedTask.getTotalSize(),
					destClient.length(DEST_FILENAME ),
					"Transfer task total bytes was not updated by status listener.");
			Assert.assertEquals(savedTask.getStatus(),
					TransferStatusType.COMPLETED,
					"Transfer task status was not updated by status listener.");
		}
		catch (Exception e) 
		{
    		actuallyThrewException = true;
        	if (!shouldThrowException) e.printStackTrace();
        }
		finally {
			try { destClient.delete(""); } catch (Exception e) {}
			try { destClient.delete(DEST_FILENAME); } catch (Exception e) {}
		}
		
		Assert.assertEquals(actuallyThrewException, shouldThrowException, message);
	}
	
	@DataProvider(name = "ftpDirectoryImportProvider")
	protected Object[][] ftpDirectoryImportProvider() throws RemoteDataException, RemoteCredentialException, PermissionException
	{
		List<Object[]> testCases = new ArrayList<Object[]>();
		for (StorageProtocolType destType: testDestTypes) 
		{	
			testCases.add(new Object[] { URI.create("ftp://testuser:testuser@docker.example.com:10021/" + DEST_DIRNAME), destType, true, "FTP URI with auth should succeed for relative directory path" });
			testCases.add(new Object[] { URI.create("ftp://testuser:testuser@docker.example.com:10021//home/testuser/" + DEST_DIRNAME), destType, true, "FTP URI with auth should succeed for absolute directory path" }); 
			testCases.add(new Object[] { URI.create("ftp://docker.example.com:10021/" + DEST_DIRNAME), destType, true, "anonymous FTP URI should succeed for relative directory path" });
			testCases.add(new Object[] { URI.create("ftp://docker.example.com:10021//home/testuser/" + DEST_DIRNAME), destType, true, "anonymous FTP URI should succeed for absolute directory path" });
		}
		
		return testCases.toArray(new Object[][]{});
	}
	
	@Test(dataProvider="ftpDirectoryImportProvider", dependsOnMethods={"copyFileFtpImport"}, enabled=true)
	public void copyDirectoryFtpImport(URI ftpUri, StorageProtocolType destClientType, boolean shouldThrowException, String message)
	throws Exception
	{
		RemoteDataClient srcClient = null;
		RemoteDataClient destClient = null;
		
		boolean actuallyThrewException = false;
		try 
		{
			srcClient = new RemoteDataClientFactory().getInstance(SYSTEM_USER, null, ftpUri);
			srcClient.authenticate();
			
			destClient = getRemoteDataClientFromSystemJson(destClientType);
			destClient.authenticate();
			
			srcClient.put(LOCAL_DIR, "");
			Assert.assertTrue(srcClient.doesExist(LOCAL_DIR), 
					"Failed to copy test file to source system");
			
			TransferTask task = new TransferTask(
					ftpUri.toString(), 
					destClient.getUriForPath(DEST_DIRNAME).toString(),
					SYSTEM_USER, null, null);
			
			TransferTaskDao.persist(task);
			
			URLCopy urlCopy = new URLCopy(srcClient, destClient);
			
			urlCopy.copy(ftpUri.getPath(), DEST_DIRNAME, task);
			
			destClient.authenticate();
			Assert.assertTrue(destClient.doesExist(DEST_DIRNAME), 
					"Failed to copy test file to dest system");
			
			TransferTask savedTask = TransferTaskDao.getById(task.getId());
			
			Assert.assertEquals(savedTask.getBytesTransferred(),
					destClient.length(DEST_DIRNAME), 
					"Transfer task bytes transferred was not updated by status listener.");
			Assert.assertEquals(savedTask.getTotalSize(),
					destClient.length(DEST_DIRNAME ),
					"Transfer task total bytes was not updated by status listener.");
			Assert.assertEquals(savedTask.getStatus(),
					TransferStatusType.COMPLETED,
					"Transfer task status was not updated by status listener.");
		}
		catch (Exception e) 
		{
    		actuallyThrewException = true;
        	if (!shouldThrowException) e.printStackTrace();
        }
		finally {
			try { destClient.delete(""); } catch (Exception e) {}
			try { destClient.delete(DEST_FILENAME); } catch (Exception e) {}
		}
		
		Assert.assertEquals(actuallyThrewException, shouldThrowException, message);
	}
	
	@DataProvider(name = "copyProvider", parallel=false)
	protected Object[][] copyProvider()
	{
		List<Object[]> testCases = new ArrayList<Object[]>();
		
		for(StorageProtocolType srcType: testSrcTypes) {
			for(StorageProtocolType destType: testDestTypes) {
//			StorageProtocolType destType = testClientTypes.get(0);
				testCases.add(new Object[] { 
						srcType, 
						destType, 
						"Copy from " + srcType + " to " + 
								destType + " should not fail",
						false});
			}
		}
			
		return testCases.toArray(new Object[][]{});
	}
	
	@Test(dataProvider="copyProvider", dependsOnMethods= {"copyDirectoryFtpImport"})
	public void copyFileAndMonitor(StorageProtocolType srcClientType, StorageProtocolType destClientType, String message, boolean shouldThrowException)
	throws Exception
	{
		RemoteDataClient srcClient = null;
		RemoteDataClient destClient = null;
		
		boolean actuallyThrewException = false;
		try 
		{
			srcClient = getRemoteDataClientFromSystemJson(srcClientType);
			srcClient.authenticate();
			
			destClient = getRemoteDataClientFromSystemJson(destClientType);
			destClient.authenticate();
			
			srcClient.put(LOCAL_BINARY_FILE, "");
			Assert.assertTrue(srcClient.doesExist(SOURCE_FILENAME), 
					"Failed to copy test file to source system");
			
			TransferTask task = new TransferTask(
					srcClient.getUriForPath(SOURCE_FILENAME).toString(), 
					destClient.getUriForPath(DEST_FILENAME).toString(),
					SYSTEM_USER, null, null);
			
			TransferTaskDao.persist(task);
			
			URLCopy urlCopy = new URLCopy(srcClient, destClient);
			
			urlCopy.copy(SOURCE_FILENAME, DEST_FILENAME, task);
			
			destClient.authenticate();
			Assert.assertTrue(destClient.doesExist(DEST_FILENAME), 
					"Failed to copy test file to dest system");
			
			TransferTask savedTask = TransferTaskDao.getById(task.getId());
			
			Assert.assertEquals(savedTask.getBytesTransferred(),
					destClient.length(DEST_FILENAME), 
					"Transfer task bytes transferred was not updated by status listener.");
			Assert.assertEquals(savedTask.getTotalSize(),
					destClient.length(DEST_FILENAME ),
					"Transfer task total bytes was not updated by status listener.");
			Assert.assertEquals(savedTask.getStatus(),
					TransferStatusType.COMPLETED,
					"Transfer task status was not updated by status listener.");
		}
		catch (Exception e) 
		{
    		actuallyThrewException = true;
        	if (!shouldThrowException) e.printStackTrace();
        }
		finally {
			try { srcClient.delete(""); } catch (Exception e) {}
			try { destClient.delete(""); } catch (Exception e) {}
			try { srcClient.delete(SOURCE_FILENAME); } catch (Exception e) {}
			try { destClient.delete(DEST_FILENAME); } catch (Exception e) {}
		}
		
		Assert.assertEquals(actuallyThrewException, shouldThrowException, message);
	}
	
	@Test(dataProvider="copyProvider", dependsOnMethods={"copyFileAndMonitor"}, enabled=true)
	public void copyDirectoryAndMonitor(StorageProtocolType srcClientType, StorageProtocolType destClientType, String message, boolean shouldThrowException)
	throws Exception
	{
		RemoteDataClient srcClient = null;
		RemoteDataClient destClient = null;
		
		boolean actuallyThrewException = false;
		try 
		{
			srcClient = getRemoteDataClientFromSystemJson(srcClientType);
			srcClient.authenticate();
			
			destClient = getRemoteDataClientFromSystemJson(destClientType);
			destClient.authenticate();
			
			srcClient.put(LOCAL_DIR, "");
			Assert.assertTrue(srcClient.doesExist(SOURCE_DIRNAME), 
					"Failed to copy test file to source system");
			
			TransferTask task = new TransferTask(srcClient.getUriForPath(SOURCE_DIRNAME).toString(), 
					destClient.getUriForPath(DEST_DIRNAME).toString(),
					SYSTEM_USER, null, null);
			TransferTaskDao.persist(task);
			
			URLCopy urlCopy = new URLCopy(srcClient, destClient);
			
			urlCopy.copy(SOURCE_DIRNAME, DEST_DIRNAME, task);
			
			destClient.authenticate();
			Assert.assertTrue(destClient.doesExist(DEST_DIRNAME), 
					"Failed to copy test file to dest system");
			
			TransferTask savedTask = TransferTaskDao.getById(task.getId());
			long localSize = FileUtils.sizeOfDirectory(LOCAL_DIR);
			Assert.assertEquals(savedTask.getBytesTransferred(),
					localSize,
					"Transfer task bytes transferred was not updated by status listener.");
			Assert.assertEquals(savedTask.getTotalSize(),
					localSize,
					"Transfer task total bytes was not updated by status listener.");
			Assert.assertEquals(savedTask.getStatus(),
					TransferStatusType.COMPLETED,
					"Transfer task status was not updated by status listener.");
		}
		catch (Exception e) 
		{
    		actuallyThrewException = true;
        	if (!shouldThrowException) e.printStackTrace();
        }
		finally {
			try { srcClient.delete(""); } catch (Exception e) {}
			try { destClient.delete(""); } catch (Exception e) {}
			try { srcClient.delete(SOURCE_DIRNAME); } catch (Exception e) {}
			try { destClient.delete(DEST_DIRNAME); } catch (Exception e) {}
		}
		
		Assert.assertEquals(actuallyThrewException, shouldThrowException, message);
	}
}
