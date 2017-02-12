package org.iplantc.service.jobs.model.enumerations;

/**
 * @author rcardone
 * 
 */
public enum JobPhaseType
{
    // Same alphabetic order as defined in the database enumeration.
    ARCHIVING, MONITORING, ROLLINGBACK, STAGING, SUBMITTING;
	
	@Override
	public String toString() {
		return name();
	}
}
