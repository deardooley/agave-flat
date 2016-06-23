/**
 * 
 */
package org.iplantc.service.data.model;

import static org.iplantc.service.io.model.enumerations.TransformTaskStatus.TRANSFORMING_QUEUED;

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
import javax.xml.bind.annotation.XmlElement;

import org.iplantc.service.io.exceptions.TaskException;
import org.iplantc.service.io.model.LogicalFile;
import org.iplantc.service.io.model.QueueTask;
import org.iplantc.service.io.model.enumerations.TransformTaskStatus;
import org.iplantc.service.io.util.ServiceUtils;
import org.iplantc.service.systems.model.RemoteSystem;
/**
 * @author dooley
 *
 */
@Entity
@Table(name = "decoding_tasks")
public class DecodingTask implements QueueTask
{
	private Long id;
    private RemoteSystem storageSystem;
	private String sourcePath; // path to the user file in IRODS
	private String destPath; // path to the user file in IRODS
	private String srcTransform;
	private String destTransform;
	private String currentFilter;
	private TransformTaskStatus status;
	private String callbackKey;
	private String destinationUri;
	private LogicalFile logicalFile;
	private String owner;
	private Date created = new Date();
	private Date lastUpdated = new Date();
	
	private Integer version = 0;
	
	public DecodingTask() {}
	
	public DecodingTask(LogicalFile logicalFile, RemoteSystem storageSystem, String sourcePath,
			String destPath, String srcTransform, String destTransform, 
			String currentFilter, String destinationUri, String owner) {
		setLogicalFile(logicalFile);
        setStorageSystem(storageSystem);
		setSourcePath(sourcePath);
		setDestPath(destPath);
		setSrcTransform(srcTransform);
		setDestTransform(destTransform);
		setCurrentFilter(currentFilter);
		setCallbackKey(UUID.randomUUID().toString());
		setStatus(TRANSFORMING_QUEUED);
		setDestinationUri(destinationUri);
		setOwner(owner);
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
	@Column(name = "id", unique = true, nullable = false)@XmlElement
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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_id")
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
	 * @param srcTransform the srcTransform to set
	 */
	public void setSrcTransform(String srcTransform) {
		if (!ServiceUtils.isValid(srcTransform)) {
			throw new TaskException("Invalid source transform name");
		}
		this.srcTransform = srcTransform;
	}

	/**
	 * @return the srcTransformN
	 */
	@Column(name = "src_transform", nullable = false, length = 64)
	public String getSrcTransform() {
		return srcTransform;
	}
	
	/**
	 * @param destTransform the destTransform to set
	 */
	public void setDestTransform(String destTransform) {
		if (!ServiceUtils.isValid(destTransform)) {
			throw new TaskException("Invalid destination transform name");
		}
		this.destTransform = destTransform;
	}

	/**
	 * @return the destTransform
	 */
	@Column(name = "dest_transform", nullable = false, length = 64)
	public String getDestTransform() {
		return destTransform;
	}

	/**
	 * @return the handler
	 */
	@Column(name = "current_filter", nullable = false, length = 64)
	public String getCurrentFilter() {
		return currentFilter;
	}

	/**
	 * @param handler the handler to set
	 */
	public void setCurrentFilter(String currentFilter) {
		if (!ServiceUtils.isValid(currentFilter)) {
			throw new TaskException("Invalid filter name for transform " + destTransform);
		}
		this.currentFilter = currentFilter;
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
	
	/**
	 * @return the destinationUri
	 */
	@Column(name = "destination_uri", nullable = false, length = 255)
	public String getDestinationUri() {
		return destinationUri;
	}

	/**
	 * @param destinationUri the destinationUri to set
	 */
	public void setDestinationUri(String destinationUri) {
		this.destinationUri = destinationUri;
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

//	@Override
//	@Column(name = "event_id", length = 64)
//	public String getEventId()
//	{
//		return eventId;
//	}
//
//	@Override
//	public void setEventId(String eventId)
//	{
//		this.eventId = eventId;
//	}

	public void setStatus(TransformTaskStatus status)
	{
		this.status = status;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 8)
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
		if (o instanceof DecodingTask) {
			DecodingTask task = (DecodingTask)o;
			return (task.srcTransform.equals(srcTransform) &&
					task.destTransform.equals(destTransform) &&
					task.currentFilter.equals(currentFilter)); 
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.io.model.QueueTask#getOwner()
	 */
	@Override
	@Column(name = "owner", nullable = false, length = 32)
	public String getOwner() {
		return this.owner;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.io.model.QueueTask#setOwner(java.lang.String)
	 */
	@Override
	public void setOwner(String owner) {
		this.owner = owner;
		
	}
	
	
	
}
