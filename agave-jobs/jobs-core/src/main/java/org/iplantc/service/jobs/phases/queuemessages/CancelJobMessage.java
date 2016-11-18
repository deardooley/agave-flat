package org.iplantc.service.jobs.phases.queuemessages;

import java.io.IOException;

import org.iplantc.service.jobs.phases.queuemessages.AbstractQueueMessage.JobCommand;

/** This message cancels the job identified by uuid.
 * 
 * @author rcardone
 */
public final class CancelJobMessage 
 extends AbstractQueueMessage
{
    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    // Job name and unique id.
    public String name;
    public String uuid;
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    public CancelJobMessage(){super(JobCommand.TCP_CANCEL_JOB);}
    
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
    public static CancelJobMessage fromJson(String json)
     throws IOException
    {
        CancelJobMessage m = (CancelJobMessage) AbstractQueueMessage.fromJson(json);
        if (m.command != JobCommand.TCP_CANCEL_JOB)
        {
            String msg = "Invalid command value for CancelJobMessage: " + m.command;
            throw new IOException(msg);
        }
        return m;
    }
}
