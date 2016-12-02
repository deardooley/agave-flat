/**
 * 
 */
package org.iplantc.service.jobs.managers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.exceptions.SoftwareException;
import org.iplantc.service.apps.managers.ApplicationManager;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.common.util.TimeUtils;
import org.iplantc.service.io.dao.LogicalFileDao;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.permissions.PermissionManager;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobProcessingException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobEvent;
import org.iplantc.service.jobs.model.enumerations.JobArchivePathMacroType;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.remote.exceptions.RemoteExecutionException;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.AuthConfig;
import org.iplantc.service.systems.model.BatchQueue;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Handles processing and validation of job requests.
 * Generally speaking, this will delegate most of the work to the other *QueueProcessor
 * classes which have field-specific behavior.
 * 
 * @see {@link JobRequestInputProcessor}
 * @see {@link JobRequestParameterProcessor}
 * @see {@link JobRequestQueueProcessor}
 * @see {@link JobRequestInputProcessor}
 * @see {@link JobRequestNotificationProcessor}
 * @author dooley
 *
 */
public class JobRequestProcessor {

	protected String username;
	protected String internalUsername;
	protected JobRequestQueueProcessor queueProcessor;
	protected JobRequestInputProcessor inputProcessor;
	protected JobRequestParameterProcessor parameterProcessor;
	protected JobRequestNotificationProcessor notificationProcessor;
	protected ExecutionSystem executionSystem;
	protected Software software;
	
	public JobRequestProcessor() {}
		
	/**
	 * 
	 */
	public JobRequestProcessor(String jobRequestOwner, String internalUsername) {
		this.username = jobRequestOwner;
		this.internalUsername = internalUsername;
	}
	
	/**
	 * Takes a JsonNode representing a job request and parses it into a job object.
	 *
	 * @param json a JsonNode containing the job request
	 * @return validated job object ready for submission
	 * @throws JobProcessingException
	 */
	public Job processJob(JsonNode json)
	throws JobProcessingException
	{
	    HashMap<String, Object> jobRequestMap = new HashMap<String, Object>();

		String currentKey = null;

		try
		{
			Iterator<String> fields = json.fieldNames();
   			while(fields.hasNext()) {
				String key = fields.next();

				if (StringUtils.isEmpty(key)) continue;

				currentKey = key;

				if (key.equals("notifications")) {
					continue;
				}
				
				if (key.equals("callbackUrl")) {
					continue;
				}

				if (key.equals("dependencies"))
				{
					throw new JobProcessingException(400,
							"Job dependencies are not yet supported.");
				}

				JsonNode child = json.get(key);

				if (child.isNull()) {
				    jobRequestMap.put(key, null);
				}
				else if (child.isNumber())
				{
				    jobRequestMap.put(key, child.asText());
				}
				else if (child.isObject())
				{
					Iterator<String> childFields = child.fieldNames();
					while(childFields.hasNext())
					{
						String childKey = childFields.next();
						JsonNode childchild = child.path(childKey);
						if (StringUtils.isEmpty(childKey) || childchild.isNull() || childchild.isMissingNode()) {
							continue;
						}
						else if (childchild.isDouble()) {
						    jobRequestMap.put(childKey, childchild.decimalValue().toPlainString());
						}
						else if (childchild.isNumber())
						{
						    jobRequestMap.put(childKey, new Long(childchild.longValue()).toString());
						}
						else if (childchild.isArray()) {
						    List<String> arrayValues = new ArrayList<String>();
							for (Iterator<JsonNode> argIterator = childchild.iterator(); argIterator.hasNext();)
							{
								JsonNode argValue = argIterator.next();
								if (argValue.isNull() || argValue.isMissingNode()) {
									continue;
								} else {
								    arrayValues.add(argValue.asText());
								}
							}
							jobRequestMap.put(childKey, StringUtils.join(arrayValues, ";"));
						}
						else if (childchild.isTextual()) {
						    jobRequestMap.put(childKey, childchild.textValue());
						}
						else if (childchild.isBoolean()) {
						    jobRequestMap.put(childKey, childchild.asBoolean() ? "true" : "false");
						}
					}
				}
				else
				{
				    jobRequestMap.put(key, json.get(key).asText());
				}
			}

//   			List<Notification> notifications = new ArrayList<Notification>();

//   			if (json.has("dependencies"))
//			{
//   				if (!json.get("dependencies").isArray())
//				{
//					throw new NotificationException("Invalid " + currentKey + " value given. "
//							+ "dependencies must be an array of dependency objects one or more "
//							+ "valid dependency constraints.");
//				}
//				else
//				{
//					currentKey = "dependencies";
//
//					ArrayNode jsonDependencies = (ArrayNode)json.get("dependencies");
//					for (int i=0; i<jsonDependencies.size(); i++)
//					{
//						currentKey = "dependencies["+i+"]";
//						JsonNode jsonDependency = jsonDependencies.get(i);
//						JobDependency dependency = JobDependency.fromJson(dependency);
//
//					}
//				}
//			}
   			
   			Job job = processJob(jobRequestMap);
   			
   			this.notificationProcessor = new JobRequestNotificationProcessor(this.username, job);
   			
			if (json.has("notifications")) {
				this.notificationProcessor.process(json.get("notifications"));
			}
			else if (json.has("callbackUrl")) {
				this.notificationProcessor.process(json.get("callbackUrl"));
			}
			
			for (Notification notification: this.notificationProcessor.getNotifications()) {
				job.addNotification(notification);
			}

			// If the job request had notification configured for job creation
			// they could not have fired yet. Here we explicitly add them.
			for (JobEvent jobEvent: job.getEvents()) {
			    JobEventProcessor eventProcessor = new JobEventProcessor(jobEvent);
			    eventProcessor.process();
			}

			return job;
		}
		catch (NotificationException e) {
			throw new JobProcessingException(500, e.getMessage());
		}
		catch (JobProcessingException e) {
			throw e;
		}
		catch (SoftwareException e) {
			throw new JobProcessingException(400, e.getMessage());
		}
		catch (Throwable e) {
			throw new JobProcessingException(400,
					"Failed to parse json job description. Invalid value for " +
							currentKey + ". " + e.getMessage(), e);
		}
	}
	
