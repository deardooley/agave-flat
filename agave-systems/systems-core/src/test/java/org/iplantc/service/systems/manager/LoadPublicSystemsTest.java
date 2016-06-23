package org.iplantc.service.systems.manager;

import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.SystemsModelTestCommon;
import org.iplantc.service.transfer.RemoteDataClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import java.io.IOException;

public class LoadPublicSystemsTest extends SystemsModelTestCommon {
	private static String SYSTEM_OWNER = "dooley";
	
	private SystemDao dao = new SystemDao();
	private SystemManager systemManager = new SystemManager();
	
	@BeforeClass
	public void beforeClass() throws Exception
	{
		super.beforeClass();
	}
	
	@BeforeMethod
	public void beforeMethod() throws Exception {
		clearSystems();
	}
	
	@AfterMethod
	public void afterMethod() throws Exception {
		clearSystems();
	}
	
	@DataProvider(name="loadPublicSystemProvider")
	public Object[][] loadPublicSystemProvider()
	{
		return new Object[][] {
				{ EXECUTION_SYSTEM_TEMPLATE_DIR + "/lonestar.tacc.teragrid.org.json", true},
	//			{ EXECUTION_SYSTEM_TEMPLATE_DIR + "/trestles.sdsc.teragrid.org.json", false},
				{ EXECUTION_SYSTEM_TEMPLATE_DIR + "/stampede.tacc.utexas.edu.json", false},
				{ STORAGE_SYSTEM_TEMPLATE_DIR + "/data.iplantcollaborative.org.json", true},
	//			{ STORAGE_SYSTEM_TEMPLATE_DIR + "/sftp.example.com.json", false},
		};
	}
	
	@Test(dataProvider="loadPublicSystemProvider",enabled=false)
	public void loadPublicSystem(String systemJsonFile, boolean setAsGlobalDefault) 
	throws JSONException, IOException, SystemException, PermissionException
	{
		
		JSONObject json = jtd.getTestDataObject(systemJsonFile);
		RemoteSystem system = systemManager.parseSystem(json, SYSTEM_OWNER, null);
		system.setOwner(SYSTEM_OWNER);
		system.setAvailable(true);
		system.setPubliclyAvailable(true);
		system.setGlobalDefault(setAsGlobalDefault);
		dao.persist(system);
		
		Assert.assertNotNull(system.getId(), "System " + system.getSystemId() + " was not persisted");
		
		RemoteDataClient client = null;
		try {
			client = system.getRemoteDataClient();
			client.authenticate();
			client.disconnect();
		} catch (Exception e) {
			Assert.fail("Failed to authenticate using storage config for " + system.getSystemId(), e);
		}
		finally {
			try {client.disconnect();} catch (Exception e) {}
		}
	}
}
