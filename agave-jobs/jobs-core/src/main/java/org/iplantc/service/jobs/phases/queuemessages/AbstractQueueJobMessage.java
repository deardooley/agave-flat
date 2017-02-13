package org.iplantc.service.jobs.phases.queuemessages;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.jobs.exceptions.JobException;

/** This abstract topic message contains the common fields needed in job-specific messages.
 * See the class comment in JobInterruptDao for a discussion of job epochs.
 * 
 * @author rcardone
 */
public abstract class AbstractQueueJobMessage 
 extends AbstractQueueMessage
{
    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    // Note that adding fields, or changing the names or ordering, will require 
    // changes to the QueueMessagesTest program.
    //
    // Command fields.
    public String tenantId;  // Caller's tenant id
    public String jobName;   // Job name
    public String jobUuid;   // Job unique id
    public int    epoch;     // Epoch in which this interrupt executes
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    public AbstractQueueJobMessage(JobCommand jobCommand){super(jobCommand);}
    
    public AbstractQueueJobMessage(JobCommand jobCommand,
                                   String     jobName,
                                   String     jobUuid,
                                   String     tenantId,
                                   int        epoch)
    {
        this(jobCommand);
        this.jobName = jobName;
        this.jobUuid = jobUuid;
        this.tenantId = tenantId;
        this.epoch = epoch;
    }
    
    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* validate:                                                              */
    /* ---------------------------------------------------------------------- */
    /** Make sure all fields are filled in.
     * 
     * @throws JobException if any field is not initialized
     */
    public void validate() throws JobException
    {
        // Make sure all fields are filled in.
        if (StringUtils.isBlank(tenantId)) {
            String msg = "Invalid tenantId assignment in " + 
                         getClass().getSimpleName() + " object.";
            throw new JobException(msg);
        }
        if (StringUtils.isBlank(jobName)) {
            String msg = "Invalid job name assignment in " + 
                         getClass().getSimpleName() + " object.";
            throw new JobException(msg);
        }
        if (StringUtils.isBlank(jobUuid)) {
            String msg = "Invalid job uuid assignment in " + 
                         getClass().getSimpleName() + " object.";
            throw new JobException(msg);
        }
        if (epoch < 0) {
            String msg = "Invalid epoch value " + epoch + " in " + 
                         getClass().getSimpleName() + " object.";
            throw new JobException(msg);
        }
    }
}