	/**
	 * Takes a JsonNode representing a job request and parses it into a job object.
	 *
	 * @param json a JsonNode containing the job request
	 * @return validated job object ready for submission
	 * @throws JobProcessingException
	 */
	public Job processResubmissionJob(JsonNode json)
	throws JobProcessingException
	{
	    HashMap<String, Object> jobRequestMap = new HashMap<String, Object>();

		String currentKey = null;

		try
		{
			Iterator<String> fields = json.fieldNames();
   			while(fields.hasNext()) {
				String key = fields.next();

				if (StringUtils.isEmpty(key)) continue;

				currentKey = key;

				if (key.equals("notifications")) {
					continue;
				}
				
				if (key.equals("callbackUrl")) {
					continue;
				}

				if (key.equals("dependencies"))
				{
					throw new JobProcessingException(400,
							"Job dependencies are not yet supported.");
				}

				JsonNode child = json.get(key);

				if (child.isNull()) {
				    jobRequestMap.put(key, null);
				}
				else if (child.isNumber())
				{
				    jobRequestMap.put(key, child.asText());
				}
				else if (child.isObject())
				{
					Iterator<String> childFields = child.fieldNames();
					while(childFields.hasNext())
					{
						String childKey = childFields.next();
						JsonNode childchild = child.path(childKey);
						if (StringUtils.isEmpty(childKey) || childchild.isNull() || childchild.isMissingNode()) {
							continue;
						}
						else if (childchild.isDouble()) {
						    jobRequestMap.put(childKey, childchild.decimalValue().toPlainString());
						}
						else if (childchild.isNumber())
						{
						    jobRequestMap.put(childKey, new Long(childchild.longValue()).toString());
						}
						else if (childchild.isArray()) {
						    List<String> arrayValues = new ArrayList<String>();
							for (Iterator<JsonNode> argIterator = childchild.iterator(); argIterator.hasNext();)
							{
								JsonNode argValue = argIterator.next();
								if (argValue.isNull() || argValue.isMissingNode()) {
									continue;
								} else {
								    arrayValues.add(argValue.asText());
								}
							}
							jobRequestMap.put(childKey, StringUtils.join(arrayValues, ";"));
						}
						else if (childchild.isTextual()) {
						    jobRequestMap.put(childKey, childchild.textValue());
						}
						else if (childchild.isBoolean()) {
						    jobRequestMap.put(childKey, childchild.asBoolean() ? "true" : "false");
						}
					}
				}
				else
				{
				    jobRequestMap.put(key, json.get(key).asText());
				}
			}
   			
   			Job job = processJob(jobRequestMap);
   			
   			this.notificationProcessor = new JobRequestNotificationProcessor(this.username, job);
   			
			if (json.has("notifications")) {
				this.notificationProcessor.process(json.get("notifications"));
			}
			else if (json.has("callbackUrl")) {
				this.notificationProcessor.process(json.get("callbackUrl"));
			}

			
			
			for (Notification notification: this.notificationProcessor.getNotifications()) {
				job.addNotification(notification);
			}

			// If the job request had notification configured for job creation
			// they could not have fired yet. Here we explicitly add them.
			for (JobEvent jobEvent: job.getEvents()) {
			    JobEventProcessor eventProcessor = new JobEventProcessor(jobEvent);
			    eventProcessor.process();
			}

			return job;
		}
		catch (NotificationException e) {
			throw new JobProcessingException(500, e.getMessage());
		}
		catch (JobProcessingException e) {
			throw e;
		}
		catch (SoftwareException e) {
			throw new JobProcessingException(400, e.getMessage());
		}
		catch (Throwable e) {
			throw new JobProcessingException(400,
					"Failed to parse json job description. Invalid value for " +
							currentKey + ". " + e.getMessage(), e);
		}
	}

