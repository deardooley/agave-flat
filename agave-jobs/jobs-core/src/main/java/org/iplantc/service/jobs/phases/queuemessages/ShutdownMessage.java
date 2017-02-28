package org.iplantc.service.jobs.phases.queuemessages;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;

/** This message is used the shutdown all threads in the listed phases.
 * 
 * @author rcardone
 */
public final class ShutdownMessage 
 extends AbstractQueueMessage
{
    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    // Job name and unique id.
    public List<JobPhaseType> phases = new LinkedList<>();
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    public ShutdownMessage(){super(JobCommand.TPC_SHUTDOWN);}
    
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
    public static ShutdownMessage fromJson(String json)
     throws IOException
    {
        ShutdownMessage m = (ShutdownMessage) AbstractQueueMessage.fromJson(json);
        if (m.command != JobCommand.TPC_SHUTDOWN)
        {
            String msg = "Invalid command value for ShutdownMessage: " + m.command;
            throw new IOException(msg);
        }
        return m;
    }
    
    /* ---------------------------------------------------------------------- */
    /* validate:                                                              */
    /* ---------------------------------------------------------------------- */
    /** Make sure all fields are filled in.
     * 
     * @throws JobException if any field is not initialized
     */
    public void validate() throws JobException
    {
        // The list can be empty by not null.
        if (phases == null) {
            String msg = "Null phases list in " + 
                         getClass().getSimpleName() + " object.";
            throw new JobException(msg);
        }
    }
}
