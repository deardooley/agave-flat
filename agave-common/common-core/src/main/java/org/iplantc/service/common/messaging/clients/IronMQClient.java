/**
 * 
 */
package org.iplantc.service.common.messaging.clients;

import io.iron.ironmq.Client;
import io.iron.ironmq.Cloud;
import io.iron.ironmq.EmptyQueueException;
import io.iron.ironmq.Messages;
import io.iron.ironmq.Queue;
import io.iron.ironmq.QueueModel;
import io.iron.ironmq.Queues;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.exceptions.MessageProcessingException;
import org.iplantc.service.common.exceptions.MessagingException;
import org.iplantc.service.common.messaging.model.Message;

/**
 * @author dooley
 *
 */
public class IronMQClient implements MessageQueueClient
{
	private static final Logger log = Logger.getLogger(IronMQClient.class);
	
	private Client client;
	private boolean stop = false; 
	
	public IronMQClient() {
		client = new Client(Settings.MESSAGING_SERVICE_USERNAME, Settings.MESSAGING_SERVICE_PASSWORD, Cloud.ironAWSUSEast);
	}
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.messaging.MessagingClient#push(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void push(String exchange, String queue, String message)
			throws MessagingException
	{
		try {
			
			Queue ironQueue = client.queue(queue);
			ironQueue.push(message, 0);
		} catch (IOException e) {
			throw new MessagingException("Failed to push message to the " + queue + " queue", e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.messaging.MessageQueueClient#push(java.lang.String, java.lang.String, java.lang.String, int)
	 */
	@Override
	public void push(String exchange, String queue, String message, int secondsToDelay)
	throws MessagingException
	{
		try {
			Queue ironQueue = client.queue(queue);
			ironQueue.push(message, secondsToDelay);
		} catch (IOException e) {
			throw new MessagingException("Failed to push message to the " + queue + " queue", e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.messaging.MessagingClient#reject(java.lang.String, java.lang.String, java.lang.Object, java.lang.String)
	 */
	@Override
	public void reject(String exchange, String queue, Object messageId,
			String message) throws MessagingException
	{
		try {
	        Queue ironQueue = client.queue(queue);
	        io.iron.ironmq.Message ironMessage = new io.iron.ironmq.Message();
	        ironMessage.setId((String)messageId);
	        ironQueue.releaseMessage(ironMessage);
        } 
        catch (Exception e) {
            throw new MessagingException("Failed to remove message from the queue", e);
        }
	}
	
	@Override
	public void delete(String exchange, String queue, Object messageId) throws MessagingException
	{
		try {
			Queue ironQueue = client.queue(queue);
			ironQueue.deleteMessage((String)messageId);
		} 
		catch (Exception e) {
			throw new MessagingException("Failed to remove message from the queue", e);
		}
		
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.messaging.MessagingClient#pop(java.lang.String, java.lang.String)
	 */
	@Override
	public Message pop(String exchange, String queue) throws MessagingException
	{
		try
		{
			Queue ironQueue = client.queue(queue);
			io.iron.ironmq.Message msg = ironQueue.reserve();
			return new Message(msg.getId(), msg.getBody());
		}
		catch (EmptyQueueException e) {
            return null;
        }
        catch (IOException e)
		{
			throw new MessagingException("Failed to retrieve message from the " + queue + " queue", e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.messaging.MessagingClient#reserve(java.lang.String, java.lang.String, int)
	 */
	@Override
	public Message reserve(String exchange, String queue, int timeout) throws MessagingException
	{
		try
		{
			Queue ironQueue = client.queue(queue);
			Messages messages = ironQueue.reserve(1, timeout);
			if (messages.getSize() > 0) {
				io.iron.ironmq.Message msg = messages.getMessage(0);
				return new Message(msg.getId(), msg.getBody());
			}
			else {
				return null;
			}
		}
		catch (EmptyQueueException e) {
            return null;
        }
        catch (IOException e)
		{
			throw new MessagingException("Failed to retrieve message from the " + queue + " queue", e);
		}
	}

	
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.messaging.MessagingClient#pop(java.lang.String, java.lang.String)
	 */
	@Override
	public List<Message> pop(String exchange, String queue, int count) throws MessagingException
	{
		List<Message> messages = new ArrayList<Message>();
		
		try
		{
			Queue ironQueue = client.queue(queue);
			Messages msgs = ironQueue.reserve(count);
			for(io.iron.ironmq.Message msg: msgs.getMessages()) {
				messages.add(new Message(msg.getId(), msg.getBody()));
			}
		}
		catch (EmptyQueueException e) {
		    // no messages were available
		}
		catch (IOException e)
		{
			throw new MessagingException("Failed to retrieve messages from the " + queue + " queue", e);
		}
        return messages;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.messaging.MessagingClient#listen(java.lang.String, java.lang.String, org.iplantc.service.common.messaging.MessageQueueListener)
	 */
	@Override
	public void listen(String exchange, String queue, MessageQueueListener listener) 
	throws MessagingException
	{
		try
		{
			stop = false;
			
			Queue ironQueue = client.queue(queue);
			
			while (!stop)
			{   
			    String body = null;
				try 
				{
				    io.iron.ironmq.Message msg = ironQueue.reserve();
				    body = msg.getBody();
	                listener.processMessage(body);
					ironQueue.deleteMessage(msg);
				} catch (MessageProcessingException e) {
					log.error(e);
					// message will be returned to the queue
				} catch (EmptyQueueException e) {
				    // no messages to be fetched.
			    } catch (Throwable e) {
					throw new MessageProcessingException("Failed to process message " + body, e);
				}
			} 
		}
		catch (Exception e)
		{
			throw new MessagingException("Listener to event queue " + queue + " failed.", e);
		}
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.messaging.MessagingClient#stop()
	 */
	@Override
	public void stop()
	{
		stop = true;
	}
	
	@Override
	public boolean touch(Object messageId, String queue)
			throws MessagingException
	{
		return false;
	}
	
    /* (non-Javadoc)
     * @see org.iplantc.service.common.messaging.MessageQueueClient#listQueues()
     */
    @Override
    public List<Object> listQueues() throws MessagingException, NotImplementedException {
        try
        {
            List<Object> queues = new ArrayList<Object>();
            List<String> queueNames = new ArrayList<String>();
            for(QueueModel queue: Queues.getQueues(client)) {
                queueNames.add(queue.getName());
                queues.add(queue);
            }
            return queues;
        }
        catch (IOException e)
        {
            throw new MessagingException("Failed to retrieve info about available queue", e);
        }
    }
    
    /* (non-Javadoc)
     * @see org.iplantc.service.common.messaging.MessageQueueClient#listQueueNames()
     */
    @Override
    public List<String> listQueueNames() throws MessagingException, NotImplementedException {
        try
        {
            List<String> queueNames = new ArrayList<String>();
            for(QueueModel queue: Queues.getQueues(client)) {
                queueNames.add(queue.getName());
            }
            return queueNames;
        }
        catch (IOException e)
        {
            throw new MessagingException("Failed to retrieve info about available queue", e);
        }
    }
    
    /* (non-Javadoc)
     * @see org.iplantc.service.common.messaging.MessageQueueClient#queueExist(java.lang.String)
     */
    @Override
    public boolean queueExist(String queueName) throws MessagingException {
        return listQueueNames().contains(queueName);
    }
    
    /* (non-Javadoc)
     * @see org.iplantc.service.common.messaging.MessageQueueClient#findQueueMatching(java.lang.String)
     */
    @Override
    public String findQueueMatching(String regex) throws MessagingException {
        for(String q: listQueueNames()) {
            if (q.matches(regex)) return q;
        }
        
        return null;
    }
    
    @Override
    public boolean messageExist(String queue, Object messageId) throws MessagingException {
        
        try {
            Queue ironQueue = client.queue(queue);
            ironQueue.getMessageById((String)messageId);
            return true;
        } 
        catch (EmptyQueueException e) {
            return false;
        }
        catch (Exception e) {
            throw new MessagingException("Failed to remove message from the queue", e);
        }
    }
}