	/**
	 * Takes a Form representing a job request and parses it into a job object. This is a
	 * stripped down, unstructured version of the other processJob method.
	 *
	 * @param json a JsonNode containing the job request
	 * @return validated job object ready for submission
	 * @throws JobProcessingException
	 */
	public Job processJob(Map<String, Object> jobRequestMap)
	throws JobProcessingException
	{
	    Job job = new Job();

		// validate the user gave a valid job name
		String name = processNameRequest(jobRequestMap);
		
		// validate the user gave a valid software description
		setSoftware(processSoftwareName(jobRequestMap));

		// validate the optional execution system matches the software execution system
		setExecutionSystem(processExecutionSystemRequest(jobRequestMap, getSoftware()));
		
		/***************************************************************************
		 **						Batch Parameter Selection 						  **
		 ***************************************************************************/

		String currentParameter = null;
		String queueName = null;
		BatchQueue jobQueue = null;
		Long nodeCount = null;
		Double memoryPerNode = null;
		String requestedTime = null;
		Long processorsPerNode = null;

		try
		{
			/********************************** Queue Selection *****************************************/

			currentParameter = "batchQueue";
			this.queueProcessor = new JobRequestQueueProcessor(getSoftware(), executionSystem);
			
			jobQueue = this.queueProcessor.process(jobRequestMap);

			/********************************** Node Count Selection *****************************************/

			currentParameter = "nodeCount";
			String userNodeCount = (String)jobRequestMap.get("nodeCount");
			nodeCount = processNodeCountRequest(getSoftware(), userNodeCount);


			// if the queue wasn't specified by the user or app, pick a queue with just node count info
			if (jobQueue == null) {
				jobQueue = this.queueProcessor.selectQueue(executionSystem, nodeCount, -1.0, (long)-1, BatchQueue.DEFAULT_MIN_RUN_TIME);
			}

			if (jobQueue == null) {
				throw new JobProcessingException(400, "Invalid " +
						(StringUtils.isEmpty(userNodeCount) ? "" : "default ") +
						"nodeCount. No queue found on " +
						executionSystem.getSystemId() + " that support jobs with " +
						nodeCount + " nodes.");
			} else if (!this.queueProcessor.validateBatchSubmitParameters(jobQueue, nodeCount, (long)-1, -1.0, BatchQueue.DEFAULT_MIN_RUN_TIME)) {
				throw new JobProcessingException(400, "Invalid " +
						(StringUtils.isEmpty(userNodeCount) ? "" : "default ") +
						"nodeCount. The " + jobQueue.getName() + " queue on " +
						executionSystem.getSystemId() + " does not support jobs with " + nodeCount + " nodes.");
			}

			/********************************** Max Memory Selection *****************************************/

			currentParameter = "memoryPerNode";
			String userMemoryPerNode = (String)jobRequestMap.get("memoryPerNode");
			if (StringUtils.isEmpty(userMemoryPerNode)) {
				userMemoryPerNode = (String)jobRequestMap.get("maxMemory");
			}

			if (StringUtils.isEmpty(userMemoryPerNode))
			{
				if (getSoftware().getDefaultMemoryPerNode() != null) {
					memoryPerNode = getSoftware().getDefaultMemoryPerNode();
				}
				else if (jobQueue.getMaxMemoryPerNode() != null && jobQueue.getMaxMemoryPerNode() > 0) {
					memoryPerNode = jobQueue.getMaxMemoryPerNode();
				}
				else {
					memoryPerNode = (double)0;
				}
			}
			else // memory was given, validate
			{
				try {
					// try to parse it as a number in GB first
					memoryPerNode = Double.parseDouble(userMemoryPerNode);
				}
				catch (Throwable e)
				{
					// Otherwise parse it as a string matching ###.#[EPTGM]B
					try
					{
						memoryPerNode = BatchQueue.parseMaxMemoryPerNode(userMemoryPerNode);
					}
					catch (NumberFormatException e1)
					{
						memoryPerNode = (double)0;
					}
				}
			}

			if (memoryPerNode <= 0) {
				throw new JobProcessingException(400,
						"Invalid " + (StringUtils.isEmpty(userMemoryPerNode) ? "" : "default ") +
						"memoryPerNode. memoryPerNode should be a postive value specified in ###.#[EPTGM]B format.");
			}

			if (!this.queueProcessor.validateBatchSubmitParameters(jobQueue, nodeCount, (long)-1, memoryPerNode, BatchQueue.DEFAULT_MIN_RUN_TIME)) {
				throw new JobProcessingException(400, "Invalid " +
						(StringUtils.isEmpty(userMemoryPerNode) ? "" : "default ") +
						"memoryPerNode. The " + jobQueue.getName() + " queue on " +
						executionSystem.getSystemId() + " does not support jobs with " + nodeCount + " nodes and " +
						memoryPerNode + "GB memory per node");
			}

			/********************************** Run Time Selection *****************************************/

			currentParameter = "requestedTime";
			//requestedTime = pTable.containsKey("requestedTime") ? pTable.get("requestedTime") : software.getDefaultMaxRunTime();

			String userRequestedTime = (String)jobRequestMap.get("maxRunTime");
			if (StringUtils.isEmpty(userRequestedTime)) {
				// legacy compatibility
				userRequestedTime = (String)jobRequestMap.get("requestedTime");
			}

			if (StringUtils.isEmpty(userRequestedTime))
			{
				if (!StringUtils.isEmpty(getSoftware().getDefaultMaxRunTime())) {
					requestedTime = getSoftware().getDefaultMaxRunTime();
				} else if (!StringUtils.isEmpty(jobQueue.getMaxRequestedTime())) {
					requestedTime = jobQueue.getMaxRequestedTime();
				}
			}
			else
			{
				requestedTime = userRequestedTime;
			}

			if (!TimeUtils.isValidRequestedJobTime(requestedTime)) {
				throw new JobProcessingException(400,
						"Invalid maxRunTime. maxRunTime should be the maximum run time " +
							"time for this job in hh:mm:ss format.");
			} else if (TimeUtils.compareRequestedJobTimes(requestedTime, BatchQueue.DEFAULT_MIN_RUN_TIME) == -1) {
				throw new JobProcessingException(400,
						"Invalid maxRunTime. maxRunTime should be greater than 00:00:00.");
			}

			if (!this.queueProcessor.validateBatchSubmitParameters(jobQueue, nodeCount, (long)-1, memoryPerNode, requestedTime)) {
				throw new JobProcessingException(400, "Invalid " +
						(StringUtils.isEmpty(userRequestedTime) ? "" : "default ") +
						"maxRunTime. The " + jobQueue.getName() + " queue on " +
						executionSystem.getSystemId() + " does not support jobs with " + nodeCount + " nodes, " +
						memoryPerNode + "GB memory per node, and a run time of " + requestedTime);
			}

			/********************************** Max Processors Selection *****************************************/

			currentParameter = "processorsPerNode";
			String userProcessorsPerNode = (String)jobRequestMap.get("processorsPerNode");
			if (StringUtils.isEmpty(userProcessorsPerNode)) {
				userProcessorsPerNode = (String)jobRequestMap.get("processorCount");
			}
			if (StringUtils.isEmpty(userProcessorsPerNode))
			{
				if (getSoftware().getDefaultProcessorsPerNode() != null) {
					processorsPerNode = getSoftware().getDefaultProcessorsPerNode();
				} else if (jobQueue.getMaxProcessorsPerNode() != null && jobQueue.getMaxProcessorsPerNode() > 0) {
					processorsPerNode = jobQueue.getMaxProcessorsPerNode();
				} else {
					processorsPerNode = new Long(1);
				}
			}
			else
			{
				processorsPerNode = NumberUtils.toLong(userProcessorsPerNode);
			}

			if (processorsPerNode < 1) {
				throw new JobProcessingException(400,
						"Invalid " + (StringUtils.isEmpty(userProcessorsPerNode) ? "" : "default ") +
						"processorsPerNode value. processorsPerNode must be a positive integer value.");
			}

			if (!this.queueProcessor.validateBatchSubmitParameters(jobQueue, nodeCount, processorsPerNode, memoryPerNode, requestedTime)) {
				throw new JobProcessingException(400, "Invalid " +
						(StringUtils.isEmpty(userProcessorsPerNode) ? "" : "default ") +
						"processorsPerNode. The " + jobQueue.getName() + " queue on " +
						executionSystem.getSystemId() + " does not support jobs with " + nodeCount + " nodes, " +
						memoryPerNode + "GB memory per node, a run time of " + requestedTime + " and " +
						processorsPerNode + " processors per node");
			}
		}
		catch (JobProcessingException e)
		{
			throw e;
		}
		catch (Exception e) {
			throw new JobProcessingException(400, "Invalid " + currentParameter + " value.", e);
		}

		/***************************************************************************
		 **						Verifying remote connectivity 					  **
		 ***************************************************************************/

		checkExecutionSystemLogin(executionSystem);
		checkExecutionSystemStorage(executionSystem);

		/***************************************************************************
		 **						Verifying Input Parmaeters						  **
		 ***************************************************************************/

		// Verify the inputs by their keys given in the SoftwareInputs
		// in the Software object. 
		getInputProcessor().process(jobRequestMap);
		ObjectNode jobInputs = getInputProcessor().getJobInputs();
		
		/***************************************************************************
		 **						Verifying  Parameters							  **
		 ***************************************************************************/
		getParameterProcessor().process(jobRequestMap);
		ObjectNode jobParameters = this.parameterProcessor.getJobParameters();
		
		/***************************************************************************
         **                 Create and assign job data                            **
         ***************************************************************************/

        try
        {
            // create a job object
            job.setName(name);
            job.setOwner(username);
            job.setSoftwareName(getSoftware().getUniqueName());
            job.setInternalUsername(internalUsername);
            job.setSystem(getSoftware().getExecutionSystem().getSystemId());
            job.setBatchQueue(jobQueue.getName());
            job.setNodeCount(nodeCount);
            job.setProcessorsPerNode(processorsPerNode);
            job.setMemoryPerNode(memoryPerNode);
            job.setMaxRunTime(requestedTime);
            job.setInputsAsJsonObject(jobInputs);
            job.setParametersAsJsonObject(jobParameters);
            job.setSubmitTime(new DateTime().toDate());
        }
        catch (JobException e) {
            throw new JobProcessingException(500, e.getMessage(), e);
        }
        
        /***************************************************************************
		 **						End Batch Queue Selection 						  **
		 ***************************************************************************/

        
        /***************************************************************************
		 **						Verifying optional notifications				  **
		 ***************************************************************************/
        
		processCallbackUrlRequest(jobRequestMap, job);

		/***************************************************************************
		 **						Verifying archive configuration					  **
		 ***************************************************************************/

        // determine whether the user wanted to archiving the output
		boolean archiveOutput = processArchiveOutputRequest(jobRequestMap);
		job.setArchiveOutput(archiveOutput);
		
		// find the archive system 
		RemoteSystem archiveSystem = processArchiveSystemRequest(archiveOutput, jobRequestMap);
		job.setArchiveSystem(archiveSystem);
		
		// process the path now that we have all the info to resolve runtime job macros used
		// in a user-supplied archive path.
		String archivePath = processArchivePath(archiveOutput, jobRequestMap, archiveSystem, job);
		
		try
        {
		    job.setArchivePath(archivePath);

            // persisting the job makes it available to the job queue
            // for submission
		    DateTime created = new DateTime();
		    job.setCreated(created.toDate());
            JobDao.persist(job);
            job.setCreated(created.toDate());
            job.setStatus(JobStatusType.PENDING, JobStatusType.PENDING.getDescription());
            JobDao.persist(job);

            return job;
        }
        catch (Throwable e)
        {
            throw new JobProcessingException(500, "Failed to submit the request to the job queue.", e);
        }
	}

