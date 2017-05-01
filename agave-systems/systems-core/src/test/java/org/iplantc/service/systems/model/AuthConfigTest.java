package org.iplantc.service.systems.model;

import org.apache.commons.lang.NotImplementedException;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.gridforum.jgss.ExtendedGSSCredential;
import org.iplantc.service.common.auth.MyProxyClient;
import org.iplantc.service.systems.model.enumerations.AuthConfigType;
import org.iplantc.service.systems.model.enumerations.CredentialServerProtocolType;
import org.iplantc.service.systems.model.enumerations.StorageProtocolType;
import org.iplantc.service.transfer.ftp.FTP;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;

@Test(groups={"integration"})
public class AuthConfigTest extends SystemsModelTestCommon {

    @BeforeClass
    public void beforeClass() throws Exception {
        super.beforeClass();

    }
    
    @BeforeMethod
	public void setUpMethod() throws Exception {
		jsonTree = jtd.getTestDataObject(JSONTestDataUtil.TEST_SYSTEM_FOLDER + "storage/gridftp.example.com.json")
				.getJSONObject("storage")
				.getJSONObject("auth");
	}

    @DataProvider(name = "authConfigId")
    public Object[][] authConfigId() {
    	return new Object[][] {
    			{ "name", null, "AuthConfig.name cannot be null", true },
    			{ "name", "", "AuthConfig.name cannot be empty", true },
    			{ "name", new Object(), "AuthConfig.name cannot be object", true },
    			{ "name", Arrays.asList("Harry"), "AuthConfig.name cannot be array", true },
    			{ "name", "test name", "AuthConfig.name may contain spaces", false },
    			{ "name", "test~name", "AuthConfig.name cannot contain ~ characters", true },
    			{ "name", "test`name", "AuthConfig.name cannot contain ` characters", true },
    			{ "name", "test!name", "AuthConfig.name cannot contain ! characters", true },
    			{ "name", "test@name", "AuthConfig.name cannot contain @ characters", true },
    			{ "name", "test#name", "AuthConfig.name cannot contain # characters", true },
    			{ "name", "test$name", "AuthConfig.name cannot contain $ characters", true },
    			{ "name", "test%name", "AuthConfig.name cannot contain % characters", true },
    			{ "name", "test^name", "AuthConfig.name cannot contain ^ characters", true },
    			{ "name", "test&name", "AuthConfig.name cannot contain & characters", true },
    			{ "name", "test*name", "AuthConfig.name cannot contain * characters", true },
    			{ "name", "test(name", "AuthConfig.name cannot contain ( characters", true },
    			{ "name", "test)name", "AuthConfig.name cannot contain ) characters", true },
    			{ "name", "test_name", "AuthConfig.name can contain _ characters", false },
    			{ "name", "test+name", "AuthConfig.name cannot contain + characters", true },
    			{ "name", "test=name", "AuthConfig.name cannot contain = characters", true },
    			{ "name", "test{name", "AuthConfig.name cannot contain { characters", true },
    			{ "name", "test}name", "AuthConfig.name cannot contain } characters", true },
    			{ "name", "test|name", "AuthConfig.name cannot contain | characters", true },
    			{ "name", "test\\name", "AuthConfig.name cannot contain \\ characters", true },
    			{ "name", "test\nname", "AuthConfig.name cannot contain carrage return characters", true },
    			{ "name", "test\tname", "AuthConfig.name cannot contain tab characters", true },
    			{ "name", "test:name", "AuthConfig.name cannot contain : characters", true },
    			{ "name", "test;name", "AuthConfig.name cannot contain ; characters", true },
    			{ "name", "test'name", "AuthConfig.name cannot contain ' characters", true },
    			{ "name", "test\"name", "AuthConfig.name cannot contain \" characters", true },
    			{ "name", "test,name", "AuthConfig.name cannot contain , characters", true },
    			{ "name", "test?name", "AuthConfig.name cannot contain ? characters", true },
    			{ "name", "test/name", "AuthConfig.name cannot contain / characters", true },
    			{ "name", "test<name", "AuthConfig.name cannot contain < characters", true },
    			{ "name", "test>name", "AuthConfig.name cannot contain > characters", true },
    			{ "name", "test-name", "AuthConfig.name can contain dashes", false },
    			{ "name", "test.name", "AuthConfig.name cannot contain periods", false },
    			{ "name", "testname", "AuthConfig.name cannot contain all chars", false },
    			{ "name", "1234567890", "AuthConfig.name cannot contain all numbers", false },
    			{ "name", "test0name", "AuthConfig.name cannot contain alpha", false },
    			
    	};
    }

    @Test (groups={"model","system"}, dataProvider="authConfigId")
    public void authConfigIdValidationTest(String name, Object changeValue, String message, boolean exceptionThrown) throws Exception {
    	jsonTree = jsonTree.getJSONObject("server");
        super.commonAuthenticationSystemFromJSON(name,changeValue,message,exceptionThrown);
    }
    
    @DataProvider(name = "authConfigName")
    public Object[][] authConfigName() throws Exception {
        
    	return new Object[][] {
    			{ "username", new Object(), "username cannot be object", true },
    			{ "username", Arrays.asList("Harry"), "username cannot be array", true },
    			{ "username", "test name", "username can be a valid string", false },

    			{ "password", new Object(), "password cannot be object", true },
    			{ "password", Arrays.asList("Harry"), "password cannot be array", true },
    			{ "password", "test site", "password can be a valid string", false },
    			
    			{ "credential", new Object(), "credential cannot be object", true },
    			{ "credential", Arrays.asList("Harry"), "credential cannot be array", true },
    			{ "credential", "Harry", "credential can be a valid string", false },
    			{ "credential", "Harry\n", "credential can have newlines \n", false },
    			{ "credential", "", "credential cannot be empty", true },
    			{ "credential", null, "credential can be null", false },
    			
    			{ "type", null, "type cannot be null", true },
    			{ "type", "", "type cannot be empty", true },
    			{ "type", new Object(), "type cannot be object", true },
    			{ "type", Arrays.asList("Harry"), "type cannot be array", true },
    			{ "type", "protocol", "type cannot be an invalid system type", true },
    			{ "type", "X509", "type can be MYPROXY", false },
    			{ "type", "x509", "type is case insensitive", false },
    	};
    }

