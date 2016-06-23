/**
 * 
 */
package org.iplantc.service.jobs.callbacks;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.ObjectNotFoundException;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobCallbackException;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.joda.time.DateTime;

/**
 * @author dooley
 *
 */
public class JobCallback {
    
    private JobStatusType status;
    private Job job;
    private String localSchedulerJobId;
    private Date created;
    
    public JobCallback(Job job, JobStatusType status) 
    throws JobCallbackException    
    {
        setJob(job);
        setStatus(status);
        setCreated(new Date());
    }
    
    /**
     * Callback defined by string values
     * @param uuid
     * @param status
     * @param callbackToken
     * @throws JobCallbackException
     * @throws PermissionException if the {@code updateToken} does not match 
     * the one associated with the job {@code uuid}
     */
    public JobCallback(String uuid, String status, String callbackToken) 
    throws JobCallbackException, PermissionException 
    {
        setJob(uuid, callbackToken);
        setStatus(status);
        setCreated(new Date());
    }
    
    public JobCallback(String uuid, String status, String callbackToken, String localSchedulerJobId) 
    throws JobCallbackException, PermissionException 
    {
        this(uuid, status, callbackToken);
        setLocalSchedulerJobId(localSchedulerJobId);
    }
    
    /**
     * The new status represented by this callback.
     * @return the status
     */
    public synchronized JobStatusType getStatus() {
        return status;
    }

    /**
     * Sets the new {@link JobStatusType} defined by this callback.
     * @param status the status to set
     * @throws JobCallbackException if the status is null
     */
    protected synchronized void setStatus(JobStatusType status) throws JobCallbackException {
        if (status == null) {
            throw new JobCallbackException("Job cannot be null");
        } 
        
        this.status = status;
    }
    
    /**
     * Sets the new {@link JobStatusType} defined by this callback.
     * @param status the case-insensitive String value of the status to set
     * @throws JobCallbackException if the status value is null or invalid
     */
    public synchronized void setStatus(String status) throws JobCallbackException {
        if (StringUtils.isEmpty(status)) {
            throw new JobCallbackException("Job callback status cannot be null");
        } else {
            try {
                setStatus(JobStatusType.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new JobCallbackException("Invalid job status provided.");
            }
        }
    }

    /**
     * @return the job
     */
    public synchronized Job getJob() {
        return job;
    }

    /**
     * Sets the job for this callback
     * 
     * @param job the job to set
     * @throws JobCallbackException if the job is null
     */
    protected synchronized void setJob(Job job) throws JobCallbackException {
        if (job == null) {
            throw new JobCallbackException("Job cannot be null");
        }
        this.job = job;
    }
    
    /**
     * Sets the job for this callback
     * 
     * @param uuid the uuid of the job
     * @param updateToken the callback token assocated with this job
     * @throws JobCallbackException if the uuid is null or invalid
     */
    public synchronized void setJob(String uuid, String updateToken) 
    throws JobCallbackException, PermissionException, ObjectNotFoundException 
    {
        if (StringUtils.isEmpty(uuid)) {
            throw new JobCallbackException("No job id provided");
        } else if (StringUtils.isEmpty(updateToken)) {
            throw new JobCallbackException("No update token provided");
        } else {
            try {
                setJob(JobDao.getByUuid(uuid));
                
                if (!StringUtils.equals(updateToken, getJob().getUpdateToken())) {
                    this.job = null;
                    throw new PermissionException("Invalid update token provided for job " + uuid);
                }
            } catch (JobException e) {
                throw new JobCallbackException("Unable to retrieve job matching the given uuid", e);
            } catch (JobCallbackException e) {
                throw new ObjectNotFoundException(e, "No job found for id " + uuid);
            }
        }
    }

    /**
     * @return the created
     */
    public synchronized Date getCreated() {
        return created;
    }

    /**
     * @param created the created to set
     */
    public synchronized void setCreated(Date created) {
        this.created = created;
    }
    
    /**
     * The local id of the job on the {@link ExecutionSystem}
     * @return 
     */
    public String getLocalSchedulerJobId() {
        return localSchedulerJobId;
    }

    /**
     * Sets the local id of the job on the {@link ExecutionSystem}
     * @param localSchedulerJobId
     */
    public void setLocalSchedulerJobId(String localSchedulerJobId) {
        this.localSchedulerJobId = localSchedulerJobId;
    }

    public String toString() {
        return String.format("Job %s received callback with status %s on %s",
                job == null ? "null" : job.getUuid(),
                status == null ? "unknown" : status.name(),
                new DateTime(created).toString());
    }
}
