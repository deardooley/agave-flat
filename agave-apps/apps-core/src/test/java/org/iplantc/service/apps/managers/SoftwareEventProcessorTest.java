package org.iplantc.service.apps.managers;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.iplantc.service.apps.dao.AbstractDaoTest;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.dao.SoftwareEventDao;
import org.iplantc.service.apps.exceptions.SoftwareEventPersistenceException;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwareEvent;
import org.iplantc.service.apps.model.SoftwarePermission;
import org.iplantc.service.apps.model.enumerations.SoftwareEventType;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.transfer.model.enumerations.PermissionType;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Test(groups={"integration"})
public class SoftwareEventProcessorTest extends AbstractDaoTest {

	private static final Answer<List<String>> JOB_UUID_ANSWER = new Answer<List<String>>() {
		 private List<String> uuids;
		 
	     public List<String> answer(InvocationOnMock invocation) throws Throwable {
	         if (uuids == null) {
	        	 uuids = uuidGen(UUIDType.JOB, 5);
	         }
	         
	         return uuids;
	     }
	     
	     public List<String> uuidGen(UUIDType type, int quantity) {
	 		List<String> uuids = new ArrayList<String>();
	 		
	 		for (int i=0;i<quantity;i++) {
	 			uuids.add(new AgaveUUID(type).toString());
	 		}
	 		
	 		return uuids;
	 	}
	};
	
	private static final Answer<List<String>> USER_JOB_UUID_ANSWER = new Answer<List<String>>() {
		 private List<String> uuids;
		 
	     public List<String> answer(InvocationOnMock invocation) throws Throwable {
	         if (uuids == null) {
	        	 uuids = uuidGen(UUIDType.JOB, 2);
	         }
	         
	         return uuids;
	     }
	     
	     public List<String> uuidGen(UUIDType type, int quantity) {
	 		List<String> uuids = new ArrayList<String>();
	 		
	 		for (int i=0;i<quantity;i++) {
	 			uuids.add(new AgaveUUID(type).toString());
	 		}
	 		
	 		return uuids;
	 	}
	};
	
	private static final Answer<List<String>> SOFTWARE_UUID_ANSWER = new Answer<List<String>>() {
		 private List<String> uuids;
		 
	     public List<String> answer(InvocationOnMock invocation) throws Throwable {
	         if (uuids == null) {
	        	 uuids = uuidGen(UUIDType.APP, 2);
	         }
	         
	         return uuids;
	     }
	     
	     public List<String> uuidGen(UUIDType type, int quantity) {
	 		List<String> uuids = new ArrayList<String>();
	 		
	 		for (int i=0;i<quantity;i++) {
	 			uuids.add(new AgaveUUID(type).toString());
	 		}
	 		
	 		return uuids;
	 	}
	};
	
	@Override
	@BeforeClass
	protected void beforeClass() throws Exception {
		super.beforeClass();
	}

//	@BeforeMethod
//	protected void beforeMethod() {
////		reset(eventProcessor);
//	}
	
	@DataProvider
    protected Object[][] doJobEventsProvider() throws Exception {
		Software privateSoftware = createSoftware();
    	Software publicSoftware = createSoftware();
    	publicSoftware.setPubliclyAvailable(true);
    	
    	
    	ObjectNode privateSoftwareJson = (ObjectNode)mapper.createObjectNode();
    	privateSoftwareJson.set("app", mapper.readTree(privateSoftware.toJSON()));
    	
    	ObjectNode publicSoftwareJson = (ObjectNode)mapper.createObjectNode();
    	publicSoftwareJson.set("app", mapper.readTree(publicSoftware.toJSON()));
    	
    	List<Object[]> testCases = new ArrayList<Object[]>();
    	for (String createdBy: new String[] { SYSTEM_OWNER, SYSTEM_SHARE_USER, Settings.PUBLIC_USER_USERNAME, TENANT_ADMIN }) {
    		for (SoftwareEventType eventType: SoftwareEventType.values()) {
    			testCases.add(new Object[]{ publicSoftware, eventType, createdBy, publicSoftwareJson });
    			testCases.add(new Object[]{ privateSoftware, eventType, createdBy, privateSoftwareJson });
//    			break;
    		}
//    		break;
    	}
    	return testCases.toArray(new Object[][] {});
    }
    
