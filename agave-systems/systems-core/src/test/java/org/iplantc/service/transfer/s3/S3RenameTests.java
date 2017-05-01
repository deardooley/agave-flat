package org.iplantc.service.transfer.s3;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.systems.exceptions.EncryptionException;
import org.iplantc.service.systems.exceptions.RemoteCredentialException;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.transfer.BaseTransferTestCase;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import com.google.common.io.Files;

@Test(groups={"integration"})
public class S3RenameTests {
	@Factory
	public Object[] createInstances() {
		return new Object[]{
//			new RenameTest("alphaFilename"),
//			new RenameTest("hypenated-filename"),
			new RenameTest("versionedFilename-1"),
//			new RenameTest("hypenated-versionedfilename-1"),
		};
	}
	
	public class RenameTest extends BaseTransferTestCase {
	    private final Logger log = Logger.getLogger(S3RenameTests.RenameTest.class);
	    protected String baseFileItemName = "test";
	    protected String containerName;
	    private S3Jcloud client;
	    
	    public RenameTest(String baseFileItemName) {
	    	this.baseFileItemName = baseFileItemName;
	    }
	    
	    @BeforeClass
	    @Override
	    protected void beforeClass() throws Exception {
	    	super.beforeClass();
	    	
	        JSONObject json = getSystemJson();
	        StorageSystem system = (StorageSystem) StorageSystem.fromJSON(json);
	        storageConfig = system.getStorageConfig();
	        salt = system.getSystemId() + storageConfig.getHost()
	                + storageConfig.getDefaultAuthConfig().getUsername();
	        containerName = system.getStorageConfig().getContainerName();
	    }

	    @AfterClass
	    protected void afterClass() throws Exception {
	    	S3Jcloud client = null;
	        try {
	            client = (S3Jcloud)getRemoteDataClient();
	            client.authenticate();

	            if (client.getBlobStore().containerExists(containerName)) {
	            	client.getBlobStore().clearContainer(containerName);
	            }
	        } catch (RemoteDataException e) {
	            throw e;
	        } catch (Exception e) {
	            Assert.fail("Failed to clean up after test.", e);
	        }
	        finally {
	        	try {client.disconnect();} catch (Exception e) {}
	        }

	    }

	    /**
	     * Fetches the test system description for the protocol.
	     * 
	     * @return
	     * @throws JSONException
	     * @throws IOException
	     */
	    protected JSONObject getSystemJson() throws JSONException, IOException {
	        return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "s3.example.com.json");
	    }
	    
	    /**
	     * Creates a pre-authenticated {@link RemoteDataClient} for the test protocol. 
	     * @return
	     * @throws RemoteDataException
	     * @throws RemoteCredentialException
	     * @throws EncryptionException 
	     * @throws IOException 
	     */
	    protected S3Jcloud getRemoteDataClient() 
	    throws RemoteDataException, RemoteCredentialException, EncryptionException, IOException 
	    {
	    	if (client == null) {
		    	client = new S3Jcloud(storageConfig.getDefaultAuthConfig().getClearTextPublicKey(salt), 
		    						storageConfig.getDefaultAuthConfig().getClearTextPrivateKey(salt), 
		    						storageConfig.getRootDir(), 
		    						storageConfig.getHomeDir(), 
		    						storageConfig.getContainerName(), 
		    						storageConfig.getHost(), 
		    						storageConfig.getPort());
		    	
		    	client.authenticate();
	    	}
	    	
	    	return client;
	    }
	    
	    
	    /**
	     * Generates the prefix or suffixes to use in rename tests.
	     * @param placement
	     * @return
	     */
	    @DataProvider(name="renameProvider")
	    private Object[][] _renamePrefixOrSuffixProvider() {
	    	String[] filenameDelimiters = { ".", "-", "_" };
	    	String tokenToAppendAndPrepend = "foo";
	    	
	    	List<Object[]> testCases = new ArrayList<Object[]>();
	    	for (String delimiter: filenameDelimiters) {
	    		testCases.add(new Object[]{ baseFileItemName, baseFileItemName + delimiter, "Renaming with delimiter appended should not fail"});
	    		testCases.add(new Object[]{ baseFileItemName, baseFileItemName + delimiter + tokenToAppendAndPrepend, "Renaming with delimiter and token appended should not fail"});
	    		testCases.add(new Object[]{ baseFileItemName, delimiter + baseFileItemName, "Renaming with delimiter and token prepended should not fail"});
				testCases.add(new Object[]{ baseFileItemName, delimiter + tokenToAppendAndPrepend + baseFileItemName, "Renaming with delimiter and token prepended should not fail"});
				testCases.add(new Object[]{ baseFileItemName, delimiter + tokenToAppendAndPrepend + baseFileItemName + delimiter + tokenToAppendAndPrepend, "Renaming with delimiter and token appended and prepended should not fail"});
	    	}
	    		    	
	    	return testCases.toArray(new Object[][]{});
	    }
	    
//	    @DataProvider
//	    protected Object[][] testRenameWithSuffixProvider() {
//	    	return _renamePrefixOrSuffixProvider("testfileitem", "suffix");
//	    }
//	    
//	    @Test(dataProvider="testRenameWithSuffixProvider")
//	    public void testRenamedDirectoryWithSuffix(String originalFilename, String renamedFilename, String message) 
//	    throws IOException, RemoteDataException 
//	    {
//	    	_testRenamedDirectory(basename, prefix, message);
//	    }
//	    
//	    @Test(dataProvider="testRenameWithSuffixProvider")
//	    public void testRenamedFileWithSuffix(String originalFilename, String renamedFilename, String message) 
//	    throws IOException, RemoteDataException 
//	    {
//	    	_testRenamedFile(basename, prefix, message);
//	    }
//	    
//	    /**************************************************************
//	     * PREFIX TESTS
//	     **************************************************************/ 
//	    
//	    
//	    @DataProvider
//	    protected Object[][] testRenameWithPrefixProvider() {
//	    	return _renamePrefixOrSuffixProvider(baseFileItemName, "prefix");
//	    }  
	    