	/**
	 * @param jobRequestMap
	 * @param username
	 * @param job
	 * @throws JobProcessingException
	 */
	public void processCallbackUrlRequest(Map<String, Object> jobRequestMap, Job job) 
	throws JobProcessingException 
	{
		String defaultNotificationCallback = null;
		if (jobRequestMap.containsKey("callbackUrl")) {
			defaultNotificationCallback = (String)jobRequestMap.get("callbackUrl");
		} else if (jobRequestMap.containsKey("callbackURL")) {
			defaultNotificationCallback = (String)jobRequestMap.get("callbackURL");
		} else if (jobRequestMap.containsKey("notifications")) {
			defaultNotificationCallback = (String)jobRequestMap.get("notifications");
		}
		
		try {
			this.notificationProcessor = new JobRequestNotificationProcessor(username, job);
			
			if (StringUtils.isEmpty(defaultNotificationCallback)) {
				// nothing to do here. continue on
			}
			else {
				if (StringUtils.startsWithAny(defaultNotificationCallback,  new String[]{ "{", "[" })) {
					ObjectMapper mapper = new ObjectMapper();
					this.notificationProcessor.process(mapper.readTree(defaultNotificationCallback));
				}
				else {
					this.notificationProcessor.process(defaultNotificationCallback);
				}
			
				for (Notification n: this.notificationProcessor.getNotifications()) {
	                job.addNotification(n);
	            }
			}
		}
		catch (NotificationException e) {
            throw new JobProcessingException(500, "Failed to assign notification to job", e);
        } catch (IOException e) {
        	throw new JobProcessingException(400, "Unable to parse notification address provided in callbackUrl", e);
		}
	}

