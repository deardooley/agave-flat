package org.iplantc.service.tags.model;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.tags.AbstractTagTest;
import org.iplantc.service.tags.TestDataHelper;
import org.iplantc.service.tags.exceptions.TagException;
import org.iplantc.service.tags.model.enumerations.PermissionType;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Test(groups={"integration"})
public class TagPermissionTest extends AbstractTagTest {
	
	@BeforeClass
	protected void beforeClass() throws Exception
	{
		super.beforeClass();
	}
	
	@AfterClass
	protected void afterClass() throws TagException {
		super.afterClass();
	}

	@DataProvider
	public Object[][] createTagPermissionProvider() {
	    return new Object[][] {
	      new Object[] { new TagPermission() },
	      new Object[] { new TagPermission(TestDataHelper.TEST_USER, PermissionType.ALL) },
	      new Object[] { new TagPermission(new Tag(), TestDataHelper.TEST_USER, PermissionType.ALL) },
	    };
	}
	
	@Test(dataProvider = "createTagPermissionProvider")
	public void createTagPermission(TagPermission tagPermission) throws UUIDException {
		Assert.assertNotNull(tagPermission.getCreated(),
				"created should not be null on tag creation");
		Assert.assertNotNull(tagPermission.getLastUpdated(),
				"lastUpdated should not be null on tag creation");
		Assert.assertNotNull(tagPermission.getUuid(),
				"uuid should not be null on tag creation");
		Assert.assertEquals(new AgaveUUID(tagPermission.getUuid()).getResourceType(),
				UUIDType.PERMISSION,
				"Tag should create uuid of same type upon creation.");
	}
	

	@Test
	public void TagPermissionStringPermissionType() {
	TagPermission permission = new TagPermission(TestDataHelper.TEST_SHAREUSER, PermissionType.ALL);
		Assert.assertEquals(permission.getPermission(),PermissionType.ALL,  "permission should be equal to constructor value.");
		Assert.assertEquals(permission.getUsername(),TestDataHelper.TEST_SHAREUSER,  "username should be equal to constructor value.");
		Assert.assertNull(permission.getEntityId(), "tag should be null if not passed in constructor.");
	}

	@Test
	public void TagPermissionTagStringPermissionType() {
		Tag tag = new Tag("foo", TestDataHelper.TEST_USER, new String[]{ defaultTenant.getUuid() });
		TagPermission permission = new TagPermission(tag, TestDataHelper.TEST_SHAREUSER, PermissionType.ALL);
		
		Assert.assertEquals(permission.getPermission(),PermissionType.ALL,  "permission should be equal to constructor value.");
		Assert.assertEquals(permission.getUsername(),TestDataHelper.TEST_SHAREUSER,  "username should be equal to constructor value.");
//		Assert.assertNull(permission, "permission should be null if not passed in constructor.");
		Assert.assertEquals(permission.getEntityId(), tag.getUuid(),  "tag should be null if not passed in constructor.");
		
	}

	@Test
	public void doClone() {
		Tag tag = new Tag("foo", TestDataHelper.TEST_USER, new String[]{ defaultTenant.getUuid() });
		TagPermission oldPermission = new TagPermission(tag, TestDataHelper.TEST_SHAREUSER, PermissionType.ALL);
		TagPermission newPermission = oldPermission.clone();
		Assert.assertEquals(oldPermission.getUsername(), newPermission.getUsername(), "Username should carry over when cloning a permission");
		Assert.assertNotEquals(oldPermission.getUuid(), newPermission.getUuid(), "uuid should not carry over when cloning a permission");
		Assert.assertEquals(oldPermission.getPermission(), newPermission.getPermission(), "permissionType should carry over when cloning a permission");
//		Assert.assertNotEquals(oldPermission.getCreated(), newPermission.getCreated(), "creation date should not carry over when cloning a permission");
		Assert.assertNull(newPermission.getId(), "id should be null on new object after clone.");
	}

	@DataProvider
	public Object[][] fromJSONProvider() {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode json = mapper.createObjectNode();
		json.put("username", TestDataHelper.TEST_SHAREUSER);
		json.put("permission", PermissionType.ALL.name());
		
	    return new Object[][] {
	      new Object[] { json, false, "valid username and permission should not thorw exceptions deserializing" },
	      new Object[] { mapper.createObjectNode().put("username", TestDataHelper.TEST_SHAREUSER), true, "missing permission should throw exception deserializing" },
	      new Object[] { json.deepCopy().put("permission", "foo"), true, "invalid permission should throw exception deserializing" },
	      new Object[] { mapper.createObjectNode().put("permission", PermissionType.ALL.name()), true, "missing username should thorw exception deserializing" },
	      new Object[] { json.deepCopy().put("username", ""), true, "empty username should thorw exception deserializing" },
	      new Object[] { json.deepCopy().putNull("username"), true, "null username should thorw exception deserializing" },
	      new Object[] { json.deepCopy().put("username", "aa"), true, "invalid username should thorw exception deserializing" },
	    };
	}
	
