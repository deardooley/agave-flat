package org.iplantc.service.monitor.resources.impl;

import java.io.IOException;

import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.binary.Base64;
import org.iplantc.service.common.persistence.JndiSetup;
import org.iplantc.service.monitor.AbstractMonitorTest;
import org.iplantc.service.monitor.MonitorApplication;
import org.iplantc.service.monitor.TestDataHelper;
import org.iplantc.service.monitor.dao.MonitorDao;
import org.iplantc.service.monitor.exceptions.MonitorException;
import org.iplantc.service.monitor.model.Monitor;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.restlet.Client;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.Server;
import org.restlet.data.Form;
import org.restlet.data.Header;
import org.restlet.data.MediaType;
import org.restlet.data.Protocol;
import org.restlet.ext.jaxrs.JaxRsApplication;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.util.Series;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MonitorResourceImplTest extends AbstractMonitorTest 
{	
	private Component comp = new Component();
	private MonitorDao dao = new MonitorDao();
	private Client client = new Client(new Context(), Protocol.HTTP);
	private String testJWT;
	
	private void initRestletServer() throws Exception
	{
		JndiSetup.init();
		
		// create Component (as ever for Restlet)
        Server server = comp.getServers().add(Protocol.HTTP, 8182);

        // create JAX-RS runtime environment
        JaxRsApplication application = new JaxRsApplication(comp.getContext());

        application.add(new MonitorApplication());

        // Attach the application to the component and start it
        comp.getDefaultHost().attach(application);
        comp.start();
	}
	
	private void initJWT() throws Exception
	{
		JsonNode json = new ObjectMapper().createObjectNode()
				.put("iss", "wso2.org/products/am")
				.put("exp", new DateTime().plusHours(4).toDate().getTime())
				.put("http://wso2.org/claims/subscriber", System.getProperty("user.name"))
				.put("http://wso2.org/claims/applicationid", "5")
				.put("http://wso2.org/claims/applicationname", "DefaultApplication")
				.put("http://wso2.org/claims/applicationtier", "Unlimited")
				.put("http://wso2.org/claims/apicontext", "/apps")
				.put("http://wso2.org/claims/version", "2.0")
				.put("http://wso2.org/claims/tier", "Unlimited")
				.put("http://wso2.org/claims/keytype", "PRODUCTION")
				.put("http://wso2.org/claims/usertype", "APPLICATION_USER")
				.put("http://wso2.org/claims/enduser", System.getProperty("user.name"))
				.put("http://wso2.org/claims/enduserTenantId", "-9999")
				.put("http://wso2.org/claims/emailaddress", System.getProperty("user.name"))
				.put("http://wso2.org/claims/fullname", "Dev User")
				.put("http://wso2.org/claims/givenname", "Dev")
				.put("http://wso2.org/claims/lastname", "User")
				.put("http://wso2.org/claims/primaryChallengeQuestion", "N/A")
				.put("http://wso2.org/claims/role", "Internal/everyone")
				.put("http://wso2.org/claims/title", "N/A");
		StringBuilder builder = new StringBuilder();
		builder.append("eyJ0eXAiOiJKV1QiLCJhbGciOiJTSEEyNTZ3aXRoUlNBIiwieDV0IjoiTm1KbU9HVXhNelpsWWpNMlpEUmhOVFpsWVRBMVl6ZGhaVFJpT1dFME5XSTJNMkptT1RjMVpBPT0ifQ==.");
		builder.append(new String(Base64.encodeBase64(json.toString().getBytes())));
		builder.append(".FA6GZjrB6mOdpEkdIQL/p2Hcqdo2QRkg/ugBbal8wQt6DCBb1gC6wPDoAenLIOc+yDorHPAgRJeLyt2DutNrKRFv6czq1wz7008DrdLOtbT4EKI96+mXJNQuxrpuU9lDZmD4af/HJYZ7HXg3Hc05+qDJ+JdYHfxENMi54fXWrxs=");
				
		testJWT = builder.toString();
	}
	
	@BeforeClass
	public void beforeClass() throws Exception
	{
		initRestletServer();
		
		super.beforeClass();
		
		initJWT();
	}
	
	@AfterMethod
	public void afterMethod() throws Exception
	{
		clearMonitors();
		clearNotifications();
		clearQueues();
	}
	
	@AfterClass
	public void afterClass() throws MonitorException
	{
		super.afterClass();
		try { comp.stop(); } catch (Exception e) {}
	}

	/**
	 * Parses standard response stanza for the actual body and code
	 * 
	 * @param representation
	 * @param shouldSucceed
	 * @return
	 * @throws JSONException
	 * @throws IOException
	 */
	private JsonNode verifyResponse(Representation representation, boolean shouldSucceed) 
	throws JSONException, IOException 
	{
		String responseBody = representation.getText();
		
		Assert.assertNotNull(responseBody, "Null body returned");
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode json = mapper.getFactory().createJsonParser(responseBody).readValueAsTree();
		if (shouldSucceed) {
			Assert.assertEquals(json.get("result").asText().toLowerCase(), "success", "Error when success should have occurred");
		} else {
			Assert.assertEquals(json.get("result").asText().toLowerCase(), "error", "Success when error should have occurred");
		}
		
		return json.get("response");
	}
	
	/**
	 * Creates a client to call the api given by the url. This will reuse the connection
	 * multiple times rather than starting up a new client every time.
	 * 
	 * @param url
	 * @return
	 */
	private ClientResource getService(String url)
	{
		ClientResource service = new ClientResource(url);
    	Series<Header> headers = (Series<Header>) service.getRequest().getAttributes().get("org.restlet.http.headers");;
    	if (headers == null) { 
    		headers = new Series<Header>(Header.class); 
    	} 
    	headers.add("x-jwt-assertion", testJWT);
    	service.setNext(client);
    	service.getRequest().getAttributes().put("org.restlet.http.headers", headers);
    	service.setReferrerRef("http://test.example.com");
    	return service;
	}

	@DataProvider(name="addMonitorProvider")
	public Object[][] addMonitorProvider() throws Exception
	{
		ObjectNode jsonExecutionMonitorNoSystem = (ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR);
		ObjectNode jsonExecutionMonitorNoFrequency = (ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR);
		ObjectNode jsonExecutionMonitorNoUpdateSystemStatus = (ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR);
		ObjectNode jsonExecutionMonitorNoInternalUsername = (ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR);
		jsonExecutionMonitorNoSystem.remove("target");
		jsonExecutionMonitorNoFrequency.remove("system");
		jsonExecutionMonitorNoUpdateSystemStatus.remove("updateSystemStatus");
		jsonExecutionMonitorNoInternalUsername.remove("internalUsername");
		
		return new Object[][] {
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR), "Valid monitor json should parse", false },
			{ jsonExecutionMonitorNoSystem, "Missing system should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", ""), "Empty target should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", mapper.createObjectNode()), "Object for target should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", mapper.createArrayNode()), "Array for target should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", 5), "Integer for target should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", 5.5), "Decimal for target should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", publicStorageSystem.getSystemId()), "Public storage system should not throw an exception", false },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", publicExecutionSystem.getSystemId()), "Public execution system should not throw an exception", false },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", privateExecutionSystem.getSystemId()), "Private execution system should not throw an exception", false },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", privateStorageSystem.getSystemId()), "Private storage system should not throw an exception", false },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("target", sharedExecutionSystem.getSystemId()), "Shared execution system should not throw an exception", false },
			

			{ jsonExecutionMonitorNoFrequency, "Missing frequency should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("frequency", ""), "Empty frequency should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("frequency", mapper.createObjectNode()), "Object for frequency should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("frequency", mapper.createArrayNode()), "Array for frequency should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("frequency", 5), "Integer for frequency should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("frequency", 5.5), "Decimal for frequency should throw exception", true },
			
			{ jsonExecutionMonitorNoUpdateSystemStatus, "Missing updateSystemStatus should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("updateSystemStatus", ""), "Empty updateSystemStatus should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("updateSystemStatus", mapper.createObjectNode()), "Object for updateSystemStatus should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("updateSystemStatus", mapper.createArrayNode()), "Array for updateSystemStatus should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("updateSystemStatus", 5), "Integer for updateSystemStatus should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("updateSystemStatus", 5.5), "Decimal for updateSystemStatus should throw exception", true },
			
			{ jsonExecutionMonitorNoInternalUsername, "Missing internalUsername should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("internalUsername", ""), "Empty internalUsername should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("internalUsername", mapper.createObjectNode()), "Object for internalUsername should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("internalUsername", mapper.createArrayNode()), "Array for internalUsername should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("internalUsername", 5), "Integer for internalUsername should throw exception", true },
			{ ((ObjectNode)dataHelper.getTestDataObject(TestDataHelper.TEST_EXECUTION_MONITOR)).put("internalUsername", 5.5), "Decimal for internalUsername should throw exception", true },
			
		};
	}
	
	@Test(dataProvider="addMonitorProvider")
	public void addSingleMonitor(JsonNode body, String errorMessage, boolean shouldThrowException)
	{
		ClientResource resource = getService("http://localhost:8182/monitors");  
		
		JsonNode json = null;
		try 
		{
			Representation response = resource.post(new StringRepresentation(body.toString()));
			Assert.assertEquals(resource.getStatus().equals(Status.OK), !shouldThrowException, "Response failed when it should not have");
			Assert.assertNotNull(response, "Expected json monitor object, instead received null");
			Assert.assertEquals(response.getMediaType(), MediaType.APPLICATION_JSON, 
					"Expected json media type returned, instead received " + response.getMediaType());
			
			json = verifyResponse(response, true);
			
			Assert.assertNotNull(json.get("id").asText().toLowerCase(), "No monitor uuid given");
		}
		catch (Exception e) {
			Assert.fail("Unexpected exception thrown", e);
		}
	}
	
	@Test(dataProvider="addMonitorProvider", dependsOnMethods={"addSingleMonitor"})
	public void addMonitorFromForm(JsonNode body, String errorMessage, boolean shouldThrowException)
	{
		ClientResource resource = getService("http://localhost:8182/monitors");  
		
		JsonNode json = null;
		try 
		{
			Form form = new Form();
			if (body != null) 
			{
				if (body.has("target")) 
				{
					form.add("target", body.get("target").asText());
				}
				
				if (body.has("frequency")) 
				{
					form.add("frequency", body.get("frequency").asText());
				}
				
				if (body.has("updateSystemStatus")) 
				{
					form.add("updateSystemStatus", body.get("updateSystemStatus").asText());
				}
				
				if (body.has("internalUsername")) 
				{
					form.add("internalUsername", body.get("internalUsername").asText());
				}
			}
			
			Representation response = resource.post(form.getWebRepresentation());
			Assert.assertEquals(resource.getStatus().equals(Status.OK), !shouldThrowException, "Response failed when it should not have");
			Assert.assertNotNull(response, "Expected json monitor object, instead received null");
			Assert.assertEquals(response.getMediaType(), MediaType.APPLICATION_JSON, 
					"Expected json media type returned, instead received " + response.getMediaType());
			
			json = verifyResponse(response, true);
			
			Assert.assertNotNull(json.get("id").asText().toLowerCase(), "No monitor uuid given");
		}
		catch (Exception e) {
			Assert.fail("Unexpected exception thrown", e);
		}
	}

	@DataProvider(name="deleteMonitorProvider")
	public Object[][] deleteMonitorProvider() throws Exception
	{
		Monitor validMonitor = createStorageMonitor();
		dao.persist(validMonitor);
		
//		Monitor invalidSystemMonitor = createStorageMonitor();
//		invalidSystemMonitor.setOwner(TestDataHelper.SYSTEM_SHARE_USER);
//		invalidSystemMonitor.setSystem(privateStorageSystem);
//		dao.persist(invalidSystemMonitor);
		
		Monitor otherUserMonitor = createExecutionMonitor();
		otherUserMonitor.setSystem(sharedExecutionSystem);
		otherUserMonitor.setOwner(TestDataHelper.SYSTEM_SHARE_USER);
		dao.persist(otherUserMonitor);
		
		return new Object[][] {
			{ validMonitor.getUuid(), "Deleting valid storage monitor should succeed", false },
			{ "", "Empty uuid should fail", true },
			{ "abcd", "Invalid uuid should fail", true },
			{ otherUserMonitor.getUuid(), "Deleting unowned monitor should fail", true },
		};
	}
	
	@Test(dataProvider="deleteMonitorProvider", dependsOnMethods={"addMonitorFromForm"})
	public void deleteMonitor(String uuid, String errorMessage, boolean shouldThrowException)
	{
		ClientResource resource = getService("http://localhost:8182/monitors/" + uuid);  
		
		JsonNode json = null;
		try 
		{
			Representation response = resource.delete();
			Assert.assertEquals(resource.getStatus().equals(Status.OK), !shouldThrowException, "Response failed when it should not have");
			Assert.assertNotNull(response, "Expected json monitor object, instead received null");
			Assert.assertEquals(response.getMediaType(), MediaType.APPLICATION_JSON, 
					"Expected json media type returned, instead received " + response.getMediaType());
			
			json = verifyResponse(response, shouldThrowException);
			
			Assert.assertTrue(json.isNull(), "Message results attribute was not null");
		}
		catch (Exception e) {
			Assert.fail("Unexpected exception thrown", e);
		}
	}