	@Test(dataProvider="doJobEventsProvider", enabled=true)
	public void doJobEvents(Software software, SoftwareEventType eventType, String createdBy, ObjectNode appJson) 
	throws Throwable 
	{		
		ObjectMapper mapper = new ObjectMapper();
		SoftwareEventDao softwareEventDao = mock(SoftwareEventDao.class);
		software.setId((long)5);
		
		// mock db lookup
		SoftwareDao mockDao = mock(SoftwareDao.class);
		when(mockDao.getActiveJobsForSoftware(software)).then(JOB_UUID_ANSWER);
		
		List<String> uuids = JOB_UUID_ANSWER.answer(null);
		
		SoftwareEventProcessor eventProcessor = new SoftwareEventProcessor();
		eventProcessor.setEntityEventDao(softwareEventDao);
		eventProcessor.setDao(mockDao);
		SoftwareEventProcessor spy = spy(eventProcessor);
		
		spy.doJobEvents(software, eventType.name(), createdBy, appJson);
		
		verify(spy).doJobEvents(software, eventType.name(), createdBy, appJson);
		
		verify(mockDao).getActiveJobsForSoftware(software);
		
		for(String uuid: uuids) {
			
			verify(spy).fireNotification(uuid, "APP_" + eventType.name(), createdBy, 
					((ObjectNode)appJson.deepCopy().set("job", mapper.createObjectNode().put("id", uuid))));
		}
	}
	
	@DataProvider
    protected Object[][] doSystemEventsProvider() throws Exception {
		
		Software privateSoftware = createSoftware();
		Software publicSoftware = createSoftware();
    	publicSoftware.setPubliclyAvailable(true);
    	
    	ObjectNode privateSoftwareJson = (ObjectNode)mapper.createObjectNode();
    	privateSoftwareJson.set("app", mapper.readTree(privateSoftware.toJSON()));
    	
    	ObjectNode publicSoftwareJson = (ObjectNode)mapper.createObjectNode();
    	publicSoftwareJson.set("app", mapper.readTree(publicSoftware.toJSON()));
    	
    	
    	List<Object[]> testCases = new ArrayList<Object[]>();
    	for (String createdBy: new String[] { SYSTEM_OWNER, SYSTEM_SHARE_USER, Settings.PUBLIC_USER_USERNAME, TENANT_ADMIN }) {
    		for (SoftwareEventType eventType: SoftwareEventType.values()) {
    			testCases.add(new Object[]{ publicSoftware, eventType, createdBy, publicSoftwareJson });
    			testCases.add(new Object[]{ privateSoftware, eventType, createdBy, privateSoftwareJson });
//    			break;
    		}
//    		break;
    	}
    	return testCases.toArray(new Object[][] {});
    }
    
	@Test(dataProvider="doSystemEventsProvider", enabled=true)
	public void doSystemEvents(Software software, SoftwareEventType eventType, String createdBy, ObjectNode appJson) 
	throws Throwable 
	{		
		ObjectMapper mapper = new ObjectMapper();
		SoftwareEventDao softwareEventDao = mock(SoftwareEventDao.class);
		software.setId((long)5);
		
		SoftwareEventProcessor eventProcessor = new SoftwareEventProcessor();
		eventProcessor.setEntityEventDao(softwareEventDao);
		SoftwareEventProcessor spy = spy(eventProcessor);
		
		spy.doSystemEvents(software, eventType.name(), createdBy, appJson);
		
		verify(spy).doSystemEvents(software, eventType.name(), createdBy, appJson);
		
		verify(spy).fireNotification(eq(software.getExecutionSystem().getUuid()), eq("APP_" + eventType.name()), eq(createdBy), 
				eq((ObjectNode)appJson.deepCopy().set("system", mapper.readTree(software.getExecutionSystem().toJSON()))));
		
	}
	
