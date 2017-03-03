package org.iplantc.service.transfer.http;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.FileUtils;
import org.iplantc.service.transfer.BaseTransferTestCase;
import org.iplantc.service.transfer.RemoteDataClientFactory;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.sftp.MaverickSFTP;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Reduced test harness for http client tests.
 */

@Test(groups= {"http","filesystem"})
public class HTTPClientTest extends BaseTransferTestCase 
{
	private URI httpUri = null;
	private URI httpsUri = null;
	private URI httpPortUri = null;
	private URI httpsPortUri = null;
	private URI httpsQueryUri = null;
	private URI httpNoPathUri = null;
	private URI httpEmptyPathUri = null;
	private URI httpBasicUri = null;

	private URI fileNotFoundUri = null;
	private URI httpMissingPasswordBasicUri = null;
	private URI httpInvalidPasswordBasicUri = null;
	
    @BeforeClass
    protected void beforeClass() throws Exception 
    {
    	super.beforeClass();
    	httpUri = new URI("http://httpbin.org/stream-bytes/32768");
    	httpsUri = new URI("https://httpbin.agaveapi.co/stream-bytes/32768");
    	httpPortUri = new URI("http://docker.example.com:10080/public/test_upload.bin");
    	httpsPortUri = new URI("https://docker.example.com:10443/public/test_upload.bin");
    	httpsQueryUri = new URI("https://docker.example.com:10443/public/test_upload.bin?t=now");
    	httpNoPathUri = new URI("http://docker.example.com:10080");
    	httpEmptyPathUri = new URI("http://docker.example.com:10080/");
    	httpBasicUri = new URI("http://testuser:testuser@docker.example.com:10080/private/test_upload.bin");

    	fileNotFoundUri = new URI("http://docker.example.com:10080/" + MISSING_FILE);
    	httpMissingPasswordBasicUri = new URI("http://testuser@docker.example.com:10080/private/test_upload.bin");
    	httpInvalidPasswordBasicUri = new URI("http://testuser:testotheruser@docker.example.com:10080/private/test_upload.bin");
    }
    
    @AfterClass
    protected void afterClass() throws Exception 
    {
    	FileUtils.deleteDirectory(LOCAL_DOWNLOAD_DIR);
    }
    
    @BeforeMethod
    protected void beforeMethod() throws Exception 
    {
    	FileUtils.deleteDirectory(LOCAL_DOWNLOAD_DIR);
    }
    
    @AfterMethod
    protected void afterMethod() 
    {
    	try {
    		client.disconnect();
    	} catch (Exception e) {}
    }
    
    @Test
	public void isPermissionMirroringRequired() throws Exception
	{
		client = new RemoteDataClientFactory().getInstance(SYSTEM_USER, null, httpUri);
		
		Assert.assertFalse(client.isPermissionMirroringRequired(), 
				"HTTP permission mirroring should not be enabled.");
	}
    
    @DataProvider(name="getInputStreamProvider")
    public Object[][] getInputStreamProvider()
    {
    	String downloadFile = new File(LOCAL_DOWNLOAD_DIR, FilenameUtils.getName(LOCAL_BINARY_FILE)).getPath();
        return new Object[][] {
                { downloadFile, fileNotFoundUri, true, "get on 494 should throw exception." },
                { downloadFile, httpMissingPasswordBasicUri, true, "getInputStream on url with missing password file should throw exception." },
                { downloadFile, httpInvalidPasswordBasicUri, true, "getInputStream on url with bad password file should throw exception." },
                { downloadFile, httpNoPathUri, true, "getInputStream on dev https url with custom port and no path should throw an exception." },
                { downloadFile, httpEmptyPathUri, true, "getInputStream on dev https url with custom port and empty path should throw an exception." },
                
                { downloadFile, httpUri, false, "getInputStream on non dev http url should not throw an exception." },
                { downloadFile, httpsUri, false, "getInputStream on non dev https url should not throw an exception." },
                { downloadFile, httpPortUri, false, "getInputStream on dev http url with custom port should not throw an exception." },
                { downloadFile, httpsPortUri, false, "getInputStream on dev https url with custom port should not throw an exception." },
                { downloadFile, httpsQueryUri, false, "getInputStream on dev https url with custom port and query parameters should not throw an exception." },
                { downloadFile, httpBasicUri, false, "getInputStream on non test http url with custom port and basic auth should not throw an exception." },
        };
    }

