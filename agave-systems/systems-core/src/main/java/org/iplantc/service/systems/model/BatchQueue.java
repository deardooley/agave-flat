package org.iplantc.service.systems.model;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.util.TimeUtils;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.systems.Settings;
import org.iplantc.service.systems.dao.BatchQueueDao;
import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.util.ServiceUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Contains the bean to define a batch queue. Each queue
 * can have its own custom directives and quotas to ensure
 * maximum customizability within a RemoteSystem
 *
 * @author dooley
 *
 */
@Entity
@Table(name = "batchqueues")
public class BatchQueue implements LastUpdatable, Comparable<BatchQueue> {

	public static final Long DEFAULT_MAX_JOBS = new Long(10);
	public static final Long DEFAULT_MAX_MEMORY = new Long(64);
	public static final String DEFAULT_MAX_RUN_TIME = "9999:23:59";
	public static final String DEFAULT_MIN_RUN_TIME = "00:00:01";

	private Integer			id;
	private String 			name;
	private String 			mappedName;
	private String 			description;
	private Long			maxJobs = new Long(-1);
	private Long			maxUserJobs = new Long(-1);
	private Long			maxNodes = new Long(-1);
	private Double			maxMemoryPerNode = new Double(-1);
	private Long			maxProcessorsPerNode = new Long(-1);
	private String			maxRequestedTime = "999:59:59";
	private String 			customDirectives;
	private ExecutionSystem executionSystem;
	private boolean			systemDefault = false;
	private String          uuid;
	private Date 			lastUpdated = new Date();
	private Date 			created = new Date();

	public BatchQueue() {
	    this.uuid = new AgaveUUID(UUIDType.BATCH_QUEUE).toString();
	}

	public BatchQueue(String name) {
		this();
		this.name = name;
	}

	public BatchQueue(String name, Long maxJobs, Double maxMemoryPerNode)
	{
	    this(name);
		setMaxJobs(maxJobs);
		setMaxMemoryPerNode(maxMemoryPerNode);
	}

	public BatchQueue(String name, Long maxJobs, Long maxUserJobs,
			Long maxNodes, Double maxMemoryPerNode, Long maxProcessorsPerNode,
			String maxRequestedTime, String customDirectives,
			boolean systemDefault) {
		this(name, maxJobs, maxMemoryPerNode);
		this.maxUserJobs = maxUserJobs;
		this.maxNodes = maxNodes;
		this.maxMemoryPerNode = maxMemoryPerNode;
		this.maxProcessorsPerNode = maxProcessorsPerNode;
		this.maxRequestedTime = maxRequestedTime;
		this.customDirectives = customDirectives;
		this.systemDefault = systemDefault;
	}

	@Id
	@GeneratedValue
	@Column(name = "id", unique = true, nullable = false)
	public Integer getId()
	{
		return this.id;
	}

	public void setId(Integer id)
	{
		this.id = id;
	}

	/**
	 * @return the name
	 */
	@Column(name = "`name`", nullable = false, length = 128)
	public String getName()
	{
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name)
	{
		if (StringUtils.length(name) > 128) {
			throw new SystemException("'system.queues.name' must be less than 128 characters.");
		}
		if (!name.equals(name.replaceAll( "[^0-9a-zA-Z\\.\\-]" , ""))) {
			throw new SystemException("'system.queues.name' may only contain alphanumeric characters, periods, and dashes.");
		}

		this.name = name;
	}

	/**
	 * @return the mappedName
	 */
	@Column(name = "`mapped_name`", nullable = true, length = 128)
	public String getMappedName() {
		return mappedName;
	}

	/**
	 * @param mappedName the mappedName to set
	 */
	public void setMappedName(String mappedName) {
		if (StringUtils.length(mappedName) > 128) {
			throw new SystemException("'system.queues.mappedName' must be less than 128 characters.");
		}
		if (mappedName != null && !mappedName.equals(mappedName.replaceAll( "[^0-9a-zA-Z\\.\\-]" , ""))) {
			throw new SystemException("'system.queues.mappedName' may only contain alphanumeric characters, periods, and dashes.");
		}

		this.mappedName = mappedName;
	}
	
	/**
	 * Returns the effective name of this queue to use when interacting with 
	 * the remote scheduler. The {@link #mappedName} is used by default. If 
	 * that is not defined, the {@link #name} is used instead.
	 * 
	 * @return the {@link #mappedName} if not null, otherwise {@link #name} 
	 */
	@Transient
	public String getEffectiveMappedName() {
		return StringUtils.isEmpty(getMappedName()) ? getName() : getMappedName();
	}