    @Test (groups={"model","system"}, dataProvider="authConfigName")
    public void authConfigNameValidationTest(String name, Object changeValue, String message, boolean exceptionThrown) throws Exception {
        super.commonAuthConfigFromJSON(name,changeValue,message,exceptionThrown);
    }
    
    @DataProvider(name = "anonymousFTPAuthConfigProvider")
    public Object[][] anonymousFTPAuthConfigProvider() {
    	return new Object[][] {
    			{ "username", "alpha", "username if specified should only be " + FTP.ANONYMOUS_USER, true },
    			{ "username", "", "username if specified should only be " + FTP.ANONYMOUS_USER, true },
    			{ "username", "ANONYMOUS", "username should be case sensitive. capitalizing the anonymous username should fail", true },
    			{ "username", FTP.ANONYMOUS_USER, FTP.ANONYMOUS_USER + " should be accepted as a valid username", false },
    			{ "username", null, "username can be null", false },
    			
    			{ "password", "hello", "password should be rejected when not equal to a valid email address or " + FTP.ANONYMOUS_PASSWORD, true },
    			{ "password", "", "password should be rejected when not equal to a valid email address or " + FTP.ANONYMOUS_PASSWORD, true },
    			{ "password", null, "password can be null", false },
    			{ "password", FTP.ANONYMOUS_PASSWORD, "password can be null", false },
    			{ "password", "foo@example.com", "password can be a valid email address", false },
    			{ "password", FTP.ANONYMOUS_PASSWORD.toUpperCase(), "password should be case sensitive. capitalizing an anonymous password should fail", true },
    	};
    }
    
    @Test (groups={"model","system"}, dataProvider="anonymousFTPAuthConfigProvider")
    public void anonymousFTPAuthConfigTest(String fieldName, String changeValue, String message, boolean exceptionThrown) 
    throws Exception 
    {   
    	jsonTree = jtd.getTestDataObject(JSONTestDataUtil.TEST_SYSTEM_FOLDER + "storage/ftp-anonymous.example.com.json")
				.getJSONObject("storage")
				.getJSONObject("auth");
    	
    	super.commonAuthConfigFromJSON(fieldName,changeValue,message,exceptionThrown);
    }
    
    @DataProvider(name = "authConfigKeyValidationTestProvider")
    public Object[][] authConfigKeyValidationTestProvider() {
        
    	return new Object[][] {
    			{ "publicKey", new Object(), "publicKey cannot be object", true },
    			{ "publicKey", Arrays.asList("Harry"), "publicKey cannot be array", true },
    			{ "publicKey", "Harry", "publicKey can be a valid string", false },
    			{ "publicKey", "Harry", "publicKey can have newlines \n", false },
    			
    			{ "privateKey", new Object(), "privateKey cannot be object", true },
    			{ "privateKey", Arrays.asList("Harry"), "privateKey cannot be array", true },
    			{ "privateKey", "Harry", "privateKey can be a valid string", false },
    			{ "privateKey", "Harry\n", "privateKey can have newlines \n", false }
    	};
    }
    @Test (groups={"model","system"}, dataProvider="authConfigKeyValidationTestProvider")
    public void authConfigKeyValidationTest(String name, Object changeValue, String message, boolean exceptionThrown) 
    throws Exception 
    {
    	jsonTree = jtd.getTestDataObject(JSONTestDataUtil.TEST_SYSTEM_FOLDER + "storage/sftp.example.com.json")
				.getJSONObject("storage")
				.getJSONObject("auth");
    	jsonTree = updateTestData("type", "SSHKEYS");
    	jsonTree = updateTestData("publicKey", "publicKeyValue");
    	jsonTree = updateTestData("privateKey", "privateKeyValue");
    	
    	super.commonAuthConfigFromJSON(name,changeValue,message,exceptionThrown);
    	
    }
    