	@Test(dataProvider = "fromJSONProvider")
	public void fromJSON(ObjectNode json, boolean shouldThrowException, String message) {
		Tag tag = new Tag("foo", TestDataHelper.TEST_USER, new String[]{ defaultTenant.getUuid() });
		try {
			TagPermission.fromJSON(json, tag.getUuid());
			Assert.assertFalse(shouldThrowException, message);
		} catch (Exception e) {
			if (!shouldThrowException) 
				Assert.fail(message, e);
		}
	}
	
	@DataProvider
	public Object[][] fromJSONMapsNullAndEmptyToNoneProvider() {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode json = mapper.createObjectNode();
		json.put("username", TestDataHelper.TEST_SHAREUSER);
		json.put("permission", PermissionType.ALL.name());
		
	    return new Object[][] {
	      new Object[] { json.deepCopy().put("permission", ""), false, "empty permission value should map to NONE" },
	      new Object[] { json.deepCopy().putNull("permission"), false, "null permission value should map to NONE" },
	    };
	}
	
	@Test(dataProvider = "fromJSONMapsNullAndEmptyToNoneProvider")
	public void fromJSONMapsNullAndEmptyToNone(ObjectNode json, boolean shouldThrowException, String message) {
		Tag tag = new Tag("foo", TestDataHelper.TEST_USER, new String[]{ defaultTenant.getUuid() });
		try {
			TagPermission.fromJSON(json, tag.getUuid());
			Assert.assertFalse(shouldThrowException, message);
		} catch (Exception e) {
			Assert.fail(message, e);
		}
	}
	
	@DataProvider
	public Object[][] fromJSONMapsCaseInsensitivePermissionProvider() {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode json = mapper.createObjectNode();
		json.put("username", TestDataHelper.TEST_SHAREUSER);
		json.put("permission", PermissionType.ALL.name());
		
		ArrayList<Object[]> testCases = new ArrayList<Object[]>();
		
		for (PermissionType pem: PermissionType.values()) {
			testCases.add(new Object[] { 
					json.deepCopy().put("permission", StringUtils.capitalize(pem.name())), 
					pem,
					"permission value should be case insensitive" });
			
			testCases.add(new Object[] { 
					json.deepCopy().put("permission", pem.name().toLowerCase()), 
					pem,
					"permission value should be case insensitive" });
			
			testCases.add(new Object[] { 
					json.deepCopy().put("permission", pem.name().toUpperCase()), 
					pem,
					"permission value should be case insensitive" });
		}
		
	    return testCases.toArray(new Object[][]{});
	}
	
	@Test(dataProvider = "fromJSONMapsCaseInsensitivePermissionProvider")
	public void fromJSONMapsCaseInsensitivePermission(ObjectNode json, PermissionType expectedPermissionType, String message) {
		Tag tag = new Tag("foo", TestDataHelper.TEST_USER, new String[]{ defaultTenant.getUuid() });
		try {
			TagPermission permission = TagPermission.fromJSON(json, tag.getUuid());
			Assert.assertEquals(permission.getPermission(), expectedPermissionType, message );
		} 
		catch (Exception e) {
			Assert.fail(message, e);
		}
	}

	@Test
	public void toJSON() 
	{
		try 
		{
			Tag tag = createTag();
			TagPermission permission = new TagPermission(tag, TestDataHelper.TEST_SHAREUSER, PermissionType.READ);
			
			ObjectNode json = (ObjectNode) new ObjectMapper().readTree(permission.toJSON());
			
			Assert.assertTrue(json.get("_links").get("self").has("href"), "No hypermedia found in serialized response");
			Assert.assertTrue(json.get("_links").has("tag"), "No permissions reference found in serialized response");
//			Assert.assertTrue(json.get("_links").has("permissions"), "No history reference found in serialized response");
			Assert.assertTrue(json.get("_links").has("profile"), "No owner reference found in serialized response");
		} 
		catch (Exception e) {
			Assert.fail("Permission serialization should never throw exception", e);
		}
	}
}