	    @Test(dataProvider="renameProvider")
	    public void testRenamedAlphaDirectoryNameWithPrefix(String basename, String prefix, String message) 
	    throws IOException, RemoteDataException 
	    {
	    	_testRenamedDirectory(basename, prefix, message);
	    }
	    
	    @Test(dataProvider="renameProvider")
	    public void testRenamedFileWithPrefix(String basename, String prefix, String message) 
	    throws IOException, RemoteDataException 
	    {
	    	_testRenamedFile(basename, prefix, message);
	    }
	    
	    /**
	     * Generates a test file with the given {@code basename} on the remote system
	     * and performs the rename test.
	     * 
	     * @param originalFilename
	     * @param renamedFilename
	     * @param message
	     * @throws IOException
	     * @throws RemoteDataException
	     */
	    protected void _testRenamedFile(String originalFilename, String renamedFilename, String message) 
	    throws IOException, RemoteDataException 
	    {
	    	S3Jcloud s3 = null;
	    	File tmpDir = null;
	    	File tmpFile = null;
	    	try {
	    		tmpDir = Files.createTempDir();
	    		tmpFile = new File(tmpDir, originalFilename);
		    	FileUtils.write(tmpFile, "some temp file data", "utf-8");
	    		s3 = getRemoteDataClient();
	    		s3.put(tmpFile.getAbsolutePath(), originalFilename);
		        s3.doRename(originalFilename, renamedFilename);
		        
		        Assert.assertFalse(s3.doesExist(originalFilename), "Original directory is not present on the remote system");
		        Assert.assertTrue(s3.doesExist(renamedFilename), "Renamed directory is not present on the remote system");
	    	}
	    	catch (Exception e) {
	    		Assert.fail(message, e);
	    	}
	    	finally {
	    		FileUtils.deleteQuietly(tmpDir);
	    		try { s3.getBlobStore().clearContainer(s3.containerName); } catch (Exception e){ Assert.fail("Failed to clean up after test", e); }
	    	}
	    }
	    
	    /**
	     * Generates a test folder with the given {@code basename} on the remote system
	     * and performs the rename test.
	     * 
	     * @param originalFilename
	     * @param renamedFilename
	     * @param message
	     * @throws IOException
	     * @throws RemoteDataException
	     */
	    protected void _testRenamedDirectory(String originalFilename, String renamedFilename, String message) 
	    throws IOException, RemoteDataException 
	    {
	    	S3Jcloud s3 = null;
	    	try {
	    		s3 = getRemoteDataClient();
	    		
		    	s3.mkdirs(originalFilename);
		        s3.doRename(originalFilename, renamedFilename);
		        
		        Assert.assertFalse(s3.doesExist(originalFilename), "Original directory is not present on the remote system");
		        Assert.assertTrue(s3.doesExist(renamedFilename), "Renamed directory is not present on the remote system");
	    	}
	    	catch (Exception e) {
	    		Assert.fail(message, e);
	    	}
	    	finally {
	    		try { s3.getBlobStore().clearContainer(s3.containerName); } catch (Exception e){ Assert.fail("Failed to clean up after test", e); }
	    	}
	    }
	}
}


