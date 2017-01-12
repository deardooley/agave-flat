package org.iplantc.service.jobs.phases.queuemessages;

import java.io.IOException;

import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;

/** This message is used to start the specified number of workers on
 * the specified queue.
 * 
 * @author rcardone
 */
public final class ResetMaxMessagesMessage 
 extends AbstractQueueConfigMessage
{
    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    // Note that adding fields, or changing the names or ordering, will require 
    // changes to the QueueMessagesTest program.
    //
    // Command fields.
    public int    maxMessages; // Maximum number of messages on queue
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    public ResetMaxMessagesMessage(){super(JobCommand.TPC_RESET_MAX_MESSAGES);}
    
    public ResetMaxMessagesMessage(String queueName, String tenantId, 
                                   JobPhaseType phase, int maxMessages)
    {
        super(JobCommand.TPC_RESET_MAX_MESSAGES,
              queueName, 
              tenantId, 
              phase);
        this.maxMessages = maxMessages;
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
        if (maxMessages < 1) {
            String msg = "Invalid maxMessages value \"" + maxMessages + " in " + 
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
    public static ResetMaxMessagesMessage fromJson(String json)
     throws IOException
    {
        ResetMaxMessagesMessage m = (ResetMaxMessagesMessage) AbstractQueueMessage.fromJson(json);
        if (m.command != JobCommand.TPC_RESET_MAX_MESSAGES)
        {
            String msg = "Invalid command value for ResetMaxMessagesMessage: " + m.command;
            throw new IOException(msg);
        }
        return m;
    }
}
