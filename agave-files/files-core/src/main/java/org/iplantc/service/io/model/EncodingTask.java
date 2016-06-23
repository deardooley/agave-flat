/**
 * 
 */
package org.iplantc.service.io.model;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.commons.lang3.StringUtils;
import org.iplantc.service.io.exceptions.TaskException;
import org.iplantc.service.io.model.enumerations.TransformTaskStatus;
import org.iplantc.service.io.util.ServiceUtils;
import org.iplantc.service.systems.model.RemoteSystem;

/**
 * This class represents a request to perform a transform operation on a file.
 * The transform proceeds from source format to destination format and as such
 * could be modeled as an autonomous entity with many to many relationships to
 * an EncodingTask and DecodingTask that only differ in that one stages in data
 * and one stages data out. An even better modeling would be to separate the 
 * transfer task out and reference the transform task as a foreign key
 * in the transfer task table.
 * 
 * @author dooley
 *
 */
@Entity
@Table(name = "encoding_tasks")
public class EncodingTask implements QueueTask {
	
	private Long id;
    private RemoteSystem storageSystem;
	private String sourcePath;
	private String destPath;
	private String transformName;
	private String transformFilterName;
	private String callbackKey;
	protected LogicalFile logicalFile;
	protected String owner;
	protected TransformTaskStatus status = TransformTaskStatus.TRANSFORMING_QUEUED;
	protected Date created = new Date();
	private Date lastUpdated = new Date();
	private Integer	version = 0;	// Entity version used for optimistic locking
	 
	
	public EncodingTask() {}
	
	public EncodingTask(LogicalFile file, RemoteSystem storageSystem, String sourcePath, String destPath, String transformName, String transformFilterName, String owner) {
		setLogicalFile(file);
        setStorageSystem(storageSystem);
		setSourcePath(sourcePath);
		setDestPath(destPath);
		setOwner(owner);
		setTransformName(transformName);
		setTransformFilterName(transformFilterName);
		this.callbackKey = UUID.randomUUID().toString();
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the id
	 */
	@Id
	@GeneratedValue
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return id;
	}


    /**
     * @param storageSystem the storage system to set
     */
    public void setStorageSystem(RemoteSystem storageSystem) {
        this.storageSystem = storageSystem;
    }

    /**
     * @return the storageSystem
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "system_id", referencedColumnName = "id")
    public RemoteSystem getStorageSystem() {
        return storageSystem;
    }

	/**
	 * @param sourcePath the sourcePath to set
	 */
	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	/**
	 * @return the sourcePath
	 */
	@Column(name = "source_path", nullable = false, length = 255)
	public String getSourcePath() {
		return sourcePath;
	}

	/**
	 * @param destPath the destPath to set
	 */
	public void setDestPath(String destPath) {
		this.destPath = destPath;
	}

	/**
	 * @return the destPath
	 */
	@Column(name = "dest_path", nullable = false, length = 255)
	public String getDestPath() {
		return destPath;
	}

	/**
	 * @param transformName the transformName to set
	 */
	public void setTransformName(String transformName) {
		if (!ServiceUtils.isValid(transformName)) {
			throw new TaskException("Invalid transform name");
		}
		this.transformName = transformName;
	}

	/**
	 * @return the transformName
	 */
	@Column(name = "transform_filter_name", nullable = false, length = 32)
	public String getTransformName() {
		return transformName;
	}

	/**
	 * @return the handler
	 */
	@Column(name = "transform_name", nullable = false, length = 32)
	public String getTransformFilterName() {
		return transformFilterName;
	}

	/**
	 * @param handler the handler to set
	 */
	public void setTransformFilterName(String transformFilterName) {
		if (!ServiceUtils.isValid(transformFilterName)) {
			throw new TaskException("Invalid transform filter name");
		}
		this.transformFilterName = transformFilterName;
	}

	/**
	 * @param callbackKey the callbackKey to set
	 */
	public void setCallbackKey(String callbackKey) {
		this.callbackKey = callbackKey;
	}

	/**
	 * @return the callbackKey
	 */
	@Column(name = "callback_key", nullable = false, length = 64)
	public String getCallbackKey() {
		return callbackKey;
	}

	@Override
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logical_file_id")
    public LogicalFile getLogicalFile()
	{
		return logicalFile;
	}

	@Override
	public void setLogicalFile(LogicalFile logicalFile)
	{
		this.logicalFile = logicalFile;
	}

    /* (non-Javadoc)
     * @see org.iplantc.service.io.model.QueueTask#getOwner()
     */
    @Override
    @Column(name = "owner", nullable = false, length = 32)
    public String getOwner() {
        return owner;
    }

    /* (non-Javadoc)
     * @see org.iplantc.service.io.model.QueueTask#setOwner(java.lang.String)
     */
    @Override
    public void setOwner(String owner) {
        this.owner = owner;
    }

	public void setStatus(TransformTaskStatus status)
	{
		this.status = status;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	public TransformTaskStatus getStatus()
	{
		return status;
	}
	
	@Override
	@Transient
	public String getStatusAsString()
	{
		return status.name();
	}

	@Override
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created", nullable = false, length = 19)
	public Date getCreated()
	{
		return created;
	}

	@Override
	public void setCreated(Date created)
	{
		this.created = created;
	}
	
	/**
	 * @return the lastUpdated
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "last_updated", nullable = false, length = 19)
	public Date getLastUpdated() {
		return lastUpdated;
	}

	/**
	 * @param lastUpdated
	 *            the lastUpdated to set
	 */
	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
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

	/* (non-Javadoc)
	 * @see org.iplantc.service.io.model.QueueTask#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof EncodingTask) {
			EncodingTask task = (EncodingTask)o;
			return (task.logicalFile.equals(logicalFile) && 
					task.transformName.equals(transformName) &&
					StringUtils.equals(task.owner, owner) &&
					task.transformFilterName.equals(transformFilterName)); 
		}
		return false;
	}
}
