package org.iplantc.service.jobs.phases;

import com.rabbitmq.client.AMQP.BasicProperties;

/** This class provides a centralized location for naming queuing artifacts 
 * such as queues, topics, exchanges, channels, etc.  This motivation is to 
 * standardize names so that they are easily identified when using RabbitMQ 
 * management tools. 
 * 
 * @author rcardone
 */
public class QueueConstants
{
    // ----- Job Worker Names.
    public static final String WORKER_EXCHANGE_NAME = "Agave.Job.Worker.Exchange";
    
    // ----- Job Scheduler Topic Names.
    public static final String TOPIC_EXCHANGE_NAME = "Agave.Job.Topic.Exchange";
    public static final String TOPIC_QUEUE_NAME = "Agave.Job.Topic";
    public static final String TOPIC_ALL_ROUTING_KEY = TOPIC_QUEUE_NAME + ".All";
    public static final String TOPIC_ALL_BINDING_KEY = TOPIC_QUEUE_NAME + ".All.#";
    
    // ----- RabbitMQ pre-configured properties objects. 
    public static final BasicProperties PERSISTENT_JSON =
            new BasicProperties("application/json",
                                null,
                                null,
                                2,
                                0, null, null, null,
                                null, null, null, null,
                                null, null);

}
