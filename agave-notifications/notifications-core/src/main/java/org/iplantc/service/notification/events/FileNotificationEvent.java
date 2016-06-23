/**
 * 
 */
package org.iplantc.service.notification.events;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.notification.exceptions.NotificationException;
import org.iplantc.service.notification.model.Notification;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author dooley
 *
 */
public class FileNotificationEvent extends AbstractEventFilter {

	private static final Logger log = Logger.getLogger(FileNotificationEvent.class);
	
	/**
	 * @param notification
	 */
	public FileNotificationEvent(AgaveUUID associatedUuid, Notification notification, String event, String owner)
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
		if (StringUtils.equalsIgnoreCase(event, "upload")) {
			body = "${TYPE} ${PATH} on ${SYSTEM} was successfully uploaded.";
		} else if (StringUtils.equalsIgnoreCase(event, "deleted")) {
			body = "${TYPE} ${PATH} was deleted. It is no longer available for execution. " +
					"Running jobs will complete as before. Any new or pending jobs " +
					"utilizing this app will fail.";
		} else if (StringUtils.equalsIgnoreCase(event, "rename")) {
			body = "${TYPE} ${PATH} on ${SYSTEM} was disabled. It is no longer available for execution.";
		} else if (StringUtils.equalsIgnoreCase(event, "move")) {
			body = "${TYPE} ${PATH} on ${SYSTEM} was disabled. It is no longer available for execution.";
		} else if (StringUtils.equalsIgnoreCase(event, "copy")) {
			body = "${TYPE} ${PATH} on ${SYSTEM} was disabled. It is no longer available for execution.";
		} else if (StringUtils.equalsIgnoreCase(event, "mkdir")) {
			body = "${TYPE} ${PATH} on ${SYSTEM} was created.";
		} else if (StringUtils.equalsIgnoreCase(event, "updated")) {
			body = "${TYPE} ${PATH} on ${SYSTEM} was udpated. The new app description is available " +
					"from the apps service.";
		} else if (StringUtils.equalsIgnoreCase(event, "pem_add")) {
			body = "${TYPE} ${PATH} on ${SYSTEM} added a user permission. Please verify the user has " + 
					"the proper execution role on the execution system if they intend on " +
					"running jobs using this app.";
		} else if (StringUtils.equalsIgnoreCase(event, "pem_delete")) {
			body = "${TYPE} ${PATH} on ${SYSTEM} removed a user permission. Any currently running jobs will " +
					"complete as before. Any new or pending jobs utilizing this app will fail " +
					"for the revoked user.";
		} else if (StringUtils.equalsIgnoreCase(event, "download")) {
			body = "${TYPE} ${PATH} on ${SYSTEM} was downloaded by ${OWNER}.";
		} else if (StringUtils.equalsIgnoreCase(event, "upload")) {
			body = "A new file/folder was uploaded to ${PATH} on ${SYSTEM} by ${OWNER}.";
		} else if (StringUtils.equalsIgnoreCase(event, "TRANSFORMING")) { 
		    body = "A file/folder at ${PATH} on ${SYSTEM} has begun transformation to ${FORMAT}.";
		} else if (StringUtils.equalsIgnoreCase(event, "TRANSFORMING_FAILED")) { 
		    body = "Transformation of ${PATH} on ${SYSTEM} to ${FORMAT} format failed."; 
		} else if (StringUtils.equalsIgnoreCase(event, "TRANSFORMING_COMPLETED")) { 
		    body = "Transformation of ${PATH} on ${SYSTEM} to ${FORMAT} format completed successfully."; 
		} else if (StringUtils.equalsIgnoreCase(event, "PREPROCESSING")) { 
		    body = "Preprocessing has begun for ${PATH} on ${SYSTEM}";
		} else if (StringUtils.equalsIgnoreCase(event, "TRANSFORMING_QUEUED")) {
		    body = "${PATH} on ${SYSTEM} has been successfully queued for transformation to ${FORMAT} format.";
		} else if (StringUtils.equalsIgnoreCase(event, "STAGING")) { 
            body = "A file/folder at ${PATH} on ${SYSTEM} has begun staging.";
        } else if (StringUtils.equalsIgnoreCase(event, "STAGING_FAILED")) { 
            body = "Failed to stage ${PATH} to ${SYSTEM}."; 
        } else if (StringUtils.equalsIgnoreCase(event, "STAGING_COMPLETED")) { 
            body = "Staging of ${PATH} to ${SYSTEM} completed successfully."; 
        } else if (StringUtils.equalsIgnoreCase(event, "STAGING_QUEUED")) {
            body = "The staging of ${PATH} on ${SYSTEM} has been successfully queued.";
        } else if (StringUtils.equalsIgnoreCase(event, "CONTENT_CHANGE")) {
            body = "One or more files or folders has bee updated in ${PATH} on ${SYSTEM} by ${OWNER}.";
        } else {
			body = "${TYPE} ${PATH} recieved a(n) ${EVENT} event";
		}
		
