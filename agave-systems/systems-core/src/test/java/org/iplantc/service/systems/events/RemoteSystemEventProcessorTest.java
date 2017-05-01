package org.iplantc.service.systems.events;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.iplantc.service.common.exceptions.EntityEventPersistenceException;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.common.uuid.UniqueId;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.JSONTestDataUtil;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.SystemRole;
import org.iplantc.service.systems.model.SystemsModelTestCommon;
import org.iplantc.service.systems.model.enumerations.RoleType;
import org.iplantc.service.systems.model.enumerations.SystemEventType;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Test(groups={"integration"})
public class RemoteSystemEventProcessorTest extends SystemsModelTestCommon {

	private static final Answer<List<String>> APP_UUID_ANSWER = new Answer<List<String>>() {
		 private List<String> uuids;
		 
	     public List<String> answer(InvocationOnMock invocation) throws Throwable {
	         if (uuids == null) {
	        	 uuids = uuidGen(UUIDType.SYSTEM, 5);
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
	
	@BeforeClass
	protected void beforeClass() throws Exception {
		super.beforeClass();
	}

//	private List<String> uuidGen(UUIDType type, int quantity) {
//		List<String> uuids = new ArrayList<String>();
//		
//		for (int i=0;i<quantity;i++) {
//			uuids.add(new AgaveUUID(type).toString());
//		}
//		
//		return uuids;
//	}
	
	@BeforeMethod
	protected void beforeMethod() {
//		reset(eventProcessor);
	}
	
	private RemoteSystem getPrivateStorageSystem() throws Exception {	
        RemoteSystem privateStorageSystem = StorageSystem.fromJSON( jtd.getTestDataObject(
                JSONTestDataUtil.TEST_STORAGE_SYSTEM_FILE));
        privateStorageSystem.setOwner(SYSTEM_OWNER);
        privateStorageSystem.setSystemId(new UniqueId().getStringId());
        return privateStorageSystem;
    }
    
    private RemoteSystem getPrivateExecutionSystem() throws Exception {   
        RemoteSystem privateExecutionSystem = ExecutionSystem.fromJSON( jtd.getTestDataObject(
                JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE));
        privateExecutionSystem.setOwner(SYSTEM_OWNER);
        privateExecutionSystem.setSystemId(new UniqueId().getStringId());
        return privateExecutionSystem;
    }

    @DataProvider
    protected Object[][] doSoftwareEventsProvider() throws Exception {
    	RemoteSystem storageSystem = getPrivateStorageSystem();
    	RemoteSystem executionSystem = getPrivateExecutionSystem();
    	
    	ObjectNode storageSystemJson = (ObjectNode)new ObjectMapper().readTree(storageSystem.toJSON());
    	ObjectNode executionSystemJson = (ObjectNode)new ObjectMapper().readTree(executionSystem.toJSON());
    	
    	List<Object[]> testCases = new ArrayList<Object[]>();
    	for (String createdBy: new String[] { SYSTEM_OWNER, SYSTEM_SHARE_USER, SYSTEM_PUBLIC_USER, TENANT_ADMIN }) {
    		for (SystemEventType eventType: SystemEventType.values()) {
    			testCases.add(new Object[]{ storageSystem, eventType, createdBy, storageSystemJson });
    			testCases.add(new Object[]{ executionSystem, eventType, createdBy, executionSystemJson });
//    			break;
    		}
//    		break;
    	}
    	return testCases.toArray(new Object[][] {});
    }
    
	@Test(dataProvider="doSoftwareEventsProvider", enabled=false)
	public void doSoftwareEvents(RemoteSystem system, SystemEventType eventType, String createdBy, ObjectNode systemJson) 
	throws Throwable 
	{		
		ObjectMapper mapper = new ObjectMapper();
		SystemHistoryEventDao entityEventDao = mock(SystemHistoryEventDao.class);
		system.setId((long)5);
		
		// mock db lookup
		SystemDao mockDao = mock(SystemDao.class);
		when(mockDao.getUserOwnedAppsForSystemId(createdBy, system.getId())).then(APP_UUID_ANSWER);
		
		List<String> uuids = APP_UUID_ANSWER.answer(null);
		
		RemoteSystemEventProcessor eventProcessor = new RemoteSystemEventProcessor();
		eventProcessor.setEntityEventDao(entityEventDao);
		eventProcessor.setDao(mockDao);
		RemoteSystemEventProcessor spy = spy(eventProcessor);
		
		spy.doSoftwareEvents(system, eventType.name(), createdBy, systemJson);
		
		verify(spy).doSoftwareEvents(system, eventType.name(), createdBy, systemJson);
		
		verify(mockDao).getUserOwnedAppsForSystemId(createdBy, system.getId());
		
		for(String uuid: uuids) {
			
			verify(spy).fireNotification(eq(uuid), eq("SYSTEM_" + eventType.name()), eq(createdBy), 
					eq((ObjectNode)systemJson.deepCopy().set("app", mapper.createObjectNode().put("uuid", uuid))));
		}
	}
	
	@DataProvider
    protected Object[][] processPermissionEventProvider() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
    	RemoteSystem storageSystem = getPrivateStorageSystem();
    	RemoteSystem executionSystem = getPrivateExecutionSystem();
    	
    	ObjectNode storageSystemJson = (ObjectNode)mapper.createObjectNode().put("system", mapper.readTree(storageSystem.toJSON()));
    	ObjectNode executionSystemJson = (ObjectNode)mapper.createObjectNode().put("system", mapper.readTree(executionSystem.toJSON()));
    	
    	List<Object[]> testCases = new ArrayList<Object[]>();
    	for (String createdBy: new String[] { SYSTEM_OWNER, SYSTEM_SHARE_USER, SYSTEM_PUBLIC_USER, TENANT_ADMIN }) {
    		for (RoleType roleType: RoleType.values()) {
    			SystemRole storageSystemRole = new SystemRole(SYSTEM_UNSHARED_USER, roleType);
				if (roleType != RoleType.PUBLISHER) {
    				testCases.add(new Object[]{ storageSystem, 
    						storageSystemRole, 
    						(roleType == RoleType.NONE ? SystemEventType.ROLES_REVOKE : SystemEventType.ROLES_GRANT), 
    						createdBy, 
    						storageSystemJson.deepCopy().put("role", mapper.valueToTree(storageSystemRole)) });
    			}
				SystemRole executionSystemRole = new SystemRole(SYSTEM_UNSHARED_USER, roleType);
    			testCases.add(new Object[]{ executionSystem, 
    					executionSystemRole, 
    					(roleType == RoleType.NONE ? SystemEventType.ROLES_REVOKE : SystemEventType.ROLES_GRANT), 
    					createdBy, 
    					executionSystemJson.deepCopy().put("role", mapper.valueToTree(executionSystemRole)) });
//    			break;
    		}
//    		break;
    	}
    	return testCases.toArray(new Object[][] {});
    }
	

	@Test(dataProvider="processPermissionEventProvider")
	public void processPermissionEvent(RemoteSystem system, SystemRole systemRole, SystemEventType expectedEventType, String createdBy, ObjectNode systemJson) 
	throws EntityEventPersistenceException
	{
		SystemHistoryEventDao entityEventDao = mock(SystemHistoryEventDao.class);
		system.setId((long)5);
		
		// mock db lookup
		SystemDao mockDao = mock(SystemDao.class);
		when(mockDao.getUserOwnedAppsForSystemId(createdBy, system.getId())).then(APP_UUID_ANSWER);
		
//		List<String> uuids = APP_UUID_ANSWER.answer(null);
		
		RemoteSystemEventProcessor eventProcessor = new RemoteSystemEventProcessor();
		eventProcessor.setEntityEventDao(entityEventDao);
		eventProcessor.setDao(mockDao);
		RemoteSystemEventProcessor spy = spy(eventProcessor);
		
		spy.processPermissionEvent(system, systemRole, createdBy);
		
		verify(spy).processPermissionEvent(system, systemRole, createdBy);
		
		// verify the permission history event was created
		verify(entityEventDao).persist(any(SystemHistoryEvent.class));
		
		// verify the ROLE_* system event fired
		verify(spy).fireNotification(system.getSystemId(), expectedEventType.name(), 
				createdBy, (ObjectNode)systemJson);
		
		// verify that all apps get notified
		verify(spy).doSoftwareEvents(system, SystemEventType.ROLES_GRANT.name(), createdBy, systemJson);
	}

	@Test(enabled=false)
	public void processPublishEvent() {
		throw new RuntimeException("Test not implemented");
	}

	@Test(enabled=false)
	public void processStatusChangeEvent() {
		throw new RuntimeException("Test not implemented");
	}

	@Test(enabled=false)
	public void processSystemUpdateEvent() {
		throw new RuntimeException("Test not implemented");
	}
}
