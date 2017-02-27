/**
 * 
 */
package org.iplantc.service.jobs.model.dto;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.util.Date;

import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.common.model.Tenant;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uri.UrlPathEscaper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.model.views.View;
import org.iplantc.service.profile.model.InternalUser;
import org.iplantc.service.systems.model.BatchQueue;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteSystem;
import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Serialization bean for {@link Job} objects and search results.
 * @author dooley
 *
 */
public class JobDTO {
	
	@JsonIgnore
	private static final Logger log = Logger.getLogger(JobDTO.class);
	
	/**
	 * Unique db identifier for the job record.
	 */
	@JsonIgnore
	private Long				id;
	
	/**
	 * The serialized {@link AgaveUUID} for this job.
	 */
	@JsonProperty("id")
	@JsonView(View.Summary.class)
	private String				uuid; 			// Unique id not based on database id
	
	/**
	 * {@link Software#getUniqueName()} of the app being run by this job.
	 */
	@JsonProperty("appId")
	@JsonView(View.Summary.class)
	private String				software_name;	// Unique name of an application
	
	/**
	 * Whether the job should be archived after code execution completes.
	 */
	@JsonIgnore
	@JsonProperty("archive")
	@JsonView(View.Full.class)
	private boolean				archive_output;	
	
	/**
	 * The path to the archived job {@link #work_path} on the {@link #archive_system}
	 */
	@JsonProperty("archivePath")
	@JsonView(View.Full.class)
	private String				archive_path;
	
	/**
	 * {@link RemoteSystem} to which the job {@link #work_path} will be transferred.
	 */
	@JsonProperty("archiveSystem")
	@JsonView(View.Full.class)
	private String				archive_system;	
	
	/**
	 * Charge for this job against the user's allocation
	 */
	@JsonProperty("charge")
	@JsonView(View.Full.class)
	private double				charge;			
	
	/**
	 * Job creation timestampe
	 */
	@JsonProperty("created")
	@JsonView(View.Summary.class)
	private Date				created;
	
	/**
	 * Job completion timestamp
	 */
	@JsonProperty("endTime")
	@JsonView(View.Summary.class)
	private Date				end_time;
	
	/**
	 * Current status message for the job 
	 */
	@JsonProperty("message")
	@JsonView(View.Full.class)
	private String				error_message;
	
	/**
	 * {@link ExecutionSystem#getSystemId()} on which this job is to be run. 
	 */
	@JsonProperty("executionSystem")
	@JsonView(View.Summary.class)
	private String				execution_system;
	
	/**
	 * {@link ObjectNode} encoded list of {@link JobInput}s
	 */
	@JsonProperty("inputs")
	@JsonView(View.Full.class)
	private String				inputs;
	
	/**
	 * {@link InternalUser} tied to the {@link #owner} of the job to which
	 * this job will be attributed.  
	 */
	@JsonProperty("internalUsername")
	@JsonView(View.Full.class)
	private String				internal_username;
	
	/**
	 * Last modified timestamp 
	 */
	@JsonProperty("lastUpdated")
	@JsonView(View.Full.class)
	private Date				last_updated;	
 
	/**
	 * The batch scheduler id or process id of the job on a
	 * remote system.
	 */
	@JsonProperty("localJobId")
	@JsonView(View.Full.class)
	private String				local_job_id;
	
	/**
	 * Requested max run time in HHH:MM:DD format that the job
	 * should run.
	 */
	@JsonProperty("maxRunTime")
	@JsonView(View.Full.class)
	private String				requested_time;
	
	/**
	 * Requested memory per node for the job.
	 */
	@JsonProperty("memoryPerNode")
	@JsonView(View.Full.class)
	private double				memory_request;	
	
	/**
	 * A freeform name for the job.
	 */
	@JsonProperty("name")
	@JsonView(View.Summary.class)
	private String				name;			

	/**
	 * Requested total number of nodes needed to run this job.
	 */
	@JsonProperty("nodeCount")
	@JsonView(View.Full.class)
	private int					node_count;		
	
	/**
	 * The principal to whom this job is attributed.
	 */
	@JsonProperty("owner")
	@JsonView(View.Summary.class)
	private String				owner;
	
