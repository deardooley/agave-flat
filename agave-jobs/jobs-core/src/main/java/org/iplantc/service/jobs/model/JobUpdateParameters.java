package org.iplantc.service.jobs.model;

import java.util.Date;

import org.iplantc.service.jobs.model.enumerations.JobStatusType;

/** Instances of this class are passed to the JobDao.update method to specify
 * the fields to be updated and their new values.  This class is just a 
 * container.
 * 
 * Use the set accessors to assigned update values to job fields.  Use the
 * unset methods to remove a previously set value from being processed.  Job 
 * fields can be assigned null through this interface. 
 * 
 * @author rcardone
 */
public class JobUpdateParameters
{
    /* ********************************************************************** */
    /*                                 Fields                                 */
    /* ********************************************************************** */
    // Job fields that can be updated.
    // Remember to associate new fields with a
    // flag field, getter, setter, and unset method.
    private String              archivePath;
    private Date                created;
    private Date                endTime;
    private String              errorMessage;
    private Date                lastUpdated;
    private String              localJobId;
    private Integer             retries;
    private Date                startTime;
    private JobStatusType       status;
    private Integer             statusChecks;
    private Date                submitTime;
    private boolean             visible;
    private String              workPath;
    
    // Flags that indicate whether the field was set.
    private boolean             archivePathFlag;
    private boolean             createdFlag;
    private boolean             endTimeFlag;
    private boolean             errorMessageFlag;
    private boolean             lastUpdatedFlag;
    private boolean             localJobIdFlag;
    private boolean             retriesFlag;
    private boolean             startTimeFlag;
    private boolean             statusFlag;
    private boolean             statusChecksFlag;
    private boolean             submitTimeFlag;
    private boolean             visibleFlag;
    private boolean             workPathFlag;

    /* ********************************************************************** */
    /*                               Accessors                                */
    /* ********************************************************************** */
    // Job field accessors.  Setters update the value and set the flag.
    public String getArchivePath()
    {
        return archivePath;
    }
    public void setArchivePath(String archivePath)
    {
        archivePathFlag = true;
        this.archivePath = archivePath;
    }
    public Date getCreated()
    {
        return created;
    }
    public void setCreated(Date created)
    {
        createdFlag = true;
        this.created = created;
    }
    public Date getEndTime()
    {
        return endTime;
    }
    public void setEndTime(Date endTime)
    {
        endTimeFlag = true;
        this.endTime = endTime;
    }
    public String getErrorMessage()
    {
        return errorMessage;
    }
    public void setErrorMessage(String errorMessage)
    {
        errorMessageFlag = true;
        this.errorMessage = errorMessage;
    }
    public Date getLastUpdated()
    {
        return lastUpdated;
    }
    public void setLastUpdated(Date lastUpdated)
    {
        lastUpdatedFlag = true;
        this.lastUpdated = lastUpdated;
    }
    public String getLocalJobId()
    {
        return localJobId;
    }
    public void setLocalJobId(String localJobId)
    {
        localJobIdFlag = true;
        this.localJobId = localJobId;
    }
    public Integer getRetries()
    {
        return retries;
    }
    public void setRetries(Integer retries)
    {
        retriesFlag = true;
        this.retries = retries;
    }
    public Date getStartTime()
    {
        return startTime;
    }
    public void setStartTime(Date startTime)
    {
        startTimeFlag = true;
        this.startTime = startTime;
    }
    public JobStatusType getStatus()
    {
        return status;
    }
    public void setStatus(JobStatusType status)
    {
        statusFlag = true;
        this.status = status;
    }
    public Integer getStatusChecks()
    {
        return statusChecks;
    }
    public void setStatusChecks(Integer statusChecks)
    {
        statusChecksFlag = true;
        this.statusChecks = statusChecks;
    }
    public Date getSubmitTime()
    {
        return submitTime;
    }
    public void setSubmitTime(Date submitTime)
    {
        submitTimeFlag = true;
        this.submitTime = submitTime;
    }
    public boolean isVisible()
    {
        return visible;
    }
    public void setVisible(boolean visible)
    {
        visibleFlag = true;
        this.visible = visible;
    }
    public String getWorkPath()
    {
        return workPath;
    }
    public void setWorkPath(String workPath)
    {
        workPathFlag = true;
        this.workPath = workPath;
    }

    // Flag field getters.
    public boolean isArchivePathFlag()
    {
        return archivePathFlag;
    }

    public boolean isCreatedFlag()
    {
        return createdFlag;
    }

    public boolean isEndTimeFlag()
    {
        return endTimeFlag;
    }

    public boolean isErrorMessageFlag()
    {
        return errorMessageFlag;
    }

    public boolean isLastUpdatedFlag()
    {
        return lastUpdatedFlag;
    }

    public boolean isLocalJobIdFlag()
    {
        return localJobIdFlag;
    }

    public boolean isRetriesFlag()
    {
        return retriesFlag;
    }

    public boolean isStartTimeFlag()
    {
        return startTimeFlag;
    }

    public boolean isStatusFlag()
    {
        return statusFlag;
    }

    public boolean isStatusChecksFlag()
    {
        return statusChecksFlag;
    }

    public boolean isSubmitTimeFlag()
    {
        return submitTimeFlag;
    }

    public boolean isVisibleFlag()
    {
        return visibleFlag;
    }

    public boolean isWorkPathFlag()
    {
        return workPathFlag;
    }
    
    /* ********************************************************************** */
    /*                            Unset Methods                               */
    /* ********************************************************************** */
    // Unset methods also clear the value fields.
    public void unsetArchivePath()
    {
        archivePath = null;
        archivePathFlag = false;
    }
    public void unsetCreated()
    {
        created = null;
        createdFlag = false;
    }
    public void unsetEndTime()
    {
        endTime = null;
        endTimeFlag = false;
    }
    public void unsetErrorMessage()
    {
        errorMessage = null;
        errorMessageFlag = false;
    }
    public void unsetLastUpdated()
    {
        lastUpdated = null;
        lastUpdatedFlag = false;
    }
    public void unsetLocalJobId()
    {
        localJobId = null;
        localJobIdFlag = false;
    }
    public void unsetRetries()
    {
        retries = null;
        retriesFlag = false;
    }
    public void unsetStartTime()
    {
        startTime = null;
        startTimeFlag = false;
    }
    public void unsetStatus()
    {
        status = null;
        statusFlag = false;
    }
    public void unsetStatusChecks()
    {
        statusChecks = null;
        statusChecksFlag = false;
    }
    public void unsetSubmitTime()
    {
        submitTime = null;
        submitTimeFlag = false;
    }
    public void unsetVisible()
    {
        visible = false;
        visibleFlag = false;
    }
    public void unsetWorkPath()
    {
        workPath = null;
        workPathFlag = false;
    }
}