	/**
	 * @param jobRequestMap
	 * @param username
	 * @param internalUsername
	 * @param job
	 * @param archiveSystem
	 * @return
	 * @throws JobProcessingException
	 */
	public String processArchivePath(boolean archiveOutput, Map<String, Object> jobRequestMap, RemoteSystem archiveSystem, Job job) 
	throws JobProcessingException 
	{
		String archivePath = null;
		if (archiveOutput) {
			
		    if (jobRequestMap.containsKey("archivePath")) {
		    
    		    archivePath = (String)jobRequestMap.get("archivePath");

    		    if (StringUtils.isNotEmpty(archivePath)) {
    		    	
                    // resolve any macros from the user-supplied archive path into valid values based
                    // on the job request and use those
                    archivePath = JobArchivePathMacroType.resolveMacrosInPath(job, archivePath);

                    createArchivePath(archiveSystem, archivePath);
    			}
		    }
		    else {
		    	
		    }
		}

		if (StringUtils.isEmpty(archivePath)) {
			archivePath = this.username + "/archive/jobs/job-" + job.getUuid();
		}
		
		return archivePath;
	}

	/**
	 * @param archiveSystem
	 * @param archivePath
	 * @throws JobProcessingException
	 */
	public boolean createArchivePath(RemoteSystem archiveSystem, String archivePath) 
	throws JobProcessingException 
	{
		RemoteDataClient remoteDataClient = null;
		try
		{
		    remoteDataClient = archiveSystem.getRemoteDataClient(internalUsername);
		    remoteDataClient.authenticate();

		    LogicalFile logicalFile = LogicalFileDao.findBySystemAndPath(archiveSystem, archivePath);
		    PermissionManager pm = new PermissionManager(archiveSystem, remoteDataClient, logicalFile, username);

		    if (!pm.canWrite(remoteDataClient.resolvePath(archivePath)))
		    {
		        throw new JobProcessingException(403,
		                "User does not have permission to access the provided archive path " + archivePath);
		    }
		    else
		    {
		        if (!remoteDataClient.doesExist(archivePath))
		        {
		            if (!remoteDataClient.mkdirs(archivePath, username)) {
		                throw new JobProcessingException(400,
		                        "Unable to create job archive directory " + archivePath);
		            }
		        }
		        else
		        {
		            if (!remoteDataClient.isDirectory(archivePath))
		            {
		                throw new JobProcessingException(400,
		                        "Archive path is not a folder");
		            }
		        }
		    }
		    
		    return true;
		}
		catch (JobProcessingException e) {
		    throw e;
		}
		catch (RemoteDataException e) {
		    int httpcode = 500;
		    if (e.getMessage().contains("No credentials associated")) {
		        httpcode = 400;
		    }
		    throw new JobProcessingException(httpcode, e.getMessage(), e);
		}
		catch (Throwable e) {
		    throw new JobProcessingException(500, "Could not verify archive path", e);
		}
		finally {
		    try { remoteDataClient.disconnect(); } catch (Exception e) {}
		}
	}

