package org.iplantc.service.apps.model;

import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.ParamDef;
import org.iplantc.service.apps.Settings;
import org.iplantc.service.apps.exceptions.SoftwareException;
import org.iplantc.service.apps.model.enumerations.ParallelismType;
import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.util.TimeUtils;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.BatchQueue;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.enumerations.ExecutionType;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * A SoftwareResources is any software available to the user.
 *
 * @author Rion Dooley < dooley [at] tacc [dot] utexas [dot] edu >
 *
 */
@Entity
@Table(name = "softwares", uniqueConstraints=
	@UniqueConstraint(columnNames={"name","version","publicly_available","revision_count","tenant_id"}))
@FilterDef(name="softwareTenantFilter", parameters=@ParamDef(name="tenantId", type="string"))
@Filters(@Filter(name="softwareTenantFilter", condition="tenant_id=:tenantId"))
public class Software {

	private Long					id;							// unique id of this software
	private String					name;						// name of the software
	private String					icon;						// url of the application icon
	private ParallelismType			parallelism;				// type of application parallel, serial
	private String					version;					// version tag of the software
	private String					owner;						// who added this application
	private String					helpURI;					// url of the software's help website
	private String 					label;						// label of the app
	private String					shortDescription;			// one liner about the software
	private String					longDescription;			// detailed description of the software
	private String					tags;						// comma separated list of tags
	private String					ontology;					// comma separated list of ontological terms
	private Long					defaultNodes;				// default nodes for this app if none specified
	private Double					defaultMemoryPerNode;				// default memory per nodefor this app if none specified
	private Long					defaultProcessorsPerNode = new Long(1);		// default processors per node for this app if none provided.
	private String					defaultQueue;				// default queue to use when running app.
	private String					defaultMaxRunTime = "01:00:00";	// default time to run this app
	//private String					executionHost;		    // system on which to run this software
	private ExecutionSystem			executionSystem;			// registered system on which to run this software
	private ExecutionType			executionType;				// type of system this app should run on...in leu of no explicity exectionHost
	private StorageSystem			storageSystem;				// system on which this app's assets are stored.
	private String					deploymentPath;				// path to the deployment folder
	private String					executablePath;				// path to executable relative to the deployment dir
	private String					testPath;					// path to test executable relative to the deployment dir
	private boolean					checkpointable;				// does this app checkpoint? only descriptive
	private String					modules;					// comma separated list of LMOD modules
	private boolean					available	= true;			// is this available to users
	private Integer					revisionCount = 1;			// how many times has this app desciption been updated?
	private boolean					publiclyAvailable = false;	// is the app available to the public?
	private String					checksum;					// checksun of compressed app. only used on public apps
	private String					uuid;						// uuid to be used for metadata and provenance
	private String					tenantId;
	private List<SoftwareInput> 	inputs;				// set of inputs available to the software
	private List<SoftwareParameter>	parameters;		// set of parameters available to the software
	private List<SoftwareOutput>	outputs;			// set of outputs produced by the software
	private Set<SoftwarePermission>	permissions;	// Share permissions for this software
	private Date					created		= new Date();	// date software was added
	private Date					lastUpdated	= new Date();	// last date the software was updated