    @Test(dataProvider="getInputStreamProvider", dependsOnMethods = { "isPermissionMirroringRequired" })
    public void getInputStream(String localFile, URI uri, boolean shouldThrowException, String message)
    throws IOException, RemoteDataException
    {
    	boolean actuallyThrewException = false;
        InputStream in = null;
        BufferedOutputStream bout = null;
        try
        {
        	client = new RemoteDataClientFactory().getInstance(SYSTEM_USER, null, uri);
        	
        	in = client.getInputStream(uri.getPath(), true);
            
        	Assert.assertEquals(false, shouldThrowException, "Opening an input stream should have thrown an exception for " + uri.toString());
        	
        	File downloadfile = new File(localFile);
            if (!FileUtils.fileExists(downloadfile.getParent())) {
                FileUtils.getFile(downloadfile.getParent()).mkdirs();
            }
            
            bout = new BufferedOutputStream(new FileOutputStream(downloadfile));

            int bufferSize = client.getMaxBufferSize();
            byte[] b = new byte[bufferSize];
            int len = 0;

            while ((len = in.read(b)) > -1) {
                bout.write(b, 0, len);
            }

            bout.flush();
            
            Assert.assertTrue(downloadfile.exists(), "Data not found on local system after streaming download.");
            Assert.assertTrue(downloadfile.length() > 0, "Download file is empty.");
        }
        catch (Exception e) {
            actuallyThrewException = true;
            if (!shouldThrowException) e.printStackTrace();
        }
        finally {
        	try { in.close(); } catch (Exception e) {}
            try { bout.close(); } catch (Exception e) {}
        }

        Assert.assertEquals(actuallyThrewException, shouldThrowException, message);
    }
    
    @Test(dependsOnMethods = { "getInputStream" })
    public void getInputStreamThrowsExceptionWhenNoPermission()
    {
    	try 
    	{
    		client = new RemoteDataClientFactory().getInstance(SYSTEM_USER, null, httpInvalidPasswordBasicUri);
        	
        	client.getInputStream(httpInvalidPasswordBasicUri.getPath(), true);
    		Assert.fail("getInputStream should throw RemoteDataException on no permissions");
    	} 
    	catch (RemoteDataException e) {
    		Assert.assertTrue(true);
        }
    	catch (Exception e) {
    		Assert.fail("getInputStream should throw RemoteDataException on no permissions");
    	}
    }
    
	@DataProvider(name="getFileRetrievesToCorrectLocationProvider")
    public Object[][] getFileRetrievesToCorrectLocationProvider()
    {
    	return new Object[][] {
    		{ LOCAL_DOWNLOAD_DIR, LOCAL_DOWNLOAD_DIR + "/" + FilenameUtils.getName(LOCAL_BINARY_FILE), "Downloading to existing path creates new file in path." },
    		{ LOCAL_DOWNLOAD_DIR + "/" + FilenameUtils.getName(LOCAL_BINARY_FILE), LOCAL_DOWNLOAD_DIR + "/" + FilenameUtils.getName(LOCAL_BINARY_FILE), "Downloading to explicit file path where no file exists creates the file." },
    	};
    }
    
    @Test(dataProvider="getFileRetrievesToCorrectLocationProvider", dependsOnMethods = { "getInputStreamThrowsExceptionWhenNoPermission" })
	public void getFileRetrievesToCorrectLocation(String localPath, String expectedDownloadPath, String message) 
	{
    	try 
    	{
    		FileUtils.getFile(LOCAL_DOWNLOAD_DIR).mkdirs();
        	
    		client = new RemoteDataClientFactory().getInstance(SYSTEM_USER, null, httpPortUri);
    		client.get(httpPortUri.getPath(), localPath);
    		
    		Assert.assertTrue(FileUtils.getFile(expectedDownloadPath).exists(), message);   		
    	} 
    	catch (Exception e) {
        	Assert.fail("get should not throw unexpected exception", e);
        }
	}
    
