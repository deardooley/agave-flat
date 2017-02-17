package org.iplantc.service.jobs.model;

import java.util.Date;

/** This is the in memory representation of the job lease. 
 * See the class comment in JobQueue for the rationale for
 * not using Hibernate.
 *  
 * @author rcardone
 */
public final class JobLease 
{
    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    // Database fields.
    private String lease;
    private Date   lastUpdated;
    private Date   expiresAt;
    private String lessee;

    /* ********************************************************************** */
    /*                               Accessors                                */
    /* ********************************************************************** */
    public String getLease() {
        return lease;
    }
    public void setLease(String lease) {
        this.lease = lease;
    }
    public Date getLastUpdated() {
        return lastUpdated;
    }
    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    public Date getExpiresAt() {
        return expiresAt;
    }
    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }
    public String getLessee() {
        return lessee;
    }
    public void setLessee(String lessee) {
        this.lessee = lessee;
    }
}
