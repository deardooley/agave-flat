/**
 * 
 */
package org.iplantc.service.jobs.model;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ParamDef;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uri.UrlPathEscaper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.exceptions.JobEventProcessingException;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.managers.JobEventProcessor;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.util.ServiceUtils;
import org.iplantc.service.jobs.util.Slug;
import org.iplantc.service.notification.dao.NotificationDao;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.Notification;
import org.iplantc.service.systems.model.BatchQueue;
import org.iplantc.service.systems.model.RemoteSystem;
import org.joda.time.DateTime;
import org.json.JSONException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author dooley
 * 
 */
@Entity
@Table(name = "jobs")
@FilterDef(name="jobTenantFilter", parameters=@ParamDef(name="tenantId", type="string"))
@Filters(@Filter(name="jobTenantFilter", condition="tenant_id=:tenantId"))
public class Job {
	
	private static final Logger log = Logger.getLogger(Job.class);
	
	private Long				id;
	private String				uuid; 			// Unique id not based on database id
	private String				name;			// Human-readable name for this
												// job. Defaults to $application
	private String				owner;			// username of the user
												// submitting the job
	private String				internalUsername; // Username of InternalUser tied
												// tied to this job.
	private String				system;			// System on which this job ran.
												// Type is specified by the
												// software
	private String				softwareName;	// Unique name of an application
												// as given in the /apps/list
	private Long				nodeCount;		// Requested nodes to use
	private Long				processorsPerNode; // Requested processors per node
	private Double				memoryPerNode;	// Requested memory per node
	private String				batchQueue;		// Queue used to run the job
	private String				maxRunTime;		// Requested time needed to run the job in 00:00:00 format
	private String				outputPath;		// relative path of the job's output folder
	private boolean				archiveOutput;	// Boolean. If 'true' stage
												// job/output to $IPLANTHOME/job
	private String				archivePath;	// Override default location for
												// archiving job output
	private RemoteSystem		archiveSystem;	// System on which to archive the output.
												// Uses iPlant Data Store by default.
	private String				workPath;		// Override default location for
												// archiving job output
	private JobStatusType		status = JobStatusType.PENDING;			// Enumerated status of the job
	private Integer				statusChecks = 0; // number of times this job has been actively checked. 
												// used in exponential backoff
	private String				updateToken;	// Token needed to validate all
												// callbacks to update job
												// status
	private String				inputs;			// JSON encoded list of inputs
	private String				parameters;		// JSON encoded list of
												// parameters
	private String				localJobId;		// Local job or process id of
												// the job on the remote system
	private String				schedulerJobId; // Optional Unique job id given
												// by the scheduler if used.
	private Float				charge;			// Charge for this job against
												// the user's allocation
	private Date				submitTime;		// Date and time job was
												// submitted to the remote
												// system
	private Date				startTime;		// Date and time job was started
	private Date				endTime;		// Date and time job completed
	private String				errorMessage;	// Error message of this job
												// execution
	private Date				lastUpdated;	// Last date and time job status
												// was updated
	private Date				created;		// Date job request was
												// submitted
	private Integer				retries;		// Number of attempts to resubmit this job
	private boolean				visible;		// Can a user see the job?
	private Integer				version = 0;	// Entity version used for optimistic locking
	private String				tenantId;		// current api tenant
	private List<JobEvent>		events = new ArrayList<JobEvent>(); // complete history of events for this job
	
//	private Set<Notification>	notifications = new HashSet<Notification>(); // all notifications registered to this job
	
	public Job()
	{
		created = new DateTime().toDate();
		lastUpdated = created;
		updateToken = UUID.randomUUID().toString();
		nodeCount = new Long(1);
		processorsPerNode = new Long(1);
		memoryPerNode = new Double(1);
		maxRunTime = BatchQueue.DEFAULT_MAX_RUN_TIME;
		retries = 0;
		archiveOutput = true;
		visible = true;
		
		tenantId = TenancyHelper.getCurrentTenantId();
		uuid = new AgaveUUID(UUIDType.JOB).toString();
	}

