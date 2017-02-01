package org.iplantc.service.apps.managers;

import static org.iplantc.service.apps.model.JSONTestDataUtil.TEST_OWNER;
import static org.iplantc.service.apps.model.JSONTestDataUtil.TEST_SOFTWARE_SYSTEM_FILE;

import java.io.IOException;
import java.util.List;

import org.iplantc.service.apps.dao.AbstractDaoTest;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.exceptions.SoftwareResourceException;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwarePermission;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.testng.Assert;
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
	
	/**
	 * Tests that an application is semantically identical before and after update.
	 * @throws Exception 
	 */
	@Test
	public void processSoftwareUpdatesExistingSoftware() 
	throws Exception
	{
		try {
			
			JSONObject json = jtd.getTestDataObject(TEST_SOFTWARE_SYSTEM_FILE);
			Software oldSoftware = Software.fromJSON(json, TEST_OWNER);
			oldSoftware.setExecutionSystem(privateExecutionSystem);
			oldSoftware.setOwner(TEST_OWNER);
			oldSoftware.setVersion(software.getVersion());
			
			SoftwareDao.persist(oldSoftware);
			ApplicationManager manager = Mockito.spy(new ApplicationManager());
			
			Mockito.doReturn(Boolean.TRUE).when(manager).validateSoftwareDependencies(Mockito.any(String.class), Mockito.any(Software.class));
			
			Software newSoftware = manager.processSoftware(oldSoftware, json, oldSoftware.getOwner());
			
			Assert.assertEquals(newSoftware.getUuid(), oldSoftware.getUuid(), "UUID should be the same before and after processing");
			
			Assert.assertEquals(newSoftware.getCreated().toString(), oldSoftware.getCreated().toString(), "created timestamp should be the same before and after processing");
			
			Assert.assertTrue(newSoftware.getLastUpdated().after(oldSoftware.getLastUpdated()), "lastUpdated timestamp should not be equal before and after processing");

			Assert.assertTrue(newSoftware.getRevisionCount() == (oldSoftware.getRevisionCount() + 1), "revision count was not incremented on update");
			
			for (SoftwarePermission oldPem : oldSoftware.getPermissions()) {
				Assert.assertTrue(newSoftware.getPermissions().contains(oldPem), "All permissions should be carried over after processing.");
			}
			
			SoftwareDao.replace(oldSoftware, newSoftware);
			
			// search for new and old software objects
			Software oldSoftwareResult = SoftwareDao.get(oldSoftware.getId());
			Software newSoftwareResult = SoftwareDao.get(newSoftware.getId());
			
			Assert.assertNull(oldSoftwareResult, "Search for old software by id should return null.");
			
			Assert.assertNotNull(newSoftwareResult, "Search for new software by id should return new software.");
			
			
			newSoftwareResult = SoftwareDao.getSoftwareByUniqueName(newSoftwareResult.getUniqueName());
			
			Assert.assertNotNull(newSoftwareResult, "Search for new software by unique name should return new software.");
			
			
			List<Software> nameResults = SoftwareDao.getByName(newSoftware.getName(), false, true);
			
			Assert.assertEquals(nameResults.size(), 1, "Search for new software by name should return only the new software object.");
			
			Assert.assertNotNull(newSoftwareResult, "Search for new software by id should return new software.");
			
			for (SoftwarePermission oldPem : oldSoftware.getPermissions()) {
				Assert.assertTrue(newSoftwareResult.getPermissions().contains(oldPem), "All permissions should be carried over after processing.");
			}
		}
		catch (JSONException | IOException e) {
			Assert.fail("No exception should be thrown updating a valid app with a new one.", e);
		}
	}
	
	
	/**
	 * Tests that an application is semantically identical before and after multiple updates.
	 * @throws Exception 
	 */
	@Test
	public void processSoftwareMultipleTimesUpdatesExistingSoftware() 
	throws Exception
	{
		try {
			
			JSONObject json = jtd.getTestDataObject(TEST_SOFTWARE_SYSTEM_FILE);
			Software oldSoftware = Software.fromJSON(json, TEST_OWNER);
			oldSoftware.setExecutionSystem(privateExecutionSystem);
			oldSoftware.setOwner(TEST_OWNER);
			oldSoftware.setVersion(software.getVersion());
			
			SoftwareDao.persist(oldSoftware);
			ApplicationManager manager = Mockito.spy(new ApplicationManager());
			
			Mockito.doReturn(Boolean.TRUE).when(manager).validateSoftwareDependencies(Mockito.any(String.class), Mockito.any(Software.class));
			
			for (int i=0; i<20; i++) {
				Software newSoftware = manager.processSoftware(oldSoftware, json, oldSoftware.getOwner());
				
				Assert.assertEquals(newSoftware.getUuid(), oldSoftware.getUuid(), "UUID should be the same before and after processing");
				
				Assert.assertEquals(newSoftware.getCreated().toString(), oldSoftware.getCreated().toString(), "created timestamp should be the same before and after processing");
				
				Assert.assertTrue(newSoftware.getLastUpdated().after(oldSoftware.getLastUpdated()), "lastUpdated timestamp should not be equal before and after processing");
	
				Assert.assertTrue(newSoftware.getRevisionCount() == (i + 2), "revision count was not incremented on update");
				
				for (SoftwarePermission oldPem : oldSoftware.getPermissions()) {
					Assert.assertTrue(newSoftware.getPermissions().contains(oldPem), "All permissions should be carried over after processing.");
				}
				
				SoftwareDao.replace(oldSoftware, newSoftware);
				
				// search for new and old software objects
				Software oldSoftwareResult = SoftwareDao.get(oldSoftware.getId());
				Software newSoftwareResult = SoftwareDao.get(newSoftware.getId());
				
				Assert.assertNull(oldSoftwareResult, "Search for old software by id should return null.");
				
				Assert.assertNotNull(newSoftwareResult, "Search for new software by id should return new software.");
				
				
				newSoftwareResult = SoftwareDao.getSoftwareByUniqueName(newSoftwareResult.getUniqueName());
				
				Assert.assertNotNull(newSoftwareResult, "Search for new software by unique name should return new software.");
				
				
				List<Software> nameResults = SoftwareDao.getByName(newSoftware.getName(), false, true);
				
				Assert.assertEquals(nameResults.size(), 1, "Search for new software by name should return only the new software object.");
				
				Assert.assertNotNull(newSoftwareResult, "Search for new software by id should return new software.");
				
				for (SoftwarePermission oldPem : oldSoftware.getPermissions()) {
					Assert.assertTrue(newSoftwareResult.getPermissions().contains(oldPem), "All permissions should be carried over after processing.");
				}
				
				// update oldSoftware to latest version
				oldSoftware = newSoftware;
			}
		}
		catch (JSONException | IOException e) {
			Assert.fail("No exception should be thrown updating a valid app with a new one.", e);
		}
	}
			
}
