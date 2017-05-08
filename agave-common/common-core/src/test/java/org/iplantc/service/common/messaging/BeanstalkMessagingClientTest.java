package org.iplantc.service.common.messaging;

import java.util.ArrayList;
import java.util.List;

import org.iplantc.service.common.Settings;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.surftools.BeanstalkClient.Job;
import com.surftools.BeanstalkClientImpl.ClientImpl;

@Test(groups={"integration"})
public class BeanstalkMessagingClientTest {

	public static String TEST_EXCHANGE_TOPIC = "test.exchange.topic";
	public static String TEST_EXCHANGE_TOPIC_QUEUE = "test_queue";
	
	private ClientImpl nativeClient;
	
	@BeforeMethod
	public void beforeMethod()
	{
		nativeClient = new ClientImpl(Settings.MESSAGING_SERVICE_HOST, 11300);
		nativeClient.useTube(TEST_EXCHANGE_TOPIC_QUEUE);
	}
	@AfterMethod
	public void afterMethod()
	{
		try { nativeClient.close(); } catch (Throwable e) { }
	}

	@BeforeClass
	public void beforeClass()
	{
		
	}

	@AfterClass
	public void afterClass()
	{}

	//@Test
	public void listen()
	{
		throw new RuntimeException("Test not implemented");
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
		BeanstalkClient agaveClient = new BeanstalkClient();
		
		try
		{
			// push a message onto the exchange
			agaveClient.push(TEST_EXCHANGE_TOPIC, TEST_EXCHANGE_TOPIC_QUEUE, message);
			//agaveClient.stop();
			
			nativeClient.watch(TEST_EXCHANGE_TOPIC_QUEUE);
			Job job = nativeClient.reserve(null);
			
			//System.out.println(msg.getId());
			String poppedMessage = new String(job.getData());
			
			Assert.assertEquals(poppedMessage, message, errorMessage);
			
			nativeClient.delete(job.getJobId());
		}
		catch (Exception e)
		{
			if (!shouldThrowException) {
				Assert.fail(errorMessage, e);
			}
		} 
		finally {
			agaveClient.stop();
		}
	}
	
	@DataProvider(name="popProvider")
	public Object[][] popProvider()
	{
		return new Object[][] {
			{ "abcd1234-abcd1234-abcd1234-abcd1234", false, "Failed to push simple string onto queue." },
		};
	}
	
	@Test(dataProvider="popProvider", dependsOnMethods={"reject"})
	public void pop(String message, boolean shouldThrowException, String errorMessage)
	{
		BeanstalkClient agaveClient = new BeanstalkClient();
		
		try
		{
			// push a message onto the exchange
			nativeClient.useTube(TEST_EXCHANGE_TOPIC_QUEUE);
			Long jobid = nativeClient.put(65536, 0, 120, message.getBytes());
			
			Message poppedMessage = agaveClient.pop(TEST_EXCHANGE_TOPIC, TEST_EXCHANGE_TOPIC_QUEUE);
			
			Assert.assertNotNull(poppedMessage, "No message popped from the queue.");
			Assert.assertNotNull(poppedMessage.getId(), "No message id returned.");
			Assert.assertEquals(jobid, (Long)poppedMessage.getId(), "Retrieved wrong message from queue");
			Assert.assertEquals(poppedMessage.getMessage(), message);

			agaveClient.delete(TEST_EXCHANGE_TOPIC, TEST_EXCHANGE_TOPIC_QUEUE, (Long)poppedMessage.getId());
		}
		catch (Exception e)
		{
			if (!shouldThrowException) {
				Assert.fail(errorMessage, e);
			}
		}
		finally {
			agaveClient.stop();
		}
	}
	
	@Test(dependsOnMethods={"pop"})
	public void popMultiple()
	{
		BeanstalkClient agaveClient = new BeanstalkClient();
		
		try
		{
			String messageText = "abcd1234-abcd1234-abcd1234-abcd1234";
			// push a message onto the exchange
			nativeClient.useTube(TEST_EXCHANGE_TOPIC_QUEUE);
			List<Long> notifs = new ArrayList<Long>();
			for(int i=0;i<5;i++)
			{
				notifs.add(nativeClient.put(65536, 0, 120, messageText.getBytes()));
			}
			
			for(int i=0;i<5;i++)
			{
				Message poppedMessage = agaveClient.pop(TEST_EXCHANGE_TOPIC, TEST_EXCHANGE_TOPIC_QUEUE);
				
				Assert.assertNotNull(poppedMessage, "No message popped from the queue.");
				Assert.assertNotNull(poppedMessage.getId(), "No message id returned.");
				Assert.assertEquals(notifs.get(i), (Long)poppedMessage.getId(), "Retrieved wrong message from queue");
				Assert.assertEquals(poppedMessage.getMessage(), messageText);

				agaveClient.delete(TEST_EXCHANGE_TOPIC, TEST_EXCHANGE_TOPIC_QUEUE, (Long)poppedMessage.getId());
			}
		}
		catch (Throwable e)
		{
			Assert.fail("Failed to pop multiple messages", e);
		}
		finally {
			agaveClient.stop();
		}
	}


	@Test(dependsOnMethods={"push"})
	public void reject()
	{
		BeanstalkClient agaveClient = new BeanstalkClient();
		String message = "abcd1234-abcd1234-abcd1234-abcd1234";
		try
		{
			// push a message onto the exchange
			agaveClient.push(TEST_EXCHANGE_TOPIC, TEST_EXCHANGE_TOPIC_QUEUE, message);
			
			Message msg = agaveClient.pop(TEST_EXCHANGE_TOPIC, TEST_EXCHANGE_TOPIC_QUEUE);
			
			Assert.assertEquals(msg.getMessage(), message, "Pushed and popped messages do not match");
			
			agaveClient.reject(TEST_EXCHANGE_TOPIC, TEST_EXCHANGE_TOPIC_QUEUE, (Long)msg.getId(), message);
			
			nativeClient.watch(TEST_EXCHANGE_TOPIC_QUEUE);
			nativeClient.delete((Long)msg.getId());
			
			Assert.assertNull(nativeClient.peek((Long)msg.getId()), "Job was not deleted");
		}
		catch (Exception e)
		{
			Assert.fail("Failed to release message back to queue", e);
		} 
		finally {
			agaveClient.stop();
		}
	}

	//@Test
	public void stop()
	{
		throw new RuntimeException("Test not implemented");
	}
}
