package org.iplantc.service.jobs.phases.schedulers.dto;

/** This class holds the UUID of a job ready to be archived.
 * 
 * @author rcardone
 */
public final class JobArchiveInfo
{
    /* ********************************************************************** */
    /*                                 Fields                                 */
    /* ********************************************************************** */
    private String uuid;
    
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
}
