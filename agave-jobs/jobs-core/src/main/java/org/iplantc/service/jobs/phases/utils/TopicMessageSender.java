package org.iplantc.service.jobs.phases.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.iplantc.service.jobs.phases.queuemessages.AbstractQueueConfigMessage;
import org.iplantc.service.jobs.phases.queuemessages.AbstractQueueJobMessage;
import org.iplantc.service.jobs.phases.queuemessages.AbstractQueueMessage;
import org.iplantc.service.jobs.phases.queuemessages.ResetNumWorkersMessage;
import org.iplantc.service.jobs.phases.queuemessages.ShutdownMessage;

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
    /** Put a job interrupt message on the job topic queue.  The caller is 
     * responsible for already having updated the status of the job.  The topic
     * thread will read the queued interrupt message and update the interrupts
     * table with the job information.  Any worker thread subsequently servicing
     * the job will note either the interrupt or the status change.  
     * 
     * @param message a concrete job message
     * @throws JobException on error
     */
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
            // Create the durable, non-autodelete topic exchange and topic queue.
            initQueue(topicChannel);
        
            // Serialize message.
            String json = messageToJson(message);
            
            // Send the message to the job topic so that exactly one scheduler 
            // reads the message.  The choice of scheduler is arbitrary.
            try {
                // TODO: Publisher confirm?
                topicChannel.basicPublish(QueueConstants.TOPIC_EXCHANGE_NAME, 
                                          QueueConstants.TOPIC_STAGING_ROUTING_KEY, 
                                          QueueConstants.PERSISTENT_JSON, 
                                          json.getBytes());
            } 
            catch (Exception e) {
                String msg = "Unable to publish job interrupt message to topic " +
                             QueueConstants.TOPIC_QUEUE_PREFIX + ": " + json;
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
    /* sendConfigMessage:                                                     */
    /* ---------------------------------------------------------------------- */
    /** Route a queue configuration message on the job topic queue.    
     * 
     * @param message a concrete queue configuration message
     * @throws JobException on error
     */
    public static void sendConfigMessage(AbstractQueueConfigMessage message) 
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
            // Create the durable, non-autodelete topic exchange and topic queue.
            initQueue(topicChannel);
        
            // Serialize message.
            String json = messageToJson(message);
            
            // The phase determines the one scheduler that will service this message.
            // The topic thread on the target scheduler will receive the message.
            String routingKey = null;
            switch (message.phase)
            {
                case ARCHIVING:
                    routingKey = QueueConstants.TOPIC_ARCHIVING_ROUTING_KEY;
                    break;
                case MONITORING:
                    routingKey = QueueConstants.TOPIC_MONITORING_ROUTING_KEY;
                    break;
                case STAGING:
                    routingKey = QueueConstants.TOPIC_STAGING_ROUTING_KEY;
                    break;
                case SUBMITTING:
                    routingKey = QueueConstants.TOPIC_SUBMITTING_ROUTING_KEY;
                    break;
                default:
                    String msg = "Unknown phase " + message.phase + 
                                 " encountered during topic message processing.";
                    _log.error(msg);
                    throw new JobException(msg);
            }
            
            // Send the message to the job topic so that only the  chosen scheduler 
            // reads the message.  
            try {
                // TODO: Publisher confirm?
                topicChannel.basicPublish(QueueConstants.TOPIC_EXCHANGE_NAME, 
                                          routingKey, 
                                          QueueConstants.PERSISTENT_JSON, 
                                          json.getBytes());
            } 
            catch (Exception e) {
                String msg = "Unable to publish queue configuration message to topic " +
                             QueueConstants.TOPIC_QUEUE_PREFIX + ": " + json;
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
    /* sendShutdownMessage:                                                   */
    /* ---------------------------------------------------------------------- */
    /** Put a shutdown message on the topic queue for selected schedulers or for
     * all schedulers if none are explicitly specified in the request.  On the
     * receiving end, each scheduler inspects double-checks the phases list in
     * the request to determine if the message applies to them.  
     * 
     * @param message a shutdown message
     * @throws JobException on error
     */
    public static void sendShutdownMessage(ShutdownMessage message) 
      throws JobException
    {
        // Validate that the message is complete.
        if (message == null)
        {
            String msg = "Null message received by sendShutdownMessage().";
            _log.error(msg);
            throw new JobException(msg);
        }
        
        // Validate message.
        message.validate();
        
        // Get a new communication channel for each call.
        Channel topicChannel = getNewOutChannel();
        
        try {
            // Create the durable, non-autodelete topic exchange and topic queue.
            initQueue(topicChannel);
        
            // Serialize message.
            String json = messageToJson(message);
            
            // Assign routing keys.
            ArrayList<String> routingKeys = new ArrayList<>(4);
            if (message.phases.isEmpty())
                routingKeys.add(QueueConstants.TOPIC_ALL_ROUTING_KEY);
            else {
                if (message.phases.contains(JobPhaseType.STAGING))
                    routingKeys.add(QueueConstants.TOPIC_STAGING_ROUTING_KEY);
                if (message.phases.contains(JobPhaseType.SUBMITTING))
                    routingKeys.add(QueueConstants.TOPIC_SUBMITTING_ROUTING_KEY);
                if (message.phases.contains(JobPhaseType.MONITORING))
                    routingKeys.add(QueueConstants.TOPIC_MONITORING_ROUTING_KEY);
                if (message.phases.contains(JobPhaseType.ARCHIVING))
                    routingKeys.add(QueueConstants.TOPIC_ARCHIVING_ROUTING_KEY);
            }
            
            // Send the message to all topic threads targeted by the shutdown command. 
            String routingKey = null;
            ListIterator<String> it = routingKeys.listIterator();
            try {
                while (it.hasNext()) {
                    // Assign next key.
                    routingKey = it.next();
                    
                    // TODO: Publisher confirm?
                    topicChannel.basicPublish(QueueConstants.TOPIC_EXCHANGE_NAME, 
                                              routingKey, 
                                              QueueConstants.PERSISTENT_JSON, 
                                              json.getBytes());
                }
            } 
            catch (Exception e) {
                String msg = "Unable to publish shutdown message to topic " +
                             QueueConstants.TOPIC_QUEUE_PREFIX + "with routing key " + 
                             routingKey + ": " + json;
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
    /* ResetNumWorkersMessage:                                                */
    /* ---------------------------------------------------------------------- */
    /** Put a ResetNumWorkers message the topic queue for schedulers managing
     * the specified queue.  Each scheduler inspects the phases list to determine 
     * if the message implicitly or explicitly applies to them.    
     * 
     * @param message a shutdown message
     * @throws JobException on error
     */
    public static void sendResetNumWorkersMessage(ResetNumWorkersMessage message) 
      throws JobException
    {
        // Validate that the message is complete.
        if (message == null)
        {
            String msg = "Null message received by sendShutdownMessage().";
            _log.error(msg);
            throw new JobException(msg);
        }
        
        // Validate message.
        message.validate();
        
        // Get a new communication channel for each call.
        Channel topicChannel = getNewOutChannel();
        
        try {
            // Create the durable, non-autodelete topic exchange and topic queue.
            initQueue(topicChannel);
        
            // Serialize message.
            String json = messageToJson(message);
            
            // Assign routing keys.
            String routingKey = null;
            if (message.phase == JobPhaseType.STAGING)
                routingKey = QueueConstants.TOPIC_STAGING_ROUTING_KEY;
            else if ((message.phase == JobPhaseType.SUBMITTING))
                routingKey = QueueConstants.TOPIC_SUBMITTING_ROUTING_KEY;
            else if ((message.phase == JobPhaseType.MONITORING))
                routingKey = QueueConstants.TOPIC_MONITORING_ROUTING_KEY;
            else if ((message.phase == JobPhaseType.ARCHIVING))
                routingKey = QueueConstants.TOPIC_ARCHIVING_ROUTING_KEY;
            else {
                String msg = "Unknown scheduler phase encountered: " + message.phase.name();
                _log.error(msg);
                throw new JobException(msg);
            }
            
            // Send the message to all topic threads targeted by the shutdown command. 
            try {
                // TODO: Publisher confirm?
                topicChannel.basicPublish(QueueConstants.TOPIC_EXCHANGE_NAME, 
                                          routingKey, 
                                          QueueConstants.PERSISTENT_JSON, 
                                          json.getBytes());
            } 
            catch (Exception e) {
                String msg = "Unable to publish ResetNumWorkers message to topic " +
                             QueueConstants.TOPIC_QUEUE_PREFIX + "with routing key " + 
                             routingKey + ": " + json;
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
 
    /* ---------------------------------------------------------------------- */
    /* initQueue:                                                             */
    /* ---------------------------------------------------------------------- */
    /** Create or initialize the topic exchange and queue.
     * 
     * @param topicChannel channel on which the exchange will be declared 
     *                     and queue bound
     * @throws JobException on error
     */
    private static void initQueue(Channel topicChannel)
     throws JobException
    {
        // Create the durable, non-autodelete topic exchange.
        boolean durable = true;
        try {topicChannel.exchangeDeclare(QueueConstants.TOPIC_EXCHANGE_NAME, "topic", durable);}
            catch (IOException e) {
                String msg = "Unable to create exchange on " + OUTBOUND_CONNECTION_NAME + 
                             "/" + topicChannel.getChannelNumber() + ": " + e.getMessage();
                _log.error(msg, e);
                throw new JobException(msg, e);
            }
    
        // Configure the durable topic queues with a well-known names.
        durable = true;
        boolean exclusive = false;
        boolean autoDelete = false;
        
        // Create each phase's topic queue if necessary.
        JobPhaseType[] phases = JobPhaseType.values();
        for (JobPhaseType phase : phases) {
            
            // No harm done if queues already exist and have been created with the same options.
            String queueName = QueueUtils.getTopicQueueName(phase);
            try {topicChannel.queueDeclare(queueName, durable, exclusive, autoDelete, null);}
                catch (IOException e) {
                    String msg = "Unable to declare topic queue " + 
                                 queueName + " on " + OUTBOUND_CONNECTION_NAME + "/" + 
                                 topicChannel.getChannelNumber() + ": " + e.getMessage();
                    _log.error(msg, e);
                    throw new JobException(msg, e);
                }
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* messageToJson:                                                         */
    /* ---------------------------------------------------------------------- */
    /** Serialize a message into a json string.
     * 
     * @param message source message
     * @return json string
     * @throws JobException on error
     */
    private static String messageToJson(AbstractQueueMessage message)
     throws JobException
    {
        // Serialize the message to json.
        String json = null;
        try {json = message.toJson();}
        catch (Exception e) {
           String msg = "Unable to serialize " + message.command.name() +
                        ": " + e.getMessage();
           _log.error(msg, e);
           throw new JobException(msg, e);
        }
        
        return json;
    }
}
