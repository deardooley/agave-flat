package org.iplantc.service.uuid.resources.impl;

import org.iplantc.service.uuid.AbstractUuidTest;

public class UuidResourceImplTest extends AbstractUuidTest 
{	
//	private Component comp = new Component();
//	private TagDao dao = new TagDao();
//	private Client client = new Client(new Context(), Protocol.HTTP);
//	private String testJWT;
//	
//	private void initRestletServer() throws Exception
//	{
//		JndiSetup.init();
//		
//		// create Component (as ever for Restlet)
//        Server server = comp.getServers().add(Protocol.HTTP, 8182);
//
//        // create JAX-RS runtime environment
//        JaxRsApplication application = new JaxRsApplication(comp.getContext());
//
//        application.add(new UuidApplication());
//
//        // Attach the application to the component and start it
//        comp.getDefaultHost().attach(application);
//        comp.start();
//	}
//	
//	private void initJWT() throws Exception
//	{
//		JsonNode json = new ObjectMapper().createObjectNode()
//				.put("iss", "wso2.org/products/am")
//				.put("exp", new DateTime().plusHours(4).toDate().getTime())
//				.put("http://wso2.org/claims/subscriber", System.getProperty("user.name"))
//				.put("http://wso2.org/claims/applicationid", "5")
//				.put("http://wso2.org/claims/applicationname", "DefaultApplication")
//				.put("http://wso2.org/claims/applicationtier", "Unlimited")
//				.put("http://wso2.org/claims/apicontext", "/apps")
//				.put("http://wso2.org/claims/version", "2.0")
//				.put("http://wso2.org/claims/tier", "Unlimited")
//				.put("http://wso2.org/claims/keytype", "PRODUCTION")
//				.put("http://wso2.org/claims/usertype", "APPLICATION_USER")
//				.put("http://wso2.org/claims/enduser", System.getProperty("user.name"))
//				.put("http://wso2.org/claims/enduserTenantId", "-9999")
//				.put("http://wso2.org/claims/emailaddress", System.getProperty("user.name"))
//				.put("http://wso2.org/claims/fullname", "Dev User")
//				.put("http://wso2.org/claims/givenname", "Dev")
//				.put("http://wso2.org/claims/lastname", "User")
//				.put("http://wso2.org/claims/primaryChallengeQuestion", "N/A")
//				.put("http://wso2.org/claims/role", "Internal/everyone")
//				.put("http://wso2.org/claims/title", "N/A");
//		StringBuilder builder = new StringBuilder();
//		builder.append("eyJ0eXAiOiJKV1QiLCJhbGciOiJTSEEyNTZ3aXRoUlNBIiwieDV0IjoiTm1KbU9HVXhNelpsWWpNMlpEUmhOVFpsWVRBMVl6ZGhaVFJpT1dFME5XSTJNMkptT1RjMVpBPT0ifQ==.");
//		builder.append(new String(Base64.encodeBase64(json.toString().getBytes())));
//		builder.append(".FA6GZjrB6mOdpEkdIQL/p2Hcqdo2QRkg/ugBbal8wQt6DCBb1gC6wPDoAenLIOc+yDorHPAgRJeLyt2DutNrKRFv6czq1wz7008DrdLOtbT4EKI96+mXJNQuxrpuU9lDZmD4af/HJYZ7HXg3Hc05+qDJ+JdYHfxENMi54fXWrxs=");
//				
//		testJWT = builder.toString();
//	}
//	
//	@BeforeClass
//	public void beforeClass() throws Exception
//	{
//		super.beforeClass();
//		
//		initRestletServer();
//		initJWT();
//	}
//	
//	@AfterMethod
//	public void afterMethod() throws Exception
//	{
//		
//	}
//	
//	@AfterClass
//	public void afterClass() throws UUIDException
//	{
//		
//		try { comp.stop(); } catch (Exception e) {}
//	}
//
//	/**
//	 * Parses standard response stanza for the actual body and code
//	 * 
//	 * @param representation
//	 * @param shouldSucceed
//	 * @return
//	 * @throws JSONException
//	 * @throws IOException
//	 */
//	private JsonNode verifyResponse(Representation representation, boolean shouldSucceed) 
//	throws JSONException, IOException 
//	{
//		String responseBody = representation.getText();
//		
//		Assert.assertNotNull(responseBody, "Null body returned");
//		
//		ObjectMapper mapper = new ObjectMapper();
//		JsonNode json = mapper.getFactory().createJsonParser(responseBody).readValueAsTree();
//		if (shouldSucceed) {
//			Assert.assertEquals(json.get("result").asText().toLowerCase(), "success", "Error when success should have occurred");
//		} else {
//			Assert.assertEquals(json.get("result").asText().toLowerCase(), "error", "Success when error should have occurred");
//		}
//		
//		return json.get("response");
//	}
//	
//	/**
//	 * Creates a client to call the api given by the url. This will reuse the connection
//	 * multiple times rather than starting up a new client every time.
//	 * 
//	 * @param url
//	 * @return
//	 */
//	private ClientResource getService(String url)
//	{
//		ClientResource service = new ClientResource(url);
//    	Series<Header> headers = (Series<Header>) service.getRequest().getAttributes().get("org.restlet.http.headers");;
//    	if (headers == null) { 
//    		headers = new Series<Header>(Header.class); 
//    	} 
//    	headers.add("x-jwt-assertion", testJWT);
//    	service.setNext(client);
//    	service.getRequest().getAttributes().put("org.restlet.http.headers", headers);
//    	service.setReferrerRef("http://test.example.com");
//    	return service;
//	}
//
//	@DataProvider(name="addTagProvider")
//	public Object[][] addMonitorProvider() throws Exception
//	{
////		ObjectNode jsonExecutionMonitorNoSystem = createTag();
////		ObjectNode jsonExecutionMonitorNoFrequency = (ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR);
////		ObjectNode jsonExecutionMonitorNoUpdateSystemStatus = (ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR);
////		ObjectNode jsonExecutionMonitorNoInternalUsername = (ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR);
////		jsonExecutionMonitorNoSystem.remove("target");
////		jsonExecutionMonitorNoFrequency.remove("system");
////		jsonExecutionMonitorNoUpdateSystemStatus.remove("updateSystemStatus");
////		jsonExecutionMonitorNoInternalUsername.remove("internalUsername");
////		
//		return new Object[][] {
//			{ dataHelper.getTestDataObject(TestDataHelper.TEST_TAG), "Valid tag json should parse", false },
////			{ jsonExecutionMonitorNoSystem, "Missing system should throw exception", true },
////			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", ""), "Empty target should throw exception", true },
////			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", mapper.createObjectNode()), "Object for target should throw exception", true },
////			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", mapper.createArrayNode()), "Array for target should throw exception", true },
////			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", 5), "Integer for target should throw exception", true },
////			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", 5.5), "Decimal for target should throw exception", true },
////			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", publicStorageSystem.getSystemId()), "Public storage system should not throw an exception", false },
////			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", publicExecutionSystem.getSystemId()), "Public execution system should not throw an exception", false },
////			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", privateExecutionSystem.getSystemId()), "Private execution system should not throw an exception", false },
////			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", privateStorageSystem.getSystemId()), "Private storage system should not throw an exception", false },
////			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", sharedExecutionSystem.getSystemId()), "Shared execution system should not throw an exception", false },
////			
////
////			{ jsonExecutionMonitorNoFrequency, "Missing frequency should throw exception", true },
////			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("frequency", ""), "Empty frequency should throw exception", true },
////			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("frequency", mapper.createObjectNode()), "Object for frequency should throw exception", true },
////			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("frequency", mapper.createArrayNode()), "Array for frequency should throw exception", true },
////			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("frequency", 5), "Integer for frequency should throw exception", true },
////			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("frequency", 5.5), "Decimal for frequency should throw exception", true },
////			
////			{ jsonExecutionMonitorNoUpdateSystemStatus, "Missing updateSystemStatus should throw exception", true },
////			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("updateSystemStatus", ""), "Empty updateSystemStatus should throw exception", true },
////			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("updateSystemStatus", mapper.createObjectNode()), "Object for updateSystemStatus should throw exception", true },
////			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("updateSystemStatus", mapper.createArrayNode()), "Array for updateSystemStatus should throw exception", true },
////			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("updateSystemStatus", 5), "Integer for updateSystemStatus should throw exception", true },
////			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("updateSystemStatus", 5.5), "Decimal for updateSystemStatus should throw exception", true },
////			
////			{ jsonExecutionMonitorNoInternalUsername, "Missing internalUsername should throw exception", true },
////			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("internalUsername", ""), "Empty internalUsername should throw exception", true },
////			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("internalUsername", mapper.createObjectNode()), "Object for internalUsername should throw exception", true },
////			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("internalUsername", mapper.createArrayNode()), "Array for internalUsername should throw exception", true },
////			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("internalUsername", 5), "Integer for internalUsername should throw exception", true },
////			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("internalUsername", 5.5), "Decimal for internalUsername should throw exception", true },
//			
//		};
//	}
//	
//	@Test(dataProvider="addTagProvider")
//	public void addSingleTag(JsonNode body, String errorMessage, boolean shouldThrowException)
//	{
//		ClientResource resource = getService("http://localhost:8182/");  
//		
//		JsonNode json = null;
//		try 
//		{
//			Representation response = resource.post(new StringRepresentation(body.toString()));
//			Assert.assertEquals(resource.getStatus().equals(Status.OK), !shouldThrowException, "Response failed when it should not have");
//			Assert.assertNotNull(response, "Expected json monitor object, instead received null");
//			Assert.assertEquals(response.getMediaType(), MediaType.APPLICATION_JSON, 
//					"Expected json media type returned, instead received " + response.getMediaType());
//			
//			json = verifyResponse(response, true);
//			
//			Assert.assertNotNull(json.get("id").asText().toLowerCase(), "No tag uuid given");
//		}
//		catch (Exception e) {
//			Assert.fail("Unexpected exception thrown", e);
//		}
//	}
//	
//	@Test(dataProvider="addTagProvider", dependsOnMethods={"addSingleMonitor"})
//	public void addMonitorFromForm(JsonNode body, String errorMessage, boolean shouldThrowException)
//	{
//		ClientResource resource = getService("http://localhost:8182/monitors");  
//		
//		JsonNode json = null;
//		try 
//		{
//			Form form = new Form();
//			if (body != null) 
//			{
//				if (body.has("target")) 
//				{
//					form.add("target", body.get("target").asText());
//				}
//				
//				if (body.has("frequency")) 
//				{
//					form.add("frequency", body.get("frequency").asText());
//				}
//				
//				if (body.has("updateSystemStatus")) 
//				{
//					form.add("updateSystemStatus", body.get("updateSystemStatus").asText());
//				}
//				
//				if (body.has("internalUsername")) 
//				{
//					form.add("internalUsername", body.get("internalUsername").asText());
//				}
//			}
//			
//			Representation response = resource.post(form.getWebRepresentation());
//			Assert.assertEquals(resource.getStatus().equals(Status.OK), !shouldThrowException, "Response failed when it should not have");
//			Assert.assertNotNull(response, "Expected json monitor object, instead received null");
//			Assert.assertEquals(response.getMediaType(), MediaType.APPLICATION_JSON, 
//					"Expected json media type returned, instead received " + response.getMediaType());
//			
//			json = verifyResponse(response, true);
//			
//			Assert.assertNotNull(json.get("id").asText().toLowerCase(), "No monitor uuid given");
//		}
//		catch (Exception e) {
//			Assert.fail("Unexpected exception thrown", e);
//		}
//	}
//
//	@DataProvider(name="getTagProvider")
//	public Object[][] getTagProvider() throws Exception
//	{
//		Tag validMonitor = createTag();
//		validMonitor.setOwner(TestDataHelper.TEST_USER);
//		dao.persist(validMonitor);
//		
//		Tag inactiveMonitor = createTag();
//		
//		Tag otherUserMonitor = createTag();
//		otherUserMonitor.setOwner(TestDataHelper.TEST_SHAREUSER);
//		dao.persist(otherUserMonitor);
//		
//		return new Object[][] {
//			{ validMonitor.getUuid(), "Requesting valid tag should succeed", false },
//			{ inactiveMonitor.getUuid(), "Requesting invalid tag should fail", true },
//			{ "", "Empty uuid should fail", true },
//			{ "abcd", "Invalid uuid should fail", true },
//			{ otherUserMonitor.getUuid(), "Requesting unowned tag should fail", true },
//		};
//	}
//
//	@Test(dependsOnMethods={"getTag"})
//	public void getResourceForUuid(String errorMessage, boolean shouldThrowException)
//	{
//		ClientResource resource = getService("http://localhost:8182/");  
//		
//		JsonNode json = null;
//		try
//		{
//			Tag m1 = createTag();
//			dao.persist(m1);
//			
//			Tag m2 = createTag();
//			dao.persist(m2);
//			
//			Representation response = resource.get();
//			
//			Assert.assertNotNull(response, "Expected json tag object, instead received null");
//			Assert.assertEquals(response.getMediaType(), MediaType.APPLICATION_JSON, 
//					"Expected json media type returned, instead received " + response.getMediaType());
//			
//			json = verifyResponse(response, shouldThrowException);
//			Assert.assertEquals(resource.getStatus().equals(Status.OK), !shouldThrowException, "Response failed when it should not have");
//			Assert.assertTrue(json instanceof ArrayNode, "Service returned object rather than array");
//			Assert.assertEquals(json.size(), 2, "Invalid number of monitors returned.");
//			
//		}
//		catch (Exception e) {
//			Assert.fail("Unexpected exception thrown", e);
//		}
//	}
}
