package org.iplantc.service.tags.managers;

import org.iplantc.service.tags.AbstractTagTest;
import org.iplantc.service.tags.TestDataHelper;
import org.iplantc.service.tags.model.Tag;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

@Test(groups={"integration"})
public class TagManagerTest extends AbstractTagTest {

	@DataProvider
	private Object[][] addTagForUserProvider() throws Exception
	{	
		return new Object[][] {
			{ createTag(), "Tag should insert.", true },
		};
	}

	@Test(dataProvider = "addTagForUserProvider")
	public void addTagForUser(Tag tag, String errorMessage, boolean shouldThrowException)
	{
		try {
			dao.persist(tag);
			ObjectNode json = (ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_TAG);
			
			TagManager manager = new TagManager();
			Tag savedTag = manager.addTagForUser(json, TestDataHelper.TEST_USER);
			
			Assert.assertEquals(savedTag.getUuid(), tag.getUuid(), "Tag uuid should not change after saving");
			Assert.assertEquals(savedTag.getOwner(), tag.getOwner(), "Tag owner should not change after saving");
			Assert.assertFalse(shouldThrowException, errorMessage);
		}
		catch (Throwable e) {
			if (!shouldThrowException) {
				Assert.fail("Unexpected error occurred", e);
			}
		}
	}

  @Test
  public void deleteUserTag() {
    throw new RuntimeException("Test not implemented");
  }

  @Test
  public void getDao() {
    throw new RuntimeException("Test not implemented");
  }

  @Test
  public void getEventProcessor() {
    throw new RuntimeException("Test not implemented");
  }

  @Test
  public void setDao() {
    throw new RuntimeException("Test not implemented");
  }

  @Test
  public void setEventProcessor() {
    throw new RuntimeException("Test not implemented");
  }

  @Test
  public void updateTag() {
    throw new RuntimeException("Test not implemented");
  }

  @Test
  public void updateTagAssociationId() {
    throw new RuntimeException("Test not implemented");
  }
}
