package org.iplantc.service.jobs.phases;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.iplantc.service.jobs.phases.schedulers.AbstractPhaseScheduler;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

/**
 * @author rcardone
 *
 */
public final class PhaseWorker 
 extends Thread
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(PhaseWorker.class);
    
    // Communication constants.
    public static final String WORKER_EXCHANGE_NAME = "JobWorkerExchange";

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
        // Unpack the parameters.
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
        
        // This thread is starting.
        if (_log.isDebugEnabled())
            _log.debug("-> Starting worker thread " + getName() + "");
        
        // Create the channel and bind our queue to it.
        _channel = initChannel();
        
        // Exit without a fuss if something's wrong.
        if (_channel == null) return;
        
        // Use a queuing consumer so that we control the threading.  This
        // consumer implements a blocking read call, which allows us to
        // process requests on this thread.
        QueueingConsumer consumer = new QueueingConsumer(_channel);
        
        // We explicitly acknowledge message receipt after processing them.
        boolean autoack = false;
        try {_channel.basicConsume(_queueName, autoack, consumer);}
        catch (IOException e) {
            String msg = "Worker " + getName() + " is unable consume messages from queue " + 
                        _queueName + ".";
           _log.error(msg, e);
           try {_channel.abort(AMQP.CHANNEL_ERROR, msg);} catch (Exception e1){}
           return; // TODO: figure out better longterm strategy.
        }
        
        // Tracing.
        if (_log.isDebugEnabled())
            _log.debug("[*] Worker " + getName() + " consuming queue " + _queueName + ".");
        
        
        // Read loop.
        while (true) {
            // Wait for next request.
            QueueingConsumer.Delivery delivery = null;
            try {
                // TODO: figure out how to handle interruptions; for now we just exit.
                delivery = consumer.nextDelivery();
            } catch (ShutdownSignalException e) {
                _log.info("Shutdown signal received by thread " + getName(), e);
                break;
            } catch (ConsumerCancelledException e) {
                _log.info("Cancelled signal received by thread " + getName(), e);
                break;
            } catch (InterruptedException e) {
                _log.info("Interrupted signal received by thread " + getName(), e);
                break;
            }
            
            // Decompose the received message.
            Envelope envelope = delivery.getEnvelope();
            AMQP.BasicProperties properties = delivery.getProperties();
            byte[] body = delivery.getBody();

            // Tracing.
            if (_log.isDebugEnabled()) dumpMessageInfo(envelope, properties, body);
            
            // Process messages read from queue.
            String message = null;
            try {message = new String(body);}
            catch (Exception e)
            {
                // All message bodies should contain string data.
                String msg = "Worker " + getName() + 
                             " cannot decode data from queue " + 
                             _queueName + ": " + e.getMessage();
                _log.error(msg, e);
              
                // Reject this unreadable message so that
                // it gets discarded or dead-lettered.
                boolean requeue = false;
                try {_channel.basicReject(envelope.getDeliveryTag(), requeue);} 
                catch (IOException e1) {
                    // We're in trouble if we cannot reject a message.
                    String msg1 = "Worker " + getName() + 
                            " cannot reject a message received on queue " + 
                            _queueName + ": " + e1.getMessage();
                    _log.error(msg1, e);
                    break;
                }
            }
        
            // For now, just print what we receive.
            // TODO: create command processor 
            if (message != null)
                System.out.println("Worker " + getName() +  
                                   " received message:\n > " +
                                   message + "\n");
        
            // Don't forget to send the ack!
            boolean multipleAck = false;
            try {_channel.basicAck(envelope.getDeliveryTag(), multipleAck);}
            catch (IOException e) {
                // We're in trouble if we cannot acknowledge a message.
                String msg = "Worker " + getName() + 
                        " cannot acknowledge a message received on queue " + 
                        _queueName + ": " + e.getMessage();
                _log.error(msg, e);
                break;
            }
        }
        
        // This thread is terminating.
        if (_log.isDebugEnabled())
            _log.debug("<- Exiting worker thread " + getName() + ".");
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
    private Channel initChannel() {
        
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
         
         // Success.
         return channel;
    }
    
    /* ---------------------------------------------------------------------- */
    /* dumpMessageInfo:                                                       */
    /* ---------------------------------------------------------------------- */
    /** Write debug message and threading information.  This methods should
     * only be called after checking that debugging is enabled.
     * 
     * @param envelope the message envelope
     * @param properties the message properties
     * @param body the message
     */
    private void dumpMessageInfo(Envelope envelope, AMQP.BasicProperties properties,
                                 byte[] body)
    {
        // We assume all input parameters are non-null.
        Thread curthd = Thread.currentThread();
        ThreadGroup curgrp = curthd.getThreadGroup();
        String msg = "\n------------------- Worker Bytes Received: " + body.length + "\n";
        msg += "Thread(name=" +curthd.getName() + ", isDaemon=" + curthd.isDaemon() + ")\n";
        msg += "ThreadGroup(name=" + curgrp.getName() + ", parentGroup=" + curgrp.getParent().getName() +
                  ", activeGroupCount=" + curgrp.activeGroupCount() + ", activeThreadCount=" + 
                  curgrp.activeCount() + ", isDaemon=" + curgrp.isDaemon() + ")\n";
      
        // Output is truncated at array size.
        Thread[] thdArray = new Thread[200];
        int thdArrayLen = curgrp.enumerate(thdArray, false); // non-recursive 
        msg += "ThreadArray(length=" + thdArrayLen + ", names=";
        for (int i = 0; i < thdArrayLen; i++) msg += thdArray[i].getName() + ", ";
        msg += "\n";
      
        // Output is truncated at array size.
        ThreadGroup[] grpArray = new ThreadGroup[200];
        int grpArrayLen = curgrp.enumerate(grpArray, false); // non-recursive 
        msg += "ThreadGroupArray(length=" + grpArrayLen + ", names=";
        for (int i = 0; i < grpArrayLen; i++) msg += grpArray[i].getName() + ", ";
        msg += "\n";
      
        msg += envelope.toString() + "\n";
        StringBuilder buf = new StringBuilder(512);
        properties.appendPropertyDebugStringTo(buf);
        msg += "Properties" + buf.toString() + "\n";
        msg += "-------------------------------------------------\n";
        _log.debug(msg);
        
    }
}
