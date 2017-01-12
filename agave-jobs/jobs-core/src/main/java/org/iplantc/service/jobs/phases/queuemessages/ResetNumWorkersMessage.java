package org.iplantc.service.jobs.phases.queuemessages;

import java.io.IOException;

import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;

/** This message is used to start the specified number of workers on
 * the specified queue.
 * 
 * @author rcardone
 */
public final class ResetNumWorkersMessage 
 extends AbstractQueueConfigMessage
{
    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    // Note that adding fields, or changing the names or ordering, will require 
    // changes to the QueueMessagesTest program.
    //
    // Command fields.
    public int    numWorkers;  // Target number of workers
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    public ResetNumWorkersMessage(){super(JobCommand.TPC_RESET_NUM_WORKERS);}
    
    public ResetNumWorkersMessage(String queueName, String tenantId, 
                                  JobPhaseType phase, int numWorkers)
    {
        super(JobCommand.TPC_RESET_NUM_WORKERS,
              queueName, 
              tenantId, 
              phase);
        this.numWorkers = numWorkers;
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
        if (numWorkers < 1) {
            String msg = "Invalid numWorkers value \"" + numWorkers + " in " + 
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
    public static ResetNumWorkersMessage fromJson(String json)
     throws IOException
    {
        ResetNumWorkersMessage m = (ResetNumWorkersMessage) AbstractQueueMessage.fromJson(json);
        if (m.command != JobCommand.TPC_RESET_NUM_WORKERS)
        {
            String msg = "Invalid command value for ResetNumWorkersMessage: " + m.command;
            throw new IOException(msg);
        }
        return m;
    }
}
