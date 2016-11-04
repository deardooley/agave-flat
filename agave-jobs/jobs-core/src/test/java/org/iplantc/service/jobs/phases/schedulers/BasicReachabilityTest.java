package org.iplantc.service.jobs.phases.schedulers;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/** Very basic test program to see if we can read and write topics and queues.
 * This program depends on certain queues existing in the server and the 
 * server running.
 * 
 * This test needs to be formalized as part of a test suite that checks that
 * the positive and negative behaviors are exhibited as expected.  Currently, 
 * one checks that the tests passed by looking at the server logs to
 * see what requests were processed.
 * 
 * @author rcardone
 *
 */
public class BasicReachabilityTest 
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */   
    // The topic exchange defined by the job scheduler.
    public static final String TOPIC_EXCHANGE_NAME = "JobTopicExchange";
    
    // The job queue worker exchange defined by the job scheduler.
    private static final String WORKER_EXCHANGE_NAME = "JobWorkerExchange";
    
    /* ********************************************************************** */
    /*                                 Fields                                 */
    /* ********************************************************************** */   
    // Connection to RabbitMQ shared by all tests.
    private  Connection _connection;
    
    /* ********************************************************************** */
    /*                            Set Up / Tear Down                          */
    /* ********************************************************************** */    
    /* ---------------------------------------------------------------------- */
    /* setup:                                                                 */
    /* ---------------------------------------------------------------------- */
    @BeforeSuite
    private void setup() throws Exception
    {
        try {
          ConnectionFactory factory = new ConnectionFactory();
          factory.setHost("localhost");
          _connection = factory.newConnection();
        } catch (Exception e) {
            throw e;
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* teardown:                                                              */
    /* ---------------------------------------------------------------------- */
    @AfterSuite
    private void teardown() throws IOException
    {
        _connection.close();
    }
    
    /* ********************************************************************** */
    /*                              Test Methods                              */
    /* ********************************************************************** */    
    /* ---------------------------------------------------------------------- */
    /* writeTopic:                                                            */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void writeTopic() throws IOException, TimeoutException
    {   
      // Get an exchange to write.
      String exchangeName = TOPIC_EXCHANGE_NAME;
      Channel channel = _connection.createChannel();
      boolean durable = true;
      channel.exchangeDeclare(exchangeName, "topic", durable);

      // ---------- Positive Tests
      // Routing key can be phase part can be "All" or a phase name. 
      String routingKey = "JobScheduler.All";
      String message = "writeTopic message sent with routing key: " + routingKey;
      channel.basicPublish(exchangeName, routingKey, null, message.getBytes("UTF-8"));
      
      // Put some more text on the main topic queue.
      routingKey = "JobScheduler.All.anything";
      message = "writeTopic message sent with routing key: " + routingKey;
      channel.basicPublish(exchangeName, routingKey, null, message.getBytes("UTF-8"));
      
      // Put some more text on the main topic queue.
      routingKey = "JobScheduler.STAGING";
      message = "writeTopic message sent with routing key: " + routingKey;
      channel.basicPublish(exchangeName, routingKey, null, message.getBytes("UTF-8"));
        
      // Put some more text on the main topic queue.
      routingKey = "JobScheduler.STAGING.junk";
      message = "writeTopic message sent with routing key: " + routingKey;
      channel.basicPublish(exchangeName, routingKey, null, message.getBytes("UTF-8"));
        
      // ---------- Negative Tests
      // Put some more text on the main topic queue.
      routingKey = "JobScheduler.NOT_DELIVERABLE";
      message = "This message should never be delivered! - routing key: " + routingKey;
      channel.basicPublish(exchangeName, routingKey, null, message.getBytes("UTF-8"));
      
      // Put some more text on the main topic queue.
      routingKey = "JobSchedule";
      message = "This message should never be delivered! - routing key: " + routingKey;
      channel.basicPublish(exchangeName, routingKey, null, message.getBytes("UTF-8"));
      
      // Put some more text on the main topic queue.
      routingKey = "JobScheduler"; 
      message = "This message should never be delivered! - routing key: " + routingKey;
      channel.basicPublish(exchangeName, routingKey, null, message.getBytes("UTF-8"));
      
      // Put some more text on the main topic queue.
      routingKey = "X";
      message = "This message should never be delivered! - routing key: " + routingKey;
      channel.basicPublish(exchangeName, routingKey, null, message.getBytes("UTF-8"));
      
      // Clean up.
      channel.close();
    }
    
    /* ---------------------------------------------------------------------- */
    /* writeQueue:                                                            */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void writeQueue() throws IOException
    {   
        // Note that worker queues use direct routing to their queue names.
        // That is, each worker thread manages a queue bound with the queue's
        // name to the well-known worker exchange.  This test depends on 
        // certain queue name existing in the server.
        
        // Get an exchange to write.
        String exchangeName = WORKER_EXCHANGE_NAME;
        Channel channel = _connection.createChannel();
        boolean durable = true;
        channel.exchangeDeclare(exchangeName, "direct", durable);
        
        // ---------- Positive Tests
        // Put some text on the default STAGING queue for iplant (STAGING.iplantc.org).
        String routingKey = "STAGING.iplantc.org";
        String message = "writeQueue message 1 sent with routing key: " + routingKey;
        channel.basicPublish(exchangeName, routingKey, null, message.getBytes("UTF-8"));
        
        // Put some text on another worker queue.
        routingKey = "STAGING.iplantc.org.Bob";
        message = "writeQueue message 2 sent with routing key: " + routingKey;
        channel.basicPublish(exchangeName, routingKey, null, message.getBytes("UTF-8"));

        // Put some text on another worker queue.
        routingKey = "STAGING.iplantc.org.Harry";
        message = "writeQueue message 3 sent with routing key: " + routingKey;
        channel.basicPublish(exchangeName, routingKey, null, message.getBytes("UTF-8"));

        // ---------- Negative Tests
        // Put some text on a non-existent worker queue.
        routingKey = "KEY_NOT_DELIVERABLE";
        message = "This message should never be delivered! - routing key: " + routingKey;
        channel.basicPublish(exchangeName, routingKey, null, message.getBytes("UTF-8"));
    }
}
