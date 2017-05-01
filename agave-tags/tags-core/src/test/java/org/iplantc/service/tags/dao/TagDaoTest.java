package org.iplantc.service.tags.dao;

import static org.iplantc.service.tags.TestDataHelper.TEST_OTHERUSER;
import static org.iplantc.service.tags.TestDataHelper.TEST_SHAREUSER;
import static org.iplantc.service.tags.TestDataHelper.TEST_USER;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.tags.AbstractTagTest;
import org.iplantc.service.tags.model.Tag;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups={"integration"})
public class TagDaoTest extends AbstractTagTest 
{
	@BeforeMethod
	public void beforeMethod() throws Exception {
		clearTags();
		clearNotifications();
	}

	@AfterMethod
	public void afterMethod() throws Exception{
		clearTags();
		clearNotifications();
	}

	@DataProvider
	public Object[][] persistProvider() throws Exception
	{
		return new Object[][] { 
				{ createTag(), "Failed to persist tag", false },
		};
	}
	
	@Test(dataProvider="persistProvider")
	public void persist(Tag tag, String errorMessage, boolean shouldThrowException)
	{
		try 
		{
			dao.persist(tag);
			Assert.assertNotNull(tag.getId(), "Tag should have an id after persisting");
		} 
		catch(Exception e) 
		{
			if (!shouldThrowException) {
				Assert.fail(errorMessage, e);
			}
		}
	}
	
	@Test(dependsOnMethods={"persist"})
	public void getAll()
	{
		try 
		{
			List<Tag> tags = new ArrayList<Tag>();
			
			for (int i=0;i<5; i++) {
				Tag tag = new Tag("tag_test_" + i, TEST_USER, new String[] {defaultTenant.getUuid()});
				dao.persist(tag);
				tags.add(tag);
			}
			List<Tag> savedTags = dao.getAll();
			
			Assert.assertFalse(savedTags.isEmpty(), "getAll should not return an empty set of tags when tags exist.");
			Assert.assertEquals(savedTags.size(), tags.size(), "All tags for a tenant should be returned ");
		} 
		catch(Exception e) 
		{
			Assert.fail("Failed to retrieve tags from the db.", e);
		}
	}
	
	@DataProvider
	public Object[][] findByUuidProvider() throws Exception
	{
		return new Object[][] { 
				{ createTag(), "Failed to find monitor by uuid", false },
		};
	}
	
	@Test(dataProvider="findByUuidProvider", dependsOnMethods={"getAll"})
	public void findByUuid(Tag tag, String errorMessage, boolean shouldThrowException)
	{
		try 
		{
			dao.persist(tag);
			Assert.assertNotNull(tag.getId(), "Failed to generate an monitor ID.");
			
			String tagUuid = tag.getUuid();
			
			tag = dao.findByUuid(tagUuid);
			
			Assert.assertNotNull(tag, "Tag was not found in db.");
		} 
		catch(Exception e) 
		{
			if (!shouldThrowException) {
				Assert.fail(errorMessage, e);
			}
		}
	}
	
	@Test(dependsOnMethods={"findByUuid"})
	public void getUserTags() throws Exception
	{
		try 
		{
			final Tag tag = new Tag("tag_test_1", TEST_USER, new String[] {defaultTenant.getUuid()});
			dao.persist(tag);
			Assert.assertNotNull(tag.getId(), "Failed to generate an monitor ID.");
			
			final Tag tag2 = new Tag("tag_test_2", TEST_USER, new String[] {defaultTenant.getUuid()});
			dao.persist(tag2);
			Assert.assertNotNull(tag2.getId(), "Failed to generate an monitor ID.");
			
			final Tag tag3 = new Tag("tag_test_3", TEST_OTHERUSER, new String[] {new AgaveUUID(UUIDType.NOTIFICATION).toString()});
			dao.persist(tag3);
			Assert.assertNotNull(tag3.getId(), "Failed to generate an monitor ID.");
			
			List<Tag> tags = dao.getUserTags(TEST_USER, 10000, 0);
			
			Assert.assertNotNull(tags, "Tags with matching uuid should be returned from db.");
			Assert.assertEquals(tags.size(), 2, "Incorrect number of tags returned on findByUuid");
			Assert.assertTrue(CollectionUtils.exists(tags, new Predicate() {
				@Override
				public boolean evaluate(Object object) {
					return StringUtils.equals(tag.getUuid(), ((Tag)object).getUuid());
				}
				
			}), "Full result set not returned from findByUuid");
			Assert.assertTrue(CollectionUtils.exists(tags, new Predicate() {
				@Override
				public boolean evaluate(Object object) {
					return StringUtils.equals(tag2.getUuid(), ((Tag)object).getUuid());
				}
				
			}), "Full result set not returned from findByUuid");
		} 
		catch(Exception e) {
			Assert.fail("Querying by uuid should not throw exception", e);
		}
	}
	
	@DataProvider(name="deleteProvider")
	public Object[][] deleteProvider() throws Exception
	{
		return new Object[][] { 
				{ createTag(), "Failed to delete tag", false },
		};
	}

	@Test(dataProvider="deleteProvider",dependsOnMethods={"findByUuid"})
	public void delete(Tag tag, String errorMessage, boolean shouldThrowException)
	{
		try 
		{
			dao.persist(tag);
			Assert.assertNotNull(tag.getId(), "Failed to generate an monitor ID.");
			
			String tagUuid = tag.getUuid();
			
			tag = dao.findByUuid(tagUuid);
			
			Assert.assertNotNull(tag, "Tag was not found in db.");
			
			dao.delete(tag);
			
			tag = dao.findByUuid(tagUuid);
			
			Assert.assertNull(tag, "Tag was not found deleted from the db.");
		} 
		catch(Exception e) 
		{
			if (!shouldThrowException) {
				Assert.fail(errorMessage, e);
			}
		}
	}
	
	@DataProvider(name="doesTagNameExistForUserProvider")
	public Object[][] doesTagNameExistForUserProvider() throws Exception
	{
		return new Object[][] { 
				{ TEST_USER, TEST_USER, true },
				{ TEST_USER, TEST_SHAREUSER, false },
				{ TEST_USER, TEST_OTHERUSER, false },
				
				{ TEST_SHAREUSER, TEST_SHAREUSER, true },
				{ TEST_SHAREUSER, TEST_USER, false },
				{ TEST_SHAREUSER, TEST_OTHERUSER, false },
				
				{ TEST_OTHERUSER, TEST_OTHERUSER, true },
				{ TEST_OTHERUSER, TEST_USER, false },
				{ TEST_OTHERUSER, TEST_SHAREUSER, false },
		};
	}
	
	@Test(dependsOnMethods={"delete"}, dataProvider="doesTagNameExistForUserProvider")
	public void doesTagNameExistForUser(String owner, String searchUsername, boolean shouldTagNameExist) throws Exception
	{
		try 
		{
			Tag tag = createTag();
			tag.setOwner(owner);
			dao.persist(tag);
			
			Assert.assertNotNull(tag.getId(), "testuserTag was not saved in db.");
			
			Assert.assertEquals(dao.doesTagNameExistForUser(searchUsername, tag.getName()), 
								shouldTagNameExist,
								"Test for tag name existence should return true when the tag exists.");
		} 
		catch(Exception e) 
		{
			Assert.fail("Looking up username should not throw exceptions.", e);
		}
	}
}
