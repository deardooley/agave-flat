package org.iplantc.service.jobs.phases.queuemessages;

import java.io.IOException;

/** This message is used to terminate the specified number of workers on
 * the specified queue.
 * 
 * @author rcardone
 */
public final class RefreshQueueInfoMessage 
 extends AbstractQueueMessage
{
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    public RefreshQueueInfoMessage(){super(JobCommand.TPC_REFRESH_QUEUE_INFO);}
    
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
    public static RefreshQueueInfoMessage fromJson(String json)
     throws IOException
    {
        RefreshQueueInfoMessage m = (RefreshQueueInfoMessage) AbstractQueueMessage.fromJson(json);
        if (m.command != JobCommand.TPC_REFRESH_QUEUE_INFO)
        {
            String msg = "Invalid command value for ResetPriorityMessage: " + m.command;
            throw new IOException(msg);
        }
        return m;
    }
}
