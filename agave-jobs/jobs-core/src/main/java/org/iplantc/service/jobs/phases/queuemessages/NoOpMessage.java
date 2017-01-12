package org.iplantc.service.jobs.phases.queuemessages;

import java.io.IOException;

/** This message class adds a field that allows a test message
 * to be specified.  If present, the message will be printed in 
 * log record.  
 * 
 * @author rcardone
 */
public final class NoOpMessage 
 extends AbstractQueueMessage
{
    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    // Optional string that gets logged if present.
    public String testMessage;
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    public NoOpMessage(){super(JobCommand.NOOP);}
    
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
    public static NoOpMessage fromJson(String json)
     throws IOException
    {
        NoOpMessage m = (NoOpMessage) AbstractQueueMessage.fromJson(json);
        if (m.command != JobCommand.NOOP)
        {
            String msg = "Invalid command value for NoOpMessage: " + m.command;
            throw new IOException(msg);
        }
        return m;
    }
}
