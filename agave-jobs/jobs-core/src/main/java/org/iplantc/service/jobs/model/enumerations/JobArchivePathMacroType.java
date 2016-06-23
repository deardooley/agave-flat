package org.iplantc.service.jobs.model.enumerations;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.cfg.NotYetImplementedException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.util.Slug;
import org.joda.time.DateTime;


/**
 * Defines the template variables supported in the job archive path.
 * By leveraging these variables in the {@link Job#archivePath}, field
 * of a job request, dynamic archive paths can be created based on
 * runtime values of the job. For example, one could specify the following
 * to route the archived output to a different folder based on the owner,
 * terminal timestamp, final job status, and job id.
 * <pre><code>{
 *   ...
 *   archive:true,
 *   archivePath: ${OWNER}/archive/${END_TIME}/${STATUS}/${ID}
 *   ...
 * }</code></pre>
 * 
 * @author dooley
 *
 */
public enum JobArchivePathMacroType implements WrapperTemplateVariableType
{
	/**
     * Name of the app run by this job. This is the {@link Software#uniqueName}.
     */
    APP_ID,
	
	/**
     * {@link RemoteSystem#systemId} of the {@link ExecutionSystem} on which the job ran 
     */
    EXECUTION_SYSTEM,
	
	/**
     * {@link BatchQueue#name} on the remote {@link ExecutionSystem} to which 
     * the job was submitted 
     */
    BATCH_QUEUE,
	
	/**
     * Job name, slugified, and cleaned
     */
    JOB_NAME,
    
    /**
     * UUID of the job 
     */
    JOB_ID,  
	
    /**
     * Total nodes requested
     */
    JOB_NODE_COUNT,
	
	/**
     * Processors per node requested
     */
    JOB_PROCESSORS_PER_NODE,
	
	/**
	 * Memory requested in GB 
	 */
	JOB_MEMORY_PER_NODE,

	/**
     * Timestamp at which the job request was submitted to Agave  
     * in Slugified ISO8601 format. (ie. yyyy-MM-ddTHHmmssSSSZZ)
     */
    JOB_CREATED_TIME,
	
    /**
     * Timestamp at which Agave became aware the job began running {@link ExecutionSystem}
     * in ISO8601 format. (ie. yyyy-MM-dd'T'HH:mm:ss.SSSZZ)
     */
//    JOB_START_TIME,
	
    /**
     * Timestamp at which Agave became aware the job completed its execution, or was otherwise
     * stopped. Value is given in in ISO8601 format. (ie. yyyy-MM-dd'T'HH:mm:ss.SSSZZ)
     */
//    JOB_END_TIME,
	
	/**
     * Timestamp at which the job was submitted to the {@link ExecutionSystem} for execution
     * in ISO8601 format. (ie. yyyy-MM-dd'T'HH:mm:ss.SSSZZ)
     */
//    JOB_SUBMIT_TIME,
	
	/**
     * Terminal status of the job
     */
//    JOB_STATUS,
	
	/**
	 * Username of the job 
	 */
	JOB_OWNER,
	
	/**
	 * {@link RemoteSystem#systemId} of the {@link StorageSystem} to which this job was archived.
	 */
	ARCHIVE_SYSTEM,
	
