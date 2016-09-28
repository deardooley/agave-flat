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
public class SoftwareNotificationEvent extends AbstractEventFilter {

	private static final Logger log = Logger.getLogger(SoftwareNotificationEvent.class);
	
	/**
	 * @param notification
	 */
	public SoftwareNotificationEvent(AgaveUUID associatedUuid, Notification notification, String event, String owner)
	{
		super(associatedUuid, notification, event, owner);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getEmailBody()
	 */
	@Override
	public String getEmailBody()
	{
		String body = null;
		if (StringUtils.equalsIgnoreCase(event, "created") || StringUtils.equalsIgnoreCase(event, "registered")) {
			body = "App ${APP_ID} was successfully registered. The new app description " +
					"is available from the apps service. ${APP_ID} is available for immediate use.";
		} else if (StringUtils.equalsIgnoreCase(event, "cloned")) {
			body = "App ${APP_ID} was cloned by " + owner + ".";
		} else if (StringUtils.equalsIgnoreCase(event, "deleted")) {
			body = "App ${APP_ID} was deleted by " + owner + ". It is no longer available for execution. " +
					"Running jobs will complete as before. Any new or pending jobs " +
					"utilizing this app will fail.";
		} else if (StringUtils.equalsIgnoreCase(event, "disabled")) {
			body = "App ${APP_ID} was disabled by " + owner + ". It is no longer available for execution.";
		} else if (StringUtils.equalsIgnoreCase(event, "restored")) {
            body = "App ${APP_ID} was restored to service by " + owner + ". It is available for immediate use.";
        } else if (StringUtils.equalsIgnoreCase(event, "updated")) {
			body = "App ${APP_ID} was udpated by " + owner + ". The new app description is available " +
					"from the apps service.";
		} else if (StringUtils.equalsIgnoreCase(event, "permission_grant")) {
			body = "App ${APP_ID} received a new user permission added by " + owner + ". Please verify the user has " + 
					"the proper execution role on the execution system if they intend on " +
					"running jobs using this app.";
		} else if (StringUtils.equalsIgnoreCase(event, "permission_revoke")) {
			body = "App ${APP_ID} had a user permission removed by " + owner + ". Any currently running jobs will " +
					"complete as before. Any new or pending jobs utilizing this app will fail " +
					"for the revoked user.";
		} else if (StringUtils.equalsIgnoreCase(event, "published")) {
			body = "App ${APP_ID} was successfully published by " + owner + ". It is now available for public use.";
		} else if (StringUtils.equalsIgnoreCase(event, "unpublished")) {
            body = "App ${APP_ID} was successfully unpublished by " + owner + 
                    ". All application assets are still archived and available for future "
                    + "use, however the application is no longer available for public use at this time.";
        } else if (StringUtils.equalsIgnoreCase(event, "publishing_failed")) {
            body = "App ${APP_ID} failed to be published by user " + owner + 
                    ". The app will not be publicly available until publishing succeeds.";
		} else if (StringUtils.equalsIgnoreCase(event, "cloning_failed")) {
            body = "App ${APP_ID} failed to be cloned as another app by " + owner;
		} else if (StringUtils.startsWithIgnoreCase(event, "job_")) {
            body = "App ${APP_ID} received a ${EVENT} event from job ${JOB_ID} owned by ${JOB_OWNER}.";
        } else {
			body = "App ${APP_ID} recieved a(n) " + event +  " event by " + owner + ".";
		}
		
		return resolveMacros(body, false);
	}
	
	@Override
	public String getHtmlEmailBody()
    {
	    return "<p>" + getEmailBody() + "</p>";
    }

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getEmailSubject()
	 */
	@Override
	public String getEmailSubject()
	{
		String subject = null;
		if (StringUtils.equalsIgnoreCase(event, "permission_grant")) {
            subject = "App ${APP_ID} added a user permission";
        } else if (StringUtils.equalsIgnoreCase(event, "permission_revoke")) {
            subject = "App ${APP_ID} removed a user permission";
        } else if (StringUtils.equalsIgnoreCase(event, "created")) {
			subject = "App ${APP_ID} was registered";
		} else if (StringUtils.equalsIgnoreCase(event, "cloning_failed")) {
			subject = "App ${APP_ID} failed to be cloned";
		} else if (StringUtils.equalsIgnoreCase(event, "publishing_failed")) {
			subject = "App ${APP_ID} failed to be published";
		} else if (StringUtils.startsWithIgnoreCase(event, "job_")) {
		    subject = "App ${APP_ID} received a ${EVENT} event from job ${JOB_ID}.";
        } else {
			subject = "App ${APP_ID} received a ${EVENT} event";
		}
		
		return resolveMacros(subject, false);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.AbstractNotificationEvent#resolveMacros(java.lang.String, boolean)
	 */
	@Override
	public String resolveMacros(String body, boolean urlEncode)
	{
		try 
		{
			Map<String, Object> jobFieldMap = getJobRow("softwares", associatedUuid.toString());
			
			body = StringUtils.replace(body, "${UUID}", associatedUuid.toString());
			body = StringUtils.replace(body, "${EVENT}", event);
			body = StringUtils.replace(body, "${OWNER}", owner);
			String softwareUniqueName = (String)jobFieldMap.get("name") + "-" + (String)jobFieldMap.get("version");
			if (jobFieldMap.get("publicly_available") instanceof Byte) {
                if ((Byte)jobFieldMap.get("publicly_available") == 1) {
                    softwareUniqueName += "u" + ((Integer)jobFieldMap.get("revision_count")).toString();
                }
            }
			else if (jobFieldMap.get("publicly_available") instanceof Boolean) {
                softwareUniqueName += "u" + ((Integer)jobFieldMap.get("revision_count")).toString();
            }
            else if ((Integer)jobFieldMap.get("publicly_available") == 1) {
                softwareUniqueName += "u" + ((Integer)jobFieldMap.get("revision_count")).toString();
            }
			
			body = StringUtils.replace(body, "${APP_ID}", softwareUniqueName);
			
			if (StringUtils.isNotEmpty(getCustomNotificationMessageContextData())) {
                ObjectMapper mapper = new ObjectMapper();
                try {
                    JsonNode json = mapper.readTree(getCustomNotificationMessageContextData());
                    if (json.isObject()) {
                        if (json.has("job") ) {
                        	JsonNode jsonJob = json.get("job");
                        	body = StringUtils.replace(body, "${JOB_APPID}", jsonJob.hasNonNull("appId") ? jsonJob.get("appId").asText() : "");
                            body = StringUtils.replace(body, "${JOB_APP_ID}", jsonJob.hasNonNull("appId") ? jsonJob.get("appId").asText() : "");
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
                            
                        } else if (json.has("software") || json.has("app") ) {
                        	JsonNode jsonSoftware = json.has("software") ? json.get("software") : json.get("app");
                            body = StringUtils.replace(body, "${APP_ID}", jsonSoftware.has("id") ? jsonSoftware.get("id").asText() : "");
                            body = StringUtils.replace(body, "${APP_NAME}", jsonSoftware.has("name") ? jsonSoftware.get("name").asText() : "");
                            body = StringUtils.replace(body, "${APP_VERSION}", jsonSoftware.has("version") ? jsonSoftware.get("version").asText() : "");
                            body = StringUtils.replace(body, "${APP_OWNER}", jsonSoftware.has("owner") ? jsonSoftware.get("owner").asText() : "");
                            body = StringUtils.replace(body, "${APP_SHORT_DESCRIPTION}", jsonSoftware.has("shortDescription") ? jsonSoftware.get("shortDescription").asText() : "");
                            body = StringUtils.replace(body, "${APP_UUID}", jsonSoftware.has("uuid") ? jsonSoftware.get("uuid").asText() : "");
                            body = StringUtils.replace(body, "${APP_IS_PUBLIC}", jsonSoftware.has("isPublic") ? jsonSoftware.get("isPublic").asText() : "");
                            body = StringUtils.replace(body, "${APP_LABEL}", jsonSoftware.has("label") ? jsonSoftware.get("label").asText() : "");
                            body = StringUtils.replace(body, "${APP_LONG_DESCRIPTION}", jsonSoftware.has("longDescription") ? jsonSoftware.get("longDescription").asText() : "");
                            body = StringUtils.replace(body, "${APP_AVAILABLE}", jsonSoftware.has("available") ? jsonSoftware.get("available").asText() : "");
                            body = StringUtils.replace(body, "${APP_CHECKPOINTABLE}", jsonSoftware.has("checkpointable") ? jsonSoftware.get("checkpointable").asText() : "");
                            body = StringUtils.replace(body, "${APP_DEFAULT_MAX_RUN_TIME}", jsonSoftware.has("defaultMaxRunTime") ? jsonSoftware.get("defaultMaxRunTime").asText() : "");
                            body = StringUtils.replace(body, "${APP_DEFAULT_MEMORY_PER_NODE}", jsonSoftware.has("defaultMemoryPerNode") ? jsonSoftware.get("defaultMemoryPerNode").asText() : "");
                            body = StringUtils.replace(body, "${APP_DEFAULT_PROCESSORS_PER_NODE}", jsonSoftware.has("defaultProcessorsPerNode") ? jsonSoftware.get("defaultProcessorsPerNode").asText() : "");
                            body = StringUtils.replace(body, "${APP_DEFAULT_NODE_COUNT}", jsonSoftware.has("defaultNodeCount") ? jsonSoftware.get("defaultNodeCount").asText() : "");
                            body = StringUtils.replace(body, "${APP_DEFAULT_QUEUE}", jsonSoftware.has("defaultQueue") ? jsonSoftware.get("defaultQueue").asText() : "");
                            body = StringUtils.replace(body, "${APP_DEPLOYMENT_PATH}", jsonSoftware.has("deploymentPath") ? jsonSoftware.get("deploymentPath").asText() : "");
                            body = StringUtils.replace(body, "${APP_DEPLOYMENT_SYSTEM}", jsonSoftware.has("deploymentSystem") ? jsonSoftware.get("deploymentSystem").asText() : "");
                            body = StringUtils.replace(body, "${APP_HELP_URI}", jsonSoftware.has("helpURI") ? jsonSoftware.get("helpURI").asText() : "");
                            body = StringUtils.replace(body, "${APP_URL}", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_APP_SERVICE) + jsonSoftware.get("id").asText());
                            body = StringUtils.replace(body, "${APP_JSON}", jsonSoftware.get("software").toString());
                        } 
                        else if (json.has("system")) {
    			    		JsonNode jsonSystem = json.get("system");
    			    		body = StringUtils.replace(body, "${SYSTEM_ID}", jsonSystem.has("id") ? jsonSystem.get("id").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_STATUS}", jsonSystem.has("status") ? jsonSystem.get("status").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_TYPE}", jsonSystem.has("type") ? jsonSystem.get("type").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_NAME}", jsonSystem.has("name") ? jsonSystem.get("name").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_PUBLIC}", jsonSystem.has("public") ? jsonSystem.get("public").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_GLOBALDEFAULT}", jsonSystem.has("public") ? jsonSystem.get("public").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_LASTUPDATED}", jsonSystem.has("lastUpdated") ? jsonSystem.get("lastUpdated").asText() : "");
    						
    						JsonNode jsonSystemStorage = jsonSystem.get("storage");
    						body = StringUtils.replace(body, "${SYSTEM_STORAGE_PROTOCOL}", jsonSystemStorage.has("lastUpdated") ? jsonSystem.get("lastUpdated").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_STORAGE_HOST}", jsonSystemStorage.has("host") ? jsonSystem.get("host").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_STORAGE_PORT}", jsonSystemStorage.has("port") ? jsonSystem.get("port").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_STORAGE_RESOURCE}", jsonSystemStorage.has("resource") ? jsonSystem.get("resource").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_STORAGE_ZONE}", jsonSystemStorage.has("zone") ? jsonSystem.get("zone").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_STORAGE_ROOTDIR}", jsonSystemStorage.has("rootDir") ? jsonSystem.get("rootDir").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_STORAGE_HOMEDIR}", jsonSystemStorage.has("homeDir") ? jsonSystem.get("homeDir").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_STORAGE_AUTH_TYPE}", jsonSystemStorage.has("auth") && jsonSystemStorage.get("auth").has("type") ? jsonSystemStorage.get("auth").get("type").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_STORAGE_CONTAINER}", jsonSystemStorage.has("container") ? jsonSystemStorage.get("container").asText() : "");
    						
