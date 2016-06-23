package org.iplantc.service.systems.model;

import java.util.Arrays;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class StorageSystemsTest extends SystemsModelTestCommon{

    @BeforeClass
    public void beforeClass() throws Exception {
        super.beforeClass();
    }
    
    @BeforeMethod
	public void setUpMethod() throws Exception {
		jsonTree = jtd.getTestDataObject(JSONTestDataUtil.TEST_STORAGE_SYSTEM_FILE);
	}

    @DataProvider(name = "storageSystemId")
    public Object[][] storageSystemId() {
    	return new Object[][] {
    			{ "id", null, "systemId cannot be null", true },
    			{ "id", "", "systemId cannot be empty", true },
    			{ "id", new Object(), "systemId cannot be object", true },
    			{ "id", Arrays.asList("Harry"), "systemId cannot be array", true },
    			{ "id", "test name", "systemId cannot contain spaces", true },
    			{ "id", "test~name", "systemId cannot contain ~ characters", true },
    			{ "id", "test`name", "systemId cannot contain ` characters", true },
    			{ "id", "test!name", "systemId cannot contain ! characters", true },
    			{ "id", "test@name", "systemId cannot contain @ characters", true },
    			{ "id", "test#name", "systemId cannot contain # characters", true },
    			{ "id", "test$name", "systemId cannot contain $ characters", true },
    			{ "id", "test%name", "systemId cannot contain % characters", true },
    			{ "id", "test^name", "systemId cannot contain ^ characters", true },
    			{ "id", "test&name", "systemId cannot contain & characters", true },
    			{ "id", "test*name", "systemId cannot contain * characters", true },
    			{ "id", "test(name", "systemId cannot contain ( characters", true },
    			{ "id", "test)name", "systemId cannot contain ) characters", true },
    			{ "id", "test_name", "systemId cannot contain _ characters", true },
    			{ "id", "test+name", "systemId cannot contain + characters", true },
    			{ "id", "test=name", "systemId cannot contain = characters", true },
    			{ "id", "test{name", "systemId cannot contain { characters", true },
    			{ "id", "test}name", "systemId cannot contain } characters", true },
    			{ "id", "test|name", "systemId cannot contain | characters", true },
    			{ "id", "test\\name", "systemId cannot contain \\ characters", true },
    			{ "id", "test\nname", "systemId cannot contain carrage return characters", true },
    			{ "id", "test\tname", "systemId cannot contain tab characters", true },
    			{ "id", "test:name", "systemId cannot contain : characters", true },
    			{ "id", "test;name", "systemId cannot contain ; characters", true },
    			{ "id", "test'name", "systemId cannot contain ' characters", true },
    			{ "id", "test\"name", "systemId cannot contain \" characters", true },
    			{ "id", "test,name", "systemId cannot contain , characters", true },
    			{ "id", "test?name", "systemId cannot contain ? characters", true },
    			{ "id", "test/name", "systemId cannot contain / characters", true },
    			{ "id", "test<name", "systemId cannot contain < characters", true },
    			{ "id", "test>name", "systemId cannot contain > characters", true },
    			{ "id", "test-name", "systemId can contain dashes", false },
    			{ "id", "test.name", "systemId cannot contain periods", false },
    			{ "id", "testname", "systemId cannot contain all chars", false },
    			{ "id", "1234567890", "systemId cannot contain all numbers", false },
    			{ "id", "test0name", "systemId cannot contain alpha", false },
    			
    	};
    }

    @Test (groups={"model","system"}, dataProvider="storageSystemId")
    public void storageSystemIdValidationTest(String name, Object changeValue, String message, boolean exceptionThrown) throws Exception {
        super.commonStorageSystemFromJSON(name,changeValue,message,exceptionThrown);
    }
    
    @DataProvider(name = "storageSystemName")
    public Object[][] storageSystemName() {
    	return new Object[][] {
    			{ "name", null, "Name cannot be null", true },
    			{ "name", "", "Name cannot be empty", true },
    			{ "name", new Object(), "Name cannot be object", true },
    			{ "name", Arrays.asList("Harry"), "Name cannot be array", true },
    			{ "name", "test name", "Name can be a valid string", false },

    			{ "site", null, "site can be null", false },
    			{ "site", "", "site cannot be empty", true },
    			{ "site", new Object(), "site cannot be object", true },
    			{ "site", Arrays.asList("Harry"), "site cannot be array", true },
    			{ "site", "test site", "site can be a valid string", false },
    			
    			{ "type", null, "type cannot be null", true },
    			{ "type", "", "type cannot be empty", true },
    			{ "type", new Object(), "type cannot be object", true },
    			{ "type", Arrays.asList("Harry"), "type cannot be array", true },
    			{ "type", "type", "type cannot be an invalid system type", true },
    			{ "type", "EXECUTION", "execution type cannot be EXECUTION", true },
    			{ "type", "AUTHENTICATION", "execution type cannot be AUTHENTICATION", true },
    			{ "type", "STORAGE", "type can be STORAGE", false },
    			{ "type", "storage", "type is case insensitive", false },
    			
//    			{ "default", "", "defaultSystem cannot be empty string", true },
//    			{ "default", "Harry", "defaultSystem cannot be string", true },
//    			{ "default", new Object(), "defaultSystem cannot be object", true },
//    			{ "default", Arrays.asList("Harry"), "defaultSystem cannot be array", true },
//    			{ "default", null, "defaultSystem can be null", false },
    			
    			{ "available", "", "Available cannot be empty string", true },
    			{ "available", "Harry", "Available cannot be string", true },
    			{ "available", new Object(), "Available cannot be object", true },
    			{ "available", Arrays.asList("Harry"), "Available cannot be array", true },
    			{ "available", null, "Available can be null", false },
    			
/*
    			{ "protocol", null, "protocol cannot be null", true },
    			{ "protocol", "", "protocol cannot be empty", true },
    			{ "protocol", new Object(), "protocol cannot be object", true },
    			{ "protocol", Arrays.asList("Harry"), "protocol cannot be array", true },
    			{ "protocol", "protocol", "protocol cannot be an invalid system type", true },
    			{ "protocol", "GRIDFTP", "protocol can be GRIDFTP", false },
    			{ "protocol", "FTP", "protocol can be FTP", false },
    			{ "protocol", "SFTP", "protocol can be SFTP", false },
    			{ "protocol", "sftp", "protocol is case insensitive", false },
*/

    	};
    }

    @Test (groups={"model","system"}, dataProvider="storageSystemName")
    public void storageSystemNameValidationTest(String name, Object changeValue, String message, boolean exceptionThrown) throws Exception {
        super.commonStorageSystemFromJSON(name,changeValue,message,exceptionThrown);
    }
    
    @DataProvider(name = "storageSystemAvailable")
    public Object[][] storageSystemAvailable() {
    	return new Object[][] { 
		    { "available", null, true, "Available is true if null", false },
			{ "available", Boolean.FALSE, false, "Available can be false", false },
			{ "available", Boolean.TRUE, true, "Available can be true", false },
		};
	}
    
    @Test (groups={"model","system"}, dataProvider="storageSystemAvailable")
    public void storageSystemAvailableTest(String name, Boolean changeValue, boolean expectedValue, String message, boolean exceptionThrown) 
    throws Exception 
    {
    	if (changeValue == null) {
    		jsonTree.remove("available");
    	} else {
    		jsonTree.put("available", changeValue.booleanValue());
    	}
		
		try 
		{
			StorageSystem system = StorageSystem.fromJSON(jsonTree);
			Assert.assertEquals(system.isAvailable(), expectedValue, message);
		}
		catch(Exception se){
			se.printStackTrace();
		}
    }
    
    @DataProvider(name = "storageSystemDefault")
    public Object[][] storageSystemDefault() {
    	return new Object[][] { 
			{ "default", null, false, "defaultSystem will be false if set to null in json" },
			{ "default", Boolean.FALSE, false, "defaultSystem will be false if set to false in json" },
			{ "default", Boolean.TRUE, false, "defaultSystem will be false if set to true in json" },
		};
	}
    
   // @Test (groups={"model","system"}, dataProvider="storageSystemDefault")
    public void storageSystemDefaultTest(String name, Boolean changeValue, boolean expectedValue, String message) 
    throws Exception 
    {
    	if (changeValue == null) {
    		jsonTree.remove("default");
    	} else {
    		jsonTree.put("default", changeValue.booleanValue());
    	}
		
		try 
		{
			StorageSystem system = StorageSystem.fromJSON(jsonTree);
			Assert.assertEquals(system.isGlobalDefault(), expectedValue, message);
		}
		catch(Exception se){
			se.printStackTrace();
		}
    }
    
    @Test
    public void storageSystemStoragePasswordEncryptionTest()
    {
    	try 
		{
    		// get password from json
    		JSONObject authJson = jsonTree.getJSONObject("storage").getJSONObject("auth");
	    	Assert.assertNotNull(authJson, "No auth config associated with this storage config.");
	    	
	    	String originalPassword  = authJson.getString("password");
	    	Assert.assertNotNull(authJson, "No password associated with this auth config.");
	    	
	    	// get password from deserialized auth config
	    	StorageSystem system = StorageSystem.fromJSON(jsonTree);
	    	AuthConfig authConfig = system.getStorageConfig().getDefaultAuthConfig();
	    	Assert.assertNotNull(authConfig, "No storage config associated with this system.");
	    	
	    	Assert.assertNotEquals(originalPassword, authConfig.getPassword(), "Storage password was not encrypted during deserialization");
	    	
	    	String salt = system.getEncryptionKeyForAuthConfig(authConfig);
	    	String clearTextPassword = authConfig.getClearTextPassword(salt);
	    
	    	Assert.assertEquals(originalPassword, clearTextPassword, "Decrypted storage password does not match original.");
		} 
    	catch (Exception e) {
			Assert.fail("Encryption test failed to match storage passwords.", e);
		}
    }
}
