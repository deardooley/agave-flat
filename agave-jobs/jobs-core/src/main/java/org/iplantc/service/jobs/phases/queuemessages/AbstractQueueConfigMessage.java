package org.iplantc.service.jobs.phases.queuemessages;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;

/** This abstract topic message contains the common fields needed in job-specific messages.
 * 
 * @author rcardone
 */
public abstract class AbstractQueueConfigMessage 
 extends AbstractQueueMessage
{
    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    // Note that adding fields, or changing the names or ordering, will require 
    // changes to the QueueMessagesTest program.
    //
    // Command fields.
    public String queueName;   // Target queue name
    public String tenantId;    // Caller's tenantId
    public JobPhaseType phase; // Phase the queue services
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    public AbstractQueueConfigMessage(JobCommand jobCommand){super(jobCommand);}
    
    public AbstractQueueConfigMessage(JobCommand   jobCommand,
                                      String       queueName,
                                      String       tenantId,
                                      JobPhaseType phase)
    {
        this(jobCommand);
        this.queueName = queueName;
        this.tenantId = tenantId;
        this.phase = phase;
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
        if (StringUtils.isBlank(queueName)) {
            String msg = "Invalid queue name assignment in " + 
                         getClass().getSimpleName() + " object.";
            throw new JobException(msg);
        }
        if (StringUtils.isBlank(tenantId)) {
            String msg = "Invalid tenantId assignment in " + 
                         getClass().getSimpleName() + " object.";
            throw new JobException(msg);
        }
        if (phase == null) {
            String msg = "Null phase assignment in " + 
                         getClass().getSimpleName() + " object.";
            throw new JobException(msg);
        }
    }
}
