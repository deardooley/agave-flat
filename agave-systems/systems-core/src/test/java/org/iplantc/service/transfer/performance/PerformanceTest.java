/**
 * 
 */
package org.iplantc.service.transfer.performance;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;

import org.apache.commons.io.FileUtils;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.SystemsModelTestCommon;
import org.iplantc.service.transfer.RemoteDataClient;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author dooley
 *
 */
@Test(groups= {"performance"})
public class PerformanceTest extends SystemsModelTestCommon {

	private SystemDao dao;
	static File sourceFile;
	static File downloadFile;
	static long sourceFileChecksum = -1;
	private RemoteDataClient ftpClient;
	private RemoteDataClient sftpClient;
	private RemoteDataClient gridftpClient;
	private RemoteDataClient httpClient;
	private RemoteDataClient httpsClient;
	private RemoteDataClient irodsClient;
	
	@BeforeClass
	public void beforeClass() throws Exception
	{
		super.beforeClass();
		
		dao = new SystemDao();
		
		createTestData();
		
		initDataClients();
	}
	
	@AfterClass
	public void afterClass() throws Exception
	{
		deleteTestData();
		
		clearSystems();
	}
	
	private void deleteTestData()
	{
		try { sourceFile.delete(); } catch (Throwable e) {}
		try { downloadFile.delete(); } catch (Throwable e) {}
	}
	
	private void createTestData() throws IOException
	{
		// create a temp file
		sourceFile = File.createTempFile("upload", "dat", FileUtils.getTempDirectory());
		downloadFile = new File(sourceFile.getParentFile(), "download.dat");
		
		java.util.Random rnd = new java.util.Random();

		FileOutputStream out = new FileOutputStream(sourceFile);
		byte[] buf = new byte[1024000];
		for (int i = 0; i < 10; i++) {
			rnd.nextBytes(buf);
			out.write(buf);
		}
		out.close();

		CheckedInputStream cis = new CheckedInputStream(new FileInputStream(
				sourceFile), new Adler32());

		try {
			byte[] tempBuf = new byte[16384];
			while (cis.read(tempBuf) >= 0) {
			}
			sourceFileChecksum = cis.getChecksum().getValue();
		} catch (IOException e) {
		} finally {
			cis.close();
		}
		
		System.out.println("Input file size: " + sourceFile.length());
	}
	
	private void initDataClients() throws Exception
	{
//		StorageSystem ftpSystem = StorageSystem.fromJSON(jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/ftp.example.com.json"));
//		ftpSystem.setOwner(SYSTEM_OWNER);
//		dao.persist(ftpSystem);
//		ftpClient = ftpSystem.getRemoteDataClient();
//		
//		StorageSystem sftpSystem = StorageSystem.fromJSON(jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/sftp.example.com.json"));
//		sftpSystem.setOwner(SYSTEM_OWNER);
//		dao.persist(sftpSystem);
//		sftpClient = sftpSystem.getRemoteDataClient();
//		
		JSONObject json = jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/ranch.tacc.utexas.edu.json");
		json.remove("id");
		json.put("id", this.getClass().getSimpleName());
		StorageSystem gridftpSystem = StorageSystem.fromJSON(json);
		gridftpSystem.setOwner(SYSTEM_OWNER);
		dao.persist(gridftpSystem);
		gridftpClient = gridftpSystem.getRemoteDataClient();
		
//		httpClient = new RemoteDataClientFactory().getInstance(SYSTEM_OWNER, null, new URI("http://dl.dropboxusercontent.com/s/775leb8f1mpswxv/agave.zip?dl=1&token_hash=AAENpUYeRGKlNXkOk-LJ0EILIKiZINhDHep9jPH1bLGFlw"));
//		
//		httpsClient = new RemoteDataClientFactory().getInstance(SYSTEM_OWNER, null, new URI("https://dl.dropboxusercontent.com/s/775leb8f1mpswxv/agave.zip?dl=1&token_hash=AAENpUYeRGKlNXkOk-LJ0EILIKiZINhDHep9jPH1bLGFlw"));
//		
//		StorageSystem irodsSystem = StorageSystem.fromJSON(jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/irods.example.com.json"));
//		irodsSystem.setOwner(SYSTEM_OWNER);
//		dao.persist(irodsSystem);
//		irodsClient = irodsSystem.getRemoteDataClient();
	}
	
	@DataProvider
	public Object[][] uploadSpeedTestProvider() throws Exception
	{
		return new Object[][] {
				//{ ftpClient },
//				{ sftpClient },
				{ gridftpClient },
//				{ httpClient },
//				{ httpsClient },
//				{ irodsClient },
		};
	}
	
	@Test(dataProvider="uploadSpeedTestProvider", enabled=false)
	public void uploadSpeedTest(RemoteDataClient client) 
	{
		long t1 = 0, t2 = 0;
		long length = sourceFile.length();
		
		try
		{	
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
			try {client.disconnect();} catch (Exception e) {}
		}
		
		long e = t2 - t1;
		float kbs;
		if (e >= 1000) {
			kbs = (((float) length / 1024) / ((float) e / 1000) / 1000);
			System.out.println("Upload Transfered via " + client.getClass().getSimpleName() + " at "
					+ String.valueOf(kbs) + " MB/s");
		}
	}
	
	@DataProvider
	public Object[][] downloadSpeedTestProvider() throws Exception
	{
		return new Object[][] {
				//{ ftpClient },
//				{ sftpClient },
				{ gridftpClient },
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
