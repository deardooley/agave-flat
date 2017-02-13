package org.iplantc.service.jobs.phases.queuemessages;

import java.io.IOException;

/** This message cancels the job identified by uuid.
 * 
 * See the class comment in JobInterruptDao for a discussion of job epochs.
 * 
 * @author rcardone
 */
public final class DeleteJobMessage 
 extends AbstractQueueJobMessage
{
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    // Constructor used for testing only.
    DeleteJobMessage(){super(JobCommand.TPC_DELETE_JOB);}
    
    // Real constructor.
    public DeleteJobMessage(String     jobName,
                            String     jobUuid,
                            String     tenantId,
                            int        epoch)
    {
        super(JobCommand.TPC_DELETE_JOB, jobName, jobUuid, tenantId, epoch);
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
    public static DeleteJobMessage fromJson(String json)
     throws IOException
    {
        DeleteJobMessage m = (DeleteJobMessage) AbstractQueueMessage.fromJson(json);
        if (m.command != JobCommand.TPC_DELETE_JOB)
        {
            String msg = "Invalid command value for DeleteJobMessage: " + m.command;
            throw new IOException(msg);
        }
        return m;
    }
}
