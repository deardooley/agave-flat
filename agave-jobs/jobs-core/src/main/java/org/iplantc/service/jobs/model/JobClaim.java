package org.iplantc.service.jobs.model;

public final class JobClaim
{
    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    // Database fields.
    private String jobUuid;
    private String workerUuid;
    private String host;
    private String containerId;

    /* ********************************************************************** */
    /*                               Accessors                                */
    /* ********************************************************************** */
    public String getJobUuid()
    {
        return jobUuid;
    }
    public void setJobUuid(String jobUuid)
    {
        this.jobUuid = jobUuid;
    }
    public String getWorkerUuid()
    {
        return workerUuid;
    }
    public void setWorkerUuid(String workerUuid)
    {
        this.workerUuid = workerUuid;
    }
    public String getHost()
    {
        return host;
    }
    public void setHost(String host)
    {
        this.host = host;
    }
    public String getContainerId()
    {
        return containerId;
    }
    public void setContainerId(String containerId)
    {
        this.containerId = containerId;
    }
}
