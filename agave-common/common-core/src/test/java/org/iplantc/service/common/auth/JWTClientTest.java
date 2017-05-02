package org.iplantc.service.common.auth;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups = {"broken", "unit"} )
public class JWTClientTest 
{
	public static String JWT_TEST_FOLDER = "src/test/resources/jwt/";
	@BeforeClass
	public void beforeClass() {
	}

	@AfterClass
	public void afterClass() {
	}
	
	@DataProvider
	public Object[][] parseProvider() throws Exception 
	{
		String jwtToken = FileUtils.readFileToString(new File(JWT_TEST_FOLDER + "stubbs.jwt"));
		String jwtToken2 = FileUtils.readFileToString(new File(JWT_TEST_FOLDER + "stubbs2.jwt"));
		return new Object[][] { 
				{ jwtToken, false, "Valid token should fail to parse" },
				{ jwtToken2, false, "Valid token should parse successfully" },
		};
	}

	
	@Test(dataProvider = "parseProvider")
	public void parse(String serializedToken, boolean shouldThrowException, String message) 
	{
		try 
		{
			Assert.assertTrue(JWTClient.parse(serializedToken, "iplantc-org"), message);
			
			Assert.assertFalse(shouldThrowException, message);
		}
		catch (Exception e) {
			if (!shouldThrowException) {
				Assert.fail("Unexpected error occurred parsing jwt token", e);
			}
		}
	}
	
	@Test(dataProvider = "parseProvider", dependsOnMethods = {"parse"})
	public void getCurrentJWSObject(String serializedToken, boolean shouldThrowException, String message) {
		try 
		{
			Assert.assertTrue(JWTClient.parse(serializedToken, "iplantc-org"), message);
			
			Assert.assertFalse(shouldThrowException, message);
			
			Assert.assertNotNull(JWTClient.getCurrentJWSObject(), "JWT claims set was null.");
			Assert.assertTrue(JWTClient.getCurrentJWSObject().size() > 0, "JWT claims set was empty.");
		}
		catch (Exception e) {
			if (!shouldThrowException) {
				Assert.fail("Unexpected error occurred parsing jwt token", e);
			}
		}
	}

	@DataProvider
	public Object[][] getCurrentEndUserProvider() throws Exception 
	{
		String jwtToken = FileUtils.readFileToString(new File(JWT_TEST_FOLDER + "stubbs.jwt"));
		String jwtToken2 = FileUtils.readFileToString(new File(JWT_TEST_FOLDER + "stubbs2.jwt"));
		return new Object[][] { 
				{ jwtToken, "tacc/jstubbs", false, "Decoded end user does not match expected end user" },
				{ jwtToken2, "testadmin@test.com", false, "Decoded end user does not match expected end user" },
		};
	}
	
	@Test(dataProvider = "getCurrentEndUserProvider", dependsOnMethods={"getCurrentJWSObject"})
	public void getCurrentEndUser(String serializedToken, String expectedUsername, boolean shouldThrowException, String message) {
		try 
		{
			Assert.assertTrue(JWTClient.parse(serializedToken, "iplantc-org"), message);
			
			Assert.assertFalse(shouldThrowException, message);
			Assert.assertNotNull(JWTClient.getCurrentJWSObject(), "JWT claims set was null.");
			Assert.assertTrue(JWTClient.getCurrentJWSObject().size() > 0, "JWT claims set was empty.");
			
			String endUsername = JWTClient.getCurrentEndUser();
			
			Assert.assertEquals(endUsername, expectedUsername, 
					"Incorrect enduser claim retrieved from token. Found " + endUsername + ", expected " + expectedUsername);
		}
		catch (Exception e) {
			if (!shouldThrowException) {
				Assert.fail("Unexpected error occurred parsing jwt token", e);
			}
		}
	}
	
	@DataProvider
	public Object[][] getCurrentSubscriberProvider() throws Exception 
	{
		String jwtToken = FileUtils.readFileToString(new File(JWT_TEST_FOLDER + "stubbs.jwt"));
		String jwtToken2 = FileUtils.readFileToString(new File(JWT_TEST_FOLDER + "stubbs2.jwt"));
		return new Object[][] { 
				{ jwtToken, "TACC/jstubbs", false, "Decoded subscriber not match expected subscriber" },
				{ jwtToken2, "testadmin@test.com", false, "Decoded subscriber not match expected subscriber" },
		};
	}

	@Test(dataProvider = "getCurrentSubscriberProvider", dependsOnMethods={"getCurrentJWSObject"})
	public void getCurrentSubscriber(String serializedToken, String expectedCurrentSubscriber, boolean shouldThrowException, String message) 
	{
		try 
		{
			Assert.assertTrue(JWTClient.parse(serializedToken, "iplantc-org"), message);
			
			Assert.assertFalse(shouldThrowException, message);
			Assert.assertNotNull(JWTClient.getCurrentJWSObject(), "JWT claims set was null.");
			Assert.assertTrue(JWTClient.getCurrentJWSObject().size() > 0, "JWT claims set was empty.");
			
			String currentSubscriber = JWTClient.getCurrentSubscriber();
			
			Assert.assertEquals(currentSubscriber, expectedCurrentSubscriber, 
					"Incorrect subscriber claim retrieved from token. Found " + currentSubscriber + ", expected " + expectedCurrentSubscriber);
		}
		catch (Exception e) {
			if (!shouldThrowException) {
				Assert.fail("Unexpected error occurred parsing jwt token", e);
			}
		}
	}
	