		return resolveMacros(body, false);
	}
	
	@Override
    public String getHtmlEmailBody()
    {
        return "<p>" + getEmailBody() + "</p>";
    }

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.NotificationEvent#getEmailSubject()
	 */
	@Override
	public String getEmailSubject()
	{
		String subject = null;
		if (StringUtils.equalsIgnoreCase(event, "upload")) {
			subject = "${TYPE} ${PATH} on ${SYSTEM} was successfully uploaded";
		} else if (StringUtils.equalsIgnoreCase(event, "deleted")) {
			subject = "${TYPE} ${PATH} on ${SYSTEM} was deleted";
		} else if (StringUtils.equalsIgnoreCase(event, "rename")) {
			subject = "${TYPE} ${PATH} on ${SYSTEM} was renamed";
		} else if (StringUtils.equalsIgnoreCase(event, "move")) {
			subject = "${TYPE} ${PATH} on ${SYSTEM} was move";
		} else if (StringUtils.equalsIgnoreCase(event, "copy")) {
			subject = "${TYPE} ${PATH} on ${SYSTEM} was copied";
		} else if (StringUtils.equalsIgnoreCase(event, "mkdir")) {
			subject = "${TYPE} ${PATH} on ${SYSTEM} was created.";
		} else if (StringUtils.equalsIgnoreCase(event, "updated")) {
			subject = "${TYPE} ${PATH} on ${SYSTEM} was udpated";
		} else if (StringUtils.equalsIgnoreCase(event, "download")) {
			subject = "${TYPE} ${PATH} on ${SYSTEM} was downloaded";
		} else if (StringUtils.equalsIgnoreCase(event, "upload")) {
			subject = "A file/folder was uploaded ${PATH} on ${SYSTEM}";
		} else if (StringUtils.equalsIgnoreCase(event, "pem_add")) {
			subject = "${TYPE} ${PATH} on ${SYSTEM} added a user permission";
		} else if (StringUtils.equalsIgnoreCase(event, "pem_delete")) {
			subject = "${TYPE} ${PATH} on ${SYSTEM} removed a user permission";
		} else {
			subject = "${TYPE} ${PATH} recieved a(n) ${EVENT} event";
		}
		
		return resolveMacros(subject, false);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.AbstractNotificationEvent#resolveMacros(java.lang.String, boolean)
	 */
	@Override
	public String resolveMacros(String body, boolean urlEncode)
	{
		body = StringUtils.replace(body, "${UUID}", this.associatedUuid.toString());
		body = StringUtils.replace(body, "${EVENT}", this.event);
		body = StringUtils.replace(body, "${OWNER}", this.owner);
		
		if (StringUtils.isNotEmpty(getCustomNotificationMessageContextData())) {
		    ObjectMapper mapper = new ObjectMapper();
		    try {
		        JsonNode json = mapper.readTree(getCustomNotificationMessageContextData());
			    if (json.isObject()) {
			    	if (json.has("file") ) {
			    		JsonNode jsonEntity = json.get("file");
                        body = StringUtils.replace(body, "${FILE_UUID}", jsonEntity.has("id") ? jsonEntity.get("id").asText() : "");
                        body = StringUtils.replace(body, "${FILE_NAME}", jsonEntity.has("name") ? jsonEntity.get("name").asText() : "");
                        body = StringUtils.replace(body, "${FILE_OWNER}", jsonEntity.has("owner") ? jsonEntity.get("owner").asText() : "");
                        body = StringUtils.replace(body, "${FILE_INTERNALUSERNAME}", jsonEntity.has("internalUsername") ? jsonEntity.get("internalUsername").asText() : "");
                        body = StringUtils.replace(body, "${FILE_LASTMODIFIED}", jsonEntity.has("lastModified") ? jsonEntity.get("lastModified").asText() : "");
                        body = StringUtils.replace(body, "${FILE_PATH}", jsonEntity.has("path") ? jsonEntity.get("path").asText() : "");
                        body = StringUtils.replace(body, "${FILE_STATUS}", jsonEntity.has("status") ? jsonEntity.get("status").asText() : "");
                        body = StringUtils.replace(body, "${FILE_SYSTEMID}", jsonEntity.has("systemId") ? jsonEntity.get("systemId").asText() : "");
                        body = StringUtils.replace(body, "${FILE_NATIVEFORMAT}", jsonEntity.has("nativeFormat") ? jsonEntity.get("nativeFormat").asText() : "");
                        body = StringUtils.replace(body, "${FILE_PERMISSIONS}", jsonEntity.has("permissions") ? jsonEntity.get("permissions").asText() : "");
                        body = StringUtils.replace(body, "${FILE_LENGTH}", jsonEntity.has("length") ? jsonEntity.get("length").asText() : "");
                        body = StringUtils.replace(body, "${FILE_URL}", jsonEntity.has("_links") ? jsonEntity.get("_links").get("href").get("self").asText() : "");
                        body = StringUtils.replace(body, "${FILE_MIMETYPE}", jsonEntity.has("mimeType") ? jsonEntity.get("mimeType").asText(): "");
                        body = StringUtils.replace(body, "${FILE_TYPE}", jsonEntity.has("type") ? jsonEntity.get("type").asText(): "");
                        body = StringUtils.replace(body, "${FILE_JSON}", jsonEntity.toString());
                    } else if (json.has("permission") ) {
                    	JsonNode jsonPermission = json.get("permission");
                        body = StringUtils.replace(body, "${PERMISSION_ID}", jsonPermission.has("id") ? jsonPermission.get("id").asText() : "");
                        body = StringUtils.replace(body, "${PERMISSION_PERMISSION}", jsonPermission.has("permission") ? jsonPermission.get("permission").asText() : "");
                        body = StringUtils.replace(body, "${PERMISSION_USERNAME}", jsonPermission.has("username") ? jsonPermission.get("username").asText() : "");
                        body = StringUtils.replace(body, "${PERMISSION_LASTUPDATED}", jsonPermission.has("lastUpdated") ? jsonPermission.get("lastUpdated").asText() : "");
                        body = StringUtils.replace(body, "${PERMISSION_JSON}", jsonPermission.toString());
                    } else if (json.has("id")) {
                    	body = StringUtils.replace(body, "${FILE_ID}", json.has("id") ? json.get("id").asText() : "");
                        body = StringUtils.replace(body, "${FILE_NAME}", json.has("name") ? json.get("name").asText() : "");
                        body = StringUtils.replace(body, "${FILE_OWNER}", json.has("owner") ? json.get("owner").asText() : "");
                        body = StringUtils.replace(body, "${FILE_URL}", json.has("_links") ? json.get("_links").get("href").get("self").asText() : "");
                        body = StringUtils.replace(body, "${FILE_ASSOCIATEDIDS}", json.has("associatedIds") ? new DateTime(json.get("associatedIds").asText()).toString(): "");
                        body = StringUtils.replace(body, "${FILE_JSON}", json.toString());
			        }
			    }
			    body = StringUtils.replace(body, "${RAW_JSON}", mapper.writer().withDefaultPrettyPrinter().writeValueAsString(getCustomNotificationMessageContextData()));
		    } catch (Exception e) {
		        body = StringUtils.replace(body, "${RAW_JSON}", getCustomNotificationMessageContextData());
		    }
		}
		
		try 
		{
			Map<String, Object> jobFieldMap = getJobRow(associatedUuid.toString());
			body = StringUtils.replace(body, "${SOURCE}", (String)jobFieldMap.get("source"));
			body = StringUtils.replace(body, "${NAME}", (String)jobFieldMap.get("name"));
			body = StringUtils.replace(body, "${TYPE}", "");
			body = StringUtils.replace(body, "${FORMAT}", (String)jobFieldMap.get("native_format"));
			body = StringUtils.replace(body, "${PATH}", (String)jobFieldMap.get("absolutepath"));
			body = StringUtils.replace(body, "${SYSTEM}", (String)jobFieldMap.get("system_id"));
			
			return body;
		}
		catch (Exception e) {
			log.error("Failed to create notification body", e);
			return "The status of file item with uuid " + associatedUuid.toString() +
					" has changed to " + event;
		}
	}
	
	@SuppressWarnings("unchecked")
	protected Map<String, Object> getJobRow(String uuid) throws NotificationException 
	{
		try 
        {
        	HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
            session.clear();
            
            String sql = "SELECT f.native_format as native_format, s.system_id as system_id, f.path as absolutepath, f.name as name, f.source as source " +
            		"FROM logical_files f left join systems s on f.system_id = s.id " +
            		"WHERE f.uuid = :uuid";
            
            Map<String, Object> row = (Map<String, Object>)session
            		.createSQLQuery(sql)
            		.setString("uuid", uuid.toString())
            		.setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP)
            		.uniqueResult();

            session.flush();
            
            if (row == null)
                throw new UUIDException("No such uuid present");
            
            return row;
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
