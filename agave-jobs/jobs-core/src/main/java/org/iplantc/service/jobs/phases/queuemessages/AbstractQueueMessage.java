package org.iplantc.service.jobs.phases.queuemessages;

/** This is the base class for all message that can be queued on
 * any distributed (i.e., RabbitMQ) job queue.
 * 
 * @author rcardone
 */
public abstract class AbstractQueueMessage 
{
    /* ********************************************************************** */
    /*                                  Enums                                 */
    /* ********************************************************************** */
    /* Enumeration of all command types.  The commands are prefixed by their 
     * intended consumer.
     *  
     *  Prefixes
     *  -------- 
     *  WKR -> worker threads
     *  TPC -> topic threads
     */
    public enum JobCommand { 
        NOOP,
        WKR_PROCESS_JOB
    }
    
    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    // Identify the command to be executed.
    public final JobCommand command;
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    protected AbstractQueueMessage(JobCommand jobCommand){this.command = jobCommand;}
}
