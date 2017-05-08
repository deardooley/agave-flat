package org.iplantc.service.apps.dao;

import java.io.IOException;
import java.util.List;

import org.iplantc.service.apps.exceptions.SoftwareException;
import org.iplantc.service.apps.model.Software;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups={"integration"})
public class SoftwareDaoTest extends AbstractDaoTest {
	@Test(dataProvider = "dp")
	public void f(Integer n, String s) {
	}

	@DataProvider
	public Object[][] dp() {
		return new Object[][] { new Object[] { 1, "a" },
				new Object[] { 2, "b" }, };
	}

	@BeforeClass
	public void beforeClass() {
	}

	@AfterClass
	public void afterClass() {
	}

	@Test
	public void delete() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void findMatchingStringMapSearchTermObjectboolean() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void findMatchingStringMapSearchTermObjectintintboolean() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getLong() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getString() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getActiveJobsForSoftware() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getActiveUserJobsForSoftware() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getAllboolean() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getAll() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getAllByAttribute() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getAllBySemanticName() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getAllBySystemId() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getAllByTag() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getAllPrivate() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getAllPrivateString() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getAllPrivateByName() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getAllPublic() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getAllPublicString() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getAllPublicByName() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getByName() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getMaxRevisionForPublicSoftware() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getMaxVersionForSoftwareName() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getPreviousVersionsOfPublshedSoftware() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getSession() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getSoftwareByNameAndVersion() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getSoftwareByUniqueName() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getUserAppsStringboolean() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getUserAppsStringbooleanintint() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getUserAppsByAttribute() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void getUserAppsBySystemId() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void merge() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void persist() {
		throw new RuntimeException("Test not implemented");
	}

	@Test
	public void replace() throws SoftwareException, JSONException, IOException {
		JSONObject json = jtd.getTestDataObject(FORK_SOFTWARE_TEMPLATE_FILE);
		Software oldSoftware = Software.fromJSON(json, SYSTEM_OWNER);
		SoftwareDao.persist(oldSoftware);

		Software newSoftware = Software.fromJSON(json, SYSTEM_OWNER);
		SoftwareDao.replace(oldSoftware, newSoftware);
		
		Assert.assertNotEquals(oldSoftware.getId(), newSoftware.getId(),
				"New software should not be assigned the id of the old software");
		
		// search for new and old software objects
		Software oldSoftwareResult = SoftwareDao.get(oldSoftware.getId());
		Software newSoftwareResult = SoftwareDao.get(newSoftware.getId());
		
		Assert.assertNull(oldSoftwareResult, "Search for old software by id should return null.");
		
		Assert.assertNotNull(newSoftwareResult, "Search for new software by id should return new software.");
		
		
		newSoftwareResult = SoftwareDao.getSoftwareByUniqueName(newSoftwareResult.getUniqueName());
		
		Assert.assertNotNull(newSoftwareResult, "Search for new software by unique name should return new software.");
		
		
		List<Software> nameResults = SoftwareDao.getByName(newSoftware.getName(), false, true);
		
		Assert.assertEquals(nameResults.size(), 1, "Search for new software by name should return only the new software object.");
		
		Assert.assertNotNull(newSoftwareResult, "Search for new software by id should return new software.");

	}
}
