package org.iplantc.service.metadata.dao;

import java.util.List;

import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.metadata.exceptions.MetadataException;
import org.iplantc.service.metadata.model.MetadataPermission;
import org.iplantc.service.metadata.model.enumerations.PermissionType;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups={"broken", "integration"})
public class MetadataPermissionDaoTest {

	private String uuid = null;
    private String TEST_OWNER = "owner";
    private String TEST_SHARED_OWNER = "sharedowner";

	@BeforeMethod
	public void setUpUuid() throws Exception
	{
		uuid = "testObjectIdentifier987654321";
	}

	@AfterMethod
	public void tearDownUuid() throws Exception
	{

        List<MetadataPermission> pems = MetadataPermissionDao.getByUuid(uuid);
        if (pems.size() > 0)
            for (MetadataPermission pem: pems){
                MetadataPermissionDao.delete(pem);
            }
		uuid = null;
	}

	@Test
	public void persist() throws Exception
	{
		MetadataPermission pem = new MetadataPermission(uuid, TEST_OWNER, PermissionType.READ);
		MetadataPermissionDao.persist(pem);
		Assert.assertNotNull(pem.getId(), "Metadata permission did not persist.");
	}

	@Test(dependsOnMethods={"persist", "getByUuid"})
	public void delete() throws MetadataException
	{
		MetadataPermission pem = new MetadataPermission(uuid, TEST_OWNER, PermissionType.READ);
		MetadataPermissionDao.persist(pem);
		Assert.assertNotNull(pem.getId(), "Metadata permission did not persist.");
		
		MetadataPermissionDao.delete(pem);
		List<MetadataPermission> pems = MetadataPermissionDao.getByUuid(uuid);
		Assert.assertFalse(pems.contains(pem), "Metadata permission did not delete.");
	}

	@Test(dependsOnMethods={"persist"})
	public void getByUuid() throws Exception
	{
		MetadataPermission pem = new MetadataPermission(uuid, TEST_OWNER, PermissionType.READ);
		MetadataPermissionDao.persist(pem);
		Assert.assertNotNull(pem.getId(), "Metadata permission did not persist.");
		
		String uuid2 = "anotherTestUUID123456789";
		
		List<MetadataPermission> pems = MetadataPermissionDao.getByUuid(uuid);
		Assert.assertNotNull(pems, "getByUuid did not return any permissions.");
		Assert.assertEquals(pems.size(), 1, "getByUuid did not return the correct number of permissions.");
		Assert.assertFalse(pems.contains(uuid2), "getByUuid returned a permission from another uuid.");
	}

	@Test(dependsOnMethods={"persist","delete"})
	public void getByUsernameAndUuid() throws Exception
	{
		MetadataPermission pem1 = new MetadataPermission(uuid, TEST_OWNER, PermissionType.READ);
		MetadataPermissionDao.persist(pem1);
		Assert.assertNotNull(pem1.getId(), "Metadata permission 1 did not persist.");
		
		MetadataPermission pem2 = new MetadataPermission(uuid, TEST_SHARED_OWNER, PermissionType.READ);
		MetadataPermissionDao.persist(pem2);
		Assert.assertNotNull(pem2.getId(), "Metadata permission 2 did not persist.");
		
		MetadataPermission userPem = MetadataPermissionDao.getByUsernameAndUuid(TEST_OWNER, uuid);
		Assert.assertNotNull(userPem, "getByUsernameAndUuid did not return the user permission.");
		Assert.assertEquals(userPem, pem1, "getByUsernameAndUuid did not return the correct metadata permission for the user.");
	}
}
