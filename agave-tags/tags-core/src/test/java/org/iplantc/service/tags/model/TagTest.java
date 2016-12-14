package org.iplantc.service.tags.model;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.tags.AbstractTagTest;
import org.iplantc.service.tags.TestDataHelper;
import org.iplantc.service.tags.exceptions.TagException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TagTest extends AbstractTagTest {

	
	@BeforeClass
	protected void beforeClass() throws Exception
	{
		super.beforeClass();
	}
	
	@AfterClass
	protected void afterClass() throws TagException {
		super.afterClass();
	}
	
	@Test
	public void constructTag()
	{
		Tag tag = new Tag();
		Assert.assertNotNull(tag.getUuid(), "UUID not set on instantiation.");
		Assert.assertNotNull(tag.getTenantId(), "Tenant id not set on instantiation.");
		Assert.assertNotNull(tag.getCreated(), "Creation date not set on instantiation.");
		Assert.assertNotNull(tag.getLastUpdated(), "Last updated date not set on instantiation.");
		Assert.assertNotNull(tag.getTaggedResources(), "taggedResources not set on instantiation.");
	}

	@DataProvider
	private Object[][] setNameProvider()
	{
		return new Object[][] {
				{ "foo", false, "minimum alpha characters should not throw exception" },
				{ "", true, "empty name should throw exception" },
				{ null, true, "null name should throw exception" },
				{ "f", true, "less than 3 alpha characters should throw exception" },
				{ "ff", true, "less than 3 alpha characters should throw exception" },
		};
	}
	
	@Test(dependsOnMethods={"constructTag"}, dataProvider="setNameProvider")
	public void setName(String name, boolean shouldThrowException, String message) 
	{
		try 
		{
			ObjectNode json = ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_TAG));
			if (name == null) {
				json.putNull("name");
			} else {
				json.put("name", name);
			}
			Tag tag = Tag.fromJSON(json);
			
			if (shouldThrowException) {
				Assert.fail(message);
			}
		} 
		catch (Exception e) 
		{
			if (!shouldThrowException) {
				Assert.fail(message, e);
			}
		}
	}
	
	@DataProvider
	private Object[][] setTaggedResourcesFailsInvalidAssociatedIdsProvider()
	{
		ObjectMapper mapper = new ObjectMapper();
		
		return new Object[][] {
				{ mapper.createArrayNode(), true, "empty associatedIds array should throw exception" },
				{ mapper.createArrayNode().addNull(), true, "null associatedIds array should throw exception" },
				{ mapper.createArrayNode().add(""), true, "empty uuid in associatedIds array should throw exception" },
				{ mapper.createArrayNode().add("foo"), true, "invalid uuid in associatedIds array should throw exception" },
				
		};
	}
	
	
	@Test(dependsOnMethods={"setName"}, dataProvider="setTaggedResourcesFailsInvalidAssociatedIdsProvider")
	public void setTaggedResourcesFailsInvalidAssociatedIds(ArrayNode uuids, boolean shouldThrowException, String message) 
	{
		try 
		{
			ObjectNode json = ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_TAG));
			if (uuids == null) {
				json.putNull("associatedUuids");
			} else {
				json.put("associatedUuids", uuids);
			}
			
			Tag.fromJSON(json);
			
			if (shouldThrowException) {
				Assert.fail(message);
			}
		} 
		catch (Exception e) 
		{
			if (!shouldThrowException) {
				Assert.fail(message, e);
			}
		}
	}
	
	@DataProvider
	private Object[][] setTaggedResourcesProvider()
	{
		ObjectMapper mapper = new ObjectMapper();
		
		return new Object[][] {
			{ mapper.createArrayNode().add(defaultTenant.getUuid()), new String[] {defaultTenant.getUuid()}, "valid associatedIds array should succeed" },
			{ mapper.createArrayNode().add(defaultTenant.getUuid()).add(defaultTenant.getUuid()), new String[] {defaultTenant.getUuid()}, "valid associatedIds should be stripped to unique" },
		};
	}
	
	
	@Test(dependsOnMethods={"setTaggedResourcesFailsInvalidAssociatedIds"}, dataProvider="setTaggedResourcesProvider")
	public void setTaggedResources(ArrayNode provided, String[] expected, String message) 
	{
		try 
		{
			ObjectNode json = ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_TAG));
			json.put("associatedIds", provided);
			
			Tag tag = Tag.fromJSON(json);
			
			Assert.assertEquals(tag.getTaggedResourcesAsArray().length, expected.length, message);
			Assert.assertTrue(Arrays.asList(tag.getTaggedResourcesAsArray()).containsAll(Arrays.asList(expected)), message);
		} 
		catch (Exception e) {
			Assert.fail(message, e);
		}
	}
	
	@DataProvider
	private Object[][] toJSONProvider()
	{
		ObjectMapper mapper = new ObjectMapper();
		List<Object[]> testCases = new ArrayList<Object[]>();
		for (UUIDType resourceType: UUIDType.values()) {
			Tag tag = new Tag(resourceType.name() + "_TAG", TestDataHelper.TEST_USER, new String[] {new AgaveUUID(resourceType).toString()});
			testCases.add(new Object[] { tag, resourceType, resourceType.name() + " should deserialize in hypermedia response" });
		}
			
			
		return testCases.toArray(new Object[][]{});
	}
	
	
	@Test( dataProvider="toJSONProvider")
	public void toJSON(Tag tag, UUIDType resourceType, String message) 
	{
		try 
		{
			ObjectNode json = (ObjectNode) new ObjectMapper().valueToTree(tag);
			
			Assert.assertTrue(json.get("_links").get("self").has("href"), "No hypermedia found in serialized response");
			Assert.assertTrue(json.get("_links").has("permissions"), "No permissions reference found in serialized response");
			Assert.assertTrue(json.get("_links").has("resources"), "No resources reference found in serialized response");
//			Assert.assertTrue(json.get("_links").has("permissions"), "No history reference found in serialized response");
			Assert.assertTrue(json.get("_links").has("owner"), "No owner reference found in serialized response");
		} 
		catch (Exception e) {
			Assert.fail(message, e);
		}
	}
}
