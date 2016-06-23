package org.iplantc.service.apps.managers;

import static org.iplantc.service.apps.model.JSONTestDataUtil.TEST_OWNER;
import static org.iplantc.service.apps.model.JSONTestDataUtil.TEST_SOFTWARE_SYSTEM_FILE;

import org.iplantc.service.apps.dao.AbstractDaoTest;
import org.iplantc.service.apps.model.Software;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ApplicationManagerTest extends AbstractDaoTest 
{
	private Software privateSoftware;
	private Software publicSoftware;
	private Software clonedSoftware;
	
	@AfterClass
	public void tearDown() throws Exception 
	{	
		clearSystems();
		clearSoftware();
	}
	
	@BeforeMethod
	public void beforeMethod() throws Exception  
	{
		initSystems();
		initSoftware();
	}

	@AfterMethod
	public void afterMethod() throws Exception 
	{
		clearSystems();
		clearSoftware();
	}
	
	protected void initSoftware() throws Exception
	{
		clearSoftware(); 
		
		JSONObject json = jtd.getTestDataObject(TEST_SOFTWARE_SYSTEM_FILE);
		software = Software.fromJSON(json, TEST_OWNER);
		software.setExecutionSystem(privateExecutionSystem);
		software.setOwner(TEST_OWNER);
		software.setVersion(software.getVersion());
	}
	@DataProvider
	public Object[][] publishApplicationProvider()
	{
		// 
		return new Object[][] {
				{ }	
		};
	}
	
	@Test
	public void publishApplication() 
	{
		// publish public app should fail
		// publish unshared app should fail
		// publish app without push role should fail
		// publish to private exe system should fail
		// publish to private storage system should fail
		// publish to null storage system without default storage system should fail
		// republish private app should increment revision
		// publish should create zip archive
	}
	
	@DataProvider
	public Object[][] clonePublicApplicationProvider()
	{
		// zip app folder and save checksum
		// copy app folder to remote deploymentPath of public App
		// add public storage and exe system
		// add private storage and exe system
		// add public app
		
		return new Object[][] {
				{}
		};
	}
	
//	@Test
//	public void clonePublicApplication() {
		// name and id the same fail
		// storage and exe the same fail
		// storage or exe the same ok
		// storage or exe no role fail
		// exe no publish role fail
		// destinationPath no pem fail
		// destinationPath exists fail
		// revision set to zero
		// history event written to public and cloned system
		// notifications sent
		// new app owned by username
		// new app available
//		throw new RuntimeException("Test not implemented");
//	}
	
	@DataProvider
	public Object[][] clonePrivateApplicationProvider()
	{
		// copy app folder to remote deploymentPath of public App
		// add public storage and exe system
		// add private storage and exe system
		// add public app
		
		return new Object[][] {
				{}
		};
	}

//	@Test
//	public void clonePrivateApplication() {
	// clone public app:
		// name and id the same fail
		// storage and exe the same ok
		// storage or exe the same ok
		// storage or exe no role fail
		// exe no publish role fail
		// destinationPath no pem fail
		// destinationPath exists fail
		// revision set to zero
		// history event written to public and cloned system
		// notifications sent
		// new app owned by username
		// new app available
	
//		ApplicationManager.clonePrivateApplication(softwarePrivateSystem, username, name, version, executionSystem)
//	}
//

}
