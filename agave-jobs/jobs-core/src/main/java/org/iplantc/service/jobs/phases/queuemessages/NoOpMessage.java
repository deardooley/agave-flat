package org.iplantc.service.jobs.phases.queuemessages;

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
}