    @DataProvider(name = "authConfigTypeChecks")
    public Object[][] authConfigTypeChecks() {
    	CredentialServer myproxyServer = CredentialServer.IPLANT_MYPROXY;
    	CredentialServer oauth2Server = new CredentialServer("oath2", "endpoint", 22, CredentialServerProtocolType.OAUTH2);
    	CredentialServer kerberoseServer = new CredentialServer("kerberose", "endpoint", 22, CredentialServerProtocolType.KERBEROSE);
    	CredentialServer vomsServer = new CredentialServer("voms", "endpoint", 22, CredentialServerProtocolType.OAUTH2);
    	CredentialServer oa4mpServer = new CredentialServer("oa4mp", "endpoint", 22, CredentialServerProtocolType.KERBEROSE);
    	
    	return new Object[][] { 
		    { AuthConfigType.PASSWORD, null, null, null, null, null, null, "PASSWORD config username and password cannot be null", true },
		    { AuthConfigType.PASSWORD, "username", null, null, null, null, null, "PASSWORD config password cannot be null", true },
		    { AuthConfigType.PASSWORD, null, "password", null, null, null, null, "PASSWORD config username cannot be null", true },
		    { AuthConfigType.PASSWORD, "username", "password", null, null, null, null, "PASSWORD config credential can be null", false },
		    { AuthConfigType.PASSWORD, "username", "password", "credential", null, null, null, "PASSWORD config username and password cannot have credential", true },
		    { AuthConfigType.PASSWORD, "username", null, "credential", null, null, null, "PASSWORD config password cannot be null despite credential", true },
		    { AuthConfigType.PASSWORD, null, "password", "credential", null, null, null, "PASSWORD config username cannot be null despite credential", true },
		    { AuthConfigType.PASSWORD, null, null, "credential", null, null, null, "PASSWORD config username and password cannot be null despite credential", true },
		    
		    { AuthConfigType.PASSWORD, null, null, null, myproxyServer, null, null, "PASSWORD config username cannot have credential server", true },
		    { AuthConfigType.PASSWORD, "username", "password", null, myproxyServer, null, null, "PASSWORD config cannot have credential server", true },
		    { AuthConfigType.PASSWORD, "username", "password", "credential", myproxyServer, null, null, "PASSWORD config cannot have credential or credential server", true },
		    { AuthConfigType.PASSWORD, "username", null, null, myproxyServer, null, null, "PASSWORD config password cannot be null or have credential server", true },
		    { AuthConfigType.PASSWORD, null, "password", null, myproxyServer, null, null, "PASSWORD config username cannot be null or have credential server", true },
		    { AuthConfigType.PASSWORD, null, null, "credential", myproxyServer, null, null, "PASSWORD config username and password cannot be null despite credential and credential server", true },
		    { AuthConfigType.PASSWORD, null, null, null, myproxyServer, null, null, "PASSWORD config username and password cannot be null or have credential server", true },
		    
		    { AuthConfigType.PASSWORD, null, null, null, null, "publicKey", null, "PASSWORD config publicKey cannot be specified", true },
		    { AuthConfigType.PASSWORD, "username", null, null, null, "publicKey", null, "PASSWORD config username and publicKey cannot be specified", true },
		    { AuthConfigType.PASSWORD, null, "password", null, null, "publicKey", null, "PASSWORD config password and publicKey cannot be specified", true },
		    { AuthConfigType.PASSWORD, null, null, "credential", null, "publicKey", null, "PASSWORD config credential and publicKey cannot be specified", true },
		    { AuthConfigType.PASSWORD, null, null, null, null, "publicKey", "privateKey", "PASSWORD config privateKey and publicKey cannot be specified", true },
		    { AuthConfigType.PASSWORD, "username", null, null, null, null, "privateKey", "PASSWORD config username and privateKey cannot be specified", true },
		    { AuthConfigType.PASSWORD, null, "password", null, null, null, "privateKey", "PASSWORD config password and privateKey cannot be specified", true },
		    { AuthConfigType.PASSWORD, null, null, "credential", null, null, "privateKey", "PASSWORD config credential and privateKey cannot be specified", true },
		    { AuthConfigType.PASSWORD, null, null, null, null, null, "privateKey", "PASSWORD config privateKey cannot be specified", true },
		    { AuthConfigType.PASSWORD, "username", "password", null, null, null, "privateKey", "PASSWORD config privateKey cannot be specified", true },
		    { AuthConfigType.PASSWORD, "username", "password", null, null, "publicKey", null, "PASSWORD config publicKey cannot be specified", true },
		    { AuthConfigType.PASSWORD, "username", "password", null, null, "publicKey", "privateKey", "PASSWORD config publicKey and privateKey cannot be specified", true },
		    
		    { AuthConfigType.PASSWORD, "username", "password", null, myproxyServer, "publicKey", null, "PASSWORD config publicKey cannot be specified", true },
		    { AuthConfigType.PASSWORD, "username", "password", null, myproxyServer, "publicKey", "privateKey", "PASSWORD config publicKey and privateKey cannot be specified", true },
		    { AuthConfigType.PASSWORD, "username", "password", null, myproxyServer, null, "privateKey", "PASSWORD config privateKey cannot be specified", true },
		    { AuthConfigType.PASSWORD, "username", "password", "credential", myproxyServer, "publicKey", null, "PASSWORD config publicKey cannot be specified", true },
		    { AuthConfigType.PASSWORD, "username", "password", "credential", myproxyServer, "publicKey", "privateKey", "PASSWORD config privateKey and publicKey cannot be specified", true },
		    { AuthConfigType.PASSWORD, "username", "password", "credential", myproxyServer, null, "privateKey", "PASSWORD config privateKey cannot be specified", true },
		    { AuthConfigType.PASSWORD, "username", "password", "credential", null, "publicKey", null, "PASSWORD config publicKey cannot be specified", true },
		    { AuthConfigType.PASSWORD, "username", "password", "credential", null, "publicKey", "privateKey", "PASSWORD config privateKey and publicKey cannot be specified", true },
		    { AuthConfigType.PASSWORD, "username", "password", "credential", null, null, "privateKey", "PASSWORD config privateKey cannot be specified", true },
		    
		    
		    
		    { AuthConfigType.KERBEROSE, null, null, null, null, null, null, "KERBEROSE config username and password cannot be null", true },
		    { AuthConfigType.KERBEROSE, "username", null, null, null, null, null, "KERBEROSE config password cannot be null", true },
		    { AuthConfigType.KERBEROSE, null, "password", null, null, null, null, "KERBEROSE config username cannot be null", true },
		    { AuthConfigType.KERBEROSE, "username", "password", null, null, null, null, "KERBEROSE config credential can be null", false },
		    { AuthConfigType.KERBEROSE, "username", "password", "credential", null, null, null, "KERBEROSE config username and password cannot have credential", true },
		    { AuthConfigType.KERBEROSE, "username", null, "credential", null, null, null, "KERBEROSE config password cannot be null despite credential", true },
		    { AuthConfigType.KERBEROSE, null, "password", "credential", null, null, null, "KERBEROSE config username cannot be null despite credential", true },
		    { AuthConfigType.KERBEROSE, null, null, "credential", null, null, null, "KERBEROSE config username and password cannot be null despite credential", true },
		    
		    { AuthConfigType.KERBEROSE, null, null, null, kerberoseServer, null, null, "KERBEROSE config username cannot have credential server", true },
		    { AuthConfigType.KERBEROSE, "username", "password", null, kerberoseServer, null, null, "KERBEROSE config cannot have credential server", true },
		    { AuthConfigType.KERBEROSE, "username", "password", "credential", kerberoseServer, null, null, "KERBEROSE config cannot have credential or credential server", true },
		    { AuthConfigType.KERBEROSE, "username", null, null, kerberoseServer, null, null, "KERBEROSE config password cannot be null or have credential server", true },
		    { AuthConfigType.KERBEROSE, null, "password", null, kerberoseServer, null, null, "KERBEROSE config username cannot be null or have credential server", true },
		    { AuthConfigType.KERBEROSE, null, null, "credential", kerberoseServer, null, null, "KERBEROSE config username and password cannot be null despite credential and credential server", true },
		    { AuthConfigType.KERBEROSE, null, null, null, kerberoseServer, null, null, "KERBEROSE config username and password cannot be null or have credential server", true },
		    
		    { AuthConfigType.KERBEROSE, null, null, null, null, "publicKey", null, "KERBEROSE config publicKey cannot be specified", true },
		    { AuthConfigType.KERBEROSE, "username", null, null, null, "publicKey", null, "KERBEROSE config username and publicKey cannot be specified", true },
		    { AuthConfigType.KERBEROSE, null, "password", null, null, "publicKey", null, "KERBEROSE config password and publicKey cannot be specified", true },
		    { AuthConfigType.KERBEROSE, null, null, "credential", null, "publicKey", null, "KERBEROSE config credential and publicKey cannot be specified", true },
		    { AuthConfigType.KERBEROSE, null, null, null, null, "publicKey", "privateKey", "KERBEROSE config privateKey and publicKey cannot be specified", true },
		    { AuthConfigType.KERBEROSE, "username", null, null, null, null, "privateKey", "KERBEROSE config username and privateKey cannot be specified", true },
		    { AuthConfigType.KERBEROSE, null, "password", null, null, null, "privateKey", "KERBEROSE config password and privateKey cannot be specified", true },
		    { AuthConfigType.KERBEROSE, null, null, "credential", null, null, "privateKey", "KERBEROSE config credential and privateKey cannot be specified", true },
		    { AuthConfigType.KERBEROSE, null, null, null, null, null, "privateKey", "KERBEROSE config privateKey cannot be specified", true },
		    { AuthConfigType.KERBEROSE, "username", "password", null, null, null, "privateKey", "KERBEROSE config privateKey cannot be specified", true },
		    { AuthConfigType.KERBEROSE, "username", "password", null, null, "publicKey", null, "KERBEROSE config publicKey cannot be specified", true },
		    { AuthConfigType.KERBEROSE, "username", "password", null, null, "publicKey", "privateKey", "KERBEROSE config publicKey and privateKey cannot be specified", true },
		    
		    { AuthConfigType.KERBEROSE, "username", "password", null, kerberoseServer, "publicKey", null, "KERBEROSE config publicKey cannot be specified", true },
		    { AuthConfigType.KERBEROSE, "username", "password", null, kerberoseServer, "publicKey", "privateKey", "KERBEROSE config publicKey and privateKey cannot be specified", true },
		    { AuthConfigType.KERBEROSE, "username", "password", null, kerberoseServer, null, "privateKey", "KERBEROSE config privateKey cannot be specified", true },
		    { AuthConfigType.KERBEROSE, "username", "password", "credential", kerberoseServer, "publicKey", null, "KERBEROSE config publicKey cannot be specified", true },
		    { AuthConfigType.KERBEROSE, "username", "password", "credential", kerberoseServer, "publicKey", "privateKey", "KERBEROSE config privateKey and publicKey cannot be specified", true },
		    { AuthConfigType.KERBEROSE, "username", "password", "credential", kerberoseServer, null, "privateKey", "KERBEROSE config privateKey cannot be specified", true },
		    { AuthConfigType.KERBEROSE, "username", "password", "credential", null, "publicKey", null, "KERBEROSE config publicKey cannot be specified", true },
		    { AuthConfigType.KERBEROSE, "username", "password", "credential", null, "publicKey", "privateKey", "KERBEROSE config privateKey and publicKey cannot be specified", true },
		    { AuthConfigType.KERBEROSE, "username", "password", "credential", null, null, "privateKey", "KERBEROSE config privateKey cannot be specified", true },
		    
		    
		    { AuthConfigType.X509, null, null, null, myproxyServer, null, null, "MYPROXY config username and password and credential cannot be null", true },
		    { AuthConfigType.X509, "username", null, null, myproxyServer, null, null, "MYPROXY config password cannot be null without credential", true },
		    { AuthConfigType.X509, null, "password", null, myproxyServer, null, null, "MYPROXY config username cannot be null without credential", true },
		    { AuthConfigType.X509, "username", "password", null, myproxyServer, null, null, "MYPROXY config credential can be null with username and password", false },
		    { AuthConfigType.X509, "username", "password", "credential", myproxyServer, null, null, "MYPROXY config username and password can have credential", false },
		    { AuthConfigType.X509, "username", null, "credential", myproxyServer, null, null, "MYPROXY config password can be null with valid credential", true },
		    { AuthConfigType.X509, null, "password", "credential", myproxyServer, null, null, "MYPROXY config username can be null with valid credential", true },
		    
		    { AuthConfigType.X509, null, null, null, myproxyServer, null, null, "MYPROXY config username cannot have credential server", true },
		    { AuthConfigType.X509, "username", "password", null, myproxyServer, null, null, "MYPROXY config cannot have credential server", false },
		    { AuthConfigType.X509, "username", "password", "credential", myproxyServer, null, null, "MYPROXY config cannot have credential or credential server", false },
		    { AuthConfigType.X509, "username", null, null, myproxyServer, null, null, "MYPROXY config password cannot be null or have credential server", true },
		    { AuthConfigType.X509, null, "password", null, myproxyServer, null, null, "MYPROXY config username cannot be null or have credential server", true },
		    { AuthConfigType.X509, null, null, "credential", myproxyServer, null, null, "MYPROXY config username and password cannot be null despite credential and credential server", true },
		    { AuthConfigType.X509, null, null, null, myproxyServer, null, null, "MYPROXY config username and password cannot be null or have credential server", true },
		    { AuthConfigType.X509, "username", "password", null, kerberoseServer, null, null, "MYPROXY config cannot have kerberose credential server", true },
		    { AuthConfigType.X509, "username", "password", null, oauth2Server, null, null, "MYPROXY config cannot have oauth credential server", true },
		    { AuthConfigType.X509, "username", "password", null, vomsServer, null, null, "MYPROXY config cannot have oauth credential server", true },
		    { AuthConfigType.X509, "username", "password", null, oa4mpServer, null, null, "MYPROXY config cannot have oauth credential server", true },
		    
		    
		    { AuthConfigType.X509, null, null, null, null, "publicKey", null, "X509 config publicKey cannot be specified", true },
		    { AuthConfigType.X509, "username", null, null, null, "publicKey", null, "X509 config username and publicKey cannot be specified", true },
		    { AuthConfigType.X509, null, "password", null, null, "publicKey", null, "X509 config password and publicKey cannot be specified", true },
		    { AuthConfigType.X509, null, null, "credential", null, "publicKey", null, "X509 config credential and publicKey cannot be specified", true },
		    { AuthConfigType.X509, null, null, null, null, "publicKey", "privateKey", "X509 config privateKey and publicKey cannot be specified", true },
		    { AuthConfigType.X509, "username", null, null, null, null, "privateKey", "X509 config username and privateKey cannot be specified", true },
		    { AuthConfigType.X509, null, "password", null, null, null, "privateKey", "X509 config password and privateKey cannot be specified", true },
		    { AuthConfigType.X509, null, null, "credential", null, null, "privateKey", "X509 config credential and privateKey cannot be specified", true },
		    { AuthConfigType.X509, null, null, null, null, null, "privateKey", "X509 config privateKey cannot be specified", true },
		    
		    { AuthConfigType.X509, "username", "password", null, myproxyServer, "publicKey", null, "X509 config publicKey cannot be specified", true },
		    { AuthConfigType.X509, "username", "password", null, myproxyServer, "publicKey", "privateKey", "X509 config publicKey and privateKey cannot be specified", true },
		    { AuthConfigType.X509, "username", "password", null, myproxyServer, null, "privateKey", "X509 config privateKey cannot be specified", true },
		    { AuthConfigType.X509, "username", "password", "credential", myproxyServer, "publicKey", null, "X509 config publicKey cannot be specified", true },
		    { AuthConfigType.X509, "username", "password", "credential", myproxyServer, "publicKey", "privateKey", "X509 config privateKey and publicKey cannot be specified", true },
		    { AuthConfigType.X509, "username", "password", "credential", myproxyServer, null, "privateKey", "X509 config privateKey cannot be specified", true },
		    { AuthConfigType.X509, "username", "password", "credential", null, "publicKey", null, "X509 config publicKey cannot be specified", true },
		    { AuthConfigType.X509, "username", "password", "credential", null, "publicKey", "privateKey", "X509 config privateKey and publicKey cannot be specified", true },
		    { AuthConfigType.X509, "username", "password", "credential", null, null, "privateKey", "X509 config privateKey cannot be specified", true },
		    
		    
		    { AuthConfigType.TOKEN, null, null, null, oauth2Server, null, null, "TOKEN config username and password and credential cannot be null", true },
		    { AuthConfigType.TOKEN, "username", null, null, oauth2Server, null, null, "TOKEN config password cannot be null without credential", true },
		    { AuthConfigType.TOKEN, null, "password", null, oauth2Server, null, null, "TOKEN config username cannot be null without credential", true },
		    { AuthConfigType.TOKEN, "username", "password", null, oauth2Server, null, null, "TOKEN config credential can be null with username and password", false },
		    { AuthConfigType.TOKEN, "username", "password", "credential", oauth2Server, null, null, "TOKEN config username and password can have credential", false },
		    { AuthConfigType.TOKEN, "username", null, "credential", oauth2Server, null, null, "TOKEN config password cann be null with valid credential", false },
		    { AuthConfigType.TOKEN, null, "password", "credential", oauth2Server, null, null, "TOKEN config username cannot be null with valid credential", true },
		    { AuthConfigType.TOKEN, null, null, "credential", oauth2Server, null, null, "TOKEN config username and password cannot be null with valid despite credential", true },
		    
		    { AuthConfigType.TOKEN, null, null, null, oauth2Server, null, null, "TOKEN config username cannot have credential server", true },
		    { AuthConfigType.TOKEN, "username", "password", null, oauth2Server, null, null, "TOKEN config can have null credential with pass", false },
		    { AuthConfigType.TOKEN, "username", "password", "credential", oauth2Server, null, null, "TOKEN config can have all fields", false },
		    { AuthConfigType.TOKEN, "username", null, null, oauth2Server, null, null, "TOKEN config password cannot be null or have credential server", true },
		    { AuthConfigType.TOKEN, null, "password", null, oauth2Server, null, null, "TOKEN config username cannot be null or have credential server", true },
		    { AuthConfigType.TOKEN, null, null, "credential", oauth2Server, null, null, "TOKEN config username and password cannot be null despite credential and credential server", true },
		    { AuthConfigType.TOKEN, null, null, null, oauth2Server, null, null, "TOKEN config username and password cannot be null or have credential server", true },
		    { AuthConfigType.TOKEN, "username", "password", null, kerberoseServer, null, null, "TOKEN config cannot have kerberose credential server", true },
		    { AuthConfigType.TOKEN, "username", "password", null, myproxyServer, null, null, "TOKEN config cannot have myproxyServer credential server", true },
		    
		    { AuthConfigType.TOKEN, null, null, null, null, "publicKey", null, "TOKEN config publicKey cannot be specified", true },
		    { AuthConfigType.TOKEN, "username", null, null, null, "publicKey", null, "TOKEN config username and publicKey cannot be specified", true },
		    { AuthConfigType.TOKEN, null, "password", null, null, "publicKey", null, "TOKEN config password and publicKey cannot be specified", true },
		    { AuthConfigType.TOKEN, null, null, "credential", null, "publicKey", null, "TOKEN config credential and publicKey cannot be specified", true },
		    { AuthConfigType.TOKEN, null, null, null, null, "publicKey", "privateKey", "TOKEN config privateKey and publicKey cannot be specified", true },
		    { AuthConfigType.TOKEN, "username", null, null, null, null, "privateKey", "TOKEN config username and privateKey cannot be specified", true },
		    { AuthConfigType.TOKEN, null, "password", null, null, null, "privateKey", "TOKEN config password and privateKey cannot be specified", true },
		    { AuthConfigType.TOKEN, null, null, "credential", null, null, "privateKey", "TOKEN config credential and privateKey cannot be specified", true },
		    { AuthConfigType.TOKEN, null, null, null, null, null, "privateKey", "TOKEN config privateKey cannot be specified", true },
		    
		    { AuthConfigType.TOKEN, "username", "password", null, oauth2Server, "publicKey", null, "TOKEN config publicKey cannot be specified", true },
		    { AuthConfigType.TOKEN, "username", "password", null, oauth2Server, "publicKey", "privateKey", "TOKEN config publicKey and privateKey cannot be specified", true },
		    { AuthConfigType.TOKEN, "username", "password", null, oauth2Server, null, "privateKey", "TOKEN config privateKey cannot be specified", true },
		    { AuthConfigType.TOKEN, "username", "password", "credential", oauth2Server, "publicKey", null, "TOKEN config publicKey cannot be specified", true },
		    { AuthConfigType.TOKEN, "username", "password", "credential", oauth2Server, "publicKey", "privateKey", "TOKEN config privateKey and publicKey cannot be specified", true },
		    { AuthConfigType.TOKEN, "username", "password", "credential", oauth2Server, null, "privateKey", "TOKEN config privateKey cannot be specified", true },
		    { AuthConfigType.TOKEN, "username", "password", "credential", null, "publicKey", null, "TOKEN config publicKey cannot be specified", true },
		    { AuthConfigType.TOKEN, "username", "password", "credential", null, "publicKey", "privateKey", "TOKEN config privateKey and publicKey cannot be specified", true },
		    { AuthConfigType.TOKEN, "username", "password", "credential", null, null, "privateKey", "TOKEN config privateKey cannot be specified", true },
		    
		    
		    { AuthConfigType.LOCAL, null, null, null, null, null, null, "LOCAL config username and password and credential can be null", false },
		    { AuthConfigType.LOCAL, "username", null, null, null, null, null, "LOCAL config username cannot be specified", true },
		    { AuthConfigType.LOCAL, null, "password", null, null, null, null, "LOCAL config password cannot be specified", true },
		    { AuthConfigType.LOCAL, "username", "password", null, null, null, null, "LOCAL config username and password cannot be specified", true },
		    { AuthConfigType.LOCAL, "username", "password", "credential", null, null, null, "LOCAL config username, password, and credential cannot be specified", true },
		    { AuthConfigType.LOCAL, "username", null, "credential", null, null, null, "LOCAL config username and credential cannot be specified", true },
		    { AuthConfigType.LOCAL, null, "password", "credential", null, null, null, "LOCAL config credential and password cannot be specified", true },
		    { AuthConfigType.LOCAL, null, null, "credential", null, null, null, "LOCAL config credential cannot be specified", true },
		    
		    { AuthConfigType.LOCAL, null, null, null, null, "publicKey", null, "LOCAL config publicKey cannot be specified", true },
		    { AuthConfigType.LOCAL, "username", null, null, null, "publicKey", null, "LOCAL config username and publicKey cannot be specified", true },
		    { AuthConfigType.LOCAL, null, "password", null, null, "publicKey", null, "LOCAL config password and publicKey cannot be specified", true },
		    { AuthConfigType.LOCAL, null, null, "credential", null, "publicKey", null, "LOCAL config credential and publicKey cannot be specified", true },
		    { AuthConfigType.LOCAL, null, null, null, null, "publicKey", "privateKey", "LOCAL config privateKey and publicKey cannot be specified", true },
		    { AuthConfigType.LOCAL, "username", null, null, null, null, "privateKey", "LOCAL config username and privateKey cannot be specified", true },
		    { AuthConfigType.LOCAL, null, "password", null, null, null, "privateKey", "LOCAL config password and privateKey cannot be specified", true },
		    { AuthConfigType.LOCAL, null, null, "credential", null, null, "privateKey", "LOCAL config credential and privateKey cannot be specified", true },
		    { AuthConfigType.LOCAL, null, null, null, null, null, "privateKey", "LOCAL config privateKey cannot be specified", true },
		    
		    { AuthConfigType.LOCAL, null, null, null, kerberoseServer, "publicKey", null, "LOCAL config publicKey cannot have have credential server", true },
		    { AuthConfigType.LOCAL, null, null, null, kerberoseServer, "publicKey", "privateKey", "LOCAL config privateKey and publicKey cannot have credential server", true },
		    { AuthConfigType.LOCAL, null, null, null, kerberoseServer, null, "privateKey", "LOCAL config privateKey cannot have credential server", true },
		    { AuthConfigType.LOCAL, null, null, "credential", null, "publicKey", null, "LOCAL config publicKey cannot have have credential ", true },
		    { AuthConfigType.LOCAL, null, null, "credential", null, "publicKey", "privateKey", "LOCAL config privateKey and publicKey cannot have  server", true },
		    { AuthConfigType.LOCAL, null, null, "credential", null, null, "privateKey", "LOCAL config privateKey cannot have credential ", true },
		    
		    { AuthConfigType.LOCAL, null, null, null, kerberoseServer, null, null, "LOCAL config username cannot have credential server", true },
		    { AuthConfigType.LOCAL, "username", "password", null, kerberoseServer, null, null, "LOCAL config cannot have credential server", true },
		    { AuthConfigType.LOCAL, "username", "password", "credential", kerberoseServer, null, null, "LOCAL config cannot have credential or credential server", true },
		    { AuthConfigType.LOCAL, "username", null, null, kerberoseServer, null, null, "LOCAL config password cannot be null or have credential server", true },
		    { AuthConfigType.LOCAL, null, "password", null, kerberoseServer, null, null, "LOCAL config username cannot be null or have credential server", true },
		    { AuthConfigType.LOCAL, null, null, "credential", kerberoseServer, null, null, "LOCAL config username and password cannot be null despite credential and credential server", true },
		     
		    { AuthConfigType.SSHKEYS, null, null, null, null, null, null, "SSHKEYS config publicKey and privateKey and username cannot be null", true },
		    { AuthConfigType.SSHKEYS, "username", null, null, null, null, null, "SSHKEYS config publicKey and privateKey and password cannot be null", true },
		    { AuthConfigType.SSHKEYS, null, "password", null, null, null, null, "SSHKEYS config publicKey and privateKey and username cannot be null", true },
		    { AuthConfigType.SSHKEYS, "username", "password", null, null, null, null, "SSHKEYS config publicKey and privateKey cannot be null", true },
		    { AuthConfigType.SSHKEYS, "username", "password", null, null, "publicKey", null, "SSHKEYS config privateKey and username cannot be null", true },
		    { AuthConfigType.SSHKEYS, "username", "password", null, null, null, "privateKey", "SSHKEYS config publicKey and privateKey and username cannot be null", true },
		    { AuthConfigType.SSHKEYS, "username", "password", null, null, "publicKey", "privateKey", "SSHKEYS config credential can be null", false },
		    { AuthConfigType.SSHKEYS, "username", "password", "credential", null, "publicKey", "privateKey", "SSHKEYS config username and password can have credential", true },
		    { AuthConfigType.SSHKEYS, "username", null, null, null, "publicKey", "privateKey", "SSHKEYS config password can be null", false },
		    { AuthConfigType.SSHKEYS, null, "password", "credential", null, "publicKey", "privateKey", "SSHKEYS config username cannot be null despite credential", true },
		    { AuthConfigType.SSHKEYS, null, "password", null, null, "publicKey", "privateKey", "SSHKEYS config username cannot be null", true },
		    { AuthConfigType.SSHKEYS, null, null, "credential", null, "publicKey", "privateKey", "SSHKEYS config username and password cannot be null despite credential", true },
		    
		    { AuthConfigType.SSHKEYS, "username", null, null, myproxyServer, "publicKey", "privateKey", "SSHKEYS config username cannot have credential server", true },
		    { AuthConfigType.SSHKEYS, "username", "password", null, myproxyServer, "publicKey", "privateKey", "SSHKEYS config cannot have credential server", true },
		    { AuthConfigType.SSHKEYS, "username", "password", "credential", myproxyServer, "publicKey", "privateKey", "SSHKEYS config cannot have credential or credential server", true },
		    { AuthConfigType.SSHKEYS, "username", null, null, myproxyServer, "publicKey", "privateKey", "SSHKEYS config password cannot be null or have credential server", true },
		    { AuthConfigType.SSHKEYS, null, "password", null, myproxyServer, "publicKey", "privateKey", "SSHKEYS config username cannot be null or have credential server", true },
		    { AuthConfigType.SSHKEYS, null, null, "credential", myproxyServer, "publicKey", "privateKey", "SSHKEYS config username and password cannot be null despite credential and credential server", true },
		    { AuthConfigType.SSHKEYS, null, null, null, myproxyServer, "publicKey", "privateKey", "SSHKEYS config username and password cannot be null or have credential server", true },
		    
		    
		    { AuthConfigType.ANONYMOUS, null, null, null, null, null, null, "ANONYMOUS config username should allow null value", false },
		    { AuthConfigType.ANONYMOUS, "anonymous", null, null, null, null, null, "ANONYMOUS config username should allow anonymous username and null password", false },
		    { AuthConfigType.ANONYMOUS, null, "guest", null, null, null, null, "ANONYMOUS config username should allow null username and valid password", false },
		    { AuthConfigType.ANONYMOUS, "anonymous", "guest", null, null, null, null, "ANONYMOUS config username should allow valid username and password", false },
		    { AuthConfigType.ANONYMOUS, "anonymous", "guest", null, null, "publicKey", null, "ANONYMOUS should not accept publicKey", true },
		    { AuthConfigType.ANONYMOUS, "anonymous", "guest", null, null, null, "privateKey", "ANONYMOUS should not accept privateKey", true },
		    { AuthConfigType.ANONYMOUS, "anonymous", "guest", null, null, "publicKey", "privateKey", "ANONYMOUS should not accept publicKey or privateKey", true },
		    { AuthConfigType.ANONYMOUS, "anonymous", "guest", "credential", null, "publicKey", "privateKey", "ANONYMOUS should not accept publicKey or privateKey or credential", true },
		    { AuthConfigType.ANONYMOUS, "anonymous", null, null, null, "publicKey", "privateKey", "ANONYMOUS should not accept publicKey or privateKey", true },
		    { AuthConfigType.ANONYMOUS, null, "guest", "credential", null, "publicKey", "privateKey", "ANONYMOUS should not accept publicKey or privateKey or credential", true },
		    { AuthConfigType.ANONYMOUS, null, "guest", null, null, "publicKey", "privateKey", "ANONYMOUS should not accept publicKey or privateKey", true },
		    { AuthConfigType.ANONYMOUS, null, null, "credential", null, "publicKey", "privateKey", "ANONYMOUS should not accept publicKey or privateKey", true },
		    
		    { AuthConfigType.ANONYMOUS, null, null, null, myproxyServer, "publicKey", "privateKey", "ANONYMOUS config should not accept publicKey or privateKey or credential or credential server", true },
		    { AuthConfigType.ANONYMOUS, "anonymous", "guest", null, myproxyServer, "publicKey", "privateKey", "ANONYMOUS config should not accept publicKey or privateKey or credential server", true },
		    { AuthConfigType.ANONYMOUS, "anonymous", "guest", "credential", myproxyServer, "publicKey", "privateKey", "ANONYMOUS config should not accept publicKey or privateKey or credential or credential server", true },
		    { AuthConfigType.ANONYMOUS, "anonymous", null, null, myproxyServer, "publicKey", "privateKey", "ANONYMOUS config should not accept a credential server", true },
		    { AuthConfigType.ANONYMOUS, null, "guest", null, myproxyServer, "publicKey", "privateKey", "ANONYMOUS config should not accept publicKey or privateKey or credential server", true },
		    { AuthConfigType.ANONYMOUS, null, null, "credential", myproxyServer, "publicKey", "privateKey", "ANONYMOUS config should not accept publicKey or privateKey or credential or credential server", true },
		    { AuthConfigType.ANONYMOUS, null, null, null, myproxyServer, "publicKey", "privateKey", "ANONYMOUS config should not accept username and password cannot have credential server", true },
		    
		};
	}
    
