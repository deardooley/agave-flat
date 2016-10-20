package org.iplantc.service.jobs.model.enumerations;

/**
 * @author rcardone
 * 
 */
public enum JobPhaseType
{
    // Same order as defined in the database enumeration.
    ARCHIVING, MONITORING, STAGING, SUBMITTING;
	
	@Override
	public String toString() {
		return name();
	}
}
