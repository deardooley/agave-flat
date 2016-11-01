package org.iplantc.service.jobs.phases;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.iplantc.service.jobs.phases.schedulers.AbstractPhaseScheduler;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

/**
 * @author rcardone
 *
 */
public class PhaseWorker 
 extends Thread
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(PhaseWorker.class);
    
    // Communication constants.
    private static final String WORKER_EXCHANGE_NAME = "JobWorkerExchange";

    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    // Input fields.
    private final Connection             _connection;
    private final AbstractPhaseScheduler _scheduler;
    private final String                 _tenantId;
    private final String                 _queueName;
    private final int                    _threadNum;
    
    // Calculated fields.
    private Channel                      _channel;
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    public PhaseWorker(PhaseWorkerParms parms) 
    {
        super(parms.threadGroup, parms.threadName);
        _connection = parms.connection;
        _scheduler = parms.scheduler;
        _tenantId = parms.tenantId;
        _queueName = parms.queueName;
        _threadNum = parms.threadNum;
    }
    
    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* run:                                                                   */
    /* ---------------------------------------------------------------------- */
    @Override
    public void run() {
        
        // Create the channel and bind our queue to it.
        _channel = initChannel();
        
        // Exit without a fuss if something's wrong.
        if (_channel == null) return;
        
        // Create the topic queue consumer.
        Consumer consumer = new DefaultConsumer(_channel) {
          @Override
          public void handleDelivery(String consumerTag, Envelope envelope,
                                     AMQP.BasicProperties properties, byte[] body) 
            throws IOException 
          {
            // Process messages read from topic.
            String message = null;
            try {message = new String(body);}
            catch (Exception e)
            {
                String msg = "Worker " + getName() + 
                             " cannot decode data from queue " + 
                             _queueName + ": " + e.getMessage();
                _log.error(msg, e);
            }
            
            // For now, just print what we receive.
            // TODO: create command processor 
            if (message != null)
               System.out.println("Worker " + getName() +  
                                  " received message:\n" +
                                  message);
            
            // Don't forget to send the ack!
            boolean multipleAck = false;
            _channel.basicAck(envelope.getDeliveryTag(), multipleAck);
          }
        };
        
        // Tracing.
        if (_log.isDebugEnabled())
            _log.debug("[*] Worker " + getName() + " waiting on queue " + 
                        _queueName + ".");

        // We auto-acknowledge topic broadcasts.
        boolean autoack = false;
        try {_channel.basicConsume(_queueName, autoack, consumer);}
        catch (IOException e) {
            String msg = "Worker " + getName() + " is unable consume messages from queue " + 
                         _queueName + ".";
            _log.error(msg, e);
            try {_channel.abort(AMQP.CHANNEL_ERROR, msg);} catch (Exception e1){}
            return; // TODO: figure out better longterm strategy.
        }
    }

    /* ********************************************************************** */
    /*                               Accessors                                */
    /* ********************************************************************** */
    public AbstractPhaseScheduler get_scheduler() {
        return _scheduler;
    }

    public String getTenantId() {
        return _tenantId;
    }

    public String getQueueName() {
        return _queueName;
    }

    public int getThreadNum() {
        return _threadNum;
    }

    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* initChannel:                                                           */
    /* ---------------------------------------------------------------------- */
    public Channel initChannel() {
        
        // Create this thread's channel.
        Channel channel = null;
        try {channel = _connection.createChannel();} 
             catch (IOException e) {
                 String msg = "Worker " + getName() + " aborting!  " + 
                              "Unable to create channel for queue " + _queueName + 
                              ": " + e.getMessage();
                 _log.error(msg, e);
             
                 // TODO: We need to raise an exception in a way that does not
                 //       cause an infinite cascade of new thread launches, but
                 //       does signal that there's a serious problem.
                 return null;
         }
         if (_log.isInfoEnabled()) 
             _log.info("Created channel number " + channel.getChannelNumber() + 
                       " for worker " + getName() + ".");
         
         // Set the prefetch count so that the consumer using this 
         // channel only receieves the next request after the previous
         // request has been acknowledged.
         int prefetchCount = 1;
         try {channel.basicQos(prefetchCount);}
             catch (IOException e) {
                 String msg = "Worker " + getName() + " aborting!  " + 
                         "Unable to set prefetch count for queue " + _queueName + 
                         ": " + e.getMessage();
                 _log.error(msg, e);
            
                 // TODO: We need to raise an exception in a way that does not
                 //       cause an infinite cascade of new thread launches, but
                 //       does signal that there's a serious problem.
                 return null;
             }
         
        // Create this thread's exchange.
         boolean durable = true;
         try {channel.exchangeDeclare(WORKER_EXCHANGE_NAME, "direct", durable);}
             catch (IOException e) {
                 String msg = "Worker " + getName() + " aborting!  " + 
                         "Unable to create exchange for queue " + _queueName + 
                         ": " + e.getMessage();
                 _log.error(msg, e);
            
                 // TODO: We need to raise an exception in a way that does not
                 //       cause an infinite cascade of new thread launches, but
                 //       does signal that there's a serious problem.
                 try {channel.abort(AMQP.CHANNEL_ERROR, msg);} catch (Exception e1){}
                 return null;
             }
         
         // Create the queue with the configured name.
         durable = true;
         boolean exclusive = false;
         boolean autoDelete = false;
         try {channel.queueDeclare(_queueName, durable, exclusive, autoDelete, null);}
             catch (IOException e) {
                 String msg = "Worker " + getName() + " aborting!  " + 
                         "Unable to create queue " + _queueName + 
                         ": " + e.getMessage();
                 _log.error(msg, e);
            
                 // TODO: We need to raise an exception in a way that does not
                 //       cause an infinite cascade of new thread launches, but
                 //       does signal that there's a serious problem.
                 try {channel.abort(AMQP.CHANNEL_ERROR, msg);} catch (Exception e1){}
                 return null;
             }
        
         // Bind the queue to the exchange using the queue name as the binding key.
         try {channel.queueBind(_queueName, WORKER_EXCHANGE_NAME, _queueName);}
             catch (IOException e) {
                 String msg = "Worker " + getName() + " aborting!  " + 
                         "Unable to bind queue " + _queueName + 
                         ": " + e.getMessage();
                 _log.error(msg, e);
                
                 // TODO: We need to raise an exception in a way that does not
                 //       cause an infinite cascade of new thread launches, but
                 //       does signal that there's a serious problem.
                 try {channel.abort(AMQP.CHANNEL_ERROR, msg);} catch (Exception e1){}
                 return null;
             }
         
         return channel;
    }
}
