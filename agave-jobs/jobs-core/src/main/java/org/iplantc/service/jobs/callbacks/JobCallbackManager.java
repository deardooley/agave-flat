package org.iplantc.service.jobs.callbacks;

import static org.iplantc.service.jobs.model.enumerations.JobStatusType.ARCHIVING;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.ARCHIVING_FAILED;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.ARCHIVING_FINISHED;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.CLEANING_UP;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.HEARTBEAT;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.RUNNING;

import java.io.FileNotFoundException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.ObjectNotFoundException;
import org.iplantc.service.apps.model.enumerations.SoftwareEventType;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobCallbackException;
import org.iplantc.service.jobs.exceptions.JobEventProcessingException;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.managers.JobEventProcessor;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobEvent;
import org.iplantc.service.jobs.model.enumerations.JobEventType;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.enumerations.SystemEventType;

import com.fasterxml.jackson.databind.JsonNode;

public class JobCallbackManager {
    
    private static final Logger log = Logger.getLogger(JobCallbackManager.class);

    /**
     * Handles general {@link Job} status callbacks sent to the trigger endpoints 
     * from running jobs as GET requests.
     * 
     * @param callback
     * @return the updated job
     * @throws JobCallbackException
     * @throws ObjectNotFoundException
     * @throws JobException
     */
    public Job process(JobCallback callback) 
    throws JobCallbackException, ObjectNotFoundException, JobException
    {
        if (callback == null) {
            throw new JobCallbackException("Callback cannot be null.");
        }
        
        Job job = callback.getJob();
        
        if (job == null) {
            throw new ObjectNotFoundException(null, "No job found with given id");
        } 
        else if (!job.isVisible()) {
            throw new JobCallbackException("Failed to process callback. Job has already been deleted.");
        }
        else 
        {
//            if (!job.isRunning() && JobStatusType.isRunning(callback.getStatus()))
//            {
//                // can't set a stopped job back to running. Bad request
//                throw new JobCallbackException("Job " + callback.getJob().getUuid() + " is not running.");
//            }
            
            TenancyHelper.setCurrentEndUser(job.getOwner());
            TenancyHelper.setCurrentTenantId(job.getTenantId());
            
            validateLocalSchedulerJobId(callback);
            
            String message = null;
            JobStatusType status = callback.getStatus();
            
            // the HEARTBEAT status is used to update the job timestamp and is used
            // by app developers just to keep aware of a job being alive.
            if (!job.isArchiveOutput() && (status == ARCHIVING || 
                    status == ARCHIVING_FAILED || 
                    status == ARCHIVING_FINISHED)) 
            {
                // can't update an archive status when the job is not set to archive
                throw new JobCallbackException("Job " + job.getUuid()
                        + " is not configured for archive.");
            } 
            else if (!ArrayUtils.contains(job.getStatus().getNextValidStates(), status)) 
            {
                throw new JobCallbackException("Job " + job.getUuid()
                        + " cannot update status from " + job.getStatus().name() 
                        + " to " + status.name() + ".");
            }
            else
            {
                // heartbeats need to set an event, but they do not udpate the job status.
                if (status == HEARTBEAT) 
                {
                    log.debug("Job " + job.getUuid() + " received heartbeat notification.");
                    JobEvent event = new JobEvent(HEARTBEAT, HEARTBEAT.getDescription(), job.getOwner());
                    job.addEvent(event);
                    JobDao.persist(job);
                    
                    status = job.getStatus();
                    message = job.getErrorMessage();
                }
                else if (status == CLEANING_UP)
                {
                    // if job was not running, then fetch the job start time from the 
                    // remote system offline so we have accurate reporting
                    if (job.getStatus() != RUNNING) {
//                        RemoteAccountingValidator validator = new RemoteAccoutingValidator(job);
//                        validator.run();
                    }
                    if (job.getStatus() == CLEANING_UP) {
                        log.debug("Job " + job.getUuid() + " received notification identical to its current status.");
                        message = "Job receieved duplicate " + status.name() + " notification";
                    } else {
                        log.debug("Job " + job.getUuid() + " received " + status.name() + " notification.");
                        message = CLEANING_UP.getDescription();
                    }
                    
                    job = JobManager.updateStatus(job, CLEANING_UP, message);
                    
                    if (!job.isArchiveOutput()) {
                        log.debug("Job " + job.getUuid() + " will skip archiving at user request.");
                        status = JobStatusType.FINISHED;
                        message = "Job completed. Skipping archiving at user request.";
                    }
                }
                else if (job.getStatus() == status) 
                {
                    log.debug("Job " + job.getUuid() + " received notification identical to its current status.");
                    message = "Job receieved duplicate " + status.name() + " notification";
                }
                else 
                {
                    log.debug("Job " + job.getUuid() + " received " + status.name() + " notification.");
                    message = status.getDescription();
                }
            
                return JobManager.updateStatus(job, status, message);
            }
        }
    }
    
