/**
 * 
 */
package org.iplantc.service.io.model.enumerations;

import org.apache.commons.lang.StringUtils;



/**
 * The basic file operations that can be performed by users.
 * @author dooley
 *
 */ 
public enum FileOperationType {
	COPY,
	MKDIR,
	RENAME,
	MOVE,
	TOUCH,
	LIST,
	INDEX,
	REMOVE,
	CKSUM;
	
	/**
	 * Case-insensitive value of operation
	 * 
	 * @param value
	 * @return
	 */
	public static FileOperationType valueOfIgnoreCase(String value) {
		String upperValue = StringUtils.upperCase(value);
		
		return FileOperationType.valueOf(upperValue);
	}
}
