package org.iplantc.service.jobs.phases;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.phases.queuemessages.AbstractQueueJobMessage;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/** This class sends messages to the scheduler topic.  It maintains its own 
 * connection to the queueing system, which is shared by all message sending
 * routines.
 * 
 * The close method frees resources and is meant to be called only on system
 * shutdown.
 * 
 * @author rcardone
 */
public class TopicMessageSender
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(TopicMessageSender.class);
    
    // Outbound connection name for queueing system.
    private static final String OUTBOUND_CONNECTION_NAME = "Agave.TopicMessageSender.OutConnection";
    
    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    // This phase's queuing artifacts.
    private static ConnectionFactory    _factory;
    private static Connection           _outConnection;
    
    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* sendJobMessage:                                                        */
    /* ---------------------------------------------------------------------- */
    public static void sendJobMessage(AbstractQueueJobMessage message) 
      throws JobException
    {
        // Validate that the message is complete.
        if (message == null)
        {
            String msg = "Null message received by sendJobMessage().";
            _log.error(msg);
            throw new JobException(msg);
        }
        
        // Validate message.
        message.validate();
        
        // Get a new communication channel for each call.
        Channel topicChannel = getNewOutChannel();
        
        try {
            // Create the durable, non-autodelete topic exchange.
            boolean durable = true;
            try {topicChannel.exchangeDeclare(QueueConstants.TOPIC_EXCHANGE_NAME, "topic", durable);}
                catch (IOException e) {
                    String msg = "Unable to create exchange on " + OUTBOUND_CONNECTION_NAME + 
                                 "/" + topicChannel.getChannelNumber() + ": " + e.getMessage();
                    _log.error(msg, e);
                    throw new JobException(msg, e);
                }
        
            // Create the durable topic with a well-known name.
            durable = true;
            boolean exclusive = false;
            boolean autoDelete = false;
            try {topicChannel.queueDeclare(QueueConstants.TOPIC_QUEUE_NAME, 
                                           durable, exclusive, autoDelete, null);}
                catch (IOException e) {
                    String msg = "Unable to declare topic queue " + 
                                 QueueConstants.TOPIC_QUEUE_NAME +
                                 " on " + OUTBOUND_CONNECTION_NAME + "/" + 
                                 topicChannel.getChannelNumber() + ": " + e.getMessage();
                    _log.error(msg, e);
                    throw new JobException(msg, e);
                }
        
            // Serialize message.
            String json = null;
            try {json = message.toJson();}
            catch (Exception e) {
               String msg = "Unable to serialize " + message.command.name() +
                            ": " + e.getMessage();
               _log.error(msg, e);
               throw new JobException(msg, e);
            }
            
            // Send the message to the job topic.
            try {
                // TODO: Publisher confirm?
                topicChannel.basicPublish(QueueConstants.TOPIC_EXCHANGE_NAME, 
                                          QueueConstants.TOPIC_ALL_ROUTING_KEY, 
                                          QueueConstants.PERSISTENT_JSON, 
                                          json.getBytes());
            } 
            catch (Exception e) {
                String msg = "Unable to publish job interrupt message to topic " +
                             QueueConstants.TOPIC_QUEUE_NAME + ": " + json;
                _log.error(msg, e);
                throw new JobException(msg, e);
            }
        }
        finally {
            // Always close the topic channel.
            try {topicChannel.close();}
            catch (Exception e) {
                String msg = "Error closing topic channel";
                _log.error(msg, e);
            }
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* close:                                                                 */
    /* ---------------------------------------------------------------------- */
    /** Release all resources managed by this class.  This method should be 
     * called only when the jobs service is shutting down.  Static fields are 
     * reset to their uninitialized values. 
     */
    public static synchronized void close()
    {
        // Close our connection to the queueing system.
        if (_outConnection != null) { 
           try {_outConnection.close();} catch (Exception e){}
           _outConnection = null;
        }
        
        // Free the factory.
        _factory = null;
    }
    
    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* getConnectionFactory:                                                  */
    /* ---------------------------------------------------------------------- */
    /** Return a connection factory, creating it if necessary.
     * 
     * @return this class's queue connection factory.
     */
    private static ConnectionFactory getConnectionFactory()
    {
        // Create the factory if necessary.
        if (_factory == null) 
        {
            // Get a rabbitmq connection factory.
            _factory = new ConnectionFactory();
            
            // Set the factory parameters.
            // TODO: generalize w/auth & network info & heartbeat
            _factory.setHost("localhost");
            
            // Set automatic recover on.
            // TODO: Consider adding shutdown, cancel, recovery, etc. listeners.
            // TODO: Also consider how to handle unroutable messages
            _factory.setAutomaticRecoveryEnabled(true);
        }
        
        return _factory;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getOutConnection:                                                      */
    /* ---------------------------------------------------------------------- */
    /** Return a outbound connection to the queuing subsystem, creating the 
     * connection if necessary.  This method is synchronized to avoid creating
     * multiple connections.
     * 
     * @return this scheduler's connection
     * @throws JobException on error.
     */
    private static synchronized Connection getOutConnection()
     throws JobException
    {
        // Create the connection if necessary.
        if (_outConnection == null)
        {
            try {_outConnection = getConnectionFactory().newConnection(OUTBOUND_CONNECTION_NAME);}
            catch (IOException e) {
                String msg = "Unable to create new outbound connection to queuing subsystem: " +
                             e.getMessage();
                _log.error(msg, e);
                throw new JobException(msg, e);
            } catch (TimeoutException e) {
                String msg = "Timeout while creating new outbound connection to queuing subsystem: " + 
                             e.getMessage();
                _log.error(msg, e);
                throw new JobException(msg, e);
            }
        }
        
        return _outConnection;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getNewOutChannel:                                                      */
    /* ---------------------------------------------------------------------- */
    /** Return a new outbound channel on the existing queuing system connection.
     * 
     * @return the new channel
     * @throws JobException on error
     */
    private static synchronized Channel getNewOutChannel()
      throws JobException
    {
        // Create a new channel in this phase's connection.
        Channel channel = null;
        try {channel = getOutConnection().createChannel();} 
         catch (IOException e) {
             String msg = "Unable to create channel on " + OUTBOUND_CONNECTION_NAME + 
                          ": " + e.getMessage();
             _log.error(msg, e);
             throw new JobException(msg, e);
         }
         if (_log.isInfoEnabled()) 
             _log.info("Created channel number " + channel.getChannelNumber() + 
                       " on " + OUTBOUND_CONNECTION_NAME + ".");
         
         return channel;
    }
    
}