	/**
	 * No-arg constructor for JavaBean tools.
	 */
	public Software()
	{
		tenantId = TenancyHelper.getCurrentTenantId();
		inputs = new ArrayList<SoftwareInput>();
		outputs = new ArrayList<SoftwareOutput>();
		parameters = new ArrayList<SoftwareParameter>();
		permissions = new HashSet<SoftwarePermission>();
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
	 * @return the name
	 */
	@Column(name = "name", nullable = false, length = 64)
	public String getName()
	{
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name)
	{
		if (!ServiceUtils.isValid(name)) {
			throw new SoftwareException("No name specified");
		} else if (name.length() > 64) {
			throw new SoftwareException("'app.name' must be less than 64 characters.");
		}

		if (!name.equals(name.replaceAll( "[^0-9a-zA-Z\\.\\-\\_\\(\\)]" , ""))) {
			throw new SoftwareException("'app.name' may only contain alphanumeric characters, periods, and dashes.");
		}

		this.name = name;
	}

	/**
	 * @return the name
	 */
	@Column(name = "icon", nullable = true, length = 128)
	public String getIcon()
	{
		return icon;
	}

	/**
	 * @param icon
	 *            the icon to set
	 */
	public void setIcon(String icon)
	{
		if (StringUtils.length(icon) > 128) {
			throw new SoftwareException("'app.icon' must be less than 128 characters.");
		} else if (!StringUtils.isEmpty(icon)){
			try {
				new URL(icon);
			} catch (Throwable e) {
				throw new SoftwareException("'app.icon' must be a valid URL.");
			}
		}

		this.icon = icon;
	}

	/**
	 * @return the parallelism
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "parallelism", nullable = false, length = 8)
	public ParallelismType getParallelism()
	{
		return parallelism;
	}

	/**
	 * @param parallelism
	 *            the parallelism to set
	 */
	public void setParallelism(ParallelismType parallelism)
	{
		this.parallelism = parallelism;
	}

	/**
	 * @param parallelism
	 *            the parallelism to set
	 */
	public void setParallelism(String parallelism)
	{
		if (!ServiceUtils.isValid(parallelism))
		{
			throw new SoftwareException("No parallelism specified");
		}
		else
		{
			try {
				this.parallelism = ParallelismType.valueOf(parallelism.toUpperCase());
			} catch (Exception e) {
				throw new SoftwareException("Invalid parallelism value. Parallelism must be one of " +
						ServiceUtils.explode(",",Arrays.asList(ParallelismType.values())));
			}
		}
	}

	/**
	 * @return the version
	 */
	@Column(name = "version", nullable = false, length = 16)
	public String getVersion()
	{
		return version;
	}

	/**
	 * @param version
	 *            the version to set
	 */
	public void setVersion(String version)
	{
		if (StringUtils.isEmpty(version))
		{
			throw new SoftwareException("No version specified");
		}
		else
		{
			if (version.matches("((?:0|[1-9]+[\\d]*)\\.[\\.\\d]+)"))
			{
				if (version.length() > 16) {
					throw new SoftwareException("'software.version' must be less than 16 characters.");
				}
				this.version = version;
			}
			else
			{
				throw new SoftwareException("Invalid version specified");
			}
		}
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
	 */
	public void setOwner(String owner)
	{
		if (StringUtils.length(owner) > 32) {
			throw new SoftwareException("app.owner must be less than 255 characters.");
		} else if (StringUtils.isEmpty(owner)) {
			throw new SoftwareException("No app.owner specified");
		}

		this.owner = owner;
	}

	/**
	 * @return the helpURI
	 */
	@Column(name = "helpURI", length = 128)
	public String getHelpURI()
	{
		return helpURI;
	}

	/**
	 * @param helpURI
	 *            the helpURI to set
	 */
	public void setHelpURI(String helpURI)
	{
		if (StringUtils.length(helpURI) > 128) {
			throw new SoftwareException("'app.helpURI' must be less than 128 characters.");
		} else if (!StringUtils.isEmpty(helpURI)){
			try {
				new URL(helpURI);
			} catch (Throwable e) {
				throw new SoftwareException("'app.helpURI' must be a valid URL.");
			}
		}

		this.helpURI = helpURI;
	}

	/**
	 * @return the label
	 */
	@Column(name = "label", length = 64)
	public String getLabel()
	{
		return label;
	}

	/**
	 * @param label the label to set
	 */
	public void setLabel(String label)
	{
		if (StringUtils.length(label) > 64) {
			throw new SoftwareException("app.label must be less than 64 characters.");
		}

		this.label = label;
	}

	/**
	 * @return the shortDescription
	 */
	@Column(name = "short_description", length = 255)
	public String getShortDescription()
	{
		return shortDescription;
	}

	/**
	 * @param shortDescription
	 *            the shortDescription to set
	 */
	public void setShortDescription(String shortDescription)
	{
		if (StringUtils.length(shortDescription) > 255) {
			throw new SoftwareException("app.shortDescription must be less than 255 characters.");
		}

		this.shortDescription = shortDescription;
	}

	/**
	 * @return the longDescription
	 */
	@Column(name = "long_description", length = 4096)
	public String getLongDescription()
	{
		return longDescription;
	}

	/**
	 * @param longDescription
	 *            the longDescription to set
	 */
	public void setLongDescription(String longDescription)
	{
		if (StringUtils.length(longDescription) > 4096) {
			throw new SoftwareException("app.longDescription must be less than 4096 characters.");
		}

		this.longDescription = longDescription;
	}

	/**
	 * @return the tags scrubbed and inserted into a list.
	 */
	@Transient
	public List<String> getTagsAsList()
	{
		if (StringUtils.isNotEmpty(getTags()))
		{
			List<String> filteredTags = new ArrayList<String>();
			String[] tags = StringUtils.split(getTags(), ",");
			if (tags != null)
			{
				for(String tag: tags) {
					tag = StringUtils.replace(StringUtils.trimToNull(tag),"\"", "");
					if (tag != null) {
						filteredTags.add(tag);
					}
				}
			}
			return filteredTags;
		}
		else
		{
			return new ArrayList<String>();
		}
	}

	/**
	 * @return the tags
	 */
	@Column(name = "tags", length = 255)
	private String getTags()
	{
		return tags;
	}

	/**
	 * @param tags
	 *            the tags to set
	 */
	public void setTags(String tags)
	{
		if (StringUtils.length(tags) > 255) {
			throw new SoftwareException("app.tags must be less than 255 characters.");
		}

		this.tags = tags;
	}

	/**
	 * @return the ontologies scrubbed and inserted into a list.
	 */
	@Transient
	public List<String> getOntologyAsList()
	{
		if (StringUtils.isNotEmpty(getOntology()))
		{
			List<String> filteredOntologies = new ArrayList<String>();
			String[] ontologies = StringUtils.split(getOntology(), ",");
			if (ontologies != null)
			{
				for(String term: ontologies) {
					term = StringUtils.replace(StringUtils.trimToNull(term),"\"", "");
					if (term != null) {
						filteredOntologies.add(term);
					}
				}
			}
			return filteredOntologies;
		}
		else
		{
			return new ArrayList<String>();
		}
	}

	/**
	 * @return the ontology
	 */
	@Column(name = "ontology", length = 255)
	private String getOntology()
	{
		return ontology;
	}

	/**
	 * @param ontology
	 *            the ontology to set
	 */
	public void setOntology(String ontology)
	{
		if (StringUtils.length(ontology) > 255) {
			throw new SoftwareException("app.ontology must be less than 255 characters.");
		}

		this.ontology = ontology;
	}

	/**
	 * Returns the number of nodes needed to run this app. This will be used when the user does not specify
	 * a node count in the job request.
	 * @return
	 */
	@Column(name = "default_nodes", nullable=true)
	public Long getDefaultNodes() {
		return defaultNodes;
	}

	/**
	 * Set the number of nodes needed to run this app. This will be used when the user does not specify
	 * a node count in the job request.
	 *
	 * @param defaultNodes the defaultNodes to set
	 */
	public void setDefaultNodes(Long defaultNodes) {
		if (defaultNodes != null && defaultNodes < 1) {
			throw new SoftwareException("In specified, app.defaultNodeCount must be a postive integer value.");
		}

		this.defaultNodes = defaultNodes;
	}

	/**
	 * Returns the memory in GB needed to run this app. This will be used when the user does not specify
	 * a core count in the job request.
	 * @return
	 */
	@Column(name = "default_memory", nullable=true)
	public Double getDefaultMemoryPerNode() {
		return defaultMemoryPerNode;
	}

	/**
	 * Set the memory in GB needed to run this app. This will be used when the user does not specify
	 * a core count in the job request.
	 *
	 * @param defaultMemoryPerNode
	 */
	public void setDefaultMemoryPerNode(Double defaultMemoryPerNode)
	{
		if (defaultMemoryPerNode != null && defaultMemoryPerNode < 1) {
			throw new SoftwareException("If specified, app.defaultMemoryPerNode must be a postive number representing GB.");
		}

		this.defaultMemoryPerNode = defaultMemoryPerNode;
	}

	/**
	 * Returns the number of processors per node to use. This will be used when the user does not specify
	 * a core count in the job request.
	 * @return
	 */
	@Column(name = "default_procesors", nullable=true)
	public Long getDefaultProcessorsPerNode() {
		return defaultProcessorsPerNode;
	}

	/**
	 * Set the number of processors per node to use. This will be used when the user does not specify
	 * a core count in the job request.
	 * @param defaultProcessorsPerNode
	 */
	public void setDefaultProcessorsPerNode(Long defaultProcessorsPerNode)
	{
		if (defaultProcessorsPerNode != null && defaultProcessorsPerNode < 1) {
			throw new SoftwareException("In specified, app.defaultProcessorsPerNode must be a positive integer value.");
		}

		this.defaultProcessorsPerNode = defaultProcessorsPerNode;
	}

	/**
	 * Returns the name of the default queue on the execution system for this app.
	 * This will be used when the user does not specify a queue in the job request.
	 * If null, the system default queue will be used for systems using a scheduler.
	 * In other cases, this is ignored.
	 *
	 * @return
	 */
	@Column(name = "default_queue", length = 128, nullable=true)
	public String getDefaultQueue() {
		return defaultQueue;
	}

	/**
	 * Sets the name of the default queue on the execution system for this app.
	 * This will be used when the user does not specify a queue in the job request.
	 * If null, the system default queue will be used for systems using a scheduler.
	 * In other cases, this is ignored.
	 *
	 * @param defaultQueue
	 */
	public void setDefaultQueue(String defaultQueue)
	{
		if (StringUtils.length(defaultQueue) > 128) {
			throw new SoftwareException("app.defaultQueue must be less than 128 characters.");
		}

		this.defaultQueue = defaultQueue;
	}

	/**
	 * Returns the default run time for this app in hh:mm:ss format. This is used
	 * if the user does not provide a default request time in the job request. If null,
	 * the queue's maxRequestedTime will be used.
	 *
	 * @return the defaultMaxRunTime
	 */
	@Column(name = "default_requested_time", nullable = true, length = 19)
	public String getDefaultMaxRunTime()
	{
		return defaultMaxRunTime;
	}

	/**
	 * Set the default run time for this app in hh:mm:ss format. This is used
	 * if the user does not provide a default request time in the job request. If null,
	 * the queue's maxRequestedTime will be used.
	 *
	 * @param defaultMaxRunTime the requestedTime to set
	 */
	public void setDefaultMaxRunTime(String defaultMaxRunTime)
	{
		if (StringUtils.length(defaultMaxRunTime) > 19) {
			throw new SoftwareException("app.defaultMaxRunTime must be less than 19 characters.");
		}

		this.defaultMaxRunTime = defaultMaxRunTime;
	}

	/**
	 * @return the executionSystem
	 */
	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "system_id", referencedColumnName = "id")
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
	 * @param executionType
	 *            the executionType to set
	 */
	public void setExecutionType(String executionType)
	{
		if (!ServiceUtils.isValid(executionType))
		{
			throw new SoftwareException("No execution type specified");
		}
		else
		{
			try {
				this.executionType = ExecutionType.valueOf(executionType.toUpperCase());
			} catch (Exception e) {
				throw new SoftwareException("Invalid executionType value. executionType must be one of " +
						ServiceUtils.explode(",",Arrays.asList(ExecutionType.values())));
			}
		}
	}

	/**
	 * @param executionType
	 *            the executionType to set
	 */
	public void setExecutionType(ExecutionType executionType)
	{
		this.executionType = executionType;
	}

	/**
	 * @return the executionType
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "execution_type", nullable = false, length = 8)
	public ExecutionType getExecutionType()
	{
		return executionType;
	}

	/**
	 * @return the storageSystem
	 */
	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "storage_system_id", referencedColumnName = "id")
    public StorageSystem getStorageSystem()
	{
		return storageSystem;
	}

