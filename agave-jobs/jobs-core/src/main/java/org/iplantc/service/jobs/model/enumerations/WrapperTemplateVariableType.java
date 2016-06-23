package org.iplantc.service.jobs.model.enumerations;

import org.iplantc.service.jobs.model.Job;

public interface WrapperTemplateVariableType
{

	/**
	 * Resolves a template variable into the actual runtime value
	 * for a given job. Tenancy is honored with respect to the 
	 * job.
	 * 
	 * @param job A valid job object
	 * @return resolved value of the variable.
	 */
	public abstract String resolveForJob(Job job);

}