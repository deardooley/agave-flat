package org.iplantc.service.jobs.phases.queuemessages;

import java.io.IOException;

/** This message stops the job identified by uuid.
 * 
 * See the class comment in JobInterruptDao for a discussion of job epochs.
 * 
 * @author rcardone
 */
public final class StopJobMessage
 extends AbstractQueueJobMessage
{
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    // Constructor used for testing only.
    StopJobMessage(){super(JobCommand.TPC_STOP_JOB);}
    
    // Real constructor.
    public StopJobMessage(String     jobName,
                          String     jobUuid,
                          String     tenantId,
                          int        epoch)
    {
        super(JobCommand.TPC_STOP_JOB, jobName, jobUuid, tenantId, epoch);
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
    public static StopJobMessage fromJson(String json)
     throws IOException
    {
        StopJobMessage m = (StopJobMessage) AbstractQueueMessage.fromJson(json);
        if (m.command != JobCommand.TPC_STOP_JOB)
        {
            String msg = "Invalid command value for StopJobMessage: " + m.command;
            throw new IOException(msg);
        }
        return m;
    }
}
