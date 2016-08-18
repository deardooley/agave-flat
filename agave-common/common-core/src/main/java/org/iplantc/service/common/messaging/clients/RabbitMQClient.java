/**
 * 
 */
package org.iplantc.service.common.messaging.clients;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.exceptions.MessageProcessingException;
import org.iplantc.service.common.exceptions.MessagingException;
import org.iplantc.service.common.messaging.model.Message;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.QueueingConsumer;
import com.surftools.BeanstalkClient.Job;

/**
 * @author dooley
 *
 */
public class RabbitMQClient implements MessageQueueClient 
{
	private static final Logger log = Logger.getLogger(RabbitMQClient.class);
	
	private Connection connection = null;
	private Channel channel = null;
	private QueueingConsumer consumer = null;
	
	private boolean stop = false; 
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.messaging.MessagingClient#publish(java.lang.String, java.lang.String)
	 */
	@Override
	public void push(String exchange, String queue, String message) throws MessagingException
	{
		try 
		{
			ConnectionFactory factory = new ConnectionFactory();
		    factory.setHost(Settings.MESSAGING_SERVICE_HOST);
		    //factory.setVirtualHost(tenantId); // use virutal hosting to isolate to a tenant
		    connection = factory.newConnection();
		    channel = connection.createChannel();
		    
		    channel.exchangeDeclare(exchange, "topic", true, false, false, null);
		    //channel.exchangeDeclarePassive(exchange);
//		    channel.queueDeclare(queue, true, false, false, null);
		    
		    channel.basicPublish(exchange, queue, 
		                MessageProperties.PERSISTENT_TEXT_PLAIN,
		                message.getBytes());
		    
		    log.debug("[" + queue + "] Published message '" + message + "'");
		}
		catch (IOException e) {
			throw new MessagingException("Failed to publish message",e);
		}
		finally {
			try {channel.close();} catch(Exception e) {}
		    try {connection.close();} catch(Exception e) {}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.messaging.MessageQueueClient#push(java.lang.String, java.lang.String, java.lang.String, int)
	 */
	@Override
	public void push(String exchange, String queue, String message, int secondsToDelay)
	throws MessagingException
	{
		try 
		{
			ConnectionFactory factory = new ConnectionFactory();
		    factory.setHost(Settings.MESSAGING_SERVICE_HOST);
		    //factory.setVirtualHost(tenantId); // use virutal hosting to isolate to a tenant
		    connection = factory.newConnection();
		    channel = connection.createChannel();
		    
		    Map<String, Object> args = new HashMap<String, Object>();
		    args.put("x-delayed-type", "direct");
		    
		    channel.exchangeDeclare(exchange, "x-delayed-message", true, false, args);
		    //channel.exchangeDeclarePassive(exchange);
//		    channel.queueDeclare(queue, true, false, false, null);
		    
		    AMQP.BasicProperties.Builder props = new AMQP.BasicProperties.Builder();
		    Map<String, Object> headers = new HashMap<String, Object>();
		    headers.put("x-delay", secondsToDelay * 1000);
		    props.headers(headers);
		    
		    
		    channel.basicPublish(exchange, "", props.build(), message.getBytes());
		    
		    log.debug("[" + queue + "] Published message '" + message + "' with a delay of " + secondsToDelay + " seconds");
		}
		catch (IOException e) {
			throw new MessagingException("Failed to publish message",e);
		}
		finally {
			try {channel.close();} catch(Exception e) {}
		    try {connection.close();} catch(Exception e) {}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.messaging.MessagingClient#reserve(java.lang.String, java.lang.String, int)
	 */
	@Override
	public Message reserve(String exchange, String queue, int timeout) throws MessagingException
	{
		QueueingConsumer.Delivery delivery = null;
		ConnectionFactory factory = new ConnectionFactory();
	    factory.setHost(Settings.MESSAGING_SERVICE_HOST);
	    try {
		    connection = factory.newConnection();
		    channel = connection.createChannel();
		    
		    Map<String, Object> args = new HashMap<String, Object>();
		    args.put("x-message-ttl", timeout*1000);
		          
		    channel.exchangeDeclare(exchange, "topic", true, false, false, args);
		    
		    channel.queueBind(queue, exchange, "*");
		    
		    channel.basicQos(1);
		    
		    consumer = new QueueingConsumer(channel);
		    channel.basicConsume(queue, true, consumer);
		    
		    delivery = consumer.nextDelivery();
			
			String message = new String(delivery.getBody());
			
			return new Message(delivery.getEnvelope().getDeliveryTag(), message);
	    }
		catch (IOException e)
		{
			throw new MessagingException("Failed to establish connection to messaging service",e);
		}
		catch (Exception e)
		{
			throw new MessagingException("Failed to retrieve next message.",e);
		}
	    finally {
	    	try { channel.close(); } catch (Exception e1) {}
			try { connection.close(); } catch (Exception e1) {}
	    }
	}

	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.messaging.MessagingClient#nextMessage(java.lang.String)
	 */
	@Override
	public Message pop(String exchange, String queue) throws MessagingException
	{
		QueueingConsumer.Delivery delivery = null;
		ConnectionFactory factory = new ConnectionFactory();
	    factory.setHost(Settings.MESSAGING_SERVICE_HOST);
	    try {
		    connection = factory.newConnection();
		    channel = connection.createChannel();
		    
		    channel.exchangeDeclare(exchange, "topic", true, false, false, null);
		    
		    //channel.queueDeclare(queue, true, false, false, null);
//		    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
		    
		    channel.queueBind(queue, exchange, "*");
		    
		    channel.basicQos(1);
		    
		    consumer = new QueueingConsumer(channel);
		    channel.basicConsume(queue, true, consumer);
		    
		    delivery = consumer.nextDelivery();
			
			String message = new String(delivery.getBody());
			
//			channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false); 
			
			return new Message(delivery.getEnvelope().getDeliveryTag(), message);
	    }
		catch (IOException e)
		{
			throw new MessagingException("Failed to establish connection to messaging service",e);
		}
		catch (Exception e)
		{
			throw new MessagingException("Failed to retrieve next message.",e);
		}
	    finally {
	    	try { channel.close(); } catch (Exception e1) {}
			try { connection.close(); } catch (Exception e1) {}
	    }
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.messaging.MessagingClient#nextMessage(java.lang.String)
	 */
	@Override
	public List<Message> pop(String exchange, String queue, int count) throws MessagingException
	{
		List<Message> messages = new ArrayList<Message>();
		QueueingConsumer.Delivery delivery = null;
		ConnectionFactory factory = new ConnectionFactory();
	    factory.setHost(Settings.MESSAGING_SERVICE_HOST);
	    try {
		    connection = factory.newConnection();
		    channel = connection.createChannel();
		    
		    channel.exchangeDeclare(exchange, "topic", true, false, false, null);
		    
		    channel.queueBind(queue, exchange, "*");
		    
		    channel.basicQos(1);
		    
		    consumer = new QueueingConsumer(channel);
		    channel.basicConsume(queue, true, consumer);
		    
		    for (int i=0;i<count;i++)
		    {
			    delivery = consumer.nextDelivery();
				
				String message = new String(delivery.getBody());
			
				messages.add(new Message(delivery.getEnvelope().getDeliveryTag(), message));
	    	}
	    	return messages;
	    }
		catch (IOException e)
		{
			throw new MessagingException("Failed to establish connection to messaging service",e);
		}
		catch (Exception e)
		{
			throw new MessagingException("Failed to retrieve next message.",e);
		}
	    finally {
	    	try { channel.close(); } catch (Exception e1) {}
			try { connection.close(); } catch (Exception e1) {}
	    }
	}

	@Override
	public void reject(String exchange, String queue, Object messageId, String message)
			throws MessagingException
	{
		ConnectionFactory factory = new ConnectionFactory();
	    factory.setHost(Settings.MESSAGING_SERVICE_HOST);
	    try {
		    connection = factory.newConnection();
		    channel = connection.createChannel();
		    
//		    channel.queueDeclare(Settings.MESSAGING_NOTIFICATION_QUEUE_NAME, true, false, false, null);
		    channel.exchangeDeclare(exchange, "topic", true, false, false, null);
		    channel.queueBind(queue, exchange, "*");
		    
		    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
		    
		    channel.basicQos(1);
		    
		    consumer = new QueueingConsumer(channel);
		    
		    channel.basicConsume(queue, true, consumer);
		    
		    channel.basicReject((Long)messageId, true);
	    }
		catch (IOException e)
		{
			throw new MessagingException("Failed to establish connection to messaging service",e);
		}
		catch (Exception e)
		{
			throw new MessagingException("Failed to acknowledge message " + messageId, e);
		}
	    finally {
	    	try { channel.close(); } catch (Exception e1) {}
			try { connection.close(); } catch (Exception e1) {}
	    }    
	}

	@Override
	public void delete(String exchange, String queue, Object messageId) throws MessagingException
	{
		ConnectionFactory factory = new ConnectionFactory();
	    factory.setHost(Settings.MESSAGING_SERVICE_HOST);
	    try {
	    	connection = factory.newConnection();
			channel = connection.createChannel();
			
			//			    channel.queueDeclare(Settings.MESSAGING_NOTIFICATION_QUEUE_NAME, true, false, false, null);
			channel.exchangeDeclare(exchange, "topic", true, false, false, null);
			channel.queueBind(queue, exchange, "*");
			
			System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
			
			channel.basicQos(1);
			
			consumer = new QueueingConsumer(channel);
			
			channel.basicConsume(queue, true, consumer);
			
			channel.basicAck((Long)messageId, false);
		} 
		catch (IOException e)
		{
			throw new MessagingException("Failed to establish connection to messaging service",e);
		}
		catch (Exception e)
		{
			throw new MessagingException("Failed to delete message " + messageId, e);
		}
	    finally {
	    	try { channel.close(); } catch (Exception e1) {}
			try { connection.close(); } catch (Exception e1) {}
	    } 
		
	}
	
//	@Override
//	public void acknowledge(String exchange, Object messageId) throws MessagingException
//	{
//		ConnectionFactory factory = new ConnectionFactory();
//	    factory.setHost(Settings.MESSAGING_SERVICE_HOST);
//	    try {
//		    connection = factory.newConnection();
//		    channel = connection.createChannel();
//		    
//		    channel.queueDeclare(Settings.MESSAGING_NOTIFICATION_QUEUE_NAME, true, false, false, null);
//		    channel.exchangeDeclare(exchange, "topic");
//		    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
//		    
//		    channel.basicQos(1);
//		    
//		    consumer = new QueueingConsumer(channel);
//		    channel.basicConsume(Settings.MESSAGING_NOTIFICATION_QUEUE_NAME, false, consumer);
//		    
//		    channel.basicAck((Long)messageId, false);
//	    }
//		catch (IOException e)
//		{
//			throw new MessagingException("Failed to establish connection to messaging service",e);
//		}
//		catch (Exception e)
//		{
//			throw new MessagingException("Failed to acknowledge message " + messageId, e);
//		}
//	    finally {
//	    	try { channel.close(); } catch (Exception e1) {}
//			try { connection.close(); } catch (Exception e1) {}
//	    }    
//	}

	@Override
	public void listen(String exchange, String queue, MessageQueueListener listener)
	throws MessagingException
	{
		QueueingConsumer.Delivery delivery = null;
		ConnectionFactory factory = new ConnectionFactory();
	    factory.setHost(Settings.MESSAGING_SERVICE_HOST);
	    try {
		    connection = factory.newConnection();
		    channel = connection.createChannel();
		    
		    channel.exchangeDeclare(exchange, "topic", true, false, false, null);
		    
		    //channel.queueDeclare(queue, true, false, false, null);
//		    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
		    
		    channel.queueBind(queue, exchange, "*");
		    
		    channel.basicQos(1);
		    
		    consumer = new QueueingConsumer(channel);
		    channel.basicConsume(queue, false, consumer);
		    
			stop = false;
			
			while (!stop)
			{
				delivery = consumer.nextDelivery();
				
				if (delivery == null) break;
				
				String body = new String(delivery.getBody());
	
				System.out.println(" [x] Received '" + body + "'");
				
				try 
				{
					listener.processMessage(body);
					channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
				} catch (MessageProcessingException e) {
					channel.basicReject(delivery.getEnvelope().getDeliveryTag(), true);
					log.error("Failed to process message", e);
				} catch (Throwable e) {
					channel.basicReject(delivery.getEnvelope().getDeliveryTag(), true);
					throw new MessageProcessingException("Failed to process message " + body, e);
				}
				System.out.println(" [x] Done");
			} 
		}
		catch (Throwable e)
		{
			throw new MessagingException("Listener to event queue " + queue + " failed.", e);
		}
		finally {
			try { channel.close(); } catch (Exception e1) {}
			try { connection.close(); } catch (Exception e1) {}
		}  
	}

	@Override
	public void stop()
	{
		stop = true;
	}

	@Override
	public boolean touch(Object messageId, String queue)
			throws MessagingException
	{
		// messages don't automatically expire in rabbit. nothing to do here.
		return true;
	}

    @Override
    public List<Object> listQueues() throws MessagingException, NotImplementedException {
        throw new NotImplementedException();
    }

    @Override
    public List<String> listQueueNames() throws MessagingException, NotImplementedException {
        throw new NotImplementedException();
    }

    @Override
    public boolean queueExist(String queueName) throws MessagingException {
        throw new NotImplementedException();
    }

    @Override
    public String findQueueMatching(String regex) throws MessagingException {
        throw new NotImplementedException();
    }

    @Override
    public boolean messageExist(String queue, Object messageId) throws MessagingException {
        throw new NotImplementedException();
    }
}
