package org.iplantc.service.monitor.model;

import java.io.IOException;

import org.iplantc.service.monitor.AbstractMonitorTest;
import org.iplantc.service.monitor.TestDataHelper;
import org.iplantc.service.monitor.exceptions.MonitorException;
import org.iplantc.service.systems.model.RemoteSystem;
import org.json.JSONException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MonitorTest extends AbstractMonitorTest {

	@BeforeClass
	protected void beforeClass() throws Exception
	{
		super.beforeClass();
	}
	
	@Test
	public void constructMonitor()
	{
		Monitor monitor = new Monitor();
		Assert.assertNotNull(monitor.getUuid(), "UUID not set on instantiation.");
		Assert.assertNotNull(monitor.getTenantId(), "Tenant id not set on instantiation.");
		Assert.assertNotNull(monitor.getCreated(), "Creation date not set on instantiation.");
		Assert.assertNotNull(monitor.getLastUpdated(), "Last updated date not set on instantiation.");
		Assert.assertNotNull(monitor.getNextUpdateTime(), "Next updated date not set on instantiation.");
	}

	@DataProvider(name="initMonitorStringIntStringProvider")
	private Object[][] initMonitorStringIntStringProvider()
	{
		return new Object[][] {
				{ privateStorageSystem, "Valid private storage system should be accepted", false },
				{ publicStorageSystem, "Valid public storage system should be accepted", false },
				{ privateExecutionSystem, "Valid private execution system should be accepted", false },
				{ publicExecutionSystem, "Valid public execution system should be accepted", false },
				{ sharedExecutionSystem, "Valid shared execution system should be accepted", false },
				{ null, "null system should throw an exception", true }
		};
	}
	
	@Test(dependsOnMethods={"constructMonitor"}, dataProvider="initMonitorStringIntStringProvider")
	public void initMonitorStringIntString(RemoteSystem system, String message, boolean shouldThrowException) 
	{
		try 
		{
			Monitor notif = new Monitor(system, 5, TEST_USER);
			Assert.assertNotNull(notif.getUuid(), "UUID not set on instantiation.");
			Assert.assertNotNull(notif.getCreated(), "Creation date not set on instantiation.");
			Assert.assertNotNull(notif.getLastUpdated(), "Last updated date not set on instantiation.");
		} 
		catch (Exception e) 
		{
			if (!shouldThrowException) {
				Assert.fail(message, e);
			}
		}
	}
	
	@DataProvider(name="setMonitorFrequencyTestProvider")
	private Object[][] setMonitorFrequencyTestProvider()
	{
		return new Object[][] {
				{ 0, "0 frequency should throw exception", true },
				{ 4, "4 frequency should throw exception", true },
				{ -1, "-1 frequency should throw exception", true },
				{ 5, "5 or greater frequency should be accepted", false },
				{ 6, "5 or greater frequency should throw exception", false }
		};
	}
	
	@Test(dependsOnMethods={"initMonitorStringIntString"}, dataProvider="setMonitorFrequencyTestProvider")
	public void setMonitorFrequencyTest(int frequency, String message, boolean shouldThrowException) 
	{
		try 
		{
			new Monitor(publicStorageSystem, frequency, TEST_USER);
		} 
		catch (Exception e) 
		{
			if (!shouldThrowException) {
				Assert.fail(message, e);
			}
		}
	}
	
	@DataProvider(name="fromJSONProvider")
	private Object[][] fromJSONProvider() throws JSONException, IOException
	{
		ObjectNode jsonExecutionMonitorNoSystem = (ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR);
		ObjectNode jsonExecutionMonitorNoFrequency = (ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR);
		ObjectNode jsonExecutionMonitorNoUpdateSystemStatus = (ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR);
		ObjectNode jsonExecutionMonitorNoInternalUsername = (ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR);
		jsonExecutionMonitorNoSystem.remove("target");
		jsonExecutionMonitorNoFrequency.remove("system");
		jsonExecutionMonitorNoUpdateSystemStatus.remove("updateSystemStatus");
		jsonExecutionMonitorNoInternalUsername.remove("internalUsername");
		
		return new Object[][] {
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR), "Valid monitor json should parse", false },
			{ jsonExecutionMonitorNoSystem, "Missing system should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", ""), "Empty target should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", mapper.createObjectNode()), "Object for target should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", mapper.createArrayNode()), "Array for target should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", 5), "Integer for target should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", 5.5), "Decimal for target should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", publicStorageSystem.getSystemId()), "Public storage system should not throw an exception", false },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", publicExecutionSystem.getSystemId()), "Public execution system should not throw an exception", false },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", privateExecutionSystem.getSystemId()), "Private execution system should not throw an exception", false },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", privateStorageSystem.getSystemId()), "Private storage system should not throw an exception", false },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", sharedExecutionSystem.getSystemId()), "Shared execution system should not throw an exception", false },
			

			{ jsonExecutionMonitorNoFrequency, "Missing frequency should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("frequency", ""), "Empty frequency should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("frequency", mapper.createObjectNode()), "Object for frequency should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("frequency", mapper.createArrayNode()), "Array for frequency should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("frequency", 5), "Integer for frequency should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("frequency", 5.5), "Decimal for frequency should throw exception", true },
			
			{ jsonExecutionMonitorNoUpdateSystemStatus, "Missing updateSystemStatus should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("updateSystemStatus", ""), "Empty updateSystemStatus should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("updateSystemStatus", mapper.createObjectNode()), "Object for updateSystemStatus should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("updateSystemStatus", mapper.createArrayNode()), "Array for updateSystemStatus should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("updateSystemStatus", 5), "Integer for updateSystemStatus should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("updateSystemStatus", 5.5), "Decimal for updateSystemStatus should throw exception", true },
			
			{ jsonExecutionMonitorNoInternalUsername, "Missing internalUsername should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("internalUsername", ""), "Empty internalUsername should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("internalUsername", mapper.createObjectNode()), "Object for internalUsername should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("internalUsername", mapper.createArrayNode()), "Array for internalUsername should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("internalUsername", 5), "Integer for internalUsername should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("internalUsername", 5.5), "Decimal for internalUsername should throw exception", true },
			
		};
	}

	@Test(dependsOnMethods={"setMonitorFrequencyTest"}, dataProvider="fromJSONProvider")
	public void fromJSON(JsonNode json, String message, boolean shouldThrowException)
	{
		try 
		{
			Monitor.fromJSON(json, null, TEST_USER);
		} 
		catch (MonitorException e)
		{
			if (!shouldThrowException) {
				Assert.fail(message, e);
			}
		}
		catch (Exception e) 
		{
			Assert.fail(message, e);
		}
	}
	
	@DataProvider(name="permissionTestProvider")
	private Object[][] permissionTestProvider() throws JSONException, IOException
	{
		return new Object[][] {
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", publicStorageSystem.getSystemId()), "Public storage system should not throw an exception", false },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", publicExecutionSystem.getSystemId()), "Public execution system should not throw an exception", false },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", privateExecutionSystem.getSystemId()), "Private execution system user does not have a role on should not throw an exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", privateStorageSystem.getSystemId()), "Private storage system user does not have a role on should throw an exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", sharedExecutionSystem.getSystemId()), "Shared storage system user has a role on should not throw an exception", false },
		};
	}
	
	@Test(dependsOnMethods={"fromJSON"}, dataProvider="permissionTestProvider")
	public void permissionTest(JsonNode json, String message, boolean shouldThrowException)
	{
		try 
		{
			Monitor.fromJSON(json, null, TestDataHelper.SYSTEM_SHARE_USER);
		} 
		catch (MonitorException e)
		{
			if (!shouldThrowException) {
				Assert.fail(message, e);
			}
		}
		catch (Exception e) 
		{
			Assert.fail(message, e);
		}
	}
}
