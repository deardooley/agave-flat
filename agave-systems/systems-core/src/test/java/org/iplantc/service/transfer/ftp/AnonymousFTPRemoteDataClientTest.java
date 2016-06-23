/**
 * 
 */
package org.iplantc.service.transfer.ftp;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.iplantc.service.transfer.AbstractRemoteDataClientTest;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author dooley
 *
 */
@Test(singleThreaded=true, groups= {"ftp","ftp-anonymous","filesystem"})
public class AnonymousFTPRemoteDataClientTest extends AbstractRemoteDataClientTest {

	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.AbstractRemoteDataClientTest#getSystemJson()
	 */
	@Override
	protected JSONObject getSystemJson() throws JSONException, IOException {
		return jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + "/" + "ftp-anonymous.example.com.json");
	}
	
	@Test(groups= {"proxy"})
    @Override
    public void isPermissionMirroringRequired()
    {
        Assert.assertFalse(getClient().isPermissionMirroringRequired(), 
                "FTP permission mirroring should not be enabled.");
    }
    
    @Test(groups= {"proxy"}, dependsOnMethods= {"isPermissionMirroringRequired"})
    @Override
    public void isThirdPartyTransferSupported()
    {
        Assert.assertFalse(getClient().isThirdPartyTransferSupported());
    }
	
	@DataProvider(name="anonymousAuthenticationTestProvider")
	public Object[][] anonymousAuthenticationTestProvider()
	{
		return new Object[][] {
				{ FTP.ANONYMOUS_USER, null, false, "Invalid anonymous credentials should be allowed" },
				{ FTP.ANONYMOUS_USER, "", false, "Invalid username credentials should throw exception" },
				{ FTP.ANONYMOUS_USER, "guest", false, "Invalid anonymous credentials should throw exception" },
				{ FTP.ANONYMOUS_USER, "info@example.com", false, "Invalid anonymous credentials should throw exception" },
				
		};
	}
	
	@Test(dataProvider="anonymousAuthenticationTestProvider")
    public void anonymousAuthenticationTest(String username, String password, boolean shouldThrowException, String message) 
    {
		FTP ftp = new FTP(system.getStorageConfig().getHost(), 
				system.getStorageConfig().getPort(), 
				username, 
				password, 
				system.getStorageConfig().getRootDir(),
				system.getStorageConfig().getHomeDir());
		
		try {
			ftp.authenticate();
			Assert.assertEquals(false, shouldThrowException, message);
		} catch (Exception e) {
			if (!shouldThrowException) {
				Assert.fail(message, e);
			}
		}
		finally {
		    try { ftp.disconnect(); } catch (Exception e) {}
		}
    }
	
	@Override
	@Test(enabled=false)
	public void authenticate() {}
	
	@Override
	@Test(enabled=false)
    public void _getInputStreamThrowsExceptionWhenNoPermission()
    {
		// permission checks don't mean much when we're chrooting to a virtual fs
    }
	
	@Override
	@Test(enabled=false)
    public void _lsThrowsExceptionWhenNoPermission()
    {
		// permission checks don't mean much when we're chrooting to a virtual fs
    }
	
	@Override
	@Test(enabled=false)
    public void _mkdirsWithoutRemotePermissionThrowsRemoteDataException() 
	{
	   // permission checks don't mean much when we're chrooting to a virtual fs
	}
    
	@Override
	@Test(enabled=false)
    public void _putFileWithoutRemotePermissionThrowsRemoteDataException() 
	{
		// permission checks don't mean much when we're chrooting to a virtual fs
	}
	
	@Override
	@Test(enabled=false)
    public void _putFolderWithoutRemotePermissionThrowsRemoteDataException() 
	{
    	// permission checks don't mean much when we're chrooting to a virtual fs
	}
    
	@Override
	@Test(enabled=false)
    public void _deleteThrowsExceptionWhenNoPermission()
    {
    	// permission checks don't mean much when we're chrooting to a virtual fs
    }
    
	@Override
	@Test(enabled=false)
    public void _doRenameThrowsRemotePermissionExceptionToRestrictedDest()
	{
    	// permission checks don't mean much when we're chrooting to a virtual fs
	}
    
    @Override
    @Test(enabled=false)
    public void _getOutputStreamThrowsExceptionWhenNoPermission()
    {
    	// permission checks don't mean much when we're chrooting to a virtual fs
    }
    
    @Override
    @Test(enabled=false)
    public void _copyThrowsRemoteDataExceptionToRestrictedDest()
	{
		// permission checks don't mean much when we're chrooting to a virtual fs
	}

	@Override
	@Test(enabled=false)
	protected String getForbiddenDirectoryPath(boolean shouldExist) {
		if (shouldExist) {
			return "/home/testotheruser";
		} else {
			return "/root/helloworld";
		}
	}
	
	@Override
	@Test(enabled=false)
    public void _copyThrowsRemoteDataExceptionToRestrictedSource()
    {
        try 
        {
            getClient().copy(MISSING_DIRECTORY, "foo");
               
            Assert.fail("copy a remote source path that does not exist should throw FileNotFoundException.");
        }
        catch (FileNotFoundException e) {
            Assert.assertTrue(true);
        }
        catch (Exception e) {
            Assert.fail("copy a remote source path that does not exist should throw FileNotFoundException.", e);
        }
    }
}