	/**
	 * @param jobRequestMap
	 * @param username
	 * @param job
	 * @param systemManager
	 * @return
	 * @throws JobProcessingException
	 * @throws SystemException
	 */
	protected RemoteSystem processArchiveSystemRequest(boolean archiveOutput, Map<String, Object> jobRequestMap) 
	throws JobProcessingException, SystemException {
		
		RemoteSystem archiveSystem;
		SystemManager systemManager = new SystemManager();

		if (archiveOutput && jobRequestMap.containsKey("archiveSystem"))
	    {
			// lookup the user system
			String archiveSystemId = (String)jobRequestMap.get("archiveSystem");
			archiveSystem = new SystemDao().findUserSystemBySystemId(username, archiveSystemId, RemoteSystemType.STORAGE);
			if (archiveSystem == null) {
				throw new JobProcessingException(400,
						"No storage system found matching " + archiveSystem + " for " + username);
			}
		}
		else
		{
		    // grab the user's default storage system
            archiveSystem = systemManager.getUserDefaultStorageSystem(username);

            if (archiveOutput && archiveSystem == null) {
				throw new JobProcessingException(400,
						"Invalid archiveSystem. No archiveSystem was provided and you "
						+ "have no public or private default storage system configured. "
						+ "Please specify a valid system id for archiveSystem or configure "
						+ "a default storage system.");
			}
		}
		return archiveSystem;
	}

	/**
	 * Should the job output be archived? Default true;
	 * 
	 * @param jobRequestMap
	 * @param job
	 */
	protected boolean processArchiveOutputRequest(Map<String, Object> jobRequestMap) {
		if (jobRequestMap.containsKey("archive") 
				&& StringUtils.isNotEmpty((String)jobRequestMap.get("archive"))) {
			
		    String doArchive = (String)jobRequestMap.get("archive");
		    
		    if (!BooleanUtils.toBoolean(doArchive) && !doArchive.equals("1")) {
			    return false;
			}
		}
		
		return true;
	}

	/**
	 * Can we login to the remote system?
	 * 
	 * @param internalUsername
	 * @param software
	 * @param executionSystem
	 * @throws JobProcessingException
	 */
	public boolean checkExecutionSystemLogin(ExecutionSystem executionSystem)
	throws JobProcessingException {
		AuthConfig authConfig = executionSystem.getLoginConfig().getAuthConfigForInternalUsername(this.internalUsername);
		String salt = executionSystem.getEncryptionKeyForAuthConfig(authConfig);
		if (authConfig.isCredentialExpired(salt))
		{
			throw new JobProcessingException(412,
					(authConfig.isSystemDefault() ? "Default " : "Internal user " + this.internalUsername) +
					" credential for " + executionSystem.getSystemId() + " is not active." +
					" Please add a valid " + executionSystem.getLoginConfig().getType() +
					" execution credential for the execution system and resubmit the job.");
		}

		try
		{
			if (!executionSystem.getRemoteSubmissionClient(internalUsername).canAuthentication()) {
				throw new RemoteExecutionException("Unable to authenticate to " + executionSystem.getSystemId());
			}
		}
		catch (Throwable e)
		{
			throw new JobProcessingException(412,
					"Unable to authenticate to " + executionSystem.getSystemId() + " with the " +
					(authConfig.isSystemDefault() ? "default " : "internal user " + this.internalUsername) +
					"credential. Please check the " + executionSystem.getLoginConfig().getType() +
					" execution credential for the execution system and resubmit the job.");
		}
		
		return true;
	}
	