	/**
	 * @return the id
	 */
	@Id
	@GeneratedValue
	@Column(name = "id", unique = true, nullable = false)
	public Long getId()
	{
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(Long id)
	{
		this.id = id;
	}

	/**
	 * @return the uuid
	 */
	@Column(name = "uuid", nullable = false, length = 64, unique = true)
	public String getUuid()
	{
		return uuid;
	}

	/**
	 * @param uuid the uuid to set
	 */
	public void setUuid(String uuid)
	{
		this.uuid = uuid;
	}
	
	/**
	 * @return the name
	 */
	@Column(name = "name", nullable = false, length = 64)
	public String getName()
	{
		return name;
	}

	/**
	 * @param name the name to set
	 * @throws JobException 
	 */
	public void setName(String name) throws JobException
	{
		if (!StringUtils.isEmpty(name) && name.length() > 64) {
			throw new JobException("'job.name' must be less than 64 characters");
		}
		
		this.name = name;
	}

	/**
	 * @return the owner
	 */
	@Column(name = "owner", nullable = false, length = 32)
	public String getOwner()
	{
		return owner;
	}

	/**
	 * @param owner
	 *            the owner to set
	 * @throws JobException 
	 */
	public void setOwner(String owner) throws JobException
	{
		if (!StringUtils.isEmpty(owner) && owner.length() > 32) {
			throw new JobException("'job.owner' must be less than 32 characters");
		}
		
		this.owner = owner;
	}

	/**
	 * @return the internalUsername
	 */
	@Column(name = "internal_username", nullable = true, length = 32)
	public String getInternalUsername()
	{
		return internalUsername;
	}

	/**
	 * @param internalUsername the internalUsername to set
	 * @throws JobException 
	 */
	public void setInternalUsername(String internalUsername) throws JobException
	{
		if (!StringUtils.isEmpty(internalUsername) && internalUsername.length() > 32) {
			throw new JobException("'job.internalUsername' must be less than 32 characters");
		}
		
		this.internalUsername = internalUsername;
	}

	/**
	 * @return the system
	 */
	@Column(name = "execution_system", nullable = false, length = 64)
	public String getSystem()
	{
		return system;
	}

	/**
	 * @param system
	 *            the system to set
	 * @throws JobException 
	 */
	public void setSystem(String system) throws JobException
	{
		if (!StringUtils.isEmpty(system) && system.length() > 64) {
			throw new JobException("'job.system' must be less than 64 characters");
		}
		
		this.system = system;
	}

	/**
	 * @return the softwareName
	 */
	@Column(name = "software_name", nullable = false, length = 80)
	public String getSoftwareName()
	{
		return softwareName;
	}

	/**
	 * @param softwareName
	 *            the softwareName to set
	 * @throws JobException 
	 */
	public void setSoftwareName(String softwareName) throws JobException
	{
		if (!StringUtils.isEmpty(softwareName) && softwareName.length() > 80) {
			throw new JobException("'job.software' must be less than 80 characters");
		}
		
		this.softwareName = softwareName;
	}
	
	/**
	 * @return the batchQueue
	 */
	@Column(name = "queue_request", nullable = false, length = 128)
	public String getBatchQueue()
	{
		return batchQueue;
	}

	/**
	 * @param queueRequest
	 *            the queueRequest to set
	 * @throws JobException 
	 */
	public void setBatchQueue(String queueRequest) throws JobException
	{
		if (!StringUtils.isEmpty(queueRequest) && queueRequest.length() > 128) {
			throw new JobException("'job.batchQueue' must be less than 128 characters");
		}
		
		this.batchQueue = queueRequest;
	}
	
	/**
	 * @return the nodeCount
	 */
	@Column(name = "node_count", nullable = false)
	public Long getNodeCount()
	{
		return nodeCount;
	}

	/**
	 * @param nodeCount
	 *            the nodeCount to set
	 */
	public void setNodeCount(Long nodeCount) throws JobException
	{
		if (nodeCount == null || nodeCount < 1) {
			throw new JobException("'job.nodeCount' must be a positive integer value");
		}
		
		this.nodeCount = nodeCount;
	}

	/**
	 * @return the processorsPerNode
	 */
	@Column(name = "processor_count", nullable = false)
	public Long getProcessorsPerNode()
	{
		return processorsPerNode;
	}

	/**
	 * @param processorCount
	 *            the processorCount to set
	 */
	public void setProcessorsPerNode(Long processorsPerNode) throws JobException
	{
		if (processorsPerNode == null || processorsPerNode < 1) {
			throw new JobException("'job.processorsPerNode' must be a positive integer value");
		}
		
		this.processorsPerNode = processorsPerNode;
	}

	/**
	 * @return the memoryPerNode
	 */
	@Column(name = "memory_request", nullable = false)
	public Double getMemoryPerNode()
	{
		return memoryPerNode;
	}

	/**
	 * @param memoryPerNode
	 *            the memoryPerNode to set
	 */
	public void setMemoryPerNode(Double memoryPerNode)
	{
		this.memoryPerNode = memoryPerNode;
	}

//	/**
//	 * @return the callbackUrl
//	 */
//	@Column(name = "callback_url", nullable = true, length = 255)
//	public String getCallbackUrl()
//	{
//		return callbackUrl;
//	}
//
//	/**
//	 * @param callbackUrl
//	 *            the callbackUrl to set
//	 * @throws JobException 
//	 */
//	public void setCallbackUrl(String callbackUrl) throws JobException
//	{
//		if (!StringUtils.isEmpty(callbackUrl) && callbackUrl.length() > 255) {
//			throw new JobException("'job.callbackUrl' must be less than 255 characters");
//		}
//		
//		this.callbackUrl = callbackUrl;
//	}

	/**
	 * @param outputPath the outputPath to set
	 * @throws JobException 
	 */
	public void setOutputPath(String outputPath) throws JobException
	{
		if (!StringUtils.isEmpty(outputPath) && outputPath.length() > 255) {
			throw new JobException("'job.outputPath' must be less than 255 characters");
		}
		
		this.outputPath = outputPath;
	}

	/**
	 * @return the outputPath
	 */
	@Column(name = "output_path", nullable = true, length = 255)
	public String getOutputPath()
	{
		return outputPath;
	}

	/**
	 * @return the archiveOutput
	 */
	@Column(name = "archive_output", columnDefinition = "TINYINT(1)")
	public Boolean isArchiveOutput()
	{
		return archiveOutput;
	}

	/**
	 * @param archiveOutput
	 *            the archiveOutput to set
	 */
	public void setArchiveOutput(Boolean archiveOutput)
	{
		this.archiveOutput = archiveOutput;
	}

	/**
	 * @return the archivePath
	 */
	@Column(name = "archive_path", nullable = true, length = 255)
	public String getArchivePath()
	{
		return archivePath;
	}

	/**
	 * @param archivePath
	 *            the archivePath to set
	 * @throws JobException 
	 */
	public void setArchivePath(String archivePath) throws JobException
	{
		if (!StringUtils.isEmpty(archivePath) && archivePath.length() > 255) {
			throw new JobException("'job.archivePath' must be less than 255 characters");
		}
		
		this.archivePath = archivePath;
	}

	/**
	 * @return the archiveSystem
	 */
	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "archive_system", referencedColumnName = "id")
    public RemoteSystem getArchiveSystem()
	{
		return archiveSystem;
	}

