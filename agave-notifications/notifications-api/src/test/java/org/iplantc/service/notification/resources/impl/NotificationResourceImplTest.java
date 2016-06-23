package org.iplantc.service.notification.resources.impl;

import java.io.IOException;

import javax.ws.rs.WebApplicationException;
//import javax.ws.rs.core.MediaType;
//import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.Status;

import org.iplantc.service.notification.AbstractNotificationTest;
import org.iplantc.service.notification.NotificationApplication;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.dao.NotificationDao;
import org.iplantc.service.notification.model.Notification;
import org.json.JSONException;
import org.restlet.Component;
import org.restlet.Server;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Protocol;
import org.restlet.ext.jaxrs.JaxRsApplication;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NotificationResourceImplTest extends AbstractNotificationTest {
	
	private NotificationResourceImpl service;
	private Component comp = new Component();
	private NotificationDao dao = new NotificationDao();
	private Notification referenceNotification;
	
	private void initRestletServer() throws Exception
	{
		// create Component (as ever for Restlet)
        Server server = comp.getServers().add(Protocol.HTTP, 8182);

        // create JAX-RS runtime environment
        JaxRsApplication application = new JaxRsApplication(comp.getContext());

        application.add(new NotificationApplication());

        // Attach the application to the component and start it
        comp.getDefaultHost().attach(application);
        comp.start();
	}
	
	@BeforeClass
	public void beforeClass() throws Exception
	{
		initRestletServer();
		
		// create a notification so we have something to reference in the web service 
		// interactions.
		referenceNotification = new Notification("SENT", "dooley@tacc.utexas.edu");
		referenceNotification.setAssociatedUuid(referenceNotification.getUuid());
		referenceNotification.setOwner(TEST_USER);
		dao.persist(referenceNotification);
	}

	@AfterClass
	public void afterClass() throws Exception
	{
		for(Notification n: dao.getAll()) {
			dao.delete(n);
		}
		comp.stop();
	}
	
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

	@DataProvider(name="addNotificationProvider")
	public Object[][] addNotificationProvider() throws Exception
	{
		ObjectMapper mapper = new ObjectMapper();
		
		return new Object[][] {
			{ mapper.createObjectNode().put("url", TEST_URL).put("event","START").put("associatedUuid",referenceNotification.getUuid()), "Valid url should succeed", true },
			
			{ mapper.createObjectNode(), "Empty object should throw exception", true },
			{ mapper.createArrayNode(), "Array should throw exception", true },
			{ mapper.createObjectNode().put("event","START").put("associatedUuid",referenceNotification.getUuid()), "Missing url should throw exception", true },
			{ mapper.createObjectNode().put("url", "").put("event","START").put("associatedUuid",referenceNotification.getUuid()), "Empty url should throw exception", true },
			{ mapper.createObjectNode().putNull("url").put("event","START").put("associatedUuid",referenceNotification.getUuid()), "Null url should throw exception", true },
			{ mapper.createObjectNode().put("event","START").put("associatedUuid",referenceNotification.getUuid()).set("url", mapper.createArrayNode()), "Array for url should throw exception", true },
			{ mapper.createObjectNode().put("event","START").put("associatedUuid",referenceNotification.getUuid()).set("url", mapper.createObjectNode()), "Object for url should throw exception", true },
			{ mapper.createObjectNode().put("url", TEST_EMAIL).put("event","START").put("associatedUuid",referenceNotification.getUuid()), "Valid email should succeed", false },
			
			{ mapper.createObjectNode().put("url", TEST_EMAIL).put("associatedUuid",referenceNotification.getUuid()), "Missing event should throw exception", true },
			{ mapper.createObjectNode().put("event", "").put("url", TEST_EMAIL).put("associatedUuid",referenceNotification.getUuid()), "Empty event should throw exception", true },
			{ mapper.createObjectNode().putNull("event").put("url", TEST_EMAIL).put("associatedUuid",referenceNotification.getUuid()), "Null event should throw exception", true },
			{ mapper.createObjectNode().put("url", TEST_EMAIL).put("associatedUuid",referenceNotification.getUuid()).set("event", mapper.createArrayNode()), "Array for event should throw exception", true },
			{ mapper.createObjectNode().put("url", TEST_EMAIL).put("associatedUuid",referenceNotification.getUuid()).set("event", mapper.createObjectNode()), "Object for event should throw exception", true },
			
			{ mapper.createObjectNode().put("event","START").put("url", TEST_EMAIL), "Missing associatedUuid should throw exception", true },
			{ mapper.createObjectNode().put("associatedUuid", "").put("event","START").put("url", TEST_EMAIL), "Empty associatedUuid should throw exception", true },
			{ mapper.createObjectNode().putNull("associatedUuid").put("event","START").put("url", TEST_EMAIL).put("associatedUuid",referenceNotification.getUuid()), "Null associatedUuid should throw exception", true },
			{ mapper.createObjectNode().put("event","START").put("url", TEST_EMAIL).set("associatedUuid", mapper.createArrayNode()), "Array for associatedUuid should throw exception", true },
			{ mapper.createObjectNode().put("event","START").put("url", TEST_EMAIL).set("associatedUuid", mapper.createObjectNode()), "Object for associatedUuid should throw exception", true },
			{ mapper.createObjectNode().put("associatedUuid", WILDCARD_ASSOCIATED_UUID).put("event","START").put("url", TEST_EMAIL), "Wildcard associatedUuid should throw exception for non-admin", true },
			
			
		};
	}
	
	@Test(dataProvider="addNotificationProvider")
	public void addNotification(JsonNode body, String errorMessage, boolean shouldThrowException)
	{
		ClientResource resource = new ClientResource("http://localhost:8182/notifications");  
		resource.setReferrerRef("http://test.example.com");
		JsonNode json = null;
		try 
		{
			Representation response = resource.post(new StringRepresentation(body.toString()));
			Assert.assertEquals(resource.getStatus().equals(Status.OK), !shouldThrowException, "Response failed when it should not have");
			Assert.assertNotNull(response, "Expected json notification object, instead received null");
			Assert.assertEquals(response.getMediaType(), MediaType.APPLICATION_JSON, 
					"Expected json media type returned, instead received " + response.getMediaType());
			
			json = verifyResponse(response, true);
			
			Assert.assertNotNull(json.get("id").asText().toLowerCase(), "No notification uuid given");
		}
		catch (Throwable e) {
			Assert.fail("Unexpected exception thrown", e);
		}
		finally {
			try {
				if (json != null) {
					if (json.has("id")) {
						Notification n = dao.findByUuid(json.get("id").asText()); 
						dao.delete(n);
					}
					
				}
			} catch (Exception e) {}
		}
	}
	
	@Test(dataProvider="addNotificationProvider", dependsOnMethods={"addNotification"})
	public void addNotificationFromForm(JsonNode body, String errorMessage, boolean shouldThrowException)
	{
		ClientResource resource = new ClientResource("http://localhost:8182/notifications");  
		resource.setReferrerRef("http://test.example.com");
		JsonNode json = null;
		try 
		{
			Form form = new Form();
			if (body != null) 
			{
				if (body.has("event")) 
				{
					form.add("event", body.get("event").asText());
				}
				
				if (body.has("url")) 
				{
					form.add("url", body.get("url").asText());
				}
				
				if (body.has("associatedUuid")) 
				{
					form.add("associatedUuid", body.get("associatedUuid").asText());
				}
			}
			
			Representation response = resource.post(form.getWebRepresentation());
			Assert.assertEquals(resource.getStatus().equals(Status.OK), !shouldThrowException, "Response failed when it should not have");
			Assert.assertNotNull(response, "Expected json notification object, instead received null");
			Assert.assertEquals(response.getMediaType(), MediaType.APPLICATION_JSON, 
					"Expected json media type returned, instead received " + response.getMediaType());
			
			json = verifyResponse(response, true);
			
			Assert.assertNotNull(json.get("id").asText().toLowerCase(), "No notification uuid given");
		}
		catch (Throwable e) {
			Assert.fail("Unexpected exception thrown", e);
		}
		finally {
			try {
				if (json != null) {
					if (json.has("id")) {
						Notification n = dao.findByUuid(json.get("id").asText()); 
						dao.delete(n);
					}
					
				}
			} catch (Exception e) {}
		}
	}

	@DataProvider(name="deleteNotificationProvider")
	public Object[][] deleteNotificationProvider() throws Exception
	{
		Notification n = new Notification("SENT", TEST_EMAIL);
		n.setOwner(TEST_USER);
		n.setAssociatedUuid(referenceNotification.getUuid());
		dao.persist(n);
		
		Notification n2 = new Notification("SENT", TEST_EMAIL);
		n2.setOwner(TEST_USER + "-test");
		n2.setAssociatedUuid(referenceNotification.getUuid());
		dao.persist(n2);
		
		return new Object[][] {
			{ n.getUuid(), "Valid url should succeed", false },
			{ "", "Empty uuid should fail", true },
			{ "abcd", "Invalid uuid should fail", true },
			{ n2.getUuid(), "Deleting by non-owner should fail", true },
		};
	}
	
	@Test(dataProvider="deleteNotificationProvider", dependsOnMethods={"addNotificationFromForm"})
	public void deleteNotification(String uuid, String errorMessage, boolean shouldThrowException)
	{
		ClientResource resource = new ClientResource("http://localhost:8182/notifications/" + uuid);  
		resource.setReferrerRef("http://test.example.com");
		resource.setChallengeResponse(ChallengeScheme.HTTP_BASIC, TEST_USER, Settings.IRODS_PASSWORD);
		JsonNode json = null;
		try 
		{
			Representation response = resource.delete();
			Assert.assertEquals(resource.getStatus().equals(Status.OK), !shouldThrowException, "Response failed when it should not have");
			Assert.assertNotNull(response, "Expected json notification object, instead received null");
			Assert.assertEquals(response.getMediaType(), MediaType.APPLICATION_JSON, 
					"Expected json media type returned, instead received " + response.getMediaType());
			
			json = verifyResponse(response, shouldThrowException);
			
			Assert.assertTrue(json.isNull(), "Message results attribute was not null");
		}
		catch (Throwable e) {
			Assert.fail("Unexpected exception thrown", e);
		}
		finally {
			try {
				Notification n = dao.findByUuid(uuid); 
				if (n != null) {
					dao.delete(n);
				}
			} catch (Exception e) {}
		}
	}

	@Test(dataProvider="deleteNotificationProvider", dependsOnMethods={"deleteNotification"})
	public void fireNotification(String uuid, String errorMessage, boolean shouldThrowException)
	{
		ClientResource resource = new ClientResource("http://localhost:8182/notifications/" + uuid);  
		resource.setReferrerRef("http://test.example.com");
		resource.setChallengeResponse(ChallengeScheme.HTTP_BASIC, TEST_USER, Settings.IRODS_PASSWORD);
		JsonNode json = null;
		try 
		{
			Representation response = resource.delete();
			
			Assert.assertNotNull(response, "Expected json notification object, instead received null");
			Assert.assertEquals(response.getMediaType(), MediaType.APPLICATION_JSON, 
					"Expected json media type returned, instead received " + response.getMediaType());
			
			json = verifyResponse(response, shouldThrowException);
			Assert.assertEquals(resource.getStatus().equals(Status.OK), !shouldThrowException, "Response failed when it should not have"); 
			
		}
		catch (Throwable e) {
			Assert.fail("Unexpected exception thrown", e);
		}
		finally {
			try {
				Notification n = dao.findByUuid(uuid); 
				if (n != null) {
					dao.delete(n);
				}
			} catch (Exception e) {}
		}
	}

//	@Test
//	public void getNotification(String uuid, String errorMessage, boolean shouldThrowException)
//	{
//		try 
//		{
//			throw new RuntimeException("Test not implemented");
//		}
//		catch (WebApplicationException e) {
//			if (!shouldThrowException) 
//				Assert.fail(errorMessage, e);
//		}
//		catch (Throwable e) {
//			Assert.fail("Unexpected exception thrown", e);
//		}
//	}

//	@Test
//	public void getNotifications(String errorMessage, boolean shouldThrowException)
//	{
//		try 
//		{
//			throw new RuntimeException("Test not implemented");
//		}
//		catch (Throwable e) {
//			Assert.fail("Unexpected exception thrown", e);
//		}
//	}
}
