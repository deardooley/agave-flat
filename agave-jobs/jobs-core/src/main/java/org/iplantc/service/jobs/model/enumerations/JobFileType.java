/**
 * 
 */
package org.iplantc.service.jobs.model.enumerations;

/**
 * @author dooley
 * 
 */
public enum JobFileType
{
	INPUT, OUTPUT;
	
	@Override
	public String toString() {
		return name();
	}
}