	/**
	 * @param internalUsername
	 * @param software
	 * @param executionSystem
	 * @throws JobProcessingException
	 */
	public boolean checkExecutionSystemStorage(ExecutionSystem executionSystem)
	throws JobProcessingException {
		AuthConfig authConfig = executionSystem.getStorageConfig().getAuthConfigForInternalUsername(internalUsername);
		String salt = executionSystem.getEncryptionKeyForAuthConfig(authConfig);
		if (authConfig.isCredentialExpired(salt))
		{
			throw new JobProcessingException(412,
					"Credential for " + executionSystem.getSystemId() + " is not active." +
					" Please add a valid " + executionSystem.getStorageConfig().getType() +
					" storage credential for the execution system and resubmit the job.");
		}
		
		RemoteDataClient remoteExecutionDataClient = null;
		try {
			remoteExecutionDataClient = executionSystem.getRemoteDataClient(this.internalUsername);
			remoteExecutionDataClient.authenticate();
		} catch (Throwable e) {
			throw new JobProcessingException(412,
					"Unable to authenticate to " + executionSystem.getSystemId() + " with the " +
					(authConfig.isSystemDefault() ? "default " : "internal user " + this.internalUsername) +
					"credential. Please check the " + executionSystem.getLoginConfig().getType() +
					" execution credential for the execution system and resubmit the job.");
		} finally {
			try { remoteExecutionDataClient.disconnect(); } catch (Exception e) {}
		}
		
		return true;

	}

	/**
	 * @param jobRequestMap
	 * @param jobName
	 * @return
	 * @throws JobProcessingException
	 */
	public Software processSoftwareName(Map<String, Object> jobRequestMap) 
	throws JobProcessingException 
	{
		String softwareName = null;
		if (jobRequestMap.containsKey("appId")) {
			softwareName = (String)jobRequestMap.get("appId");
		} else {
			softwareName = (String)jobRequestMap.get("softwareName");
		}

		if (StringUtils.isEmpty(softwareName)) {
			throw new JobProcessingException(400,
					"appId cannot be empty");
		}
		else if (StringUtils.length(softwareName) > 80) {
			throw new JobProcessingException(400,
					"appId must be less than 80 characters");
		}
		else if (!softwareName.contains("-") || softwareName.endsWith("-"))
		{
			throw new JobProcessingException(400,
					"Invalid appId. " +
					"Please specify an app using its unique id. " +
					"The unique id is defined by the app name " +
					"and version separated by a hyphen. eg. example-1.0");
		}
		
		Software software = SoftwareDao.getSoftwareByUniqueName(softwareName.trim());

		if (software == null) {
			throw new JobProcessingException(400, "No app found matching " + softwareName + " for " + username);
		}
		else if (!isSoftwareInvokableByUser(software, username)) {
			throw new JobProcessingException(403, "Permission denied. You do not have permission to access this app");
		}
		
		return software;
	}
	
	/**
	 * Checks for availability of software for the user requesting the job.
	 * Delegates to {@link ApplicationManager#isInvokableByUser(Software, String)}.
	 * Wrapped here for testability.
	 * 
	 * @param software
	 * @param username
	 * @return
	 */
	public boolean isSoftwareInvokableByUser(Software software, String username) {
		return ApplicationManager.isInvokableByUser(software, username);
	}

	/**
	 * @param jobRequestMap
	 * @return
	 * @throws JobProcessingException
	 */
	public String processNameRequest(Map<String, Object> jobRequestMap)
	throws JobProcessingException 
	{
		String name = null;
		if (jobRequestMap.containsKey("name")) {
			name = (String)jobRequestMap.get("name");
		} else {
			name = (String)jobRequestMap.get("jobName");
		}

		if (StringUtils.isEmpty(name)) {
			throw new JobProcessingException(400,
					"Job name cannot be empty.");
		}
		else if (StringUtils.length(name) > 64) {
			throw new JobProcessingException(400,
					"Job name must be less than 64 characters.");
		}
		else {
			name = name.trim();
		}
		
		return name;
	}

