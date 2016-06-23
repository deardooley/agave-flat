/**
 *
 */
package org.iplantc.service.jobs.managers;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.jobs.exceptions.JobEventProcessingException;
import org.iplantc.service.jobs.model.JobEvent;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.notification.managers.NotificationManager;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.model.ExecutionSystem;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Handles processing of all {@link JobEvent}s created in the service.
 * 
 * @author dooley
 *
 */
public class JobEventProcessor {

    private static final Logger log = Logger.getLogger(JobEventProcessor.class);

    private JobEvent event;
    
    public JobEventProcessor() {}
    
    public JobEventProcessor(JobEvent event) throws JobEventProcessingException {
        this.setEvent(event);
    }

    public void process() {

        ObjectMapper mapper = new ObjectMapper();
    	JsonNode jsonJob = null;
        try {
        	jsonJob = mapper.readTree(getEvent().getJob().toJSON());
        } catch (Exception e) {
            log.error("Unable to generate job json for notification", e);
            try {
	            jsonJob = mapper.createObjectNode()
	            		.put("id", getEvent().getJob().getId())
	            		.put("name", getEvent().getJob().getName())
	            		.put("executionSystem", getEvent().getJob().getSystem())
	            		.put("appId", getEvent().getJob().getSoftwareName())
	            		.put("owner", getEvent().getJob().getOwner());
            } catch (Exception e1) {
            	// init an object so we're not completely empty
            	jsonJob = mapper.createObjectNode();
            }
        }

        // Send the job event itself
        NotificationManager.process(getEvent().getJob().getUuid(), getEvent().getStatus(), getEvent().getCreatedBy(), jsonJob.toString());

        // process job related events for systems and apps
        ObjectNode jsonContent = mapper.createObjectNode();
        jsonContent.put("job", jsonJob);
        
        if (getEvent().getStatus().equalsIgnoreCase(JobStatusType.PENDING.name()))
        {
        	processJobExecutionSystemEvent("JOB_CREATED", jsonJob);
        	
        	processJobSoftwareEvent("JOB_CREATED", jsonJob);
        }
        else if (getEvent().getStatus().equalsIgnoreCase(JobStatusType.QUEUED.name()))
        {
        	processJobExecutionSystemEvent("JOB_QUEUED", jsonJob);
        	
            // NotificationManager.process(executionSystem.getQueue(getEvent().getJob().getBatchQueue()).getUuid(), "JOB_SUBMITTED", getEvent().getCreatedBy(), jsonJob);
        }
        else if (getEvent().getStatus().equalsIgnoreCase(JobStatusType.RUNNING.name()))
        {
        	processJobExecutionSystemEvent("JOB_RUNNING", jsonJob);
        	
        	// NotificationManager.process(executionSystem.getQueue(getEvent().getJob().getBatchQueue()).getUuid(), "JOB_RUNNING", getEvent().getCreatedBy(), jsonJob);
        }
        else if (getEvent().getStatus().equalsIgnoreCase(JobStatusType.FINISHED.name()) ||
                getEvent().getStatus().equalsIgnoreCase(JobStatusType.FAILED.name()) ||
                getEvent().getStatus().equalsIgnoreCase(JobStatusType.STOPPED.name()) || 
                getEvent().getStatus().equalsIgnoreCase(JobStatusType.PAUSED.name()))
        {
        	processJobExecutionSystemEvent("JOB_" + getEvent().getStatus(), jsonJob);
        	
        	// NotificationManager.process(executionSystem.getQueue(getEvent().getJob().getBatchQueue()).getUuid(), "JOB_COMPLETED", getEvent().getCreatedBy(), jsonJob);
        }        
    }
    
