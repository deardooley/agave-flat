package org.iplantc.service.profile.model;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.iplantc.service.common.dao.TenantDao;
import org.iplantc.service.common.model.Tenant;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.profile.ModelTestCommon;
import org.iplantc.service.profile.TestDataHelper;
import org.iplantc.service.profile.exceptions.ProfileException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Tests deserialization from JSON into an InternalUser object
 *
 * @author dooley
 *
 */
@Test(groups={"integration"})
public class ProfileTest extends ModelTestCommon{

	private Tenant defaultTenant = null;
	private Tenant alternateTenant = null;
	private Tenant decoyTenant = null;
	private TenantDao tenantDao = new TenantDao();

    @BeforeClass
    public void setUp() throws Exception {
        super.setUp();
        clearTenants();
        initTenants();
    }

    @BeforeMethod
	public void beforeMethod() throws Exception {
		jsonTree = dataHelper.getTestDataObject(TestDataHelper.TEST_INTERNAL_USER_FILE);

	}

    @AfterClass
	public void afterClass() throws Exception {
		clearTenants();
	}

    private void initTenants() throws Exception
    {
    	defaultTenant = new Tenant(TenancyHelper.getCurrentTenantId(), "https://default.example.com", "default@example.com", "Default Admin");
    	Thread.sleep(500);
    	alternateTenant = new Tenant("alternate", "https://alternate.example.com", "alternate@example.com", "Alternate Admin");
    	Thread.sleep(500);
    	decoyTenant = new Tenant("decoy", "https://decoy.example.com", "decoy@example.com", "Decoy Admin");
    	tenantDao.persist(defaultTenant);
    	tenantDao.persist(alternateTenant);
    	tenantDao.persist(decoyTenant);
    }

    private void clearTenants() throws Exception
    {
    	try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.clear();
			HibernateUtil.disableAllFilters();

			session.createQuery("DELETE Tenant").executeUpdate();

			session.flush();
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception e) {}

			throw new ProfileException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
    }

    @DataProvider
    protected Object[][] testUuidCreationProvider() throws Exception {
    	return new Object[][] {
    			{ defaultTenant },
    			{ alternateTenant },
    			{ decoyTenant },
    	};
    }
    @Test (groups={"model","profile"}, dataProvider="testUuidCreationProvider")
    public void testUuidCreation(Tenant tenant)
    throws Exception
    {
    	 TenancyHelper.setCurrentTenantId(tenant.getTenantCode());
    	 Profile profile = new Profile();
    	 profile.setUsername(tenant.getTenantCode() + "TenantUser");

    	 Assert.assertNotNull(profile.getUuid(), "Minimal profile should use the default tenant to generate a non-null uuid");

    	 String[] uuidTokens = profile.getUuid().split("-");

    	 Assert.assertEquals(uuidTokens.length, 4, "Profile uuid has the wrong structure.");

    	 Assert.assertEquals(uuidTokens[0], tenant.getUuid().split("-")[0],
    			"First token in profile uuid should be first token in tenant uuid.");

    	 Assert.assertEquals(profile.getUuid().split("-")[1], profile.getUsername(),
    			"Second token in profile uuid should be profile username.");

    	 Assert.assertEquals(profile.getUuid().split("-")[3], UUIDType.PROFILE.getCode(),
     			"Last token in profile uuid should be a profile uuid code " + UUIDType.PROFILE.getCode());

    }

}
