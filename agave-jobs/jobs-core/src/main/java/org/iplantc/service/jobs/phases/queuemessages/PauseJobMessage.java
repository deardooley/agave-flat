package org.iplantc.service.jobs.phases.queuemessages;

import java.io.IOException;

import org.iplantc.service.jobs.phases.queuemessages.AbstractQueueMessage.JobCommand;

/** This message pauses the job identified by uuid..
 * 
 * @author rcardone
 */
public final class PauseJobMessage 
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
    public PauseJobMessage(){super(JobCommand.TCP_PAUSE_JOB);}
    
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
    public static PauseJobMessage fromJson(String json)
     throws IOException
    {
        PauseJobMessage m = (PauseJobMessage) AbstractQueueMessage.fromJson(json);
        if (m.command != JobCommand.TCP_PAUSE_JOB)
        {
            String msg = "Invalid command value for PauseJobMessage: " + m.command;
            throw new IOException(msg);
        }
        return m;
    }
}
