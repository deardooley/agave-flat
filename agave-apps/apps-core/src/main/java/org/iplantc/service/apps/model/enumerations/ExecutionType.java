/**
 * 
 */
package org.iplantc.service.apps.model.enumerations;

/**
 * @author dooley
 * 
 */
public enum ExecutionType
{
	ATMOSPHERE, HPC, CONDOR, CLI;
	
	@Override
	public String toString() {
		return name();
	}
}