    						JsonNode jsonSystemLogin = jsonSystem.get("login");
    						body = StringUtils.replace(body, "${SYSTEM_LOGIN_PROTOCOL}", jsonSystemLogin.has("protocol") ? jsonSystemLogin.get("protocol").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_LOGIN_HOST}", jsonSystemLogin.has("host") ? jsonSystemLogin.get("host").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_LOGIN_PORT}", jsonSystemLogin.has("port") ? jsonSystemLogin.get("port").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_LOGIN_AUTH_TYPE}", jsonSystemLogin.has("auth") && jsonSystemLogin.get("auth").has("type") ? jsonSystemLogin.get("auth").get("type").asText() : "");
    					} 
                        else if (json.has("id")) {
                            body = StringUtils.replace(body, "${APP_ID}", json.get("id").textValue());
                            body = StringUtils.replace(body, "${APP_JSON}", json.toString());
                        }
                    }
                    body = StringUtils.replace(body, "${RAW_JSON}", mapper.writer().withDefaultPrettyPrinter().writeValueAsString(getCustomNotificationMessageContextData()));
                } catch (Exception e) {
                    body = StringUtils.replace(body, "${RAW_JSON}", getCustomNotificationMessageContextData());
                }
            }
			
			return body;
		}
		catch (Exception e) {
			log.error("Failed to create notification body", e);
			return "The status of app with uuid " + associatedUuid.toString() +
					" has changed to " + event;
		}
	}
}
