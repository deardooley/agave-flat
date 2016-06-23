package org.iplantc.service.io.uuid;

import java.io.IOException;

import org.apache.commons.lang.NotImplementedException;
import org.hibernate.Session;
import org.iplantc.service.common.dao.TenantDao;
import org.iplantc.service.common.exceptions.TenantException;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.model.Tenant;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AbstractUUIDTest;
import org.iplantc.service.common.uuid.UUIDEntityLookup;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.io.BaseTestCase;
import org.iplantc.service.io.model.JSONTestDataUtil;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.iplantc.service.systems.model.StorageSystem;
import org.json.JSONException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Should be implemented for every entity.
 * 
 * @author dooley
 *
 */
public abstract class AbstractUUIDEntityLookupTest<T> implements AbstractUUIDTest<T> {
	protected String destPath;
	protected StorageSystem system;
	
	@BeforeClass
	protected void beforeClass() throws TenantException, SystemArgumentException, JSONException, IOException {
		
		tearDownFiles();
		tearDownSystems();
		tearDownTenants();
		
		setUpSystems();
		setUpTenants();
		
		this.destPath = "testparentparentparent/testparentparent/testparent/test.dat";
	}
	
	@AfterMethod
	protected void afterMethod() {
		tearDownFiles();
	}
	
	@AfterClass
	protected void afterClass() {
		tearDownFiles();
		tearDownSystems();
		tearDownTenants();
	}
	
	protected void setUpTenants() throws TenantException {
		TenantDao tenantDao = new TenantDao();
		Tenant tenant = tenantDao.findByTenantId("iplantc.org");
		if (tenant == null) {
			tenant = new Tenant("iplantc.org", "https://agave.iplantc.org", "dooley@tacc.utexas.edu", "Test Admin");
			tenantDao.persist(tenant);
		}
	}
	
	protected void tearDownTenants() {
		Session session = HibernateUtil.getSession();
		session.createQuery("delete Tenant").executeUpdate();
		session.flush();
		session.close();
	}
	
	protected void setUpSystems() throws SystemArgumentException, JSONException, IOException {
		this.system = StorageSystem.fromJSON(JSONTestDataUtil.getInstance().getTestDataObject(JSONTestDataUtil.TEST_STORAGE_SYSTEM_FILE));
		this.system.setOwner(BaseTestCase.SYSTEM_OWNER);
		this.system.setPubliclyAvailable(true);
		this.system.setGlobalDefault(true);
		this.system.setAvailable(true);
		
		new SystemDao().persist(this.system);
	}
	
	protected void tearDownSystems() {
		Session session = HibernateUtil.getSession();
		session.createQuery("delete StorageSystem").executeUpdate();
		session.flush();
		session.close();
	}
	
	protected void setUpFiles() {
		throw new NotImplementedException("Intentionally not implemented");
	}
	
	protected void tearDownFiles() {
		Session session = HibernateUtil.getSession();
		session.createQuery("delete FileEvent").executeUpdate();
		session.createQuery("delete LogicalFile").executeUpdate();
		session.flush();
		session.close();
	}
	
	
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
	
	@DataProvider
	protected Object[][] resolveLogicalFilePathProvider() throws IOException {
		T testEntity = createEntity();
		return new Object[][] { 
				{ getEntityType(), getEntityUuid(testEntity), getUrlFromEntityJson(serializeEntityToJSON(testEntity)) } };
	}

	@Test(dataProvider = "resolveLogicalFilePathProvider")
	public void getResourceUrl(UUIDType uuidType, String uuid, String expectedUrl) {
		 _getResourceUrl(uuidType, uuid, expectedUrl);
	}
	
	protected void _getResourceUrl(UUIDType uuidType, String uuid, String expectedUrl) {
		try {
			String resolvedUrl = UUIDEntityLookup
					.getResourceUrl(uuidType, uuid);

			Assert.assertEquals(
					TenancyHelper.resolveURLToCurrentTenant(resolvedUrl),
					expectedUrl,
					"Resolved "
							+ uuidType.name().toLowerCase()
							+ " urls should match those created by the entity class itself.");
		} catch (UUIDException e) {
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
