package org.iplantc.service.jobs.phases.queuemessages;

import java.io.IOException;

import org.iplantc.service.jobs.phases.queuemessages.AbstractQueueMessage.JobCommand;

/** This message stops the job identified by uuid.
 * 
 * @author rcardone
 */
public final class StopJobMessage
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
    public StopJobMessage(){super(JobCommand.TCP_STOP_JOB);}
    
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
    public static StopJobMessage fromJson(String json)
     throws IOException
    {
        StopJobMessage m = (StopJobMessage) AbstractQueueMessage.fromJson(json);
        if (m.command != JobCommand.TCP_STOP_JOB)
        {
            String msg = "Invalid command value for StopJobMessage: " + m.command;
            throw new IOException(msg);
        }
        return m;
    }
}
