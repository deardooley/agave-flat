package org.iplantc.service.systems.manager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.UniqueId;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.iplantc.service.systems.exceptions.SystemRoleException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.JSONTestDataUtil;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.SystemRole;
import org.iplantc.service.systems.model.SystemsModelTestCommon;
import org.iplantc.service.systems.model.enumerations.RoleType;
import org.json.JSONException;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SystemRoleManagerTest extends SystemsModelTestCommon {
    private SystemDao dao = new SystemDao();
    
    @Override
    @BeforeClass
    public void beforeClass() throws Exception {
        jtd = JSONTestDataUtil.getInstance();
    }
    
    @BeforeMethod
    public void beforeMethod() throws Exception {
        clearSystems();
    }
    
    @AfterMethod
    public void afterMethod() throws Exception {
        clearSystems();
    }
    
    private StorageSystem getStorageSystem(boolean publik)
    throws SystemArgumentException, JSONException, IOException, SystemRoleException
    {
        return getStorageSystem(publik, null);
    }
    
    private StorageSystem getStorageSystem(boolean publik, SystemRole[] initialRoles)
    throws SystemArgumentException, JSONException, IOException, SystemRoleException 
    {
        StorageSystem system = StorageSystem.fromJSON( jtd.getTestDataObject(
                JSONTestDataUtil.TEST_STORAGE_SYSTEM_FILE));
        system.setOwner(SYSTEM_OWNER);
        system.setPubliclyAvailable(true);
        system.setGlobalDefault(true);
        system.setSystemId(new UniqueId().getStringId());
        
        if (initialRoles != null) {
            for (SystemRole role: initialRoles) {
                system.addRole(role.clone());
            }
        }
        
        return system;
    }
    
    private ExecutionSystem getExecutionSystem(boolean publik)
    throws SystemArgumentException, JSONException, IOException, SystemRoleException 
    {
        return getExecutionSystem(publik, null);
    }
    
    private ExecutionSystem getExecutionSystem(boolean publik, SystemRole[] initialRoles) 
    throws SystemArgumentException, JSONException, IOException, SystemRoleException 
    {
        ExecutionSystem system = ExecutionSystem.fromJSON( jtd.getTestDataObject(
            JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE));
        system.setOwner(SYSTEM_OWNER);
        system.setPubliclyAvailable(true);
        system.setGlobalDefault(true);
        system.setSystemId(new UniqueId().getStringId());
        
        if (initialRoles != null) {
            for (SystemRole role: initialRoles) {
                system.addRole(role);
            }
        }
        
        return system;
    }
    
    @DataProvider
    public Object[][] clearRolesProvider() throws Exception {
        SystemRole[] roles = new SystemRole[]{ new SystemRole(SYSTEM_SHARE_USER, RoleType.USER), new SystemRole(SYSTEM_SHARE_USER, RoleType.USER) };
        
        return new Object[][] {
                {getStorageSystem(true, Arrays.copyOf(roles, 2)), false, "System roles were not cleared on public storage system"},
                {getStorageSystem(false, Arrays.copyOf(roles, 2)), false, "System roles were not cleared on private storage system"},
                {getExecutionSystem(true, Arrays.copyOf(roles, 2)), false, "System roles were not cleared on public execution system"},
//                {getExecutionSystem(false, Arrays.copyOf(roles, 2)), false, "System roles were not cleared on private execution system"},
        };
    }

    @Test(dataProvider="clearRolesProvider")
    public void clearRoles(RemoteSystem system, boolean shouldThrowException, String message) {
        
        try {
            dao.persist(system);
            SystemRoleManager manager = new SystemRoleManager(system);
            manager.clearRoles(system.getOwner());
            
            RemoteSystem clearedSystem = new SystemDao().findById(system.getId());
            
            Assert.assertNotNull(clearedSystem, "Unable to fetch system by id after clearing roles.");
            Assert.assertTrue(clearedSystem.getRoles().isEmpty(), "System roles should be empty after clearing.");
        } 
        catch (Exception e) {
            if (!shouldThrowException) {
                Assert.fail("Clearing permissions on system should not throw exceptions",e);
            }
        }
        finally {
            try { dao.remove(system); } catch (Exception e) {}
        }
    }
    
//    @DataProvider
//    public Object[][] setRoleFailsOnPublicSystemsProvider() throws Exception {
//        return new Object[][] {
//            { getStorageSystem(true), TENANT_ADMIN, false, "Setting role on public storage system should not throw exception for a tenant admin." },
//            { getStorageSystem(true), SYSTEM_OWNER, true, "Setting role on public storage system should throw exception for system owner." },
//            { getStorageSystem(true), SYSTEM_SHARE_USER, true, "Setting role on public storage system should throw exception for system shared user." },
//            { getStorageSystem(true), SYSTEM_PUBLIC_USER, true, "Setting role on public storage system should throw exception for public user." },
//            { getStorageSystem(true), SYSTEM_UNSHARED_USER, true, "Setting role on public storage system should throw exception for random user." },
//            
//            { getExecutionSystem(true), TENANT_ADMIN, false, "Setting role on public execution system should not throw exception a tenant admin." },
//            { getExecutionSystem(true), SYSTEM_OWNER, true, "Setting role on public execution system should throw exception for system owner." },
//            { getExecutionSystem(true), SYSTEM_SHARE_USER, true, "Setting role on public execution system should throw exception for system shared user." },
//            { getExecutionSystem(true), SYSTEM_PUBLIC_USER, true, "Setting role on public execution system should throw exception for public user." },
//            { getExecutionSystem(true), SYSTEM_UNSHARED_USER, true, "Setting role on public execution system should throw exception for random user." },
//        };
//    }
//    
//    @Test(dataProvider="setRoleFailsOnPublicSystemsProvider")
//    public void setRoleFailsOnPublicSystems(RemoteSystem system, String requestingUser, boolean shouldThrowException, String message) {
//        try {
//            dao.persist(system);
//            TenancyHelper.setCurrentEndUser(requestingUser);
//            
//            for (RoleType role: RoleType.values()) {
//                SystemRoleManager manager = new SystemRoleManager(system);
//                manager.setRole(SYSTEM_UNSHARED_USER, role);
//            }
//        } 
//        catch (Exception e) {
//            if (!shouldThrowException) {
//                Assert.fail("Clearing permissions on system should not throw exceptions",e);
//            }
//        }
//        finally {
//            try { dao.remove(system); } catch (Exception e) {}
//        }
//    }
    
    @DataProvider
    public Object[][] setPublisherRoleFailsOnStorageSystemProvider() throws Exception {
        
        return new Object[][] {
            { getStorageSystem(false), TENANT_ADMIN, "Setting PUBLISHER role on private storage system should throw exception for a tenant admin." },
            { getStorageSystem(false), SYSTEM_OWNER, "Setting PUBLISHER role on private storage system should throw exception for a tenant admin." },
            { getStorageSystem(false, new SystemRole[]{ new SystemRole(SYSTEM_SHARE_USER, RoleType.PUBLISHER)}), SYSTEM_SHARE_USER, "Setting PUBLISHER role on private storage system should throw exception for a tenant admin." },
            
            { getStorageSystem(true), TENANT_ADMIN, "Setting PUBLISHER role on private storage system should throw exception for a tenant admin." },
            { getStorageSystem(true), SYSTEM_OWNER, "Setting PUBLISHER role on private storage system should throw exception for a tenant admin." },
            { getStorageSystem(true, new SystemRole[]{ new SystemRole(SYSTEM_SHARE_USER, RoleType.PUBLISHER)}), SYSTEM_SHARE_USER, "Setting PUBLISHER role on private storage system should throw exception for a tenant admin." },
        };
    }
    
    @Test(dependsOnMethods={"clearRoles"}, dataProvider="setPublisherRoleFailsOnStorageSystemProvider")
    public void setPublisherRoleFailsOnStorageSystem(RemoteSystem system, String requestingUser, String message) {
        try {
            dao.persist(system);
            TenancyHelper.setCurrentEndUser(requestingUser);
            
            SystemRoleManager manager = new SystemRoleManager(system);
            
            manager.setRole(SYSTEM_SHARE_USER, RoleType.PUBLISHER, requestingUser);
            
            Assert.fail(message);
        } 
        catch (Exception e) {
            
        }
        finally {
            try { dao.remove(system); } catch (Exception e) {}
        }
    }
    
    @DataProvider
    public Object[][] setRoleDoesNotChangeExistingOwnerRoleProvider() throws Exception {
        List<Object[]> testCases = new ArrayList<Object[]>();
        
        for (RoleType roleType: RoleType.values()) {
            if (roleType != RoleType.OWNER) {
                if (roleType != RoleType.PUBLISHER) {
                    testCases.add(new Object[]{getStorageSystem(true), TENANT_ADMIN, SYSTEM_OWNER, roleType, RoleType.OWNER, false, "setRole should not alter owner permission on public storage system"});
                    testCases.add(new Object[]{getStorageSystem(false), TENANT_ADMIN, SYSTEM_OWNER, roleType, RoleType.OWNER, false, "setRole should not alter owner permission on private storage system"});
                }
                testCases.add(new Object[]{getExecutionSystem(true), TENANT_ADMIN, SYSTEM_OWNER, roleType, RoleType.OWNER, false, "setRole should not alter owner permission on public execution system"});
                testCases.add(new Object[]{getExecutionSystem(false), TENANT_ADMIN, SYSTEM_OWNER, roleType, RoleType.OWNER, false, "setRole should not alter owner permission on private execution system"});
            }
        };
        
        return testCases.toArray(new Object[][] {});
    }
    
    @Test(dependsOnMethods={"setPublisherRoleFailsOnStorageSystem"}, dataProvider="setRoleDoesNotChangeExistingOwnerRoleProvider")
    public void setRoleDoesNotChangeExistingOwnerRole(RemoteSystem system, String requestingUser, String roleUsername, RoleType roleType, RoleType expectedRoleType, boolean shouldThrowException, String message) {
        try {
            system.addRole(new SystemRole(roleUsername, roleType));
            dao.persist(system);
            
            TenancyHelper.setCurrentEndUser(requestingUser);
            
            SystemRoleManager manager = new SystemRoleManager(system);
            manager.setRole(roleUsername, roleType, requestingUser);
            
            RemoteSystem updatedSystem = dao.findById(system.getId());
            SystemRole udpatedRole = updatedSystem.getUserRole(roleUsername);
            
            Assert.assertNotNull(udpatedRole, "No user role retured after adding to system.");
            Assert.assertEquals(udpatedRole.getUsername(), roleUsername, "getUserRole returned role for incorrect user after adding to system.");
            Assert.assertEquals(udpatedRole.getRole(), expectedRoleType, message);         
        } 
        catch (Exception e) {
            Assert.fail("Updating owner role should not throw exception.", e);
        }
        finally {
            try { dao.remove(system); } catch (Exception e) {}
        }
    }
    
    @DataProvider
    public Object[][] setRoleDoesNotChangeExistingSuperAdminRoleProvider() throws Exception {
        List<Object[]> testCases = new ArrayList<Object[]>();
        
        for (RoleType roleType: RoleType.values()) {
            if (roleType != RoleType.ADMIN) {
                if (roleType != RoleType.PUBLISHER) {
                    testCases.add(new Object[]{getStorageSystem(true), TENANT_ADMIN, TENANT_ADMIN, roleType, RoleType.ADMIN, false, "setRole should not alter tenant admin permission on public storage system"});
                    testCases.add(new Object[]{getStorageSystem(false), TENANT_ADMIN, TENANT_ADMIN, roleType, RoleType.ADMIN, false, "setRole should not alter tenant admin permission on private storage system"});
                }
                testCases.add(new Object[]{getExecutionSystem(true), TENANT_ADMIN, TENANT_ADMIN, roleType, RoleType.ADMIN, false, "setRole should not alter tenant admin permission on public execution system"});
                testCases.add(new Object[]{getExecutionSystem(false), TENANT_ADMIN, TENANT_ADMIN, roleType, RoleType.ADMIN, false, "setRole should not alter tenant admin permission on private execution system"});
            }
        };
        
        return testCases.toArray(new Object[][] {});
    }
    
//    @Test(dependsOnMethods={"setRoleDoesNotChangeExistingOwnerRole"}, dataProvider="setRoleDoesNotChangeExistingSuperAdminRoleProvider")
    @Test(dataProvider="setRoleDoesNotChangeExistingSuperAdminRoleProvider")
    public void setRoleDoesNotChangeExistingSuperAdminRole(RemoteSystem system, String requestingUser, String roleUsername, RoleType roleType, RoleType expectedRoleType, boolean shouldThrowException, String message) {
        try {
            system.addRole(new SystemRole(roleUsername, roleType));
            dao.persist(system);
            
            TenancyHelper.setCurrentEndUser(requestingUser);
            
            SystemRoleManager manager = new SystemRoleManager(system);
            manager.setRole(roleUsername, roleType, requestingUser);
            
            RemoteSystem updatedSystem = dao.findById(system.getId());
            SystemRole udpatedRole = updatedSystem.getUserRole(roleUsername);
            
            Assert.assertNotNull(udpatedRole, "No user role retured after adding to system.");
            Assert.assertEquals(udpatedRole.getUsername(), roleUsername, "getUserRole returned role for incorrect user after adding to system.");
            Assert.assertEquals(udpatedRole.getRole(), expectedRoleType, message);         
        } 
        catch (Exception e) {
            Assert.fail("Updating owner role should not throw exception.", e);
        }
        finally {
            try { dao.remove(system); } catch (Exception e) {}
        }
    }
    
//    @Test
//    public void setRoleNoneRemovesExistingRole(RemoteSystem system, boolean shouldThrowException, String message) {
//        throw new RuntimeException("Test not implemented");
//    }
//    @Test
//    public void setRoleDoesNotChangeExistingDuplicateRole(RemoteSystem system, boolean shouldThrowException, String message) {
//        throw new RuntimeException("Test not implemented");
//    }
    
    @DataProvider
    public Object[][] getRolesSetByTenantAdminProvider() throws Exception {
        
        return new Object[][] {
                {getStorageSystem(true), TENANT_ADMIN, TENANT_ADMIN, RoleType.OWNER, RoleType.ADMIN, false, "setRole should throw exception when an owner permission is applied"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_SHARE_USER, RoleType.OWNER, RoleType.OWNER, false, "setRole should throw exception when an owner permission is applied"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_UNSHARED_USER , RoleType.OWNER, RoleType.OWNER, false, "setRole should throw exception when an owner permission is applied"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_PUBLIC_USER, RoleType.OWNER, RoleType.OWNER, false, "setRole should throw exception when an owner permission is applied"},
                
                {getStorageSystem(true), TENANT_ADMIN, TENANT_ADMIN, RoleType.ADMIN, RoleType.ADMIN, false, "setRole should not throw exception when a tenant admin sets an admin permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_SHARE_USER, RoleType.ADMIN, RoleType.ADMIN, false, "setRole should throw exception when a tenant admin sets an admin permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_UNSHARED_USER, RoleType.ADMIN, RoleType.ADMIN, false, "setRole should throw exception when a tenant admin sets an admin permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_PUBLIC_USER, RoleType.ADMIN, RoleType.ADMIN, false, "setRole should throw exception when a tenant admin sets an admin permission on a public system"},
                
//                {getStorageSystem(true), TENANT_ADMIN, TENANT_ADMIN, RoleType.PUBLISHER, RoleType.ADMIN, false, "setRole should not throw exception when a tenant admin sets an publisher permission on a public system"},
//                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_OWNER, RoleType.PUBLISHER, RoleType.OWNER, false, "setRole should not throw exception when a tenant admin sets an publisher permission on a public system"},
//                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_SHARE_USER, RoleType.PUBLISHER, RoleType.PUBLISHER, false, "setRole should throw exception when a tenant admin sets an publisher permission on a public system"},
//                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_UNSHARED_USER, RoleType.PUBLISHER, RoleType.PUBLISHER, false, "setRole should throw exception when a tenant admin sets an publisher permission on a public system"},
//                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_PUBLIC_USER, RoleType.PUBLISHER, RoleType.PUBLISHER, false, "setRole should throw exception when a tenant admin sets an publisher permission on a public system"},
//                
                {getStorageSystem(true), TENANT_ADMIN, TENANT_ADMIN, RoleType.USER, RoleType.ADMIN, false, "setRole should not throw exception when a tenant admin sets an user permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_SHARE_USER, RoleType.USER, RoleType.USER, false, "setRole should throw exception when a tenant admin sets an user permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_UNSHARED_USER, RoleType.USER, RoleType.USER, false, "setRole should throw exception when a tenant admin sets an user permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_PUBLIC_USER, RoleType.USER, RoleType.USER, false, "setRole should throw exception when a tenant admin sets an user user on a public system"},
                
                {getStorageSystem(true), TENANT_ADMIN, TENANT_ADMIN, RoleType.GUEST, RoleType.ADMIN, false, "setRole should not throw exception when a tenant admin sets a guest permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_SHARE_USER, RoleType.GUEST, RoleType.GUEST, false, "setRole should throw exception when a tenant admin sets a guest permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_UNSHARED_USER, RoleType.GUEST, RoleType.GUEST, false, "setRole should throw exception when a tenant admin sets a guest permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_PUBLIC_USER, RoleType.GUEST, RoleType.GUEST, false, "setRole should throw exception when a tenant admin sets a guest user on a public system"},
                
                {getStorageSystem(true), TENANT_ADMIN, TENANT_ADMIN, RoleType.NONE, RoleType.ADMIN, false, "setRole should not throw exception when a tenant admin sets a guest permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_SHARE_USER, RoleType.NONE, RoleType.USER, false, "setRole should throw exception when a tenant admin sets a guest permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_UNSHARED_USER, RoleType.NONE, RoleType.USER, false, "setRole should throw exception when a tenant admin sets a guest permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_PUBLIC_USER, RoleType.NONE, RoleType.USER, false, "setRole should throw exception when a tenant admin sets a guest user on a public system"},
                
                // execution tests
                {getExecutionSystem(true), TENANT_ADMIN, TENANT_ADMIN, RoleType.OWNER, RoleType.ADMIN, false, "setRole should throw exception when an owner permission is applied"},
                {getExecutionSystem(true), TENANT_ADMIN, SYSTEM_SHARE_USER, RoleType.OWNER, RoleType.OWNER, false, "setRole should throw exception when an owner permission is applied"},
                {getExecutionSystem(true), TENANT_ADMIN, SYSTEM_UNSHARED_USER , RoleType.OWNER, RoleType.OWNER, false, "setRole should throw exception when an owner permission is applied"},
                {getExecutionSystem(true), TENANT_ADMIN, SYSTEM_PUBLIC_USER, RoleType.OWNER, RoleType.OWNER, false, "setRole should throw exception when an owner permission is applied"},
                
                {getExecutionSystem(true), TENANT_ADMIN, TENANT_ADMIN, RoleType.ADMIN, RoleType.ADMIN, false, "setRole should not throw exception when a tenant admin sets an admin permission on a public system"},
                {getExecutionSystem(true), TENANT_ADMIN, SYSTEM_OWNER, RoleType.ADMIN, RoleType.OWNER, false, "setRole should not throw exception when a tenant admin sets an admin permission on a public system"},
                {getExecutionSystem(true), TENANT_ADMIN, SYSTEM_SHARE_USER, RoleType.ADMIN, RoleType.ADMIN, false, "setRole should throw exception when a tenant admin sets an admin permission on a public system"},
                {getExecutionSystem(true), TENANT_ADMIN, SYSTEM_UNSHARED_USER, RoleType.ADMIN, RoleType.ADMIN, false, "setRole should throw exception when a tenant admin sets an admin permission on a public system"},
                {getExecutionSystem(true), TENANT_ADMIN, SYSTEM_PUBLIC_USER, RoleType.ADMIN, RoleType.ADMIN, false, "setRole should throw exception when a tenant admin sets an admin permission on a public system"},
                
                {getExecutionSystem(true), TENANT_ADMIN, TENANT_ADMIN, RoleType.PUBLISHER, RoleType.ADMIN, false, "setRole should not throw exception when a tenant admin sets an publisher permission on a public system"},
                {getExecutionSystem(true), TENANT_ADMIN, SYSTEM_OWNER, RoleType.PUBLISHER, RoleType.OWNER, false, "setRole should not throw exception when a tenant admin sets an publisher permission on a public system"},
                {getExecutionSystem(true), TENANT_ADMIN, SYSTEM_SHARE_USER, RoleType.PUBLISHER, RoleType.PUBLISHER, false, "setRole should throw exception when a tenant admin sets an publisher permission on a public system"},
                {getExecutionSystem(true), TENANT_ADMIN, SYSTEM_UNSHARED_USER, RoleType.PUBLISHER, RoleType.PUBLISHER, false, "setRole should throw exception when a tenant admin sets an publisher permission on a public system"},
                {getExecutionSystem(true), TENANT_ADMIN, SYSTEM_PUBLIC_USER, RoleType.PUBLISHER, RoleType.PUBLISHER, false, "setRole should throw exception when a tenant admin sets an publisher permission on a public system"},
                
                {getExecutionSystem(true), TENANT_ADMIN, TENANT_ADMIN, RoleType.USER, RoleType.ADMIN, false, "setRole should not throw exception when a tenant admin sets an user permission on a public system"},
                {getExecutionSystem(true), TENANT_ADMIN, SYSTEM_OWNER, RoleType.USER, RoleType.OWNER, false, "setRole should not throw exception when a tenant admin sets an user permission on a public system"},
                {getExecutionSystem(true), TENANT_ADMIN, SYSTEM_SHARE_USER, RoleType.USER, RoleType.USER, false, "setRole should throw exception when a tenant admin sets an user permission on a public system"},
                {getExecutionSystem(true), TENANT_ADMIN, SYSTEM_UNSHARED_USER, RoleType.USER, RoleType.USER, false, "setRole should throw exception when a tenant admin sets an user permission on a public system"},
                {getExecutionSystem(true), TENANT_ADMIN, SYSTEM_PUBLIC_USER, RoleType.USER, RoleType.USER, false, "setRole should throw exception when a tenant admin sets an user user on a public system"},
                
                {getExecutionSystem(true), TENANT_ADMIN, TENANT_ADMIN, RoleType.GUEST, RoleType.ADMIN, false, "setRole should not throw exception when a tenant admin sets a guest permission on a public system"},
                {getExecutionSystem(true), TENANT_ADMIN, SYSTEM_OWNER, RoleType.GUEST, RoleType.OWNER, false, "setRole should not throw exception when a tenant admin sets a guest permission on a public system"},
                {getExecutionSystem(true), TENANT_ADMIN, SYSTEM_SHARE_USER, RoleType.GUEST, RoleType.GUEST, false, "setRole should throw exception when a tenant admin sets a guest permission on a public system"},
                {getExecutionSystem(true), TENANT_ADMIN, SYSTEM_UNSHARED_USER, RoleType.GUEST, RoleType.GUEST, false, "setRole should throw exception when a tenant admin sets a guest permission on a public system"},
                {getExecutionSystem(true), TENANT_ADMIN, SYSTEM_PUBLIC_USER, RoleType.GUEST, RoleType.GUEST, false, "setRole should throw exception when a tenant admin sets a guest user on a public system"},
                
                {getExecutionSystem(true), TENANT_ADMIN, TENANT_ADMIN, RoleType.NONE, RoleType.ADMIN, false, "setRole should not throw exception when a tenant admin sets a guest permission on a public system"},
                {getExecutionSystem(true), TENANT_ADMIN, SYSTEM_OWNER, RoleType.NONE, RoleType.OWNER, false, "setRole should not throw exception when a tenant admin sets a guest permission on a public system"},
                {getExecutionSystem(true), TENANT_ADMIN, SYSTEM_SHARE_USER, RoleType.NONE, RoleType.USER, false, "setRole should throw exception when a tenant admin sets a guest permission on a public system"},
                {getExecutionSystem(true), TENANT_ADMIN, SYSTEM_UNSHARED_USER, RoleType.NONE, RoleType.USER, false, "setRole should throw exception when a tenant admin sets a guest permission on a public system"},
                {getExecutionSystem(true), TENANT_ADMIN, SYSTEM_PUBLIC_USER, RoleType.NONE, RoleType.USER, false, "setRole should throw exception when a tenant admin sets a guest user on a public system"},
                
        };
    }
    
    @DataProvider
    public Object[][] getRolesSetBySystemOwnerProvider() throws Exception {
        
        return new Object[][] {
                {getStorageSystem(true), SYSTEM_OWNER, TENANT_ADMIN, RoleType.OWNER, false, "setRole should throw exception when an owner permission is applied"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_OWNER, RoleType.OWNER, false, "setRole should throw exception when an owner permission is applied"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_SHARE_USER, RoleType.OWNER, false, "setRole should throw exception when an owner permission is applied"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_UNSHARED_USER , RoleType.OWNER, false, "setRole should throw exception when an owner permission is applied"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_PUBLIC_USER, RoleType.OWNER, false, "setRole should throw exception when an owner permission is applied"},
                
                {getStorageSystem(true), TENANT_ADMIN, TENANT_ADMIN, RoleType.ADMIN, false, "setRole should not throw exception when a tenant admin sets an admin permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_OWNER, RoleType.ADMIN, false, "setRole should not throw exception when a tenant admin sets an admin permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_SHARE_USER, RoleType.ADMIN, false, "setRole should throw exception when a tenant admin sets an admin permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_UNSHARED_USER, RoleType.ADMIN, false, "setRole should throw exception when a tenant admin sets an admin permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_PUBLIC_USER, RoleType.ADMIN, false, "setRole should throw exception when a tenant admin sets an admin permission on a public system"},
                
                {getStorageSystem(true), TENANT_ADMIN, TENANT_ADMIN, RoleType.PUBLISHER, false, "setRole should not throw exception when a tenant admin sets an publisher permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_OWNER, RoleType.PUBLISHER, false, "setRole should not throw exception when a tenant admin sets an publisher permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_SHARE_USER, RoleType.PUBLISHER, false, "setRole should throw exception when a tenant admin sets an publisher permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_UNSHARED_USER, RoleType.PUBLISHER, false, "setRole should throw exception when a tenant admin sets an publisher permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_PUBLIC_USER, RoleType.PUBLISHER, false, "setRole should throw exception when a tenant admin sets an publisher permission on a public system"},
                
                {getStorageSystem(true), TENANT_ADMIN, TENANT_ADMIN, RoleType.USER, false, "setRole should not throw exception when a tenant admin sets an user permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_OWNER, RoleType.USER, false, "setRole should not throw exception when a tenant admin sets an user permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_SHARE_USER, RoleType.USER, false, "setRole should throw exception when a tenant admin sets an user permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_UNSHARED_USER, RoleType.USER, false, "setRole should throw exception when a tenant admin sets an user permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_PUBLIC_USER, RoleType.USER, false, "setRole should throw exception when a tenant admin sets an user user on a public system"},
                
                {getStorageSystem(true), TENANT_ADMIN, TENANT_ADMIN, RoleType.GUEST, false, "setRole should not throw exception when a tenant admin sets a guest permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_OWNER, RoleType.GUEST, false, "setRole should not throw exception when a tenant admin sets a guest permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_SHARE_USER, RoleType.GUEST, false, "setRole should throw exception when a tenant admin sets a guest permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_UNSHARED_USER, RoleType.GUEST, false, "setRole should throw exception when a tenant admin sets a guest permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_PUBLIC_USER, RoleType.GUEST, false, "setRole should throw exception when a tenant admin sets a guest user on a public system"},
                
                {getStorageSystem(true), TENANT_ADMIN, TENANT_ADMIN, RoleType.NONE, false, "setRole should not throw exception when a tenant admin sets a guest permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_OWNER, RoleType.NONE, false, "setRole should not throw exception when a tenant admin sets a guest permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_SHARE_USER, RoleType.NONE, false, "setRole should throw exception when a tenant admin sets a guest permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_UNSHARED_USER, RoleType.NONE, false, "setRole should throw exception when a tenant admin sets a guest permission on a public system"},
                {getStorageSystem(true), TENANT_ADMIN, SYSTEM_PUBLIC_USER, RoleType.NONE, false, "setRole should throw exception when a tenant admin sets a guest user on a public system"},
                
        };
    }
    
    @Test(dependsOnMethods={"setRoleDoesNotChangeExistingSuperAdminRole"}, dataProvider="getRolesSetByTenantAdminProvider")
    public void setNewRoleForUser(RemoteSystem system, String requestingUser, String roleUsername, RoleType roleType, RoleType expectedRoleType, boolean shouldThrowException, String message) {
        
        try {
            dao.persist(system);
            TenancyHelper.setCurrentEndUser(requestingUser);
            
            SystemRoleManager manager = new SystemRoleManager(system);
            manager.setRole(roleUsername, roleType, requestingUser);

            RemoteSystem updatedSystem = dao.findById(system.getId());
            SystemRole udpatedRole = updatedSystem.getUserRole(roleUsername);
            
            Assert.assertNotNull(udpatedRole, "No user role retured after adding to system.");
            Assert.assertEquals(udpatedRole.getUsername(), roleUsername, "getUserRole returned role for incorrect user after adding to system.");
            Assert.assertEquals(udpatedRole.getRole(), expectedRoleType, "Incorrect user roleType retured after adding to system.");
        } 
        catch (Exception e) {
            if (!shouldThrowException) {
                Assert.fail(message, e);
            }
        }
        finally {
            try { dao.remove(system); } catch (Exception e) {}
        }
    }
    
    @Test(dependsOnMethods={"setNewRoleForUser"}, dataProvider="getRolesSetByTenantAdminProvider")
    public void setDuplicateRoleForUserReturnsSameRole(RemoteSystem system, String requestingUser, String roleUsername, RoleType roleType, RoleType expectedRoleType, boolean shouldThrowException, String message) {
        
        try {
            system.addRole(new SystemRole(roleUsername, roleType));
            dao.persist(system);
            
            TenancyHelper.setCurrentEndUser(requestingUser);
            
            SystemRoleManager manager = new SystemRoleManager(system);
            manager.setRole(roleUsername, roleType, requestingUser);
            
            RemoteSystem updatedSystem = dao.findById(system.getId());
            SystemRole udpatedRole = updatedSystem.getUserRole(roleUsername);
            
            Assert.assertNotNull(udpatedRole, "No user role retured after adding to system.");
            Assert.assertEquals(udpatedRole.getUsername(), roleUsername, "getUserRole returned role for incorrect user after adding to system.");
            Assert.assertEquals(udpatedRole.getRole(), expectedRoleType, "Incorrect user roleType retured after adding to system.");         
        } 
        catch (Exception e) {
            if (!shouldThrowException) {
                Assert.fail(message, e);
            }
        }
        finally {
            try { dao.remove(system); } catch (Exception e) {}
        }
    }
}
