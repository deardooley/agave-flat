/**
 * 
 */
package org.iplantc.service.common.queue;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.exceptions.AgaveCacheException;
import org.iplantc.service.common.exceptions.TaskQueueException;
import org.iplantc.service.common.util.AgaveRedissonClient;
import org.redisson.Redisson;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;

import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
/**
 * @author dooley
 *
 */
public class DistributedBlockingTaskQueue {
	
	private static final Logger log = Logger
			.getLogger(DistributedBlockingTaskQueue.class);
	
	private RBlockingQueue<String> queue = null;
	private String tenant;
	private String queueName;
	
	/**
	 * 
	 */
	public DistributedBlockingTaskQueue(String tenant, String queueName) {
//		this.queueName = queueName;
		this.tenant = tenant;
	}
	
	/**
	 * Fetch the next element and remove it from the queue. 
	 * @param queueName name of the task queue
	 * @return the next value on the queue
	 * @throws TaskQueueException 
	 */
	public String pop(String queueName) 
	throws TaskQueueException {
		Set<String> values = pop(queueName, 1);
		if (values.isEmpty()) {
			return null;
		} else {
			return values.iterator().next();
		}
	}
	
	/**
	 * Fetch the next element and remove it from the queue. 
	 * @param queueName name of the task queue
	 * @return the next value on the queue
	 * @throws TaskQueueException 
	 */
	public Set<String> pop(String queueName, int count) 
	throws TaskQueueException {
		try {
		  HashSet<String> set = new HashSet<String>();
		  getQueue().drainTo(set, count);
		  return set;
		} 
		catch (RuntimeException e) {
			throw new TaskQueueException("Failed to check queue.", e);
		}
	}
	
	/**
	 * Remove an element from the queue. 
	 * @param queueName name of the task queue
	 * @return true of the element was removed. false if it was not or did not exist
	 * @throws TaskQueueException 
	 */
	public boolean remove(String value) 
	throws TaskQueueException {
		try {
		  return getQueue().remove(value);
		} 
		catch (RuntimeException e) {
			throw new TaskQueueException("Failed to add "+value+" to queue.", e);
		}
	}
	
	/**
	 * Push an element into the queue. 
	 * @param queueName name of the task queue
	 * @param value
	 * @return true of the element was added. false if the value was already present
	 * @throws TaskQueueException 
	 */
	public boolean push(String queueName, String value) throws TaskQueueException {
		try {
		  return getQueue().add(queueName);
		} 
		catch (RuntimeException e) {
			throw new TaskQueueException("Failed to add "+value+" to queue.", e);
		}
	}
	
	/**
	 * Checks for existence of a value in a 
	 * @param queueName the name of the queue to check for the given value
	 * @param value the value to check for existence
	 * @return
	 * @throws TaskQueueException
	 */
	public boolean contains(String value) 
	throws TaskQueueException {
		try {
		  return getQueue().contains(queueName);
		} 
		catch (RuntimeException e) {
			throw new TaskQueueException("Failed to check existence of "+value+" in the queue.", e);
		}
	}
	
	/**
	 * Determines if the queue already exists 
	 * @param queueName the name of the queue to check for existence
	 * @return true if the queue exists, false otherwise
	 * @throws TaskQueueException
	 */
	public boolean containsKey(String queueName) 
	throws TaskQueueException {
		try {
		  return getQueue().isExists();
		} 
		catch (RuntimeException e) {
			throw new TaskQueueException("Failed to check queue.", e);
		}
	}

	/**
	 * @return the queue
	 * @throws AgaveCacheException 
	 */
	public RBlockingQueue<String> getQueue() 
	throws TaskQueueException {
		if (queue == null) {
			try {
				queue = AgaveRedissonClient.getInstance().getBlockingDeque(queueName);
			}
			catch (AgaveCacheException e) {
				throw new TaskQueueException("Failed to connect to redis.", e);
			}
		}
		
		return queue;
	}
}
