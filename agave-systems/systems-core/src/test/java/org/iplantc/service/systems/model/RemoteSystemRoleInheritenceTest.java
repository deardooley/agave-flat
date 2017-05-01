package org.iplantc.service.systems.model;

import org.iplantc.service.systems.Settings;
import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.iplantc.service.systems.model.enumerations.RoleType;
import org.iplantc.service.systems.model.enumerations.StorageProtocolType;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups={"broken", "unit"})
public class RemoteSystemRoleInheritenceTest
{
	private final String GOD_USER = "dooley";
	private final String SYSTEM_OWNER = "testuser";
	private final String SYSTEM_SHARE_GUEST_USER = "shareguest";
	private final String SYSTEM_SHARE_USER_USER = "shareuser";
	private final String SYSTEM_SHARE_PUBLISHER_USER = "sharepublisher";
	private final String SYSTEM_SHARE_ADMIN_USER = "shareadmin";
	private final String SYSTEM_UNSHARED_USER = "testotheruser";
	
	private ExecutionSystem getExecutionSystem() {
		ExecutionSystem system = new ExecutionSystem();
		system.setStorageConfig(new StorageConfig("localhost", -1, StorageProtocolType.LOCAL, null));
		return system;
	}
	
	private StorageSystem getStorageSystem() {
		StorageSystem system = new StorageSystem();
		system.setStorageConfig(new StorageConfig("localhost", -1, StorageProtocolType.LOCAL, null));
		return system;
	}
	
	@BeforeClass
    public void setUp() {}
	
	@DataProvider
	public Object[][] privateSystemUserRoleTestProvider() throws SystemArgumentException
	{	
		ExecutionSystem publicStorageSystem = getExecutionSystem();
		publicStorageSystem.setSystemId("publicStorageSystem");
		publicStorageSystem.setOwner(SYSTEM_OWNER);
		publicStorageSystem.setPubliclyAvailable(false);
		
		ExecutionSystem publicExecutionSystem = getExecutionSystem();
		publicExecutionSystem.setSystemId("publicExecutionSystem");
		publicExecutionSystem.setOwner(SYSTEM_OWNER);
		publicExecutionSystem.setPubliclyAvailable(false);
		
		return new Object[][] {
			{ publicStorageSystem, GOD_USER, RoleType.ADMIN, "God user should have admin on every system." },
			{ publicStorageSystem, SYSTEM_OWNER, RoleType.OWNER, "System owner should have admin on every system they own." },
			{ publicStorageSystem, SYSTEM_SHARE_GUEST_USER, RoleType.NONE, "All other users should have " + RoleType.NONE + " role on public systems." },
			{ publicStorageSystem, SYSTEM_SHARE_USER_USER, RoleType.NONE, "All other users should have " + RoleType.NONE + " role on public systems." },
			{ publicStorageSystem, SYSTEM_SHARE_PUBLISHER_USER, RoleType.NONE, "All other users should have " + RoleType.NONE + " role on public systems." },
			{ publicStorageSystem, SYSTEM_SHARE_ADMIN_USER, RoleType.NONE, "All other users should have " + RoleType.NONE + " role on public systems." },
			{ publicStorageSystem, SYSTEM_UNSHARED_USER, RoleType.NONE, "All other users should have " + RoleType.NONE + " role on public systems." },
				
			{ publicExecutionSystem, GOD_USER, RoleType.ADMIN, "God user should have admin on every system." },
			{ publicExecutionSystem, SYSTEM_OWNER, RoleType.OWNER, "System owner should have admin on every system they own." },
			{ publicExecutionSystem, SYSTEM_SHARE_GUEST_USER, RoleType.NONE, "All other users should have " + RoleType.NONE + " role on public systems." },
			{ publicExecutionSystem, SYSTEM_SHARE_USER_USER, RoleType.NONE, "All other users should have " + RoleType.NONE + " role on public systems." },
			{ publicExecutionSystem, SYSTEM_SHARE_PUBLISHER_USER, RoleType.NONE, "All other users should have " + RoleType.NONE + " role on public systems." },
			{ publicExecutionSystem, SYSTEM_SHARE_ADMIN_USER, RoleType.NONE, "All other users should have " + RoleType.NONE + " role on public systems." },
			{ publicExecutionSystem, SYSTEM_UNSHARED_USER, RoleType.NONE, "All other users should have " + RoleType.NONE + " role on public systems." },
		};
	}
	
