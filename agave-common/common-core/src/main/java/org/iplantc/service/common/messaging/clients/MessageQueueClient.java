package org.iplantc.service.common.messaging.clients;

import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.iplantc.service.common.exceptions.MessageProcessingException;
import org.iplantc.service.common.exceptions.MessagingException;
import org.iplantc.service.common.messaging.model.Message;

public interface MessageQueueClient {

	/**
	 * Publishes a message onto the given queue
	 * @param queue
	 * @param message
	 * @throws MessagingException
	 */
	public void push(String exchange, String queue, String message) throws MessagingException;
	
	/**
	 * Publishes a message onto the given queue with a delayed execution
	 * @param queue
	 * @param message
	 * @param secondsToDelay
	 * @throws MessagingException
	 */
	public void push(String exchange, String queue, String message, int secondsToDelay) throws MessagingException;
	
	/**
	 * Returns a message back to the queue for consumption by a different process.
	 * 
	 * @param messageId
	 * @param message
	 * @throws MessagingException
	 */
	public void reject(String exchange, String queue, Object messageId, String message) throws MessagingException;
	
	/**
	 * Deletes a message from the queue.
	 * 
	 * @param messageId
	 * @throws MessagingException
	 */
	public void delete(String exchange, String queue, Object messageId) throws MessagingException;
	
	/**
	 * Gets the next event from the queue. The distribution of messages is controlled by 
	 * the messaging system. This will simply pull the next messages.
	 * @param queue
	 * @return
	 * @throws MessagingException
	 */
	public Message pop(String exchange, String queue) throws MessagingException;
	
	/**
	 * Gets the next count events from the queue. The distribution of messages is controlled by 
	 * the messaging system. This will simply pull the next count messages.
	 * @param exchange
	 * @param queue
	 * @param count
	 * @return
	 * @throws MessagingException
	 */
	public List<Message> pop(String exchange, String queue, int count) throws MessagingException;
	
//	/**
//	 * Explicitly acknowledge receipt of a message. This is needed in long running tasks.
//	 * @param messageId
//	 * @return
//	 * @throws MessagingException
//	 */
//	public void acknowledge(String exchange,Object messageId) throws MessagingException;
//	
	
	/**
	 * Touches a message or job in the queue to keep it alive. Used for long running tasks.
	 * 
	 * @param messageId
	 * @param queue
	 * @return
	 * @throws MessagingException
	 */
	public boolean touch(Object messageId, String queue) throws MessagingException;
	
	/**
	 * Starts a watch loop that will listen to the event queue and process anything that comes 
	 * until MessagingClient.stop is called 
	 * @param queue
	 * @param listener
	 * @throws MessagingException
	 */
	public void listen(String exchange, String queue, MessageQueueListener listener) 
	throws MessagingException, MessageProcessingException;
	
	/**
	 * Forces a listening loop to terminate
	 */
	public void stop();
	
	/**
	 * Returns a list of all the queues available in the underlying
	 * service. This may not be available uniformly.
	 * 
	 * @return List of the available queues
	 * @throws MessagingException 
	 * @throws NotImplementedException if the underlying service does not support messaging
	 */
	public List<Object> listQueues() throws MessagingException, NotImplementedException;
	
	/**
     * Returns a list of the names of all the queues available in the underlying
     * service. This may not be available uniformly.
     * 
     * @return List of the available queue names
     * @throws MessagingException 
     * @throws NotImplementedException if the underlying service does not support messaging
     */
    public List<String> listQueueNames() throws MessagingException, NotImplementedException;
	
	/**
	 * Returns true if a queue exists match the given name.
	 * 
	 * @param queueName
	 * @return
	 * @throws MessagingException
	 */
	public boolean queueExist(String queueName) throws MessagingException;
	
	/**
	 * Find a queue whose name matches the given regular expression. There is no guarantee
	 * that this will be the best fit match, just that it will match. 
	 * 
	 * @param queueName
	 * @return name of queue matching the regex
	 * @throws MessagingException
	 */
	public String findQueueMatching(String regex) throws MessagingException;
	
	/**
	 * Checks for the existence of a given message.
	 * 
	 * @param queue name of queue
	 * @param messageId unique message id
	 * @return true if a message with the given ID exists. False otherwise.
	 * @throws MessagingException
	 */
	public boolean messageExist(String queue, Object messageId) throws MessagingException;

	/**
	 * Identical to {@link #pop(String, String)}, but waits <code>timeout</code> to
	 * recieve a reserved message. 
	 * 
	 * @param queue name of queue
	 * @param messageId unique message id
	 * @param timeout the timeout of the message in seconds.
	 * @return true if a message with the given ID exists. False otherwise.
	 * @throws MessagingException if a message is not received during timeout or the operation fails
	 */
	public Message reserve(String exchange, String queue, int timeout)
	throws MessagingException;

    
	
}
