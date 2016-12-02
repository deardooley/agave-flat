package org.iplantc.service.jobs.phases.queuemessages;

import java.io.IOException;

/** This message cancels the job identified by uuid.
 * 
 * @author rcardone
 */
public final class DeleteJobMessage 
 extends AbstractQueueJobMessage
{
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    public DeleteJobMessage(){super(JobCommand.TCP_DELETE_JOB);}
    
    public DeleteJobMessage(String     jobName,
                            String     jobUuid,
                            String     tenantId)
    {
        super(JobCommand.TCP_DELETE_JOB, jobName, jobUuid, tenantId);
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
        if (m.command != JobCommand.TCP_DELETE_JOB)
        {
            String msg = "Invalid command value for DeleteJobMessage: " + m.command;
            throw new IOException(msg);
        }
        return m;
    }
}
