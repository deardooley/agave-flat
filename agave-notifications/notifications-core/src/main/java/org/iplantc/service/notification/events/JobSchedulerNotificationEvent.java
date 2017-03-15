/**
 * 
 */
package org.iplantc.service.notification.events;

import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.notification.events.enumerations.JobSchedulerEventType;
import org.iplantc.service.notification.model.Notification;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author rcardone
 */
public class JobSchedulerNotificationEvent extends AbstractEventFilter {

	private static final Logger _log = Logger.getLogger(JobSchedulerNotificationEvent.class);
	
	// Reuse the same json mapper object.
	private static final ObjectMapper mapper = new ObjectMapper();
	
	// Assigned on first demand so that the caller can set the
	// custom data after construction if necessary.
	private Properties _properties;
	
	// Constructor
    public JobSchedulerNotificationEvent(AgaveUUID associatedUuid, Notification notification, String event, String owner)
    {
        // This guarantees that properties is assigned.
        super(associatedUuid, notification, event, owner);
    }	
    
	// Constructor
	public JobSchedulerNotificationEvent(AgaveUUID associatedUuid, Notification notification, String event, String owner,
	                                     String customData)
	{
	    // Always set this event's properties field to non-null.
		super(associatedUuid, notification, event, owner, customData);
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getEmailBody()
	 */
	@Override
	public String getEmailBody()
	{
	    // Make sure we know the type of event.
	    JobSchedulerEventType eventType = null;
	    try {eventType = JobSchedulerEventType.valueOf(event);}
	    catch (Exception e) {
	        String msg = "Unable to convert the value of \"" + event + "\" into a JobSchedulerEventType.";
	        _log.error(msg, e);
	        return "Scheduler " + associatedUuid + " generated unknown event \"" + event + "\".";
	    }
	    
	    // Assign the properties field.
	    assignProperties();
	    
	    // Initialize the output buffer.
		StringBuilder buf = new StringBuilder(512);
		buf.append("A job scheduler instance generated an event.\n\n");
		
		// All event key/values
        buf.append("Timestamp: ");
        buf.append(DateTime.now().toString());
        buf.append("\n");
        buf.append("Scheduler uuid: ");
        buf.append(associatedUuid.toString());
        buf.append("\n");
        buf.append("Phase: ");
        buf.append(_properties.getProperty("PHASE"));
        buf.append("\n");
        buf.append("Event: ");
        buf.append(_properties.getProperty(eventType.name()));
        buf.append("\n");
        buf.append("Tenant: ");
        buf.append(_properties.getProperty("TENANT_ID"));
        buf.append("\n");
        
        // Event-specific key/values.
		switch (eventType) 
		{
		    case SCHEDULER_STARTED: 
                buf.append("JOB_SCHEDULER_MODE: ");
                buf.append(_properties.getProperty("JOB_SCHEDULER_MODE"));
                buf.append("\n");
                buf.append("JOB_WORKER_MODE: ");
                buf.append(_properties.getProperty("JOB_WORKER_MODE"));
                buf.append("\n");
                buf.append("JOB_ADMIN_MODE: ");
                buf.append(_properties.getProperty("JOB_ADMIN_MODE"));
                buf.append("\n");
                buf.append("JOB_ENABLE_ZOMBIE_CLEANUP: ");
                buf.append(_properties.getProperty("JOB_ENABLE_ZOMBIE_CLEANUP"));
                buf.append("\n");
                buf.append("JOB_CLAIM_POLL_ITERATIONS: ");
                buf.append(_properties.getProperty("JOB_CLAIM_POLL_ITERATIONS"));
                buf.append("\n");
                buf.append("JOB_CLAIM_POLL_SLEEP_MS: ");
                buf.append(_properties.getProperty("JOB_CLAIM_POLL_SLEEP_MS"));
                buf.append("\n");
                buf.append("JOB_UUID_QUERY_LIMIT: ");
                buf.append(_properties.getProperty("JOB_UUID_QUERY_LIMIT"));
                buf.append("\n");
                buf.append("JOB_SCHEDULER_LEASE_SECONDS: ");
                buf.append(_properties.getProperty("JOB_SCHEDULER_LEASE_SECONDS"));
                buf.append("\n");
                buf.append("JOB_SCHEDULER_NORMAL_POLL_MS: ");
                buf.append(_properties.getProperty("JOB_SCHEDULER_NORMAL_POLL_MS"));
                buf.append("\n");
                buf.append("JOB_INTERRUPT_TTL_SECONDS: ");
                buf.append(_properties.getProperty("JOB_INTERRUPT_TTL_SECONDS"));
                buf.append("\n");
                buf.append("JOB_INTERRUPT_DELETE_MS: ");
                buf.append(_properties.getProperty("JOB_INTERRUPT_DELETE_MS"));
                buf.append("\n");
                buf.append("JOB_ZOMBIE_MONITOR_MS: ");
                buf.append(_properties.getProperty("JOB_ZOMBIE_MONITOR_MS"));
                buf.append("\n");
                buf.append("JOB_THREAD_DEATH_MS: ");
                buf.append(_properties.getProperty("JOB_THREAD_DEATH_MS"));
                buf.append("\n");
                buf.append("JOB_THREAD_DEATH_POLL_MS: ");
                buf.append(_properties.getProperty("JOB_THREAD_DEATH_POLL_MS"));
                buf.append("\n");
                buf.append("JOB_CONNECTION_CLOSE_MS: ");
                buf.append(_properties.getProperty("JOB_CONNECTION_CLOSE_MS"));
                buf.append("\n");
                buf.append("JOB_WORKER_INIT_RETRY_MS: ");
                buf.append(_properties.getProperty("JOB_WORKER_INIT_RETRY_MS"));
                buf.append("\n");
                buf.append("JOB_MAX_SUBMISSION_RETRIES: ");
                buf.append(_properties.getProperty("JOB_MAX_SUBMISSION_RETRIES"));
                buf.append("\n");
                buf.append("JOB_QUEUE_CONFIG_FOLDER: ");
                buf.append(_properties.getProperty("JOB_QUEUE_CONFIG_FOLDER"));
                buf.append("\n");
                buf.append("JOB_MAX_THREADS_PER_QUEUE: ");
                buf.append(_properties.getProperty("JOB_MAX_THREADS_PER_QUEUE"));
                buf.append("\n");
                buf.append("JOB_THREAD_RESTART_SECONDS: ");
                buf.append(_properties.getProperty("JOB_THREAD_RESTART_SECONDS"));
                buf.append("\n");
                buf.append("JOB_THREAD_RESTART_LIMIT: ");
                buf.append(_properties.getProperty("JOB_THREAD_RESTART_LIMIT"));
                buf.append("\n");
                buf.append("DEDICATED_TENANT_ID: ");
                buf.append(_properties.getProperty("DEDICATED_TENANT_ID"));
                buf.append("\n");
                buf.append("DRAIN_ALL_QUEUES_ENABLED: ");
                buf.append(_properties.getProperty("DRAIN_ALL_QUEUES_ENABLED"));
                buf.append("\n");
		        break;
		    case SCHEDULER_EXCEPTION:
                buf.append("Scheduler name: ");
                buf.append(_properties.getProperty("SCHEDULER_NAME"));
                buf.append("\n");
                buf.append("Exception type: ");
                buf.append(_properties.getProperty("EXCEPTION_TYPE"));
                buf.append("\n");
                buf.append("Exception message: ");
                buf.append(_properties.getProperty("EXCEPTION_MESSAGE"));
                buf.append("\n");
                buf.append("Action: ");
                buf.append(_properties.getProperty("ACTION"));
                buf.append("\n");
		        break;
		    case TOPIC_THREAD_STARTED:
		    case INTERRUPT_THREAD_STARTED:
		    case ZOMBIE_THREAD_STARTED:
                buf.append("Thread name: ");
                buf.append(_properties.getProperty("THREAD_NAME"));
                buf.append("\n");
		        break;
		    case WORKER_THREAD_STARTED:
		    case WORKER_THREAD_SCHEDULED_TO_STOP:
                buf.append("Thread name: ");
                buf.append(_properties.getProperty("THREAD_NAME"));
                buf.append("\n");
                buf.append("Thread uuid: ");
                buf.append(_properties.getProperty("THREAD_UUID"));
                buf.append("\n");
                buf.append("Thread number: ");
                buf.append(_properties.getProperty("THREAD_NUMBER"));
                buf.append("\n");
                buf.append("Queue name: ");
                buf.append(_properties.getProperty("QUEUE_NAME"));
                buf.append("\n");
		        break;
            case TOPIC_THREAD_EXECPTION:
            case INTERRUPT_THREAD_EXECPTION:
            case ZOMBIE_THREAD_EXECPTION:
		    case WORKER_THREAD_EXCEPTION:
                buf.append("Thread name: ");
                buf.append(_properties.getProperty("THREAD_NAME"));
                buf.append("\n");
                buf.append("Exception type: ");
                buf.append(_properties.getProperty("EXCEPTION_TYPE"));
                buf.append("\n");
                buf.append("Exception message: ");
                buf.append(_properties.getProperty("EXCEPTION_MESSAGE"));
                buf.append("\n");
                buf.append("Action: ");
                buf.append(_properties.getProperty("ACTION"));
                buf.append("\n");
		        break;
		    case TPC_DELETE_JOB_RECEIVED:
		    case TPC_PAUSE_JOB_RECEIVED:
		    case TPC_STOP_JOB_RECEIVED:
                buf.append("Job name: ");
                buf.append(_properties.getProperty("JOB_NAME"));
                buf.append("\n");
                buf.append("Job uuid: ");
                buf.append(_properties.getProperty("JOB_UUID"));
                buf.append("\n");
                buf.append("Job epoch: ");
                buf.append(_properties.getProperty("JOB_EPOCH"));
                buf.append("\n");
		        break;
		    case SCHEDULER_STOPPING:
		    case TPC_SHUTDOWN_RECEIVED:
		    case TPC_REFRESH_QUEUE_INFO:
                buf.append("Scheduler name: ");
                buf.append(_properties.getProperty("SCHEDULER_NAME"));
                buf.append("\n");
		        break;
		    case TPC_RESET_NUM_WORKERS_RECEIVED:
                buf.append("Queue name: ");
                buf.append(_properties.getProperty("QUEUE_NAME"));
                buf.append("\n");
                buf.append("Number of workers delta: ");
                buf.append(_properties.getProperty("NUM_WORKERS_DELTA"));
                buf.append("\n");
		        break;
		    case TPC_NOOP_RECEIVED:
                buf.append("Message: ");
                buf.append(_properties.getProperty("NOOP_MESSAGE"));
                buf.append("\n");
		        break;
		    default:
		        break;
		}
		
		return buf.toString();
	}
	
	/* (non-Javadoc)
     * @see org.iplantc.service.notification.events.EventFilter#getHtmlEmailBody()
     */
    @Override
    public String getHtmlEmailBody()
    {
        // Make sure we know the type of event.
        JobSchedulerEventType eventType = null;
        try {eventType = JobSchedulerEventType.valueOf(event);}
        catch (Exception e) {
            String msg = "Unable to convert the value of \"" + event + "\" into a JobSchedulerEventType.";
            _log.error(msg, e);
            return "<p>Scheduler " + associatedUuid + " generated unknown event \"" + event + "\".</p>";
        }
        
        // Assign the properties field.
        assignProperties();
        
        // Initialize the output buffer.
        StringBuilder buf = new StringBuilder(512);
        buf.append("<p>A job scheduler instance generated an event.</p><br>");
        
        // All event key/values
        buf.append("<p>");
        buf.append("<strong>Timestamp: </strong>");
        buf.append(DateTime.now().toString());
        buf.append("<br>");
        buf.append("<strong>Scheduler uuid: </strong>");
        buf.append(associatedUuid.toString());
        buf.append("<br>");
        buf.append("<strong>Phase: </strong>");
        buf.append(_properties.getProperty("PHASE"));
        buf.append("<br>");
        buf.append("<strong>Event: </strong>");
        buf.append(_properties.getProperty(eventType.name()));
        buf.append("<br>");
        buf.append("<strong>Tenant: </strong>");
        buf.append(_properties.getProperty("TENANT_ID"));
        buf.append("<br>");
        
        // Event-specific key/values.
        switch (eventType) 
        {
            case SCHEDULER_STARTED: 
                buf.append("<strong>JOB_SCHEDULER_MODE: </strong>");
                buf.append(_properties.getProperty("JOB_SCHEDULER_MODE"));
                buf.append("<br>");
                buf.append("<strong>JOB_WORKER_MODE: </strong>");
                buf.append(_properties.getProperty("JOB_WORKER_MODE"));
                buf.append("<br>");
                buf.append("<strong>JOB_ADMIN_MODE: </strong>");
                buf.append(_properties.getProperty("JOB_ADMIN_MODE"));
                buf.append("<br>");
                buf.append("<strong>JOB_ENABLE_ZOMBIE_CLEANUP: </strong>");
                buf.append(_properties.getProperty("JOB_ENABLE_ZOMBIE_CLEANUP"));
                buf.append("<br>");
                buf.append("<strong>JOB_CLAIM_POLL_ITERATIONS: </strong>");
                buf.append(_properties.getProperty("JOB_CLAIM_POLL_ITERATIONS"));
                buf.append("<br>");
                buf.append("<strong>JOB_CLAIM_POLL_SLEEP_MS: </strong>");
                buf.append(_properties.getProperty("JOB_CLAIM_POLL_SLEEP_MS"));
                buf.append("<br>");
                buf.append("<strong>JOB_UUID_QUERY_LIMIT: </strong>");
                buf.append(_properties.getProperty("JOB_UUID_QUERY_LIMIT"));
                buf.append("<br>");
                buf.append("<strong>JOB_SCHEDULER_LEASE_SECONDS: </strong>");
                buf.append(_properties.getProperty("JOB_SCHEDULER_LEASE_SECONDS"));
                buf.append("<br>");
                buf.append("<strong>JOB_SCHEDULER_NORMAL_POLL_MS: </strong>");
                buf.append(_properties.getProperty("JOB_SCHEDULER_NORMAL_POLL_MS"));
                buf.append("<br>");
                buf.append("<strong>JOB_INTERRUPT_TTL_SECONDS: </strong>");
                buf.append(_properties.getProperty("JOB_INTERRUPT_TTL_SECONDS"));
                buf.append("<br>");
                buf.append("<strong>JOB_INTERRUPT_DELETE_MS: </strong>");
                buf.append(_properties.getProperty("JOB_INTERRUPT_DELETE_MS"));
                buf.append("<br>");
                buf.append("<strong>JOB_ZOMBIE_MONITOR_MS: </strong>");
                buf.append(_properties.getProperty("JOB_ZOMBIE_MONITOR_MS"));
                buf.append("<br>");
                buf.append("<strong>JOB_THREAD_DEATH_MS: </strong>");
                buf.append(_properties.getProperty("JOB_THREAD_DEATH_MS"));
                buf.append("<br>");
                buf.append("<strong>JOB_THREAD_DEATH_POLL_MS: </strong>");
                buf.append(_properties.getProperty("JOB_THREAD_DEATH_POLL_MS"));
                buf.append("<br>");
                buf.append("<strong>JOB_CONNECTION_CLOSE_MS: </strong>");
                buf.append(_properties.getProperty("JOB_CONNECTION_CLOSE_MS"));
                buf.append("<br>");
                buf.append("<strong>JOB_WORKER_INIT_RETRY_MS: </strong>");
                buf.append(_properties.getProperty("JOB_WORKER_INIT_RETRY_MS"));
                buf.append("<br>");
                buf.append("<strong>JOB_MAX_SUBMISSION_RETRIES: </strong>");
                buf.append(_properties.getProperty("JOB_MAX_SUBMISSION_RETRIES"));
                buf.append("<br>");
                buf.append("<strong>JOB_QUEUE_CONFIG_FOLDER: </strong>");
                buf.append(_properties.getProperty("JOB_QUEUE_CONFIG_FOLDER"));
                buf.append("<br>");
                buf.append("<strong>JOB_MAX_THREADS_PER_QUEUE: </strong>");
                buf.append(_properties.getProperty("JOB_MAX_THREADS_PER_QUEUE"));
                buf.append("<br>");
                buf.append("<strong>JOB_THREAD_RESTART_SECONDS: </strong>");
                buf.append(_properties.getProperty("JOB_THREAD_RESTART_SECONDS"));
                buf.append("<br>");
                buf.append("<strong>JOB_THREAD_RESTART_LIMIT: </strong>");
                buf.append(_properties.getProperty("JOB_THREAD_RESTART_LIMIT"));
                buf.append("<br>");
                buf.append("<strong>DEDICATED_TENANT_ID: </strong>");
                buf.append(_properties.getProperty("DEDICATED_TENANT_ID"));
                buf.append("<br>");
                buf.append("<strong>DRAIN_ALL_QUEUES_ENABLED: </strong>");
                buf.append(_properties.getProperty("DRAIN_ALL_QUEUES_ENABLED"));
                buf.append("<br>");
                break;
            case SCHEDULER_EXCEPTION:
                buf.append("<strong>Scheduler name: </strong>");
                buf.append(_properties.getProperty("SCHEDULER_NAME"));
                buf.append("<br>");
                buf.append("<strong>Exception type: </strong>");
                buf.append(_properties.getProperty("EXCEPTION_TYPE"));
                buf.append("<br>");
                buf.append("<strong>Exception message: </strong>");
                buf.append(_properties.getProperty("EXCEPTION_MESSAGE"));
                buf.append("<br>");
                buf.append("<strong>Action: </strong>");
                buf.append(_properties.getProperty("ACTION"));
                buf.append("<br>");
                break;
            case TOPIC_THREAD_STARTED:
            case INTERRUPT_THREAD_STARTED:
            case ZOMBIE_THREAD_STARTED:
                buf.append("<strong>Thread name: </strong>");
                buf.append(_properties.getProperty("THREAD_NAME"));
                buf.append("<br>");
                break;
            case WORKER_THREAD_STARTED:
            case WORKER_THREAD_SCHEDULED_TO_STOP:
                buf.append("<strong>Thread name: </strong>");
                buf.append(_properties.getProperty("THREAD_NAME"));
                buf.append("<br>");
                buf.append("<strong>Queue name: </strong>");
                buf.append(_properties.getProperty("QUEUE_NAME"));
                buf.append("<br>");
                break;
            case TOPIC_THREAD_EXECPTION:
            case INTERRUPT_THREAD_EXECPTION:
            case ZOMBIE_THREAD_EXECPTION:
            case WORKER_THREAD_EXCEPTION:
                buf.append("<strong>Thread name: </strong>");
                buf.append(_properties.getProperty("THREAD_NAME"));
                buf.append("<br>");
                buf.append("<strong>Exception type: </strong>");
                buf.append(_properties.getProperty("EXCEPTION_TYPE"));
                buf.append("<br>");
                buf.append("<strong>Exception message: </strong>");
                buf.append(_properties.getProperty("EXCEPTION_MESSAGE"));
                buf.append("<br>");
                buf.append("<strong>Action: </strong>");
                buf.append(_properties.getProperty("ACTION"));
                buf.append("<br>");
                break;
            case TPC_DELETE_JOB_RECEIVED:
            case TPC_PAUSE_JOB_RECEIVED:
            case TPC_STOP_JOB_RECEIVED:
                buf.append("<strong>Job name: </strong>");
                buf.append(_properties.getProperty("JOB_NAME"));
                buf.append("<br>");
                buf.append("<strong>Job uuid: </strong>");
                buf.append(_properties.getProperty("JOB_UUID"));
                buf.append("<br>");
                buf.append("<strong>Job epoch: </strong>");
                buf.append(_properties.getProperty("JOB_EPOCH"));
                buf.append("<br>");
                break;
            case SCHEDULER_STOPPING:
            case TPC_SHUTDOWN_RECEIVED:
            case TPC_REFRESH_QUEUE_INFO:
                buf.append("<strong>Scheduler name: </strong>");
                buf.append(_properties.getProperty("SCHEDULER_NAME"));
                buf.append("<br>");
                break;
            case TPC_RESET_NUM_WORKERS_RECEIVED:
                buf.append("<strong>Queue name: </strong>");
                buf.append(_properties.getProperty("QUEUE_NAME"));
                buf.append("<br>");
                buf.append("<strong>Number of workers delta: </strong>");
                buf.append(_properties.getProperty("NUM_WORKERS_DELTA"));
                buf.append("<br>");
                break;
            case TPC_NOOP_RECEIVED:
                buf.append("<strong>Message: </strong>");
                buf.append(_properties.getProperty("NOOP_MESSAGE"));
                buf.append("<br>");
                break;
            default:
                break;
        }
        buf.append("</p>");
        
        return buf.toString();
    }

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getEmailSubject()
	 */
	@Override
	public String getEmailSubject()
	{
		String subject = "Job scheduler " + associatedUuid + " generated event " + event;
		return subject;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.AbstractNotificationEvent#resolveMacros(java.lang.String, boolean)
	 */
	@Override
	public String resolveMacros(String body, boolean urlEncode)
	{
		return body;
	}
	
	/** Convert any custom json data into a properties object and assign
	 * it to the properties field.  If no json data is available or if 
	 * the conversion fails, assign the field an empty properties object.
	 */
	private void assignProperties()
	{
	    // No need to calculate the properties more than once.
	    if (_properties != null) return;
	    
	    // Initialization.
	    String customData = getCustomNotificationMessageContextData();
	    
        // Convert the customData from json to a map.
        if (!StringUtils.isBlank(customData)) {
            try {_properties = mapper.readValue(customData, Properties.class);}
            catch (Exception e) {
                String msg = "JobSchedulerNotificationEvent unable to parse custom data.";
                _log.error(msg, e);
            } 
        }
	    
        // Always assign the properties variable.
        if (_properties == null) _properties = new Properties();
	}
}
