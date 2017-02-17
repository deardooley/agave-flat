package org.iplantc.service.jobs.model;

import java.util.Date;

import org.iplantc.service.jobs.model.enumerations.JobPhaseType;

/** This is the in memory representation of a published job record. 
 * See the class comment in JobQueue for the rationale for not
 * using Hibernate.
 *  
 * @author rcardone
 */
public final class JobPublished 
{
    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    // Database fields.
    private JobPhaseType phase;
    private String       jobUuid;
    private Date         created;
    private String       creator;

    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    public JobPublished(){}
    
    public JobPublished(JobPhaseType phase, String jobUuid, String creator)
    {
        this.phase = phase;
        this.jobUuid = jobUuid;
        this.creator = creator;
        this.created = new Date();
    }

    /* ********************************************************************** */
    /*                               Accessors                                */
    /* ********************************************************************** */
    public JobPhaseType getPhase()
    {
        return phase;
    }
    public void setPhase(JobPhaseType phase)
    {
        this.phase = phase;
    }
    public String getJobUuid()
    {
        return jobUuid;
    }
    public void setJobUuid(String jobUuid)
    {
        this.jobUuid = jobUuid;
    }
    public Date getCreated()
    {
        return created;
    }
    public void setCreated(Date created)
    {
        this.created = created;
    }
    public String getCreator()
    {
        return creator;
    }
    public void setCreator(String creator)
    {
        this.creator = creator;
    }
}
