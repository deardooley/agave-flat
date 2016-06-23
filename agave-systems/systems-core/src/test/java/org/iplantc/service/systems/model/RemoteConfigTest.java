package org.iplantc.service.systems.model;

import java.io.File;
import java.util.HashMap;

import org.iplantc.service.systems.model.enumerations.AuthConfigType;
import org.iplantc.service.systems.model.enumerations.LoginProtocolType;
import org.iplantc.service.systems.model.enumerations.StorageProtocolType;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class RemoteConfigTest extends SystemsModelTestCommon{

	private HashMap<StorageProtocolType, StorageConfig> storageConfigs = new HashMap<StorageProtocolType, StorageConfig>();
	private HashMap<LoginProtocolType, LoginConfig> loginConfigs = new HashMap<LoginProtocolType, LoginConfig>();
	
    @BeforeClass
    public void beforeClass() throws Exception  
    {
        super.beforeClass();
        
        try {
        // pull in storage configs
    	for(StorageProtocolType storageProtocol: StorageProtocolType.values()) {
    		File testSystem = new File(JSONTestDataUtil.TEST_SYSTEM_FOLDER + "storage/" + 
    				storageProtocol.name().toLowerCase() + ".example.com.json");
    		if (testSystem.exists()) {
	    		jsonTree = jtd.getTestDataObject(testSystem.getPath())
	    				.getJSONObject("storage");
	    		
	    		storageConfigs.put(storageProtocol, StorageConfig.fromJSON(jsonTree));
    		}
    	}
    	
    	// pull in login configs
    	for(LoginProtocolType loginProtocol: LoginProtocolType.values()) {
    		File testSystem = new File(JSONTestDataUtil.TEST_SYSTEM_FOLDER + "execution/" + 
    				loginProtocol.name().toLowerCase() + ".example.com.json");
    		if (testSystem.exists()) {
	    		jsonTree = jtd.getTestDataObject(testSystem.getPath())
	    				.getJSONObject("login");
	    		
	    		loginConfigs.put(loginProtocol, LoginConfig.fromJSON(jsonTree));
    		}
    	}
        } catch(Exception e) {
        	e.printStackTrace();
        	throw e;
        }
    }
    
    @DataProvider(name = "storageLoginCredentialType")
    public Object[][] storageLoginCredentialType() 
    {
    	return new Object[][] {
    			{ storageConfigs.get(StorageProtocolType.FTP), AuthConfigType.X509, "StorageProtocolType.FTP is incompatible with LoginCredentialType.X509", false },
    			{ storageConfigs.get(StorageProtocolType.FTP), AuthConfigType.KERBEROSE, "StorageProtocolType.FTP is incompatible with LoginCredentialType.KERBEROSE", false },
    			{ storageConfigs.get(StorageProtocolType.FTP), AuthConfigType.TOKEN, "StorageProtocolType.FTP is compatible with LoginCredentialType.TOKEN", false },
    			{ storageConfigs.get(StorageProtocolType.FTP), AuthConfigType.PASSWORD, "StorageProtocolType.FTP is compatible with LoginCredentialType.PASSWORD", true },
    			{ storageConfigs.get(StorageProtocolType.FTP), AuthConfigType.LOCAL, "StorageProtocolType.FTP is incompatible with LoginCredentialType.LOCAL", false },
    			{ storageConfigs.get(StorageProtocolType.FTP), AuthConfigType.SSHKEYS, "StorageProtocolType.FTP is incompatible with LoginCredentialType.SSHKEYS", false },
    			{ storageConfigs.get(StorageProtocolType.FTP), AuthConfigType.ANONYMOUS, "StorageProtocolType.FTP is compatible with LoginCredentialType.ANONYMOUS", true },
    			
    			{ storageConfigs.get(StorageProtocolType.GRIDFTP), AuthConfigType.X509, "StorageProtocolType.GRIDFTP is compatible with LoginCredentialType.X509", true },
    			{ storageConfigs.get(StorageProtocolType.GRIDFTP), AuthConfigType.KERBEROSE, "StorageProtocolType.GRIDFTP is incompatible with LoginCredentialType.KERBEROSE", false },
    			{ storageConfigs.get(StorageProtocolType.GRIDFTP), AuthConfigType.TOKEN, "StorageProtocolType.GRIDFTP is incompatible with LoginCredentialType.TOKEN", false },
    			{ storageConfigs.get(StorageProtocolType.GRIDFTP), AuthConfigType.PASSWORD, "StorageProtocolType.GRIDFTP is incompatible with LoginCredentialType.PASSWORD", false },
    			{ storageConfigs.get(StorageProtocolType.GRIDFTP), AuthConfigType.LOCAL, "StorageProtocolType.GRIDFTP is incompatible with LoginCredentialType.LOCAL", false },
    			{ storageConfigs.get(StorageProtocolType.GRIDFTP), AuthConfigType.SSHKEYS, "StorageProtocolType.GRIDFTP is incompatible with LoginCredentialType.SSHKEYS", false },
    			{ storageConfigs.get(StorageProtocolType.GRIDFTP), AuthConfigType.ANONYMOUS, "StorageProtocolType.GRIDFTP is incompatible with LoginCredentialType.ANONYMOUS", false },
    			
    			{ storageConfigs.get(StorageProtocolType.IRODS), AuthConfigType.X509, "StorageProtocolType.IRODS is compatible with LoginCredentialType.X509", true },
    			{ storageConfigs.get(StorageProtocolType.IRODS), AuthConfigType.KERBEROSE, "StorageProtocolType.IRODS is incompatible with LoginCredentialType.KERBEROSE", false },
    			{ storageConfigs.get(StorageProtocolType.IRODS), AuthConfigType.TOKEN, "StorageProtocolType.IRODS is incompatible with LoginCredentialType.TOKEN", false },
    			{ storageConfigs.get(StorageProtocolType.IRODS), AuthConfigType.PASSWORD, "StorageProtocolType.IRODS is compatible with LoginCredentialType.PASSWORD", true },
    			{ storageConfigs.get(StorageProtocolType.IRODS), AuthConfigType.LOCAL, "StorageProtocolType.IRODS is incompatible with LoginCredentialType.LOCAL", false },
    			{ storageConfigs.get(StorageProtocolType.IRODS), AuthConfigType.SSHKEYS, "StorageProtocolType.IRODS is incompatible with LoginCredentialType.SSHKEYS", false },
    			{ storageConfigs.get(StorageProtocolType.IRODS), AuthConfigType.ANONYMOUS, "StorageProtocolType.IRODS is incompatible with LoginCredentialType.ANONYMOUS", false },
    			
    			{ storageConfigs.get(StorageProtocolType.IRODS4), AuthConfigType.X509, "StorageProtocolType.IRODS4 is compatible with LoginCredentialType.X509", true },
    			{ storageConfigs.get(StorageProtocolType.IRODS4), AuthConfigType.KERBEROSE, "StorageProtocolType.IRODS4 is incompatible with LoginCredentialType.KERBEROSE", false },
    			{ storageConfigs.get(StorageProtocolType.IRODS4), AuthConfigType.TOKEN, "StorageProtocolType.IRODS4 is incompatible with LoginCredentialType.TOKEN", false },
    			{ storageConfigs.get(StorageProtocolType.IRODS4), AuthConfigType.PASSWORD, "StorageProtocolType.IRODS4 is compatible with LoginCredentialType.PASSWORD", true },
    			{ storageConfigs.get(StorageProtocolType.IRODS4), AuthConfigType.LOCAL, "StorageProtocolType.IRODS4 is incompatible with LoginCredentialType.LOCAL", false },
    			{ storageConfigs.get(StorageProtocolType.IRODS4), AuthConfigType.SSHKEYS, "StorageProtocolType.IRODS4 is incompatible with LoginCredentialType.SSHKEYS", false },
    			{ storageConfigs.get(StorageProtocolType.IRODS4), AuthConfigType.ANONYMOUS, "StorageProtocolType.IRODS4 is incompatible with LoginCredentialType.ANONYMOUS", false },
    			
    			{ storageConfigs.get(StorageProtocolType.SFTP), AuthConfigType.X509, "StorageProtocolType.SFTP is incompatible with LoginCredentialType.X509", false },
    			{ storageConfigs.get(StorageProtocolType.SFTP), AuthConfigType.KERBEROSE, "StorageProtocolType.SFTP is compatible with LoginCredentialType.KERBEROSE", false },
    			{ storageConfigs.get(StorageProtocolType.SFTP), AuthConfigType.TOKEN, "StorageProtocolType.SFTP is incompatible with LoginCredentialType.TOKEN", false },
    			{ storageConfigs.get(StorageProtocolType.SFTP), AuthConfigType.PASSWORD, "StorageProtocolType.SFTP is compatible with LoginCredentialType.PASSWORD", true },
    			{ storageConfigs.get(StorageProtocolType.SFTP), AuthConfigType.LOCAL, "StorageProtocolType.SFTP is incompatible with LoginCredentialType.LOCAL", false },
    			{ storageConfigs.get(StorageProtocolType.SFTP), AuthConfigType.SSHKEYS, "StorageProtocolType.SFTP is compatible with LoginCredentialType.SSHKEYS", true },
    			{ storageConfigs.get(StorageProtocolType.SFTP), AuthConfigType.ANONYMOUS, "StorageProtocolType.SFTP is incompatible with LoginCredentialType.ANONYMOUS", false },
    			
    			
    			{ storageConfigs.get(StorageProtocolType.LOCAL), AuthConfigType.X509, "StorageProtocolType.LOCAL is incompatible with LoginCredentialType.X509", false },
    			{ storageConfigs.get(StorageProtocolType.LOCAL), AuthConfigType.KERBEROSE, "StorageProtocolType.LOCAL is incompatible with LoginCredentialType.KERBEROSE", false },
    			{ storageConfigs.get(StorageProtocolType.LOCAL), AuthConfigType.TOKEN, "StorageProtocolType.LOCAL is incompatible with LoginCredentialType.TOKEN", false },
    			{ storageConfigs.get(StorageProtocolType.LOCAL), AuthConfigType.PASSWORD, "StorageProtocolType.LOCAL is incompatible with LoginCredentialType.PASSWORD", false },
    			{ storageConfigs.get(StorageProtocolType.LOCAL), AuthConfigType.LOCAL, "StorageProtocolType.LOCAL is compatible with LoginCredentialType.LOCAL", true },
    			{ storageConfigs.get(StorageProtocolType.LOCAL), AuthConfigType.SSHKEYS, "StorageProtocolType.LOCAL is incompatible with LoginCredentialType.SSHKEYS", false },
    			{ storageConfigs.get(StorageProtocolType.LOCAL), AuthConfigType.ANONYMOUS, "StorageProtocolType.LOCAL is incompatible with LoginCredentialType.ANONYMOUS", false },
    			
    			{ storageConfigs.get(StorageProtocolType.S3), AuthConfigType.X509, "StorageProtocolType.S3 is incompatible with LoginCredentialType.X509", false },
    			{ storageConfigs.get(StorageProtocolType.S3), AuthConfigType.KERBEROSE, "StorageProtocolType.S3 is incompatible with LoginCredentialType.KERBEROSE", false },
    			{ storageConfigs.get(StorageProtocolType.S3), AuthConfigType.TOKEN, "StorageProtocolType.S3 is compatible with LoginCredentialType.TOKEN", false },
    			{ storageConfigs.get(StorageProtocolType.S3), AuthConfigType.PASSWORD, "StorageProtocolType.S3 is incompatible with LoginCredentialType.PASSWORD", false },
    			{ storageConfigs.get(StorageProtocolType.S3), AuthConfigType.LOCAL, "StorageProtocolType.S3 is incompatible with LoginCredentialType.LOCAL", false },
    			{ storageConfigs.get(StorageProtocolType.S3), AuthConfigType.SSHKEYS, "StorageProtocolType.S3 is incompatible with LoginCredentialType.SSHKEYS", false },
    			{ storageConfigs.get(StorageProtocolType.S3), AuthConfigType.ANONYMOUS, "StorageProtocolType.S3 is incompatible with LoginCredentialType.ANONYMOUS", false },
    			
    			
    	};
    }

    @Test (groups={"model","system"}, dataProvider="storageLoginCredentialType")
    public void storageLoginCredentialTypeTest(StorageConfig storageConfig, AuthConfigType credentialType, String message, boolean assertionValue) 
    throws Exception 
    {	
		Assert.assertEquals(storageConfig.getProtocol().accepts(credentialType), assertionValue);
    }
    
    @DataProvider(name = "loginLoginCredentialType")
    public Object[][] loginLoginCredentialType() 
    {
    	return new Object[][] {
    			{ loginConfigs.get(LoginProtocolType.API), AuthConfigType.X509, "LoginProtocolType.API is incompatible with LoginCredentialType.X509", false },
    			{ loginConfigs.get(LoginProtocolType.API), AuthConfigType.KERBEROSE, "LoginProtocolType.API is incompatible with LoginCredentialType.KERBEROSE", false },
    			{ loginConfigs.get(LoginProtocolType.API), AuthConfigType.TOKEN, "LoginProtocolType.API is compatible with LoginCredentialType.TOKEN", true },
    			{ loginConfigs.get(LoginProtocolType.API), AuthConfigType.PASSWORD, "LoginProtocolType.API is compatible with LoginCredentialType.PASSWORD", true },
    			{ loginConfigs.get(LoginProtocolType.API), AuthConfigType.LOCAL, "LoginProtocolType.API is incompatible with LoginCredentialType.LOCAL", false },
    			
//    			{ loginConfigs.get(LoginProtocolType.GRAM), AuthConfigType.X509, "LoginProtocolType.GRAM is compatible with LoginCredentialType.X509", true },
//    			{ loginConfigs.get(LoginProtocolType.GRAM), AuthConfigType.KERBEROSE, "LoginProtocolType.GRAM is incompatible with LoginCredentialType.KERBEROSE", false },
//    			{ loginConfigs.get(LoginProtocolType.GRAM), AuthConfigType.TOKEN, "LoginProtocolType.GRAM is incompatible with LoginCredentialType.TOKEN", false },
//    			{ loginConfigs.get(LoginProtocolType.GRAM), AuthConfigType.PASSWORD, "LoginProtocolType.GRAM is incompatible with LoginCredentialType.PASSWORD", false },
//    			{ loginConfigs.get(LoginProtocolType.GRAM), AuthConfigType.LOCAL, "LoginProtocolType.GRAM is incompatible with LoginCredentialType.LOCAL", false },
    			
    			{ loginConfigs.get(LoginProtocolType.GSISSH), AuthConfigType.X509, "LoginProtocolType.GSISSH is compatible with LoginCredentialType.X509", true },
    			{ loginConfigs.get(LoginProtocolType.GSISSH), AuthConfigType.KERBEROSE, "LoginProtocolType.GSISSH is incompatible with LoginCredentialType.KERBEROSE", false },
    			{ loginConfigs.get(LoginProtocolType.GSISSH), AuthConfigType.TOKEN, "LoginProtocolType.GSISSH is incompatible with LoginCredentialType.TOKEN", false },
    			{ loginConfigs.get(LoginProtocolType.GSISSH), AuthConfigType.PASSWORD, "LoginProtocolType.GSISSH is incompatible with LoginCredentialType.PASSWORD", false },
    			{ loginConfigs.get(LoginProtocolType.GSISSH), AuthConfigType.LOCAL, "LoginProtocolType.GSISSH is incompatible with LoginCredentialType.LOCAL", false },
    			
    			{ loginConfigs.get(LoginProtocolType.SSH), AuthConfigType.X509, "LoginProtocolType.SSH is incompatible with LoginCredentialType.X509", false },
    			{ loginConfigs.get(LoginProtocolType.SSH), AuthConfigType.KERBEROSE, "LoginProtocolType.SSH is compatible with LoginCredentialType.KERBEROSE", false },
    			{ loginConfigs.get(LoginProtocolType.SSH), AuthConfigType.TOKEN, "LoginProtocolType.SSH is incompatible with LoginCredentialType.TOKEN", false },
    			{ loginConfigs.get(LoginProtocolType.SSH), AuthConfigType.PASSWORD, "LoginProtocolType.SSH is compatible with LoginCredentialType.PASSWORD", true },
    			{ loginConfigs.get(LoginProtocolType.SSH), AuthConfigType.LOCAL, "LoginProtocolType.SSH is incompatible with LoginCredentialType.LOCAL", false },
    			
    			{ loginConfigs.get(LoginProtocolType.LOCAL), AuthConfigType.X509, "LoginProtocolType.LOCAL is incompatible with LoginCredentialType.X509", false },
    			{ loginConfigs.get(LoginProtocolType.LOCAL), AuthConfigType.KERBEROSE, "LoginProtocolType.LOCAL is incompatible with LoginCredentialType.KERBEROSE", false },
    			{ loginConfigs.get(LoginProtocolType.LOCAL), AuthConfigType.TOKEN, "LoginProtocolType.LOCAL is incompatible with LoginCredentialType.TOKEN", false },
    			{ loginConfigs.get(LoginProtocolType.LOCAL), AuthConfigType.PASSWORD, "LoginProtocolType.LOCAL is incompatible with LoginCredentialType.PASSWORD", false },
    			{ loginConfigs.get(LoginProtocolType.LOCAL), AuthConfigType.LOCAL, "LoginProtocolType.SFTP is compatible with LoginCredentialType.LOCAL", true },
    	
    	};
    }

    @Test (groups={"model","system"}, dataProvider="loginLoginCredentialType")
    public void loginLoginCredentialTypeTest(LoginConfig loginConfig, AuthConfigType credentialType, String message, boolean assertionValue) 
    throws Exception 
    {	
		Assert.assertEquals(loginConfig.getProtocol().accepts(credentialType), assertionValue);
    }
}
