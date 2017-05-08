package org.iplantc.service.common.messaging;

import org.iplantc.service.common.Settings;
import org.iplantc.service.common.exceptions.MessagingException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

@Test(groups={"notReady", "integration"})
public class RabbitMQMessagingClientTest {

	public static String TEST_EXCHANGE_TOPIC = "test.exchange.topic";
	public static String TEST_EXCHANGE_TOPIC_QUEUE = "test_queue";
	
	private Connection connection = null;
	private Channel channel = null;
	private QueueingConsumer consumer = null;
	
	//@AfterMethod
	public void afterMethod()
	{
		// verify it's there
		ConnectionFactory factory = new ConnectionFactory();
	    factory.setHost(Settings.MESSAGING_SERVICE_HOST);
	    
	    try
		{	
		    connection = factory.newConnection();
		    channel = connection.createChannel();
		    
		    channel.exchangeDelete(TEST_EXCHANGE_TOPIC);
		}
	    catch (Exception e)
		{
			Assert.fail("Failed to delete the exchange after the test.",e);
		}
		finally {
			try {channel.close();} catch(Exception e) {}
		    try {connection.close();} catch(Exception e) {}
		}
	}

	@BeforeClass
	public void beforeClass()
	{}

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
		RabbitMQClient amqp = new RabbitMQClient();
		
		try
		{
			// push a message onto the exchange
			amqp.push(TEST_EXCHANGE_TOPIC, TEST_EXCHANGE_TOPIC_QUEUE, message);
			
			Message poppedMessage = amqp.pop(TEST_EXCHANGE_TOPIC, TEST_EXCHANGE_TOPIC_QUEUE);
			
			Assert.assertNotNull(message, "No message popped from the queue.");
			Assert.assertNotNull(poppedMessage.getId(), "No message id returned.");
			Assert.assertEquals(poppedMessage.getMessage(), message);
		}
		catch (MessagingException e)
		{
			Assert.assertTrue(shouldThrowException, errorMessage);
		}
		finally {
			//try {channel.exchangeDelete(TEST_EXCHANGE_TOPIC);} catch(Exception e) {}
			try {channel.close();} catch(Exception e) {}
		    try {connection.close();} catch(Exception e) {}
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
		RabbitMQClient amqp = new RabbitMQClient();
		
		try
		{
			// push a message onto the exchange
			amqp.push(TEST_EXCHANGE_TOPIC, TEST_EXCHANGE_TOPIC_QUEUE, message);
			
			// verify it's there
			ConnectionFactory factory = new ConnectionFactory();
		    factory.setHost(Settings.MESSAGING_SERVICE_HOST);
		    //factory.setVirtualHost(tenantId); // use virutal hosting to isolate to a tenant
		    connection = factory.newConnection();
		    channel = connection.createChannel();
		    
		    //channel.exchangeDeclare(TEST_EXCHANGE_TOPIC, "topic", true, false, false, null);
		    //channel.exchangeDeclarePassive(TEST_EXCHANGE_TOPIC);
		    channel.queueBind(TEST_EXCHANGE_TOPIC_QUEUE, TEST_EXCHANGE_TOPIC, "*");
		    
		    channel.basicQos(1);
		    
		    consumer = new QueueingConsumer(channel);
		    
		    channel.basicConsume(TEST_EXCHANGE_TOPIC_QUEUE, true, consumer);
		    
		    QueueingConsumer.Delivery delivery = consumer.nextDelivery();
			
			String poppedMessage = new String(delivery.getBody());
			
			Assert.assertEquals(poppedMessage, message, errorMessage);
		}
		catch (Exception e)
		{
			if (!shouldThrowException) {
				Assert.fail(errorMessage, e);
			}
		}
		finally {
			//try {channel.exchangeDelete(TEST_EXCHANGE_TOPIC);} catch(Exception e) {}
			try {channel.close();} catch(Exception e) {}
		    try {connection.close();} catch(Exception e) {}
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
