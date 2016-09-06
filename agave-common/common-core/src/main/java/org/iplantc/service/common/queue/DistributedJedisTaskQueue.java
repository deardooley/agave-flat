/**
 * 
 */
package org.iplantc.service.common.queue;

import java.util.Set;

import org.apache.log4j.Logger;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.exceptions.TaskQueueException;

import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
/**
 * @author dooley
 *
 */
public class DistributedJedisTaskQueue {
	
	private static final Logger log = Logger
			.getLogger(DistributedJedisTaskQueue.class);
	
	private static ThreadLocal<JedisPool> jedisPoolThreadlocal = new ThreadLocal<JedisPool>();
	private String tenant;
//	private String table;
	
	/**
	 * 
	 */
	public DistributedJedisTaskQueue(String tenant) {
//		this.table = table;
		this.tenant = tenant;
	}
	
	public Jedis getClient() {
		if (jedisPoolThreadlocal.get() == null) {
			jedisPoolThreadlocal.set(new JedisPool(new JedisPoolConfig(), Settings.CACHE_HOST, Settings.CACHE_PORT));
		}
		
		return jedisPoolThreadlocal.get().getResource();
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
		Jedis jedis = null;
		try {
		  jedis = getClient();
		  return jedis.spop(queueName, count);
		} 
		catch (JedisConnectionException e) {
			throw new TaskQueueException("Failed to connect to redis.", e);
		}
		catch (JedisException e) {
			throw new TaskQueueException("Failed to pop element off queue", e);
		}
		finally {
		  if (jedis != null) {
		    jedis.close();
		  }
		}
	}
	
	/**
	 * Remove an element from the queue. 
	 * @param queueName name of the task queue
	 * @param value the value to remove
	 * @return true of the element was removed. false if it was not or did not exist
	 * @throws TaskQueueException 
	 */
	public boolean remove(String queueName, String value) 
	throws TaskQueueException {
		Jedis jedis = null;
		try {
		  jedis = getClient();
		  return jedis.srem(queueName, value) == (long)1;
		} 
		catch (JedisConnectionException e) {
			throw new TaskQueueException("Failed to connect to redis.", e);
		}
		catch (JedisException e) {
			throw new TaskQueueException("Failed to push element onto queue", e);
		}
		finally {
		  if (jedis != null) {
		    jedis.close();
		  }
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
		Jedis jedis = null;
		try {
		  jedis = getClient();
		  return jedis.sadd(queueName, value) == (long)1;
		} 
		catch (JedisConnectionException e) {
			throw new TaskQueueException("Failed to connect to redis.", e);
		}
		catch (JedisException e) {
			throw new TaskQueueException("Failed to push element onto queue", e);
		}
		finally {
		  if (jedis != null) {
		    jedis.close();
		  }
		}
	}
	
	/**
	 * Checks for existence of a value in a 
	 * @param queueName the name of the queue to check for the given value
	 * @param value the value to check for existence
	 * @return
	 * @throws TaskQueueException
	 */
	public boolean contains(String queueName, String value) 
	throws TaskQueueException {
		Jedis jedis = null;
		try {
		  jedis = getClient();
		  return jedis.sismember(queueName, value);
		} 
		catch (JedisConnectionException e) {
			throw new TaskQueueException("Failed to connect to redis.", e);
		}
		catch (JedisException e) {
			throw new TaskQueueException("Failed to determine existence of the value in redis", e);
		}
		finally {
		  if (jedis != null) {
		    jedis.close();
		  }
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
		Jedis jedis = null;
		try {
		  jedis = getClient();
		  return jedis.exists(queueName);
		} 
		catch (JedisConnectionException e) {
			throw new TaskQueueException("Failed to connect to redis.", e);
		}
		catch (JedisException e) {
			throw new TaskQueueException("Failed to determine existence of the key in redis", e);
		}
		finally {
		  if (jedis != null) {
		    jedis.close();
		  }
		}
	}
}
