package org.iplantc.service.common.uuid;

import java.io.IOException;

import org.iplantc.service.common.exceptions.UUIDException;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Should be implemented for every entity.
 * 
 * @author dooley
 *
 */
@Test(groups={"broken", "unit"})
public abstract class AbstractUUIDEntityLookupTest<T> implements AbstractUUIDTest<T> {

	/**
	 * Extracts the HAL from a json representation of an object and returns the
	 * _links.self.href value.
	 * 
	 * @param entityJson
	 * @return
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	protected String getUrlFromEntityJson(String entityJson)
			throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();

		return mapper.readTree(entityJson).get("_links").get("self")
				.get("href").asText();
	}
	
	@Test
	public void getResourceUrl() {
		 try {
			 T testEntity = createEntity();
				String resolvedUrl = UUIDEntityLookup
						.getResourceUrl(getEntityType(), getEntityUuid(testEntity));
				
				Assert.assertEquals(
						resolvedUrl,
						getUrlFromEntityJson(serializeEntityToJSON(testEntity)),
						"Resolved "
								+ getEntityType().name().toLowerCase()
								+ " urls should match those created by the entity class itself.");
			} catch (UUIDException | IOException  e) {
				Assert.fail("Resolving logical file path from UUID should not throw exception.", e);
			}
	}
	
//	@Test
//	public abstract void getAgaveRelativePathFromAbsolutePath() {
//		RemoteDataClient rdc = getEntity().getRemoteDataClient();
//		LogicalFile lf = new LogicalFile();
//		lf.
//	}
//
//		
//
//	/**
//	 * Generates a test case using the abstract methods implemented for this
//	 * class.
//	 * @return
//	 * @throws JsonProcessingException
//	 * @throws IOException
//	 */
//	protected Object[][] resolveLogicalFilePathProvider()
//			throws JsonProcessingException, IOException {
//
//		T testEntity = createEntity();
//		return new Object[][] { 
//				{ getEntityType(), getEntityUuid(testEntity), getUrlFromEntityJson(serializeEntityToJSON(testEntity)) } };
//	}
//
//	@Test(dataProvider = "resolveLogicalFilePathProvider")
//	public void resolveLogicalFilePath(UUIDType uuidType, String uuid, String expectedUrl) {
//		
//	}
}