	@DataProvider
	public Object[][] getCurrentTenantProvider() throws Exception 
	{
		String jwtToken = FileUtils.readFileToString(new File(JWT_TEST_FOLDER + "stubbs.jwt"));
		String jwtToken2 = FileUtils.readFileToString(new File(JWT_TEST_FOLDER + "stubbs2.jwt"));
		return new Object[][] { 
				{ jwtToken, "iplantc.org", false, "Decoded subscriber not match expected subscriber" },
				{ jwtToken2, "test.com", false, "Decoded subscriber not match expected subscriber" },
		};
	}

	@Test(dataProvider = "getCurrentTenantProvider", dependsOnMethods={"getCurrentJWSObject"})
	public void getCurrentTenant(String serializedToken, String expectedCurrentTenant, boolean shouldThrowException, String message) 
	{
		try 
		{
			Assert.assertTrue(JWTClient.parse(serializedToken, expectedCurrentTenant), message);
			
			Assert.assertFalse(shouldThrowException, message);
			Assert.assertNotNull(JWTClient.getCurrentJWSObject(), "JWT claims set was null.");
			Assert.assertTrue(JWTClient.getCurrentJWSObject().size() > 0, "JWT claims set was empty.");
			
			String currentTenant = JWTClient.getCurrentTenant();
			
			Assert.assertEquals(currentTenant, expectedCurrentTenant, 
					"Incorrect tenant claim retrieved from token. Found " + currentTenant + ", expected " + expectedCurrentTenant);
		}
		catch (Exception e) {
			if (!shouldThrowException) {
				Assert.fail("Unexpected error occurred parsing jwt token", e);
			}
		}
	}
	
	@DataProvider
	public Object[][] getCurrentAdminProvider() throws Exception 
	{
		String jwtUserToken = FileUtils.readFileToString(new File(JWT_TEST_FOLDER + "stubbs.jwt"));
		String jwtAdminToken = FileUtils.readFileToString(new File(JWT_TEST_FOLDER + "admin.jwt"));
		String jwtSuperToken = FileUtils.readFileToString(new File(JWT_TEST_FOLDER + "superadmin.jwt"));
		return new Object[][] { 
				{ jwtUserToken, false, false, "User role should not have admin" },
				{ jwtAdminToken, true, false, "Admin role should have admin" },
				{ jwtSuperToken, true, false, "Superadmin role should have admin" }
		};
	}
	
	@Test(dataProvider = "getCurrentAdminProvider", dependsOnMethods={"getCurrentTenant"})
	public void isTenantAdmin(String serializedToken, boolean expectedIsAdmin, boolean shouldThrowException, String message) 
	{
		try 
		{
			Assert.assertTrue(JWTClient.parse(serializedToken, "iplantc-org"), message);
			
			Assert.assertFalse(shouldThrowException, message);
			Assert.assertNotNull(JWTClient.getCurrentJWSObject(), "JWT claims set was null.");
			Assert.assertTrue(JWTClient.getCurrentJWSObject().size() > 0, "JWT claims set was empty.");
			
			boolean admin = JWTClient.isTenantAdmin();
			
			Assert.assertEquals(admin, expectedIsAdmin, 
					"Unexpected user role. Found " + admin + ", expected " + expectedIsAdmin);
		}
		catch (Exception e) {
			if (!shouldThrowException) {
				Assert.fail("Unexpected error occurred parsing jwt token", e);
			}
		}
	}
	
	@DataProvider
	public Object[][] getCurrentSuperAdminProvider() throws Exception 
	{
		String jwtUserToken = FileUtils.readFileToString(new File(JWT_TEST_FOLDER + "stubbs.jwt"));
		String jwtAdminToken = FileUtils.readFileToString(new File(JWT_TEST_FOLDER + "admin.jwt"));
		String jwtSuperToken = FileUtils.readFileToString(new File(JWT_TEST_FOLDER + "superadmin.jwt"));
		return new Object[][] { 
				{ jwtUserToken, false, false, "User role should not have superadmin" },
				{ jwtAdminToken, false, false, "Admin role should not have superadmin" },
				{ jwtSuperToken, true, false, "Superadmin role should have superadmin" },
		};
	}
	
	@Test(dataProvider = "getCurrentSuperAdminProvider", dependsOnMethods={"isTenantAdmin"})
	public void isSuperAdmin(String serializedToken, boolean expectedIsSuperAdmin, boolean shouldThrowException, String message) 
	{
		try 
		{
			Assert.assertTrue(JWTClient.parse(serializedToken, "iplantc-org"), message);
			
			Assert.assertFalse(shouldThrowException, message);
			Assert.assertNotNull(JWTClient.getCurrentJWSObject(), "JWT claims set was null.");
			Assert.assertTrue(JWTClient.getCurrentJWSObject().size() > 0, "JWT claims set was empty.");
			
			boolean superAdmin = JWTClient.isSuperAdmin();
			
			Assert.assertEquals(superAdmin, expectedIsSuperAdmin, 
					"Unexpected user role. Found " + superAdmin + ", expected " + expectedIsSuperAdmin);
		}
		catch (Exception e) {
			if (!shouldThrowException) {
				Assert.fail("Unexpected error occurred parsing jwt token", e);
			}
		}
	}

}
