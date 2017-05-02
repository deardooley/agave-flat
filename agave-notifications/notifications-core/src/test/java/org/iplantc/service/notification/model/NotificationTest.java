package org.iplantc.service.notification.model;

import java.io.IOException;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.notification.AbstractNotificationTest;
import org.iplantc.service.notification.TestDataHelper;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.enumerations.NotificationStatusType;
import org.iplantc.service.notification.model.enumerations.RetryStrategyType;
import org.json.JSONException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Test(groups={"integration"})
public class NotificationTest extends AbstractNotificationTest {

	@BeforeClass
	public void beforeClass()
	{
		dataHelper = TestDataHelper.getInstance();
		
		HibernateUtil.getConfiguration();
	}
	
	@Test
	public void initNotification()
	{
		Notification notif = new Notification();
		Assert.assertNotNull(notif.getUuid(), "UUID not set on instantiation.");
		Assert.assertNotNull(notif.getCreated(), "Creation date not set on instantiation.");
		Assert.assertNotNull(notif.getLastUpdated(), "Last updated date not set on instantiation.");
		Assert.assertNotNull(notif.getPolicy(), "Policy should never be null.");
		Assert.assertNotNull(notif.getStatus(), "Status should always be set to available by default.");
		Assert.assertEquals(notif.getStatus(), NotificationStatusType.ACTIVE, "Status should always be set to available by default.");
		Assert.assertTrue(notif.isVisible(), "Visible should be true by default.");
	}

	@DataProvider(name="initNotificationStringStringProvider")
	private Object[][] initNotificationStringStringProvider()
	{
		return new Object[][] {
				{ "job@example.com", "Valid email address should be accepted", false },
				{ "@example.com", "Valid email address should throw exception", true },
				{ "job@", "Valid email address should throw exception", true },
				{ "@", "Valid email address should throw exception", true },
				
				{ "http://example.com", "Valid url should be accepted", false },
				{ "http://example.com/${FOO_BAR}/", "Valid url should be accepted", false },
				{ "http://example.com/${FOO_BAR}/#/?event=${event}", "Valid url should be accepted", false },
				{ "http://example.com/${FOO_BAR}/#/?event=${event}", "Valid url should be accepted", false },
				{ "https://example.com", "Valid url should be accepted", false },
				{ "example.com", "Hostname only should throw exception", true },
				{ "ftp://example.com", "FTP url protocol should throw exception", true },
				{ "sftp://example.com", "SFTP url protocol should throw exception", true },
				{ "agave://example.com", "AGAVE url protocol should throw exception", true },
				{ "gridftp://example.com", "GRIDFTP url protocol should throw exception", true },
				{ "file://example.com", "FILE url protocol should throw exception", true },
				{ "/example", "relative path should throw exception", true },
				{ "file:///", "FILE url protocol should throw exception", true },
				{ "///", "FILE url protocol should throw exception", true },
		};
	}
	
	@Test(dependsOnMethods={"initNotification"}, dataProvider="initNotificationStringStringProvider")
	public void initNotificationStringString(String url, String message, boolean shouldThrowException) 
	{
		try 
		{
			Notification notif = new Notification("FINISHED", "joe@example.com");
			Assert.assertNotNull(notif.getUuid(), "UUID not set on instantiation.");
			Assert.assertNotNull(notif.getCreated(), "Creation date not set on instantiation.");
			Assert.assertNotNull(notif.getLastUpdated(), "Last updated date not set on instantiation.");
			Assert.assertNotNull(notif.getPolicy(), "Policy should never be null.");
			Assert.assertNotNull(notif.getStatus(), "Status should always be set to available by default.");
			Assert.assertEquals(notif.getStatus(), NotificationStatusType.ACTIVE, "Status should always be set to available by default.");
			Assert.assertTrue(notif.isVisible(), "Visible should be true by default.");
		} 
		catch (Exception e) 
		{
			if (!shouldThrowException) {
				Assert.fail(message, e);
			}
		}
	}
	
	@DataProvider(name="fromJSONProvider")
	private Object[][] fromJSONProvider() throws JSONException, IOException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode jsonEmailNoUrlNotification = dataHelper.getTestDataObject(TestDataHelper.TEST_EMAIL_NOTIFICATION);
		ObjectNode jsonEmailNoEventNotification = dataHelper.getTestDataObject(TestDataHelper.TEST_EMAIL_NOTIFICATION);
		ObjectNode jsonEmailNoAssociatedIdNotification = dataHelper.getTestDataObject(TestDataHelper.TEST_EMAIL_NOTIFICATION);
		jsonEmailNoUrlNotification.remove("url");
		jsonEmailNoEventNotification.remove("event");
		jsonEmailNoAssociatedIdNotification.remove("associatedUuid");
		
		ObjectNode jsonWebhookNoUrlNotification = dataHelper.getTestDataObject(TestDataHelper.TEST_WEBHOOK_NOTIFICATION);
		ObjectNode jsonWebhookNoEventNotification = dataHelper.getTestDataObject(TestDataHelper.TEST_WEBHOOK_NOTIFICATION);
		ObjectNode jsonWebhookNoAssociatedIdNotification = dataHelper.getTestDataObject(TestDataHelper.TEST_WEBHOOK_NOTIFICATION);
		jsonWebhookNoUrlNotification.remove("url");
		jsonWebhookNoEventNotification.remove("url");
		jsonWebhookNoAssociatedIdNotification.remove("url");
		
