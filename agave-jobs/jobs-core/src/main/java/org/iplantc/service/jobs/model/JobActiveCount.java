package org.iplantc.service.jobs.model;

/** This class holds information about active jobs needed to check job quotas.
 * 
 * @author rcardone
 */
public final class JobActiveCount
{
    /* ********************************************************************** */
    /*                                 Fields                                 */
    /* ********************************************************************** */
    private String tenantId;
    private String owner;
    private String executionSystem;
    private String queueRequest;
    private int    count;
    
    /* ********************************************************************** */
    /*                               Accessors                                */
    /* ********************************************************************** */
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
    public int getCount()
    {
        return count;
    }
    public void setCount(int count)
    {
        this.count = count;
    }
}
