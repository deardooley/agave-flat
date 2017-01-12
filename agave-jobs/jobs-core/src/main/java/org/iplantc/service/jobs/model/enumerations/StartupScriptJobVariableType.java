package org.iplantc.service.jobs.model.enumerations;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.cfg.NotYetImplementedException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.util.Slug;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.BatchQueue;
import org.iplantc.service.systems.model.ExecutionSystem;


public enum StartupScriptJobVariableType implements WrapperTemplateVariableType
{
	// job value macros
	AGAVE_JOB_NAME,
	AGAVE_JOB_ID,
	AGAVE_JOB_APP_ID,
	AGAVE_JOB_EXECUTION_SYSTEM,
	AGAVE_JOB_BATCH_QUEUE,
	AGAVE_JOB_ARCHIVE_PATH,
	
	AGAVE_JOB_OWNER,
	AGAVE_JOB_TENANT;
	
	private static final Logger log = Logger.getLogger(StartupScriptJobVariableType.class);

	/**
	 * Resolves job-specific macros in the {@link ExecutionSystem#getStartupScript()} value for the 
	 * system. Tenancy is honored with respect to the system and job.
	 * 
	 * @param job A valid job object
	 * @return resolved value of the variable.
	 */
	@Override
	public String resolveForJob(Job job) {
		if (this == AGAVE_JOB_NAME)
		{
			return Slug.toSlug(job.getName());
		}
		else if (this == AGAVE_JOB_ID)
		{
			return StringUtils.isNotEmpty(job.getUuid()) ? job.getUuid() : "";
		}
		else if (this == AGAVE_JOB_APP_ID)
		{
			return job.getSoftwareName();
		}
		else if (this == AGAVE_JOB_EXECUTION_SYSTEM)
		{
			return StringUtils.isNotEmpty(job.getSystem()) ? job.getSystem() : "";
		}
		else if (this == AGAVE_JOB_BATCH_QUEUE)
		{
			String queueName = job.getBatchQueue();
			try {
				ExecutionSystem system = (ExecutionSystem)new SystemDao().findBySystemId(job.getSystem());
				if (system != null) {
					BatchQueue queue = system.getQueue(job.getBatchQueue());
					if (queue != null) {
						queueName = queue.getEffectiveMappedName();
					}
				}
			}
			catch (Throwable e) {
				log.error("Failed to resolve job queue name to effective queue name in job " + job.getUuid());
			}

			return queueName;
		}
		else if (this == AGAVE_JOB_ARCHIVE_PATH)
		{
			return (StringUtils.isNotEmpty(job.getArchivePath()) ? job.getArchivePath() : "");
		}
		else if (this == AGAVE_JOB_OWNER)
		{
		    return job.getOwner();
	    }
		else if (this == AGAVE_JOB_TENANT)
        {
            return job.getTenantId();
        }
		else {
			throw new NotYetImplementedException("The startupScript variable " + name() + " is not yet supported.");
		}
	}
}
