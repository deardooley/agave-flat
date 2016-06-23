package org.iplantc.service.monitor.dao;

import static org.iplantc.service.monitor.TestDataHelper.SYSTEM_OWNER;
import static org.iplantc.service.monitor.TestDataHelper.SYSTEM_SHARE_USER;

import java.util.List;

import org.iplantc.service.monitor.AbstractMonitorTest;
import org.iplantc.service.monitor.events.DomainEntityEvent;
import org.iplantc.service.monitor.events.DomainEntityEventDao;
import org.iplantc.service.monitor.exceptions.MonitorException;
import org.iplantc.service.monitor.model.Monitor;
import org.iplantc.service.monitor.model.enumeration.MonitorEventType;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class DomainEntityEventDaoTest extends AbstractMonitorTest { 
	
	private DomainEntityEventDao systemHistoryEventDao;
	
	@BeforeClass
	protected void beforeClass() throws Exception
	{
		super.beforeClass();
		systemHistoryEventDao = new DomainEntityEventDao();
	}
	
	@AfterClass
	public void afterClass() throws MonitorException {
		super.afterClass();
	}

	@AfterMethod
	public void afterMethod() throws Exception
	{
		clearMonitors();
	}
	
	@Test
	public void persist() throws Exception
	{
		DomainEntityEvent entityEvent = new DomainEntityEvent(createStorageMonitor().getUuid(), 
				MonitorEventType.CREATED, MonitorEventType.CREATED.getDescription(), SYSTEM_OWNER );
		systemHistoryEventDao.persist(entityEvent);
		Assert.assertNotNull(entityEvent.getId(), "system event did not persist.");
	}

	@Test(dependsOnMethods={"persist"})
	public void delete() throws Exception
	{
		DomainEntityEvent entityEvent = new DomainEntityEvent(createStorageMonitor().getUuid(), MonitorEventType.CREATED, SYSTEM_OWNER);
		systemHistoryEventDao.persist(entityEvent);
		Assert.assertNotNull(entityEvent.getId(), "system event did not persist.");
		
		systemHistoryEventDao.delete(entityEvent);
		DomainEntityEvent userPem = systemHistoryEventDao.getEntityEventByUuid(entityEvent.getEntity());
		Assert.assertNull(userPem, "A system event should be returned after deleting.");
	}

	@Test(dependsOnMethods={"delete"})
	public void getEntityEventByEntityUuid() throws Exception
	{
		Monitor system = createStorageMonitor();
		DomainEntityEvent entityEvent = new DomainEntityEvent(system.getUuid(), MonitorEventType.CREATED, SYSTEM_OWNER);
		systemHistoryEventDao.persist(entityEvent);
		Assert.assertNotNull(entityEvent.getId(), "system event did not persist.");
		
		Monitor system2 = createStorageMonitor();
		DomainEntityEvent entityEvent2 = new DomainEntityEvent(system2.getUuid(), MonitorEventType.CREATED, SYSTEM_OWNER);
		systemHistoryEventDao.persist(entityEvent2);
		Assert.assertNotNull(entityEvent2.getId(), "system event did not persist.");
		
		
		List<DomainEntityEvent> pems = systemHistoryEventDao.getEntityEventByEntityUuid(entityEvent.getEntity());
		Assert.assertNotNull(pems, "getBytagId did not return any permissions.");
		Assert.assertEquals(pems.size(), 1, "getBytagId did not return the correct number of permissions.");
		Assert.assertFalse(pems.contains(entityEvent2), "getBytagId returned a permission from another system.");
	}

	@Test(dependsOnMethods={"getEntityEventByEntityUuid"})
	public void getAllEntityEventWithStatusForEntityUuid() throws Exception
	{
		Monitor system = createStorageMonitor();
		DomainEntityEvent entityEvent1 = new DomainEntityEvent(system.getUuid(), MonitorEventType.CREATED, SYSTEM_OWNER);
		systemHistoryEventDao.persist(entityEvent1);
		Assert.assertNotNull(entityEvent1.getId(), "system event 1 did not persist.");
		
		DomainEntityEvent entityEvent2 = new DomainEntityEvent(system.getUuid(), MonitorEventType.UPDATED, SYSTEM_SHARE_USER);
		systemHistoryEventDao.persist(entityEvent2);
		Assert.assertNotNull(entityEvent2.getId(), "system event 2 did not persist.");
		
		List<DomainEntityEvent> results = systemHistoryEventDao.getAllEntityEventWithStatusForEntityUuid(system.getUuid(), MonitorEventType.UPDATED);
		Assert.assertNotNull(results, "getAllEntityEventWithStatusForEntityUuid did not return the status events for the system.");
		Assert.assertEquals(results.size(), 1, "getAllEntityEventWithStatusForEntityUuid did not return the status events for the system.");
		Assert.assertEquals(results.get(0).getUuid(), entityEvent2.getUuid(), "getAllEntityEventWithStatusForEntityUuid did not return the correct system event for the user.");
	}
}
