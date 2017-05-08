package org.iplantc.service.common.dao;


import org.hibernate.Session;
import org.iplantc.service.common.exceptions.TenantException;
import org.iplantc.service.common.model.Tenant;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups={"integration"})
public class TenantDaoTest {

	private TenantDao dao;

	@BeforeClass
	public void beforeClass()
	{
		dao = new TenantDao();
	}

	@AfterMethod
	public void afterMethod()
	{
		Session session = HibernateUtil.getSession();
		session.clear();
		HibernateUtil.disableAllFilters();
		session.createQuery("delete Tenant").executeUpdate();
		session.flush();
		HibernateUtil.commitTransaction();
	}

	private Tenant createTenant()
	{
		Tenant tenant = new Tenant();
		tenant.setBaseUrl("https://api.example.com");
		tenant.setTenantCode("example.com");
		tenant.setContactEmail("foo@example.com");
		tenant.setContactName("Foo Bar");
		tenant.setStatus("ACTIVE");
		return tenant;
	}

	@Test
	public void save() throws TenantException
	{
		Tenant tenant = createTenant();
		dao.persist(tenant);
		Assert.assertNotNull(tenant.getId(), "Tenant failed to save.");
	}

	@Test(dependsOnMethods={"save"})
	public void findByTenantId() throws TenantException
	{
		Tenant tenant = createTenant();

		dao.persist(tenant);
		Assert.assertNotNull(tenant.getId(), "Tenant failed to save.");

		Tenant savedTenant = dao.findByTenantId(tenant.getTenantCode());

		Assert.assertNotNull(savedTenant, "Failed to find a matching tenant by id");

		Assert.assertEquals(savedTenant.getId(), tenant.getId(), "Tenant ids were not equal. Wrong tenant returned from findByTenantId");
	}

	@Test(dependsOnMethods={"findByTenantId"})
	public void update() throws TenantException
	{
		Tenant tenant = createTenant();

		dao.persist(tenant);
		Assert.assertNotNull(tenant.getId(), "Tenant failed to save.");

		tenant.setContactEmail("bar@example.com");

		dao.persist(tenant);

		Tenant savedTenant = dao.findByTenantId(tenant.getTenantCode());

		Assert.assertNotNull(savedTenant, "Failed to find a matching tenant by id");

		Assert.assertEquals(savedTenant.getContactEmail(), tenant.getContactEmail(), "Tenant failed to update.");
	}

	@Test(dependsOnMethods={"update"})
	public void remove() throws TenantException
	{
		Tenant tenant = createTenant();

		dao.persist(tenant);
		Assert.assertNotNull(tenant.getId(), "Tenant failed to save.");

		dao.delete(tenant);

		Tenant savedTenant = dao.findByTenantId(tenant.getTenantCode());

		Assert.assertNull(savedTenant, "Tenant was not removed.");
	}
}
