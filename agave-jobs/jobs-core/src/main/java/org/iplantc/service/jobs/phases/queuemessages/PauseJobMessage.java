package org.iplantc.service.jobs.phases.queuemessages;

import java.io.IOException;

/** This message pauses the job identified by uuid..
 * 
 * @author rcardone
 */
public final class PauseJobMessage 
 extends AbstractQueueJobMessage
{
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    // Constructor used for testing only.
    PauseJobMessage(){super(JobCommand.TPC_PAUSE_JOB);}
    
    // Real constructor.
    public PauseJobMessage(String     jobName,
                           String     jobUuid,
                           String     tenantId)
    {
        super(JobCommand.TPC_PAUSE_JOB, jobName, jobUuid, tenantId);
    }
    
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
        if (m.command != JobCommand.TPC_PAUSE_JOB)
        {
            String msg = "Invalid command value for PauseJobMessage: " + m.command;
            throw new IOException(msg);
        }
        return m;
    }
}
