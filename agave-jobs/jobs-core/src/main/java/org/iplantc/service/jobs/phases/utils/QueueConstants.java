package org.iplantc.service.jobs.phases.utils;

import org.iplantc.service.jobs.model.enumerations.JobPhaseType;

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
    
    // All phase routing and binding keys.
    public static final String TOPIC_ALL_ROUTING_KEY = TOPIC_QUEUE_NAME + ".All";
    public static final String TOPIC_ALL_BINDING_KEY = TOPIC_QUEUE_NAME + ".All.#";
    
    // Phase-specific routing keys.
    public static final String TOPIC_STAGING_ROUTING_KEY = TOPIC_QUEUE_NAME + "." + JobPhaseType.STAGING.name();
    public static final String TOPIC_SUBMITTING_ROUTING_KEY = TOPIC_QUEUE_NAME + "." + JobPhaseType.SUBMITTING.name();
    public static final String TOPIC_MONITORING_ROUTING_KEY = TOPIC_QUEUE_NAME + "." + JobPhaseType.MONITORING.name();
    public static final String TOPIC_ARCHIVING_ROUTING_KEY = TOPIC_QUEUE_NAME + "." + JobPhaseType.ARCHIVING.name();
    
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
