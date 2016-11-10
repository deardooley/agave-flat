/**
 * 
 */
package org.iplantc.service.notification.events;

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
public class MetadataSchemaNotificationEvent extends AbstractEventFilter {

	private static final Logger log = Logger.getLogger(MetadataSchemaNotificationEvent.class);
	
	/**
	 * @param notification
	 */
	public MetadataSchemaNotificationEvent(AgaveUUID associatedUuid, Notification notification, String event, String owner)
	{
		super(associatedUuid, notification, event, owner);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getEmailBody()
	 */
	@Override
	public String getEmailBody()
	{
		String body = "The following metadata schemata received a(n) " + event +  
					" event at "+ new DateTime().toString() + " from " + owner + ".\n\n";
		
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
		return resolveMacros("Metadata schemata ${UUID} received a(n) ${EVENT} event", false);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.AbstractNotificationEvent#resolveMacros(java.lang.String, boolean)
	 */
	@Override
	public String resolveMacros(String body, boolean urlEncode)
	{
		try 
		{
			body = StringUtils.replace(body,"${UUID}", associatedUuid.toString());
			body = StringUtils.replace(body,"${EVENT}", event);
			body = StringUtils.replace(body,"${OWNER}", owner);
			
			if (StringUtils.isNotEmpty(getCustomNotificationMessageContextData())) {
			    ObjectMapper mapper = new ObjectMapper();
			    try {
			        JsonNode json = mapper.readTree(getCustomNotificationMessageContextData());
    			    if (json.isObject()) {
    			    	JsonNode jsonMetadataSchema = null;
    			    	if (json.hasNonNull("metadataSchema")) {
    			    		jsonMetadataSchema = json.get("metadataSchema");
    			    	} else if (json.hasNonNull("id")) {
    			    		jsonMetadataSchema = json;
    			    	}
			    		
    			    	body = StringUtils.replace(body, "${METADATA_SCHEMA_ID}", jsonMetadataSchema.hasNonNull("id") ? jsonMetadataSchema.get("id").asText() : "");
						body = StringUtils.replace(body, "${METADATA_SCHEMA_OWNER}", jsonMetadataSchema.hasNonNull("owner") ? jsonMetadataSchema.get("owner").asText() : "");
						body = StringUtils.replace(body, "${METADATA_SCHEMA_SCHEMA}", jsonMetadataSchema.hasNonNull("schema") ? jsonMetadataSchema.get("schema").asText() : "");
						body = StringUtils.replace(body, "${METADATA_SCHEMA_LASTUPDATED}", jsonMetadataSchema.hasNonNull("lastUpdated") ? jsonMetadataSchema.get("lastUpdated").asText() : "");
						body = StringUtils.replace(body, "${METADATA_SCHEMA_CREATED}", jsonMetadataSchema.hasNonNull("created") ? jsonMetadataSchema.get("created").asText() : "");
    			    }
    			    body = StringUtils.replace(body, "${RAW_JSON}", mapper.writer().withDefaultPrettyPrinter().writeValueAsString(getCustomNotificationMessageContextData()));
			    } catch (Exception e) {
			        body = StringUtils.replace(body, "${RAW_JSON}", getCustomNotificationMessageContextData());
			    }
			}
			    
			return body;
		}
		catch (Exception e) {
			log.error("Failed to create metadata schemata body", e);
			return "The status of metadata schemata " + associatedUuid.toString() +
					" has received a(n) " + event;
		}
	}

}