		return new Object[][] {
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_EMAIL_NOTIFICATION), "Valid email json should parse", false },
			{ jsonEmailNoUrlNotification, "Missing url should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_EMAIL_NOTIFICATION).put("url", ""), "Empty url should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_EMAIL_NOTIFICATION).putObject("url"), "Object for url should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_EMAIL_NOTIFICATION).put("url", mapper.createArrayNode().addObject()), "Array for url should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_EMAIL_NOTIFICATION).put("url", 5), "Integer for url should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_EMAIL_NOTIFICATION).put("url", 5.5), "Decimal for url should throw exception", true },

			{ jsonEmailNoEventNotification, "Missing event should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_EMAIL_NOTIFICATION).put("event", ""), "Empty event should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_EMAIL_NOTIFICATION).putObject("event"), "Object for event should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_EMAIL_NOTIFICATION).put("event", mapper.createArrayNode().addObject()), "Array for event should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_EMAIL_NOTIFICATION).put("event", 5), "Integer for event should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_EMAIL_NOTIFICATION).put("event", 5.5), "Decimal for event should throw exception", true },
			
			{ jsonEmailNoAssociatedIdNotification, "Missing associatedUuid should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_EMAIL_NOTIFICATION).put("associatedUuid", ""), "Empty associatedUuid should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_EMAIL_NOTIFICATION).putObject("associatedUuid"), "Object for associatedUuid should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_EMAIL_NOTIFICATION).put("associatedUuid", mapper.createArrayNode().addObject()), "Array for associatedUuid should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_EMAIL_NOTIFICATION).put("associatedUuid", 5), "Integer for associatedUuid should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_EMAIL_NOTIFICATION).put("associatedUuid", 5.5), "Decimal for associatedUuid should throw exception", true },
			
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_WEBHOOK_NOTIFICATION), "Valid email json should parse", false },
			{ jsonWebhookNoUrlNotification, "Missing url should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_WEBHOOK_NOTIFICATION).put("url", ""), "Empty url should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_WEBHOOK_NOTIFICATION).putObject("url"), "Object for url should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_WEBHOOK_NOTIFICATION).put("url", mapper.createArrayNode().addObject()), "Array for url should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_WEBHOOK_NOTIFICATION).put("url", 5), "Integer for url should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_WEBHOOK_NOTIFICATION).put("url", 5.5), "Decimal for url should throw exception", true },

			{ jsonWebhookNoEventNotification, "Missing event should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_WEBHOOK_NOTIFICATION).put("event", ""), "Empty event should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_WEBHOOK_NOTIFICATION).putObject("event"), "Object for event should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_WEBHOOK_NOTIFICATION).put("event", mapper.createArrayNode().addObject()), "Array for event should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_WEBHOOK_NOTIFICATION).put("event", 5), "Integer for event should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_WEBHOOK_NOTIFICATION).put("event", 5.5), "Decimal for event should throw exception", true },
			
			{ jsonWebhookNoAssociatedIdNotification, "Missing associatedUuid should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_WEBHOOK_NOTIFICATION).put("associatedUuid", ""), "Empty associatedUuid should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_WEBHOOK_NOTIFICATION).putObject("associatedUuid"), "Object for associatedUuid should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_WEBHOOK_NOTIFICATION).put("associatedUuid", mapper.createArrayNode().addObject()), "Array for associatedUuid should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_WEBHOOK_NOTIFICATION).put("associatedUuid", 5), "Integer for associatedUuid should throw exception", true },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_WEBHOOK_NOTIFICATION).put("associatedUuid", 5.5), "Decimal for associatedUuid should throw exception", true },
			
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_REALTIME_NOTIFICATION), "Realtime notification should pass", false },
			{ dataHelper.getTestDataObject(TestDataHelper.TEST_POLICY_NOTIFICATION), "Realtime policy notification should pass", false }
		};
	}

	@Test(dependsOnMethods={"initNotificationStringString"}, dataProvider="fromJSONProvider")
	public void fromJSON(JsonNode json, String message, boolean shouldThrowException)
	{
		try 
		{
			Notification.fromJSON(json);
		} 
		catch (NotificationException e)
		{
			if (!shouldThrowException) {
				Assert.fail(message, e);
			}
		}
		catch (Exception e) 
		{
			Assert.fail(message, e);
		}
	}
	
	@Test
	public void setRetryDelay()
	{
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        
		NotificationPolicy policy = new NotificationPolicy();
		
		Validator validator = factory.getValidator();
        policy.setRetryDelay(-1);
		Assert.assertFalse(validator.validate(policy).isEmpty(), "Positive retryDelay value < 1.");
    	
		policy.setRetryStrategyType(RetryStrategyType.NONE);
		policy.setRetryDelay(1);
		Assert.assertFalse(validator.validate(policy).isEmpty(), "zero retryDelay value throws exception when strategy is none or immediate.");
    	
		policy.setRetryStrategyType(RetryStrategyType.IMMEDIATE);
		policy.setRetryDelay(1);
		Assert.assertFalse(validator.validate(policy).isEmpty(), "zero retryDelay value throws exception when strategy is none or immediate.");
    	
		policy.setRetryStrategyType(RetryStrategyType.EXPONENTIAL);
		policy.setRetryDelay(1);
		Assert.assertTrue(validator.validate(policy).isEmpty(), "positive retryDelay value does not throw exception when strategy is exponential.");
    	
		policy.setRetryStrategyType(RetryStrategyType.DELAYED);
		policy.setRetryDelay(0);
		Assert.assertFalse(validator.validate(policy).isEmpty(), "zero retryDelay value throws exception when strategy is delayed.");
    }
}
