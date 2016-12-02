package org.iplantc.service.jobs.model.enumerations;

/** All the ways a job can be interrupted by the end user.
 * 
 * @author rcardone
 */
public enum JobInterruptType
{
    // Same order as defined in the database enumeration.
    DELETE, PAUSE, STOP;
	
	@Override
	public String toString() {
		return name();
	}
}
