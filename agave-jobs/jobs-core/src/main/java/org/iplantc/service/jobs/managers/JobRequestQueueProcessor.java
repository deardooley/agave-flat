package org.iplantc.service.jobs.managers;

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.common.util.TimeUtils;
import org.iplantc.service.jobs.exceptions.JobProcessingException;
import org.iplantc.service.systems.model.BatchQueue;
import org.iplantc.service.systems.model.ExecutionSystem;

/**
 * Handles validation of queue selection and 
 * limits in a job request.
 * @author dooley
 *
 */
public class JobRequestQueueProcessor {

	private ExecutionSystem executionSystem;
	private Software software;
	
	public JobRequestQueueProcessor(Software software, ExecutionSystem executionSystem) {
		this.executionSystem = executionSystem;
		this.software = software;
	}
	
	/**
	 * Validates the {@link BatchQueue} values passed into a job request 
	 * or selects a valid default queue on the execution system if no
	 * {@code queue} or {@code batchQueue} fields are found in the job request.
	 * 
	 * @param jobRequestMap 
	 * @throws JobProcessingException
	 */
	public BatchQueue process(Map<String, Object> jobRequestMap) 
	throws JobProcessingException 
	{
		BatchQueue selectedQueue = null;;
		
		// we support the batchQueue parameter 
		String userSuppliedQueueName = (String)jobRequestMap.get("batchQueue");
		
		// if that's not supplied, check the legacy queue parameter
		if (StringUtils.isEmpty(userSuppliedQueueName)) {
			userSuppliedQueueName = (String)jobRequestMap.get("queue");
		}
		
		// honor the name length
		if (StringUtils.isEmpty(userSuppliedQueueName)) {
			
			// use the software default queue if present, otherwise we'll pick it for them in a bit
			if (StringUtils.isNotEmpty(software.getDefaultQueue())) {
				
				selectedQueue = executionSystem.getQueue(software.getDefaultQueue());
				
				if (selectedQueue == null) {
					throw new JobProcessingException(400,
							"Invalid default batchQueue. The default batchQueue, " + software.getDefaultQueue() +
							", specified by " + software.getUniqueName() +
							", is not defined on system " + executionSystem.getSystemId());
				}
				else {
					return selectedQueue;
				}
			}
			else {
				// we will select a queue later on based on the node, memory, proc, runtime, etc
				// parameters in the job request.
				
				return null;
			}
		}
		// user gave a queue. see if it's a valid queue on the execution system
		else {
			selectedQueue = executionSystem.getQueue(userSuppliedQueueName);
			if (selectedQueue == null) {
				throw new JobProcessingException(400,
						"Invalid batchQueue. No batchQueue named " + userSuppliedQueueName +
						" is defined on system " + executionSystem.getSystemId());
			}
			else {
				return selectedQueue;
			}
		}
	}
	
	/**
	 * Finds queue on the given executionSystem that supports the given number of nodes and
	 * memory per node given.
	 *
	 * @param nodes a positive integer value or -1 for no limit
	 * @param processors positive integer value or -1 for no limit
	 * @param memory memory in GB or -1 for no limit
	 * @param requestedTime time in hh:mm:ss format
	 * @return a BatchQueue matching the given parameters or null if no match can be found
	 */
	public BatchQueue selectQueue(ExecutionSystem executionSystem, Long nodes, Double memory, String requestedTime)
	{

		return selectQueue(executionSystem, nodes, memory, (long)-1, requestedTime);
	}

	/**
	 * Finds queue on the given executionSystem that supports the given number of nodes and
	 * memory per node given.
	 *
	 * @param nodes a positive integer value or -1 for no limit
	 * @param processors positive integer value or -1 for no limit
	 * @param memory memory in GB or -1 for no limit
	 * @param requestedTime time in hh:mm:ss format
	 * @return a BatchQueue matching the given parameters or null if no match can be found
	 */
	public BatchQueue selectQueue(ExecutionSystem executionSystem, Long nodes, Double memory, Long processors, String requestedTime)
	{

		if (validateBatchSubmitParameters(executionSystem.getDefaultQueue(), nodes, processors, memory, requestedTime))
		{
			return executionSystem.getDefaultQueue();
		}
		else
		{
			BatchQueue[] queues = executionSystem.getBatchQueues().toArray(new BatchQueue[]{});
			Arrays.sort(queues);
			for (BatchQueue queue: queues)
			{
				if (queue.isSystemDefault())
					continue;
				else if (validateBatchSubmitParameters(queue, nodes, processors, memory, requestedTime))
					return queue;
			}
		}

		return null;
	}


	/**
	 * Validates that the queue supports the number of nodes, processors per node, memory and
	 * requestedTime provided. If any of these values are null or the given values exceed the queue
	 * limits, it returns false.
	 *
	 * @param queue the BatchQueue to check against
	 * @param nodes a positive integer value or -1 for no limit
	 * @param processors positive integer value or -1 for no limit
	 * @param memory memory in GB or -1 for no limit
	 * @param requestedTime time in hh:mm:ss format
	 * @return true if all the values are non-null and within the limits of the queue
	 */
	public boolean validateBatchSubmitParameters(BatchQueue queue, Long nodes, Long processors, Double memory, String requestedTime)
	{
		if (queue == null ||
			nodes == null ||  nodes == 0 || nodes < -1 ||
			processors == null || processors == 0 || processors < -1 ||
			memory == null || memory == 0 || memory < -1 ||
			StringUtils.isEmpty(requestedTime) || StringUtils.equals("00:00:00", requestedTime))
		{
			return false;
		}

		if (queue.getMaxNodes() > 0 && queue.getMaxNodes() < nodes) {
			return false;
		}

		if (queue.getMaxProcessorsPerNode() > 0 && queue.getMaxProcessorsPerNode() < processors) {
			return false;
		}

		if (queue.getMaxMemoryPerNode() > 0 && queue.getMaxMemoryPerNode() < memory) {
			return false;
		}

		if (queue.getMaxRequestedTime() != null &&
				TimeUtils.compareRequestedJobTimes(queue.getMaxRequestedTime(), requestedTime) == -1)

		{
			return false;
		}

		return true;
	}

}
