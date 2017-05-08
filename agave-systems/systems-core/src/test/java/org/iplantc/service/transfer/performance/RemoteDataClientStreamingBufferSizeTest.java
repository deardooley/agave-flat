package org.iplantc.service.transfer.performance;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.codehaus.plexus.util.FileUtils;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.transfer.BaseTransferTestCase;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.sftp.MaverickSFTPOutputStream;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

//import org.apache.commons.io.FileUtils;

/**
 * Runs streaming downloads using several buffer sizes to find the best one.
 */
@Test(groups= {"streaming","performance","integration"})
public class RemoteDataClientStreamingBufferSizeTest extends BaseTransferTestCase {

	protected static String LOCAL_BINARY_FILE = "src/test/resources/bufferdata.bin";

	private Map<String, RemoteDataClient> clientMap = new HashMap<String, RemoteDataClient>();
	
	public RemoteDataClientStreamingBufferSizeTest() {}

	@BeforeClass
	public void beforeClass() throws Exception
	{
		super.beforeClass();
		JSONObject json = null;
		String[] protocols = { "sftp", "gridftp", "irods" };
		
		for (String protocol: protocols) 
		{
			json = jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + protocol + ".example.com.json");
			json.remove("id");
			json.put("id", this.getClass().getSimpleName());
			
			StorageSystem system = (StorageSystem) StorageSystem.fromJSON(json);
			
			clientMap.put(protocol, system.getRemoteDataClient());
		}
	}

	@AfterClass
	public void afterClass() throws Exception
	{
		for (RemoteDataClient client: clientMap.values()) {
			try { 
				client.authenticate();
				client.delete(FilenameUtils.getName(LOCAL_BINARY_FILE));
				client.disconnect(); 
			} 
			catch (Exception e) {}
			finally {
				try {client.disconnect();} catch (Exception e) {}
			}
		}
		
		FileUtils.deleteDirectory(LOCAL_DOWNLOAD_DIR);
	}

	public Object[][] createInputStreamData(String protocol) 
	{
		RemoteDataClient client = clientMap.get(protocol);
		try { 
			client.authenticate();
			client.put(LOCAL_BINARY_FILE, "");
			Assert.assertTrue(client.doesExist(FilenameUtils.getName(LOCAL_BINARY_FILE)));
		} catch (Exception e) {
			Assert.fail("Failed to stage file to " + protocol + " server.", e);
		} finally {
			try {client.disconnect();} catch (Exception e) {}
		}
		
		return new Object[][] {
				{ protocol, clientMap.get(protocol), (int)Math.pow(2, 10) },
//				{ protocol, clientMap.get(protocol), (int)Math.pow(2, 12) },
//				{ protocol, clientMap.get(protocol), (int)Math.pow(2, 14) },
//				{ protocol, clientMap.get(protocol), (int)Math.pow(2, 15) },
				{ protocol, clientMap.get(protocol), (int)Math.pow(2, 16) },
//				{ protocol, clientMap.get(protocol), (int)Math.pow(2, 17) },
//				{ protocol, clientMap.get(protocol), (int)Math.pow(2, 18) },
//				{ protocol, clientMap.get(protocol), (int)Math.pow(2, 19) },
				{ protocol, clientMap.get(protocol), (int)Math.pow(2, 20) },
		};
	}
	
	@DataProvider(name = "sftpInputStreamProvider")
	public Object[][] sftpInputStreamProvider()
	{
		return createInputStreamData("sftp");
	}
	
//	@Test(dataProvider = "sftpInputStreamProvider")//, dependsOnMethods={"gridftpInputStreamTest"})
	public void sftpInputStreamTest(String protocol, RemoteDataClient client, int bufferSize) throws IOException, RemoteDataException
	{
		inputStreamTest(protocol, client, bufferSize);
	}
	
	@DataProvider(name = "gridftpInputStreamProvider")
	public Object[][] gridftpInputStreamProvider()
	{
		return createInputStreamData("gridftp");
	}
	
	//@Test(dataProvider = "gridftpInputStreamProvider")
	public void gridftpInputStreamTest(String protocol, RemoteDataClient client, int bufferSize) throws IOException, RemoteDataException
	{
		inputStreamTest(protocol, client, bufferSize);
	}
	
	@DataProvider(name = "irodsInputStreamProvider")
	public Object[][] irodsInputStreamProvider()
	{
		return createInputStreamData("irods");
	}

	@Test(dataProvider = "irodsInputStreamProvider")//, dependsOnMethods={"sftpInputStreamTest"})
	public void irodsInputStreamTest(String protocol, RemoteDataClient client, int bufferSize) throws IOException, RemoteDataException
	{
		inputStreamTest(protocol, client, bufferSize);
	}
	
	@SuppressWarnings("unused")
	public void inputStreamTest(String protocol, RemoteDataClient client, int bufferSize) throws IOException, RemoteDataException
	{
		InputStream in = null;
		BufferedOutputStream bout = null;
		
		try
		{
			client.authenticate();
			//System.out.println("Buffer size " + bufferSize + "");
			in = client.getInputStream(FilenameUtils.getName(LOCAL_BINARY_FILE), true);
			
			File downloadfile = new File(LOCAL_DOWNLOAD_DIR, "buffer_test.out");
			if (!downloadfile.getParentFile().exists()) {
				downloadfile.getParentFile().mkdirs();
            }
			
			downloadfile.createNewFile();
			bout = new BufferedOutputStream(new FileOutputStream(downloadfile));
			long fileSize = FileUtils.getFile(LOCAL_BINARY_FILE).length();
			
			byte[] b = new byte[bufferSize];
			int len = 0;
			long total = 0;
			long lastLen = 0;
			long lastTime = System.currentTimeMillis();
			long startTime = System.currentTimeMillis();
			double maxSpeed = 0, minSpeed = 0;
			while ( ( len = in.read(b) ) > -1)
			{
				bout.write(b, 0, len);
				total += len;
				long time = System.currentTimeMillis() - lastTime;
				lastTime = System.currentTimeMillis();
				double speed = (len > 0 ? (double)len / (((double)time)/1000.0) : 0);
				
				maxSpeed = Math.max(maxSpeed, speed);
				minSpeed = Math.min(minSpeed, speed);

				lastLen = len;
			}
			
			long totalTime = System.currentTimeMillis() - startTime;
			double avgSpeed = (double)total / (((double)totalTime)/1000.0);
			System.out.println(protocol + "\t" + total + "\t" + bufferSize + "\t" + totalTime + "\t" + avgSpeed + "\t" + minSpeed + "\t" + maxSpeed);
			
		}
		catch (Throwable e)
		{
//			if (!protocol.equals("sftp") && bufferSize > 32000) {
//				Assert.fail("Failed to transfer file via " + protocol + " with buffer size " + bufferSize, e);
//			}
			System.out.println("Failed to transfer file via " + protocol + " with buffer size " + bufferSize);
		} finally {
			try { bout.flush(); } catch (Exception e) {}
			try { in.close(); } catch (Exception e) {}
			try { bout.close(); } catch (Exception e) {}
			try { client.disconnect(); } catch (Exception e) {}
		}
	}

	public MaverickSFTPOutputStream getOutputStream(String path, boolean passive,
			boolean append) throws IOException, RemoteDataException
	{
		return null;
	}

}