	@Test(dataProvider="privateSystemUserRoleTestProvider")
    public void privateSystemUserRoleTest(RemoteSystem system, String username, RoleType expectedRole, String message) 
	{	
		Assert.assertEquals(system.getUserRole(username).getRole(), expectedRole, message);
    }

	@DataProvider
	public Object[][] publicSystemUserRoleTestProvider() throws SystemArgumentException
	{
		
		StorageSystem publicStorageSystem = getStorageSystem();
		publicStorageSystem.setSystemId("publicStorageSystem");
		publicStorageSystem.setOwner(SYSTEM_OWNER);
		publicStorageSystem.setPubliclyAvailable(true);
		
		ExecutionSystem publicExecutionSystem = getExecutionSystem();
		publicExecutionSystem.setSystemId("publicExecutionSystem");
		publicExecutionSystem.setOwner(SYSTEM_OWNER);
		publicExecutionSystem.setPubliclyAvailable(true);
		
		return new Object[][] {
			{ publicStorageSystem, GOD_USER, RoleType.ADMIN, "God user should have admin on every system." },
			{ publicStorageSystem, SYSTEM_OWNER, RoleType.OWNER, "System owner should have admin on every system they own." },
			{ publicStorageSystem, SYSTEM_SHARE_GUEST_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public systems." },
			{ publicStorageSystem, SYSTEM_SHARE_USER_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public systems." },
			{ publicStorageSystem, SYSTEM_SHARE_PUBLISHER_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public systems." },
			{ publicStorageSystem, SYSTEM_SHARE_ADMIN_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public systems." },
			{ publicStorageSystem, SYSTEM_UNSHARED_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public systems." },
				
			{ publicExecutionSystem, GOD_USER, RoleType.ADMIN, "God user should have admin on every system." },
			{ publicExecutionSystem, SYSTEM_OWNER, RoleType.OWNER, "System owner should have admin on every system they own." },
			{ publicExecutionSystem, SYSTEM_SHARE_GUEST_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public execution systems." },
			{ publicExecutionSystem, SYSTEM_SHARE_USER_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public execution systems." },
			{ publicExecutionSystem, SYSTEM_SHARE_PUBLISHER_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public execution systems." },
			{ publicExecutionSystem, SYSTEM_SHARE_ADMIN_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public execution systems." },
			{ publicExecutionSystem, SYSTEM_UNSHARED_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public execution systems." },
		};
	}
	
	@Test(dataProvider="publicSystemUserRoleTestProvider")
    public void publicSystemUserRoleTest(RemoteSystem system, String username, RoleType expectedRole, String message) 
	{	
		Assert.assertEquals(system.getUserRole(username).getRole(), expectedRole, message);
    }
	
	@DataProvider
	public Object[][] publicSharedSystemUserRoleTestProvider() throws SystemArgumentException
	{
		StorageSystem publicSharedStorageSystem = getStorageSystem();
		publicSharedStorageSystem.setSystemId("publicSharedStorageSystem");
		publicSharedStorageSystem.setOwner(SYSTEM_OWNER);
		publicSharedStorageSystem.setPubliclyAvailable(true);
		publicSharedStorageSystem.addRole(new SystemRole(SYSTEM_SHARE_GUEST_USER, RoleType.GUEST));
		publicSharedStorageSystem.addRole(new SystemRole(SYSTEM_SHARE_USER_USER, RoleType.USER));
		publicSharedStorageSystem.addRole(new SystemRole(SYSTEM_SHARE_PUBLISHER_USER, RoleType.PUBLISHER));
		publicSharedStorageSystem.addRole(new SystemRole(SYSTEM_SHARE_ADMIN_USER, RoleType.ADMIN));
		
		ExecutionSystem publicSharedExecutionSystem = getExecutionSystem();
		publicSharedExecutionSystem.setSystemId("publicSharedExecutionSystem");
		publicSharedExecutionSystem.setOwner(SYSTEM_OWNER);
		publicSharedExecutionSystem.setPubliclyAvailable(true);
		publicSharedExecutionSystem.addRole(new SystemRole(SYSTEM_SHARE_GUEST_USER, RoleType.GUEST));
		publicSharedExecutionSystem.addRole(new SystemRole(SYSTEM_SHARE_USER_USER, RoleType.USER));
		publicSharedExecutionSystem.addRole(new SystemRole(SYSTEM_SHARE_PUBLISHER_USER, RoleType.PUBLISHER));
		publicSharedExecutionSystem.addRole(new SystemRole(SYSTEM_SHARE_ADMIN_USER, RoleType.ADMIN));
		
		return new Object[][] {
			{ publicSharedStorageSystem, GOD_USER, RoleType.ADMIN, "God user should have admin on every system." },
			{ publicSharedStorageSystem, SYSTEM_OWNER, RoleType.OWNER, "System owner should have admin on every system they own." },
			{ publicSharedStorageSystem, SYSTEM_SHARE_GUEST_USER, RoleType.GUEST, "Users with explicit guest role on public system should maintain guest role" },
			{ publicSharedStorageSystem, SYSTEM_SHARE_USER_USER, RoleType.USER, "Users with explicit user role on public system should maintain user role" },
			{ publicSharedStorageSystem, SYSTEM_SHARE_PUBLISHER_USER, RoleType.USER, "Users with explicit publisher role on public storage system should have user role" },
			{ publicSharedStorageSystem, SYSTEM_SHARE_ADMIN_USER, RoleType.ADMIN, "Users with explicit admin role on public system should maintain admin role" },
			{ publicSharedStorageSystem, SYSTEM_UNSHARED_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public systems." },
			
			{ publicSharedExecutionSystem, GOD_USER, RoleType.ADMIN, "God user should have admin on every system." },
			{ publicSharedExecutionSystem, SYSTEM_OWNER, RoleType.OWNER, "System owner should have admin on every system they own." },
			{ publicSharedExecutionSystem, SYSTEM_SHARE_GUEST_USER, RoleType.GUEST, "Users with explicit guest role on public system should maintain guest role" },
			{ publicSharedExecutionSystem, SYSTEM_SHARE_USER_USER, RoleType.USER, "Users with explicit user role on public system should maintain user role" },
			{ publicSharedExecutionSystem, SYSTEM_SHARE_PUBLISHER_USER, RoleType.PUBLISHER, "Users with explicit publisher role on public execution system should have publisher role" },
			{ publicSharedExecutionSystem, SYSTEM_SHARE_ADMIN_USER, RoleType.ADMIN, "Users with explicit admin role on public system should maintain admin role" },
			{ publicSharedExecutionSystem, SYSTEM_UNSHARED_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public systems." },
		};
	}
	
	@Test(dataProvider="publicSharedSystemUserRoleTestProvider")
    public void publicSharedSystemUserRoleTest(RemoteSystem system, String username, RoleType expectedRole, String message) 
	{
    	Assert.assertEquals(system.getUserRole(username).getRole(), expectedRole, message);
    	
    }
	
	@DataProvider
	public Object[][] publicGuestSystemUserRoleTestProvider() throws SystemArgumentException
	{
		StorageSystem publicGuestStorageSystem = getStorageSystem();
		publicGuestStorageSystem.setSystemId("publicGuestStorageSystem");
		publicGuestStorageSystem.setOwner(SYSTEM_OWNER);
		publicGuestStorageSystem.setPubliclyAvailable(true);
		publicGuestStorageSystem.addRole(new SystemRole(Settings.WORLD_USER_USERNAME, RoleType.GUEST));
		
		ExecutionSystem publicGuestExecutionSystem = getExecutionSystem();
		publicGuestExecutionSystem.setSystemId("publicGuestExecutionSystem");
		publicGuestExecutionSystem.setOwner(SYSTEM_OWNER);
		publicGuestExecutionSystem.setPubliclyAvailable(true);
		publicGuestExecutionSystem.addRole(new SystemRole(Settings.WORLD_USER_USERNAME, RoleType.GUEST));
		
		return new Object[][] {
//			{ publicGuestStorageSystem, GOD_USER, RoleType.ADMIN, "God user should have admin on every system." },
//			{ publicGuestStorageSystem, SYSTEM_OWNER, RoleType.OWNER, "System owner should have admin on every system they own." },
			{ publicGuestStorageSystem, SYSTEM_SHARE_GUEST_USER, RoleType.GUEST, "All other users should have " + RoleType.GUEST + " role on public systems." },
			{ publicGuestStorageSystem, SYSTEM_SHARE_USER_USER, RoleType.GUEST, "All other users should have " + RoleType.GUEST + " role on public systems." },
			{ publicGuestStorageSystem, SYSTEM_SHARE_PUBLISHER_USER, RoleType.GUEST, "All other users should have " + RoleType.GUEST + " role on public systems." },
			{ publicGuestStorageSystem, SYSTEM_SHARE_ADMIN_USER, RoleType.GUEST, "All other users should have " + RoleType.GUEST + " role on public systems." },
			{ publicGuestStorageSystem, SYSTEM_UNSHARED_USER, RoleType.GUEST, "All other users should have " + RoleType.GUEST + " role on public systems." },
			
			{ publicGuestExecutionSystem, GOD_USER, RoleType.ADMIN, "God user should have admin on every system." },
			{ publicGuestExecutionSystem, SYSTEM_OWNER, RoleType.OWNER, "System owner should have admin on every system they own." },
			{ publicGuestExecutionSystem, SYSTEM_SHARE_GUEST_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public execution systems." },
			{ publicGuestExecutionSystem, SYSTEM_SHARE_USER_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public execution systems." },
			{ publicGuestExecutionSystem, SYSTEM_SHARE_PUBLISHER_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public execution systems." },
			{ publicGuestExecutionSystem, SYSTEM_SHARE_ADMIN_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public execution systems." },
			{ publicGuestExecutionSystem, SYSTEM_UNSHARED_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public execution systems." },
		};
	}
	
	@Test(dataProvider="publicGuestSystemUserRoleTestProvider")
    public void publicGuestSystemUserRoleTest(RemoteSystem system, String username, RoleType expectedRole, String message) 
	{
    	Assert.assertEquals(system.getUserRole(username).getRole(), expectedRole, message);
    }
	
	@DataProvider
	public Object[][] publicSharedGuestSystemUserRoleTestProvider() throws SystemArgumentException
	{
		StorageSystem publicSharedGuestStorageSystem = getStorageSystem();
		publicSharedGuestStorageSystem.setSystemId("publicSharedGuestStorageSystem");
		publicSharedGuestStorageSystem.setOwner(SYSTEM_OWNER);
		publicSharedGuestStorageSystem.setPubliclyAvailable(true);
		publicSharedGuestStorageSystem.addRole(new SystemRole(Settings.WORLD_USER_USERNAME, RoleType.GUEST));
		publicSharedGuestStorageSystem.addRole(new SystemRole(SYSTEM_SHARE_GUEST_USER, RoleType.GUEST));
		publicSharedGuestStorageSystem.addRole(new SystemRole(SYSTEM_SHARE_USER_USER, RoleType.USER));
		publicSharedGuestStorageSystem.addRole(new SystemRole(SYSTEM_SHARE_PUBLISHER_USER, RoleType.PUBLISHER));
		publicSharedGuestStorageSystem.addRole(new SystemRole(SYSTEM_SHARE_ADMIN_USER, RoleType.ADMIN));
		
		ExecutionSystem publicSharedGuestExecutionSystem = getExecutionSystem();
		publicSharedGuestExecutionSystem.setSystemId("publicSharedGuestExecutionSystem");
		publicSharedGuestExecutionSystem.setOwner(SYSTEM_OWNER);
		publicSharedGuestExecutionSystem.setPubliclyAvailable(true);
		publicSharedGuestExecutionSystem.addRole(new SystemRole(Settings.WORLD_USER_USERNAME, RoleType.GUEST));
		publicSharedGuestExecutionSystem.addRole(new SystemRole(SYSTEM_SHARE_GUEST_USER, RoleType.GUEST));
		publicSharedGuestExecutionSystem.addRole(new SystemRole(SYSTEM_SHARE_USER_USER, RoleType.USER));
		publicSharedGuestExecutionSystem.addRole(new SystemRole(SYSTEM_SHARE_PUBLISHER_USER, RoleType.PUBLISHER));
		publicSharedGuestExecutionSystem.addRole(new SystemRole(SYSTEM_SHARE_ADMIN_USER, RoleType.ADMIN));
		
		return new Object[][] {
			{ publicSharedGuestStorageSystem, GOD_USER, RoleType.ADMIN, "God user should have admin on every system." },
			{ publicSharedGuestStorageSystem, SYSTEM_OWNER, RoleType.OWNER, "System owner should have admin on every system they own." },
			{ publicSharedGuestStorageSystem, SYSTEM_SHARE_GUEST_USER, RoleType.GUEST, "Users with explicit guest role on public guest system should maintain guest role" },
			{ publicSharedGuestStorageSystem, SYSTEM_SHARE_USER_USER, RoleType.USER, "Users with explicit user role on public guest system should maintain user role" },
			{ publicSharedGuestStorageSystem, SYSTEM_SHARE_PUBLISHER_USER, RoleType.USER, "Users with explicit publisher role on public guest storage system should have user role" },
			{ publicSharedGuestStorageSystem, SYSTEM_SHARE_ADMIN_USER, RoleType.ADMIN, "Users with explicit admin role on public guest system should maintain admin role" },
			{ publicSharedGuestStorageSystem, SYSTEM_UNSHARED_USER, RoleType.GUEST, "All other users should have " + RoleType.GUEST + " role on public systems." },
			
			{ publicSharedGuestExecutionSystem, GOD_USER, RoleType.ADMIN, "God user should have admin on every system." },
			{ publicSharedGuestExecutionSystem, SYSTEM_OWNER, RoleType.OWNER, "System owner should have admin on every system they own." },
			{ publicSharedGuestExecutionSystem, SYSTEM_SHARE_GUEST_USER, RoleType.GUEST, "Users with explicit guest role on public guest system should maintain guest role" },
			{ publicSharedGuestExecutionSystem, SYSTEM_SHARE_USER_USER, RoleType.USER, "Users with explicit user role on public guest system should maintain user role" },
			{ publicSharedGuestExecutionSystem, SYSTEM_SHARE_PUBLISHER_USER, RoleType.PUBLISHER, "Users with explicit publisher role on public guest storage system should have PUBLISHER role" },
			{ publicSharedGuestExecutionSystem, SYSTEM_SHARE_ADMIN_USER, RoleType.ADMIN, "Users with explicit admin role on public guest system should maintain admin role" },
			{ publicSharedGuestExecutionSystem, SYSTEM_UNSHARED_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public execution systems." },
		};
	}
	
	@Test(dataProvider="publicSharedGuestSystemUserRoleTestProvider")
    public void publicSharedGuestSystemUserRoleTest(RemoteSystem system, String username, RoleType expectedRole, String message) 
	{
    	Assert.assertEquals(system.getUserRole(username).getRole(), expectedRole, message);
    }
	
	@DataProvider
	public Object[][] publicPublicGuestSystemUserRoleTestProvider() throws SystemArgumentException
	{
		StorageSystem publicGuestStorageSystem = getStorageSystem();
		publicGuestStorageSystem.setSystemId("publicGuestStorageSystem");
		publicGuestStorageSystem.setOwner(SYSTEM_OWNER);
		publicGuestStorageSystem.setPubliclyAvailable(true);
		publicGuestStorageSystem.addRole(new SystemRole(Settings.PUBLIC_USER_USERNAME, RoleType.GUEST));
		
		ExecutionSystem publicGuestExecutionSystem = getExecutionSystem();
		publicGuestExecutionSystem.setSystemId("publicGuestExecutionSystem");
		publicGuestExecutionSystem.setOwner(SYSTEM_OWNER);
		publicGuestExecutionSystem.setPubliclyAvailable(true);
		publicGuestExecutionSystem.addRole(new SystemRole(Settings.PUBLIC_USER_USERNAME, RoleType.GUEST));
		
		return new Object[][] {
			{ publicGuestStorageSystem, GOD_USER, RoleType.ADMIN, "God user should have admin on every system." },
			{ publicGuestStorageSystem, SYSTEM_OWNER, RoleType.OWNER, "System owner should have admin on every system they own." },
			{ publicGuestStorageSystem, SYSTEM_SHARE_GUEST_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public systems." },
			{ publicGuestStorageSystem, SYSTEM_SHARE_USER_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public systems." },
			{ publicGuestStorageSystem, SYSTEM_SHARE_PUBLISHER_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public systems." },
			{ publicGuestStorageSystem, SYSTEM_SHARE_ADMIN_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public systems." },
			{ publicGuestStorageSystem, SYSTEM_UNSHARED_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public systems." },
			
			{ publicGuestExecutionSystem, GOD_USER, RoleType.ADMIN, "God user should have admin on every system." },
			{ publicGuestExecutionSystem, SYSTEM_OWNER, RoleType.OWNER, "System owner should have admin on every system they own." },
			{ publicGuestExecutionSystem, SYSTEM_SHARE_GUEST_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public execution systems." },
			{ publicGuestExecutionSystem, SYSTEM_SHARE_USER_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public execution systems." },
			{ publicGuestExecutionSystem, SYSTEM_SHARE_PUBLISHER_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public execution systems." },
			{ publicGuestExecutionSystem, SYSTEM_SHARE_ADMIN_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public execution systems." },
			{ publicGuestExecutionSystem, SYSTEM_UNSHARED_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public execution systems." },
		};
	}
	
	@Test(dataProvider="publicPublicGuestSystemUserRoleTestProvider")
    public void publicPublicGuestSystemUserRoleTest(RemoteSystem system, String username, RoleType expectedRole, String message) 
	{
    	Assert.assertEquals(system.getUserRole(username).getRole(), expectedRole, message);
    }
	
	@DataProvider
	public Object[][] publicPublicSharedGuestSystemUserRoleTestProvider() throws SystemArgumentException
	{
		StorageSystem publicSharedGuestStorageSystem = getStorageSystem();
		publicSharedGuestStorageSystem.setSystemId("publicSharedGuestStorageSystem");
		publicSharedGuestStorageSystem.setOwner(SYSTEM_OWNER);
		publicSharedGuestStorageSystem.setPubliclyAvailable(true);
		publicSharedGuestStorageSystem.addRole(new SystemRole(Settings.PUBLIC_USER_USERNAME, RoleType.GUEST));
		publicSharedGuestStorageSystem.addRole(new SystemRole(SYSTEM_SHARE_GUEST_USER, RoleType.GUEST));
		publicSharedGuestStorageSystem.addRole(new SystemRole(SYSTEM_SHARE_USER_USER, RoleType.USER));
		publicSharedGuestStorageSystem.addRole(new SystemRole(SYSTEM_SHARE_PUBLISHER_USER, RoleType.PUBLISHER));
		publicSharedGuestStorageSystem.addRole(new SystemRole(SYSTEM_SHARE_ADMIN_USER, RoleType.ADMIN));
		
		ExecutionSystem publicSharedGuestExecutionSystem = getExecutionSystem();
		publicSharedGuestExecutionSystem.setSystemId("publicSharedGuestExecutionSystem");
		publicSharedGuestExecutionSystem.setOwner(SYSTEM_OWNER);
		publicSharedGuestExecutionSystem.setPubliclyAvailable(true);
		publicSharedGuestExecutionSystem.addRole(new SystemRole(Settings.PUBLIC_USER_USERNAME, RoleType.GUEST));
		publicSharedGuestExecutionSystem.addRole(new SystemRole(SYSTEM_SHARE_GUEST_USER, RoleType.GUEST));
		publicSharedGuestExecutionSystem.addRole(new SystemRole(SYSTEM_SHARE_USER_USER, RoleType.USER));
		publicSharedGuestExecutionSystem.addRole(new SystemRole(SYSTEM_SHARE_PUBLISHER_USER, RoleType.PUBLISHER));
		publicSharedGuestExecutionSystem.addRole(new SystemRole(SYSTEM_SHARE_ADMIN_USER, RoleType.ADMIN));
		
		return new Object[][] {
			{ publicSharedGuestStorageSystem, GOD_USER, RoleType.ADMIN, "God user should have admin on every system." },
			{ publicSharedGuestStorageSystem, SYSTEM_OWNER, RoleType.OWNER, "System owner should have admin on every system they own." },
			{ publicSharedGuestStorageSystem, SYSTEM_SHARE_GUEST_USER, RoleType.GUEST, "Users with explicit guest role on public guest system should maintain guest role" },
			{ publicSharedGuestStorageSystem, SYSTEM_SHARE_USER_USER, RoleType.USER, "Users with explicit user role on public guest system should maintain user role" },
			{ publicSharedGuestStorageSystem, SYSTEM_SHARE_PUBLISHER_USER, RoleType.USER, "Users with explicit publisher role on public guest storage system should have user role" },
			{ publicSharedGuestStorageSystem, SYSTEM_SHARE_ADMIN_USER, RoleType.ADMIN, "Users with explicit admin role on public guest system should maintain admin role" },
			{ publicSharedGuestStorageSystem, SYSTEM_UNSHARED_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public systems." },
			
			{ publicSharedGuestExecutionSystem, GOD_USER, RoleType.ADMIN, "God user should have admin on every system." },
			{ publicSharedGuestExecutionSystem, SYSTEM_OWNER, RoleType.OWNER, "System owner should have admin on every system they own." },
			{ publicSharedGuestExecutionSystem, SYSTEM_SHARE_GUEST_USER, RoleType.GUEST, "Users with explicit guest role on public guest system should maintain guest role" },
			{ publicSharedGuestExecutionSystem, SYSTEM_SHARE_USER_USER, RoleType.USER, "Users with explicit user role on public guest system should maintain user role" },
			{ publicSharedGuestExecutionSystem, SYSTEM_SHARE_PUBLISHER_USER, RoleType.PUBLISHER, "Users with explicit publisher role on public guest storage system should have PUBLISHER role" },
			{ publicSharedGuestExecutionSystem, SYSTEM_SHARE_ADMIN_USER, RoleType.ADMIN, "Users with explicit admin role on public guest system should maintain admin role" },
			{ publicSharedGuestExecutionSystem, SYSTEM_UNSHARED_USER, RoleType.USER, "All other users should have " + RoleType.USER + " role on public execution systems." },
		};
	}
	
	@Test(dataProvider="publicPublicSharedGuestSystemUserRoleTestProvider")
    public void publicPublicSharedGuestSystemUserRoleTest(RemoteSystem system, String username, RoleType expectedRole, String message) 
	{
    	Assert.assertEquals(system.getUserRole(username).getRole(), expectedRole, message);
    }
	
	@DataProvider
	public Object[][] privateSharedGuestSystemUserRoleTestProvider() throws SystemArgumentException
	{
		StorageSystem privatePublicUserStorageSystem = getStorageSystem();
		privatePublicUserStorageSystem.setSystemId("privatePublicUserStorageSystem");
		privatePublicUserStorageSystem.setOwner(SYSTEM_OWNER);
		privatePublicUserStorageSystem.setPubliclyAvailable(false);
		privatePublicUserStorageSystem.addRole(new SystemRole(Settings.PUBLIC_USER_USERNAME, RoleType.GUEST));
		
		ExecutionSystem privatePublicUserExecutionSystem = getExecutionSystem();
		privatePublicUserExecutionSystem.setSystemId("privatePublicUserExecutionSystem");
		privatePublicUserExecutionSystem.setOwner(SYSTEM_OWNER);
		privatePublicUserExecutionSystem.setPubliclyAvailable(false);
		privatePublicUserExecutionSystem.addRole(new SystemRole(Settings.PUBLIC_USER_USERNAME, RoleType.GUEST));
		
		StorageSystem privatePublicUserSharedStorageSystem = getStorageSystem();
		privatePublicUserSharedStorageSystem.setSystemId("privatePublicUserSharedStorageSystem");
		privatePublicUserSharedStorageSystem.setOwner(SYSTEM_OWNER);
		privatePublicUserSharedStorageSystem.setPubliclyAvailable(false);
		privatePublicUserSharedStorageSystem.addRole(new SystemRole(Settings.PUBLIC_USER_USERNAME, RoleType.GUEST));
		privatePublicUserSharedStorageSystem.addRole(new SystemRole(SYSTEM_SHARE_GUEST_USER, RoleType.GUEST));
		privatePublicUserSharedStorageSystem.addRole(new SystemRole(SYSTEM_SHARE_USER_USER, RoleType.USER));
		privatePublicUserSharedStorageSystem.addRole(new SystemRole(SYSTEM_SHARE_PUBLISHER_USER, RoleType.PUBLISHER));
		privatePublicUserSharedStorageSystem.addRole(new SystemRole(SYSTEM_SHARE_ADMIN_USER, RoleType.ADMIN));
		
		ExecutionSystem privatePublicUserSharedExecutionSystem = getExecutionSystem();
		privatePublicUserSharedExecutionSystem.setSystemId("privatePublicUserSharedExecutionSystem");
		privatePublicUserSharedExecutionSystem.setOwner(SYSTEM_OWNER);
		privatePublicUserSharedExecutionSystem.setPubliclyAvailable(false);
		privatePublicUserSharedExecutionSystem.addRole(new SystemRole(Settings.PUBLIC_USER_USERNAME, RoleType.GUEST));
		privatePublicUserSharedExecutionSystem.addRole(new SystemRole(SYSTEM_SHARE_GUEST_USER, RoleType.GUEST));
		privatePublicUserSharedExecutionSystem.addRole(new SystemRole(SYSTEM_SHARE_USER_USER, RoleType.USER));
		privatePublicUserSharedExecutionSystem.addRole(new SystemRole(SYSTEM_SHARE_PUBLISHER_USER, RoleType.PUBLISHER));
		privatePublicUserSharedExecutionSystem.addRole(new SystemRole(SYSTEM_SHARE_ADMIN_USER, RoleType.ADMIN));
		
		return new Object[][] {
			{ privatePublicUserStorageSystem, GOD_USER, RoleType.ADMIN, "God user should have admin on every system." },
			{ privatePublicUserStorageSystem, SYSTEM_OWNER, RoleType.OWNER, "System owner should have admin on every system they own." },
			{ privatePublicUserStorageSystem, SYSTEM_SHARE_GUEST_USER, RoleType.NONE, "Users with explicit guest role on private system with public guest user should maintain guest role" },
			{ privatePublicUserStorageSystem, SYSTEM_SHARE_USER_USER, RoleType.NONE, "Users with explicit user role on private system with public guest user should maintain user role" },
			{ privatePublicUserStorageSystem, SYSTEM_SHARE_PUBLISHER_USER, RoleType.NONE, "Users with explicit publisher role on private system with public guest user should have user role" },
			{ privatePublicUserStorageSystem, SYSTEM_SHARE_ADMIN_USER, RoleType.NONE, "Users with explicit admin role on private system with public guest user should maintain admin role" },
			{ privatePublicUserStorageSystem, SYSTEM_UNSHARED_USER, RoleType.NONE, "All other users should have " + RoleType.GUEST + " role on private system with public guest user ." },
			
			{ privatePublicUserExecutionSystem, GOD_USER, RoleType.ADMIN, "God user should have admin on every system." },
			{ privatePublicUserExecutionSystem, SYSTEM_OWNER, RoleType.OWNER, "System owner should have admin on every system they own." },
			{ privatePublicUserExecutionSystem, SYSTEM_SHARE_GUEST_USER, RoleType.NONE, "Users with explicit guest role on public guest system should maintain guest role" },
			{ privatePublicUserExecutionSystem, SYSTEM_SHARE_USER_USER, RoleType.NONE, "Users with explicit user role on private guest system should maintain user role" },
			{ privatePublicUserExecutionSystem, SYSTEM_SHARE_PUBLISHER_USER, RoleType.NONE, "Users with explicit publisher role on public guest storage system should have NONE role" },
			{ privatePublicUserExecutionSystem, SYSTEM_SHARE_ADMIN_USER, RoleType.NONE, "Users with explicit admin role on public guest system should maintain admin role" },
			{ privatePublicUserExecutionSystem, SYSTEM_UNSHARED_USER, RoleType.NONE, "All other users should have " + RoleType.NONE + " role on private system with public guest user." },
			
			{ privatePublicUserSharedStorageSystem, GOD_USER, RoleType.ADMIN, "God user should have admin on every system." },
			{ privatePublicUserSharedStorageSystem, SYSTEM_OWNER, RoleType.OWNER, "System owner should have admin on every system they own." },
			{ privatePublicUserSharedStorageSystem, SYSTEM_SHARE_GUEST_USER, RoleType.GUEST, "Users with explicit guest role on private shared system should maintain guest role" },
			{ privatePublicUserSharedStorageSystem, SYSTEM_SHARE_USER_USER, RoleType.USER, "Users with explicit user role on private shared system should maintain user role" },
			{ privatePublicUserSharedStorageSystem, SYSTEM_SHARE_PUBLISHER_USER, RoleType.USER, "Users with explicit publisher role on private shared storage system should have user role" },
			{ privatePublicUserSharedStorageSystem, SYSTEM_SHARE_ADMIN_USER, RoleType.ADMIN, "Users with explicit admin role on private shared system should maintain admin role" },
			{ privatePublicUserSharedStorageSystem, SYSTEM_UNSHARED_USER, RoleType.NONE, "All other users should have " + RoleType.GUEST + " role on private shared systems." },
			
			{ privatePublicUserSharedExecutionSystem, GOD_USER, RoleType.ADMIN, "God user should have admin on every system." },
			{ privatePublicUserSharedExecutionSystem, SYSTEM_OWNER, RoleType.OWNER, "System owner should have admin on every system they own." },
			{ privatePublicUserSharedExecutionSystem, SYSTEM_SHARE_GUEST_USER, RoleType.GUEST, "Users with explicit guest role on private shared system should maintain guest role" },
			{ privatePublicUserSharedExecutionSystem, SYSTEM_SHARE_USER_USER, RoleType.USER, "Users with explicit user role on private shared system should maintain user role" },
			{ privatePublicUserSharedExecutionSystem, SYSTEM_SHARE_PUBLISHER_USER, RoleType.PUBLISHER, "Users with explicit publisher role on private shared storage system should have PUBLISHER role" },
			{ privatePublicUserSharedExecutionSystem, SYSTEM_SHARE_ADMIN_USER, RoleType.ADMIN, "Users with explicit admin role on private shared system should maintain admin role" },
			{ privatePublicUserSharedExecutionSystem, SYSTEM_UNSHARED_USER, RoleType.NONE, "All other users should have " + RoleType.NONE + " role on private shared systems." },
		};
	}
	
	@Test(dataProvider="privateSharedGuestSystemUserRoleTestProvider")
    public void privateSharedGuestSystemUserRoleTest(RemoteSystem system, String username, RoleType expectedRole, String message) 
	{
    	Assert.assertEquals(system.getUserRole(username).getRole(), expectedRole, message);
    }

}
