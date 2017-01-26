package org.iplantc.service.jobs.model;

/** This class holds the UUID of a job along with other information needed
 * to check quotas related to the job.
 * 
 * @author rcardone
 */
public final class JobQuotaInfo
{
    /* ********************************************************************** */
    /*                                 Fields                                 */
    /* ********************************************************************** */
    private String uuid;
    private String tenantId;
    private String owner;
    private String executionSystem;
    private String queueRequest;
    private long   maxQueueJobs;
    private long   maxQueueUserJobs;
    private long   maxSystemJobs;      // when null in database, set to -1 here
    private long   maxSystemUserJobs;  // when null in database, set to -1 here
    
    /* ********************************************************************** */
    /*                               Accessors                                */
    /* ********************************************************************** */
    public String getUuid()
    {
        return uuid;
    }
    public void setUuid(String uuid)
    {
        this.uuid = uuid;
    }
    public String getTenantId()
    {
        return tenantId;
    }
    public void setTenantId(String tenantId)
    {
        this.tenantId = tenantId;
    }
    public String getOwner()
    {
        return owner;
    }
    public void setOwner(String owner)
    {
        this.owner = owner;
    }
    public String getExecutionSystem()
    {
        return executionSystem;
    }
    public void setExecutionSystem(String executionSystem)
    {
        this.executionSystem = executionSystem;
    }
    public String getQueueRequest()
    {
        return queueRequest;
    }
    public void setQueueRequest(String queueRequest)
    {
        this.queueRequest = queueRequest;
    }
    public long getMaxQueueJobs()
    {
        return maxQueueJobs;
    }
    public void setMaxQueueJobs(long maxQueueJobs)
    {
        this.maxQueueJobs = maxQueueJobs;
    }
    public long getMaxQueueUserJobs()
    {
        return maxQueueUserJobs;
    }
    public void setMaxQueueUserJobs(long maxQueueUserJobs)
    {
        this.maxQueueUserJobs = maxQueueUserJobs;
    }
    public long getMaxSystemJobs()
    {
        return maxSystemJobs;
    }
    public void setMaxSystemJobs(long maxSystemJobs)
    {
        this.maxSystemJobs = maxSystemJobs;
    }
    public long getMaxSystemUserJobs()
    {
        return maxSystemUserJobs;
    }
    public void setMaxSystemUserJobs(long maxSystemUserJobs)
    {
        this.maxSystemUserJobs = maxSystemUserJobs;
    }
}