    @Test (groups={"model","system"}, dataProvider="authConfigTypeChecks")
    public void authConfigTypeChecksTest(AuthConfigType type, String username, String password, 
    		String credential, CredentialServer server, String publicKey, String privateKey, String message, boolean exceptionThrown) 
    throws Exception 
    {
    	boolean exceptionFlag = false;
		String exceptionMsg = message;
		
		jsonTree.put("type", type.name());
		jsonTree.put("username", username);
		jsonTree.put("password", password);
		jsonTree.put("credential", credential);
		jsonTree.put("publicKey", publicKey);
		jsonTree.put("privateKey", privateKey);
		if (server == null) {
			jsonTree.put("server", (Object)null);
    	} else {
    		JSONObject jsvr = new JSONObject(server);
    		jsvr.put("protocol", server.getProtocol());
    		jsonTree.put("server", jsvr);
    	}
    	
		try 
		{
			AuthConfig.fromJSON(jsonTree);
		}
		catch(Exception se)
		{
			exceptionFlag = true;
			exceptionMsg = "Invalid iPlant JSON submitted, attribute type " + message + " \n\"type\" = \"" + type + "\"\n" + se.getMessage();
			if (!exceptionThrown) 
				se.printStackTrace();
		}

		System.out.println(" exception thrown?  expected " + exceptionThrown + " actual " + exceptionFlag);

		Assert.assertTrue(exceptionFlag == exceptionThrown, exceptionMsg);
    }
    
