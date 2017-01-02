package org.iplantc.service.jobs.phases.queuemessages;

import java.io.IOException;

/** This message is used to terminate the specified number of workers on
 * the specified queue.
 * 
 * @author rcardone
 */
public final class TerminateWorkersMessage 
 extends AbstractQueueMessage
{
    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    // Command fields.
    public String tenantId;    // Caller's tenantId
    public String queueName;   // Target queue name
    public int    numWorkers;  // Number of workers to terminate
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    public TerminateWorkersMessage(){super(JobCommand.TPC_TERMINATE_WORKERS);}
    
    /* ********************************************************************** */
    /*                            Public Methods                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* fromJson:                                                              */
    /* ---------------------------------------------------------------------- */
    /** Type-specific wrapper to superclass method.
     * 
     * @param json a json string that conforms to some subclass's serialization
     * @return the message object
     * @throws IOException if something goes wrong
     */
    public static TerminateWorkersMessage fromJson(String json)
     throws IOException
    {
        TerminateWorkersMessage m = (TerminateWorkersMessage) AbstractQueueMessage.fromJson(json);
        if (m.command != JobCommand.TPC_TERMINATE_WORKERS)
        {
            String msg = "Invalid command value for TerminateWorkersMessage: " + m.command;
            throw new IOException(msg);
        }
        return m;
    }
}