	/**
	 * @param archiveSystem the archiveSystem to set
	 */
	public void setArchiveSystem(RemoteSystem archiveSystem)
	{
		this.archiveSystem = archiveSystem;
	}

	/**
	 * @return the workPath
	 */
	@Column(name = "work_path", nullable = true, length = 255)
	public String getWorkPath()
	{
		return workPath;
	}

	/**
	 * @param workPath
	 *            the workPath to set
	 * @throws JobException 
	 */
	public void setWorkPath(String workPath) throws JobException
	{
		if (!StringUtils.isEmpty(workPath) && workPath.length() > 255) {
			throw new JobException("'job.workPath' must be less than 255 characters");
		}
		
		this.workPath = workPath;
	}

	/**
	 * @return the status
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	public JobStatusType getStatus()
	{
		return status;
	}
	
	/**
	 * Returns number of times this job's status has been checked
	 * by the monitoring tasks. This is used to calculate the exponential
	 * backoff used to throttle status checks on long-running jobs.
	 * 
	 * @return Integer
	 */
	@Column(name = "status_checks", nullable = false)
	public Integer getStatusChecks() {
		return statusChecks;
	}

	/**
	 * @param statusChecks
	 */
	public void setStatusChecks(Integer statusChecks) {
		this.statusChecks = statusChecks;
	}

	/**
	 * Returns a list of job events in the history of this job.
	 * 
	 * @return
	 */
	@OneToMany(cascade = {CascadeType.ALL}, mappedBy = "job", fetch=FetchType.EAGER, orphanRemoval=true)
	public List<JobEvent> getEvents() {
		return events;
	}
	
	/**
	 * @param events
	 */
	public void setEvents(List<JobEvent> events) {
		this.events = events;
	}
	
	/**
	 * Adds an event to the history of this job. This will automatically
	 * be saved with the job when the job is persisted.
	 * 
	 * @param event
	 */
	public void addEvent(JobEvent event) {
		event.setJob(this);
		this.events.add(event);
		JobEventProcessor jep;
        try {
            jep = new JobEventProcessor(event);
            jep.process();
        } catch (JobEventProcessingException e) {
            e.printStackTrace();
        }
	}
	
	/**
	 * Convenience endpoint to add a notification to this job. Notification.persistent
	 * defaults to false. 
	 * 
	 * @param event String notification event that will trigger the callback
	 * @param callbackUrl A URL or email address that will be triggered by the event
	 */
	public void addNotification(String event, String callback) throws NotificationException 
	{
		addNotification(event, callback, false);
	}
	
	/**
	 * Convenience endpoint to add a notification to this job. 
	 * 
	 * @param event String notification event that will trigger the callback
	 * @param callbackUrl A URL or email address that will be triggered by the event
	 * @param persistent Whether this notification should expire after the first successful trigger
	 */
	@Transient
	public void addNotification(String event, String callback, boolean persistent) 
	throws NotificationException 
	{
		Notification notification = new Notification(event, callback);
		notification.setOwner(owner);
		notification.setAssociatedUuid(uuid);
		notification.setPersistent(persistent);
		
		addNotification(notification);
	}
	
	/**
	 * Convenience endpoint to add a notification to this job. 
	 * 
	 * @param notification A notification event to associate with this job. The current
	 * jobs owner and uuid will be added to the notification.
	 */
	@Transient
	public void addNotification(Notification notification) throws NotificationException 
	{
		notification.setOwner(owner);
		notification.setAssociatedUuid(uuid);
		new NotificationDao().persist(notification);
	}
	
	/**
	 * Sets the job status and creates an job history event with 
	 * the given status and message;
	 * 
	 * @param status
	 * @param message
	 */
	@Transient
	public void setStatus(JobStatusType status, String message) throws JobException
	{
		setStatus(status, new JobEvent(this, status, message, getOwner()));
//		// avoid adding duplicate entries over and over from watch 
//		// and monitoring queue updates.
//		if (!this.status.equals(status) || !StringUtils.equals(getErrorMessage(), message)) {
//			// we don't want the job status being updated after the job is deleted as we
//			// already move it to a terminal state when it's deleted. Here we check for 
//			// job deletion and, then if visible, propagate the event. Otherwise, we 
//			// simply add it to the history for reference and move on.
//			if (this.isVisible()) {
//				setStatus(status);
//				setErrorMessage(message);
//				addEvent(new JobEvent(status, message, getOwner()));
//			}
//			else {
//				message += " Event will be ignored because job has been deleted.";
//				this.events.add(new JobEvent(this, status, message, getOwner()));
//			}
//		} else {
////			log.debug("Ignoring status update to " + status + " with same message");
//		}
	}
	
