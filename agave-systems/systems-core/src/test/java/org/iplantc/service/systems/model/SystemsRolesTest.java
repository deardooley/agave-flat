package org.iplantc.service.systems.model;

import org.iplantc.service.systems.Settings;
import org.iplantc.service.systems.model.enumerations.RoleType;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Logic tests for the system role object
 */
public class SystemsRolesTest
{
	private SystemRole role;
    
	@BeforeClass
    public void setUp() {}

    @BeforeMethod
	public void setUpMethod() throws Exception {
    	role = new SystemRole(Settings.IRODS_USERNAME, RoleType.NONE);
	}
    
    @DataProvider(name = "systemRoleCanUse")
    public Object[][] systemRoleCanUse() {
    	return new Object[][]{
    		{ RoleType.NONE, Settings.IRODS_USERNAME, "Can use with permission " + RoleType.NONE, false},
    		{ RoleType.USER, Settings.IRODS_USERNAME, "Can use with permission " + RoleType.USER, true},
    		{ RoleType.PUBLISHER, Settings.IRODS_USERNAME, "Can use with permission " + RoleType.PUBLISHER, true},
    		{ RoleType.OWNER, Settings.IRODS_USERNAME, "Can use with permission " + RoleType.OWNER, true},
    		{ RoleType.ADMIN, Settings.IRODS_USERNAME, "Can use with permission " + RoleType.ADMIN, true},
    	};
    }
    
    @Test (groups={"model","roles", "software"}, dataProvider="systemRoleCanUse")
    public void systemRoleCanUse(RoleType type, String username, String message, boolean expectedResult) 
    throws Exception {
    	role.setRole(type);
    	Assert.assertTrue(role.canUse() == expectedResult, message);
    }
    
    @DataProvider(name = "systemRoleCanPublish")
    public Object[][] systemRoleCanPublish() {
    	return new Object[][]{
			{ RoleType.NONE, Settings.IRODS_USERNAME, "Can use with permission " + RoleType.NONE, false},
    		{ RoleType.USER, Settings.IRODS_USERNAME, "Can use with permission " + RoleType.USER, false},
    		{ RoleType.PUBLISHER, Settings.IRODS_USERNAME, "Can use with permission " + RoleType.PUBLISHER, true},
    		{ RoleType.OWNER, Settings.IRODS_USERNAME, "Can use with permission " + RoleType.OWNER, true},
    		{ RoleType.ADMIN, Settings.IRODS_USERNAME, "Can use with permission " + RoleType.ADMIN, true},
    	};
    }

    @Test (groups={"model","roles", "software"}, dataProvider="systemRoleCanPublish")
    public void systemRoleCanPublish(RoleType type, String username, String message, boolean expectedResult) 
    throws Exception {
    	role.setRole(type);
    	Assert.assertTrue(role.canPublish() == expectedResult, message);
    }
    
    @DataProvider(name = "systemRoleCanAdmin")
    public Object[][] systemRoleCanAdmin() {
    	return new Object[][]{
			{ RoleType.NONE, Settings.IRODS_USERNAME, "Can use with permission " + RoleType.NONE, false},
    		{ RoleType.USER, Settings.IRODS_USERNAME, "Can use with permission " + RoleType.USER, false},
    		{ RoleType.PUBLISHER, Settings.IRODS_USERNAME, "Can use with permission " + RoleType.PUBLISHER, false},
    		{ RoleType.OWNER, Settings.IRODS_USERNAME, "Can use with permission " + RoleType.OWNER, true},
    		{ RoleType.ADMIN, Settings.IRODS_USERNAME, "Can use with permission " + RoleType.ADMIN, true},
    	};
    }

    @Test (groups={"model","roles", "software"}, dataProvider="systemRoleCanAdmin")
    public void systemRoleCanAdmin(RoleType type, String username, String message, boolean expectedResult) 
    throws Exception {
    	role.setRole(type);
    	Assert.assertTrue(role.canAdmin() == expectedResult, message);
    }
    
    
}
