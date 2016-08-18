/**
 * 
 */
package org.iplantc.service.common.messaging.clients;

import io.iron.ironmq.Cloud;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.exceptions.MessageProcessingException;
import org.iplantc.service.common.exceptions.MessagingException;
import org.iplantc.service.common.messaging.model.Message;

import com.surftools.BeanstalkClient.Job;
import com.surftools.BeanstalkClientImpl.ClientImpl;

/**
 * @author dooley
 *
 */
public class IronBeanstalkClient implements MessageQueueClient
{
	private static final Logger log = Logger.getLogger(IronBeanstalkClient.class);
	
	private ClientImpl nativeClient;
	private boolean stop = false; 
	
	public IronBeanstalkClient() {
		nativeClient = new ClientImpl(Cloud.ironAWSUSEast.getHost(), 11300);
		nativeClient.put(1L, 0, 3600, ("oauth " + Settings.MESSAGING_SERVICE_PASSWORD + " " + Settings.MESSAGING_SERVICE_USERNAME).getBytes());
	}
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.messaging.MessagingClient#push(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void push(String exchange, String queue, String message)
			throws MessagingException
	{
		try {
			nativeClient.useTube(queue);
			long jobId = nativeClient.put(65536, 0, 120, message.getBytes());
			if (jobId <= 0) {
				throw new IOException();
			}
		} catch (Exception e) {
			throw new MessagingException("Failed to push message to the " + queue + " tube", e);
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
			nativeClient.useTube(queue);
			long jobId = nativeClient.put(65536, secondsToDelay, 120, message.getBytes());
			if (jobId <= 0) {
				throw new IOException();
			}
		} catch (Exception e) {
			throw new MessagingException("Failed to push message to the " + queue + " tube", e);
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
			nativeClient.watch(queue);
			nativeClient.useTube(queue);
			
			nativeClient.release((Long)messageId, 65536, 0);
		} 
		catch (Exception e) {
			throw new MessagingException("Failed to release message back to the tube.", e);
		}
	}
	
	@Override
	public void delete(String exchange, String queue, Object messageId) throws MessagingException
	{
		try {
			nativeClient.watch(queue);
			nativeClient.useTube(queue);
			
			nativeClient.release((Long)messageId, 65536, 0);
			nativeClient.delete((Long)messageId);
		} 
		catch (Exception e) {
			throw new MessagingException("Failed to remove from the tube", e);
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
			nativeClient.watch(queue);
			nativeClient.useTube(queue);
			Job beanstalkJob = nativeClient.reserve(timeout);
			
			nativeClient.ignore(queue);
			return new Message(beanstalkJob.getJobId(), new String(beanstalkJob.getData()));
		}
		catch (Exception e)
		{
			throw new MessagingException("Failed to retrieve message from the " + queue + " tube", e);
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
			nativeClient.watch(queue);
			nativeClient.useTube(queue);
			Job beanstalkJob = nativeClient.reserve(null);
			nativeClient.ignore(queue);
			return new Message(beanstalkJob.getJobId(), new String(beanstalkJob.getData()));
		}
		catch (Exception e)
		{
			throw new MessagingException("Failed to retrieve message from the " + queue + " tube", e);
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
			nativeClient.watch(queue);
			nativeClient.useTube(queue);
			for (int i=0;i<count;i++) 
			{
				Job beanstalkJob = nativeClient.reserve(null);
				messages.add(new Message(beanstalkJob.getJobId(), new String(beanstalkJob.getData())));
			}
			nativeClient.ignore(queue);
			return messages;
		}
		catch (Exception e)
		{
			throw new MessagingException("Failed to retrieve messages from the " + queue + " tube", e);
		}
	}
	
	@Override
	public boolean touch(Object messageId, String queue) throws MessagingException
	{
		try 
		{
			nativeClient.useTube(queue);
			return nativeClient.touch((Long)messageId);
		} 
		catch (Exception e)
		{
			throw new MessagingException("Failed to touch message " + messageId + " from the " + queue + " tube", e);
		}
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
			
			nativeClient.useTube(queue);
			nativeClient.watch(queue);
			while (!stop)
			{
				Job job = null;
				String body = null;
				try 
				{
					job = nativeClient.reserve(null);
					body = new String(job.getData());
					System.out.println(" [x] Received '" + body + "'");
					
					listener.processMessage(body);
					nativeClient.delete(job.getJobId());
				} catch (MessageProcessingException e) {
					log.error(e);
					if (job != null)
						nativeClient.release(job.getJobId(), 65536, 0);
					// message will be returned to the queue
				} catch (Throwable e) {
					throw new MessageProcessingException("Failed to process message " + body, e);
				}
				
				System.out.println(" [x] Done");
			}
		}
		catch (Exception e)
		{
			throw new MessagingException("Listener to event queue " + queue + " failed.", e);
		}
		finally
		{
			try { nativeClient.ignore(queue); } catch (Throwable e) {}
			try { nativeClient.close(); } catch (Throwable e) {}
		}
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.messaging.MessagingClient#stop()
	 */
	@Override
	public void stop()
	{
		stop = true;
		try {nativeClient.close();} catch (Throwable e) {}
	}
	
    @Override
    public List<Object> listQueues() throws MessagingException, NotImplementedException {
        try
        {
            return Arrays.asList(nativeClient.listTubes().toArray());
        }
        catch (Exception e)
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
            return nativeClient.listTubes();
        }
        catch (Exception e)
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
            nativeClient.watch(queue);
            nativeClient.useTube(queue);
            
            Map<String, String> stats = nativeClient.statsJob((Long)messageId);
            
            return (stats != null && !stats.isEmpty());
        } 
        catch (Exception e) {
            throw new MessagingException("Failed to reject message and remove from the tube", e);
        }
    }
}