	/**
	 * Sets the job status and associates the job history event with 
	 * the job;
	 * 
	 * @param status
	 * @param message
	 */
	@Transient
	public void setStatus(JobStatusType status, JobEvent event) throws JobException
	{
		// avoid adding duplicate entries over and over from watch 
		// and monitoring queue updates.
		if (!this.status.equals(status) || !StringUtils.equals(getErrorMessage(), event.getDescription())) {
			// we don't want the job status being updated after the job is deleted as we
			// already move it to a terminal state when it's deleted. Here we check for 
			// job deletion and, then if visible, propagate the event. Otherwise, we 
			// simply add it to the history for reference and move on.
			if (this.isVisible()) {
				setStatus(status);
				setErrorMessage(event.getDescription());
				addEvent(event);
			}
			else {
				event.setDescription(event.getDescription() + " Event will be ignored because job has been deleted.");
				this.events.add(event);
			}
		} else {
//			log.debug("Ignoring status update to " + status + " with same message");
		}
	}
	
	/**
	 * @param status
	 *            the status to set
	 */
	private void setStatus(JobStatusType status)
	{
		this.status = status;
	}

	/**
	 * @return the updateToken
	 */
	@Column(name = "update_token", nullable = true, length = 64)
	public String getUpdateToken()
	{
		return updateToken;
	}

	/**
	 * @param updateToken
	 *            the updateToken to set
	 * @throws JobException 
	 */
	public void setUpdateToken(String updateToken) throws JobException
	{
		if (!StringUtils.isEmpty(updateToken) && updateToken.length() > 64) {
			throw new JobException("'job.updateToken' must be less than 64 characters");
		}
		
		this.updateToken = updateToken;
	}

	/**
	 * @return the inputs
	 */
	@Column(name = "inputs", nullable = true, columnDefinition = "TEXT")
	public String getInputs()
	{
		return inputs;
	}

//	public void setInputsAsMap(Map<String, String> map) throws JobException
//	{
//		try 
//		{
//			ObjectMapper mapper = new ObjectMapper();
//			ObjectNode json = mapper.createObjectNode();
//			for (String paramKey : map.keySet())
//			{
//				json.put(paramKey, StringUtils.trim(map.get(paramKey).toString()));
//			}
//			inputs = json.toString();
//		}
//		catch (Exception e) {
//			throw new JobException("Failed to parse job parameters", e);
//		}
//	}

//	@Transient
//	public Map<String, String> getInputsAsMap() throws JobException
//	{
//		try {
//			Map<String, String> map = new HashMap<String, String>();
//			if (!ServiceUtils.isValid(inputs))
//				return map;
//			ObjectMapper mapper = new ObjectMapper();
//			JsonNode json = mapper.readTree(inputs);
//			for (Iterator<String> inputIterator = json.fieldNames(); inputIterator.hasNext();)
//			{
//				String inputKey = inputIterator.next();
//				map.put(inputKey, StringUtils.trim(json.get(inputKey).textValue()));
//			}
//			return map;
//		}
//		catch (Exception e) {
//			throw new JobException("Failed to parse job inputs", e);
//		}
//	}
	
	@Transient
	public JsonNode getInputsAsJsonObject() throws JobException
	{
		try 
		{
			ObjectMapper mapper = new ObjectMapper();
			
			if (StringUtils.isEmpty(getInputs())) {
				return mapper.createObjectNode();
			}
			else
			{
				return mapper.readTree(getInputs());
			}
		}
		catch (Exception e) {
			throw new JobException("Failed to parse job inputs", e);
		}
	}
	
	@Transient
	public void setInputsAsJsonObject(JsonNode json) throws JobException
	{
		if (json == null || json.isNull() || json.size() == 0) {
			setInputs(null);
		} else {
			setInputs(json.toString());
		}
	}

	/**
	 * @param inputs
	 *            the inputs to set
	 * @throws JobException 
	 */
	private void setInputs(String inputs) throws JobException
	{
		if (!StringUtils.isEmpty(inputs) && inputs.length() > 16384) {
			throw new JobException("'job.inputs' must be less than 16384 characters");
		}
		
		this.inputs = inputs;
	}

	/**
	 * @return the parameters
	 */
	@Column(name = "parameters", nullable = true, columnDefinition = "TEXT")
	public String getParameters()
	{
		return parameters;
	}
	
	@Transient
	public JsonNode getParametersAsJsonObject() throws JobException
	{
		try 
		{
			ObjectMapper mapper = new ObjectMapper();
			if (StringUtils.isEmpty(getParameters()))
			{
				return mapper.createObjectNode();
			}
			else
			{
				return mapper.readTree(getParameters());
			}
		}
		catch (Exception e) {
			throw new JobException("Failed to parse job parameters", e);
		}
	}
	
