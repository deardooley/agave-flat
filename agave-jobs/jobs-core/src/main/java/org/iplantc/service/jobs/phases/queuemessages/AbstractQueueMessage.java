package org.iplantc.service.jobs.phases.queuemessages;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.log4j.Logger;
import org.iplantc.service.jobs.model.enumerations.JobInterruptType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** This is the base class for all message that can be queued on
 * any distributed (i.e., RabbitMQ) job queue.
 * 
 * @author rcardone
 */
public abstract class AbstractQueueMessage 
{
    /* ********************************************************************** */
    /*                                Constants                               */
    /* ********************************************************************** */
    private static final Logger _log = Logger.getLogger(AbstractQueueMessage.class);
    
    // Reusable json mapper.
    private static final ObjectMapper _jsonMapper = new ObjectMapper();
    
    /* ********************************************************************** */
    /*                                  Enums                                 */
    /* ********************************************************************** */
    /* Enumeration of all command types.  Most commands are prefixed by their 
     * intended consumer, though commands with no prefix can appear on any
     * queue.
     *  
     *  Prefixes
     *  -------- 
     *  WKR -> worker threads
     *  TPC -> topic threads
     */
    public enum JobCommand { 
        NOOP,
        WKR_PROCESS_JOB,
        TPC_SHUTDOWN,
        TPC_PAUSE_JOB,
        TPC_DELETE_JOB,
        TPC_STOP_JOB,   
        TPC_RESET_NUM_WORKERS,
        TPC_REFRESH_QUEUE_INFO;
        
        // Convert a job command to job interrupt type.
        public JobInterruptType toInterruptType()
        {
            switch (this)
            {   
                // Only some commands interrupt jobs.
                case TPC_PAUSE_JOB:     return JobInterruptType.PAUSE; 
                case TPC_STOP_JOB:      return JobInterruptType.STOP;
                case TPC_DELETE_JOB:    return JobInterruptType.DELETE;
                default:                return null;
            }
        }
    }
    
    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    // Note that adding fields, or changing the names or ordering, will require 
    // changes to the QueueMessagesTest program.
    //
    // Identify the command to be executed.
    public final JobCommand command;
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    protected AbstractQueueMessage(JobCommand jobCommand){command = jobCommand;}
    
    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* toJson:                                                                */
    /* ---------------------------------------------------------------------- */
    /** Generate the json string representation of any subclass.
     * 
     * @return the json serialization of the concrete subclass
     * @throws IOException if serialization fails
     */
    public String toJson() throws IOException
    {
        // Write the object as a JSON string.
        StringWriter writer = new StringWriter(150);
        _jsonMapper.writeValue(writer, this);
        return writer.toString();
    }
    
    /* ---------------------------------------------------------------------- */
    /* fromJson:                                                              */
    /* ---------------------------------------------------------------------- */
    /** Create a message object from a json string.
     * 
     * @param json a json string that conforms to some subclass's serialization
     * @return the message object
     * @throws IOException if something goes wrong
     */
    public static AbstractQueueMessage fromJson(String json)
     throws IOException
    {
        // Read the json message generically.
        JsonNode node = null;
        try {node = _jsonMapper.readTree(json);}
            catch (Exception e) {
                String msg = "Invalid queue message received json: " + json;
                _log.error(msg, e);
                throw e;
            }
        if (node == null) 
        {
            String msg = "Empty queue message received json: " + json;
            _log.error(msg);
            throw new IOException(msg);
        }
        
        // Get the command field from the queued json.
        JobCommand command = null;
        String cmd = node.path("command").asText();
        try {command = JobCommand.valueOf(cmd);}
        catch (Exception e) {
            String msg = "Invalid queue message command received: " + cmd;
            _log.error(msg, e);
            throw new IOException(msg, e);
        }
        
        // Determine the concrete class based on all known commands.
        Class<?> cls = null;
        switch (command)
        {
            case NOOP:
                cls = NoOpMessage.class;
                break;
            case WKR_PROCESS_JOB:
                cls = ProcessJobMessage.class;
                break;
            case TPC_SHUTDOWN:
                cls = ShutdownMessage.class;
                break;
            case TPC_PAUSE_JOB:
                cls = PauseJobMessage.class;
                break;
            case TPC_DELETE_JOB:
                cls = DeleteJobMessage.class;
                break;
            case TPC_STOP_JOB:
                cls = StopJobMessage.class;
                break;
            case TPC_RESET_NUM_WORKERS:
                cls = ResetNumWorkersMessage.class;
                break;
            case TPC_REFRESH_QUEUE_INFO:
                cls = RefreshQueueInfoMessage.class;
                break;
            default:
                String msg = "Unknown queue message command encountered: " + command;
                _log.error(msg);
                throw new IOException(msg);
        }
        
        // Create and populate the new message object.
        AbstractQueueMessage qmsg = null;
        try {qmsg = (AbstractQueueMessage) _jsonMapper.readValue(json, cls);}
        catch (IOException e) {
            String msg = "Unable to populate queue message with received json: " + json;
            _log.error(msg, e);
            throw new IOException(msg, e);
        }
        
        // Success.
        return qmsg;
    }
}