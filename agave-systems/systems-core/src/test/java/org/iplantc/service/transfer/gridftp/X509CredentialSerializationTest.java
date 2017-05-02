package org.iplantc.service.transfer.gridftp;

import java.io.ByteArrayOutputStream;

import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.gridforum.jgss.ExtendedGSSCredential;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.AuthConfig;
import org.iplantc.service.systems.model.JSONTestDataUtil;
import org.iplantc.service.systems.model.StorageConfig;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.SystemsModelTestCommon;
import org.iplantc.service.transfer.RemoteDataClient;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups= {"gridftp","boutique","broken","integration"})
public class X509CredentialSerializationTest extends SystemsModelTestCommon 
{
	private SystemDao dao = new SystemDao();
	
	@BeforeClass
	public void beforeClass() throws Exception {
    	super.beforeClass();
    	
    	jsonTree = jtd.getTestDataObject(JSONTestDataUtil.TEST_SYSTEM_FOLDER + 
    				"storage/gridftp.example.com.json");
    	
    	StorageConfig myProxyStorageConfig = StorageConfig.fromJSON(jsonTree.getJSONObject("storage"));
		AuthConfig myproxyAuthConfig = myProxyStorageConfig.getDefaultAuthConfig();
		String myproxySalt = jsonTree.getString("id") + 
				myProxyStorageConfig.getHost() + 
    			myproxyAuthConfig.getUsername();
		myproxyAuthConfig.encryptCurrentPassword(myproxySalt);
    	ExtendedGSSCredential proxy = (ExtendedGSSCredential)myproxyAuthConfig.retrieveCredential(myproxySalt);
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
		((GlobusGSSCredentialImpl)proxy).getX509Credential().save(out);
		String serializedCredential = new String(out.toByteArray());
    	
    	// build this into a 
    	jsonTree.getJSONObject("storage").getJSONObject("auth").remove("server");
    	jsonTree.getJSONObject("storage").getJSONObject("auth").remove("username");
    	jsonTree.getJSONObject("storage").getJSONObject("auth").remove("password");
    	jsonTree.getJSONObject("storage").getJSONObject("auth").remove("credential");
    	jsonTree.getJSONObject("storage").getJSONObject("auth").put("credential", serializedCredential);
    }
    
	@DataProvider(name="serializedCredentialTestProvider")
	private Object[][] serializedCredentialTestProvider() throws Exception
	{
		String serializedCredential = jsonTree.getJSONObject("storage").getJSONObject("auth").getString("credential");
		
		return new Object[][] {
				{ serializedCredential, "Originally retrieved credential does not work.", false },
				{ serializedCredential.replaceAll("\\n", "\\\\n"), "Double escaping new lines breaks things", true },
				{ serializedCredential.replaceAll("\\n", "\\\\r\\\\n"), "Windows new lines breaks things", true }
		};
	}
	
	@Test(dataProvider="serializedCredentialTestProvider")
	public void serializedCredentialTest(String serializedCredential, String errorMessage, boolean shouldThrowException)
	{
		StorageSystem system = null;
		try 
		{
			jsonTree.remove("id");
			jsonTree.put("id", this.getClass().getSimpleName());
			jsonTree.getJSONObject("storage").getJSONObject("auth").remove("credential");
			jsonTree.getJSONObject("storage").getJSONObject("auth").put("credential", serializedCredential);
			system = StorageSystem.fromJSON(jsonTree);
			system.setOwner(SYSTEM_OWNER);
			
			dao.persist(system);
			RemoteDataClient client = system.getRemoteDataClient();
			client.authenticate();
		} 
		catch (Throwable e) {
			if (!shouldThrowException) {
				Assert.fail(errorMessage, e);
			}
		}
		finally {
			try { dao.remove(system); } catch (Exception e) {}
		}
	}
}
