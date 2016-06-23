/**
 * 
 */
package org.iplantc.service.notification.events;

import java.math.BigInteger;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.Notification;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author dooley
 *
 */
public class SystemNotificationEvent extends AbstractEventFilter {

	private static final Logger log = Logger.getLogger(SystemNotificationEvent.class);
	
	/**
	 * @param notification
	 */
	public SystemNotificationEvent(AgaveUUID associatedUuid, Notification notification, String event, String owner)
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
		if (StringUtils.equalsIgnoreCase(event, "created")) {
			body = "System ${SYSTEM_ID} was created. The current system description is:\n\n" +
					"System ID: ${SYSTEM_ID}\n" +
					"Name: ${NAME}\n" +
					"Description: ${STATUS}\n" + 
					"Type: ${TYPE}\n" +
					"Last updated: ${LAST_UPDATED}\n" + 
					"Storage protocol: ${STORAGE_PROTOCOL}\n" +
					"Storage host: ${STORAGE_HOST}\n" +
					"Storage port: ${STORAGE_PORT}\n" +
					"Storage auth type: ${STORAGE_AUTH_TYPE}\n" +
					"Storage resource: ${STORAGE_PORT}\n" +
					"Storage zone: ${STORAGE_PORT}\n" +
					"Storage root dir: ${STORAGE_ROOT_DIR}\n" +
					"Storage home dir: ${STORAGE_HOME_DIR}\n" +
					"Login protocol: ${LOGIN_PROTOCOL}\n" +
					"Login host: ${LOGIN_HOST}\n" +
					"Login port: ${LOGIN_PORT}\n" +
					"Login auth type: ${LOGIN_AUTH_TYPE}\n";
		} else if (StringUtils.equalsIgnoreCase(event, "deleted")) {
			body = "System ${SYSTEM_ID} was deleted. All applications "
			        + "registered to this system will be disabled.\n\n";
		} else if (StringUtils.equalsIgnoreCase(event, "role_update")) {
			body = "System ${SYSTEM_ID} updated a user role.\n\n";
		} else if (StringUtils.equalsIgnoreCase(event, "role_remove")) {
			body = "System ${SYSTEM_ID} removed a user role.\n\n";
		} else if (StringUtils.equalsIgnoreCase(event, "status_update")) {
			body = "System ${SYSTEM_ID} changed status to ${AVAILABLE}.\n\n";
		} else if (StringUtils.equalsIgnoreCase(event, "credential_update")) {
			body = "System ${SYSTEM_ID} recieved a(n) credential update.\n\n";
		} else if (StringUtils.equalsIgnoreCase(event, "app_created")) {
            body = "A new app, ${APP_ID} was registered for execution on ${SYSTEM_ID}. "
                    + "The new app description is given below: \n\n"
                    + "${APP_JSON}\n\n";
		} else if (StringUtils.equalsIgnoreCase(event, "app_updated")) {
            body = "The ${APP_ID} was updated on system ${SYSTEM_ID}. "
                    + "The updated app description is: \n\n "
                    + "${APP_JSON}\n\n" ;
		} else if (StringUtils.equalsIgnoreCase(event, "app_deleted")) {
		    body = "The ${APP_ID} was delted on system ${SYSTEM_ID}. "
		            + "Current jobs using this app will complete. "
		            + "Jobs currently pending and/or in process will fail.";
        } else if (StringUtils.equalsIgnoreCase(event, "status_change")) {
			body = "System ${SYSTEM_ID} is ${STATUS} again at ${LAST_UPDATED}\n\n";
        } else if (StringUtils.startsWithIgnoreCase(event, "job_")) {
            body = "System ${SYSTEM_ID} received a ${EVENT} event.\n\n" + 
                    getCustomNotificationMessageContextData();
		} else {
			body = "System ${SYSTEM_ID} recieved a(n) ${EVENT} event. The current system description is:\n\n" + 
					"System ID: ${SYSTEM_ID}\n" +
					"Name: ${NAME}\n" +
					"Description: ${STATUS}\n" + 
					"Type: ${TYPE}\n" +
					"Last updated: ${LAST_UPDATED}\n" + 
					"Storage protocol: ${STORAGE_PROTOCOL}\n" +
					"Storage host: ${STORAGE_HOST}\n" +
					"Storage port: ${STORAGE_PORT}\n" +
					"Storage auth type: ${STORAGE_AUTH_TYPE}\n" +
					"Storage resource: ${STORAGE_PORT}\n" +
					"Storage zone: ${STORAGE_PORT}\n" +
					"Storage root dir: ${STORAGE_ROOT_DIR}\n" +
					"Storage home dir: ${STORAGE_HOME_DIR}\n" +
					"Login protocol: ${LOGIN_PROTOCOL}\n" +
					"Login host: ${LOGIN_HOST}\n" +
					"Login port: ${LOGIN_PORT}\n" +
					"Login auth type: ${LOGIN_AUTH_TYPE}\n";
		}
		