	@DataProvider
    protected Object[][] processPermissionGrantEventProvider() throws Exception {
		Software privateSoftware = createSoftware();
		Software publicSoftware = createSoftware();
    	publicSoftware.setPubliclyAvailable(true);
    	
    	ObjectNode privateSoftwareJson = (ObjectNode)mapper.createObjectNode();
    	privateSoftwareJson.set("app", mapper.readTree(privateSoftware.toJSON()));
    	
    	ObjectNode publicSoftwareJson = (ObjectNode)mapper.createObjectNode();
    	publicSoftwareJson.set("app", mapper.readTree(publicSoftware.toJSON()));
    	
    	List<Object[]> testCases = new ArrayList<Object[]>();
    	for (String createdBy: new String[] { SYSTEM_OWNER, SYSTEM_SHARE_USER, SYSTEM_PUBLIC_USER, TENANT_ADMIN }) {
    		for (PermissionType permissionType: PermissionType.values()) {
    			if (permissionType == PermissionType.NONE) continue;
    			SoftwarePermission privateSoftwarePermission = new SoftwarePermission(privateSoftware, SYSTEM_UNSHARED_USER, permissionType);
    			ObjectNode privateSoftwarePermissionEventJson = mapper.createObjectNode();
    			privateSoftwarePermissionEventJson.setAll(privateSoftwareJson);
    			privateSoftwarePermissionEventJson.set("permission", mapper.readTree(privateSoftwarePermission.toJSON()));
    			
				testCases.add(new Object[]{ privateSoftware, 
						privateSoftwarePermission, 
						createdBy, 
						privateSoftwarePermissionEventJson});
				
				SoftwarePermission publicSoftwarePermission = new SoftwarePermission(publicSoftware, SYSTEM_UNSHARED_USER, permissionType);
				ObjectNode publicSoftwarePermissionEventJson = mapper.createObjectNode();
				publicSoftwarePermissionEventJson.setAll(publicSoftwareJson);
				publicSoftwarePermissionEventJson.set("permission", mapper.readTree(publicSoftwarePermission.toJSON()));
    			
    			testCases.add(new Object[]{ publicSoftware, 
    					publicSoftwarePermission, 
    					createdBy, 
    					publicSoftwarePermissionEventJson });
    			break;
    		}
    		break;
    	}
    	return testCases.toArray(new Object[][] {});
    }
	

	@Test(dataProvider="processPermissionGrantEventProvider", enabled=true)
	public void processPermissionGrantEvent(Software software, SoftwarePermission softwarePermission, String createdBy, ObjectNode appJson) 
	throws SoftwareEventPersistenceException
	{
		SoftwareEventDao softwareEventDao = mock(SoftwareEventDao.class);
		software.setId((long)5);
		
		
		// mock db lookup
		SoftwareDao mockDao = mock(SoftwareDao.class);
		when(mockDao.getActiveUserJobsForSoftware(software, softwarePermission.getUsername())).then(USER_JOB_UUID_ANSWER);
		
		SoftwareEventProcessor eventProcessor = new SoftwareEventProcessor();
		eventProcessor.setEntityEventDao(softwareEventDao);
		eventProcessor.setDao(mockDao);
		SoftwareEventProcessor spy = spy(eventProcessor);
		
		spy.processPermissionEvent(software, softwarePermission, createdBy);
		
		verify(spy).processPermissionEvent(software, softwarePermission, createdBy);
		
		// verify the permission history event was created
		verify(softwareEventDao).persist(any(SoftwareEvent.class));
		
		// verify the PERMISSION_* software event fired
		verify(spy).fireNotification(software.getUuid(), SoftwareEventType.PERMISSION_GRANT.name(), 
				createdBy, (ObjectNode)appJson);
		
		// verify that all apps get notified
		verify(spy).doUserJobEvents(software, SoftwareEventType.PERMISSION_GRANT.name(), softwarePermission.getUsername(), createdBy, appJson);
	}
	
