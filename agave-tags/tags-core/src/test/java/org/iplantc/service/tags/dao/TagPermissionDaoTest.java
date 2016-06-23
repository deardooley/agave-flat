package org.iplantc.service.tags.dao;

import java.io.IOException;
import java.util.List;

import static org.iplantc.service.tags.TestDataHelper.*;

import org.iplantc.service.tags.AbstractTagTest;
import org.iplantc.service.tags.TestDataHelper;
import org.iplantc.service.tags.dao.TagDao;
import org.iplantc.service.tags.exceptions.TagException;
import org.iplantc.service.tags.exceptions.TagPermissionException;
import org.iplantc.service.tags.model.Tag;
import org.iplantc.service.tags.model.TagPermission;
import org.iplantc.service.tags.model.enumerations.PermissionType;
import org.json.JSONException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TagPermissionDaoTest extends AbstractTagTest { //extends AbstractDaoTest {

//	private PermissionDao tagPermissionDao;
	
	@BeforeClass
	protected void beforeClass() throws Exception
	{
		super.beforeClass();
//		tagPermissionDao = new PermissionDao();
	}
	
	@AfterClass
	protected void afterClass() throws TagException {
		super.afterClass();
	}

	@AfterMethod
	public void tearDowntag() throws Exception
	{
		clearTags();
	}

	@Test
	public void persist() throws Exception
	{
		TagPermission pem = new TagPermission(createTag(), TEST_USER, PermissionType.READ);
		PermissionDao.persist(pem);
		Assert.assertNotNull(pem.getId(), "tag permission did not persist.");
	}

	@Test(dependsOnMethods={"persist"})
	public void delete() throws TagException, TagPermissionException, JSONException, IOException
	{
		TagPermission pem = new TagPermission(createTag(), TEST_USER, PermissionType.READ);
		PermissionDao.persist(pem);
		Assert.assertNotNull(pem.getId(), "tag permission did not persist.");
		
		PermissionDao.delete(pem);
		TagPermission userPem = PermissionDao.getUserTagPermissions(TEST_USER, pem.getEntityId());
		Assert.assertNull(userPem, "A tag permission should be returned after deleting.");
	}

	@Test(dependsOnMethods={"delete"})
	public void getByUuid() throws Exception
	{
		Tag tag = createTag();
		TagPermission pem = new TagPermission(tag, TEST_USER, PermissionType.READ);
		PermissionDao.persist(pem);
		Assert.assertNotNull(pem.getId(), "tag permission did not persist.");
		
		Tag tag2 = createTag();
		TagPermission pem2 = new TagPermission(tag2, TEST_USER, PermissionType.READ);
		PermissionDao.persist(pem2);
		Assert.assertNotNull(pem2.getId(), "tag permission did not persist.");
		
		
		List<TagPermission> pems = PermissionDao.getEntityPermissions(pem.getEntityId());
		Assert.assertNotNull(pems, "getBytagId did not return any permissions.");
		Assert.assertEquals(pems.size(), 1, "getBytagId did not return the correct number of permissions.");
		Assert.assertFalse(pems.contains(pem2), "getBytagId returned a permission from another tag.");
	}

	@Test(dependsOnMethods={"getByUuid"})
	public void getByUsernameAndtagId() throws Exception
	{
		Tag tag = createTag();
		TagPermission pem1 = new TagPermission(tag, TEST_OTHERUSER, PermissionType.READ);
		PermissionDao.persist(pem1);
		Assert.assertNotNull(pem1.getId(), "tag permission 1 did not persist.");
		
		TagPermission pem2 = new TagPermission(tag, TEST_SHAREUSER, PermissionType.READ);
		PermissionDao.persist(pem2);
		Assert.assertNotNull(pem2.getId(), "tag permission 2 did not persist.");
		
		TagPermission userPem = PermissionDao.getUserTagPermissions(TEST_SHAREUSER, tag.getUuid());
		Assert.assertNotNull(userPem, "getByUsernameAndtagId did not return the user permission.");
		Assert.assertEquals(userPem, pem2, "getBytagId did not return the correct tag permission for the user.");
	}
}