	/**
	 * Tenant of the job
	 */
	TENANT_ID;
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.model.enumerations.WrapperTemplateVariableType#resolveForJob(org.iplantc.service.jobs.model.Job)
	 */
	@Override
	public String resolveForJob(Job job)
	{
	    if (this == APP_ID) 
        {
            return StringUtils.isEmpty(job.getSoftwareName()) ? "" : job.getSoftwareName();   
        } 
        else if (this == EXECUTION_SYSTEM) 
        {
            return StringUtils.isEmpty(job.getSystem()) ? "" : job.getSystem();
        } 
        else if (this == BATCH_QUEUE) 
        {
            return StringUtils.isEmpty(job.getBatchQueue()) ? "" : job.getBatchQueue();
        } 
        else if (this == JOB_NAME) 
		{
			return Slug.toSlug(job.getName());
		} 
		else if (this == JOB_ID) 
		{
			return StringUtils.isEmpty(job.getUuid()) ? "" : job.getUuid();
		} 
		else if (this == JOB_NODE_COUNT) 
		{
			return job.getNodeCount() == null ? "" : String.valueOf(job.getNodeCount());
		} 
		else if (this == JOB_PROCESSORS_PER_NODE) 
		{
			return job.getProcessorsPerNode() == null ? "" : String.valueOf(job.getProcessorsPerNode());
		} 
		else if (this == JOB_MEMORY_PER_NODE) 
		{
			return job.getMemoryPerNode() == null ? "" : String.valueOf(job.getMemoryPerNode());
		} 
//		else if (this == JOB_SUBMIT_TIME) 
//        {
//            return job.getSubmitTime() == null ? "" : new DateTime(job.getSubmitTime()).toString();
//        } 
        else if (this == JOB_CREATED_TIME) 
        {
            return job.getCreated() == null ? "" : new DateTime(job.getCreated()).toString();
        }
//        else if (this == JOB_START_TIME) 
//        {
//            return job.getStartTime() == null ? "" : new DateTime(job.getStartTime()).toString();
//        } 
//        else if (this == JOB_END_TIME) 
//        {
//            return job.getEndTime() == null ? "" : new DateTime(job.getEndTime()).toString();
//        }
        else if (this == JOB_OWNER) 
        {
            return StringUtils.isEmpty(job.getOwner()) ? "" : job.getOwner();
        }
//        else if (this == JOB_STATUS) 
//        {
//            return job.getStatus().name();
//        }
        else if (this == ARCHIVE_SYSTEM) 
        {
            if (job.isArchiveOutput()) {
                return job.getArchiveSystem() == null ? "" : job.getArchiveSystem().getSystemId();
            } else {
                return "";
            }
        } 
        else if (this == TENANT_ID) 
        {
            return StringUtils.isEmpty(job.getTenantId()) ? "" : job.getTenantId();
        }
        else 
        {
            return "";
        }
	}
	
	/**
	 * Returns array of all the {@link JobArchivePathMacroType} values that
	 * must be resolved after the job request is accepted.
	 * 
	 * @return 
	 */
	public static JobArchivePathMacroType[] getLateBindingMacros()
	{
//	    return new JobArchivePathMacroType[] { JOB_SUBMIT_TIME, JOB_END_TIME, JOB_START_TIME, JOB_STATUS };
	    return new JobArchivePathMacroType[] {};
	}
	
	/**
     * Returns array of all the {@link JobArchivePathMacroType} values that
     * must be resolved after the job request is accepted.
     * 
     * @return 
     */
    public boolean isLateBindingMacro()
    {
        return ArrayUtils.contains(JobArchivePathMacroType.getLateBindingMacros(), this);
    }
	
	/**
	 * Checks {@code path} for occurrences of any {@link JobArchivePathMacroType} value.
	 * 
	 * @param path the string to check
	 * @return if present, false if absent or {@code path} is 
	 */
	public static boolean hasMacrosInPath(String path) {
	    if (StringUtils.isEmpty(path)) return false;
	    
	    for (JobArchivePathMacroType macro: JobArchivePathMacroType.values()) {
	        if (path.contains("${" + macro.name() + "}")) return true;
	    }
	    
	    return false;
	}
	
	/**
	 * Checks for the presence of any {@link JobArchivePathMacroType} values that
     * must be resolved after a job request is accepted.
	 * @param path the string to check
	 * @return true if present, false if absent or {@code path} is null
	 */
	public static boolean hasLateBindingMacrosInPath(String path) {
	    if (StringUtils.isEmpty(path)) return false;
        
        for (JobArchivePathMacroType macro: JobArchivePathMacroType.getLateBindingMacros()) {
            if (path.contains("${" + macro.name() + "}")) return true;
        }
        
        return false;
    }

    /**
     * Resolves all macros in the path, leaving any whose value cannot yet be determined.
     * @param job
     * @param path
     * @return
     */
    public static String resolveMacrosInPath(Job job, String path) {
        if (StringUtils.isEmpty(path)) return path;
        
        for (JobArchivePathMacroType macro: JobArchivePathMacroType.values()) {
            String resolvedValue = macro.resolveForJob(job);
            path = StringUtils.replace(path, "${" + macro.name() + "}", resolvedValue);
        }
        
        return path;
    }
}