	@DataProvider
    protected Object[][] processPermissionRevocationEventProvider() throws Exception {
		Software privateSoftware = createSoftware();
		Software publicSoftware = createSoftware();
    	publicSoftware.setPubliclyAvailable(true);
    	
    	ObjectNode privateSoftwareJson = (ObjectNode)mapper.createObjectNode();
    	privateSoftwareJson.set("app", mapper.readTree(privateSoftware.toJSON()));
    	
    	ObjectNode publicSoftwareJson = (ObjectNode)mapper.createObjectNode();
    	publicSoftwareJson.set("app", mapper.readTree(publicSoftware.toJSON()));
    	
    	List<Object[]> testCases = new ArrayList<Object[]>();
    	for (String createdBy: new String[] { SYSTEM_OWNER, SYSTEM_SHARE_USER, SYSTEM_PUBLIC_USER, TENANT_ADMIN }) {
    			
			SoftwarePermission privateSoftwarePermission = new SoftwarePermission(privateSoftware, SYSTEM_UNSHARED_USER, PermissionType.NONE);
			ObjectNode privateSoftwarePermissionEventJson = mapper.createObjectNode();
			privateSoftwarePermissionEventJson.setAll(privateSoftwareJson);
			privateSoftwarePermissionEventJson.set("permission", mapper.readTree(privateSoftwarePermission.toJSON()));
			
			testCases.add(new Object[]{ privateSoftware, 
					privateSoftwarePermission, 
					createdBy, 
					privateSoftwarePermissionEventJson});
			
			SoftwarePermission publicSoftwarePermission = new SoftwarePermission(publicSoftware, SYSTEM_UNSHARED_USER, PermissionType.NONE);
			ObjectNode publicSoftwarePermissionEventJson = mapper.createObjectNode();
			publicSoftwarePermissionEventJson.setAll(publicSoftwareJson);
			publicSoftwarePermissionEventJson.set("permission", mapper.readTree(publicSoftwarePermission.toJSON()));
			
			testCases.add(new Object[]{ publicSoftware, 
					publicSoftwarePermission, 
					createdBy, 
					publicSoftwarePermissionEventJson });
    			
    		break;
    	}
    	return testCases.toArray(new Object[][] {});
    }
	
	@Test(dataProvider="processPermissionRevocationEventProvider", enabled=true)
	public void processPermissionRevocationEvent(Software software, SoftwarePermission softwarePermission, String createdBy, ObjectNode appJson) 
	throws SoftwareEventPersistenceException
	{
		SoftwareEventDao softwareEventDao = mock(SoftwareEventDao.class);
		software.setId((long)5);
		
		
		// mock db lookup
		SoftwareDao mockDao = mock(SoftwareDao.class);
		when(mockDao.getActiveUserJobsForSoftware(software, softwarePermission.getUsername())).then(USER_JOB_UUID_ANSWER);
		
		SoftwareEventProcessor eventProcessor = new SoftwareEventProcessor();
		eventProcessor.setEntityEventDao(softwareEventDao);
		eventProcessor.setDao(mockDao);
		SoftwareEventProcessor spy = spy(eventProcessor);
		
		spy.processPermissionEvent(software, softwarePermission, createdBy);
		
		verify(spy).processPermissionEvent(software, softwarePermission, createdBy);
		
		// verify the permission history event was created
		verify(softwareEventDao).persist(any(SoftwareEvent.class));
		
		// verify the PERMISSION_* software event fired
		verify(spy).fireNotification(software.getUuid(), SoftwareEventType.PERMISSION_REVOKE.name(), 
				createdBy, (ObjectNode)appJson);
		
		// verify that all apps get notified
		verify(spy).doUserJobEvents(software, SoftwareEventType.PERMISSION_REVOKE.name(), softwarePermission.getUsername(), createdBy, appJson);
	}