	/**
	 * @param storageSystem the storageSystem to set
	 */
	public void setStorageSystem(StorageSystem storageSystem)
	{
		this.storageSystem = storageSystem;
	}

	/**
	 * @return the deploymentPath
	 */
	@Column(name = "deployment_path", nullable = false, length = 255)
	public String getDeploymentPath()
	{
		return StringUtils.removeEnd(deploymentPath, "/");
	}

	/**
	 * @param deploymentPath
	 *            the deploymentPath to set
	 */
	public void setDeploymentPath(String deploymentPath)
	{
		if (StringUtils.length(deploymentPath) > 255) {
			throw new SoftwareException("app.deploymentPath must be less than 255 characters.");
		} else if (StringUtils.isEmpty(deploymentPath)) {
			throw new SoftwareException("No app.deploymentPath specified");
		}

		this.deploymentPath = StringUtils.removeEnd(deploymentPath, "/");
	}

	/**
	 * @return the executablePath
	 */
	@Column(name = "executable_path", nullable = false, length = 255)
	public String getExecutablePath()
	{
		return executablePath;
	}

	/**
	 * @param executablePath
	 *            the executablePath to set
	 */
	public void setExecutablePath(String executablePath)
	{
		if (StringUtils.length(executablePath) > 255) {
			throw new SoftwareException("app.executablePath must be less than 255 characters.");
		} else if (StringUtils.isEmpty(executablePath)) {
			throw new SoftwareException("No app.executablePath specified");
		}

		this.executablePath = executablePath;
	}

	/**
	 * @return the testPath
	 */
	@Column(name = "test_path", nullable = false, length = 255)
	public String getTestPath()
	{
		return testPath;
	}

	/**
	 * @param testPath
	 *            the testPath to set
	 */
	public void setTestPath(String testPath)
	{
		if (StringUtils.length(testPath) > 255) {
			throw new SoftwareException("app.testPath must be less than 255 characters.");
		} else if (StringUtils.isEmpty(testPath)) {
			throw new SoftwareException("No app.testPath specified");
		}

		this.testPath = testPath;
	}

	/**
	 * @return the checkpointable
	 */
	@Column(name = "checkpointable", columnDefinition = "TINYINT(1)")
	public boolean isCheckpointable()
	{
		return checkpointable;
	}

	/**
	 * @param checkpointable
	 *            the checkpointable to set
	 */
	public void setCheckpointable(boolean checkpointable)
	{
		this.checkpointable = checkpointable;
	}

	/**
	 * @return the modules scrubbed and inserted into a list.
	 */
	@Transient
	public List<String> getModulesAsList()
	{
		if (StringUtils.isNotEmpty(getModules()))
		{
			List<String> filteredModules = new ArrayList<String>();
			String[] modules = StringUtils.split(getModules(), ",");
			if (modules != null)
			{
				for(String module: modules) {
					module = StringUtils.replace(StringUtils.trimToNull(module),"\"", "");
					if (module != null) {
						filteredModules.add(module);
					}
				}
			}
			return filteredModules;
		}
		else
		{
			return new ArrayList<String>();
		}
	}

	/**
	 * @return the modules
	 */
	@Column(name = "modules", length = 255)
	private String getModules()
	{
		return modules;
	}

	/**
	 * @param modules
	 *            the modules to set
	 */
	public void setModules(String modules)
	{
		if (StringUtils.length(modules) > 255) {
			throw new SoftwareException("app.modules must be less than 255 characters long");
		}

		this.modules = modules;
	}

	/**
	 * @return the available
	 */
	@Column(name = "available", columnDefinition = "TINYINT(1)")
	public boolean isAvailable()
	{
		return available;
	}

	/**
	 * @param available
	 *            the available to set
	 */
	public void setAvailable(boolean available)
	{
		this.available = available;
	}

	/**
	 * @param revisionCount the revisionCount to set
	 */
	public void setRevisionCount(Integer revisionCount)
	{
		this.revisionCount = revisionCount;
	}

	/**
	 * @return the revisionCount
	 */
	@Column(name = "revision_count")
	public Integer getRevisionCount()
	{
		return revisionCount;
	}

	/**
	 * @param publiclyAvailable the publiclyAvailable to set
	 */
	public void setPubliclyAvailable(boolean publiclyAvailable)
	{
		this.publiclyAvailable = publiclyAvailable;
	}

	/**
	 * @return the publiclyAvailable
	 */
	@Column(name = "publicly_available", columnDefinition = "TINYINT(1)")
	public boolean isPubliclyAvailable()
	{
		return publiclyAvailable;
	}

	/**
	 * @param checksum the checksum to set
	 */
	public void setChecksum(String checksum)
	{
		this.checksum = checksum;
	}

