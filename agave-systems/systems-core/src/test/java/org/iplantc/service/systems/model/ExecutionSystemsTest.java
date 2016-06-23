package org.iplantc.service.systems.model;

import java.util.Arrays;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class ExecutionSystemsTest extends SystemsModelTestCommon{

    @BeforeClass
    public void beforeClass() throws Exception {
        super.beforeClass();
    }
    
    @BeforeMethod
	public void setUpMethod() throws Exception {
		jsonTree = jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE);
	}

    @DataProvider(name = "executionSystemId")
    public Object[][] executionSystemId() {
    	return new Object[][] {
    			{ "id", null, "resourceId cannot be null", true },
    			{ "id", "", "resourceId cannot be empty", true },
    			{ "id", new Object(), "resourceId cannot be object", true },
    			{ "id", Arrays.asList("Harry"), "resourceId cannot be array", true },
    			{ "id", "test name", "resourceId cannot contain spaces", true },
    			{ "id", "test~name", "resourceId cannot contain ~ characters", true },
    			{ "id", "test`name", "resourceId cannot contain ` characters", true },
    			{ "id", "test!name", "resourceId cannot contain ! characters", true },
    			{ "id", "test@name", "resourceId cannot contain @ characters", true },
    			{ "id", "test#name", "resourceId cannot contain # characters", true },
    			{ "id", "test$name", "resourceId cannot contain $ characters", true },
    			{ "id", "test%name", "resourceId cannot contain % characters", true },
    			{ "id", "test^name", "resourceId cannot contain ^ characters", true },
    			{ "id", "test&name", "resourceId cannot contain & characters", true },
    			{ "id", "test*name", "resourceId cannot contain * characters", true },
    			{ "id", "test(name", "resourceId cannot contain ( characters", true },
    			{ "id", "test)name", "resourceId cannot contain ) characters", true },
    			{ "id", "test_name", "resourceId cannot contain _ characters", true },
    			{ "id", "test+name", "resourceId cannot contain + characters", true },
    			{ "id", "test=name", "resourceId cannot contain = characters", true },
    			{ "id", "test{name", "resourceId cannot contain { characters", true },
    			{ "id", "test}name", "resourceId cannot contain } characters", true },
    			{ "id", "test|name", "resourceId cannot contain | characters", true },
    			{ "id", "test\\name", "resourceId cannot contain \\ characters", true },
    			{ "id", "test\nname", "resourceId cannot contain carrage return characters", true },
    			{ "id", "test\tname", "resourceId cannot contain tab characters", true },
    			{ "id", "test:name", "resourceId cannot contain : characters", true },
    			{ "id", "test;name", "resourceId cannot contain ; characters", true },
    			{ "id", "test'name", "resourceId cannot contain ' characters", true },
    			{ "id", "test\"name", "resourceId cannot contain \" characters", true },
    			{ "id", "test,name", "resourceId cannot contain , characters", true },
    			{ "id", "test?name", "resourceId cannot contain ? characters", true },
    			{ "id", "test/name", "resourceId cannot contain / characters", true },
    			{ "id", "test<name", "resourceId cannot contain < characters", true },
    			{ "id", "test>name", "resourceId cannot contain > characters", true },
    			{ "id", "test-name", "resourceId can contain dashes", false },
    			{ "id", "test.name", "resourceId cannot contain periods", false },
    			{ "id", "testname", "resourceId cannot contain all chars", false },
    			{ "id", "1234567890", "resourceId cannot contain all numbers", false },
    			{ "id", "test0name", "resourceId cannot contain alpha", false },
    			
    	};
    }

    @Test (groups={"model","system"}, dataProvider="executionSystemId")
    public void executionSystemIdValidationTest(String name, Object changeValue, String message, boolean exceptionThrown) throws Exception {
        super.commonExecutionSystemFromJSON(name,changeValue,message,exceptionThrown);
    }
    
    @DataProvider(name = "executionSystemName")
    public Object[][] executionSystemName() {
    	return new Object[][] {
    			{ "name", null, "Name cannot be null", true },
    			{ "name", "", "Name cannot be empty", true },
    			{ "name", new Object(), "Name cannot be object", true },
    			{ "name", Arrays.asList("Harry"), "Name cannot be array", true },
    			{ "name", "test name", "Name can be a valid string", false },
/*    			
//    			{ "owner", null, "owner cannot be null", true },
//    			{ "owner", "", "owner cannot be empty", true },
//    			{ "owner", new Object(), "owner cannot be object", true },
//    			{ "owner", Arrays.asList("Harry"), "owner cannot be array", true },
//    			{ "owner", "test name", "owner can be an unknown username", true },
//    			{ "owner", Settings.IRODS_USERNAME, "owner can be a valid string", false },
  */  			
    			{ "site", null, "site can be null", false },
    			{ "site", "", "site cannot be empty", true },
    			{ "site", new Object(), "site cannot be object", true },
    			{ "site", Arrays.asList("Harry"), "site cannot be array", true },
    			{ "site", "test site", "site can be a valid string", false },
    			
    			{ "type", null, "type cannot be null", true },
    			{ "type", "", "type cannot be empty", true },
    			{ "type", new Object(), "type cannot be object", true },
    			{ "type", Arrays.asList("Harry"), "type cannot be array", true },
    			{ "type", "type", "type can be an invalid system type", true },
    			{ "type", "STORAGE", "execution type cannot be STORAGE", true },
    			{ "type", "AUTHENTICATION", "execution type cannot be AUTHENTICATION", true },
    			{ "type", "EXECUTION", "type can be EXECUTION", false },
    			{ "type", "execution", "type is case insensitive", false },
    			
    			{ "scratchDir", null, "scratchDir can be null", false },
    			{ "scratchDir", "", "scratchDir can be empty", false },
    			{ "scratchDir", " ", "scratchDir can be space", false },
    			{ "scratchDir", new Object(), "scratchDir cannot be object", true },
    			{ "scratchDir", Arrays.asList("Harry"), "scratchDir cannot be array", true },
    			{ "scratchDir", "/scratch", "scratchDir can be a valid string", false },
    			
    			{ "workDir", null, "workDir can be null", false },
    			{ "workDir", "", "workDir can be empty", false },
    			{ "workDir", " ", "workDir can be space", false },
    			{ "workDir", new Object(), "workDir cannot be object", true },
    			{ "workDir", Arrays.asList("Harry"), "workDir cannot be array", true },
    			{ "workDir", "/work", "workDir can be a valid string", false },
    			
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
    			
    			{ "maxSystemJobs", null, "maxSystemJobs can be null", false },
    			{ "maxSystemJobs", new Integer(20), "maxSystemJobs can be a postitive integer", false },
    			{ "maxSystemJobs", new Integer(-20), "maxSystemJobs cannot be negative", true },
    			{ "maxSystemJobs", new Float(20.5), "maxSystemJobs cannot be a floating decimal number", true },
    			
    			{ "maxSystemJobsPerUser", null, "maxSystemJobsPerUser can be null", false },
    			{ "maxSystemJobsPerUser", new Integer(20), "maxSystemJobsPerUser can be a postitive integer", false },
    			{ "maxSystemJobsPerUser", new Integer(-20), "maxSystemJobsPerUser cannot be negative", true },
    			{ "maxSystemJobsPerUser", new Float(20.5), "maxSystemJobsPerUser cannot be a floating decimal number", true },
    			
    	};
    }

    @Test (groups={"model","system"}, dataProvider="executionSystemName")
    public void executionSystemNameValidationTest(String name, Object changeValue, String message, boolean exceptionThrown) throws Exception {
        super.commonExecutionSystemFromJSON(name,changeValue,message,exceptionThrown);
    }
    
    @DataProvider(name = "executionSystemAvailable")
    public Object[][] executionSystemAvailable() {
    	return new Object[][] { 
		    { "available", null, true, "Available is true if null", false },
			{ "available", Boolean.FALSE, false, "Available can be false", false },
			{ "available", Boolean.TRUE, true, "Available can be true", false },
		};
	}
    
    @Test (groups={"model","system"}, dataProvider="executionSystemAvailable")
    public void executionSystemAvailableTest(String name, Boolean changeValue, boolean expectedValue, String message, boolean exceptionThrown) 
    throws Exception 
    {
    	if (changeValue == null) {
    		jsonTree.remove("available");
    	} else {
    		jsonTree.put("available", changeValue.booleanValue());
    	}
		
		try 
		{
			ExecutionSystem system = ExecutionSystem.fromJSON(jsonTree);
			Assert.assertEquals(system.isAvailable(), expectedValue, message);
		}
		catch(Exception se){
			se.printStackTrace();
		}
    }
    
    @DataProvider(name = "executionSystemDefault")
    public Object[][] executionSystemDefault() {
    	return new Object[][] { 
		    { "default", null, false, "defaultSystem will be false if set to null in json" },
			{ "default", Boolean.FALSE, false, "defaultSystem will be false if set to false in json" },
			{ "default", Boolean.TRUE, false, "defaultSystem will be false if set to true in json" },
		};
	}
    
    @Test (groups={"model","system"}, dataProvider="executionSystemDefault")
    public void executionSystemDefaultTest(String name, Boolean changeValue, boolean expectedValue, String message) 
    throws Exception 
    {
    	if (changeValue == null) {
    		jsonTree.remove("default");
    	} else {
    		jsonTree.put("default", changeValue.booleanValue());
    	}
		
		try 
		{
			ExecutionSystem system = ExecutionSystem.fromJSON(jsonTree);
			Assert.assertEquals(system.isGlobalDefault(), expectedValue, message);
		}
		catch(Exception se){
			se.printStackTrace();
		}
    }

    @DataProvider(name = "executionSystemScratchDir")
    public Object[][] executionSystemScratchDir() {
    	return new Object[][] { 
    			{ "test name", "test\\ name", "scratchDir unescaped spaces should be escaped" },
    			{ "test\\ name", "test\\ name", "scratchDir unescaped spaces should be escaped" },
    			{ "test name test name", "test\\ name\\ test\\ name", "scratchDir unescaped spaces should be escaped" },
    			{ "test\\ name test\\ name", "test\\ name\\ test\\ name", "scratchDir unescaped spaces should be escaped" },
    	};
    }
    
    //@Test (groups={"model","system"}, dataProvider="executionSystemScratchDir")
    public void executionSystemScratchPathTest(Object changeValue, String expectedValue, String message) 
    throws Exception 
    {
    	jsonTree = updateTestData("scratchDir", changeValue);
		
		try 
		{
			ExecutionSystem system = ExecutionSystem.fromJSON(jsonTree);
			Assert.assertEquals(system.getScratchDir(), expectedValue, message);
		}
		catch(Exception se){
			System.out.println("Invalid iPlant execution host JSON submitted, attribute scratchDir " + message + " \n\"scratchDir\" = \"" + changeValue + "\"\n" + se.getMessage());
			se.printStackTrace();
		}
	}
    
    @DataProvider(name = "executionSystemWorkDirTest")
    public Object[][] executionSystemWorkDirTest() {
    	return new Object[][] { 
    			{ "test name", "test\\ name", "workDir unescaped spaces should be escaped" },
    			{ "test\\ name", "test\\ name", "workDir escaped spaces should not be escaped" },
    			{ "test name test name", "test\\ name\\ test\\ name", "workDir should be properly escaped" },
    			{ "test\\ name test\\ name", "test\\ name\\ test\\ name", "workDir should be properly escaped" },
    	};
    }
    
    //@Test (groups={"model","system"}, dataProvider="executionSystemWorkDirTest")
    public void executionSystemWorkPathTest(Object changeValue, String expectedValue, String message) 
    throws Exception 
    {
    	jsonTree = updateTestData("workDir", changeValue);
		
		try 
		{
			ExecutionSystem system = ExecutionSystem.fromJSON(jsonTree);
			Assert.assertEquals(system.getScratchDir(), expectedValue, message);
		}
		catch(Exception se){
			System.out.println("Invalid iPlant execution host JSON submitted, attribute workDir " + message + " \n\"workDir\" = \"" + changeValue + "\"\n" + se.getMessage());
			se.printStackTrace();
		}
	}
    
    @Test
    public void executionSystemLoginPasswordEncryptionTest()
    {
    	try 
		{
    		// get password from json
    		JSONObject authJson = jsonTree.getJSONObject("login").getJSONObject("auth");
	    	Assert.assertNotNull(authJson, "No auth config associated with this login config.");
	    	
	    	String originalPassword  = authJson.getString("password");
	    	Assert.assertNotNull(authJson, "No password associated with this auth config.");
	    	
	    	// get password from deserialized auth config
	    	ExecutionSystem system = ExecutionSystem.fromJSON(jsonTree);
	    	AuthConfig authConfig = system.getLoginConfig().getDefaultAuthConfig();
	    	Assert.assertNotNull(authConfig, "No login config associated with this system.");
	    	
	    	Assert.assertNotEquals(originalPassword, authConfig.getPassword(), "Login password was not encrypted during deserialization");
	    	
	    	String salt = system.getEncryptionKeyForAuthConfig(authConfig);
	    	String clearTextPassword = authConfig.getClearTextPassword(salt);
	    
	    	Assert.assertEquals(originalPassword, clearTextPassword, "Decrypted login password does not match original.");
		} 
    	catch (Exception e) {
			Assert.fail("Encryption test failed to match login passwords.", e);
		}
    }
    
    @Test
    public void executionSystemStoragePasswordEncryptionTest()
    {
    	try 
		{
    		// get password from json
    		JSONObject authJson = jsonTree.getJSONObject("storage").getJSONObject("auth");
	    	Assert.assertNotNull(authJson, "No auth config associated with this storage config.");
	    	
	    	String originalPassword  = authJson.getString("password");
	    	Assert.assertNotNull(authJson, "No password associated with this auth config.");
	    	
	    	// get password from deserialized auth config
	    	ExecutionSystem system = ExecutionSystem.fromJSON(jsonTree);
	    	AuthConfig authConfig = system.getLoginConfig().getDefaultAuthConfig();
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
    
    @Test
    public void executionUpdateSystemLoginPasswordEncryptionTest()
    {
    	try 
		{
    		// get password from json
    		JSONObject authJson = jsonTree.getJSONObject("login").getJSONObject("auth");
	    	Assert.assertNotNull(authJson, "No auth config associated with this login config.");
	    	
	    	String originalPassword  = authJson.getString("password");
	    	Assert.assertNotNull(authJson, "No password associated with this auth config.");
	    	
	    	// get password from deserialized auth config
	    	ExecutionSystem originalSystem = ExecutionSystem.fromJSON(jsonTree);
	    	ExecutionSystem system = ExecutionSystem.fromJSON(jsonTree, originalSystem);
	    	AuthConfig authConfig = system.getLoginConfig().getDefaultAuthConfig();
	    	Assert.assertNotNull(authConfig, "No login config associated with this system.");
	    	
	    	Assert.assertNotEquals(originalPassword, authConfig.getPassword(), "Login password was not encrypted during deserialization");
	    	
	    	String salt = system.getEncryptionKeyForAuthConfig(authConfig);
	    	String clearTextPassword = authConfig.getClearTextPassword(salt);
	    
	    	Assert.assertEquals(originalPassword, clearTextPassword, "Decrypted login password does not match original.");
		} 
    	catch (Exception e) {
			Assert.fail("Encryption test failed to match login passwords.", e);
		}
    }
}
