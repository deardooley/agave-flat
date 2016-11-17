package org.iplantc.service.jobs.phases.queuemessages;

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
}
