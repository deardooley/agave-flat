package org.iplantc.service.common.dao;


import java.util.List;

import org.iplantc.service.common.exceptions.TenantException;
import org.iplantc.service.common.model.Tenant;
import org.testng.Assert;
import org.testng.annotations.Test;

/** This is a non-destructive test of the tenant dao.
 * 
 * @author rcardone
 */
public class TenantDaoTest2 {

    private static final String TENANT_CODE_1 = "example.com"; 
    private static final String TENANT_CODE_2 = "example2.com"; 

    /* ********************************************************************** */
    /*                              Test Methods                              */
    /* ********************************************************************** */    
	@Test
	public void exists() throws TenantException
	{
	    // Get a dao.
	    TenantDao dao = new TenantDao();
	    
	    // Save the test tenant.
		Tenant tenant = createTenant(TENANT_CODE_1);
		dao.persist(tenant);
		Assert.assertNotNull(tenant.getId(), "Tenant failed to save.");
		
		// Make sure the test tenant exists.
		boolean exists = dao.exists(TENANT_CODE_1);
		Assert.assertTrue(exists, "Expected to find tenant");
		
		// Delete the test tenant.
		dao.delete(tenant);
        Tenant deletedTenant = dao.findByTenantId(TENANT_CODE_1);
        Assert.assertNull(deletedTenant, "Tenant was not removed.");
	}

    @Test
    public void getTenantIds() throws TenantException
    {
        // Get a dao.
        TenantDao dao = new TenantDao();
        
        // Save the test tenants.
        Tenant tenant1 = createTenant(TENANT_CODE_1);
        dao.persist(tenant1);
        Assert.assertNotNull(tenant1.getId(), "Tenant 1 failed to save.");
        Tenant tenant2 = createTenant(TENANT_CODE_2);
        dao.persist(tenant2);
        Assert.assertNotNull(tenant2.getId(), "Tenant 2 failed to save.");
        
        // Make sure the test tenant exists.
        List<String> tenantIds = dao.getTenantIds(true);
        System.out.println("Number of active tenant IDs retrieved: " + tenantIds.size());
        Assert.assertTrue(tenantIds.contains(TENANT_CODE_1), "Expected to find " + TENANT_CODE_1);
        Assert.assertTrue(tenantIds.contains(TENANT_CODE_2), "Expected to find " + TENANT_CODE_2);
        
        // Delete the test tenant.
        dao.delete(tenant1);
        Tenant deletedTenant1 = dao.findByTenantId(TENANT_CODE_1);
        Assert.assertNull(deletedTenant1, "Tenant 1 was not removed.");
        dao.delete(tenant2);
        Tenant deletedTenant2 = dao.findByTenantId(TENANT_CODE_2);
        Assert.assertNull(deletedTenant2, "Tenant 2 was not removed.");
    }
    
    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
	/** Create a test tenant in memory. */
    private Tenant createTenant(String tenantCode)
    {
        Tenant tenant = new Tenant();
        tenant.setBaseUrl("https://api.example.com");
        tenant.setTenantCode(tenantCode);
        tenant.setContactEmail("foo@example.com");
        tenant.setContactName("Foo Bar");
        tenant.setStatus("ACTIVE");
        return tenant;
    }
}
