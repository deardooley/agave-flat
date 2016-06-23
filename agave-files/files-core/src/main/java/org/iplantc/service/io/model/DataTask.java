/**
 * 
 */
package org.iplantc.service.io.model;

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
	
	private String created;
	
	private int attempts = 0;
	
	private String lastUpdated;
	
	
	/**
	 * 
	 */
	public DataTask() {
	}

}