	/**
	 * {@link ObjectNode} encoded list of {@link JobParameter}s
	 */
	@JsonProperty("parameters")
	@JsonView(View.Full.class)
	private String				parameters;
	
	/**
	 * Requested number of processors per node needed to run this job.
	 */
	@JsonProperty("processorsPerNode")
	@JsonView(View.Full.class)
	private int					processor_count; 
	
	/**
	 * {@link BatchQueue#getEffectiveMappedName()} of the queue to which the 
	 * job was submitted.
	 */
	@JsonProperty("batchQueue")
	@JsonView(View.Full.class)
	private String				queue_request;
	
	/**
	 * Number of retries for this job
	 */
	@JsonProperty("retries")
	@JsonView(View.Full.class)
	private int					retries;

	/**
	 * Serialized {@link JobStatusType} value of the job status.
	 */
	@JsonProperty("status")
	@JsonView(View.Summary.class)
	private String				status;
	
	/**
	 * The number of times this job has been actively checked.
	 */
//	@JsonProperty("statusChecks")
	@JsonIgnore
	private int				status_checks; 	
	
	/**
	 * Optional id given by a metascheduler if the job is managed by 
	 * something other than a batch scheduler on a remote system.
	 */
	@JsonProperty("schedulerJobId")
	@JsonView(View.Full.class)
	private String				scheduler_job_id; 
	
	/**
	 * Job start timestamp 
	 */
	@JsonProperty("startTime")
	@JsonView(View.Summary.class)
	private Date				start_time;
	
	/**
	 * Job submitted to remote system for execution timestamp
	 */
	@JsonProperty("submitTime")
	@JsonView(View.Full.class)
	private Date				submit_time;		
	
	/**
	 * {@link Tenant#getCode} to which this job is bound.
	 */
//	@JsonProperty("tenantId")
	@JsonIgnore
	private String				tenant_id;
	
	/**
	 * Token needed to validate all callbacks to update job 
	 * status and send runtime job event messages.
	 */
	@JsonIgnore
	private String				update_token;	
	
	@JsonIgnore
	private int					optlock;
	/**
	 * Optimistic locking column for the job
	 */
//	@JsonProperty("version")
	@JsonIgnore
	private BigInteger			version;
	
	/**
	 * Whether this job has been hidden from view 
	 * aka "deleted"
	 */
	@JsonIgnore
	private Boolean				visible;
	
	/**
	 * The path to the job work directory on the {@link #execution_system} 
	 */
	@JsonProperty("workPath")
	@JsonView(View.Full.class)
	private String				work_path;
	
	public JobDTO() {
		status = JobStatusType.PENDING.name();
		status_checks = 0;
		setVisible(true);
				
	}
	
	public JobDTO(Job job) {
		
	}
	
	@Transient
	public JsonNode getInputsAsJsonObject() throws JobException
	{
		try 
		{
			ObjectMapper mapper = new ObjectMapper();
			
			if (StringUtils.isEmpty(inputs)) {
				return mapper.createObjectNode();
			}
			else
			{
				return mapper.readTree(inputs);
			}
		}
		catch (Exception e) {
			throw new JobException("Failed to parse job inputs", e);
		}
	}
	
	@Transient
	@JsonIgnore
	public JsonNode getParametersAsJsonObject() throws JobException
	{
		try 
		{
			ObjectMapper mapper = new ObjectMapper();
			if (StringUtils.isEmpty(parameters))
			{
				return mapper.createObjectNode();
			}
			else
			{
				return mapper.readTree(parameters);
			}
		}
		catch (Exception e) {
			throw new JobException("Failed to parse job parameters", e);
		}
	}
	
	@Transient
	@JsonIgnore
	public JobStatusType getStatusType() {
		return JobStatusType.valueOf(status);
	}
	
	@Transient
	@JsonIgnore
	public boolean isFinished()
	{
		return JobStatusType.isFinished(getStatusType());
	}

	@Transient
	@JsonIgnore
	public boolean isSubmitting()
	{
		return JobStatusType.isSubmitting(getStatusType());
	}

	@Transient
	@JsonIgnore
	public boolean isRunning()
	{
		return JobStatusType.isRunning(getStatusType());
	}

	@Transient
	@JsonIgnore
	public boolean isArchived()
	{
		return JobStatusType.isArchived(getStatusType());
	}
	