	@Test(enabled=true)
	public void processPublishEvent() throws Throwable {
		
		Software privateSoftware = createSoftware();
		Software publishedSoftware = createSoftware();
		publishedSoftware.setPubliclyAvailable(true);
		
		SoftwareEventDao softwareEventDao = mock(SoftwareEventDao.class);
		
		
		// mock db lookup
		SoftwareDao mockDao = mock(SoftwareDao.class);
		when(mockDao.getPreviousVersionsOfPublshedSoftware(publishedSoftware)).thenReturn(SOFTWARE_UUID_ANSWER.answer(null));
		
		//.getPreviousVersionsOfPublshedSoftware(publishedSoftware)).
		
		SoftwareEventProcessor eventProcessor = new SoftwareEventProcessor();
		eventProcessor.setEntityEventDao(softwareEventDao);
		eventProcessor.setDao(mockDao);
		SoftwareEventProcessor spy = spy(eventProcessor);
		
		ObjectNode privateAppJson = (ObjectNode)mapper.createObjectNode().set("app", mapper.readTree(privateSoftware.toJSON()));
		ObjectNode publishedAppJson = (ObjectNode)mapper.createObjectNode().set("app", mapper.readTree(publishedSoftware.toJSON()));
		ObjectNode publishedAppSystemEventJson = (ObjectNode)mapper.createObjectNode().set("app", mapper.readTree(publishedSoftware.toJSON()));
		publishedAppSystemEventJson.set("system", mapper.readTree(publishedSoftware.getExecutionSystem().toJSON()));
		
		spy.processPublishEvent(privateSoftware, publishedSoftware, TENANT_ADMIN);
		
		verify(spy).processPublishEvent(privateSoftware, publishedSoftware, TENANT_ADMIN);
		
		// verify the old original app event fired
		verify(spy).fireNotification(privateSoftware.getUuid(), SoftwareEventType.PUBLISHED.name(), TENANT_ADMIN, privateAppJson);
		
		// verify the permission history event was created once for the old and new app each
		verify(softwareEventDao, times(2+SOFTWARE_UUID_ANSWER.answer(null).size())).persist(any(SoftwareEvent.class));
				
		// verify the published app event fired
		verify(spy).fireNotification(publishedSoftware.getUuid(), SoftwareEventType.CREATED.name(), TENANT_ADMIN, publishedAppJson);
		
		// verify the execution system event fired
		verify(spy).fireNotification(publishedSoftware.getExecutionSystem().getUuid(), "APP_" + SoftwareEventType.CREATED.name(), TENANT_ADMIN, publishedAppSystemEventJson);
		verify(spy).fireNotification(publishedSoftware.getExecutionSystem().getUuid(), "APP_" + SoftwareEventType.PUBLISHED.name(), TENANT_ADMIN, publishedAppSystemEventJson);
		
		// verify the previous software version events fired
		for (String previousPublishedAppRevisionUuid: SOFTWARE_UUID_ANSWER.answer(null)) {
			verify(spy).fireNotification(previousPublishedAppRevisionUuid, SoftwareEventType.REPUBLISHED.name(), TENANT_ADMIN, publishedAppJson.toString());
		}		
	}

