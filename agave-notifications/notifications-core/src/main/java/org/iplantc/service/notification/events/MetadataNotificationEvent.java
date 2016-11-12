/**
 * 
 */
package org.iplantc.service.notification.events;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.notification.model.Notification;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author dooley
 *
 */
public class MetadataNotificationEvent extends AbstractEventFilter {

	private static final Logger log = Logger.getLogger(MetadataNotificationEvent.class);
	
	/**
	 * @param notification
	 */
	public MetadataNotificationEvent(AgaveUUID associatedUuid, Notification notification, String event, String owner)
	{
		super(associatedUuid, notification, event, owner);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getEmailBody()
	 */
	@Override
	public String getEmailBody()
	{
		String body = "The following metadata object received a(n) " + event +  
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
		return resolveMacros("Metadata ${UUID} received a(n) ${EVENT} event", false);
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
    			    	JsonNode jsonMetadata = null;
    			    	if (json.hasNonNull("metadata")) {
    			    		jsonMetadata = json.get("metadata");
    			    	} else if (json.hasNonNull("id")) {
    			    		jsonMetadata = json;
    			    	}
			    		
    			    	body = StringUtils.replace(body, "${METADATA_ID}", jsonMetadata.hasNonNull("id") ? jsonMetadata.get("id").asText() : "");
						body = StringUtils.replace(body, "${METADATA_OWNER}", jsonMetadata.hasNonNull("owner") ? jsonMetadata.get("owner").asText() : "");
						body = StringUtils.replace(body, "${METADATA_NAME}", jsonMetadata.hasNonNull("name") ? jsonMetadata.get("name").asText() : "");
						body = StringUtils.replace(body, "${METADATA_VALUE}", jsonMetadata.hasNonNull("value") ? jsonMetadata.get("value").asText() : "");
						body = StringUtils.replace(body, "${METADATA_SCHEMAID}", jsonMetadata.hasNonNull("schemaId") ? jsonMetadata.get("schemaId").asText() : "");
						body = StringUtils.replace(body, "${METADATA_ASSOCIATIONIDS}", jsonMetadata.hasNonNull("associationIds") ? jsonMetadata.get("associationIds").toString() : "");
						body = StringUtils.replace(body, "${METADATA_LASTUPDATED}", jsonMetadata.hasNonNull("lastUpdated") ? jsonMetadata.get("lastUpdated").asText() : "");
						body = StringUtils.replace(body, "${METADATA_CREATED}", jsonMetadata.hasNonNull("created") ? jsonMetadata.get("created").asText() : "");
    			    }
    			    body = StringUtils.replace(body, "${RAW_JSON}", mapper.writer().withDefaultPrettyPrinter().writeValueAsString(getCustomNotificationMessageContextData()));
			    } catch (Exception e) {
			        body = StringUtils.replace(body, "${RAW_JSON}", getCustomNotificationMessageContextData());
			    }
			}
			
			return body;
		}
		catch (Exception e) {
			log.error("Failed to create metadata body", e);
			return "The status of metadata " + associatedUuid.toString() +
					" has changed to " + event;
		}
	}

}