	@Transient
	@JsonIgnore
	public boolean isFailed()
	{
		return JobStatusType.isFailed(getStatusType());
	}
	
	@Transient
	@JsonIgnore
	public String getArchiveUrl() 
	{
		if (isFinished() && isArchive_output()) {
			return TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_IO_SERVICE, tenant_id) + 
					"listings/system/" + archive_system + "/" + UrlPathEscaper.escape(archive_path);
		} else {
			return TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE, tenant_id) + 
					uuid + "/outputs/listings";
		}
	}
	
	@Transient
	public String getArchiveCanonicalUrl() 
	{
		if (isFinished() && isArchive_output()) {
			return TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_IO_SERVICE, tenant_id) + 
					"listings/system/" + archive_system + "/" + UrlPathEscaper.escape(archive_path);
		} else {
			return TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_IO_SERVICE, tenant_id) + 
					"listings/system/" + execution_system + "/" + UrlPathEscaper.escape(work_path);
		}
	}
	
//	@Transient
//	@JsonIgnore
	@JsonProperty("_links")
	@JsonView(View.Full.class)
	private ObjectNode getFullHypermedia() {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode linksObject = mapper.createObjectNode();
		linksObject.set("self", (ObjectNode)mapper.createObjectNode()
    		.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE, tenant_id) + uuid));
		linksObject.set("app", mapper.createObjectNode()
    		.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_APPS_SERVICE, tenant_id) + software_name));
		linksObject.set("executionSystem", mapper.createObjectNode()
    		.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_SYSTEM_SERVICE, tenant_id) + execution_system));
		linksObject.set("archiveSystem", mapper.createObjectNode()
        		.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_SYSTEM_SERVICE, tenant_id) + 
        				(isArchive_output() ? archive_system : execution_system)));
		
		linksObject.set("archiveData", mapper.createObjectNode()
    		.put("href", getArchiveUrl()));
    	
		linksObject.set("owner", mapper.createObjectNode()
			.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE, tenant_id) + owner));
		linksObject.set("permissions", mapper.createObjectNode()
    		.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE, tenant_id) + uuid + "/pems"));
        linksObject.set("history", mapper.createObjectNode()
			.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE, tenant_id) + uuid + "/history"));
	    linksObject.set("metadata", mapper.createObjectNode()
			.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_METADATA_SERVICE, tenant_id) + "data/?q=" + URLEncoder.encode("{\"associationIds\":\"" + uuid + "\"}")));
		linksObject.set("notifications", mapper.createObjectNode()
			.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_NOTIFICATION_SERVICE, tenant_id) + "?associatedUuid=" + uuid));
		
    	if (!StringUtils.isEmpty(internal_username)) {
    		linksObject.set("internalUser", mapper.createObjectNode()
    			.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE, tenant_id) + owner + "/users/" + internal_username));
    	}
    	
    	return linksObject;
	}
	
	@Transient
	@JsonIgnore
	private ObjectNode getSummaryHypermedia() {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode linksObject = mapper.createObjectNode();
		linksObject.set("self", (ObjectNode)mapper.createObjectNode()
    		.put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE, tenant_id) + uuid));
		linksObject.set("archiveData", mapper.createObjectNode()
    		.put("href", getArchiveUrl()));
    	
    	return linksObject;
	}
	
	@JsonValue
	public ObjectNode toJSON() throws JsonProcessingException, IOException
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode json = mapper.createObjectNode()
			.put("id", uuid)
			.put("name", name)
			.put("owner", owner)
			.put("appId", software_name)
			.put("executionSystem", execution_system)
			.put("batchQueue", queue_request)
			.put("nodeCount", node_count)
			.put("processorsPerNode", processor_count)
			.put("memoryPerNode", memory_request)
			.put("maxRunTime", requested_time)
			.put("archive", isArchive_output())
			.put("retries", retries)
			.put("localId", local_job_id)
			.put("created", new DateTime(created).toString());
			
		if (isArchive_output())
		{
			json.put("archivePath", archive_path);
			json.put("archiveSystem", archive_system);
		}
		
		json.put("outputPath", work_path);
		
		json.put("status", status);

		if (StringUtils.equals(status, JobStatusType.FAILED.name()))
		{
			json.put("message", error_message);
		}

		json.put("submitTime", submit_time == null ? null : new DateTime(submit_time).toString());
		json.put("startTime", start_time == null ? null : new DateTime(start_time).toString());
		json.put("endTime", end_time == null ? null : new DateTime(end_time).toString());
		json.put("lastUpdated", new DateTime(last_updated).toString());

		try {
			json.set("inputs", getInputsAsJsonObject());
			json.set("parameters", getParametersAsJsonObject());
		} catch (JobException e) {
			throw new IOException(e.getMessage(), e);
		}
		
    	json.set("_links", getFullHypermedia());
    	
		return json;
	}

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the uuid
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * @param uuid the uuid to set
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	/**
	 * @return the software_name
	 */
	public String getSoftware_name() {
		return software_name;
	}

	/**
	 * @param software_name the software_name to set
	 */
	public void setSoftware_name(String software_name) {
		this.software_name = software_name;
	}
	
	/**
	 * @return the archive_output
	 */
	public Boolean isArchive_output() {
		return archive_output;
	}
	
	/**
	 * @param archive_output the archive_output to set
	 */
	public void setArchive_output(Boolean archive_output) {
		this.archive_output = (archive_output != null && archive_output);
	}
	
	/**
	 * @return the archive_path
	 */
	public String getArchive_path() {
		return archive_path;
	}

	/**
	 * @param archive_path the archive_path to set
	 */
	public void setArchive_path(String archive_path) {
		this.archive_path = archive_path;
	}

	/**
	 * @return the archive_system
	 */
	public String getArchive_system() {
		return archive_system;
	}

	/**
	 * @param archive_system the archive_system to set
	 */
	public void setArchive_system(String archive_system) {
		this.archive_system = archive_system;
	}

	/**
	 * @return the charge
	 */
	public double getCharge() {
		return charge;
	}

	/**
	 * @param charge the charge to set
	 */
	public void setCharge(Double charge) {
		this.charge = charge == null ? 0 : charge.doubleValue();
	}

	/**
	 * @return the created
	 */
	public Date getCreated() {
		return created;
	}

	/**
	 * @param created the created to set
	 */
	public void setCreated(Date created) {
		this.created = created;
	}

	/**
	 * @return the end_time
	 */
	public Date getEnd_time() {
		return end_time;
	}

	/**
	 * @param end_time the end_time to set
	 */
	public void setEnd_time(Date end_time) {
		this.end_time = end_time;
	}

	/**
	 * @return the error_message
	 */
	public String getError_message() {
		return error_message;
	}

	/**
	 * @param error_message the error_message to set
	 */
	public void setError_message(String error_message) {
		this.error_message = error_message;
	}

	/**
	 * @return the execution_system
	 */
	public String getExecution_system() {
		return execution_system;
	}

	/**
	 * @param execution_system the execution_system to set
	 */
	public void setExecution_system(String execution_system) {
		this.execution_system = execution_system;
	}

	/**
	 * @return the inputs
	 */
	public String getInputs() {
		return inputs;
	}

	/**
	 * @param inputs the inputs to set
	 */
	public void setInputs(String inputs) {
		this.inputs = inputs;
	}

	/**
	 * @return the internal_username
	 */
	public String getInternal_username() {
		return internal_username;
	}

	/**
	 * @param internal_username the internal_username to set
	 */
	public void setInternal_username(String internal_username) {
		this.internal_username = internal_username;
	}

	/**
	 * @return the last_updated
	 */
	public Date getLast_updated() {
		return last_updated;
	}

	/**
	 * @param last_updated the last_updated to set
	 */
	public void setLast_updated(Date last_updated) {
		this.last_updated = last_updated;
	}

	/**
	 * @return the local_job_id
	 */
	public String getLocal_job_id() {
		return local_job_id;
	}

	/**
	 * @param local_job_id the local_job_id to set
	 */
	public void setLocal_job_id(String local_job_id) {
		this.local_job_id = local_job_id;
	}

	/**
	 * @return the requested_time
	 */
	public String getRequested_time() {
		return requested_time;
	}

	/**
	 * @param requested_time the requested_time to set
	 */
	public void setRequested_time(String requested_time) {
		this.requested_time = requested_time;
	}

	/**
	 * @return the memory_request
	 */
	public double getMemory_request() {
		return memory_request;
	}

	/**
	 * @param memory_request the memory_request to set
	 */
	public void setMemory_request(Double memory_request) {
		this.memory_request = memory_request == null ? 0 : memory_request.doubleValue();
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the node_count
	 */
	public long getNode_count() {
		return node_count;
	}

	/**
	 * @param node_count the node_count to set
	 */
	public void setNode_count(Integer node_count) {
		this.node_count = node_count == null ? 0 : node_count.intValue();
	}

	/**
	 * @return the owner
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * @param owner the owner to set
	 */
	public void setOwner(String owner) {
		this.owner = owner;
	}

	/**
	 * @return the parameters
	 */
	public String getParameters() {
		return parameters;
	}

	/**
	 * @param parameters the parameters to set
	 */
	public void setParameters(String parameters) {
		this.parameters = parameters;
	}

	/**
	 * @return the processor_count
	 */
	public long getProcessor_count() {
		return processor_count;
	}

	/**
	 * @param processor_count the processor_count to set
	 */
	public void setProcessor_count(Integer processor_count) {
		this.processor_count = processor_count == null ? 0 : processor_count.intValue();
	}

	/**
	 * @return the queue_request
	 */
	public String getQueue_request() {
		return queue_request;
	}

	/**
	 * @param queue_request the queue_request to set
	 */
	public void setQueue_request(String queue_request) {
		this.queue_request = queue_request;
	}

	/**
	 * @return the retries
	 */
	public int getRetries() {
		return retries;
	}

	/**
	 * @param retries the retries to set
	 */
	public void setRetries(Integer retries) {
		this.retries = retries == null ? 0 : retries.intValue();
	}

	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * @return the status_checks
	 */
	public int getStatus_checks() {
		return status_checks;
	}

	/**
	 * @param status_checks the status_checks to set
	 */
	public void setStatus_checks(Integer status_checks) {
		this.status_checks = status_checks == null ? 0 : status_checks.intValue();
	}

	/**
	 * @return the scheduler_job_id
	 */
	public String getScheduler_job_id() {
		return scheduler_job_id;
	}

	/**
	 * @param scheduler_job_id the scheduler_job_id to set
	 */
	public void setScheduler_job_id(String scheduler_job_id) {
		this.scheduler_job_id = scheduler_job_id;
	}

	/**
	 * @return the start_time
	 */
	public Date getStart_time() {
		return start_time;
	}

	/**
	 * @param start_time the start_time to set
	 */
	public void setStart_time(Date start_time) {
		this.start_time = start_time;
	}

	/**
	 * @return the submit_time
	 */
	public Date getSubmit_time() {
		return submit_time;
	}

	/**
	 * @param submit_time the submit_time to set
	 */
	public void setSubmit_time(Date submit_time) {
		this.submit_time = submit_time;
	}

	/**
	 * @return the tenant_id
	 */
	public String getTenant_id() {
		return tenant_id;
	}

	/**
	 * @param tenant_id the tenant_id to set
	 */
	public void setTenant_id(String tenant_id) {
		this.tenant_id = tenant_id;
	}

	/**
	 * @return the update_token
	 */
	public String getUpdate_token() {
		return update_token;
	}

	/**
	 * @param update_token the update_token to set
	 */
	public void setUpdate_token(String update_token) {
		this.update_token = update_token;
	}

	/**
	 * @return the version
	 */
	public BigInteger getVersion() {
		return version;
	}

	/**
	 * @param version the version to set
	 */
	public void setVersion(BigInteger version) {
		this.version = version;
	}

	/**
	 * @return the visible
	 */
	@Transient
	@JsonProperty("visible")
	public boolean isVisible() {
		return visible;
	}
	
	/**
	 * @return
	 */
	@JsonIgnore
	private boolean getVisible() {
		return visible;
	}
	
	/**
	 * @param visible the visible to set
	 */
	public void setVisible(Boolean visible) {
		this.visible = (visible != null && visible);
	}

	/**
	 * @return the work_path
	 */
	public String getWork_path() {
		return work_path;
	}

	/**
	 * @param work_path the work_path to set
	 */
	public void setWork_path(String work_path) {
		this.work_path = work_path;
	}
	
	public boolean equals(Job job) {
		return (job != null && StringUtils.equals(getUuid(), job.getUuid()));
	}
}