//	@Test(dataProvider="deleteMonitorProvider", dependsOnMethods={"deleteMonitor"})
//	public void fireMonitor(String uuid, String errorMessage, boolean shouldThrowException)
//	{
//		ClientResource resource = getService("http://localhost:8182/monitors/" + uuid);  
//		
//		JsonNode json = null;
//		try 
//		{
//			Representation response = resource.delete();
//			
//			Assert.assertNotNull(response, "Expected json monitor object, instead received null");
//			Assert.assertEquals(response.getMediaType(), MediaType.APPLICATION_JSON, 
//					"Expected json media type returned, instead received " + response.getMediaType());
//			
//			json = verifyResponse(response, shouldThrowException);
//			Assert.assertEquals(resource.getStatus().equals(Status.OK), !shouldThrowException, "Response failed when it should not have"); 
//			
//		}
//		catch (Exception e) {
//			Assert.fail("Unexpected exception thrown", e);
//		}
//	}
	
	@DataProvider(name="getMonitorProvider")
	public Object[][] getMonitorProvider() throws Exception
	{
		Monitor validMonitor = createStorageMonitor();
		dao.persist(validMonitor);
		
		Monitor inactiveMonitor = createStorageMonitor();
		inactiveMonitor.setActive(false);
		dao.persist(inactiveMonitor);
		
		Monitor otherUserMonitor = createExecutionMonitor();
		otherUserMonitor.setSystem(sharedExecutionSystem);
		otherUserMonitor.setOwner(TestDataHelper.SYSTEM_SHARE_USER);
		dao.persist(otherUserMonitor);
		
		return new Object[][] {
			{ validMonitor.getUuid(), "Requesting valid storage monitor should succeed", false },
			{ inactiveMonitor.getUuid(), "Requesting inactive monitor should fail", true },
			{ "", "Empty uuid should fail", true },
			{ "abcd", "Invalid uuid should fail", true },
			{ otherUserMonitor.getUuid(), "Requesting unowned monitor should fail", true },
		};
	}

	@Test(dataProvider="getMonitorProvider", dependsOnMethods={"deleteMonitor"})
	public void getMonitor(String uuid, String errorMessage, boolean shouldThrowException)
	{
		ClientResource resource = getService("http://localhost:8182/monitors/" + uuid);  
		
		JsonNode json = null;
		try
		{
			Representation response = resource.get();
			
			Assert.assertNotNull(response, "Expected json monitor object, instead received null");
			Assert.assertEquals(response.getMediaType(), MediaType.APPLICATION_JSON, 
					"Expected json media type returned, instead received " + response.getMediaType());
			
			json = verifyResponse(response, shouldThrowException);
			Assert.assertEquals(resource.getStatus().equals(Status.OK), !shouldThrowException, "Response failed when it should not have"); 
			
		}
		catch (Exception e) {
			Assert.fail("Unexpected exception thrown", e);
		}
	}

	@Test(dependsOnMethods={"getMonitor"})
	public void getMonitors(String errorMessage, boolean shouldThrowException)
	{
		ClientResource resource = getService("http://localhost:8182/monitors/");  
		
		JsonNode json = null;
		try
		{
			Monitor m1 = createStorageMonitor();
			dao.persist(m1);
			
			Monitor m2 = createExecutionMonitor();
			dao.persist(m2);
			
			Representation response = resource.get();
			
			Assert.assertNotNull(response, "Expected json monitor object, instead received null");
			Assert.assertEquals(response.getMediaType(), MediaType.APPLICATION_JSON, 
					"Expected json media type returned, instead received " + response.getMediaType());
			
			json = verifyResponse(response, shouldThrowException);
			Assert.assertEquals(resource.getStatus().equals(Status.OK), !shouldThrowException, "Response failed when it should not have");
			Assert.assertTrue(json instanceof ArrayNode, "Service returned object rather than array");
			Assert.assertEquals(json.size(), 2, "Invalid number of monitors returned.");
			
		}
		catch (Exception e) {
			Assert.fail("Unexpected exception thrown", e);
		}
	}
}
