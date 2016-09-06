/**
 * 
 */
package org.iplantc.service.common.queue;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.iplantc.service.common.exceptions.AgaveCacheException;
import org.iplantc.service.common.exceptions.TaskQueueException;
import org.iplantc.service.common.exceptions.TaskQueueLockException;
import org.iplantc.service.common.util.AgaveRedissonClient;
import org.redisson.RedissonRedLock;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RLock;

/**
 * Redisson (Redis) backed queue implementing Redlock algorithm
 * ensuring proper locking and task completion
 * @author dooley
 *
 */
public class RedLockTaskQueue {
	
	private static final Logger log = Logger
			.getLogger(RedLockTaskQueue.class);
	
	private String taskName;
	private RBlockingDeque<String> queue = null;
	
	/**
	 * 
	 */
	public RedLockTaskQueue(String taskName) {
		this.setTaskName(taskName);
	}
	
	/**
	 * Obtains a lock on an item in the queue. 
	 * @param resource the serialized resource to lock. Usually a uuid.
	 * @return true if the lock succeeded, false otherwise
	 * @throws TaskQueueException if the queue could not be reached
	 * @IllegalArgumentException if the {@code resource} is not in the queue
	 */
	public boolean lock(String resource) 
	throws TaskQueueException, IllegalArgumentException {
		try {
			log.debug("Attempting to obtain a lock on " + resource);
			if (contains(resource)) {
				RLock resourceLock = AgaveRedissonClient.getInstance().getLock(resource);
				RedissonRedLock lock = new RedissonRedLock(resourceLock);
				return lock.tryLock();
			}
			else {
				throw new IllegalArgumentException("Item not in queue");
			}
		}
		catch (AgaveCacheException e) {
			throw new TaskQueueException("Failed to connect to redis.", e);
		}
	}
	
	/**
	 * Checks whether a resource has a lock of any kind and/or state on it. 
	 * This is needed to find orphaned resources which were 
	 * removed from the queue while locked.
	 *  
	 * @param resource the serialized resource to lock. Usually a uuid.
	 * @return true if the lock exists regardless of state, 
	 * @throws TaskQueueException if the queue could not be reached
	 * @IllegalArgumentException if the {@code resource} is not in the queue
	 */
	public boolean hasLock(String resource) 
	throws TaskQueueException, IllegalArgumentException {
		try {
			log.debug("Attempting to obtain a lock on " + resource);
			if (contains(resource)) {
				RLock resourceLock = AgaveRedissonClient.getInstance().getLock(resource);
				return resourceLock.isExists();
			}
			else {
				throw new IllegalArgumentException("Item not in queue");
			}
		}
		catch (AgaveCacheException e) {
			throw new TaskQueueException("Failed to connect to redis.", e);
		}
	}
	
	/**
	 * Checks whether a resource has an existing active lock on it. 
	 *  
	 * @param resource the serialized resource to lock. Usually a uuid.
	 * @return true if the lock exists regardless of state, 
	 * @throws TaskQueueException if the queue could not be reached
	 * @IllegalArgumentException if the {@code resource} is not in the queue
	 */
	public boolean isLocked(String resource) 
	throws TaskQueueException, IllegalArgumentException {
		try {
			log.debug("Attempting to obtain a lock on " + resource);
			if (contains(resource)) {
				RLock resourceLock = AgaveRedissonClient.getInstance().getLock(resource);
				return resourceLock.isLocked();
			}
			else {
				throw new IllegalArgumentException("Item not in queue");
			}
		}
		catch (AgaveCacheException e) {
			throw new TaskQueueException("Failed to connect to redis.", e);
		}
	}
	
	/**
	 * Releases a lock on an item in the queue. 
	 * @param resource the serialized resource to lock. Usually a uuid.
	 * @return true if the lock was removed or did not exist. false otherwise
	 * @throws TaskQueueException if the queue could not be reached
	 * @IllegalArgumentException if the {@code resource} is not in the queue
	 */
	public void unlock(String resource) 
	throws TaskQueueException, IllegalArgumentException {
		try {
			log.debug("Attempting to unlock " + resource);
			if (contains(resource)) {
				RLock resourceLock = AgaveRedissonClient.getInstance().getLock(resource);
				RedissonRedLock lock = new RedissonRedLock(resourceLock);
				if (resourceLock.isExists()) {
					lock.unlock();
					resourceLock.delete();
				}
			}
			else {
				throw new IllegalArgumentException("Item not in queue");
			}
		}
		catch (AgaveCacheException e) {
			throw new TaskQueueException("Failed to connect to redis.", e);
		}
	}
	
	/**
	 * Fetch the next element and remove it from the queue.  
	 * @return the next value on the queue
	 * @throws TaskQueueException 
	 */
	public String peek() 
	throws TaskQueueException {
		try {
		  if (!getQueue().isEmpty()) {
			  return getQueue().iterator().next();
		  } else {
			  return null;
		  }
		} 
		catch (RuntimeException e) {
			throw new TaskQueueException("Failed to check queue.", e);
		}
	}
	
	/**
	 * Pops an element off the queue and creates a lock on it. 
	 *   
	 * @param taskName name of the task queue
	 * @return the next value on the queue
	 * @throws TaskQueueException 
	 */
	public String popAndLock() 
	throws TaskQueueException {
		String resource = null;
		for (Iterator<String> iter = getQueue().iterator(); iter.hasNext();) {
			String value = iter.next();
			
			// try to get a lock.
			if (lock(resource)) {
				// we have the lock. note the resource
				resource = value;
				
				// remove from the task queue so nothing else processes it.
				getQueue().remove(resource);
				
				break;
			}
			else {
				// move on to the next resource
			}
		} 
		
		return resource;
	}
	
	/**
	 * Remove an element from the queue. 
	 * @param resource name of the task queue
	 * @return true of the element was removed. false if it was not or did not exist
	 * @throws TaskQueueException 
	 */
	public boolean remove(String resource) 
	throws TaskQueueException {
		try {
			log.debug("Removing " + resource + " from " + taskName + " queue");
			boolean result = getQueue().remove(resource);
			log.debug("Successfully removed " + resource + " from " + taskName + " queue");
			return result;
		} 
		catch (RuntimeException e) {
			throw new TaskQueueException("Failed to remove "+resource+" from queue.", e);
		}
	}
	
	/**
	 * Push an element into the queue. 
	 * @param resource
	 * @return true of the element was added. false if the value was already present
	 * @throws TaskQueueException 
	 */
	public boolean push(String resource) throws TaskQueueException {
		try {
		  return getQueue().add(resource);
		} 
		catch (RuntimeException e) {
			throw new TaskQueueException("Failed to add "+resource+" to queue.", e);
		}
	}
	
	/**
	 * Checks for existence of a value in a 
	 * @param taskName the name of the queue to check for the given value
	 * @param value the value to check for existence
	 * @return
	 * @throws TaskQueueException
	 */
	public boolean contains(String value) 
	throws TaskQueueException {
		try {
		  return getQueue().contains(getTaskName());
		} 
		catch (RuntimeException e) {
			throw new TaskQueueException("Failed to check existence of "+value+" in the queue.", e);
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
				queue = AgaveRedissonClient.getInstance().getBlockingDeque(getTaskName());
			}
			catch (AgaveCacheException e) {
				throw new TaskQueueException("Failed to connect to redis.", e);
			}
		}
		
		return queue;
	}

	/**
	 * @return the taskName
	 */
	public String getTaskName() {
		return taskName;
	}

	/**
	 * @param taskName the taskName to set
	 */
	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}
}