    @DataProvider(name = "retrieveCredentialProvider")
    public Object[][] retrieveCredentialProvider() throws Exception {
    	// pull in storage configs
    	HashMap<StorageProtocolType, StorageConfig> storageConfigs = new HashMap<StorageProtocolType, StorageConfig>();
    	
    	for (StorageProtocolType storageProtocol: StorageProtocolType.values()) {
    		File testSystem = new File(JSONTestDataUtil.TEST_SYSTEM_FOLDER + "storage/" + 
    				storageProtocol.name().toLowerCase() + ".example.com.json");
    		if (testSystem.exists()) {
	    		jsonTree = jtd.getTestDataObject(testSystem.getPath())
	    				.getJSONObject("storage");
	    		//StorageConfig storageConfig = StorageConfig.fromJSON(jsonTree);
	    		
	    		storageConfigs.put(storageProtocol, StorageConfig.fromJSON(jsonTree));
    		}
    	}
    	
    	AuthConfig passAuthConfig = AuthConfig.IPLANT_IRODS_AUTH_CONFIG;
    	
    	AuthConfig kerberoseAuthConfig = passAuthConfig.clone();
    	kerberoseAuthConfig.setType(AuthConfigType.KERBEROSE);
    	
    	AuthConfig localAuthConfig = new AuthConfig();
    	localAuthConfig.setType(AuthConfigType.LOCAL);
    	
    	AuthConfig myproxyAuthConfig = storageConfigs.get(StorageProtocolType.GRIDFTP).getDefaultAuthConfig();
    	
    	String myproxySalt = "gridftp.example.com" + 
    			storageConfigs.get(StorageProtocolType.GRIDFTP).getHost() + 
    			myproxyAuthConfig.getUsername();
    	storageConfigs.get(StorageProtocolType.GRIDFTP).getDefaultAuthConfig().encryptCurrentPassword(myproxySalt);
    	
    	AuthConfig credentialAuthConfig = new AuthConfig();
    	credentialAuthConfig.setType(AuthConfigType.X509);
    	ExtendedGSSCredential cred = (ExtendedGSSCredential)MyProxyClient.getCommunityCredential();
    	byte[] data = cred.export(ExtendedGSSCredential.IMPEXP_OPAQUE);
    	credentialAuthConfig.setCredential(new String(data));
    	credentialAuthConfig.encryptCurrentCredential(myproxySalt);
    	
    	AuthConfig tokenAuthConfig = credentialAuthConfig.clone();
    	tokenAuthConfig.setType(AuthConfigType.TOKEN);
    	tokenAuthConfig.setCredential("abracadabra");
    	tokenAuthConfig.encryptCurrentCredential(myproxySalt);
    	
    	return new Object[][]{
    		{ passAuthConfig, null, null, "No credential retrieved for LoginCredentialType.PASSWORD auth config", false },
    		{ kerberoseAuthConfig, null, null, "No credential retrieved for LoginCredentialType.KERBEROSE auth config", false },
    		{ localAuthConfig, null, null, "No credentail retrieved for LoginCredentialType.LOCAL auth config", false },
    		{ myproxyAuthConfig, myproxySalt, GlobusGSSCredentialImpl.class, "GSSCredential retrieved for LoginCredentialType.MYPROXY auth config", false },
    		{ credentialAuthConfig, myproxySalt, GlobusGSSCredentialImpl.class, "GSSCredential retrieved for LoginCredentialType.CREDENTIAL auth config", false },
    		{ tokenAuthConfig, null, null, "LoginCredentialType.TOKEN auth config not yet implemented", true },
    	};
    }
    	
    
    @Test (groups={"model","system", "remote"}, dataProvider="retrieveCredentialProvider",enabled=false)
    public void retrieveCredentialTest(AuthConfig authConfig, String salt, Class<?> expectedType, String message, boolean exceptionThrown) 
    throws Exception 
    {
    	boolean exceptionFlag = false;
		String exceptionMsg = message;
		Object o = null;
		try 
		{
			o = authConfig.retrieveCredential(salt);
		}
		catch (NotImplementedException e) {
			exceptionFlag = true;
			exceptionMsg = "Failed to retrieve credential " + message;
			if (!exceptionThrown) 
				e.printStackTrace();
		}
		catch (Exception se)
		{
			exceptionFlag = true;
			exceptionMsg = "Failed to retrieve credential" + message;
			if (!exceptionThrown) 
				se.printStackTrace();
		}

		System.out.println(" exception thrown?  expected " + exceptionThrown + " actual " + exceptionFlag);

		if (expectedType == null) {
			Assert.assertNull(o, message);
		} else {
			Assert.assertNotNull(o, message);
			Assert.assertEquals(o.getClass().getName(), expectedType.getName(), message);
		}
		
		Assert.assertTrue(exceptionFlag == exceptionThrown, exceptionMsg);
    }
    
