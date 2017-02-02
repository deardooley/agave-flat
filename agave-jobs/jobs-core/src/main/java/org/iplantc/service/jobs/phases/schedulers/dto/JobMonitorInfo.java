package org.iplantc.service.jobs.phases.schedulers.dto;

import java.util.Date;

/** This class holds the UUID of a job along with other information needed
 * to determine if a job is ready to be monitored.
 * 
 * @author rcardone
 */
public final class JobMonitorInfo
{
    /* ********************************************************************** */
    /*                                 Fields                                 */
    /* ********************************************************************** */
    private String uuid;
    private int    statusChecks;
    private Date   lastUpdated;
    
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
    public int getStatusChecks()
    {
        return statusChecks;
    }
    public void setStatusChecks(int statusChecks)
    {
        this.statusChecks = statusChecks;
    }
    public Date getLastUpdated()
    {
        return lastUpdated;
    }
    public void setLastUpdated(Date lastUpdated)
    {
        this.lastUpdated = lastUpdated;
    }
}
