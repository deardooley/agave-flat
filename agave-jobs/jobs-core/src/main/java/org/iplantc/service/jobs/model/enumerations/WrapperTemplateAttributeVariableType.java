package org.iplantc.service.jobs.model.enumerations;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.cfg.NotYetImplementedException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.util.TimeUtils;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.util.Slug;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.BatchQueue;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.util.ServiceUtils;
import org.joda.time.DateTime;


public enum WrapperTemplateAttributeVariableType implements WrapperTemplateVariableType
{
	
	// job value macros
	IPLANT_JOB_NAME,
	AGAVE_JOB_NAME,
	AGAVE_JOB_ID,
	AGAVE_JOB_APP_ID,
	AGAVE_JOB_EXECUTION_SYSTEM,
	AGAVE_JOB_BATCH_QUEUE,
	AGAVE_JOB_SUBMIT_TIME,
	AGAVE_JOB_ARCHIVE_SYSTEM,
	AGAVE_JOB_ARCHIVE_PATH,
	AGAVE_JOB_NODE_COUNT,
	IPLANT_CORES_REQUESTED,
	AGAVE_JOB_PROCESSORS_PER_NODE,
	AGAVE_JOB_MEMORY_PER_NODE,
	AGAVE_JOB_MAX_RUNTIME,
	AGAVE_JOB_MAX_RUNTIME_MILLISECONDS,
	AGAVE_JOB_ARCHIVE_URL,

	AGAVE_JOB_OWNER,
	AGAVE_JOB_TENANT,
	AGAVE_JOB_ARCHIVE;

	private static final Logger log = Logger.getLogger(WrapperTemplateAttributeVariableType.class);
	
	@Override
	public String resolveForJob(Job job)
	{
		if (this == IPLANT_JOB_NAME || this == AGAVE_JOB_NAME)
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
		else if (this == AGAVE_JOB_SUBMIT_TIME)
		{
			return job.getSubmitTime() == null ? "" : new DateTime(job.getSubmitTime()).toString();
		}
		else if (this == AGAVE_JOB_ARCHIVE_SYSTEM)
		{
			return (job.getArchiveSystem() == null ? "" : job.getArchiveSystem().getSystemId());
		}
		else if (this == AGAVE_JOB_ARCHIVE_PATH)
		{
			return (StringUtils.isNotEmpty(job.getArchivePath()) ? job.getArchivePath() : "");
		}
		else if (this == AGAVE_JOB_NODE_COUNT)
		{
			return String.valueOf(job.getNodeCount());
		}
		else if (this == IPLANT_CORES_REQUESTED)
		{
			return String.valueOf(job.getProcessorsPerNode() * job.getNodeCount());
		}
		else if (this == AGAVE_JOB_PROCESSORS_PER_NODE)
		{
			return String.valueOf(job.getProcessorsPerNode());
		}
		else if (this == AGAVE_JOB_MEMORY_PER_NODE)
		{
			return String.valueOf(job.getMemoryPerNode());
		}
		else if (this == AGAVE_JOB_MAX_RUNTIME)
		{
			return String.valueOf(job.getMaxRunTime());
		}
		else if (this == AGAVE_JOB_MAX_RUNTIME_MILLISECONDS)
		{
			try {
				return String.valueOf(TimeUtils.getMillisecondsForMaxTimeValue(job.getMaxRunTime()));
			}
			catch (Exception e) {
				return "0";
			}
		}
		else if (this == AGAVE_JOB_OWNER)
		{
		    return job.getOwner();
	    }
		else if (this == AGAVE_JOB_TENANT)
        {
            return job.getTenantId();
        }
		else if (this == AGAVE_JOB_ARCHIVE)
        {
            return job.isArchiveOutput() ? "1" : "";
        }
		else if (this == AGAVE_JOB_ARCHIVE_URL)
		{
			if (job.isArchiveOutput())
			{
				return String.format("%smedia/system/%s/%s",
						TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_IO_SERVICE, job.getTenantId()),
						job.getArchiveSystem().getSystemId(),
						job.getArchivePath());
			}
			else
			{
				return "";
			}
		}
		else {
			throw new NotYetImplementedException("The template variable " + name() + " is not yet supported.");
		}
	}
}
