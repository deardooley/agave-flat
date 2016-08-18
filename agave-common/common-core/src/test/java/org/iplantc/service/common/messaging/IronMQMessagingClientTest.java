package org.iplantc.service.common.messaging;

import io.iron.ironmq.Client;
import io.iron.ironmq.Cloud;
import io.iron.ironmq.Queue;

import org.iplantc.service.common.messaging.clients.IronMQClient;
import org.iplantc.service.common.messaging.model.Message;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class IronMQMessagingClientTest {

	public static String TEST_EXCHANGE_TOPIC = "test.exchange.topic";
	public static String TEST_EXCHANGE_TOPIC_QUEUE = "test_queue";
	
	private Client client;
	
	//@AfterMethod
	public void afterMethod()
	{}

	@BeforeClass
	public void beforeClass()
	{
		client = new Client("51eca387e0a7bd0011000007", "MRHqhZxKJKhMaHfLYV0PXK-8LQ4", Cloud.ironAWSUSEast);
	}

	@AfterClass
	public void afterClass()
	{}

	//@Test
	public void listen()
	{
		throw new RuntimeException("Test not implemented");
	}
	
	@DataProvider(name="popProvider")
	public Object[][] popProvider()
	{
		return new Object[][] {
			{ "abcd1234-abcd1234-abcd1234-abcd1234", false, "Failed to push simple string onto queue." },
		};
	}
	
	@Test(dataProvider="popProvider", dependsOnMethods={"push"})
	public void pop(String message, boolean shouldThrowException, String errorMessage)
	{
		IronMQClient ironmq = new IronMQClient();
		Queue ironQueue = null;
		try
		{
			// push a message onto the exchange
			ironQueue = client.queue(TEST_EXCHANGE_TOPIC_QUEUE);
			ironQueue.push(message);
			
			Message poppedMessage = ironmq.pop(TEST_EXCHANGE_TOPIC, TEST_EXCHANGE_TOPIC_QUEUE);
			
			Assert.assertNotNull(message, "No message popped from the queue.");
			Assert.assertNotNull(poppedMessage.getId(), "No message id returned.");
			Assert.assertEquals(poppedMessage.getMessage(), message);

			ironQueue.deleteMessage((String)poppedMessage.getId());
		}
		catch (Exception e)
		{
			if (!shouldThrowException) {
				Assert.fail(errorMessage, e);
			}
		}
	}

	@DataProvider(name="pushProvider")
	public Object[][] pushProvider()
	{
		return new Object[][] {
			{ "abcd1234-abcd1234-abcd1234-abcd1234", false, "Failed to push simple string onto queue." },
		};
	}
	
	@Test(dataProvider="pushProvider")
	public void push(String message, boolean shouldThrowException, String errorMessage)
	{
		IronMQClient ironmq = new IronMQClient();
		Queue ironQueue = null;
		io.iron.ironmq.Message msg = null;
		try
		{
			// push a message onto the exchange
			ironmq.push(TEST_EXCHANGE_TOPIC, TEST_EXCHANGE_TOPIC_QUEUE, message);
			
			ironQueue = client.queue(TEST_EXCHANGE_TOPIC_QUEUE);
			
			msg = ironQueue.get();
			//System.out.println(msg.getId());
			String poppedMessage = new String(msg.getBody());
			
			Assert.assertEquals(poppedMessage, message, errorMessage);
			
			ironQueue.deleteMessage(msg.getId());
		}
		catch (Exception e)
		{
			if (!shouldThrowException) {
				Assert.fail(errorMessage, e);
			}
		} 
	}

	//@Test
	public void returnMessage()
	{
		throw new RuntimeException("Test not implemented");
	}

	//@Test
	public void stop()
	{
		throw new RuntimeException("Test not implemented");
	}
}