    /**
     * Processes a custom user-defined runtime job event by throwing the event onto the 
     * notification queue for the job, but replacing the standard serialized json content 
     * with the runtime data. The 
     * @param customRuntimeJsonBody the JSON object sent from the job to forward on in the notification callback 
     */
    public void processJobRuntimeCallbackEvent(JsonNode customRuntimeJsonBody) {    
    	// Send the job event itself
    	if (customRuntimeJsonBody == null) {
    		NotificationManager.process(getEvent().getJob().getUuid(), 
    									getEvent().getStatus(), 
    									getEvent().getCreatedBy(), 
    									"{}");
    	}
    	// else make sure payload is under 5k so we don't blow out our queue with
    	// user data
    	else if (StringUtils.length(customRuntimeJsonBody.toString()) > 5120) {
    		log.error("Failed to send " + getEvent().getStatus() + " custom runtime event for job " + 
    				getEvent().getJob().getUuid() + " because the message content exceeds the max " +
    				"job callback message size of 5k");
	    }
    	else {
    		NotificationManager.process(getEvent().getJob().getUuid(), 
					    				getEvent().getStatus(), 
					    				getEvent().getCreatedBy(), 
					    				customRuntimeJsonBody.toString());
    	}
    }
    
    /**
     * Constructs message content and sends message for the execution system on
     * which a job event occurs.
     * @param eventName
     * @param jsonJob
     * @return true if sent, false otherwise
     */
    protected boolean processJobExecutionSystemEvent(String eventName, JsonNode jsonJob) 
    {
    	ObjectMapper mapper = new ObjectMapper();
    	ObjectNode jsonContent = mapper.createObjectNode();
    	ExecutionSystem executionSystem = null;
        try 
    	{
            executionSystem = JobManager.getJobExecutionSystem(getEvent().getJob());
            
            jsonContent.put("job", jsonJob);
        	jsonContent.put("system", mapper.readTree(executionSystem.toJSON()));
        
    		NotificationManager.process(executionSystem.getUuid(), eventName, getEvent().getCreatedBy(), jsonContent.toString());
    		
    		return true;
        } 
        catch (JsonProcessingException e) {
        	log.error("Unable to serialize system " + getEvent().getJob().getSystem() 
    				+ " while processing a " + eventName + " event for job " + getEvent().getJob().getUuid(), e);
        	
        	if (jsonContent.has("system")) {
        		jsonContent.remove("system");
        	}
        	
        	jsonContent.put("job", jsonJob);
        	
    		NotificationManager.process(executionSystem.getUuid(), eventName, getEvent().getCreatedBy(), jsonContent.toString());
    		
        }
        catch (SystemUnavailableException e) {
            log.error("Unable to process " + eventName + " event for system " + getEvent().getJob().getSystem()
            		 + " by job " + getEvent().getJob().getUuid() + ". Unable to resolve system id", e);
        }
    	catch (Exception e) {
    		log.error("Unable to process " + eventName + " event for system " + getEvent().getJob().getSystem() 
    				+ " by job " + getEvent().getJob().getUuid(), e);
    	}
        
        return false;
    }
    
    /**
     * Constructs message content and sends message for the execution system on
     * which a job event occurs.
     * 
     * @param eventName
     * @param jsonJob
     * @return true if sent, false otherwise
     */
    protected boolean processJobSoftwareEvent(String eventName, JsonNode jsonJob) 
    {
    	Software software = null;
    	ObjectMapper mapper = new ObjectMapper();
    	ObjectNode jsonContent = mapper.createObjectNode();
        try 
    	{
        	software = JobManager.getJobSoftwarem(getEvent().getJob());
            jsonContent.set("job", jsonJob);
            jsonContent.set("software", mapper.readTree(software.toJSON()));
        
    		NotificationManager.process(software.getUuid(), eventName, getEvent().getCreatedBy(), jsonContent.toString());
    		
    		return true;
        } 
        catch (JsonProcessingException e) {
        	log.error("Unable to serialize software " + getEvent().getJob().getSoftwareName() 
    				+ " while processing a " + eventName + " event for job " + getEvent().getJob().getUuid(), e);
        	
        	if (jsonContent.has("software")) {
        		jsonContent.remove("software");
        	}
        	
        	NotificationManager.process(software.getUuid(), eventName, getEvent().getCreatedBy(), jsonContent.toString());
    		
        }
        catch (Exception e) {
    		log.error("Unable to process " + eventName + " event for software " + getEvent().getJob().getSoftwareName()
    				+ " by job " + getEvent().getJob().getUuid(), e);
    	}
        
        return false;
    }

    /**
     * @return the event
     */
    public JobEvent getEvent() {
        return event;
    }

    /**
     * @param event the event to set
     */
    public void setEvent(JobEvent event) throws JobEventProcessingException {
        this.event = event;
    }

}
