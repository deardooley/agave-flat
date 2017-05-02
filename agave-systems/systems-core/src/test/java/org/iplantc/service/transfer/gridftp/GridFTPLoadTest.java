package org.iplantc.service.transfer.gridftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.transfer.AbstractRemoteDataClientTest;
import org.iplantc.service.transfer.BaseTransferTestCase;
import org.iplantc.service.transfer.RemoteFileInfo;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups= {"gridftp","performance","broken", "integration"})
public class GridFTPLoadTest extends BaseTransferTestCase 
{
	private static final Logger log = Logger.getLogger(AbstractRemoteDataClientTest.class);
	
	@BeforeClass
    public void beforeClass() throws Exception {
    	super.beforeClass();
    	
    	JSONObject json = getSystemJson();
    	system = (StorageSystem)StorageSystem.fromJSON(json);
    	system.setOwner(SYSTEM_USER);
    	system.getStorageConfig().setHomeDir(system.getStorageConfig().getHomeDir() + "/agave-data-unittests5");
        storageConfig = system.getStorageConfig();
        salt = system.getSystemId() + storageConfig.getHost() + 
        		storageConfig.getDefaultAuthConfig().getUsername();
        
        new SystemDao().persist(system);
    }
    
    @AfterClass
    public void afterClass() throws Exception {
    	try 
    	{
    		FileUtils.deleteDirectory(LOCAL_DOWNLOAD_DIR);
    		clearSystems();
    	}
    	finally {
    		try { client.disconnect(); } catch (Exception e) {}
    	}   	
    }

    @BeforeMethod
    public void beforeMethod() throws Exception 
    {
    	try { FileUtils.deleteDirectory(LOCAL_DOWNLOAD_DIR); } catch (IOException e1) {}
        
    	try
    	{	
    		// auth client and ensure test directory is present
    		if (client == null) {
    			client = system.getRemoteDataClient();
    		}
    		client.authenticate();
    		if (client.doesExist("")) {
    			client.delete("");
    		}
    		
    		client.mkdirs("");
    		
    		if (!client.isDirectory("")) {
    			Assert.fail("System home directory " + client.resolvePath("") + " exists, but is not a directory.");
    		}
    	} 
    	catch (Exception e) {
    		Assert.fail("Failed to create home directory " + client.resolvePath("") + " before test method.", e);
    	}
    }

    protected JSONObject getSystemJson() throws JSONException, IOException {
		return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "gridftp-myproxy.example.com.json");
	}
    
    @Test
    public void testRecursiveStreamingGet() throws Exception 
    {
    	client.put(LOCAL_DIR, "");
    	
    	File localDir = new File(LOCAL_DOWNLOAD_DIR);
    	
    	streamingGet(FilenameUtils.getName(LOCAL_DIR), localDir);
    }
    
    private void streamingGet(String remotePath, File localFile) throws IOException, RemoteDataException {
    	
    	if (client.isFile(remotePath)) 
    	{
    		System.out.println("Streaming " + remotePath + " file from remote server.");
        	
    		InputStream in = null;
            BufferedOutputStream out = null;
            try
            {
                //remotePath = FilenameUtils.getName(remotePath);
                in = client.getInputStream(remotePath, true);
                out = new BufferedOutputStream(new FileOutputStream(localFile));

                int bufferSize = client.getMaxBufferSize();
                byte[] b = new byte[bufferSize];
                int len = 0;

                while ((len = in.read(b)) > -1) {
                	out.write(b, 0, len);
                }

                out.flush();
                in.close();
                out.close();
                
                Assert.assertTrue(client.doesExist(remotePath), 
                		"Data not found on remote system after writing via output stream.");
                
                Assert.assertTrue(client.isFile(remotePath), 
                		"Data found to be a directory on remote system after writing via output stream.");
            }
            catch (Exception e) {
                Assert.fail("Writing to output stream threw unexpected exception", e);
            }
            finally {
                try { in.close(); } catch (Exception e) {}
                try { out.close(); } catch (Exception e) {}
            }
    	}
    	else
    	{
    		System.out.println("Creating " + localFile.getPath() + " on the loal file system.");
        	
    		localFile.mkdirs();
    		
    		System.out.println("Listing contents of " + remotePath + " on the remote file system.");
        	
	    	for(RemoteFileInfo remoteFile: client.ls(remotePath)) 
	    	{	
	    		System.out.println("Found " + remotePath + "/" + remoteFile.getName() + " on the remote system.");
	        	
	    		streamingGet((remotePath + "/" + remoteFile.getName()) , new File(localFile, remoteFile.getName()));
	    	}
    	}
    }
    
    @Test(enabled=false)
    public void testRecursiveStreamingPut() throws Exception {
    	
    	File localDir = new File(LOCAL_DIR);
    	System.out.println("Creating parent directory");
    	streamingPut(localDir, FilenameUtils.getName(LOCAL_DIR));
    }
    
    private void streamingPut(File localFile, String remotePath) throws IOException, RemoteDataException {
    	
    	if (localFile.isFile()) 
    	{
    		OutputStream out = null;
            BufferedInputStream in = null;
            try
            {
                remotePath = localFile.getName();
                out = client.getOutputStream(remotePath, true, false);
                in = new BufferedInputStream(new FileInputStream(localFile));

                int bufferSize = client.getMaxBufferSize();
                byte[] b = new byte[bufferSize];
                int len = 0;

                while ((len = in.read(b)) > -1) {
                	out.write(b, 0, len);
                }

                out.flush();
                in.close();
                out.close();
                
                Assert.assertTrue(client.doesExist(remotePath), 
                		"Data not found on remote system after writing via output stream.");
                
                Assert.assertTrue(client.isFile(remotePath), 
                		"Data found to be a directory on remote system after writing via output stream.");
            }
            catch (Exception e) {
                Assert.fail("Writing to output stream threw unexpected exception", e);
            }
            finally {
                try { in.close(); } catch (Exception e) {}
                try { out.close(); } catch (Exception e) {}
            }
    	}
    	else
    	{
    		client.mkdirs(remotePath);
    		
	    	for(File file: localFile.listFiles()) 
	    	{	
	    		streamingPut(file, remotePath + "/" + file.getName());
	    	}
    	}
    }

	@Test(enabled=false)
	public void testRepeatedPutAndDeleteOfFileBug28() throws Exception {
		// generate a local scratch file
		String testFileName = "testRepeatedPutAndDeleteOfFileBug28andThisNameIsRealllllllllllllllllyLong.txt";
	
		int nbrIterations = 10;
	
		File localFile = new File(LOCAL_BINARY_FILE);
		
		for (int i = 0; i < nbrIterations; i++) {
			long startTime = System.currentTimeMillis();
			if (client.doesExist("")) {
				client.delete("");
			}
			client.mkdirs("");
			client.put(localFile.getAbsolutePath(), testFileName);
			client.put(LOCAL_DIR, "");
			System.out.println("Iteration[" + i + "]: " + (System.currentTimeMillis() - startTime));
		}
	}
}