	@Transient
	public void setParametersAsJsonObject(JsonNode json) throws JobException
	{
		if (json == null || json.isNull() || json.size() == 0) {
			setParameters(null);
		} else {
			setParameters(json.toString());
		}
	}

//	@Transient
//	@Deprecated
//	public Map<String, Object> getParametersAsMap() throws JobException
//	{
//		try 
//		{	
//			Map<String, Object> map = new HashMap<String, Object>();
//			JsonNode json = getParametersAsJsonObject();
//			for (Iterator<String> parameterIterator = json.fieldNames(); parameterIterator.hasNext();)
//			{
//				String parameterKey = parameterIterator.next();
//				map.put(parameterKey, json.get(parameterKey).textValue());
//			}
//			return map;
//		}
//		catch (Exception e) {
//			throw new JobException("Failed to parse job parameters", e);
//		}
//	}
//
//	@Deprecated
//	public void setParametersAsMap(Map<String, Object> map)
//	throws JobException
//	{
//		try 
//		{
//			ObjectMapper mapper = new ObjectMapper();
//			ObjectNode json = mapper.createObjectNode();
//			for (String paramKey : map.keySet())
//			{
//				json.put(paramKey, map.get(paramKey).toString());
//			}
//			parameters = json.toString();
//		}
//		catch (Exception e) {
//			throw new JobException("Failed to parse job parameters", e);
//		}
//	}
	
//	public void setParametersAsJsonNode(JsonNode json)
//	throws JobException
//	{
//		try 
//		{
//			parameters = json.toString();
//		}
//		catch (Exception e) {
//			throw new JobException("Failed to parse job parameters", e);
//		}
//	}

	/**
	 * @param parameters
	 *            the parameters to set
	 * @throws JobException 
	 */
	private void setParameters(String parameters) throws JobException
	{
		if (!StringUtils.isEmpty(parameters) && parameters.length() > 16384) {
			throw new JobException("'job.parameters' must be less than 16384 characters");
		}
		
		this.parameters = parameters;
	}

	/**
	 * @return the localJobId
	 */
	@Column(name = "local_job_id", nullable = true, length = 255)
	public String getLocalJobId()
	{
		return localJobId;
	}
	
	@Transient
	public String getNumericLocalJobId() {
		String numericLocalJobId = null;
		String[] tokens = StringUtils.split(getLocalJobId(), ".");
		if (tokens != null && tokens.length > 0) {
			for (String token: tokens) {
				if (NumberUtils.isDigits(token)) {
					numericLocalJobId = token; 
					break;
				}
			}
		}
		
		return numericLocalJobId;
	}

	/**
	 * @param localJobId
	 *            the localJobId to set
	 * @throws JobException 
	 */
	public void setLocalJobId(String localJobId) throws JobException
	{
		if (!StringUtils.isEmpty(localJobId) && localJobId.length() > 255) {
			throw new JobException("'job.localId' must be less than 255 characters");
		}
		
		this.localJobId = localJobId;
	}

	/**
	 * @return the schedulerJobId
	 */
	@Column(name = "scheduler_job_id", nullable = true, length = 255)
	public String getSchedulerJobId()
	{
		return schedulerJobId;
	}

	/**
	 * @param schedulerJobId
	 *            the schedulerJobId to set
	 * @throws JobException 
	 */
	public void setSchedulerJobId(String schedulerJobId) throws JobException
	{
		if (!StringUtils.isEmpty(schedulerJobId) && schedulerJobId.length() > 255) {
			throw new JobException("'job.schedulerJobId' must be less than 255 characters");
		}
		
		this.schedulerJobId = schedulerJobId;
	}

	/**
	 * @return the charge
	 */
	@Column(name = "charge")
	public Float getCharge()
	{
		return charge;
	}

	/**
	 * @param charge
	 *            the charge to set
	 */
	public void setCharge(Float charge)
	{
		this.charge = charge;
	}

	/**
	 * @return the submitTime
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "submit_time", nullable = true, length = 19)
	public Date getSubmitTime()
	{
		return submitTime;
	}

	/**
	 * @param submitTime
	 *            the submitTime to set
	 */
	public void setSubmitTime(Date submitTime)
	{
		this.submitTime = submitTime;
	}

	/**
	 * @param maxRunTime the maxRunTime to set
	 */
	public void setMaxRunTime(String maxRunTime)
	{
		this.maxRunTime = maxRunTime;
	}

	/**
	 * @return the maxRunTime
	 */
	@Column(name = "requested_time", nullable = true, length = 19)
	public String getMaxRunTime()
	{
		return maxRunTime;
	}

	/**
	 * @return the startTime
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "start_time", nullable = true, length = 19)
	public Date getStartTime()
	{
		return startTime;
	}

	/**
	 * @param startTime
	 *            the startTime to set
	 */
	public void setStartTime(Date startTime)
	{
		this.startTime = startTime;
	}

	/**
	 * @return the endTime
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "end_time", nullable = true, length = 19)
	public Date getEndTime()
	{
		return endTime;
	}

	/**
	 * @param endTime
	 *            the endTime to set
	 */
	public void setEndTime(Date endTime)
	{
		this.endTime = endTime;
	}

	/**
	 * @param errorMessage
	 *            the errorMessage to set
	 * @throws JobException 
	 */
	public void setErrorMessage(String errorMessage) throws JobException
	{
		if (!StringUtils.isEmpty(errorMessage) && errorMessage.length() > 16384) {
			throw new JobException("'job.message' must be less than 16384 characters");
		}
		
		this.errorMessage = errorMessage;
	}

	/**
	 * @return the errorMessage
	 */
	@Column(name = "error_message", nullable = true, length = 4096)
	public String getErrorMessage()
	{
		return errorMessage;
	}