    @Test (groups={"model","system", "remote"},enabled=false)
    public void retrieveSerializedCredentialTest() 
    throws Exception 
    {
    	File myproxySystem = new File(JSONTestDataUtil.TEST_SYSTEM_FOLDER + "storage/" + 
				"gridftp.example.com.json");
		
    	if (!myproxySystem.exists()) {
			throw new FileNotFoundException("Missing myproxy auth storage system config file");
		}
		
		jsonTree = jtd.getTestDataObject(myproxySystem.getPath());
		StorageConfig myProxyStorageConfig = StorageConfig.fromJSON(jsonTree.getJSONObject("storage"));
		AuthConfig myproxyAuthConfig = myProxyStorageConfig.getDefaultAuthConfig();
		String myproxySalt = jsonTree.getString("id") + 
				myProxyStorageConfig.getHost() + 
    			myproxyAuthConfig.getUsername();
		myproxyAuthConfig.encryptCurrentPassword(myproxySalt);
    	ExtendedGSSCredential cred = (ExtendedGSSCredential)myproxyAuthConfig.retrieveCredential(myproxySalt);
    	byte[] data = cred.export(ExtendedGSSCredential.IMPEXP_OPAQUE);
    	String serializedCredential = new String(data);
    	
    	jsonTree.getJSONObject("storage").getJSONObject("auth").remove("server");
    	jsonTree.getJSONObject("storage").getJSONObject("auth").remove("username");
    	jsonTree.getJSONObject("storage").getJSONObject("auth").remove("password");
    	jsonTree.getJSONObject("storage").getJSONObject("auth").remove("credential");
    	jsonTree.getJSONObject("storage").getJSONObject("auth").put("credential", serializedCredential);
    	
		StorageConfig x509StorageConfig = StorageConfig.fromJSON(jsonTree.getJSONObject("storage"));
		x509StorageConfig.getDefaultAuthConfig().encryptCurrentCredential(myproxySalt);
    	String decryptedCredential = x509StorageConfig.getDefaultAuthConfig().getClearTextCredential(myproxySalt);
		
    	Assert.assertEquals(serializedCredential, decryptedCredential);
    }
}