    @Test(dependsOnMethods={"getFileRetrievesToCorrectLocation"})
    public void getFileOverwritesExistingFile() 
	{
    	try 
    	{
    		File downloadDir = FileUtils.getFile(LOCAL_DOWNLOAD_DIR);
    		
    		Assert.assertTrue(downloadDir.mkdirs(), "Failed to create download directory");
    		
    		// copy the file so it's present to be overwritten without endangering our test data
    		FileUtils.copyFileToDirectory(LOCAL_BINARY_FILE, LOCAL_DOWNLOAD_DIR);
    		File downloadFile = new File(downloadDir, FilenameUtils.getName(LOCAL_BINARY_FILE));
    		
    		long originalSize = downloadFile.length();
    		
    		client = new RemoteDataClientFactory().getInstance(SYSTEM_USER, null, httpPortUri);
    		client.get(httpPortUri.getPath(), downloadFile.getAbsolutePath());
    		
    		Assert.assertTrue(downloadFile.exists(), "File disappeared after download.");  
    		
    		Assert.assertNotEquals(downloadFile.length(), originalSize, 
    				"Local file should be a different size than it was before.");
    		
    		Assert.assertEquals(downloadFile.length(), client.length(httpPortUri.getPath()), 
    				"Downloaded size is not the same as the original length.");
    		
    		
    		
    	} 
    	catch (Exception e) {
        	Assert.fail("Overwriting local file on get should not throw unexpected exception", e);
        }
	}
    
    @Test(dependsOnMethods = { "getFileOverwritesExistingFile" })
	public void getThrowsExceptionWhenDownloadingFileToNonExistentLocalPath() 
	{
    	try 
    	{
    		FileUtils.getFile(LOCAL_DOWNLOAD_DIR).delete();
        	
    		client = new RemoteDataClientFactory().getInstance(SYSTEM_USER, null, httpPortUri);
    		client.get(httpPortUri.getPath(), MISSING_DIRECTORY);
    		Assert.fail("Getting remote file to a local directory that does not exist should throw FileNotFoundException.");
    	} 
    	catch (FileNotFoundException e) {
    		Assert.assertTrue(true);
    	}
    	catch (Exception e) {
    		Assert.fail("Getting remote folder to a local directory that does not exist should throw FileNotFoundException.", e);
    	}
	}
    
    @Test(dependsOnMethods = { "getThrowsExceptionWhenDownloadingFileToNonExistentLocalPath" })
	public void getDownloadedContentMatchesOriginalContent() 
	{
    	MaverickSFTP sftp = null;
    	InputStream originalIn = null, downloadIn = null;
    	String remotePath = "/var/www/html/public/uploadedFile.bin";
    	File downloadDir = FileUtils.getFile(LOCAL_DOWNLOAD_DIR);
		
    	try 
    	{
    		URI knownFileContentDownload = new URI("http://docker.example.com:10080/public/uploadedFile.bin");
    		
    		Assert.assertTrue(downloadDir.mkdirs(), "Failed to create download directory");
    		
    		File downloadFile = new File(downloadDir, FilenameUtils.getName(LOCAL_BINARY_FILE));
    		File originalFile = new File(LOCAL_BINARY_FILE);
    		
    		sftp = new MaverickSFTP("docker.example.com", 10077, 
        			"testuser", "testuser","/","/");
    		
    		sftp.authenticate();
    		sftp.put(LOCAL_BINARY_FILE, remotePath);
    		
    		Assert.assertEquals(originalFile.length(), sftp.length(remotePath), 
    				"Local file size is not the same as the uploaded file length.");
    		
    		
    		client = new RemoteDataClientFactory().getInstance(SYSTEM_USER, null, knownFileContentDownload);
    		
    		client.get(knownFileContentDownload.getPath(), downloadFile.getAbsolutePath());
    		
    		Assert.assertEquals(downloadFile.length(), client.length(knownFileContentDownload.getPath()), 
    				"Local file size is not the same as the http client reported length.");
    		
    		Assert.assertEquals(originalFile.length(), downloadFile.length(), 
    				"Local file size is not the same as the original file length.");
    		
    		originalIn = new FileInputStream(originalFile);
    		downloadIn = new FileInputStream(downloadFile);
    		
    		Assert.assertTrue(IOUtils.contentEquals(originalIn, downloadIn), 
    				"File contents were not the same after download as before.");
    		
    	} 
    	catch (Exception e) {
    		Assert.fail("Fetching known file should not throw exception.", e);
    	}
    	finally {
    		try { downloadIn.close(); } catch (Exception e) {}
    		try { originalIn.close(); } catch (Exception e) {}
    		try { sftp.delete(remotePath); } catch (Exception e) {}
    		try { sftp.disconnect(); } catch (Exception e) {}
    	}
	}
    
    
}