	/**
	 * @param software
	 * @param userNodeCount
	 * @return
	 * @throws JobProcessingException
	 */
	public Long processNodeCountRequest(Software software, String userNodeCount) 
	throws JobProcessingException 
	{
		Long nodeCount;
		if (StringUtils.isEmpty(userNodeCount))
		{
			// use the software default queue if present
			if (software.getDefaultNodes() != null && software.getDefaultNodes() != -1) {
				nodeCount = software.getDefaultNodes();
			}
			else
			{
				// use a single node otherwise
				nodeCount = new Long(1);
			}
		}
		else
		{
			nodeCount = NumberUtils.toLong(userNodeCount);
		}

		if (nodeCount < 1)
		{
			throw new JobProcessingException(400,
					"Invalid " + (StringUtils.isEmpty(userNodeCount) ? "" : "default ") +
					"nodeCount. If specified, nodeCount must be a positive integer value.");
		}
		
//		nodeCount = pTable.containsKey("nodeCount") ? Long.parseLong(pTable.get("nodeCount")) : software.getDefaultNodes();
//		if (nodeCount < 1) {
//			throw new JobProcessingException(400,
//					"Invalid nodeCount value. nodeCount must be a positive integer value.");
//		}
		
		return nodeCount;
	}

	/**
	 * @param jobRequestMap
	 * @param software
	 * @return
	 * @throws JobProcessingException
	 */
	public ExecutionSystem processExecutionSystemRequest(Map<String, Object> jobRequestMap, Software software)
	throws JobProcessingException 
	{	
		String exeSystemName = (String)jobRequestMap.get("executionSystem");
		if (jobRequestMap.containsKey("executionSystem")) {
			exeSystemName = (String)jobRequestMap.get("executionSystem");
		} else {
			exeSystemName = (String)jobRequestMap.get("executionHost");
		}
		
		ExecutionSystem executionSystem = software.getExecutionSystem();
		if (StringUtils.length(exeSystemName) > 80) {
			throw new JobProcessingException(400,
					"executionSystem must be less than 80 characters");
		}
		else if (!StringUtils.isEmpty(exeSystemName) && !StringUtils.equals(exeSystemName, executionSystem.getSystemId())) {
			throw new JobProcessingException(403,
					"Invalid execution system. Apps are registered to run on a specific execution system. If specified, " +
					"the execution system must match the execution system in the app description. The execution system " +
					"for " + software.getName() + " is " + software.getExecutionSystem().getSystemId() + ".");
		}
		return executionSystem;
	}

	/**
	 * @return the queueProcessor
	 */
	public JobRequestQueueProcessor getQueueProcessor() {
		return queueProcessor;
	}

	/**
	 * @param queueProcessor the queueProcessor to set
	 */
	public void setQueueProcessor(JobRequestQueueProcessor queueProcessor) {
		this.queueProcessor = queueProcessor;
	}

	/**
	 * @return the inputProcessor
	 */
	public JobRequestInputProcessor getInputProcessor() {
		if (this.inputProcessor == null) {
			this.inputProcessor = new JobRequestInputProcessor(username, internalUsername, getSoftware());
		}
		return inputProcessor;
	}

	/**
	 * @param inputProcessor the inputProcessor to set
	 */
	public void setInputProcessor(JobRequestInputProcessor inputProcessor) {
		this.inputProcessor = inputProcessor;
	}

	/**
	 * @return the parameterProcessor
	 */
	public JobRequestParameterProcessor getParameterProcessor() {
		if (this.parameterProcessor == null) {
			this.parameterProcessor = new JobRequestParameterProcessor(getSoftware());
		}
		return this.parameterProcessor;
	}

	/**
	 * @param parameterProcessor the parameterProcessor to set
	 */
	public void setParameterProcessor(
			JobRequestParameterProcessor parameterProcessor) {
		this.parameterProcessor = parameterProcessor;
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return the internalUsername
	 */
	public String getInternalUsername() {
		return internalUsername;
	}

	/**
	 * @param internalUsername the internalUsername to set
	 */
	public void setInternalUsername(String internalUsername) {
		this.internalUsername = internalUsername;
	}

	/**
	 * @return the notificationProcessor
	 */
	public JobRequestNotificationProcessor getNotificationProcessor() {
		return notificationProcessor;
	}

	/**
	 * @param notificationProcessor the notificationProcessor to set
	 */
	public void setNotificationProcessor(
			JobRequestNotificationProcessor notificationProcessor) {
		this.notificationProcessor = notificationProcessor;
	}

	/**
	 * @return the executionSystem
	 */
	public ExecutionSystem getExecutionSystem() {
		return executionSystem;
	}

	/**
	 * @param executionSystem the executionSystem to set
	 */
	public void setExecutionSystem(ExecutionSystem executionSystem) {
		this.executionSystem = executionSystem;
	}

	/**
	 * @return the software
	 */
	public Software getSoftware() {
		return software;
	}

	/**
	 * @param software the software to set
	 */
	public void setSoftware(Software software) {
		this.software = software;
	}
}