	/**
	 * @return the lastUpdated
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "last_updated", nullable = false, length = 19)
	public Date getLastUpdated()
	{
		return lastUpdated;
	}

	/**
	 * @param lastUpdated
	 *            the lastUpdated to set
	 */
	public void setLastUpdated(Date lastUpdated)
	{
		this.lastUpdated = lastUpdated;
	}

	/**
	 * @param created
	 *            the created to set
	 */
	public void setCreated(Date created)
	{
		this.created = created;
	}

	/**
	 * @return the created
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created", nullable = false, length = 19, columnDefinition="DATETIME")
	public Date getCreated()
	{
		return created;
	}

	/**
	 * @param visible the visible to set
	 */
	public void setVisible(Boolean visible)
	{
		this.visible = visible;
	}

	/**
	 * @return the visible
	 */
	@Column(name = "visible", columnDefinition = "TINYINT(1)")
	public Boolean isVisible()
	{
		return visible;
	}
	
	/**
	 * @return the retries
	 */
	@Column(name = "retries")
	public Integer getRetries()
	{
		return retries;
	}

	/**
	 * @param retries the retries to set
	 */
	public void setRetries(Integer retries)
	{
		this.retries = retries;
	}
	
	/**
	 * @return the version
	 */
	@Version
    @Column(name="OPTLOCK")
    public Integer getVersion() {
		return version;
	}
	
	/**
	 * @param version the current version
	 */
	public void setVersion(Integer version) {
		this.version = version;
	}

	/**
	 * @return the tenantId
	 */
	@Column(name = "tenant_id", nullable=false, length = 128)
	public String getTenantId()
	{
		return tenantId;
	}

	/**
	 * @param tenantId the tenantId to set
	 */
	public void setTenantId(String tenantId)
	{
		this.tenantId = tenantId;
	}

	@Transient
	public String getArchiveUrl() 
	{
		if (isFinished() && archiveOutput) {
			return TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_IO_SERVICE, getTenantId()) + 
					"listings/system/" + getArchiveSystem().getSystemId() + "/" + UrlPathEscaper.escape(getArchivePath());
		} else {
			return TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE, getTenantId()) + 
					uuid + "/outputs/listings";
		}
	}
	
	@Transient
	public String getArchiveCanonicalUrl() 
	{
		if (isFinished() && archiveOutput) {
			return TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_IO_SERVICE, getTenantId()) + 
					"listings/system/" + getArchiveSystem().getSystemId() + "/" + UrlPathEscaper.escape(getArchivePath());
		} else {
			return TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_IO_SERVICE, getTenantId()) + 
					"listings/system/" + getSystem() + "/" + UrlPathEscaper.escape(getWorkPath());
		}
	}

	@Transient
	public boolean isFinished()
	{
		return JobStatusType.isFinished(status);
	}

	@Transient
	public boolean isSubmitting()
	{
		return JobStatusType.isSubmitting(status);
	}

	@Transient
	public boolean isRunning()
	{
		return JobStatusType.isRunning(status);
	}

	@Transient
	public boolean isArchived()
	{
		return JobStatusType.isArchived(status);
	}
	
	@Transient
	public boolean isFailed()
	{
		return JobStatusType.isFailed(status);
	}
	
	@Transient
	public boolean equals(Object o)
	{
		if (o instanceof Job)
		{
			return ( name.equals( ( (Job) o ).name)
					&& owner.equals( ( (Job) o ).owner)
					&& updateToken.equals( ( (Job) o ).updateToken) 
					&& softwareName.equals( ( (Job) o ).softwareName) );
		}
		else
		{
			return false;
		}
	}

	@Transient
	@JsonProperty("_links")
	private ObjectNode getHypermedia() {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode linksObject = mapper.createObjectNode();
		linksObject.put("self", (ObjectNode)mapper.createObjectNode()
    		.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE, getTenantId()) + getUuid()));
		linksObject.put("app", mapper.createObjectNode()
    		.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_APPS_SERVICE, getTenantId()) + getSoftwareName()));
		linksObject.put("executionSystem", mapper.createObjectNode()
    		.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_SYSTEM_SERVICE, getTenantId()) + getSystem()));
		linksObject.put("archiveSystem", mapper.createObjectNode()
        		.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_SYSTEM_SERVICE, getTenantId()) + 
        				(isArchiveOutput() ? getArchiveSystem().getSystemId() : getSystem())));
		
		linksObject.put("archiveData", mapper.createObjectNode()
    		.put("href", getArchiveUrl()));
    	
		linksObject.put("owner", mapper.createObjectNode()
			.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE, getTenantId()) + owner));
		linksObject.put("permissions", mapper.createObjectNode()
    		.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE, getTenantId()) + uuid + "/pems"));
        linksObject.put("history", mapper.createObjectNode()
			.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE, getTenantId()) + uuid + "/history"));
	    linksObject.put("metadata", mapper.createObjectNode()
			.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_METADATA_SERVICE, getTenantId()) + "data/?q=" + URLEncoder.encode("{\"associationIds\":\"" + uuid + "\"}")));
		linksObject.put("notifications", mapper.createObjectNode()
			.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_NOTIFICATION_SERVICE, getTenantId()) + "?associatedUuid=" + uuid));
		
    	if (!StringUtils.isEmpty(internalUsername)) {
    		linksObject.put("internalUser", mapper.createObjectNode()
    			.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE, getTenantId()) + getOwner() + "/users/" + internalUsername));
    	}
    	
    	return linksObject;
	}
	
	@JsonValue
	public String toJSON() throws JsonProcessingException, IOException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode json = mapper.createObjectNode()
			.put("id", uuid)
			.put("name", name)
			.put("owner", owner)
			.put("appId", softwareName)
			.put("executionSystem", system)
			.put("batchQueue", batchQueue)
			.put("nodeCount", nodeCount)
			.put("processorsPerNode", processorsPerNode)
			.put("memoryPerNode", memoryPerNode)
			.put("maxRunTime", maxRunTime)
			.put("archive", archiveOutput)
			.put("retries", retries)
			.put("localId", localJobId)
			.put("created", new DateTime(created).toString());
			
		if (archiveOutput)
		{
			json.put("archivePath", getArchivePath());
			json.put("archiveSystem", getArchiveSystem().getSystemId());
		}
		
		json.put("outputPath", getWorkPath());
		
		json.put("status", status.name());

		if (status.equals(JobStatusType.FAILED))
		{
			json.put("message", getErrorMessage());
		}

		json.put("submitTime", getSubmitTime() == null ? null : new DateTime(getSubmitTime()).toString());
		json.put("startTime", getStartTime() == null ? null : new DateTime(getStartTime()).toString());
		json.put("endTime", getEndTime() == null ? null : new DateTime(getEndTime()).toString());
