package org.iplantc.service.jobs.phases.queuemessages;

import java.io.IOException;

import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;

/** This message is used to terminate the specified number of workers on
 * the specified queue.
 * 
 * @author rcardone
 */
public final class ResetPriorityMessage 
 extends AbstractQueueConfigMessage
{
    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    // Note that adding fields, or changing the names or ordering, will require 
    // changes to the QueueMessagesTest program.
    //
    // Command fields.
    public int    priority;    // Queue priority
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    public ResetPriorityMessage(){super(JobCommand.TPC_RESET_PRIORITY);}
    
    public ResetPriorityMessage(String queueName, String tenantId, 
                                JobPhaseType phase, int priority)
    {
        super(JobCommand.TPC_RESET_PRIORITY,
              queueName, 
              tenantId, 
              phase);
        this.priority = priority;
    }

    /* ********************************************************************** */
    /*                            Public Methods                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* validate:                                                              */
    /* ---------------------------------------------------------------------- */
    /** Make sure all fields are filled in.
     * 
     * @throws JobException if any field is not initialized
     */
    @Override
    public void validate() throws JobException
    {
        super.validate();
        if (priority < 1) {
            String msg = "Invalid priority value \"" + priority + " in " + 
                         getClass().getSimpleName() + " object.";
            throw new JobException(msg);
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* fromJson:                                                              */
    /* ---------------------------------------------------------------------- */
    /** Type-specific wrapper to superclass method.
     * 
     * @param json a json string that conforms to some subclass's serialization
     * @return the message object
     * @throws IOException if something goes wrong
     */
    public static ResetPriorityMessage fromJson(String json)
     throws IOException
    {
        ResetPriorityMessage m = (ResetPriorityMessage) AbstractQueueMessage.fromJson(json);
        if (m.command != JobCommand.TPC_RESET_PRIORITY)
        {
            String msg = "Invalid command value for ResetPriorityMessage: " + m.command;
            throw new IOException(msg);
        }
        return m;
    }
}
