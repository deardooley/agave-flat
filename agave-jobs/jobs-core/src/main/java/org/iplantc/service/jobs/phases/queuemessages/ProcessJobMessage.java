package org.iplantc.service.jobs.phases.queuemessages;

import java.io.IOException;

/** This is the main job execution message.  When a worker thread receives
 * this message from its queue, the thread processes the job according its
 * assigned phase.
 * 
 * @author rcardone
 */
public final class ProcessJobMessage 
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
    public ProcessJobMessage(){super(JobCommand.WKR_PROCESS_JOB);}
    
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
    public static ProcessJobMessage fromJson(String json)
     throws IOException
    {
        ProcessJobMessage m = (ProcessJobMessage) AbstractQueueMessage.fromJson(json);
        return m;
    }
}
