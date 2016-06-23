/**
 * 
 */
package org.iplantc.service.io.manager.actions;

import java.net.URI;
import java.sql.Timestamp;

import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.io.model.enumerations.FileOperationType;

/**
 * @author dooley
 *
 */
//@ValidUserOperation
//@ValidPaths
public class DataTask {

//	@ValidSystemId
	private String sourceSystemId;
	
//	@ValidSystemId
	private String destSystemId;
	
//	@ValidSystemPath
	private String sourcePath;
	
//	@ValidSystemPath
	private String destPath;
	
	private String createdBy;
	
	private FileOperationType operation;
	
	private String uuid;
	
	private Timestamp created;
	
	private int attempts = 0;
	
	private Timestamp lastUpdated;

	public DataTask() {
		this.created = new Timestamp(System.currentTimeMillis());
		this.lastUpdated = created;
		this.uuid = new AgaveUUID(UUIDType.WORKER).toString();
		
	}
	
	public DataTask(String srcAgaveUrl, String destAgaveUrl, FileOperationType operation, String createdBy) {
		this();
		
		setOperation(operation);
		
		setCreatedBy(createdBy);
		
		URI src = URI.create(srcAgaveUrl);
		setSourceSystemId(src.getHost());
		setSourcePath(src.getPath());
		
		URI dest = URI.create(destAgaveUrl);
		setDestSystemId(dest.getHost());
		setDestPath(dest.getPath());
		
	}
	/**
	 * @return the sourceSystemId
	 */
	public String getSourceSystemId() {
		return sourceSystemId;
	}

	/**
	 * @param sourceSystemId the sourceSystemId to set
	 */
	public void setSourceSystemId(String sourceSystemId) {
		this.sourceSystemId = sourceSystemId;
	}

	/**
	 * @return the destSystemId
	 */
	public String getDestSystemId() {
		return destSystemId;
	}

	/**
	 * @param destSystemId the destSystemId to set
	 */
	public void setDestSystemId(String destSystemId) {
		this.destSystemId = destSystemId;
	}

	/**
	 * @return the sourcePath
	 */
	public String getSourcePath() {
		return sourcePath;
	}

	/**
	 * @param sourcePath the sourcePath to set
	 */
	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	/**
	 * @return the destPath
	 */
	public String getDestPath() {
		return destPath;
	}

	/**
	 * @param destPath the destPath to set
	 */
	public void setDestPath(String destPath) {
		this.destPath = destPath;
	}

	/**
	 * @return the createdBy
	 */
	public String getCreatedBy() {
		return createdBy;
	}

	/**
	 * @param createdBy the createdBy to set
	 */
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	/**
	 * @return the operation
	 */
	public FileOperationType getOperation() {
		return operation;
	}

	/**
	 * @param operation the operation to set
	 */
	public void setOperation(FileOperationType operation) {
		this.operation = operation;
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
	 * @return the created
	 */
	public Timestamp getCreated() {
		return created;
	}

	/**
	 * @param created the created to set
	 */
	public void setCreated(Timestamp created) {
		this.created = created;
	}

	/**
	 * @return the attempts
	 */
	public int getAttempts() {
		return attempts;
	}

	/**
	 * @param attempts the attempts to set
	 */
	public void setAttempts(int attempts) {
		this.attempts = attempts;
	}

	/**
	 * @return the lastUpdated
	 */
	public Timestamp getLastUpdated() {
		return lastUpdated;
	}

	/**
	 * @param lastUpdated the lastUpdated to set
	 */
	public void setLastUpdated(Timestamp lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
	
	public String toString() {
		return String.format("agave://%s@%s%s => agave://%s%s",
				getCreatedBy(),
				getSourceSystemId(),
				getSourcePath(),
				getDestSystemId(),
				getDestPath());
	}
}