	/**
	 * @return the checksum
	 */
	@Column(name = "checksum", length = 64)
	public String getChecksum()
	{
		return checksum;
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

	/**
	 * @return the inputs
	 */
	@OneToMany(cascade = {CascadeType.REMOVE, CascadeType.ALL})
	@Cascade({org.hibernate.annotations.CascadeType.PERSIST,
        org.hibernate.annotations.CascadeType.SAVE_UPDATE})
	@LazyCollection(LazyCollectionOption.FALSE)
//	@OrderBy("order ASC")
//	@Sort(type = SortType.COMPARATOR, comparator = SoftwareInput.class)
	public List<SoftwareInput> getInputs()
	{
		return inputs;
	}

	/**
	 * Sorts inputs by their `ordered` value.
	 * @return
	 */
	@Transient
	public List<SoftwareInput> getOrderedInputs()
	{
		List<SoftwareInput> copiedList = new ArrayList<SoftwareInput>(getInputs());
		Collections.sort(copiedList);
		return copiedList;
	}

	/**
	 * @param inputs
	 *            the inputs to set
	 */
	public void setInputs(List<SoftwareInput> inputs)
	{
		this.inputs = inputs;
	}

	/**
	 * @param input
	 *            the inputs to set
	 */
	public void addInput(SoftwareInput input)
	{
		input.setSoftware(this);
		this.inputs.add(input);
	}

	/**
	 * Returns the input with the given key
	 * @param name
	 * @return
	 */
	@Transient
	public SoftwareInput getInput(String name)
	{
		SoftwareInput input = null;

		for(SoftwareInput in : this.inputs)
		{
			if (in.getKey().equals(name))
				input = in;
		}
		return input;
	}

	/**
	 * @return the parameters
	 */
	@OneToMany(cascade = {CascadeType.REMOVE, CascadeType.ALL})
	@Cascade({org.hibernate.annotations.CascadeType.PERSIST,
        org.hibernate.annotations.CascadeType.SAVE_UPDATE})
	@LazyCollection(LazyCollectionOption.FALSE)
	@OrderBy("order ASC")
//	@Sort(type = SortType.COMPARATOR, comparator = SoftwareParameter.class)
	public List<SoftwareParameter> getParameters()
	{
		return parameters;
	}

	/**
	 * Sorts parameters by their `ordered` value.
	 * @return
	 */
	@Transient
	public List<SoftwareParameter> getOrderedParameters()
	{
		List<SoftwareParameter> copiedList = new ArrayList<SoftwareParameter>(getParameters());
		Collections.sort(copiedList);
		return copiedList;
	}

	/**
	 * @param parameters
	 *            the parameters to set
	 */
	public void setParameters(List<SoftwareParameter> parameters)
	{
		this.parameters = parameters;
	}

	/**
	 * @param parameter
	 *            the parameters to set
	 */
	public void addParameter(SoftwareParameter parameter)
	{
		parameter.setSoftware(this);
		this.parameters.add(parameter);
	}

	/**
	 * Returns the parameter with the given key
	 * @param name
	 * @return
	 */
	@Transient
	public SoftwareParameter getParameter(String name)
	{
		SoftwareParameter param = null;

		for(SoftwareParameter p : this.parameters)
		{
			if (p.getKey().equals(name))
				param = p;
		}
		return param;
	}

	/**
	 * @return the outputs
	 */
	@OneToMany(cascade = {CascadeType.REMOVE, CascadeType.ALL})
	@Cascade({org.hibernate.annotations.CascadeType.PERSIST,
        org.hibernate.annotations.CascadeType.SAVE_UPDATE})
	@LazyCollection(LazyCollectionOption.FALSE)
	@OrderBy("order ASC")
//	@Sort(type = SortType.COMPARATOR, comparator = SoftwareOutput.class)
	public List<SoftwareOutput> getOutputs()
	{
		return outputs;
	}

	/**
	 * Sorts outputs by their `ordered` value.
	 * @return
	 */
	@Transient
	public List<SoftwareOutput> getOrderedOutputs()
	{
		List<SoftwareOutput> copiedList = new ArrayList<SoftwareOutput>(getOutputs());
		Collections.sort(copiedList);
		return copiedList;
	}

	/**
	 * @param outputs
	 *            the outputs to set
	 */
	public void setOutputs(List<SoftwareOutput> outputs)
	{
		this.outputs = outputs;
	}

	/**
	 * @param output
	 *            the outputs to set
	 */
	public void addOutput(SoftwareOutput output)
	{
		output.setSoftware(this);
		this.outputs.add(output);
	}

	/**
	 * @return the permissions
	 */
//	@OneToMany(cascade = {CascadeType.REMOVE, CascadeType.ALL})
//	@Cascade({org.hibernate.annotations.CascadeType.PERSIST,
//        org.hibernate.annotations.CascadeType.SAVE_UPDATE})
//	@LazyCollection(LazyCollectionOption.FALSE)
	@OneToMany(cascade = {CascadeType.ALL}, mappedBy = "software", fetch=FetchType.EAGER, orphanRemoval=true)
	public Set<SoftwarePermission> getPermissions()
	{
		return permissions;
	}

	/**
	 * @param permissions the permissions to set
	 */
	public void setPermissions(Set<SoftwarePermission> permissions)
	{
		this.permissions = permissions;
	}

	/**
	 * @return the created
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created", nullable = false, length = 19)
	public Date getCreated()
	{
		return created;
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
	 * Returns the unique name of the software. The unique name is
	 * given by the name + "-" + version. If the software is public,
	 * a "u" + revisionCount is appended to the end.
	 *
	 * @return unique name of this software
	 */
	@Transient
	public String getUniqueName()
	{
		String uniqueName = getName() + "-" + getVersion();
		if (isPubliclyAvailable()) {
			uniqueName += "u" + getRevisionCount();
		}
		return uniqueName;
	}

	/**
	 * Checks for equivalence based on unique name and ownership.
	 * Unique name should be sufficient.
	 */
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (! ( o instanceof Software ))
			return false;
		final Software sw = (Software) o;
		if (!getUniqueName().equalsIgnoreCase(sw.getUniqueName()))
			return false;
		if (!owner.equalsIgnoreCase(sw.owner))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode()
	{
		return ( new String(this.getName() + this.version) ).hashCode();
	}

	/**
	 * Returns unique name of this app.
	 */
	public String toString()
	{
		return getUniqueName();
	}

	/**
	 * Compares two software objects based on name, version, revision, and id.
	 *
	 * @param Software object
	 * @return 1 if greater, 0 if equal, -1 if less.
	 */
	public int compareTo(Object o)
	{
		if (o instanceof Software) {
			int nameComp = this.name.compareTo( ( (Software) o ).name);
			if (nameComp == 0) {
				int versionComp = this.version.compareTo( ( (Software) o ).version);
				if (versionComp == 0) {
					int revisionComp = this.revisionCount.compareTo( ( (Software) o ).revisionCount);
					if (revisionComp == 0) {
						return this.id.compareTo( ( (Software) o ).id);
					} else {
						return revisionComp;
					}
				} else {
					return versionComp;
				}
			} else {
				return nameComp;
			}
		} else {
			return 0;
		}
	}

	/**
	 * Creates a shallow copy of the software object. Permissions
	 * are not copied over.
	 */
	public Software clone()
	{
		Software sw = new Software();

		sw.name = name;
		sw.uuid = new AgaveUUID(UUIDType.APP).toString();
		sw.parallelism = getParallelism();
		sw.icon = getIcon();
		sw.version = getVersion();
		sw.owner = getOwner();
		sw.helpURI = getHelpURI();
		sw.label = getLabel();
		sw.shortDescription = getShortDescription();
		sw.longDescription = getLongDescription();
		sw.tags = getTags();
		sw.ontology = getOntology();
		sw.defaultProcessorsPerNode = getDefaultProcessorsPerNode();
		sw.defaultMemoryPerNode = getDefaultMemoryPerNode();
		sw.defaultNodes = getDefaultNodes();
		sw.defaultMaxRunTime = getDefaultMaxRunTime();
		sw.defaultQueue = getDefaultQueue();
		sw.executionSystem = getExecutionSystem();
		sw.executionType = getExecutionType();
		sw.executablePath = getExecutablePath();
		sw.deploymentPath = getDeploymentPath();
		sw.storageSystem = getStorageSystem();
		sw.executablePath = getExecutablePath();
		sw.testPath = getTestPath();
		sw.checkpointable = isCheckpointable();
		sw.modules = getModules();
		sw.available = isAvailable();
		sw.revisionCount = getRevisionCount();
		sw.publiclyAvailable = false;
		sw.tenantId = getTenantId();

		sw.inputs = new ArrayList<SoftwareInput>();
		for (SoftwareInput input: getInputs()) {
			sw.addInput(input.clone(sw));
		}

		sw.parameters = new ArrayList<SoftwareParameter>();
		for (SoftwareParameter param: getParameters()) {
			sw.addParameter(param.clone(sw));
		}

		sw.outputs = new ArrayList<SoftwareOutput>();
		for (SoftwareOutput output: getOutputs()) {
			sw.addOutput(output.clone(sw));
		}

		sw.created = new Date();
		sw.lastUpdated = new Date();

		return sw;
	}

	/**
	 * Serializes the software object to json.
	 * @return JSON string representation of an object.
	 * @throws JSONException
	 */
	public String toJSON() throws JSONException
	{
		JSONWriter writer = new JSONStringer()
		.object()
			.key("id").value(getUniqueName())
			.key("name").value(name)
			.key("icon").value(icon)
			.key("uuid").value(uuid)
			.key("parallelism").value(parallelism)
			.key("defaultProcessorsPerNode").value(defaultProcessorsPerNode)
			.key("defaultMemoryPerNode").value(defaultMemoryPerNode)
			.key("defaultNodeCount").value(defaultNodes)
			.key("defaultMaxRunTime").value(defaultMaxRunTime)
			.key("defaultQueue").value(defaultQueue)
			.key("version").value(version)
			.key("revision").value(revisionCount)
			.key("isPublic").value(isPubliclyAvailable())
			.key("helpURI").value(helpURI)
			.key("label").value(label)
			.key("owner").value(owner)
			.key("shortDescription").value(shortDescription)
			.key("longDescription").value(longDescription)
			.key("tags").array();
				for(String tag: getTagsAsList()) {
					writer.value(tag);
				}
				writer.endArray()
			.key("ontology").array();
				for(String term: getOntologyAsList()) {
					writer.value(term);
				}
				writer.endArray()
			.key("executionType").value(executionType.name())
			.key("executionSystem").value(executionSystem.getSystemId())
			.key("deploymentPath").value(deploymentPath)
			.key("deploymentSystem").value(storageSystem.getSystemId())
			.key("templatePath").value(executablePath)
			.key("testPath").value(testPath)
			.key("checkpointable").value(checkpointable)
			.key("lastModified").value(new DateTime(lastUpdated).toString())
			.key("modules").array();
				for(String module: getModulesAsList()) {
					writer.value(module);
				}
				writer.endArray()
			.key("available").value(isAvailable());

		writer.key("inputs").array();
		for (SoftwareInput input : getOrderedInputs())
		{
			input.printJSON(writer);
		}
		writer.endArray();

		writer.key("parameters").array();
		for (SoftwareParameter param : getOrderedParameters())
		{
			param.printJSON(writer);
		}
		writer.endArray();

		writer.key("outputs").array();
		for (SoftwareOutput output : getOrderedOutputs())
		{
			output.printJSON(writer);
		}
		writer.endArray()
			.key("_links").object()
	        	.key("self").object()
	        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_APPS_SERVICE) + getUniqueName())
		        .endObject()
		        .key("executionSystem").object()
	        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_SYSTEM_SERVICE) + getExecutionSystem().getSystemId())
	        	.endObject()
	        	.key("storageSystem").object()
	        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_SYSTEM_SERVICE) + getStorageSystem().getSystemId())
	        	.endObject()
	        	.key("history").object()
	    			.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_APPS_SERVICE) + getUniqueName() + "/history")
	    		.endObject()
	        	.key("metadata").object()
	    			.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_METADATA_SERVICE) + "data/?q=" + URLEncoder.encode("{\"associationIds\":\"" + uuid + "\"}"))
	    		.endObject()
	        	.key("owner").object()
					.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_PROFILE_SERVICE) + getOwner())
				.endObject()
				.key("permissions").object()
	        		.key("href").value(TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_APPS_SERVICE) + getUniqueName() + "/pems")
		        .endObject()
		        
	        .endObject()
		.endObject();

		return writer.toString();
	}

	@SuppressWarnings("unused")
	public static Software fromJSON(JSONObject json2, String owner)
	throws SoftwareException, JSONException
	{
		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonNode;
		try
		{
			jsonNode = mapper.readTree(json2.toString());
		}
		catch (Throwable e)
		{
			throw new SoftwareException("Invalid json supplied as an app description.", e);
		}

		Software software = new Software();
        software.setOwner(owner);
        software.setUuid(new AgaveUUID(UUIDType.APP).toString());

        if (jsonNode.hasNonNull("name") && jsonNode.get("name").isTextual())
        {
        	software.setName(jsonNode.get("name").asText());
		}
		else
		{
			throw new SoftwareException("Invalid 'app.name' value. Please enter a valid name for this app.");
		}

		if (jsonNode.hasNonNull("parallelism") && jsonNode.get("parallelism").isTextual())
		{
			software.setParallelism(jsonNode.get("parallelism").asText());
		}
		else
		{
			throw new SoftwareException("No 'app.parallelism' attribute found in app description. Parallelism must be one of " +
						ServiceUtils.explode(",",Arrays.asList(ParallelismType.values())));
		}

		if (jsonNode.hasNonNull("version"))
		{
			if (jsonNode.get("version").isTextual()) {
				String version = jsonNode.get("version").asText();
				Pattern p = Pattern.compile("\\d+[.\\d+]+");
				Matcher m = p.matcher(version);

				if (m.matches()) {
					software.setVersion(version);
				} else {
					throw new SoftwareException("Invalid 'app.version' value. Please provide a version in x.x.x format");
				}
			} else {
				throw new SoftwareException("Invalid 'app.version' value. Please provide a version in x.x.x format");
			}
		}
		else
		{
			throw new SoftwareException("No version attribute found in software description");
		}

		if (jsonNode.hasNonNull("icon"))
		{
			if (jsonNode.get("icon").isTextual())
				software.setIcon(jsonNode.get("icon").asText());
			else {
				throw new SoftwareException("If specified, 'app.icon' must be a valid URL.");
			}
		} else {
			software.setIcon(null);
		}

		if (jsonNode.hasNonNull("helpURI"))
		{
			if (jsonNode.get("helpURI").isTextual())
				software.setHelpURI(jsonNode.get("helpURI").asText());
			else
				throw new SoftwareException("If specified, 'app.helpURI' must be a valid URL.");
		}else {
			software.setHelpURI(null);
		}

		if (jsonNode.hasNonNull("label"))
		{
			if (jsonNode.get("label").isTextual())
        		software.setLabel(jsonNode.get("label").asText());
        	else
        		throw new SoftwareException("If specified, 'app.label' must be a valid string.");
		} else {
			software.setLabel(null);
		}

		if (jsonNode.hasNonNull("shortDescription"))
		{
			if (jsonNode.get("shortDescription").isTextual())
        		software.setShortDescription(jsonNode.get("shortDescription").asText());
        	else
        		throw new SoftwareException("If specified, 'app.shortDescription' must be a valid string.");
		}else {
			software.setShortDescription(null);
		}

		if (jsonNode.hasNonNull("longDescription"))
		{
			if (jsonNode.get("longDescription").isTextual())
        		software.setLongDescription(jsonNode.get("longDescription").asText());
        	else
        		throw new SoftwareException("If specified, 'app.longDescription' must be a valid string.");
		} else {
			software.setLongDescription(null);
		}

		if (jsonNode.hasNonNull("tags"))
		{
			if (jsonNode.get("tags").isArray())
			{
				ArrayNode tagsArrayNode = (ArrayNode)jsonNode.get("tags");
				List<String> tagList = new ArrayList<String>();
				for(int i=0; i<tagsArrayNode.size(); i++)
				{
					JsonNode child = tagsArrayNode.get(i);
					if (!child.isTextual()) {
						throw new SoftwareException("Invalid 'app.tags' value. If specified, 'app.tags' must be a json array of strings");
					} else {
						if (!tagList.contains(child.asText())) {
							tagList.add(child.asText());
						}
					}
				}
				software.setTags(StringUtils.join(tagList, ","));
			}
			else {
				throw new SoftwareException("Invalid 'app.tags' value. If specified, 'app.tags' must be a json array of strings");
			}
		}
		else {
			software.setTags(null);
		}

		if (jsonNode.hasNonNull("ontology"))
		{
			if (jsonNode.get("ontology").isArray())
			{
				ArrayNode ontologyArrayNode = (ArrayNode)jsonNode.get("ontology");
				List<String> ontologyList = new ArrayList<String>();
				for(int i=0; i<ontologyArrayNode.size(); i++)
				{
					JsonNode child = ontologyArrayNode.get(i);
					if (!child.isTextual()) {
						throw new SoftwareException("Invalid 'app.ontology' value. If specified, "
						 		+ "'app.ontology' must be a json array of URL");
					} else {
						if (StringUtils.isNotEmpty(child.asText()) &&
								!ontologyList.contains(ServiceUtils.enquote(child.asText()))) {
							ontologyList.add(ServiceUtils.enquote(child.asText()));
						}
					}
				}
				software.setOntology(StringUtils.join(ontologyList, ","));
			}
			else {
				throw new SoftwareException("Invalid 'app.ontology' value. If specified, "
						+ "'app.ontology' must be a json array of URL");
			}
		}
		else {
			software.setOntology(null);
		}

		if (jsonNode.hasNonNull("executionSystem"))
		{
			if (jsonNode.get("executionSystem").isTextual())
			{
				String executionSystem = jsonNode.get("executionSystem").asText();

				RemoteSystem system = new SystemDao().findBySystemId(executionSystem);

				if (system == null)
				{
					throw new SoftwareException("Invalid executionSystem value, "
					        + executionSystem + ". Please specify a valid system id "
			                + "representing an execution system "
							+ "on which this app should run.");
				}
				else
				{
					if (!system.getType().equals(RemoteSystemType.EXECUTION)) {
						throw new SoftwareException("Invalid executionSystem value. " +
								"The specified executionSystem value, " + system.getSystemId() +
								", is not an execution system. Please specify a valid system id " +
								"representing an execution system on which this app should run.");
					} else {
						software.setExecutionSystem((ExecutionSystem)system);
					}
				}
			}
			else
			{
				throw new SoftwareException("Please specify a valid system id representing an execution system " +
						"on which this app should run.");
			}
		}
		else
		{
			throw new SoftwareException("No 'app.executionSystem' attribute found in software description");
		}

		if (jsonNode.hasNonNull("executionType"))
		{
			if (jsonNode.get("executionType").isTextual())
			{
				software.setExecutionType(jsonNode.get("executionType").asText());
				if (!software.getExecutionSystem().getExecutionType().getCompatibleExecutionTypes().contains(software.getExecutionType())) {
					throw new SoftwareException("Invalid executionType value. The execution system " +
							software.getExecutionSystem().getSystemId() + " execution type is incompatible with " +
							"the executionType provided. Please specify one of " +
							ServiceUtils.explode(", ", software.getExecutionSystem().getExecutionType().getCompatibleExecutionTypes()));
				}
			}
			else
			{
				throw new SoftwareException("Invalid executionType value. executionType must be one of " +
						ServiceUtils.explode(", ",Arrays.asList(ExecutionType.values())));
			}
		}
		else
		{
			throw new SoftwareException("No executionType attribute found in software description");
		}

		BatchQueue currentDefaultQueue = software.getExecutionSystem().getDefaultQueue();
		if (jsonNode.hasNonNull("defaultQueue"))
		{
			List<String> queues = new ArrayList<String>();
			for (BatchQueue q: software.getExecutionSystem().getBatchQueues()) {
				queues.add(q.getName());
			}

			if (jsonNode.get("defaultQueue").isTextual())
			{
				String appDefaultQueue = jsonNode.get("defaultQueue").asText();
				if (queues.contains(appDefaultQueue)) {
					software.setDefaultQueue(appDefaultQueue);
					currentDefaultQueue = software.getExecutionSystem().getQueue(appDefaultQueue);
				} else {
					throw new SoftwareException("Invalid defaultQueue value. If specified, one of the queues on " +
							software.getExecutionSystem().getSystemId() + " must be specified. The available queues " +
							"are: " + StringUtils.join(queues, ","));
				}
			}
			else
			{
				throw new SoftwareException("Invalid defaultQueue value. If specified, one of the queues on " +
						software.getExecutionSystem().getSystemId() + " must be specified. The available queues " +
						"are: " + StringUtils.join(queues, ","));
			}
		}
		else
		{
			software.setDefaultQueue(null);
		}

		if (jsonNode.hasNonNull("defaultNodeCount"))
		{
			if (jsonNode.get("defaultNodeCount").isIntegralNumber())
			{
				Long nodes = jsonNode.get("defaultNodeCount").asLong();
				if (software.getExecutionType().equals(ParallelismType.SERIAL))
				{
					if (nodes != -1 || nodes == 1)
					{
						throw new SoftwareException("Invalid defaultNodeCount value. If specified, defaultNodeCount must be 1 " +
							"for apps with SERIAL executionType.");
					} else {
						software.setDefaultNodes(nodes);
					}
				}
				else
				{
					// check that the ppn does not exceed the queue limits
					if (!StringUtils.isEmpty(software.getDefaultQueue()))
					{
						BatchQueue defaultQueue = software.getExecutionSystem().getQueue(software.getDefaultQueue());

						if (defaultQueue.getMaxNodes() == -1  ||
								defaultQueue.getMaxNodes() >= nodes)
						{
							software.setDefaultNodes(nodes);
						}
						else
						{
							throw new SoftwareException("Invalid defaultNodeCount value. If specified, defaultNodeCount must "
									+ "be a postitive integer value or -1 for no limit.");
						}
					}
					else // no queue default value specified. allow it if it is a valid value
					{
						if (nodes == -1 || nodes > 0) {
							software.setDefaultNodes(nodes);
						}
						else {
							throw new SoftwareException("Invalid defaultNodeCount value. If specified, defaultNodeCount must "
									+ "be a postitive integer value or -1 for no limit.");
						}
					}
				}
			}
			else
			{
				throw new SoftwareException("Invalid defaultNodeCount value. If specified, defaultNodeCount must be a " +
						"positive integer value.");
			}
		}
		else
		{
			software.setDefaultNodes((long)1);
		}

		if (jsonNode.hasNonNull("defaultProcessorsPerNode"))
		{
			if (jsonNode.get("defaultProcessorsPerNode").isIntegralNumber())
			{
				Long processors = jsonNode.get("defaultProcessorsPerNode").asLong();
//				if (software.getExecutionType().equals(ParallelismType.SERIAL))
//				{
//					if (processors != -1 || processors == 1) {
//						throw new SoftwareException("Invalid defaultProcessorsPerNode value. "
//							+ "If specified, defaultProcessorsPerNode must be 1 " +
//							"for apps with SERIAL executionType.");
//					}
//					else
//					{
//						software.setDefaultProcessorsPerNode(processors);
//					}
//				}
//				else
//				{
					// check that the ppn does not exceed the queue limits
					if (!StringUtils.isEmpty(software.getDefaultQueue()))
					{
						BatchQueue defaultQueue = software.getExecutionSystem().getQueue(software.getDefaultQueue());

						if (defaultQueue.getMaxProcessorsPerNode() == -1  ||
								defaultQueue.getMaxProcessorsPerNode() >= processors)
						{
							software.setDefaultProcessorsPerNode(processors);
						}
						else
						{
							throw new SoftwareException("Invalid defaultProcessorsPerNode value. "
									+ "If specified, defaultProcessorsPerNode must "
									+ "be a postitive integer value or -1 for no limit.");
						}
					}
					else // no queue default value specified. allow it if it is a valid value
					{
						if (processors == -1 || processors > 0) {
							software.setDefaultProcessorsPerNode(processors);
						}
						else {
							throw new SoftwareException("Invalid defaultProcessorsPerNode value. "
									+ "If specified, defaultProcessorsPerNode must "
									+ "be a postitive integer value or -1 for no limit.");
						}
					}
//				}
			}
			else
			{
				throw new SoftwareException("Invalid defaultProcessorsPerNode value. If specified, defaultProcessors must be a " +
						"positive integer value or -1 for no limit.");
			}
		}
		else
		{
			software.setDefaultProcessorsPerNode((long)1);
		}

		if (jsonNode.hasNonNull("defaultMemoryPerNode"))
		{
			double defaultMemory = 0;
			if (jsonNode.get("defaultMemoryPerNode").isNumber())
			{
				defaultMemory = jsonNode.get("defaultMemoryPerNode").asDouble();

			}
			else if (jsonNode.get("defaultMemoryPerNode").isTextual())
			{
				try {
					defaultMemory = BatchQueue.parseMaxMemoryPerNode(jsonNode.get("defaultMemoryPerNode").asText());
				}
				catch (Throwable e) {
					throw new SoftwareException("Invalid defaultMemoryPerNode value. If specified, " +
							"'defaultQueue.defaultMemoryPerNode' should be a postive value specified in ###.#[EPTGM]B format.");
				}
			}
			else
			{
				throw new SoftwareException("Invalid defaultMemoryPerNode value. If specified, " +
						"app.defaultMemoryPerNode should be a postive value specified in ###.#[EPTGM]B format.");
			}

			if (!StringUtils.isEmpty(software.getDefaultQueue()))
			{
				BatchQueue defaultQueue = software.getExecutionSystem().getQueue(software.getDefaultQueue());

				if (defaultQueue.getMaxMemoryPerNode() == -1  ||
						defaultQueue.getMaxMemoryPerNode() >= defaultMemory)
				{
					software.setDefaultMemoryPerNode(defaultMemory);
				}
				else
				{
					throw new SoftwareException("Invalid defaultMemoryPerNode value. "
							+ "If specified, defaultMemoryPerNode must "
							+ "be a postitive integer value less than the max memory per node of the target system queue.");
				}
			}
			else // no queue default value specified. allow it if it is a valid value
			{
				if (defaultMemory == -1 || defaultMemory > 0) {
					software.setDefaultMemoryPerNode(defaultMemory);
				}
				else {
					throw new SoftwareException("Invalid defaultMemoryPerNode value. "
							+ "If specified, defaultMemoryPerNode must "
							+ "be a postitive integer value or -1 for no limit.");
				}
			}
		}
		else
		{
			software.setDefaultMemoryPerNode(1.0);
		}

		if (jsonNode.hasNonNull("defaultMaxRunTime"))
		{
			if (jsonNode.get("defaultMaxRunTime").isTextual())
			{
				String defaultRequestedTime = jsonNode.get("defaultMaxRunTime").asText();
				if (!TimeUtils.isValidRequestedJobTime(defaultRequestedTime))
				{
					throw new SoftwareException(
							"Invalid defaultMaxRunTime value. "
							+ "If specified, defaultMaxRunTime must be in the form of hh:mm:ss.");
				}

				// check that the time does not exceed the default queue limit
				if (!StringUtils.isEmpty(software.getDefaultQueue()))
				{
					BatchQueue defaultQueue = software.getExecutionSystem().getQueue(software.getDefaultQueue());

					if (!StringUtils.isEmpty(defaultQueue.getMaxRequestedTime()) &&
							TimeUtils.compareRequestedJobTimes(defaultRequestedTime, defaultQueue.getMaxRequestedTime()) > 0)
					{
						throw new SoftwareException(
								"Invalid defaultMaxRunTime value. "
								+ "The defaultMaxRunTime must not exceed the defaultQueue max requested time of "
								+ defaultQueue.getMaxRequestedTime());
					}
				}

				software.setDefaultMaxRunTime(defaultRequestedTime);
			}
        	else {
        		throw new SoftwareException("Invalid defaultMaxRunTime value. "
        				+ "If specified, defaultMaxRunTime must be in the form of hh:mm:ss.");
        	}
		}
		else {
			software.setDefaultMaxRunTime(null);
		}

		RemoteSystem deploymentSystem = null;
		if (jsonNode.hasNonNull("deploymentSystem"))
		{
			if (jsonNode.get("deploymentSystem").isTextual())
			{
				String deploymentSystemId = jsonNode.get("deploymentSystem").asText();

				deploymentSystem = new SystemDao().findBySystemId(deploymentSystemId);

				if (deploymentSystem == null)
				{
					throw new SoftwareException("Invalid deploymentSystem value, "
					        + deploymentSystemId + ". Please specify a valid storage system id " +
							"on which the assets of this application are stored.");
				}
				else
				{
					if (!deploymentSystem.getType().equals(RemoteSystemType.STORAGE)) {
						throw new SoftwareException("Invalid deploymentSystem value. " +
								deploymentSystem.getSystemId() + " " +
								"is not a storage system. Please specify a valid storage system id " +
								"on which the assets of this application are stored.");
					} else {
						// relaxing requirement that the deployment system be a storage system.
						software.setStorageSystem((StorageSystem)deploymentSystem);
					}
				}
			}
			else
			{
				throw new SoftwareException("Please specify a valid storage system id " +
						"on which the assets of this application are stored.");
			}
		}
		else
		{
			// apply user's default storage
			deploymentSystem = new SystemManager().getUserDefaultStorageSystem(owner);
			software.setStorageSystem((StorageSystem)deploymentSystem);
		}

		if (jsonNode.hasNonNull("deploymentPath"))
		{
			if (jsonNode.get("deploymentPath").isTextual())
			{
				String deploymentPath = jsonNode.get("deploymentPath").asText();
				RemoteDataClient rdc;
				try
				{
					rdc = deploymentSystem.getRemoteDataClient();
					software.setDeploymentPath(deploymentPath);
				}
				catch (Exception e)
				{
					throw new SoftwareException("Failed to resolve deploymentPath on remote deploymentSystem", e);
				}
			}
        	else {
        		throw new SoftwareException("Invalid deploymentPath value. Please specify the remote path to your app folder.");
        	}
		}
		else
		{
			throw new SoftwareException("No deploymentPath attribute found in app description");
		}

		if (jsonNode.hasNonNull("templatePath"))
		{
			if (jsonNode.get("templatePath").isTextual())
			{
				String templatePath = jsonNode.get("templatePath").asText();
				if (templatePath.startsWith(software.getDeploymentPath())) {
					templatePath = templatePath.substring(software.getDeploymentPath().length());
					if (!templatePath.startsWith("/"))
						templatePath = "/" + templatePath;
				}
				software.setExecutablePath(templatePath);
			}
			else
			{
				throw new SoftwareException("Invalid templatePath value. Please specify the path to " +
						"your wrapper template relative to the deploymentPath.");
			}
		}
		else
		{
			throw new SoftwareException("No templatePath attribute found in app description");
		}

		if (jsonNode.hasNonNull("testPath"))
		{
			if (jsonNode.get("testPath").isTextual())
			{
				String testPath = jsonNode.get("testPath").asText();
				if (StringUtils.startsWith(testPath, software.getDeploymentPath())) {
					testPath = testPath.substring(software.getDeploymentPath().length());
					if (!testPath.startsWith("/"))
						testPath = "/" + testPath;
				}
				software.setTestPath(testPath);
			}
			else
			{
				throw new SoftwareException("Invalid testPath value. Please specify the path to " +
						"your wrapper template relative to the deploymentPath.");
			}
		}
		else
		{
			throw new SoftwareException("No testPath attribute found in software description");
		}

		if (jsonNode.hasNonNull("checkpointable"))
		{
			if (jsonNode.get("checkpointable").isBoolean())
				software.setCheckpointable(jsonNode.get("checkpointable").asBoolean());
			else
				throw new SoftwareException("Invalid checkpointable value. Please specify " +
						"either true or false based on the ability of this app to be checkpointed.");
		} else {
			software.setCheckpointable(false);
		}

		if (jsonNode.hasNonNull("modules"))
		{
			if (jsonNode.get("modules").isArray())
			{
				ArrayNode modulesArrayNode = (ArrayNode)jsonNode.get("modules");
				// use a list to preserve order
				List<String> modulesList = new ArrayList<String>();
				for(int i=0; i<modulesArrayNode.size(); i++)
				{
					JsonNode child = modulesArrayNode.get(i);
					if (!child.isTextual()) {
						throw new SoftwareException("Invalid 'app.modules' value. If specified, "
									+ "'app.modules' must be a json array of strings");
					} else {
						// we do not check for duplicates in the module list as redundancy
						// may be important to the user if trying to overwrite a distinct set
						// of variables in their environment. Order is, however, imposed by
						// the list.
						modulesList.add(child.asText());
					}
				}
				software.setModules(StringUtils.join(modulesList, ","));
			}
			else {
				throw new SoftwareException("Invalid 'app.modules' value. If specified, 'app.modules' must be a json array of strings");
			}
		}
		else {
			software.setModules(null);
		}

		if (jsonNode.hasNonNull("available"))
		{
			if (jsonNode.get("available").isBoolean())
				software.setAvailable(jsonNode.get("available").asBoolean());
			else
				throw new SoftwareException("Invalid available value. Please specify " +
						"either true or false based on the immediate availability of this app for use.");
		} else {
			software.setAvailable(true);
		}


		if (jsonNode.hasNonNull("inputs"))
		{
			if (jsonNode.get("inputs").isArray())
			{
				ArrayNode jsonInputs = (ArrayNode)jsonNode.get("inputs");
				for(int i = 0; i<jsonInputs.size(); i++)
				{
					JSONObject jsonInput;
					try
					{
						jsonInput = new JSONObject(mapper.writeValueAsString(jsonInputs.get(i)));
					}
					catch (JsonProcessingException e)
					{
						throw new SoftwareException("Invalid app inputs value. Please specify " +
								"an array of JSON objects describing the inputs for this app.");
					}
					SoftwareInput input = SoftwareInput.fromJSON(jsonInput);
					input.setSoftware(software);
					if (software.getInputs().contains(input)) {
						throw new SoftwareException("Duplicate application input key \"" + input.getKey() + "\". Input keys must be unique to the application.");
					}
					software.addInput(input);
				}
			}
			else
			{
				throw new SoftwareException("Invalid app inputs value. Please specify " +
						"an array of JSON objects describing the inputs for this app.");
			}
		}
		else
		{
			software.getInputs().clear();
		}

		if (jsonNode.hasNonNull("parameters"))
		{
			if (jsonNode.get("parameters").isArray())
			{
				ArrayNode jsonParameters = (ArrayNode)jsonNode.get("parameters");
				for(int i = 0; i<jsonParameters.size(); i++)
				{
					JSONObject jsonParameter;
					try
					{
						jsonParameter = new JSONObject(mapper.writeValueAsString(jsonParameters.get(i)));
					}
					catch (JsonProcessingException e)
					{
						throw new SoftwareException("Invalid app parameters value. Please specify " +
								"an array of JSON objects describing the parameters for this app.");
					}
					SoftwareParameter parameter = SoftwareParameter.fromJSON(jsonParameter);
					parameter.setSoftware(software);
					if (software.getParameters().contains(parameter)) {
						throw new SoftwareException("Duplicate application parameters key \"" + parameter.getKey() +
								"\". Parameter keys must be unique to the application.");
					}
					software.addParameter(parameter);
				}
			}
			else
			{
				throw new SoftwareException("Invalid app parameters value. Please specify " +
						"an array of JSON objects describing the parameters for this app.");
			}
		}
		else
		{
			software.getParameters().clear();
		}

		if (jsonNode.hasNonNull("outputs"))
		{
			if (jsonNode.get("outputs").isArray())
			{
				ArrayNode jsonOutputs = (ArrayNode)jsonNode.get("outputs");
				for(int i = 0; i<jsonOutputs.size(); i++)
				{
					JSONObject jsonOutput;
					try
					{
						jsonOutput = new JSONObject(mapper.writeValueAsString(jsonOutputs.get(i)));
					}
					catch (JsonProcessingException e)
					{
						throw new SoftwareException("Invalid app output value. Please specify " +
								"an array of JSON objects describing the outputs for this app.");
					}
					SoftwareOutput output = SoftwareOutput.fromJSON(jsonOutput);
					output.setSoftware(software);
					if (software.getOutputs().contains(output)) {
						throw new SoftwareException("Duplicate application output key \"" + output.getKey() +
								"\". Output keys must be unique to the application.");
					}
					software.addOutput(output);
				}
			}
			else
			{
				throw new SoftwareException("Invalid app output value. Please specify " +
						"an array of JSON objects describing the outputs for this app.");
			}
		}
		else
		{
			software.getOutputs().clear();
		}

		software.setCreated(new Date());
		software.setLastUpdated(software.getCreated());

		return software;
	}

	/**
	 * Determines if an app is owned by the given username
	 * @param username
	 * @return
	 */
	@Transient
	public boolean isOwnedBy(String username)
	{
		if (ServiceUtils.isValid(getOwner()) && ServiceUtils.isValid(username)) {
			return getOwner().equals(username);
		} else {
			return false;
		}
	}
}
