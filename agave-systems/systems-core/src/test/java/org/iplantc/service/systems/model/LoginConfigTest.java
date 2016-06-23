package org.iplantc.service.systems.model;

import org.iplantc.service.systems.model.enumerations.AuthConfigType;
import org.iplantc.service.systems.model.enumerations.LoginProtocolType;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class LoginConfigTest extends SystemsModelTestCommon{

    @BeforeClass
    public void beforeClass() throws Exception {
        super.beforeClass();
    }
    
    @BeforeMethod
	public void setUpMethod() throws Exception {
		jsonTree = jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE)
				.getJSONObject("login");
	}
    
    @DataProvider(name = "loginConfigName")
    public Object[][] loginConfigName() {
    	return new Object[][] {
    			{ "host", null, "host can be null", true },
    			{ "host", "", "host can be empty", true },
    			{ "host", new Object(), "host cannot be object", true },
    			{ "host", Arrays.asList("Harry"), "host cannot be array", true },
    			{ "host", "test name", "host can be a valid string", false },

    			{ "port", "", "port cannot be empty string", true },
    			{ "port", "Harry", "port cannot be string", true },
    			{ "port", new Object(), "port cannot be object", true },
    			{ "port", Arrays.asList("Harry"), "port cannot be array", true },
    			{ "port", new Float(22.0), "port is rounded when a floating point number", false },
//    			{ "port", null, "port can be null", false },
    			{ "port", new Integer(-1), "port cannot be negative", true },
    			{ "port", new Integer(22), "port can be an integer", false },
    			
    			{ "protocol", null, "protocol cannot be null", true },
    			{ "protocol", "", "protocol cannot be empty", true },
    			{ "protocol", new Object(), "protocol cannot be object", true },
    			{ "protocol", Arrays.asList("Harry"), "protocol cannot be array", true },
    			{ "protocol", "protocol", "protocol cannot be an invalid system type", true },
    			{ "protocol", "SSH", "protocol can be SSH", false },
    			{ "protocol", "ssh", "protocol is case insensitive", false },
    			
    			{ "auth", null, "auth config cannot be null", true },
    			{ "auth", "", "auth config cannot be empty", true },
    			{ "auth", Arrays.asList("Harry"), "auth config cannot be array", true },
    	};
    }

    @Test (groups={"model","system"}, dataProvider="loginConfigName")
    public void loginConfigNameValidationTest(String name, Object changeValue, String message, boolean shouldExceptionBeThrown) throws Exception {
        boolean exceptionFlag = false;
        String exceptionMsg = message;

        try {
            super.commonLoginConfigFromJSON(name,changeValue,message,shouldExceptionBeThrown);
        }
        catch(Exception e){
            exceptionFlag = true;
            exceptionMsg = "Invalid json";
            if (!shouldExceptionBeThrown)
                Assert.fail(message, e);
        }

        System.out.println(" exception thrown?  expected " + shouldExceptionBeThrown + " actual " + exceptionFlag);

        Assert.assertTrue(exceptionFlag == shouldExceptionBeThrown, exceptionMsg);
    }
    
    @DataProvider(name = "cloneProvider")
    public Object[][] cloneProvider() throws Exception
    {
    	AuthConfig authConfig = AuthConfig.IPLANT_IRODS_AUTH_CONFIG;
    	authConfig.setInternalUsername("bababooey");
    	
    	List<LoginConfig> loginConfigs = new ArrayList<LoginConfig>();
    	File executionDir = new File(EXECUTION_SYSTEM_TEMPLATE_DIR);
		for(File jsonFile: executionDir.listFiles()) {
			System.out.println(jsonFile.getName());
			jsonTree = jtd.getTestDataObject(jsonFile.getPath()).getJSONObject("login");
			LoginConfig loginConfig = LoginConfig.fromJSON(jsonTree);
			loginConfig.getAuthConfigs().add(authConfig); // will bypass validation check in RemoteConfig.addAuthConfigs()
			loginConfigs.add(loginConfig);
		}
    	
		Object[][] testData = new Object[loginConfigs.size()][];
		
		for(int i=0; i<loginConfigs.size(); i++)
		{
			testData[i] = new Object[] { loginConfigs.get(i), "Cloning LoginConfigs." + loginConfigs.get(i).getProtocol() + " LoginConfig does not copy auth info." };
		}
		return testData;
    }
    
    @Test (groups={"model","system"}, dataProvider="cloneProvider")
    public void cloneTest(LoginConfig config, String message) throws Exception 
    {
    	LoginConfig clonedConfig = config.clone();
    	
    	Assert.assertEquals(config.getHost(), clonedConfig.getHost(), "LoginConfig host value should have been cloned");
    	Assert.assertEquals(config.getPort(), clonedConfig.getPort(), "LoginConfig port value should have been cloned");
    	Assert.assertEquals(config.getProtocol(), clonedConfig.getProtocol(), "LoginConfig protocol value should have been cloned");
    	
    	Assert.assertTrue(clonedConfig.getAuthConfigs().isEmpty(), "LoginConfig AuthConfigs were clone when they should not have been.");
    }
    
    @DataProvider(name = "addAuthConfigProvider")
    public Object[][] addAuthConfigProvider() throws Exception
    {
    	// not sure this is useful, but it is exhaustive. The actual test for LoginProtocolType.accepts is 
    	// done elsewhere in the AuthConfigTest class. 
    	Object[][] testData = new Object[LoginProtocolType.values().length * AuthConfigType.values().length][];
    	int i=0;
    	for (LoginProtocolType protocol: LoginProtocolType.values()) {
    		for (AuthConfigType authConfigType: AuthConfigType.values()) {
    			boolean shouldPass = protocol.accepts(authConfigType);
    			testData[i] = new Object[]{ protocol, authConfigType, "The combination of " + protocol + 
    					" and " + authConfigType + " should " + (shouldPass ? "" : "not ") + "throw an exception", !shouldPass };
    			i++;
    		}
    	}
    	
    	return testData;
    }
    
    @Test (groups={"model","system"}, dataProvider="addAuthConfigProvider")
    public void addAuthConfigTest(LoginProtocolType protocol, AuthConfigType authConfigType, String message, boolean shouldThrowException) 
    throws Exception 
    {
    	boolean exceptionFlag = false;
		String exceptionMsg = message;
		
		try 
		{
			LoginConfig loginConfig = LoginConfig.fromJSON(jsonTree);
	    	loginConfig.setProtocol(protocol);
	    	
	    	AuthConfig authConfig = loginConfig.getDefaultAuthConfig();
	    	authConfig.setType(authConfigType);
	    	authConfig.setInternalUsername("bababooey");
	    	
	    	loginConfig.addAuthConfig(authConfig);
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
