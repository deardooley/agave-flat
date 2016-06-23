package org.iplantc.service.metadata.dao;

import org.iplantc.service.metadata.exceptions.MetadataException;
import org.iplantc.service.metadata.model.MetadataSchemaPermission;
import org.iplantc.service.metadata.model.enumerations.PermissionType;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

public class MetadataSchemaPermissionDaoTest {

	private String schemaId = null;
    private String TEST_OWNER = "owner";
    private String TEST_SHARED_OWNER = "sharedowner";

	@BeforeMethod
	public void setUpSchemaId() throws Exception
	{
		schemaId = "testSchemaIdentifier987654321";
	}

	@AfterMethod
	public void tearDownSchemaId() throws Exception
	{

        List<MetadataSchemaPermission> pems = MetadataSchemaPermissionDao.getBySchemaId(schemaId);
        if (pems.size() > 0)
            for (MetadataSchemaPermission pem: pems){
                MetadataSchemaPermissionDao.delete(pem);
            }
		schemaId = null;
	}

	@Test
	public void persist() throws Exception
	{
		MetadataSchemaPermission pem = new MetadataSchemaPermission(schemaId, TEST_OWNER, PermissionType.READ);
		MetadataSchemaPermissionDao.persist(pem);
		Assert.assertNotNull(pem.getId(), "Schema permission did not persist.");
	}

	@Test(dependsOnMethods={"persist", "getByJobId"})
	public void delete() throws MetadataException
	{
		MetadataSchemaPermission pem = new MetadataSchemaPermission(schemaId, TEST_OWNER, PermissionType.READ);
		MetadataSchemaPermissionDao.persist(pem);
		Assert.assertNotNull(pem.getId(), "Schema permission did not persist.");
		
		MetadataSchemaPermissionDao.delete(pem);
		List<MetadataSchemaPermission> pems = MetadataSchemaPermissionDao.getBySchemaId(schemaId);
		Assert.assertFalse(pems.contains(pem), "Schema permission did not delete.");
	}

	@Test(dependsOnMethods={"persist"})
	public void getByJobId() throws Exception
	{
		MetadataSchemaPermission pem = new MetadataSchemaPermission(schemaId, TEST_OWNER, PermissionType.READ);
		MetadataSchemaPermissionDao.persist(pem);
		Assert.assertNotNull(pem.getId(), "Schema permission did not persist.");
		
		String oid2 = "anotherTestOID123456789";
		
		List<MetadataSchemaPermission> pems = MetadataSchemaPermissionDao.getBySchemaId(schemaId);
		Assert.assertNotNull(pems, "getBySchemaId did not return any permissions.");
		Assert.assertEquals(pems.size(), 1, "getBySchemaId did not return the correct number of permissions.");
		Assert.assertFalse(pems.contains(oid2), "getBySchemaId returned a permission from another schemaId.");
	}

	@Test(dependsOnMethods={"persist","delete"})
	public void getByUsernameAndSchemaId() throws Exception
	{
		MetadataSchemaPermission pem1 = new MetadataSchemaPermission(schemaId, TEST_OWNER, PermissionType.READ);
		MetadataSchemaPermissionDao.persist(pem1);
		Assert.assertNotNull(pem1.getId(), "Metadata Schema permission 1 did not persist.");
		
		MetadataSchemaPermission pem2 = new MetadataSchemaPermission(schemaId, TEST_SHARED_OWNER, PermissionType.READ);
		MetadataSchemaPermissionDao.persist(pem2);
		Assert.assertNotNull(pem2.getId(), "Job permission 2 did not persist.");
		
		MetadataSchemaPermission userPem = MetadataSchemaPermissionDao.getByUsernameAndSchemaId(TEST_OWNER, schemaId);
		Assert.assertNotNull(userPem, "getByUsernameAndSchemaId did not return the user permission.");
		Assert.assertEquals(userPem, pem1, "getBySchemaId did not return the correct metadata schema permission for the user.");
	}
}