		return resolveMacros(body, false);	
	}
	
	/* (non-Javadoc)
     * @see org.iplantc.service.notification.events.EventFilter#getHtmlEmailBody()
     */
    @Override
    public String getHtmlEmailBody()
    {
        String body = null;
        if (StringUtils.equalsIgnoreCase(event, "created")) {
            body = "<p>System ${SYSTEM_ID} was created. The current system description is:</p>" +
                    "<br>" +
                    "<p><strong>System ID:</strong> ${SYSTEM_ID}<br>" +
                    "<strong>Name:</strong> ${NAME}<br>" +
                    "<strong>Description:</strong> ${STATUS}<br>" + 
                    "<strong>Type:</strong> ${TYPE}<br>" +
                    "<strong>Last updated:</strong> ${LAST_UPDATED}<br>" + 
                    "<strong>Storage protocol:</strong> ${STORAGE_PROTOCOL}<br>" +
                    "<strong>Storage host:</strong> ${STORAGE_HOST}<br>" +
                    "<strong>Storage port:</strong> ${STORAGE_PORT}<br>" +
                    "<strong>Storage auth type:</strong> ${STORAGE_AUTH_TYPE}<br>" +
                    "<strong>Storage resource:</strong> ${STORAGE_PORT}<br>" +
                    "<strong>Storage zone:</strong> ${STORAGE_PORT}<br>" +
                    "<strong>Storage root dir:</strong> ${STORAGE_ROOT_DIR}<br>" +
                    "<strong>Storage home dir:</strong> ${STORAGE_HOME_DIR}<br>" +
                    "<strong>Login protocol:</strong> ${LOGIN_PROTOCOL}<br>" +
                    "<strong>Login host:</strong> ${LOGIN_HOST}<br>" +
                    "<strong>Login port:</strong> ${LOGIN_PORT}<br>" +
                    "<strong>Login auth type:</strong> ${LOGIN_AUTH_TYPE}</p>";
        } else if (StringUtils.equalsIgnoreCase(event, "deleted")) {
            body = "<p>System ${SYSTEM_ID} was deleted. All applications registered to this system will be disabled.</p>";
        } else if (StringUtils.equalsIgnoreCase(event, "role_update")) {
            body = "<p>System ${SYSTEM_ID} updated a user role.</p>";
        } else if (StringUtils.equalsIgnoreCase(event, "role_remove")) {
            body = "<p>System ${SYSTEM_ID} removed a user role.</p>";
        } else if (StringUtils.equalsIgnoreCase(event, "status_update")) {
            body = "<p>System ${SYSTEM_ID} changed status to ${AVAILABLE}.</p>";
        } else if (StringUtils.equalsIgnoreCase(event, "credential_update")) {
            body = "<p>System ${SYSTEM_ID} recieved a(n) credential update.</p>";
        } else if (StringUtils.equalsIgnoreCase(event, "status_change")) {
            body = "<p>System ${SYSTEM_ID} is ${STATUS} again at ${LAST_UPDATED}</p>";
        } else if (StringUtils.equalsIgnoreCase(event, "app_created")) {
            body = "<p>A new app, ${APP_ID} was registered for execution on ${SYSTEM_ID}. "
                    + "The new app description is given below: </p>"
                    + "<br>"
                    + "<p><pre><code>${APP_JSON}</code></pre></p>";
        } else if (StringUtils.equalsIgnoreCase(event, "app_updated")) {
            body = "<p>The ${APP_ID} was updated on system ${SYSTEM_ID}. "
                    + "The updated app description is: </p>"
                    + "<br>"
                    + "<p><pre><code>${APP_JSON}</code></pre></p>" ;
        } else if (StringUtils.equalsIgnoreCase(event, "app_deleted")) {
            body = "<p>The ${APP_ID} was delted on system ${SYSTEM_ID}. "
                    + "Current jobs using this app will complete. "
                    + "Jobs currently pending and/or in process will fail.</p>";
        } else if (StringUtils.startsWithIgnoreCase(event, "job_")) {
            body = "<p>System ${SYSTEM_ID} received a ${EVENT} event from job ${JOB_ID}.</p><br><pre>" + 
                    getCustomNotificationMessageContextData() + "</pre>";
        } else {
            body = "<p>System ${SYSTEM_ID} recieved a(n) ${EVENT} event. The current system description is:</p><br>" + 
                    "<p><strong>System ID:</strong> ${SYSTEM_ID}<br>" +
                    "<strong>Name:</strong> ${NAME}<br>" +
                    "<strong>Description:</strong> ${STATUS}<br>" + 
                    "<strong>Type:</strong> ${TYPE}<br>" +
                    "<strong>Last updated:</strong> ${LAST_UPDATED}<br>" + 
                    "<strong>Storage protocol:</strong> ${STORAGE_PROTOCOL}<br>" +
                    "<strong>Storage host:</strong> ${STORAGE_HOST}<br>" +
                    "<strong>Storage port:</strong> ${STORAGE_PORT}<br>" +
                    "<strong>Storage auth type:</strong> ${STORAGE_AUTH_TYPE}<br>" +
                    "<strong>Storage resource:</strong> ${STORAGE_PORT}<br>" +
                    "<strong>Storage zone:</strong> ${STORAGE_PORT}<br>" +
                    "<strong>Storage root dir:</strong> ${STORAGE_ROOT_DIR}<br>" +
                    "<strong>Storage home dir:</strong> ${STORAGE_HOME_DIR}<br>" +
                    "<strong>Login protocol:</strong> ${LOGIN_PROTOCOL}<br>" +
                    "<strong>Login host:</strong> ${LOGIN_HOST}<br>" +
                    "<strong>Login port:</strong> ${LOGIN_PORT}<br>" +
                    "<strong>Login auth type:</strong> ${LOGIN_AUTH_TYPE}</p>";
        }
        
        return resolveMacros(body, false);  
    }

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getEmailSubject()
	 */
	@Override
	public String getEmailSubject()
	{
		String subject = null;
		if (StringUtils.equalsIgnoreCase(event, "created")) {
			subject = "System ${SYSTEM_ID} was created";
		} else if (StringUtils.equalsIgnoreCase(event, "deleted")) {
			subject = "System ${SYSTEM_ID} was deleted";
		} else if (StringUtils.equalsIgnoreCase(event, "role_update")) {
			subject = "System ${SYSTEM_ID} updated a user role";
		} else if (StringUtils.equalsIgnoreCase(event, "role_remove")) {
			subject = "System ${SYSTEM_ID} removed a user role";
		} else if (StringUtils.equalsIgnoreCase(event, "status_update")) {
			subject = "System ${SYSTEM_ID} changed status to ${AVAILABLE}";
		} else if (StringUtils.equalsIgnoreCase(event, "credential_update")) {
			subject = "System ${SYSTEM_ID} recieved a(n) credential update";
		} else if (StringUtils.equalsIgnoreCase(event, "status_change")) {
			subject = "System ${SYSTEM_ID} changed status to ${STATUS}";
		
		// app statuses
		} else if (StringUtils.equalsIgnoreCase(event, "app_created")) {
		    subject = "A new app, ${APP_ID} was registered on ${SYSTEM_ID}";
        } else if (StringUtils.equalsIgnoreCase(event, "app_updated")) {
            subject = "App ${APP_ID} was updated on ${SYSTEM_ID}";
        } else if (StringUtils.equalsIgnoreCase(event, "app_deleted")) {
            subject = "App ${APP_ID} was deleted on system ${SYSTEM_ID}.";
        
        } else if (StringUtils.startsWithIgnoreCase(event, "job_")) {
            subject = "System ${SYSTEM_ID} received a ${EVENT} event from ${JOB_ID}.";
        
            
        // catchall
        } else {
			subject = "System ${SYSTEM_ID} recieved a(n) ${EVENT} event";
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
//			Map<String, Object> jobFieldMap = getSystemEntity(associatedUuid.toString());
//			
			body = StringUtils.replace(body, "${UUID}", associatedUuid.toString());
			body = StringUtils.replace(body, "${EVENT}", event);
			body = StringUtils.replace(body, "${OWNER}", owner);
//			body = StringUtils.replace(body, "${SYSTEM_ID}", (String)jobFieldMap.get("system_id"));
//			body = StringUtils.replace(body, "${STATUS}", (String)jobFieldMap.get("status"));
//			body = StringUtils.replace(body, "${TYPE}", (String)jobFieldMap.get("type"));
//			body = StringUtils.replace(body, "${NAME}", (String)jobFieldMap.get("name"));
//			String publiclyAvailable = "false";
//			if (jobFieldMap.get("publicly_available") instanceof Byte) {
//                if ((Byte)jobFieldMap.get("publicly_available") == 1) {
//                    publiclyAvailable = "true";
//                }
//            }
//			else if (jobFieldMap.get("publicly_available") instanceof Boolean) {
//				publiclyAvailable = ((Boolean)jobFieldMap.get("publicly_available")).toString();
//            }
//            else if ((Integer)jobFieldMap.get("publicly_available") == 1) {
//                publiclyAvailable = "true";
//            }
//			body = StringUtils.replace(body, "${PUBLIC}", publiclyAvailable);
//			
//			String globalDefault = "false";
//			if (jobFieldMap.get("global_default") instanceof Byte) {
//                if ((Byte)jobFieldMap.get("global_default") == 1) {
//                	globalDefault = "true";
//                }
//            }
//			else if (jobFieldMap.get("global_default") instanceof Boolean) {
//				globalDefault = ((Boolean)jobFieldMap.get("global_default")).toString();
//            }
//            else if ((Integer)jobFieldMap.get("global_default") == 1) {
//            	globalDefault = "true";
//            }
//			body = StringUtils.replace(body, "${GLOBAL_DEFAULT}", globalDefault);
//			
//			body = StringUtils.replace(body, "${LAST_UPDATED}", new DateTime(jobFieldMap.get("last_updated")).toString());
//			body = StringUtils.replace(body, "${STORAGE_PROTOCOL}", (String)jobFieldMap.get("protocol"));
//			body = StringUtils.replace(body, "${STORAGE_HOST}", (String)jobFieldMap.get("host"));
//			body = StringUtils.replace(body, "	", jobFieldMap.get("port") == null ? "" : ((Integer)jobFieldMap.get("port")).toString());
//			body = StringUtils.replace(body, "${STORAGE_RESOURCE}", (String)jobFieldMap.get("resource"));
//			body = StringUtils.replace(body, "${STORAGE_ZONE}", (String)jobFieldMap.get("zone"));
//			body = StringUtils.replace(body, "${STORAGE_ROOT_DIR}", (String)jobFieldMap.get("root_dir"));
//			body = StringUtils.replace(body, "${STORAGE_HOME_DIR}", (String)jobFieldMap.get("home_dir"));
//			body = StringUtils.replace(body, "${STORAGE_AUTH_TYPE}", (String)jobFieldMap.get("login_credential_type"));
//			body = StringUtils.replace(body, "${STORAGE_CONTAINER}", (String)jobFieldMap.get("container"));
//			
//			body = StringUtils.replace(body, "${LOGIN_PROTOCOL}", (String)jobFieldMap.get("rlc.protocol"));
//			body = StringUtils.replace(body, "${LOGIN_HOST}", (String)jobFieldMap.get("rlc.host"));
//			body = StringUtils.replace(body, "${LOGIN_PORT}", ((Integer)jobFieldMap.get("rlc.port")).toString());
//			body = StringUtils.replace(body, "${LOGIN_AUTH_TYPE}", (String)jobFieldMap.get("rlc.login_credential_type"));
			
			if (StringUtils.isNotEmpty(getCustomNotificationMessageContextData())) {
			    ObjectMapper mapper = new ObjectMapper();
			    try {
			        JsonNode json = mapper.readTree(getCustomNotificationMessageContextData());
    			    if (json.isObject()) {
    			    	if (json.hasNonNull("system")) {
    			    		JsonNode jsonSystem = json.get("system");
    			    		body = StringUtils.replace(body, "${SYSTEM_ID}", jsonSystem.hasNonNull("id") ? jsonSystem.get("id").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_STATUS}", jsonSystem.hasNonNull("status") ? jsonSystem.get("status").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_TYPE}", jsonSystem.hasNonNull("type") ? jsonSystem.get("type").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_NAME}", jsonSystem.hasNonNull("name") ? jsonSystem.get("name").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_PUBLIC}", jsonSystem.hasNonNull("public") ? jsonSystem.get("public").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_GLOBALDEFAULT}", jsonSystem.hasNonNull("public") ? jsonSystem.get("public").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_LASTUPDATED}", jsonSystem.hasNonNull("lastUpdated") ? jsonSystem.get("lastUpdated").asText() : "");
    						
    						JsonNode jsonSystemStorage = jsonSystem.get("storage");
    						body = StringUtils.replace(body, "${SYSTEM_STORAGE_PROTOCOL}", jsonSystemStorage.hasNonNull("protocol") ? jsonSystemStorage.get("protocol").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_STORAGE_HOST}", jsonSystemStorage.hasNonNull("host") ? jsonSystemStorage.get("host").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_STORAGE_PORT}", jsonSystemStorage.hasNonNull("port") ? jsonSystemStorage.get("port").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_STORAGE_RESOURCE}", jsonSystemStorage.hasNonNull("resource") ? jsonSystemStorage.get("resource").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_STORAGE_ZONE}", jsonSystemStorage.hasNonNull("zone") ? jsonSystemStorage.get("zone").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_STORAGE_ROOTDIR}", jsonSystemStorage.hasNonNull("rootDir") ? jsonSystemStorage.get("rootDir").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_STORAGE_HOMEDIR}", jsonSystemStorage.hasNonNull("homeDir") ? jsonSystemStorage.get("homeDir").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_STORAGE_AUTH_TYPE}", jsonSystemStorage.hasNonNull("auth") && jsonSystemStorage.get("auth").hasNonNull("type") ? jsonSystemStorage.get("auth").get("type").asText() : "");
    						body = StringUtils.replace(body, "${SYSTEM_STORAGE_CONTAINER}", jsonSystemStorage.hasNonNull("container") ? jsonSystemStorage.get("container").asText() : "");
    						
    						if (jsonSystem.hasNonNull("login")) {
    							JsonNode jsonSystemLogin = jsonSystem.get("login");
	    						body = StringUtils.replace(body, "${SYSTEM_LOGIN_PROTOCOL}", jsonSystemLogin.hasNonNull("protocol") ? jsonSystemLogin.get("protocol").asText() : "");
	    						body = StringUtils.replace(body, "${SYSTEM_LOGIN_HOST}", jsonSystemLogin.hasNonNull("host") ? jsonSystemLogin.get("host").asText() : "");
	    						body = StringUtils.replace(body, "${SYSTEM_LOGIN_PORT}", jsonSystemLogin.hasNonNull("port") ? jsonSystemLogin.get("port").asText() : "");
	    						body = StringUtils.replace(body, "${SYSTEM_LOGIN_AUTH_TYPE}", jsonSystemLogin.hasNonNull("auth") && jsonSystemLogin.get("auth").hasNonNull("type") ? jsonSystemLogin.get("auth").get("type").asText() : "");
    						}
    					}
    			    	if (json.hasNonNull("job") ) {
                        	JsonNode jsonJob = json.get("job");
                            body = StringUtils.replace(body, "${JOB_APPID}", jsonJob.hasNonNull("appId") ? jsonJob.get("appId").asText() : "");
                            body = StringUtils.replace(body, "${JOB_ID}", jsonJob.hasNonNull("id") ? jsonJob.get("id").asText() : "");
                            body = StringUtils.replace(body, "${JOB_SYSTEM}", jsonJob.hasNonNull("executionSystem") ? jsonJob.get("executionSystem").asText() : "");
                            body = StringUtils.replace(body, "${JOB_STATUS}", jsonJob.hasNonNull("status") ? jsonJob.get("status").asText() : "");
                            body = StringUtils.replace(body, "${JOB_NAME}", jsonJob.hasNonNull("name") ? jsonJob.get("name").asText() : "");
                            body = StringUtils.replace(body, "${JOB_OWNER}", jsonJob.hasNonNull("owner") ? jsonJob.get("owner").asText() : "");
                            body = StringUtils.replace(body, "${JOB_URL}", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE) + jsonJob.get("id").asText());
                            body = StringUtils.replace(body, "${JOB_SUBMITTIME}", jsonJob.hasNonNull("submitTime") ? new DateTime(jsonJob.get("submitTime").asText()).toString(): "");
                            body = StringUtils.replace(body, "${JOB_STARTTIME}", jsonJob.hasNonNull("startTime") ? new DateTime(jsonJob.get("startTime").asText()).toString(): "");
                            body = StringUtils.replace(body, "${JOB_ENDTIME}", jsonJob.hasNonNull("endTime") ? new DateTime(jsonJob.get("endTime").asText()).toString(): "");
                            body = StringUtils.replace(body, "${JOB_ARCHIVEURL}", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE) + jsonJob.get("id").asText() + "/outputs/listings");
                            body = StringUtils.replace(body, "${JOB_ARCHIVEPATH}", jsonJob.hasNonNull("appId") ? jsonJob.get("appId").asText() : "");
                            body = StringUtils.replace(body, "${JOB_URL}", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE) + jsonJob.get("id").asText());
                            body = StringUtils.replace(body, "${JOB_JSON}", jsonJob.get("job").toString());
                            
    			    	}
    			    	if (json.hasNonNull("software") || json.hasNonNull("app") ) {
                        	JsonNode jsonSoftware = json.hasNonNull("software") ? json.get("software") : json.get("app");
                            body = StringUtils.replace(body, "${APP_ID}", jsonSoftware.hasNonNull("id") ? jsonSoftware.get("id").asText() : "");
                            body = StringUtils.replace(body, "${APP_NAME}", jsonSoftware.hasNonNull("name") ? jsonSoftware.get("name").asText() : "");
                            body = StringUtils.replace(body, "${APP_VERSION}", jsonSoftware.hasNonNull("version") ? jsonSoftware.get("version").asText() : "");
                            body = StringUtils.replace(body, "${APP_OWNER}", jsonSoftware.hasNonNull("owner") ? jsonSoftware.get("owner").asText() : "");
                            body = StringUtils.replace(body, "${APP_SHORTDESCRIPTION}", jsonSoftware.hasNonNull("shortDescription") ? jsonSoftware.get("shortDescription").asText() : "");
                            body = StringUtils.replace(body, "${APP_UUID}", jsonSoftware.hasNonNull("uuid") ? jsonSoftware.get("uuid").asText() : "");
                            body = StringUtils.replace(body, "${APP_IS_PUBLIC}", jsonSoftware.hasNonNull("isPublic") ? jsonSoftware.get("isPublic").asText() : "");
                            body = StringUtils.replace(body, "${APP_LABEL}", jsonSoftware.hasNonNull("label") ? jsonSoftware.get("label").asText() : "");
                            body = StringUtils.replace(body, "${APP_LONG_DESCRIPTION}", jsonSoftware.hasNonNull("longDescription") ? jsonSoftware.get("longDescription").asText() : "");
                            body = StringUtils.replace(body, "${APP_AVAILABLE}", jsonSoftware.hasNonNull("available") ? jsonSoftware.get("available").asText() : "");
                            body = StringUtils.replace(body, "${APP_CHECKPOINTABLE}", jsonSoftware.hasNonNull("checkpointable") ? jsonSoftware.get("checkpointable").asText() : "");
                            body = StringUtils.replace(body, "${APP_DEFAULTMAXRUNTIME}", jsonSoftware.hasNonNull("defaultMaxRunTime") ? jsonSoftware.get("defaultMaxRunTime").asText() : "");
                            body = StringUtils.replace(body, "${APP_DEFAULTMEMORYPERNODE}", jsonSoftware.hasNonNull("defaultMemoryPerNode") ? jsonSoftware.get("defaultMemoryPerNode").asText() : "");
                            body = StringUtils.replace(body, "${APP_DEFAULTPROCESSORSPERNODE}", jsonSoftware.hasNonNull("defaultProcessorsPerNode") ? jsonSoftware.get("defaultProcessorsPerNode").asText() : "");
                            body = StringUtils.replace(body, "${APP_DEFAULTNODECOUNT}", jsonSoftware.hasNonNull("defaultNodeCount") ? jsonSoftware.get("defaultNodeCount").asText() : "");
                            body = StringUtils.replace(body, "${APP_DEFAULTQUEUE}", jsonSoftware.hasNonNull("defaultQueue") ? jsonSoftware.get("defaultQueue").asText() : "");
                            body = StringUtils.replace(body, "${APP_DEPLOYMENTPATH}", jsonSoftware.hasNonNull("deploymentPath") ? jsonSoftware.get("deploymentPath").asText() : "");
                            body = StringUtils.replace(body, "${APP_DEPLOYMENTSYSTEM}", jsonSoftware.hasNonNull("deploymentSystem") ? jsonSoftware.get("deploymentSystem").asText() : "");
                            body = StringUtils.replace(body, "${APP_HELPURI}", jsonSoftware.hasNonNull("helpURI") ? jsonSoftware.get("helpURI").asText() : "");
                            body = StringUtils.replace(body, "${APP_URL}", TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_APP_SERVICE) + jsonSoftware.get("id").asText());
                            body = StringUtils.replace(body, "${APP_JSON}", jsonSoftware.get("software").toString());    
                        }
    			    	if (json.hasNonNull("file") ) {
    			    		JsonNode jsonEntity = json.get("file");
                            body = StringUtils.replace(body, "${FILE_UUID}", jsonEntity.hasNonNull("id") ? jsonEntity.get("id").asText() : "");
                            body = StringUtils.replace(body, "${FILE_NAME}", jsonEntity.hasNonNull("name") ? jsonEntity.get("name").asText() : "");
                            body = StringUtils.replace(body, "${FILE_OWNER}", jsonEntity.hasNonNull("owner") ? jsonEntity.get("owner").asText() : "");
                            body = StringUtils.replace(body, "${FILE_INTERNALUSERNAME}", jsonEntity.hasNonNull("internalUsername") ? jsonEntity.get("internalUsername").asText() : "");
                            body = StringUtils.replace(body, "${FILE_LASTMODIFIED}", jsonEntity.hasNonNull("lastModified") ? jsonEntity.get("lastModified").asText() : "");
                            body = StringUtils.replace(body, "${FILE_PATH}", jsonEntity.hasNonNull("path") ? jsonEntity.get("path").asText() : "");
                            body = StringUtils.replace(body, "${FILE_STATUS}", jsonEntity.hasNonNull("status") ? jsonEntity.get("status").asText() : "");
                            body = StringUtils.replace(body, "${FILE_SYSTEMID}", jsonEntity.hasNonNull("systemId") ? jsonEntity.get("systemId").asText() : "");
                            body = StringUtils.replace(body, "${FILE_NATIVEFORMAT}", jsonEntity.hasNonNull("nativeFormat") ? jsonEntity.get("nativeFormat").asText() : "");
                            body = StringUtils.replace(body, "${FILE_PERMISSIONS}", jsonEntity.hasNonNull("permissions") ? jsonEntity.get("permissions").asText() : "");
                            body = StringUtils.replace(body, "${FILE_LENGTH}", jsonEntity.hasNonNull("length") ? jsonEntity.get("length").asText() : "");
                            body = StringUtils.replace(body, "${FILE_URL}", jsonEntity.hasNonNull("_links") ? jsonEntity.get("_links").get("self").get("href").asText() : "");
                            body = StringUtils.replace(body, "${FILE_MIMETYPE}", jsonEntity.hasNonNull("mimeType") ? jsonEntity.get("mimeType").asText(): "");
                            body = StringUtils.replace(body, "${FILE_TYPE}", jsonEntity.hasNonNull("type") ? jsonEntity.get("type").asText(): "");
                            body = StringUtils.replace(body, "${FILE_JSON}", jsonEntity.toString());
                        } 
    			    	if (json.hasNonNull("permission") ) {
                        	JsonNode jsonPermission = json.get("permission");
                            body = StringUtils.replace(body, "${PERMISSION_ID}", jsonPermission.hasNonNull("id") ? jsonPermission.get("id").asText() : "");
                            body = StringUtils.replace(body, "${PERMISSION_PERMISSION}", jsonPermission.hasNonNull("permission") ? jsonPermission.get("permission").asText() : "");
                            body = StringUtils.replace(body, "${PERMISSION_USERNAME}", jsonPermission.hasNonNull("username") ? jsonPermission.get("username").asText() : "");
                            body = StringUtils.replace(body, "${PERMISSION_LASTUPDATED}", jsonPermission.hasNonNull("lastUpdated") ? jsonPermission.get("lastUpdated").asText() : "");
                            body = StringUtils.replace(body, "${PERMISSION_JSON}", jsonPermission.toString());
                        } 
    			    	if (json.hasNonNull("role") ) {
                        	JsonNode jsonRole = json.get("role");
                            body = StringUtils.replace(body, "${ROLE_ID}", jsonRole.hasNonNull("id") ? jsonRole.get("id").asText() : "");
                            body = StringUtils.replace(body, "${ROLE_ROLE}", jsonRole.hasNonNull("role") ? jsonRole.get("role").asText() : "");
                            body = StringUtils.replace(body, "${ROLE_USERNAME}", jsonRole.hasNonNull("username") ? jsonRole.get("username").asText() : "");
                            body = StringUtils.replace(body, "${ROLE_LASTUPDATED}", jsonRole.hasNonNull("lastUpdated") ? jsonRole.get("lastUpdated").asText() : "");
                            body = StringUtils.replace(body, "${ROLE_JSON}", jsonRole.toString());
                        } 
    			    	if (json.hasNonNull("id")) {
                        	JsonNode jsonSystem = json;
                        	body = StringUtils.replace(body, "${SYSTEM_ID}", jsonSystem.hasNonNull("id") ? jsonSystem.get("id").asText() : "");
    						body = StringUtils.replace(body, "${STATUS}", jsonSystem.hasNonNull("status") ? jsonSystem.get("status").asText() : "");
    						body = StringUtils.replace(body, "${TYPE}", jsonSystem.hasNonNull("type") ? jsonSystem.get("type").asText() : "");
    						body = StringUtils.replace(body, "${NAME}", jsonSystem.hasNonNull("name") ? jsonSystem.get("name").asText() : "");
    						body = StringUtils.replace(body, "${PUBLIC}", jsonSystem.hasNonNull("public") ? jsonSystem.get("public").asText() : "");
    						body = StringUtils.replace(body, "${GLOBAL_DEFAULT}", jsonSystem.hasNonNull("public") ? jsonSystem.get("public").asText() : "");
    						body = StringUtils.replace(body, "${LAST_UPDATED}", jsonSystem.hasNonNull("lastUpdated") ? jsonSystem.get("lastUpdated").asText() : "");
    						
    						JsonNode jsonSystemStorage = jsonSystem.get("storage");
    						body = StringUtils.replace(body, "${STORAGE_PROTOCOL}", jsonSystemStorage.hasNonNull("lastUpdated") ? jsonSystemStorage.get("lastUpdated").asText() : "");
    						body = StringUtils.replace(body, "${STORAGE_HOST}", jsonSystemStorage.hasNonNull("host") ? jsonSystemStorage.get("host").asText() : "");
    						body = StringUtils.replace(body, "${STORAGE_PORT}", jsonSystemStorage.hasNonNull("port") ? jsonSystemStorage.get("port").asText() : "");
    						body = StringUtils.replace(body, "${STORAGE_RESOURCE}", jsonSystemStorage.hasNonNull("resource") ? jsonSystemStorage.get("resource").asText() : "");
    						body = StringUtils.replace(body, "${STORAGE_ZONE}", jsonSystemStorage.hasNonNull("zone") ? jsonSystemStorage.get("zone").asText() : "");
    						body = StringUtils.replace(body, "${STORAGE_ROOTDIR}", jsonSystemStorage.hasNonNull("rootDir") ? jsonSystemStorage.get("rootDir").asText() : "");
    						body = StringUtils.replace(body, "${STORAGE_ROOT_DIR}", jsonSystemStorage.hasNonNull("rootDir") ? jsonSystemStorage.get("rootDir").asText() : "");
    						body = StringUtils.replace(body, "${STORAGE_HOMEDIR}", jsonSystemStorage.hasNonNull("homeDir") ? jsonSystemStorage.get("homeDir").asText() : "");
    						body = StringUtils.replace(body, "${STORAGE_HOME_DIR}", jsonSystemStorage.hasNonNull("homeDir") ? jsonSystemStorage.get("homeDir").asText() : "");
    						body = StringUtils.replace(body, "${STORAGE_AUTHTYPE}", jsonSystemStorage.hasNonNull("auth") && jsonSystemStorage.get("auth").hasNonNull("type") ? jsonSystemStorage.get("auth").get("type").asText() : "");
    						body = StringUtils.replace(body, "${STORAGE_AUTH_TYPE}", jsonSystemStorage.hasNonNull("auth") && jsonSystemStorage.get("auth").hasNonNull("type") ? jsonSystemStorage.get("auth").get("type").asText() : "");
    						body = StringUtils.replace(body, "${STORAGE_CONTAINER}", jsonSystemStorage.hasNonNull("container") ? jsonSystemStorage.get("container").asText() : "");
    						
    						if (jsonSystem.has("login")) {
	    						JsonNode jsonSystemLogin = jsonSystem.get("login");
	    						body = StringUtils.replace(body, "${LOGIN_PROTOCOL}", jsonSystemLogin.hasNonNull("protocol") ? jsonSystemLogin.get("protocol").asText() : "");
	    						body = StringUtils.replace(body, "${LOGIN_HOST}", jsonSystemLogin.hasNonNull("host") ? jsonSystemLogin.get("host").asText() : "");
	    						body = StringUtils.replace(body, "${LOGIN_PORT}", jsonSystemLogin.hasNonNull("port") ? jsonSystemLogin.get("port").asText() : "");
	    						body = StringUtils.replace(body, "${LOGIN_AUTHTYPE}", jsonSystemLogin.hasNonNull("auth") && jsonSystemLogin.get("auth").hasNonNull("type") ? jsonSystemLogin.get("auth").get("type").asText() : "");
	    						body = StringUtils.replace(body, "${LOGIN_AUTH_TYPE}", jsonSystemLogin.hasNonNull("auth") && jsonSystemLogin.get("auth").hasNonNull("type") ? jsonSystemLogin.get("auth").get("type").asText() : "");
    						}
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
			return "The status of system with uuid " + associatedUuid.toString() +
					" has changed to " + event;
		}
	}

	@SuppressWarnings("unchecked")
	protected Map<String, Object> getSystemEntity(String uuid) throws NotificationException 
	{
		try 
        {
        	HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
            session.clear();
            
            String sql = "SELECT s.*, storage.*, execution.*, rsc.* \n"
                    + "FROM systems s \n"
                    + "     left join storagesystems storage on s.id = storage.id \n"
                    + "     left join executionsystems execution on s.id = execution.id \n"
                    + "     left join (\n"
                    + "         SELECT rc.id, rc.host, rc.port, sc.protocol, sc.resource, sc.zone, sc.container, sc.root_dir, sc.home_dir, ac.login_credential_type \n"
                    + "         FROM remoteconfigs rc \n"
                    + "              left join storageconfigs sc on rc.id = sc.id \n"
                    + "              left join authconfigs ac on rc.id = ac.remote_config_id \n"
                    + "         WHERE ac.system_default = 1 \n"
                    + "     ) as rsc on rsc.id = s.id  \n"
                    + "WHERE s.uuid = :uuid";
//            log.debug(sql);
            
            Map<String, Object> systemRow = (Map<String, Object>)session
            		.createSQLQuery(sql)
            		.setString("uuid", uuid.toString())
            		.setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP)
            		.uniqueResult();
//            
//            Map<String, Object> storageConfigRow = (Map<String, Object>)session
//            		.createSQLQuery("SELECT * from remoteconfigs rc left join storageconfigs sc on rc.id = sc.id, authconfigs ac where rc.id = :storageconfigid and ac.remote_config_id = rc.id and ac.system_default = 1")
//            		.setBigInteger("storageconfigid", (BigInteger)systemRow.get("storage_config"))
//            		.setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP)
//            		.uniqueResult();
//            
//            for (String col: storageConfigRow.keySet()) {
//	       		 systemRow.put("st." + col, storageConfigRow.get(col));
//	       	}
//          
            session.flush();
            
            if (systemRow.isEmpty()) {
                throw new UUIDException("No such uuid present");
            } else if (StringUtils.equalsIgnoreCase((String)systemRow.get("type"), "EXECUTION")) {
                Map<String, Object> executionConfigRow = (Map<String, Object>)session
                       .createSQLQuery("SELECT * from remoteconfigs rc left join loginconfigs sc on rc.id = sc.id, authconfigs ac where rc.id = :loginconfigid and ac.remote_config_id = rc.id and ac.system_default = 1")
                       .setBigInteger("loginconfigid", (BigInteger)systemRow.get("login_config"))
                       .setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP)
                       .uniqueResult();
                
                for (String col: executionConfigRow.keySet()) {
                    systemRow.put("rlc." + col, executionConfigRow.get(col));
                }
            }
            
            return systemRow;
        }
        catch (Throwable ex) 
        {
			throw new NotificationException(ex);
		}  
        finally 
        {
        	try { HibernateUtil.commitTransaction();} catch (Exception e) {}
        }
	}
}
