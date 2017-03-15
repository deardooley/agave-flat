package org.iplantc.service.jobs.managers;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.exceptions.JobSchedulerException;
import org.iplantc.service.jobs.model.JobSchedulerEvent;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.iplantc.service.notification.events.enumerations.JobSchedulerEventType;
import org.iplantc.service.notification.managers.NotificationManager;

/** Event processor that puts Job Scheduler events on a notifications queue
 * to be forwarded to subscribers.  The status utility functions provide a
 * convenient way to populate events with their parameters.  The actual parameters
 * used by specific events are defined in JobSchedulerNotificationEvent.
 * 
 * @author rcardone
 */
public class JobSchedulerEventProcessor
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(JobSchedulerEventProcessor.class);
    
    // Event owner used on all calls.
    private static final String _eventOwner = "System";

    /* ********************************************************************** */
    /*                                 Fields                                 */
    /* ********************************************************************** */
    // The event this object will send.
    private final JobSchedulerEvent _event;
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    public JobSchedulerEventProcessor(JobSchedulerEvent event){_event = event;}
    
    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* process:                                                               */
    /* ---------------------------------------------------------------------- */
    public void process()
     throws JobSchedulerException
    {
        // Check that we were initialized correctly
        if (_event == null)
        {
            String msg = "Cannot process a null job scheduler event.";
            _log.error(msg);
            throw new JobSchedulerException(msg);
        }

        // Send the event.
        NotificationManager.process(_event.getAssociatedUuid().toString(), 
                                    _event.getEvent().name(), 
                                    _event.getUser(), 
                                    _event.getPropertiesAsJson());
    }
    
    /* ---------------------------------------------------------------------- */
    /* sendEvent:                                                             */
    /* ---------------------------------------------------------------------- */
    /** Create and send an event for notification processing. 
     * 
     * @param agaveUuid the agave uuid of the scheduler sending the event
     * @param eventType the job scheduler event type
     * @param properties the properties to be converted to json for inclusion
     *                   in the generated notification
     * */
    public static void sendEvent(AgaveUUID agaveUuid, JobSchedulerEventType eventType, 
                                 Properties properties)
    {
        // Create the event.  Event creation is broken out separately in
        // anticipation of persisting events in the future. 
        JobSchedulerEvent event = 
            new JobSchedulerEvent(agaveUuid, eventType, _eventOwner, properties);
            
        // Process the event.
        JobSchedulerEventProcessor eventProcessor = new JobSchedulerEventProcessor(event);
        try {eventProcessor.process();}
        catch (Exception e) {
            String msg = "Unable to process event " + eventType.name() + 
                         " for scheduler " + agaveUuid.toString() + ".";
            _log.error(msg, e);
        }
    }
    
    /* ********************************************************************** */
    /*                       Event-Specific Methods                           */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* sendSchedulerStartedEvent:                                             */
    /* ---------------------------------------------------------------------- */
    public static void sendSchedulerStartedEvent(AgaveUUID agaveUuid)
    {
        // Assign the properties used in notification messages.  See
        // JobSchedulerNotificationEvent for the key/value pairs used
        // in various event notifications.
        Properties properties = new Properties();
        properties.setProperty("TENANT_ID", "*");
        properties.setProperty("PHASE", "*");
        properties.setProperty("JOB_SCHEDULER_MODE", Boolean.valueOf(Settings.JOB_SCHEDULER_MODE).toString());
        properties.setProperty("JOB_WORKER_MODE", Boolean.valueOf(Settings.JOB_WORKER_MODE).toString());
        properties.setProperty("JOB_ADMIN_MODE", Boolean.valueOf(Settings.JOB_ADMIN_MODE).toString());
        properties.setProperty("JOB_ENABLE_ZOMBIE_CLEANUP", Boolean.valueOf(Settings.JOB_ENABLE_ZOMBIE_CLEANUP).toString());
        properties.setProperty("JOB_CLAIM_POLL_ITERATIONS", Integer.valueOf(Settings.JOB_CLAIM_POLL_ITERATIONS).toString());
        properties.setProperty("JOB_CLAIM_POLL_SLEEP_MS", Long.valueOf(Settings.JOB_CLAIM_POLL_SLEEP_MS).toString());
        properties.setProperty("JOB_UUID_QUERY_LIMIT", Integer.valueOf(Settings.JOB_UUID_QUERY_LIMIT).toString());
        properties.setProperty("JOB_SCHEDULER_LEASE_SECONDS", Integer.valueOf(Settings.JOB_SCHEDULER_LEASE_SECONDS).toString());
        properties.setProperty("JOB_SCHEDULER_NORMAL_POLL_MS", Long.valueOf(Settings.JOB_SCHEDULER_NORMAL_POLL_MS).toString());
        properties.setProperty("JOB_INTERRUPT_TTL_SECONDS", Integer.valueOf(Settings.JOB_INTERRUPT_TTL_SECONDS).toString());
        properties.setProperty("JOB_INTERRUPT_DELETE_MS", Long.valueOf(Settings.JOB_INTERRUPT_DELETE_MS).toString());
        properties.setProperty("JOB_ZOMBIE_MONITOR_MS", Long.valueOf(Settings.JOB_ZOMBIE_MONITOR_MS).toString());
        properties.setProperty("JOB_THREAD_DEATH_MS", Long.valueOf(Settings.JOB_THREAD_DEATH_MS).toString());
        properties.setProperty("JOB_THREAD_DEATH_POLL_MS", Long.valueOf(Settings.JOB_THREAD_DEATH_POLL_MS).toString());
        properties.setProperty("JOB_CONNECTION_CLOSE_MS", Long.valueOf(Settings.JOB_CONNECTION_CLOSE_MS).toString());
        properties.setProperty("JOB_WORKER_INIT_RETRY_MS", Long.valueOf(Settings.JOB_WORKER_INIT_RETRY_MS).toString());
        properties.setProperty("JOB_MAX_SUBMISSION_RETRIES", Integer.valueOf(Settings.JOB_MAX_SUBMISSION_RETRIES).toString());
        properties.setProperty("JOB_QUEUE_CONFIG_FOLDER", Settings.JOB_QUEUE_CONFIG_FOLDER);
        properties.setProperty("JOB_MAX_THREADS_PER_QUEUE", Integer.valueOf(Settings.JOB_MAX_THREADS_PER_QUEUE).toString());
        properties.setProperty("JOB_THREAD_RESTART_SECONDS", Integer.valueOf(Settings.JOB_THREAD_RESTART_SECONDS).toString());
        properties.setProperty("JOB_THREAD_RESTART_LIMIT", Integer.valueOf(Settings.JOB_THREAD_RESTART_LIMIT).toString());
        properties.setProperty("DEDICATED_TENANT_ID", Settings.getDedicatedTenantIdFromServiceProperties());
        properties.setProperty("DRAIN_ALL_QUEUES_ENABLED", Boolean.valueOf(Settings.isDrainingQueuesEnabled()).toString());

        // Send the event for notification processing.
        sendEvent(agaveUuid, JobSchedulerEventType.SCHEDULER_STARTED, properties);
    }
    
    /* ---------------------------------------------------------------------- */
    /* sendSchedulerException:                                                */
    /* ---------------------------------------------------------------------- */
    public static void sendSchedulerException(AgaveUUID agaveUuid,
                                              JobPhaseType phase,
                                              String schedulerName,
                                              Exception e, 
                                              String action)
    {
        // Assign the properties used in notification messages.  See
        // JobSchedulerNotificationEvent for the key/value pairs used
        // in various event notifications.
        Properties properties = new Properties();
        properties.setProperty("TENANT_ID", "*");
        properties.setProperty("PHASE", phase.name());
        properties.setProperty("SCHEDULER_NAME", schedulerName);
        properties.setProperty("EXCEPTION_TYPE", e.getClass().getName());
        properties.setProperty("EXCEPTION_MESSAGE", e.getMessage());
        properties.setProperty("ACTION", action);

        // Send the event for notification processing.
        sendEvent(agaveUuid, JobSchedulerEventType.SCHEDULER_EXCEPTION, properties);
    }

    /* ---------------------------------------------------------------------- */
    /* sendThreadStarted:                                                     */
    /* ---------------------------------------------------------------------- */
    public static void sendThreadStarted(JobSchedulerEventType eventType,
                                         AgaveUUID agaveUuid,
                                         JobPhaseType phase,
                                         String threadName)
    {
        // Assign the properties used in notification messages.  See
        // JobSchedulerNotificationEvent for the key/value pairs used
        // in various event notifications.
        Properties properties = new Properties();
        properties.setProperty("TENANT_ID", "*");
        properties.setProperty("PHASE", phase.name());
        properties.setProperty("THREAD_NAME", threadName);

        // Send the event for notification processing.
        sendEvent(agaveUuid, eventType, properties);
    }

    /* ---------------------------------------------------------------------- */
    /* sendWorkerThreadEvent:                                                 */
    /* ---------------------------------------------------------------------- */
    public static void sendWorkerThreadEvent(JobSchedulerEventType eventType,
                                             AgaveUUID agaveUuid,
                                             String tenantId,
                                             JobPhaseType phase,
                                             String threadName,
                                             String threadUuid,
                                             int    threadNum,
                                             String queueName)
    {
        // Assign the properties used in notification messages.  See
        // JobSchedulerNotificationEvent for the key/value pairs used
        // in various event notifications.
        Properties properties = new Properties();
        properties.setProperty("TENANT_ID", tenantId);
        properties.setProperty("PHASE", phase.name());
        properties.setProperty("THREAD_NAME", threadName);
        properties.setProperty("THREAD_UUID", threadUuid);
        properties.setProperty("THREAD_NUMBER", Integer.valueOf(threadNum).toString());
        properties.setProperty("QUEUE_NAME", queueName);

        // Send the event for notification processing.
        sendEvent(agaveUuid, eventType, properties);
    }

    /* ---------------------------------------------------------------------- */
    /* sendThreadException:                                                   */
    /* ---------------------------------------------------------------------- */
    public static void sendThreadException(JobSchedulerEventType eventType,
                                           AgaveUUID agaveUuid,
                                           String tenantId,
                                           JobPhaseType phase,
                                           String threadName,
                                           Exception e, 
                                           String action)
    {
        // Assign the properties used in notification messages.  See
        // JobSchedulerNotificationEvent for the key/value pairs used
        // in various event notifications.
        Properties properties = new Properties();
        properties.setProperty("TENANT_ID", tenantId);
        properties.setProperty("PHASE", phase.name());
        properties.setProperty("THREAD_NAME", threadName);
        properties.setProperty("EXCEPTION_TYPE", e.getClass().getName());
        properties.setProperty("EXCEPTION_MESSAGE", e.getMessage());
        properties.setProperty("ACTION", action);

        // Send the event for notification processing.
        sendEvent(agaveUuid, eventType, properties);
    }

    /* ---------------------------------------------------------------------- */
    /* sendJobEvent:                                                          */
    /* ---------------------------------------------------------------------- */
    public static void sendJobEvent(JobSchedulerEventType eventType,
                                    AgaveUUID agaveUuid,
                                    String tenantId,
                                    JobPhaseType phase,
                                    String jobName,
                                    String jobUuid, 
                                    int    epoch)
    {
        // Assign the properties used in notification messages.  See
        // JobSchedulerNotificationEvent for the key/value pairs used
        // in various event notifications.
        Properties properties = new Properties();
        properties.setProperty("TENANT_ID", tenantId);
        properties.setProperty("PHASE", phase.name());
        properties.setProperty("JOB_NAME", jobName);
        properties.setProperty("JOB_UUID", jobUuid);
        properties.setProperty("JOB_EPOCH", Integer.valueOf(epoch).toString());

        // Send the event for notification processing.
        sendEvent(agaveUuid, eventType, properties);
    }

    /* ---------------------------------------------------------------------- */
    /* sendSchedulerName:                                                     */
    /* ---------------------------------------------------------------------- */
    public static void sendSchedulerName(JobSchedulerEventType eventType,
                                         AgaveUUID agaveUuid,
                                         JobPhaseType phase,
                                         String schedulerName)
    {
        // Assign the properties used in notification messages.  See
        // JobSchedulerNotificationEvent for the key/value pairs used
        // in various event notifications.
        Properties properties = new Properties();
        properties.setProperty("TENANT_ID", "*");
        properties.setProperty("PHASE", phase.name());
        properties.setProperty("SCHEDULER_NAME", schedulerName);

        // Send the event for notification processing.
        sendEvent(agaveUuid, eventType, properties);
    }

    /* ---------------------------------------------------------------------- */
    /* sendQueueRefresh:                                                      */
    /* ---------------------------------------------------------------------- */
    public static void sendQueueRefresh(AgaveUUID agaveUuid,
                                        String tenantId,
                                        JobPhaseType phase,
                                        String queueName,
                                        int    delta)
    {
        // Assign the properties used in notification messages.  See
        // JobSchedulerNotificationEvent for the key/value pairs used
        // in various event notifications.
        Properties properties = new Properties();
        properties.setProperty("TENANT_ID", tenantId);
        properties.setProperty("PHASE", phase.name());
        properties.setProperty("QUEUE_NAME", queueName);
        properties.setProperty("NUM_WORKERS_DELTA", Integer.valueOf(delta).toString());

        // Send the event for notification processing.
        sendEvent(agaveUuid, JobSchedulerEventType.TPC_REFRESH_QUEUE_INFO, properties);
    }

    /* ---------------------------------------------------------------------- */
    /* sendNoopMessage:                                                       */
    /* ---------------------------------------------------------------------- */
    public static void sendNoopMessage(AgaveUUID agaveUuid,
                                       JobPhaseType phase,
                                       String message)
    {
        // Assign the properties used in notification messages.  See
        // JobSchedulerNotificationEvent for the key/value pairs used
        // in various event notifications.
        Properties properties = new Properties();
        properties.setProperty("TENANT_ID", "*");
        properties.setProperty("PHASE", phase.name());
        properties.setProperty("NOOP_MESSAGE", message);

        // Send the event for notification processing.
        sendEvent(agaveUuid, JobSchedulerEventType.TPC_NOOP_RECEIVED, properties);
    }

}