//		json.put("workPath", getWorkPath());
		try {
			json.set("inputs", getInputsAsJsonObject());
			json.set("parameters", getParametersAsJsonObject());
		} catch (JobException e) {
			throw new IOException(e.getMessage(), e);
		}
		
    	json.set("_links", getHypermedia());
    	
		return json.toString();
	}

	public Job copy() throws JSONException, JobException
	{
		Job job = new Job();
		job.setName(name);
		job.setOwner(owner);
		job.status = JobStatusType.PENDING;
		job.errorMessage = "Job resumitted for execution from job " + getUuid();
		job.setSoftwareName(softwareName);
		job.setSystem(system);
		job.setNodeCount(nodeCount);
		job.setBatchQueue(batchQueue);
		job.setProcessorsPerNode(processorsPerNode);
		job.setMemoryPerNode(memoryPerNode);
		job.setInputs(getInputs());
		job.setParameters(getParameters());
		job.setMaxRunTime(maxRunTime);
		job.setSubmitTime(new DateTime().toDate());
		
		job.setArchiveOutput(isArchiveOutput());
		job.setArchiveSystem(getArchiveSystem());
		
		if (StringUtils.isEmpty(getArchivePath())) {
			job.setArchivePath(getOwner() + "/archive/jobs/job-" + job.getUuid() + "-" + Slug.toSlug(getName()));
		}
		else		
		{
			if (StringUtils.contains(getArchivePath(), getUuid())) {
				job.setArchivePath(StringUtils.replaceChars(getArchivePath(), getUuid(), job.getUuid()));
			} else {
				job.setArchivePath(getOwner() + "/archive/jobs/job-" + job.getUuid() + "-" + Slug.toSlug(getName()));
			}
		}
		
		return job;
	}

	@Transient
	public Object getValueForAttributeName(String attribute) throws JobException, JSONException
	{
		Object value = null;
		
		if (attribute.equalsIgnoreCase("id")) {
			value = id;
		} else if (attribute.equalsIgnoreCase("name")) {
			value = name;
		} else if (attribute.equalsIgnoreCase("appId")) {
			value = softwareName;
		} else if (attribute.equalsIgnoreCase("owner")) {
			value = owner;
		} else if (attribute.equalsIgnoreCase("system")) {
			value = system;
		} else if (attribute.equalsIgnoreCase("software")) {
			value = softwareName;
		} else if (attribute.equalsIgnoreCase("batchQueue")) {
			value = batchQueue;
		} else if (attribute.equalsIgnoreCase("processorsPerNode")) {
			value = processorsPerNode;
		} else if (attribute.equalsIgnoreCase("requestedTime")) {
			value = maxRunTime;
		} else if (attribute.equalsIgnoreCase("memoryPerNode")) {
			value = memoryPerNode;
		} else if (attribute.equalsIgnoreCase("nodeCount")) {
			value = nodeCount;
		} else if (attribute.equalsIgnoreCase("archiveOutput")) {
			return archiveOutput;
		} else if (attribute.equalsIgnoreCase("archivePath")) {
			return archiveOutput ? archivePath : "";
		} else if (attribute.equalsIgnoreCase("outputPath")) {
			value = workPath;
		} else if (attribute.equalsIgnoreCase("outputUrl")) {
			value = getArchiveUrl();
		} else if (attribute.equalsIgnoreCase("status")) {
			value = status;
		} else if (attribute.equalsIgnoreCase("message")) {
			value = status.equals(JobStatusType.FAILED) ? errorMessage == null ? "" : errorMessage : "";
		} else if (attribute.equalsIgnoreCase("submitTime")) {
			value = submitTime == null ? null : submitTime.getTime();
		} else if (attribute.equalsIgnoreCase("startTime")) {
			value = startTime == null ? null : startTime.getTime();
		} else if (attribute.equalsIgnoreCase("endTime")) {
			value = endTime == null ? null : endTime.getTime();
		} else if (attribute.equalsIgnoreCase("inputs")) {
			value = getInputsAsJsonObject().toString();
		} else {
			throw new JobException("Unrecognized job attribute " + attribute);
		}
		
		return value;
	}

	/**
	 * Calculates when a running job should have completed by.
	 * 
	 * @return
	 */
	public Date calculateExpirationDate() 
	{
		// if it's got an end time, return that
		if (getEndTime() != null) 
		{
			return getEndTime();
		}
		else 
		{
			DateTime jobExpirationDate = null;
		
			// use when job actually started if available
			if (getStartTime() != null) 
			{
				jobExpirationDate = new DateTime(getStartTime());
			} 
			// otherwise use the worst case start time
			else
			{
				// created + 7 days to stage inputs + 7 days to submit
				jobExpirationDate = new DateTime(getCreated()).plusDays(7).plusDays(7);
			}
			
			// now adjust for run time. When no upper limit is given, use the max queue time
			if (StringUtils.isEmpty(getMaxRunTime())) 
			{
				// max queue length is 1000 hrs ~ 42 days
				return jobExpirationDate.plusHours(1000).toDate(); 
			} 
			// if we have a max run time, parse that and add it to the start time
			else 
			{
				String[] runTimeTokens = getMaxRunTime().split(":");
				
				if (runTimeTokens.length > 2) {
					jobExpirationDate = jobExpirationDate.plusHours(NumberUtils.toInt(ServiceUtils.trimLeadingZeros(runTimeTokens[0]), 0));
					jobExpirationDate = jobExpirationDate.plusMinutes(NumberUtils.toInt(ServiceUtils.trimLeadingZeros(runTimeTokens[1]), 0));
					jobExpirationDate = jobExpirationDate.plusSeconds(NumberUtils.toInt(ServiceUtils.trimLeadingZeros(runTimeTokens[2]), 0));
				}
				else if (runTimeTokens.length == 2) 
				{
					jobExpirationDate = jobExpirationDate.plusMinutes(NumberUtils.toInt(ServiceUtils.trimLeadingZeros(runTimeTokens[0]), 0));
					jobExpirationDate = jobExpirationDate.plusSeconds(NumberUtils.toInt(ServiceUtils.trimLeadingZeros(runTimeTokens[1]), 0));
				}
				else if (runTimeTokens.length == 1) {
					jobExpirationDate = jobExpirationDate.plusSeconds(NumberUtils.toInt(ServiceUtils.trimLeadingZeros(runTimeTokens[0]), 0));
				}
				
				// give a buffer of 1 day1 just in case of something weird going on
				return jobExpirationDate.plusDays(1).toDate();
			}
		}
	}
	
	public String toString() {
		return String.format("%s - %s - %s", 
				getUuid(),
				getStatus().name(),
				getLastUpdated());
	}

	/**
	 * Serializes to json with notifications embedded in hypermedia response.
	 * @param notifications
	 * @return
	 * @throws IOException 
	 */
	public String toJsonWithNotifications(List<Notification> notifications)
	throws JsonProcessingException, IOException
	{
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode notifArray = mapper.createArrayNode();
		
		if (notifications != null) {
			String baseNotificationUrl = TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_NOTIFICATION_SERVICE);
			for(Notification n: notifications) {
				notifArray.add(mapper.createObjectNode()
						.put("href", baseNotificationUrl + n.getUuid())
						.put("title", n.getEvent()));
			}
		}
				
		ObjectNode hypermedia = getHypermedia();
		hypermedia.put("notification", notifArray);
		
		ObjectNode json = mapper.createObjectNode()
			.put("id", uuid)
			.put("name", name)
			.put("owner", owner)
			.put("appId", softwareName)
			.put("executionSystem", system)
			.put("batchQueue", batchQueue)
			.put("nodeCount", nodeCount)
			.put("processorsPerNode", processorsPerNode)
			.put("memoryPerNode", memoryPerNode)
			.put("maxRunTime", maxRunTime)
			.put("archive", archiveOutput)
			.put("retries", retries)
			.put("localId", localJobId)
			.put("created", new DateTime(created).toString())
			.put("lastModified", new DateTime(lastUpdated).toString());
			
		if (archiveOutput)
		{
			json.put("archivePath", getArchivePath());
			json.put("archiveSystem", getArchiveSystem().getSystemId());
		}
		
		json.put("outputPath", getWorkPath());
		
		json.put("status", status.name());

		if (status.equals(JobStatusType.FAILED))
		{
			json.put("message", getErrorMessage());
		}

		json.put("submitTime", submitTime == null ? null : new DateTime(getSubmitTime()).toString());
		json.put("startTime", startTime == null ? null : new DateTime(getStartTime()).toString());
		json.put("endTime", endTime == null ? null : new DateTime(getEndTime()).toString());
//			json.put("workPath", getWorkPath());
		try {
			json.set("inputs", getInputsAsJsonObject());
			json.set("parameters", getParametersAsJsonObject());
		} catch (JobException e) {
			throw new IOException(e.getMessage(), e);
		}
		
		json.set("_links", hypermedia);
	    	
		return json.toString();
	}
}

	