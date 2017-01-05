package org.iplantc.service.common.dao;


import org.iplantc.service.common.exceptions.TenantException;
import org.iplantc.service.common.model.Tenant;
import org.testng.Assert;
import org.testng.annotations.Test;

/** This is a non-destructive test of the tenant dao.
 * 
 * @author rcardone
 */
public class TenantDaoTest2 {

    private static final String TENANT_CODE = "example.com"; 

	@Test
	public void exists() throws TenantException
	{
	    // Get a dao.
	    TenantDao dao = new TenantDao();
	    
	    // Save the test tenant.
		Tenant tenant = createTenant();
		dao.persist(tenant);
		Assert.assertNotNull(tenant.getId(), "Tenant failed to save.");
		
		// Make sure the test tenant exists.
		boolean exists = dao.exists(TENANT_CODE);
		Assert.assertTrue(exists, "Expected to find tenant");
		
		// Delete the test tenant.
		dao.delete(tenant);
        Tenant deletedTenant = dao.findByTenantId(TENANT_CODE);
        Assert.assertNull(deletedTenant, "Tenant was not removed.");
	}

	/** Create a test tenant in memory. */
    private Tenant createTenant()
    {
        Tenant tenant = new Tenant();
        tenant.setBaseUrl("https://api.example.com");
        tenant.setTenantCode(TENANT_CODE);
        tenant.setContactEmail("foo@example.com");
        tenant.setContactName("Foo Bar");
        tenant.setStatus("ACTIVE");
        return tenant;
    }
}
