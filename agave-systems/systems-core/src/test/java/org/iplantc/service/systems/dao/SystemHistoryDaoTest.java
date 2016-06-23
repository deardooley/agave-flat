package org.iplantc.service.systems.dao;

import java.util.List;

import org.iplantc.service.systems.events.SystemHistoryEvent;
import org.iplantc.service.systems.events.SystemHistoryEventDao;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.JSONTestDataUtil;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.SystemsModelTestCommon;
import org.iplantc.service.systems.model.enumerations.SystemEventType;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SystemHistoryDaoTest extends SystemsModelTestCommon { 
	
	private SystemHistoryEventDao systemHistoryEventDao;
	
	@BeforeClass
	protected void beforeClass() throws Exception
	{
		super.beforeClass();
		systemHistoryEventDao = new SystemHistoryEventDao();
	}
	
	@AfterClass
	protected void afterClass() throws Exception {
		clearSystems();
	}

	@AfterMethod
	public void afterMethod() throws Exception
	{
		clearSystems();
	}
	
	private ExecutionSystem createExecutionSystem() throws Exception {
		ExecutionSystem system = ExecutionSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE));
		system.setOwner(SYSTEM_OWNER);
		
		return system;
	}
	
	private StorageSystem createStorageSystem() throws Exception {
		StorageSystem system = StorageSystem.fromJSON(jtd.getTestDataObject(JSONTestDataUtil.TEST_STORAGE_SYSTEM_FILE));
		system.setOwner(SYSTEM_OWNER);
		
		return system;
	}

	@Test
	public void persist() throws Exception
	{
		SystemHistoryEvent historyEvent = new SystemHistoryEvent(createStorageSystem().getUuid(), 
				SystemEventType.CREATED, SystemEventType.CREATED.getDescription(), SYSTEM_OWNER );
		systemHistoryEventDao.persist(historyEvent);
		Assert.assertNotNull(historyEvent.getId(), "system event did not persist.");
	}

	@Test(dependsOnMethods={"persist"})
	public void delete() throws Exception
	{
		SystemHistoryEvent historyEvent = new SystemHistoryEvent(createStorageSystem().getUuid(), SystemEventType.CREATED, SYSTEM_OWNER);
		systemHistoryEventDao.persist(historyEvent);
		Assert.assertNotNull(historyEvent.getId(), "system event did not persist.");
		
		systemHistoryEventDao.delete(historyEvent);
		SystemHistoryEvent userPem = systemHistoryEventDao.getEntityEventByUuid(historyEvent.getEntity());
		Assert.assertNull(userPem, "A system event should be returned after deleting.");
	}

	@Test(dependsOnMethods={"delete"})
	public void getEntityEventByEntityUuid() throws Exception
	{
		RemoteSystem system = createStorageSystem();
		SystemHistoryEvent historyEvent = new SystemHistoryEvent(system.getUuid(), SystemEventType.CREATED, SYSTEM_OWNER);
		systemHistoryEventDao.persist(historyEvent);
		Assert.assertNotNull(historyEvent.getId(), "system event did not persist.");
		
		RemoteSystem system2 = createExecutionSystem();
		SystemHistoryEvent historyEvent2 = new SystemHistoryEvent(system2.getUuid(), SystemEventType.CREATED, SYSTEM_OWNER);
		systemHistoryEventDao.persist(historyEvent2);
		Assert.assertNotNull(historyEvent2.getId(), "system event did not persist.");
		
		
		List<SystemHistoryEvent> pems = systemHistoryEventDao.getEntityEventByEntityUuid(historyEvent.getEntity());
		Assert.assertNotNull(pems, "getBytagId did not return any permissions.");
		Assert.assertEquals(pems.size(), 1, "getBytagId did not return the correct number of permissions.");
		Assert.assertFalse(pems.contains(historyEvent2), "getBytagId returned a permission from another system.");
	}

	@Test(dependsOnMethods={"getEntityEventByEntityUuid"})
	public void getAllEntityEventWithStatusForEntityUuid() throws Exception
	{
		RemoteSystem system = createStorageSystem();
		SystemHistoryEvent historyEvent1 = new SystemHistoryEvent(system.getUuid(), SystemEventType.CREATED, SYSTEM_OWNER);
		systemHistoryEventDao.persist(historyEvent1);
		Assert.assertNotNull(historyEvent1.getId(), "system event 1 did not persist.");
		
		SystemHistoryEvent historyEvent2 = new SystemHistoryEvent(system.getUuid(), SystemEventType.UPDATED, SYSTEM_SHARE_USER);
		systemHistoryEventDao.persist(historyEvent2);
		Assert.assertNotNull(historyEvent2.getId(), "system event 2 did not persist.");
		
		List<SystemHistoryEvent> results = systemHistoryEventDao.getAllEntityEventWithStatusForEntityUuid(system.getUuid(), SystemEventType.UPDATED);
		Assert.assertNotNull(results, "getAllEntityEventWithStatusForEntityUuid did not return the status events for the system.");
		Assert.assertEquals(results.size(), 1, "getAllEntityEventWithStatusForEntityUuid did not return the status events for the system.");
		Assert.assertEquals(results.get(0).getUuid(), historyEvent2.getUuid(), "getAllEntityEventWithStatusForEntityUuid did not return the correct system event for the user.");
	}
}