    /**
     * Handles custom runtime {@link Job} job events thrown from running jobs 
     * as POST requests. The requests contain a JSON object which gets propagated 
     * to any notifications sent to the user.
     *  
     * @param callback
     * @param customRuntimeJsonBody
     * @return the updated job
     * @throws JobCallbackException
     * @throws ObjectNotFoundException
     * @throws FileNotFoundException
     * @throws JobException
     */
    public Job processCustomRuntimeEvent(JobCallback callback, JsonNode customRuntimeJsonBody) 
    throws JobCallbackException, ObjectNotFoundException, FileNotFoundException, JobException
    {
    	if (callback == null) {
            throw new JobCallbackException("Callback cannot be null.");
        }
        
        Job job = callback.getJob();
        
        // unknown job. no callback will be thrown
        if (job == null) {
            throw new ObjectNotFoundException(null, "No job found with given id");
        } 
        // deleted jobs have their runtime callbacks disabled
        else if (!job.isVisible()) {
            throw new JobCallbackException("Failed to process callback. Job has already been deleted.");
        }
        // runtime callbacks are only valid on running jobs
        else if (!job.isRunning()) {
            throw new JobCallbackException("Job " + job.getUuid() + " is not running.");
        }
        // job is running and valid
        else {
            TenancyHelper.setCurrentEndUser(job.getOwner());
            TenancyHelper.setCurrentTenantId(job.getTenantId());
            
            JobStatusType status = callback.getStatus();
            
            // we're not processing empty messages. they can use heartbeats for that
            if (customRuntimeJsonBody == null) {
            	throw new JobCallbackException("Empty JSON callback content found.");
            }
            // verify file size does not exceed 5120 characters, ie. 10KB
            else if (StringUtils.length(customRuntimeJsonBody.toString()) > 5120) {
            	throw new JobCallbackException("Message content exceeds the maximum " +
        				"callback message size of 10KB");
    	    }
            // heartbeats need to set an event, but they do not update the job status.
            else if (status == HEARTBEAT) 
            {
                log.debug("Job " + job.getUuid() + 
                		" received custom runtime event notification for job " + 
                		callback.getJob().getUuid());
                
                // response should be a json ojbect
                if (!customRuntimeJsonBody.isObject()) {
                	throw new JobCallbackException("Callback content is not a valid JSON object.");
                }
                // malformed json. no event was in body
                else if (!customRuntimeJsonBody.hasNonNull("CUSTOM_USER_JOB_EVENT_NAME")) {
                	throw new JobCallbackException("Malformed JSON callback content found.");
                }
	            // seems like valid json. process the event as a custom runtime event on the job
                else {
                	
                	String customEventName = customRuntimeJsonBody.get("CUSTOM_USER_JOB_EVENT_NAME").asText();
                	
                	if (isReservedEventName(customEventName)) {
                		throw new JobCallbackException("Illegal runtime event name. " + customEventName + 
                				" is reserved for internally generated events.");
                	}
                	else {
	                	JobEvent event = new JobEvent(customEventName, 
	                			"A custom runtime " + customEventName + " event was received for job " + 
	                					job.getUuid(), job.getOwner());
	                	event.setJob(job);
	                	
		                job.getEvents().add(event);
		                JobDao.persist(job);
		                
		                JobEventProcessor eventProcessor;
						try {
							eventProcessor = new JobEventProcessor(event);
							eventProcessor.processJobRuntimeCallbackEvent(customRuntimeJsonBody);
							
							return job;
						} 
						catch (JobEventProcessingException e) {
							throw new JobCallbackException(e.getMessage(), e);
						}
                	}
                }
            }
            else {
            	throw new FileNotFoundException("Illegal job callback reqeust.");
            }
                
        }
    }
    

    /**
     * Verifies the given user event is not an existing job, system, or software event
     * to protect against interal conflicts and unexpected behavior.
     * 
	 * @param customEventName
	 * @return {@code true} if the {@code customEventName} does not conflict with an existing event. {@code false} otherwise
	 */
	protected boolean isReservedEventName(String customEventName) {
		// check this isn't an existing event
	   	try {
	   		if (JobStatusType.valueOf(StringUtils.upperCase(customEventName)) != null) {
	   			return false;
	   		}
	   	} catch (Exception e) {}
	   	
	   	try {
       		if (JobEventType.valueOf(StringUtils.upperCase(customEventName)) != null) {
       			return false;
       		}
       	} catch (Exception e) {}
	   	
	   	try {
       		if (SystemEventType.valueOf(StringUtils.upperCase(customEventName)) != null) {
       			return false;
       		}
       	} catch (Exception e) {}
	   	
	   	try {
       		if (SoftwareEventType.valueOf(StringUtils.upperCase(customEventName)) != null) {
       			return false;
       		}
       	} catch (Exception e) {}
	   	
	   	return true;
	}

/**
    * Check for the existence of an updated local job id. 
    * <br>
    * <br>
    * <strong>note:</strong> This can currently be set once, though it may make sense 
    * to update this more than once if job ids are reused on the 
    * remote {@link ExecutionSystem} (<em>I'm talking about you LSF</em>)
    * 
    * @param callback
    * @throws JobCallbackException
    * @throws JobException
    */
   private void validateLocalSchedulerJobId(JobCallback callback) 
   throws JobCallbackException, JobException
   {   
       if (StringUtils.equals(callback.getJob().getLocalJobId(), callback.getLocalSchedulerJobId())) 
       {
           // if the job new local job id is null, ignore
           return;
       }
       else if (StringUtils.isEmpty(callback.getLocalSchedulerJobId())) 
       {
           // ignore when the new local job id is empty or null
           return;
       }
       // if the current local scheduler job id is empty, approve the update
       else if (StringUtils.isEmpty(callback.getJob().getLocalJobId()))
       {
           log.debug("Job " + callback.getJob().getUuid() + " received notification of its local job id " 
                   + callback.getLocalSchedulerJobId());
           callback.getJob().setLocalJobId(callback.getLocalSchedulerJobId());
       } 
       // If the local scheduler job is already set, throw an exception. We can 
       // just as easily fail silently or update the job in case of local scheduler 
       // job id reuse.
       else 
       {
           throw new JobCallbackException("Job " + callback.getJob().getUuid() 
                   + " has already been assigned a local id by the scheduler.");
       }
   }
}
