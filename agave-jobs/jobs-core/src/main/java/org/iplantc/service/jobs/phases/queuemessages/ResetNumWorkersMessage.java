package org.iplantc.service.jobs.phases.queuemessages;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;

/** This message is used to increase or decrease the specified number of workers on
 * the named queue.
 * 
 * @author rcardone
 */
public final class ResetNumWorkersMessage 
 extends AbstractQueueConfigMessage
{
    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    // Note that adding fields, or changing the names or ordering, will require 
    // changes to the QueueMessagesTest program.
    
    // This value is a delta that specifies changes to the number of workers
    // servicing the named queue.  Negative numbers decrease workers, positive 
    // numbers increase workers.
    public int          numWorkersDelta;
    
    // Optional parameter that specifies the unique scheduler instance names
    // to which this message should be applied.  If the list is empty, then
    // all scheduler instances assigned to the same phase as the target queue
    // will run this command.  Otherwise, only those instances in the list will
    // be affected.  The names returned by AbstractPhaseScheduler.getSchedulerName()
    // should populate this list.
    public List<String> schedulers = new LinkedList<>();
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    public ResetNumWorkersMessage(){super(JobCommand.TPC_RESET_NUM_WORKERS);}
    
    public ResetNumWorkersMessage(String queueName, String tenantId, 
                                  JobPhaseType phase, int numWorkers)
    {
        super(JobCommand.TPC_RESET_NUM_WORKERS,
              queueName, 
              tenantId, 
              phase);
        this.numWorkersDelta = numWorkers;
    }

    /* ********************************************************************** */
    /*                            Public Methods                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* validate:                                                              */
    /* ---------------------------------------------------------------------- */
    /** Make sure all fields are filled in.
     * 
     * @throws JobException if any field is not initialized
     */
    @Override
    public void validate() throws JobException
    {
        super.validate();
        if (numWorkersDelta == 0) {
            String msg = "Invalid numWorkers value \"" + numWorkersDelta + " in " + 
                         getClass().getSimpleName() + " object.  Specify a " +
                         "positive or negative integer to add or subtract workers, " +
                         "respectively.";
            throw new JobException(msg);
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* fromJson:                                                              */
    /* ---------------------------------------------------------------------- */
    /** Type-specific wrapper to superclass method.
     * 
     * @param json a json string that conforms to some subclass's serialization
     * @return the message object
     * @throws IOException if something goes wrong
     */
    public static ResetNumWorkersMessage fromJson(String json)
     throws IOException
    {
        ResetNumWorkersMessage m = (ResetNumWorkersMessage) AbstractQueueMessage.fromJson(json);
        if (m.command != JobCommand.TPC_RESET_NUM_WORKERS)
        {
            String msg = "Invalid command value for ResetNumWorkersMessage: " + m.command;
            throw new IOException(msg);
        }
        return m;
    }
}