	/**
	 * @return the description
	 */
	@Column(name = "`description`", nullable = true, length = 512)
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		if (StringUtils.length(description) > 512) {
			throw new SystemException("'system.queues.description' must be less than 512 characters.");
		}
		this.description = description;
	}

	/**
	 * @return the maxJobs
	 */
	@Column(name = "max_jobs", nullable = false)
	public Long getMaxJobs()
	{
		return maxJobs;
	}

	/**
	 * @param maxJobs the maxJobs to set
	 */
	public void setMaxJobs(Long maxJobs)
	{
		if (maxJobs != null && (maxJobs < -1 || maxJobs == 0)) {
			throw new SystemException("system.queue.maxJobs' must be a positive integer value or -1 for no limit.");
		}

		this.maxJobs = maxJobs;
	}

	/**
	 * @return the maxNodes
	 */
	@Column(name = "max_nodes", nullable = false)
	public Long getMaxNodes()
	{
		return maxNodes;
	}

	/**
	 * @param maxNodes the maxNodes to set
	 */
	public void setMaxNodes(Long maxNodes)
	{
		if (maxNodes != null && (maxNodes < -1 || maxNodes == 0)) {
			throw new SystemException("system.queue.maxNodes' must be a positive integer value or -1 for no limit.");
		}

		this.maxNodes = maxNodes;
	}

	/**
	 * @return the maxUserJobs
	 */
	@Column(name = "max_user_jobs", nullable = false)
	public Long getMaxUserJobs()
	{
		return maxUserJobs;
	}

	/**
	 * @param maxUserJobs the maxUserJobs to set
	 */
	public void setMaxUserJobs(Long maxUserJobs)
	{
		if (maxUserJobs != null && (maxUserJobs < -1 || maxUserJobs == 0)) {
			throw new SystemException("system.queue.maxUserJobs' must be a positive integer value or -1 for no limit.");
		}

		this.maxUserJobs = maxUserJobs;
	}

	/**
	 * @return the maxMemory
	 */
	@Column(name = "max_memory", nullable = false)
	public Double getMaxMemoryPerNode()
	{
		return maxMemoryPerNode;
	}

	/**
	 * Returns the max memory in ###.##[EPTGM]B format
	 * @return String human readable representation of the max memory
	 */
	@Transient
	public String getHumanReadableMaxMemoryPerNode()
	{
		return formatMaxMemoryPerNode(maxMemoryPerNode);
	}

	/**
	 * @param maxMemory the maxMemory to set
	 */
	public void setMaxMemoryPerNode(Double maxMemory)
	{
		if (maxMemory != null && (maxMemory < -1 || maxMemory == 0)) {
			throw new SystemException("system.queue.maxMemoryPerNode' should be a postive value "
					+ "specified in ###.#[EPTGM]B format or -1 for no limit.");
		}

		this.maxMemoryPerNode = maxMemory;
	}

	/**
	 * Takes a human readable value for the max memory in ###.##[EPTGM]B format
	 * and converts it to a long value in MB.
	 *
	 * @param maxMemory the maxMemory to set
	 */
	public void setMaxMemoryPerNode(String maxMemory)
	{
		this.maxMemoryPerNode = parseMaxMemoryPerNode(maxMemory);
	}

	/**
	 * Maximum number of processors for any job in this queue
	 * @return
	 */
	@Column(name = "max_procesors", nullable = false)
	public Long getMaxProcessorsPerNode() {
		return maxProcessorsPerNode;
	}

	/**
	 * @param maxProcessors
	 */
	public void setMaxProcessorsPerNode(Long maxProcessorsPerNode)
	{
		if (maxProcessorsPerNode != null && (maxProcessorsPerNode < -1 || maxProcessorsPerNode == 0)) {
			throw new SystemException("system.queue.maxProcessorsPerNode' must be a positive integer value or -1 for no limit.");
		}

		this.maxProcessorsPerNode = maxProcessorsPerNode;
	}

	/**
	 * Maximum run time of any job in this queue in (h)hh:mm:ss format
	 * @return
	 */
	public String getMaxRequestedTime() {
		return maxRequestedTime;
	}

	/**
	 * @param maxRequestedTime
	 */
	@Column(name = "`max_requested_time`", nullable = true, length = 19)
	public void setMaxRequestedTime(String maxRequestedTime)
	{
        if(maxRequestedTime.equals("")){
            throw new SystemException("'system.queues.maxRequestedTime' must not be an empty string.");
        }
		if (!StringUtils.isEmpty(maxRequestedTime))
		{
			if (maxRequestedTime.length() > 19) {
				throw new SystemException("'system.queues.maxRequestedTime' must be less than 19 characters.");
			} else if (!TimeUtils.isValidRequestedJobTime(maxRequestedTime)) {
				throw new SystemException("'system.queues.maxRequestedTime' must be in the form of hh:mm:ss.");
			}
		}

		this.maxRequestedTime = maxRequestedTime;
	}

	/**
	 * @return the customDirectives
	 */
	@Column(name = "custom_directives", length = 16384)
	public String getCustomDirectives()
	{
		return customDirectives;
	}

	@Transient
	public String[] getCustomDirectiveArray()
	{
		return customDirectives.split("\n");
	}

	/**
	 * @param customDirectives the customDirectives to set
	 */
	public void setCustomDirectives(String customDirectives)
	{
		if (!StringUtils.isEmpty(customDirectives) && customDirectives.length() > 16384) {
			throw new SystemException("'system.queues.customDirectives' must be less than 16384 characters.");
		}

		this.customDirectives = customDirectives;
	}

	/**
	 * @return the executionSystem
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "execution_system_id")
	public ExecutionSystem getExecutionSystem()
	{
		return executionSystem;
	}

	/**
	 * @param executionSystem the executionSystem to set
	 */
	public void setExecutionSystem(ExecutionSystem executionSystem)
	{
		this.executionSystem = executionSystem;
	}

	/**
	 * @return the systemDefault
	 */
	@Column(name = "system_default", columnDefinition = "TINYINT(1)")
	public boolean isSystemDefault()
	{
		return systemDefault;
	}

	/**
	 * @param systemDefault the systemDefault to set
	 */
	public void setSystemDefault(boolean systemDefault)
	{
		this.systemDefault = systemDefault;
	}

    /**
     * @return the uuid
     */
    @Column(name = "uuid", length = 128, nullable = false, unique = true)
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

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "last_updated", nullable = false, length = 19)
	public Date getLastUpdated()
	{
		return this.lastUpdated;
	}

	public void setLastUpdated(Date lastUpdated)
	{
		this.lastUpdated = lastUpdated;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created", nullable = false, length = 19)
	public Date getCreated()
	{
		return this.created;
	}

	public void setCreated(Date created)
	{
		this.created = created;
	}

	public String toString() {
		return this.name;
	}

	public String toJSON() {
	    ObjectMapper mapper = new ObjectMapper();
	    BatchQueueLoad load = new BatchQueueDao().getCurrentLoad(getExecutionSystem().getSystemId(), getName());
	    if (load == null) {
	        load = new BatchQueueLoad(getName());
	    }

	    ObjectNode json = mapper.createObjectNode()
	        .put("id", getUuid())
            .put("name", getName())
            .put("mappedName", getMappedName())
            .put("description", getDescription())
            .put("default", isSystemDefault())
            .put("maxJobs", getMaxJobs())
            .put("maxUserJobs", getMaxUserJobs())
            .put("maxNodes", getMaxNodes())
            .put("maxProcessorsPerNode", getMaxProcessorsPerNode())
            .put("maxMemoryPerNode", getMaxMemoryPerNode())
            .put("maxRequestedTime", getMaxRequestedTime())
            .put("customDirectives", getCustomDirectives());
//	    	.put("uuid", getUuid());
        json.set("load", mapper.valueToTree(load));

        ObjectNode linksObject = mapper.createObjectNode();
        linksObject.put("self", (ObjectNode)mapper.createObjectNode()
            .put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_SYSTEM_SERVICE, getExecutionSystem().getTenantId()) + getExecutionSystem().getSystemId() + "/queues/" + getUuid()));
        linksObject.put("executionSystem", mapper.createObjectNode()
            .put("href", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_SYSTEM_SERVICE, getExecutionSystem().getTenantId()) + getExecutionSystem().getSystemId()));

        json.set("_links", linksObject);

        return json.toString();

	}

	public static BatchQueue fromJSON(JSONObject jsonBatchQueue) throws SystemArgumentException
	{
		return fromJSON(jsonBatchQueue, null);
	}

	public static BatchQueue fromJSON(JSONObject jsonBatchQueue, BatchQueue batchQueue)
	throws SystemArgumentException
	{
		if (batchQueue == null) {
			batchQueue = new BatchQueue();
		}

		try
		{
			if (ServiceUtils.isNonEmptyString(jsonBatchQueue, "name")) {
				batchQueue.setName(jsonBatchQueue.getString("name"));
			} else {
				throw new SystemArgumentException("Please specify a valid string value for the 'defaultQueue.name' field. "
						+ "This is the name this system queue will map to on the remote execution system.");
			}
			
			if (jsonBatchQueue.has("mappedName") && !jsonBatchQueue.isNull("mappedName"))
			{
				try
				{
					if (jsonBatchQueue.get("mappedName") instanceof String) {
						batchQueue.setMappedName(jsonBatchQueue.getString("mappedName"));
					}
					else {
						throw new SystemException("Invalid 'mappedName' value. String expected.");
					}
				} catch (SystemException e) {
					throw new SystemArgumentException("Invalid 'mappedName' value. If provided, " +
							"please specify an alphanumeric value under 128 characters in length.");
				}
			}
			else {
				batchQueue.setMappedName(null);
			}
			
			if (jsonBatchQueue.has("description") && !jsonBatchQueue.isNull("description"))
			{
				try
				{
					if (jsonBatchQueue.get("description") instanceof String) {
						batchQueue.setDescription(jsonBatchQueue.getString("description"));
					}
					else {
						throw new SystemException("Invalid 'description' value. String expected.");
					}
				} catch (SystemException e) {
					throw new SystemArgumentException("Invalid 'description' value. If provided, " +
							"please specify short description of this queue under 512 characters in length.");
				}
			}
			else {
				batchQueue.setDescription(null);
			}

			if (jsonBatchQueue.has("default") && !jsonBatchQueue.isNull("default"))
			{
				try
				{
					boolean isDefault = jsonBatchQueue.getBoolean("default");
					batchQueue.setSystemDefault(isDefault);
				} catch (Exception e) {
					throw new SystemArgumentException("Invalid 'default' value. If provided, " +
							"please specify either true or false depending on whether you would " +
							"like this system to be the default queue for this sytstem. Default: false");
				}
			}
			else {
				batchQueue.setSystemDefault(false);
			}

			if (jsonBatchQueue.has("maxJobs") && !jsonBatchQueue.isNull("maxJobs"))
			{
				try {
					long maxJobsNumber = jsonBatchQueue.getLong("maxJobs");
					if (maxJobsNumber > 0 || maxJobsNumber == -1) {
						batchQueue.setMaxJobs(maxJobsNumber);
					} else {
						throw new SystemArgumentException("Invalid 'queue.maxJobs' value. If specified, " +
								"maxJobs should be a positive integer value or -1 for no limit.");
					}
				} catch (JSONException e) {
					throw new SystemArgumentException("Invalid 'queue.maxJobs' value. If specified, " +
							"maxJobs should be a positive integer value or -1 for no limit.");
				}
			}
			else {
				batchQueue.setMaxJobs(new Long(-1));
			}

			if (jsonBatchQueue.has("maxUserJobs") && !jsonBatchQueue.isNull("maxUserJobs"))
			{
				try {
					long maxUserJobs = jsonBatchQueue.getLong("maxUserJobs");
					if (maxUserJobs == -1) { // no limit
						batchQueue.setMaxUserJobs(maxUserJobs);
					}
					else if (maxUserJobs == 0 || maxUserJobs < -1) { // zero, negative not cool
						throw new SystemArgumentException("Invalid 'queue.maxJobs' value. If specified, " +
								"maxUserJobs should be a positive integer value or -1 for no limit.");
					}
					else if (batchQueue.getMaxJobs() == -1) // positive with no total job limit, can be anything
					{
						batchQueue.setMaxUserJobs(maxUserJobs);
					}
					else if (batchQueue.getMaxJobs() < maxUserJobs)
					{
						throw new SystemArgumentException("Invalid 'queue.maxUserJobs' value. If specified, " +
								"maxUserJobs must be less than the maxJobs for this queue.");
					}
					else
					{
						batchQueue.setMaxUserJobs(maxUserJobs);
					}
				} catch (JSONException e) {
					throw new SystemArgumentException("Invalid 'queue.maxJobs' value. If specified, " +
							"maxUserJobs should be a positive integer value or -1 for no limit.");
				}
			}
			else {
				batchQueue.setMaxUserJobs(new Long(-1));
			}

			if (jsonBatchQueue.has("maxNodes") && !jsonBatchQueue.isNull("maxNodes"))
			{
				try {
					long maxNodes = jsonBatchQueue.getLong("maxNodes");
					if (maxNodes > 0 || maxNodes == -1) {
						batchQueue.setMaxNodes(maxNodes);
					} else {
						throw new SystemArgumentException("Invalid 'queue.maxNodes' value. If specified, " +
								"maxNodes should be a positive integer value or -1 for no limit.");
					}
				} catch (JSONException e) {
					throw new SystemArgumentException("Invalid 'queue.maxNodes' value. If specified, " +
							"maxNodes should be a positive integer value or -1 for no limit.");
				}
			} else {
				batchQueue.setMaxNodes(new Long(-1));
			}

			if (jsonBatchQueue.has("maxProcessorsPerNode") && !jsonBatchQueue.isNull("maxProcessorsPerNode"))
			{
				try {
					long maxProcessorsPerNode = jsonBatchQueue.getLong("maxProcessorsPerNode");
					if (maxProcessorsPerNode > 0 || maxProcessorsPerNode == -1) {
						batchQueue.setMaxProcessorsPerNode(maxProcessorsPerNode);
					} else {
						throw new SystemArgumentException("Invalid 'queue.maxProcessorsPerNode' value. If specified, " +
								"maxProcessorsPerNode should be a positive integer value or -1 for no limit.");
					}
				} catch (JSONException e) {
					throw new SystemArgumentException("Invalid 'queue.maxProcessorsPerNode' value. If specified, " +
							"maxProcessorsPerNode should be a positive integer value or -1 for no limit.");
				}
			} else {
				batchQueue.setMaxProcessorsPerNode(new Long(-1));
			}

			if (jsonBatchQueue.has("maxRequestedTime") && !jsonBatchQueue.isNull("maxRequestedTime"))
			{
				batchQueue.setMaxRequestedTime(jsonBatchQueue.getString("maxRequestedTime"));
			}

			if (jsonBatchQueue.has("maxMemoryPerNode") && !jsonBatchQueue.isNull("maxMemoryPerNode"))
			{
				try {
					Double maxMemoryMb = parseMaxMemoryPerNode(jsonBatchQueue.getString("maxMemoryPerNode"));
					if (maxMemoryMb > 0 || maxMemoryMb == -1) {
						batchQueue.setMaxMemoryPerNode(maxMemoryMb);
					} else {
						throw new SystemArgumentException("Invalid 'queue.maxMemoryPerNode' value. If specified, " +
								"maxMemoryPerNode should be a postive value specified in ###.#[EPTGM]B format or -1 for no limit.");
					}
				}
				catch (NumberFormatException | JSONException e) {
					throw new SystemArgumentException("Invalid 'queue.maxMemoryPerNode' value. If specified, " +
							"maxMemoryPerNode should be a postive value specified in ###.#[EPTGM]B format or -1 for no limit.");
				} 
//				catch (JSONException e) {
//					throw new SystemArgumentException("Invalid 'queue.maxMemoryPerNode' value. If specified, " +
//							"maxMemoryPerNode should be a postive value specified in ###.#[EPTGM]B format or -1 for no limit.");
//				}
//				} else {
//					throw new SystemArgumentException("Invalid 'queue.maxMemoryPerNode' value. If specified, " +
//							"maxMemoryPerNode should be a postive value specified in ###.#[EPTGM]B format or -1 for no limit.");
//				}
			}
			else
			{
				batchQueue.setMaxMemoryPerNode(new Double(-1));
			}

			if (ServiceUtils.isNonEmptyString(jsonBatchQueue, "customDirectives")) {
				batchQueue.setCustomDirectives(jsonBatchQueue.getString("customDirectives"));
			}
		}
		catch (JSONException e) {
			throw new SystemArgumentException("Failed to parse 'queue' object", e);
		}
		catch (Exception e) {
			throw new SystemArgumentException("Failed to parse 'queue': " + e.getMessage(), e);
		}

		return batchQueue;
	}

	public static String formatMaxMemoryPerNode(Double memoryLimit) {
	    return FileUtils.byteCountToDisplaySize((long)(memoryLimit.longValue() * Math.pow(2, 30)));
	}

	public static Double parseMaxMemoryPerNode(String memoryLimit) throws NumberFormatException
	{
		if (StringUtils.isEmpty(memoryLimit)) {
			throw new NumberFormatException("Memory limit cannot be null or empty.");
		}

		Double returnValue = new Double(-1);

		// default numeric values to GB
		if (NumberUtils.isNumber(memoryLimit)) {
			memoryLimit = memoryLimit + "GB";
		}
		
		memoryLimit = memoryLimit.toUpperCase()
				.replaceAll(",", "")
				.replaceAll(" ", "");


	    Pattern patt = Pattern.compile("([\\d.-]+)([EPTGM]B)", Pattern.CASE_INSENSITIVE);
	    Matcher matcher = patt.matcher(memoryLimit);
	    Map<String, Integer> powerMap = new HashMap<String, Integer>();
	    powerMap.put("EB", 3);
	    powerMap.put("PB", 2);
	    powerMap.put("TB", 1);
	    powerMap.put("GB", 0);
	    if (matcher.find()) {
			String number = matcher.group(1);
			int pow = powerMap.get(matcher.group(2).toUpperCase());
			BigDecimal bytes = new BigDecimal(number);
			bytes = bytes.multiply(BigDecimal.valueOf(1024).pow(pow));
			returnValue = bytes.doubleValue();
	    } else {
	    	throw new NumberFormatException("Invalid number format.");
	    }
	    return returnValue;
	}

	public BatchQueue clone()
	{
		BatchQueue queue = new BatchQueue();
		queue.name = name;
		queue.mappedName = getMappedName();
		queue.description = getDescription();
		queue.maxJobs = getMaxJobs();
		queue.maxUserJobs = getMaxUserJobs();
		queue.maxNodes = getMaxNodes();
		queue.maxProcessorsPerNode = getMaxProcessorsPerNode();
		queue.maxMemoryPerNode = getMaxMemoryPerNode();
		queue.customDirectives = getCustomDirectives();
		queue.systemDefault = isSystemDefault();
		return queue;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((executionSystem == null) ? 0 : executionSystem.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BatchQueue other = (BatchQueue) obj;
		if (executionSystem == null) {
			if (other.executionSystem != null)
				return false;
		} else if (!executionSystem.equals(other.executionSystem))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (mappedName == null) {
			if (other.mappedName != null)
				return false;
		} else if (!mappedName.equals(other.mappedName))
			return false;
		return true;
	}

	@Override
	public int compareTo(BatchQueue q)
	{
		if (q == null) {
			return 1;
		}
		else
		{
			if (systemDefault)
			{
				return -1;
			}
			else if (q.systemDefault)
			{
				return 1;
			}
			else if ((maxNodes == -1 ? Long.MAX_VALUE: maxNodes) == (q.maxNodes == -1 ? Long.MAX_VALUE: q.maxNodes))
				if ((maxMemoryPerNode == -1 ? Double.MAX_VALUE : maxMemoryPerNode) == (q.maxMemoryPerNode == -1 ? Double.MAX_VALUE : q.maxMemoryPerNode))
					if ((maxProcessorsPerNode == -1 ? Long.MAX_VALUE : maxProcessorsPerNode) == (q.maxProcessorsPerNode == -1 ? Long.MAX_VALUE : q.maxProcessorsPerNode))
						if (StringUtils.equals(maxRequestedTime, q.maxRequestedTime))
							if (StringUtils.equals(name, q.name))
								return (mappedName == null ? (q.mappedName == null? 0 : -1) : mappedName.compareTo(q.mappedName));
							else 
								return (name == null ? (q.name == null ? 0 : -1) : name.compareTo(q.name));
						else
							return TimeUtils.compareRequestedJobTimes(maxRequestedTime, q.maxRequestedTime);
					else
						return NumberUtils.compare((maxProcessorsPerNode == -1 ? Long.MAX_VALUE : maxProcessorsPerNode), (q.maxProcessorsPerNode == -1 ? Long.MAX_VALUE : q.maxProcessorsPerNode));
				else
					return NumberUtils.compare((maxMemoryPerNode == -1 ? Double.MAX_VALUE : maxMemoryPerNode), (q.maxMemoryPerNode == -1 ? Double.MAX_VALUE : q.maxMemoryPerNode));
			else
				return NumberUtils.compare((maxNodes == -1 ? Long.MAX_VALUE: maxNodes), (q.maxNodes == -1 ? Long.MAX_VALUE: q.maxNodes));
		}
	}	
}
