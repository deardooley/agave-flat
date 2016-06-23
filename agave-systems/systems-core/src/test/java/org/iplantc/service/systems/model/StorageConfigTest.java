package org.iplantc.service.systems.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.iplantc.service.systems.model.enumerations.AuthConfigType;
import org.iplantc.service.systems.model.enumerations.StorageProtocolType;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class StorageConfigTest extends SystemsModelTestCommon
{
	@BeforeClass
    public void beforeClass() throws Exception {
        super.beforeClass();
    }
    
    @BeforeMethod
	public void setUpMethod() throws Exception {
		jsonTree = jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE)
				.getJSONObject("storage");
	}
    
    @DataProvider(name = "storageConfigName")
    public Object[][] storageConfigName() {
    	return new Object[][] {
    			{ "host", null, "host can be null", true },
    			{ "host", "", "host can be empty", true },
    			{ "host", new Object(), "host cannot be object", true },
    			{ "host", Arrays.asList("Harry"), "host cannot be array", true },
    			{ "host", "test name", "host can be a valid string", false },

    			{ "rootDir", null, "rootDir can be null", false },
    			{ "rootDir", "", "rootDir cannot be empty", true },
    			{ "rootDir", new Object(), "rootDir cannot be object", true },
    			{ "rootDir", Arrays.asList("Harry"), "rootDir cannot be array", true },
    			{ "rootDir", "user_home", "rootDir can be a valid string", false },
    			
    			{ "port", "", "port cannot be empty string", true },
    			{ "port", "Harry", "port cannot be string", true },
    			{ "port", new Object(), "port cannot be object", true },
    			{ "port", Arrays.asList("Harry"), "port cannot be array", true },
    			{ "port", new Float(22.0), "port is rounded when a floating point number", false },
    			{ "port", null, "port can be null", true },
    			{ "port", new Integer(-1), "port cannot be negative", true },
    			{ "port", new Integer(22), "port can be an integer", false },
    			
    			{ "protocol", null, "protocol cannot be null", true },
    			{ "protocol", "", "protocol cannot be empty", true },
    			{ "protocol", new Object(), "protocol cannot be object", true },
    			{ "protocol", Arrays.asList("Harry"), "protocol cannot be array", true },
    			{ "protocol", "protocol", "protocol cannot be an invalid system type", true },
    			{ "protocol", "SFTP", "protocol can be SFTP", false },
    			{ "protocol", "sftp", "protocol is case insensitive", false },
    			
    			{ "auth", null, "auth config cannot be null", true },
    			{ "auth", "", "auth config cannot be empty", true },
    			{ "auth", Arrays.asList("Harry"), "auth config cannot be array", true },
    	};
    }

    @Test (groups={"model","system"}, dataProvider="storageConfigName")
    public void storageConfigNameValidationTest(String name, Object changeValue, String message, boolean shouldExceptionBeThrown) throws Exception {
        boolean exceptionFlag = false;
        String exceptionMsg = message;

        try {
            jsonTree = updateTestData(name, changeValue);

            StorageConfig.fromJSON(jsonTree);
        }
        catch(Exception e){
            exceptionFlag = true;
            exceptionMsg = "Invalid json";
            if (!shouldExceptionBeThrown)
                e.printStackTrace();
        }

        System.out.println(" exception thrown?  expected " + shouldExceptionBeThrown + " actual " + exceptionFlag);

        Assert.assertTrue(exceptionFlag == shouldExceptionBeThrown, exceptionMsg);

    }
    
    @DataProvider(name = "storageConfigIrods")
    public Object[][] storageConfigIrods() 
    {
    	return new Object[][] {
    			{ "zone", null, "zone cannot be null for irods config", true },
    			{ "zone", "", "zone cannot be empty for irods config", true },
    			{ "zone", new Object(), "zone cannot be object for irods config", true },
    			{ "zone", Arrays.asList("Harry"), "zone cannot be array for irods config", true },
    			{ "zone", "test name", "zone can be a valid string for irods config", false },

    			{ "resource", null, "resource cannot be null for irods config", true },
    			{ "resource", "", "resource cannot be empty for irods config", true },
    			{ "resource", new Object(), "resource cannot be object for irods config", true },
    			{ "resource", Arrays.asList("Harry"), "resource cannot be array for irods config", true },
    			{ "resource", "test name", "resource can be a valid string for irods config", false },
    			
    			{ "rootDir", null, "rootDir can be null", false },
    			{ "rootDir", "", "rootDir cannot be empty", true },
    			{ "rootDir", new Object(), "rootDir cannot be object", true },
    			{ "rootDir", Arrays.asList("Harry"), "rootDir cannot be array", true },
    			{ "rootDir", "user_home", "rootDir can be a valid string", false },
    	};
    }
    
    @Test (groups={"model","system"}, dataProvider="storageConfigIrods")
    public void storageConfigIrodsValidationTest(String name, Object changeValue, String message, boolean shouldExceptionBeThrown)
    throws Exception 
    {
        boolean exceptionFlag = false;
        String exceptionMsg = message;

        try {
            jsonTree = jtd.getTestDataObject(JSONTestDataUtil.TEST_SYSTEM_FOLDER + "storage/irods.example.com.json")
                    .getJSONObject("storage");

            jsonTree = updateTestData(name, changeValue);

            StorageConfig.fromJSON(jsonTree);
        }
        catch(Exception e){
            exceptionFlag = true;
            exceptionMsg = "Invalid json";
            if (!shouldExceptionBeThrown)
                e.printStackTrace();
        }

        System.out.println(" exception thrown?  expected " + shouldExceptionBeThrown + " actual " + exceptionFlag);

        Assert.assertTrue(exceptionFlag == shouldExceptionBeThrown, exceptionMsg);
    }
    
    @DataProvider(name = "cloneProvider")
    public Object[][] cloneProvider() throws Exception
    {
    	AuthConfig authConfig = AuthConfig.IPLANT_IRODS_AUTH_CONFIG;
    	authConfig.setInternalUsername("bababooey");
    	
    	List<StorageConfig> storageConfigs = new ArrayList<StorageConfig>();
    	File executionDir = new File(STORAGE_SYSTEM_TEMPLATE_DIR);
		for(File jsonFile: executionDir.listFiles()) {
			jsonTree = jtd.getTestDataObject(jsonFile.getPath()).getJSONObject("storage");
			if (StringUtils.equalsIgnoreCase("x509", jsonTree.getJSONObject("auth").getString("type"))) {
			    continue;
//			    GSSCredential proxy = MyProxyClient.getCredential("docker.example.com", 7512, "testuser", "testuser", null);
//			    ByteArrayOutputStream out = new ByteArrayOutputStream();
//                ((GlobusGSSCredentialImpl)proxy).getX509Credential().save(out);
//                String serializedCredential = new String(out.toByteArray());
//                jsonTree.getJSONObject("auth").put("credential", serializedCredential);
			}
			
			StorageConfig storageConfig = StorageConfig.fromJSON(jsonTree);
			storageConfig.getAuthConfigs().add(authConfig); // will bypass validation check in RemoteConfig.addAuthConfigs()
			storageConfigs.add(storageConfig);
		}
    	
		Object[][] testData = new Object[storageConfigs.size()][];
		
		for(int i=0; i<storageConfigs.size(); i++)
		{
			testData[i] = new Object[] { storageConfigs.get(i), "Cloning StorageConfigs." + storageConfigs.get(i).getProtocol() + " StorageConfig does not copy auth info." };
		}
		return testData;
    }
    
    @Test (groups={"model","system"}, dataProvider="cloneProvider")
    public void cloneTest(StorageConfig config, String message) throws Exception 
    {
    	StorageConfig clonedConfig = config.clone();
    	
    	Assert.assertEquals(config.getHost(), clonedConfig.getHost(), "StorageConfig host value should have been cloned");
    	Assert.assertEquals(config.getPort(), clonedConfig.getPort(), "StorageConfig port value should have been cloned");
    	Assert.assertEquals(config.getProtocol(), clonedConfig.getProtocol(), "StorageConfig protocol value should have been cloned");
    	Assert.assertEquals(config.getRootDir(), clonedConfig.getRootDir(), "StorageConfig rootDir value should have been cloned");
    	Assert.assertEquals(config.getZone(), clonedConfig.getZone(), "StorageConfig zone value should have been cloned");
    	Assert.assertEquals(config.getResource(), clonedConfig.getResource(), "StorageConfig resource value should have been cloned");
    	
    	Assert.assertTrue(clonedConfig.getAuthConfigs().isEmpty(), "StorageConfig AuthConfigs were clone when they should not have been.");
    }
    
    @DataProvider(name = "addAuthConfigProvider")
    public Object[][] addAuthConfigProvider() throws Exception
    {
    	// not sure this is useful, but it is exhaustive. The actual test for StorageProtocolType.accepts is 
    	// done elsewhere in the AuthConfigTest class. 
    	Object[][] testData = new Object[StorageProtocolType.values().length * AuthConfigType.values().length][];
    	int i=0;
    	for (StorageProtocolType protocol: StorageProtocolType.values()) {
    		for (AuthConfigType authConfigType: AuthConfigType.values()) {
    			boolean shouldPass = protocol.accepts(authConfigType);
    			testData[i] = new Object[]{ protocol, authConfigType, "The combination of " + protocol + 
    					" and " + authConfigType + " should " + (shouldPass ? "not " : "") + "throw an exception", !shouldPass };
    			i++;
    		}
    	}
    	
    	return testData;
    }
    
    @Test (groups={"model","system"}, dataProvider="addAuthConfigProvider")
    public void addAuthConfigTest(StorageProtocolType protocol, AuthConfigType authConfigType, String message, boolean shouldThrowException) 
    throws Exception 
    {
    	boolean exceptionFlag = false;
		String exceptionMsg = message;
		
		try 
		{
			StorageConfig storageConfig = StorageConfig.fromJSON(jsonTree);
	    	storageConfig.setProtocol(protocol);
	    	
	    	AuthConfig authConfig = storageConfig.getDefaultAuthConfig();
	    	authConfig.setType(authConfigType);
	    	authConfig.setInternalUsername("bababooey");
	    	
	    	storageConfig.addAuthConfig(authConfig);
		}
		catch(Exception e){
			exceptionFlag = true;
			exceptionMsg = "Invalid storage config protocol (" + protocol + ") and auth config type " + authConfigType + ". " + message + "\n" + e.getMessage();
			if (!shouldThrowException) 
				e.printStackTrace();
		}

		System.out.println(" exception thrown?  expected " + shouldThrowException + " actual " + exceptionFlag);

		Assert.assertTrue(exceptionFlag == shouldThrowException, exceptionMsg);
    }
    
}
