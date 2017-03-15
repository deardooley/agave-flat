package org.iplantc.service.jobs.model;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.notification.events.enumerations.JobSchedulerEventType;

import com.fasterxml.jackson.databind.ObjectMapper;

/** Job scheduler event model object.  This class stores event parameters as
 * key/value pairs in a properties object.  When events are transmitted, the
 * properties object can be converted to JSON.
 * 
 * @author rcardone
 */
public class JobSchedulerEvent
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(JobSchedulerEvent.class);

    /* ********************************************************************** */
    /*                                 Fields                                 */
    /* ********************************************************************** */
    // Shared json mapper.
    private static final ObjectMapper mapper = new ObjectMapper();
    
    // Message field.
    private final AgaveUUID associatedUuid;
    private final JobSchedulerEventType event;
    private final String user;
    private final Properties properties;
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    public JobSchedulerEvent(AgaveUUID associatedUuid, JobSchedulerEventType event, 
                             String user, Properties properties)
    {
        this.associatedUuid = associatedUuid;
        this.event = event;
        this.user = user;
        this.properties = properties;
    }
    
    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* getPropertiesAsJson:                                                   */
    /* ---------------------------------------------------------------------- */
    public String getPropertiesAsJson()
    {
        // Dump properties mapping into a json string.
        String json = null;
        try {json = mapper.writeValueAsString(properties);}
        catch (Exception e){
            String msg = "Unable to convert job scheduler event properties to json.";
            _log.error(msg, e);
            json = "{}";  // return something.
        }
        return json;
    }
    
    /* ********************************************************************** */
    /*                               Accessors                                */
    /* ********************************************************************** */
    public AgaveUUID getAssociatedUuid()
    {
        return associatedUuid;
    }
    public JobSchedulerEventType getEvent()
    {
        return event;
    }
    public String getUser()
    {
        return user;
    }
    public Properties getProperties()
    {
        return properties;
    }
}
