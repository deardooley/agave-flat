/**
 * 
 */
package org.iplantc.service.transfer.performance;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;

import org.apache.commons.io.FileUtils;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.SystemsModelTestCommon;
import org.iplantc.service.systems.model.enumerations.StorageProtocolType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.io.Files;

/**
 * @author dooley
 *
 */
@Test(groups={"integration"})
public abstract class AbstractPerformanceTest extends SystemsModelTestCommon {

	private SystemDao dao;
	private File sourceFile;
	private File downloadFile;
	private File tempDir;
	private File sourceTree;
	private long sourceFileChecksum = -1;
	private RemoteDataClient client;
	
	@BeforeClass
	protected void beforeClass() throws Exception
	{
		super.beforeClass();
		
		dao = new SystemDao();
		
		initTestData();
	}
	
	@AfterClass
	protected void afterClass() throws Exception
	{
		FileUtils.deleteQuietly(tempDir);
		
		clearSystems();
	}
	
	/**
	 * Creates the test data file to upload and download. 
	 * @throws IOException
	 */
	protected void initTestData() throws IOException
	{
		// create a temp file
		tempDir = Files.createTempDir();
		downloadFile = new File(tempDir, "download.dat");
		sourceFile = new File(tempDir, "upload.dat");
		
//		RandomDirectoryTree randomDirectoryTree = new RandomDirectoryTree(tempDir);
//		randomDirectoryTree.createFile(sourceFile, 1024 * 1024 * 1024);
//		
//		sourceFileChecksum = calculateChecksum(sourceFile);
//		
//		sourceTree = randomDirectoryTree.createTree(true);
		
		
		
//		RandomAccessFile raf = null;
//		try {
//			raf = new RandomAccessFile(sourceFile, "rw");
//			raf.setLength(1024 * 1024 * 1024);
//		}
//		finally {
//			raf.close();
//		}
		
		
//		java.util.Random rnd = new java.util.Random();
//
//		FileOutputStream out = new FileOutputStream(sourceFile);
//		byte[] buf = new byte[1024000];
//		for (int i = 0; i < 10; i++) {
//			rnd.nextBytes(buf);
//			out.write(buf);
//		}
//		out.close();

		System.out.println("Input file size: " + sourceFile.length());
	}
	
	private long calculateChecksum(File file) throws IOException {
		
		FileInputStream in = null;
		CheckedInputStream cis = null;

		try {
			in = new FileInputStream(file);
			cis = new CheckedInputStream(in, new Adler32());
			
			byte[] tempBuf = new byte[16384];
			while (cis.read(tempBuf) >= 0) {}
			
			return cis.getChecksum().getValue();
		} 
		catch (IOException e) {
			throw new IOException("Failed to read checksum of test data file", e);
		} 
		finally {
			try {cis.close();} catch (Exception e) {}
			try {in.close();} catch (Exception e) {}
		}
	}
	
	/**
	 * Creates test system from test system definitions and instantiates a 
	 * {@link RemoteDataClient} for use in subsequent tests.
	 * @param protocol
	 * @return
	 * @throws Exception
	 */
	protected RemoteDataClient createStorageSystem(StorageProtocolType protocol) throws Exception {
		String systemDefPath = STORAGE_SYSTEM_TEMPLATE_DIR + "/" + protocol.name().toLowerCase() + ".example.com.json";
		JSONObject json = jtd.getTestDataObject(systemDefPath);
		StorageSystem sftpSystem = StorageSystem.fromJSON(json);
		sftpSystem.setOwner(SYSTEM_OWNER);
		dao.persist(sftpSystem);
		return sftpSystem.getRemoteDataClient();
		
	}
	
	/**
	 * Creates a {@link RemoteDataClient} for the implementing class
	 * @throws Exception
	 */
	protected abstract RemoteDataClient getRemoteDataClient() throws Exception;
	
	
	
	@DataProvider
	public Object[][] uploadSpeedTestProvider() throws Exception
	{
		return new Object[][] {
				{ sourceFile, "/dev/null" },
				{ sourceFile, "" },
//				{ sourceTree, "/dev/null" },
//				{ sourceTree, "" }
		};
	}
	
	@Test(dataProvider="uploadSpeedTestProvider", enabled=false)
	public void uploadSpeedTest(File sourcePath, String remotePath) 
	{
		long t1 = 0, t2 = 0;
		long length = sourceFile.length();
		RemoteDataClient client = null;
		try
		{	
			client = getRemoteDataClient();
			client.authenticate();
			client.mkdirs("performance-test/test-files");
			t1 = System.currentTimeMillis();
			client.put(sourceFile.getAbsolutePath(), "performance-test/test-files");
		} 
		catch (Exception e) 
		{
			Assert.fail("Failed to uplaod file via " + client.getClass().getSimpleName(), e);
		}
		finally {
			t2 = System.currentTimeMillis();
			try { 
				client.delete("performance-test"); 
			} 
			catch (Exception e) {}
			finally {
				try {client.disconnect();} catch (Exception e) {}
			}
		}
		
		long e = t2 - t1;
		float kbs;
		if (e >= 1000) {
			kbs = (((float) length / 1024) / ((float) e / 1000) / 1000);
			System.out.println("Upload Transfered via " + client.getClass().getSimpleName() + " at "
					+ String.valueOf(kbs) + " MB/s");
		}

		client = null;
	}
	
	@DataProvider
	public Object[][] downloadSpeedTestProvider() throws Exception
	{
		return new Object[][] {
				//{ ftpClient },
//				{ sftpClient },
//				{ gridftpClient },
//				{ httpClient },
//				{ httpsClient },
//				{ irodsClient },
		};
	}
	
	@Test(dataProvider="downloadSpeedTestProvider", dependsOnMethods={"uploadSpeedTest"}, enabled=false)
	public void downloadSpeedTest(RemoteDataClient client) 
	{
		long t1 = 0, t2 = 0;
		long length = sourceFile.length();
		
		try
		{	
			String remoteFile = "performance-test/test-files/" + sourceFile.getName();
			client.authenticate();
			if (!client.doesExist(remoteFile)) {
				client.mkdirs("performance-test/test-files");
				client.put(sourceFile.getAbsolutePath(), "performance-test/test-files");
			}
			
			t1 = System.currentTimeMillis();
			client.get(remoteFile, downloadFile.getAbsolutePath());
			client.delete("performance-test");
		} 
		catch (Exception e) 
		{
			Assert.fail("Failed to download file via " + client.getClass().getSimpleName(), e);
		}
		finally {
			t2 = System.currentTimeMillis();
			try {client.disconnect();} catch (Exception e) {}
		}
		
		long e = t2 - t1;
		float kbs;
		if (e >= 1000) {
			kbs = (((float) length / 1024) / ((float) e / 1000) / 1000);
			System.out.println("Download Transfered via " + client.getClass().getSimpleName() + " at "
					+ String.valueOf(kbs) + " MB/s");
		}
	}
}
