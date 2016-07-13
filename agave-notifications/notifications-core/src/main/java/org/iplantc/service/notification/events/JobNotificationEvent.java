/**
 * 
 */
package org.iplantc.service.notification.events;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.util.TimeUtils;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.model.Notification;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author dooley
 *
 */
public class JobNotificationEvent extends AbstractEventFilter {

	private static final Logger log = Logger.getLogger(JobNotificationEvent.class);
	
	/**
	 * @param notification
	 */
	public JobNotificationEvent(AgaveUUID associatedUuid, Notification notification, String event, String owner)
	{
		super(associatedUuid, notification, event, owner);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getEmailBody()
	 */
	@Override
	public String getEmailBody()
	{
		String body = "The status of job ${JOB_ID}, \"${JOB_NAME},\" " +
				"has changed to ${EVENT}.\n" +
				"\n" +
				"Name: ${JOB_NAME}\n" +
				"ID: ${JOB_ID}\n" +
				"App ID: ${APP_ID}\n" +
                "Execution System: ${JOB_SYSTEM}\n" +
                "Message: ${JOB_ERROR}\n" +
				"Submit Time: ${JOB_SUBMIT_TIME}\n" +
				"Start Time: ${JOB_START_TIME}\n" +
				"End Time: ${JOB_END_TIME}\n" +
				"Output Path: ${JOB_ARCHIVE_PATH}\n" + 
				"Output URL: ${JOB_ARCHIVE_URL}\n"; 
//		if (StringUtils.equalsIgnoreCase(event, "AGAVE_JOB_CALLBACK_NOTIFICATION"))
		return resolveMacros(body, false);
	}
	
	/* (non-Javadoc)
     * @see org.iplantc.service.notification.events.EventFilter#getHtmlEmailBody()
     */
    @Override
    public String getHtmlEmailBody()
    {
        String body = "<p>The status of job ${JOB_ID}, \"${JOB_NAME},\" " +
                "has changed to ${EVENT}.</p>" +
                "<br>" +
                "<p><strong>Name:</strong> ${JOB_NAME}<br>" +
                "<strong>ID:</strong> ${JOB_ID}<br>" +
                "<strong>AppId:</strong> ${APP_ID}<br>" +
                "<strong>Execution System:</strong> ${JOB_SYSTEM}<br>" +
                "<strong>Message:</strong> ${JOB_ERROR}<br>" +
                "<strong>Submit Time:</strong> ${JOB_SUBMIT_TIME}<br>" +
                "<strong>Start Time:</strong> ${JOB_START_TIME}<br>" +
                "<strong>End Time:</strong> ${JOB_END_TIME}<br>" +
                "<strong>Output Path:</strong> ${JOB_ARCHIVE_PATH}<br>" + 
                "<strong>Output URL:</strong> ${JOB_ARCHIVE_URL}</p>"; 
        
        return resolveMacros(body, false);
    }
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getEmailSubject()
	 */
	@Override
	public String getEmailSubject()
	{
		return resolveMacros("Job " + associatedUuid.toString() + 
				" received a ${EVENT} event", false);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.AbstractNotificationEvent#resolveMacros(java.lang.String, boolean)
	 */
	@Override
	public String resolveMacros(String body, boolean urlEncode)
	{
		try 
		{
			body = StringUtils.replace(body, "${UUID}", this.associatedUuid.toString());
			body = StringUtils.replace(body, "${EVENT}", this.event);
			body = StringUtils.replace(body, "${OWNER}", this.owner);
			
			if (StringUtils.isNotEmpty(getCustomNotificationMessageContextData())) {
			    ObjectMapper mapper = new ObjectMapper();
			    try {
			        JsonNode json = mapper.readTree(getCustomNotificationMessageContextData());
    			    if (json.isObject()) {
    			    	
    			    	if (json.hasNonNull("job") || json.hasNonNull("id") ) {
    			    		JsonNode jsonJob = null;
    			    		if (json.hasNonNull("job")) {
    			    			jsonJob = json.get("job");
    			    		}
    			    		else {
    			    			jsonJob = json;
    			    		}
    			    		
                            body = StringUtils.replace(body, "${JOB_APPID}", jsonJob.hasNonNull("appId") ? jsonJob.get("appId").asText() : "");
                            body = StringUtils.replace(body, "${JOB_APP_ID}", jsonJob.hasNonNull("appId") ? jsonJob.get("appId").asText() : "");
                            body = StringUtils.replace(body, "${APP_ID}", jsonJob.hasNonNull("appId") ? jsonJob.get("appId").asText() : "");
                            body = StringUtils.replace(body, "${JOB_BATCH_QUEUE}", jsonJob.hasNonNull("batchQueue") ? jsonJob.get("batchQueue").asText() : "");
                            body = StringUtils.replace(body, "${JOB_CREATED}", jsonJob.hasNonNull("created") ? jsonJob.get("created").asText() : "");
                            body = StringUtils.replace(body, "${JOB_END_TIME}", jsonJob.hasNonNull("endTime") ? jsonJob.get("endTime").asText() : "");
                            body = StringUtils.replace(body, "${JOB_ENDTIME}", jsonJob.hasNonNull("endTime") ? new DateTime(jsonJob.get("endTime").asText()).toString(): "");
                            body = StringUtils.replace(body, "${JOB_EXECUTION_SYSTEM}", jsonJob.hasNonNull("executionSystem") ? jsonJob.get("executionSystem").asText() : "");
                            body = StringUtils.replace(body, "${JOB_ID}", jsonJob.hasNonNull("id") ? jsonJob.get("id").asText() : "");
                            body = StringUtils.replace(body, "${JOB_INPUTS}", jsonJob.hasNonNull("inputs") ? jsonJob.get("inputs").toString() : "");
                            body = StringUtils.replace(body, "${JOB_LOCAL_ID}", jsonJob.hasNonNull("localId") ? jsonJob.get("localId").asText() : "");
                            body = StringUtils.replace(body, "${JOB_MAX_RUNTIME}", jsonJob.hasNonNull("maxRunTime") ? jsonJob.get("maxRunTime").asText() : "");
                            body = StringUtils.replace(body, "${JOB_MAX_RUNTIME_MILLISECONDS}",jsonJob.hasNonNull("maxRunTime") ? String.valueOf(TimeUtils.getMillisecondsForMaxTimeValue(jsonJob.get("maxRunTime").asText())) : "");
                            body = StringUtils.replace(body, "${JOB_MEMORY_PER_NODE}", jsonJob.hasNonNull("memoryPerNode") ? jsonJob.get("memoryPerNode").asText() : "");
                            body = StringUtils.replace(body, "${JOB_NAME}", jsonJob.hasNonNull("name") ? jsonJob.get("name").asText() : "");
                            body = StringUtils.replace(body, "${JOB_NODE_COUNT}", jsonJob.hasNonNull("nodeCount") ? jsonJob.get("nodeCount").asText() : "");
                            body = StringUtils.replace(body, "${JOB_OWNER}", jsonJob.hasNonNull("owner") ? jsonJob.get("owner").asText() : "");
                            body = StringUtils.replace(body, "${JOB_OUTPUT_PATH}", jsonJob.hasNonNull("outputPath") ? jsonJob.get("outputPath").asText() : "");
                            body = StringUtils.replace(body, "${JOB_PARAMETERS}", jsonJob.hasNonNull("parameters") ? jsonJob.get("parameters").toString() : "");
                            body = StringUtils.replace(body, "${JOB_PROCESSORS_PER_NODE}", jsonJob.hasNonNull("processorsPerNode") ? jsonJob.get("processorsPerNode").asText() : "");
                            body = StringUtils.replace(body, "${JOB_STATUS}", jsonJob.hasNonNull("status") ? jsonJob.get("status").asText() : "");
                            body = StringUtils.replace(body, "${JOB_START_TIME}", jsonJob.hasNonNull("startTime") ? jsonJob.get("startTime").asText() : "");
                            body = StringUtils.replace(body, "${JOB_STARTTIME}", jsonJob.hasNonNull("startTime") ? new DateTime(jsonJob.get("startTime").asText()).toString(): "");
                            body = StringUtils.replace(body, "${JOB_SUBMIT_TIME}", jsonJob.hasNonNull("submitTime") ? jsonJob.get("submitTime").asText() : "");
                            body = StringUtils.replace(body, "${JOB_SUBMITTIME}", jsonJob.hasNonNull("submitTime") ? new DateTime(jsonJob.get("submitTime").asText()).toString(): "");
                            body = StringUtils.replace(body, "${JOB_SYSTEM}", jsonJob.hasNonNull("executionSystem") ? jsonJob.get("executionSystem").asText() : "");
                            body = StringUtils.replace(body, "${JOB_TENANT}", TenancyHelper.getCurrentTenantId());
                            body = StringUtils.replace(body, "${JOB_URL}", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE) + jsonJob.get("id").asText());
                            
                            if (StringUtils.contains(body, "${JOB_ERROR}")) {
	                            try {
	                            	Map<String, Object> jobFieldMap = getJobRow("jobs", associatedUuid.toString());
	                            	body = StringUtils.replace(body, "${JOB_ERROR}", (String)jobFieldMap.get("error_message"));
	                            }
	                            catch (Throwable t) {
	                            	body = StringUtils.replace(body, "${JOB_ERROR}", "");
	                            }
                            }
                            
                            boolean archive = jsonJob.hasNonNull("archive") ? jsonJob.get("archive").asBoolean(false) : false;
                            if (archive) {
                            	body = StringUtils.replace(body, "${JOB_ARCHIVE}", "true");
                            	body = StringUtils.replace(body, "${JOB_ARCHIVE_SYSTEM}", jsonJob.hasNonNull("archiveSystem") ? jsonJob.get("archiveSystem").asText() : "");
                            	body = StringUtils.replace(body, "${JOB_ARCHIVEPATH}", jsonJob.hasNonNull("archivePath") ? jsonJob.get("archivePath").asText() : "");
                                body = StringUtils.replace(body, "${JOB_ARCHIVE_PATH}", jsonJob.hasNonNull("archivePath") ? jsonJob.get("archivePath").asText() : "");
                            } else {
                            	body = StringUtils.replace(body, "${JOB_ARCHIVE}", "false");
                            	body = StringUtils.replace(body, "${JOB_ARCHIVE_SYSTEM}", "");
                                body = StringUtils.replace(body, "${JOB_ARCHIVE_PATH}", "");
                            }
                            
                            String archiveUrl = null;
                            if (jsonJob.hasNonNull("_links") && jsonJob.get("_links").hasNonNull("archiveData") && jsonJob.get("_links").get("archiveData").hasNonNull("href")) {
                            	archiveUrl = jsonJob.get("_links").get("archiveData").get("href").asText();
                            } 
                            else {
                            	archiveUrl = TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE) + associatedUuid.toString() + "/outputs/listings";
                            }
                            body = StringUtils.replace(body, "${JOB_ARCHIVE_URL}", archiveUrl);
                            body = StringUtils.replace(body, "${JOB_ARCHIVEURL}", archiveUrl);
                            
                            body = StringUtils.replace(body, "${JOB_JSON}", jsonJob.get("job").toString());
    			    	}
    			    	
    			    	if (json.hasNonNull("permission") ) {
                        	JsonNode jsonPermission = json.get("permission");
                            body = StringUtils.replace(body, "${PERMISSION_ID}", jsonPermission.hasNonNull("id") ? jsonPermission.get("id").asText() : "");
                            body = StringUtils.replace(body, "${PERMISSION_PERMISSION}", jsonPermission.hasNonNull("permission") ? jsonPermission.get("permission").asText() : "");
                            body = StringUtils.replace(body, "${PERMISSION_USERNAME}", jsonPermission.hasNonNull("username") ? jsonPermission.get("username").asText() : "");
                            body = StringUtils.replace(body, "${PERMISSION_LASTUPDATED}", jsonPermission.hasNonNull("lastUpdated") ? jsonPermission.get("lastUpdated").asText() : "");
                            body = StringUtils.replace(body, "${PERMISSION_JSON}", jsonPermission.toString());
                        } 
    			    }
    			    body = StringUtils.replace(body, "${RAW_JSON}", mapper.writer().withDefaultPrettyPrinter().writeValueAsString(getCustomNotificationMessageContextData()));
			    } catch (Exception e) {
			        body = StringUtils.replace(body, "${RAW_JSON}", getCustomNotificationMessageContextData());
			    }
			}
			else {
			    Map<String, Object> jobFieldMap = getJobRow("jobs", associatedUuid.toString());
			
				body = StringUtils.replace(body, "${APP_ID}", (String)jobFieldMap.get("software_name"));
				body = StringUtils.replace(body, "${JOB_ID}", associatedUuid.toString());
				body = StringUtils.replace(body, "${JOB_SYSTEM}", (String)jobFieldMap.get("execution_system"));
				body = StringUtils.replace(body, "${JOB_STATUS}", event);
				body = StringUtils.replace(body, "${JOB_NAME}", (String)jobFieldMap.get("name"));
				body = StringUtils.replace(body, "${JOB_OWNER}", (String)jobFieldMap.get("owner"));
				body = StringUtils.replace(body, "${JOB_URL}", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE) + associatedUuid.toString());
				body = StringUtils.replace(body, "${JOB_SUBMIT_TIME}", new DateTime(jobFieldMap.get("submit_time")).toString());
				body = StringUtils.replace(body, "${JOB_START_TIME}", new DateTime(jobFieldMap.get("start_time")).toString());
				body = StringUtils.replace(body, "${JOB_END_TIME}", new DateTime(jobFieldMap.get("end_time")).toString());
				body = StringUtils.replace(body, "${JOB_ARCHIVE_URL}", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE) + associatedUuid.toString() + "/outputs/listings");
				body = StringUtils.replace(body, "${JOB_ARCHIVE_PATH}", (String)jobFieldMap.get("archive_path"));
				body = StringUtils.replace(body, "${JOB_ERROR}", (String)jobFieldMap.get("error_message"));
			}
			
			return body;
		}
		catch (Exception e) {
			log.error("Failed to create notification body for job " + associatedUuid.toString(), e);
			return "The status of job " + associatedUuid.toString() +
					" has changed to " + associatedUuid.toString();
		}
	}
}