	@Test(enabled=true)
	public void processCloneEvent() throws Throwable {
		Software privateSoftware = createSoftware();
		Software clonedSoftware = createSoftware();
		
		SoftwareEventDao softwareEventDao = mock(SoftwareEventDao.class);
		
		// mock db lookup
		SoftwareDao mockDao = mock(SoftwareDao.class);
		when(mockDao.getPreviousVersionsOfPublshedSoftware(clonedSoftware)).then(SOFTWARE_UUID_ANSWER);
		
		SoftwareEventProcessor eventProcessor = new SoftwareEventProcessor();
		eventProcessor.setEntityEventDao(softwareEventDao);
		eventProcessor.setDao(mockDao);
		SoftwareEventProcessor spy = spy(eventProcessor);
		
		ObjectNode privateSoftwareJson = (ObjectNode)mapper.createObjectNode().set("app", mapper.readTree(privateSoftware.toJSON()));
		ObjectNode clonedAppJson = (ObjectNode)mapper.createObjectNode().set("app", mapper.readTree(clonedSoftware.toJSON()));
		ObjectNode clonedSystemEventJson = (ObjectNode)mapper.createObjectNode().set("app", mapper.readTree(clonedSoftware.toJSON()));
		clonedSystemEventJson.set("system", mapper.readTree(clonedSoftware.getExecutionSystem().toJSON()));
		
		spy.processCloneEvent(privateSoftware, clonedSoftware, TENANT_ADMIN);
		
		verify(spy).processCloneEvent(eq(privateSoftware), eq(clonedSoftware), eq(TENANT_ADMIN));
		
		// verify the permission history event was created
		verify(softwareEventDao, times(2)).persist(any(SoftwareEvent.class));
		
		// verify the old original app event fired
		verify(spy).fireNotification(privateSoftware.getUuid(), SoftwareEventType.CLONED.name(), TENANT_ADMIN, privateSoftwareJson);
				
		// verify the cloned app event fired
		verify(spy).fireNotification(clonedSoftware.getUuid(), SoftwareEventType.CREATED.name(), TENANT_ADMIN, clonedAppJson);
//		verify(spy).fireNotification(eq(clonedSoftware.getUuid()), eq(SoftwareEventType.CREATED.name()), eq(TENANT_ADMIN), eq(appJson));
		
		// verify the execution system event fired
		verify(spy).fireNotification(eq(clonedSoftware.getExecutionSystem().getUuid()), eq("APP_" + SoftwareEventType.CREATED.name()), eq(TENANT_ADMIN), eq(clonedSystemEventJson));
		verify(spy).fireNotification(eq(clonedSoftware.getExecutionSystem().getUuid()), eq("APP_" + SoftwareEventType.CLONED.name()), eq(TENANT_ADMIN), eq(clonedSystemEventJson));
		
	}
	
	@DataProvider
	protected Object[][] processSystemUpdateEventProvider() {
		Object[][] testCases = new Object[SoftwareEventType.values().length][1];
		int i = 0;
		for (SoftwareEventType eventType: SoftwareEventType.values()) {
			testCases[i] = new Object[]{ eventType };
			i++;
		}
		
		return testCases;
	}

	@Test(dataProvider="processSystemUpdateEventProvider")
	public void processSoftwareUpdateEvent(SoftwareEventType eventType) throws Throwable {
		Software privateSoftware = createSoftware();
		
		// mock db lookup
		SoftwareDao mockDao = mock(SoftwareDao.class);
		SoftwareEventDao softwareEventDao = mock(SoftwareEventDao.class);
		
		SoftwareEventProcessor eventProcessor = new SoftwareEventProcessor();
		eventProcessor.setEntityEventDao(softwareEventDao);
		eventProcessor.setDao(mockDao);
		SoftwareEventProcessor spy = spy(eventProcessor);
		
		ObjectNode privateSoftwareJson = (ObjectNode)mapper.createObjectNode().set("app", mapper.readTree(privateSoftware.toJSON()));
		
		spy.processSoftwareContentEvent(privateSoftware, eventType, "Testing software update event handling", TENANT_ADMIN);
		
		verify(spy).processSoftwareContentEvent(privateSoftware, eventType, "Testing software update event handling", TENANT_ADMIN);
		
		// verify the history record was written
		verify(softwareEventDao).persist(any(SoftwareEvent.class));
		
		// verify the event was fired 
		verify(spy).fireNotification(privateSoftware.getUuid(), eventType.name(), TENANT_ADMIN, privateSoftwareJson);
				
				
		if (eventType.isStatusEvent()) {
			// verify the cloned app event fired on the system
			verify(spy).doSystemEvents(privateSoftware, eventType.name(), TENANT_ADMIN, privateSoftwareJson);
		}
	}
}
