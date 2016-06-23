/**
 * 
 */
package org.iplantc.service.notification.events;

import java.math.BigInteger;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.notification.model.Notification;
import org.joda.time.DateTime;

/**
 * @author dooley
 *
 */
public class TransferNotificationEvent extends AbstractEventFilter {

	private static final Logger log = Logger.getLogger(TransferNotificationEvent.class);
	
	/**
	 * @param notification
	 */
	public TransferNotificationEvent(AgaveUUID associatedUuid, Notification notification, String event, String owner)
	{
		super(associatedUuid, notification, event, owner);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getEmailBody()
	 */
	@Override
	public String getEmailBody()
	{
		return  "Transfer ${UUID} changed status to ${EVENT}. \n\n" + 
				"Source: ${SOURCE}\n" +
				"Destination: ${DESTINATION}\n" +
				"Status: ${STATUS}\n" + 
				"Created: ${CREATED}\n" + 
				"Start time: ${START_TIME}\n" +
				"End time: ${END_TIME}\n" +
				"Total size: ${TOTAL_SIZE}\n" +
				"Bytes transferred: ${TOTAL_TRANSFERRED}\n" +
				"Transfer rate: ${TRANSFER_RATE}\n" +
				"Attempts: ${ATTEMPTS}\n";
	}
	
	/* (non-Javadoc)
     * @see org.iplantc.service.notification.events.EventFilter#getHtmlEmailBody()
     */
    @Override
    public String getHtmlEmailBody()
    {
        return  "<p>Transfer ${UUID} changed status to ${EVENT}.</p><br>" + 
                "<strong>Source:</strong> ${SOURCE}<br>" +
                "<strong>Destination:</strong> ${DESTINATION}<br>" +
                "<strong>Status:</strong> ${STATUS}<br>" + 
                "<strong>Created:</strong> ${CREATED}<br>" + 
                "<strong>Start time:</strong> ${START_TIME}<br>" +
                "<strong>End time:</strong> ${END_TIME}<br>" +
                "<strong>Total size:</strong> ${TOTAL_SIZE}<br>" +
                "<strong>Bytes transferred:</strong> ${TOTAL_TRANSFERRED}<br>" +
                "<strong>Transfer rate:</strong> ${TRANSFER_RATE}<br>" +
                "<strong>Attempts:</strong> ${ATTEMPTS}</p>";
    }

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.EventFilter#getEmailSubject()
	 */
	@Override
	public String getEmailSubject()
	{
		return resolveMacros("Transfer ${UUID} changed status to ${EVENT}", false);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.notification.events.AbstractNotificationEvent#resolveMacros(java.lang.String, boolean)
	 */
	@Override
	public String resolveMacros(String body, boolean urlEncode)
	{
		try 
		{
			Map<String, Object> jobFieldMap = getJobRow("transfers", associatedUuid.toString());
			
			body = StringUtils.replace(body, "${UUID}", associatedUuid.toString());
			body = StringUtils.replace(body, "${EVENT}", event);
			body = StringUtils.replace(body, "${SOURCE}", (String)jobFieldMap.get("source"));
			body = StringUtils.replace(body, "${DESTINATION}", (String)jobFieldMap.get("dest"));
			body = StringUtils.replace(body, "${STATUS}", (String)jobFieldMap.get("status"));
			body = StringUtils.replace(body, "${CREATED}", new DateTime((String)jobFieldMap.get("created")).toString());
			body = StringUtils.replace(body, "${START_TIME}", new DateTime((String)jobFieldMap.get("start_time")).toString());
			body = StringUtils.replace(body, "${END_TIME}", new DateTime(jobFieldMap.get("end_time")).toString());
			body = StringUtils.replace(body, "${TOTAL_SIZE}", ((BigInteger)jobFieldMap.get("total_size")).toString());
			body = StringUtils.replace(body, "${TOTAL_TRANSFERRED}", ((Double)jobFieldMap.get("bytes_transferred")).toString());
			body = StringUtils.replace(body, "${TRANSFER_RATE}", ((BigInteger)jobFieldMap.get("transfer_rate")).toString());
			body = StringUtils.replace(body, "${ATTEMPTS}", ((Integer)jobFieldMap.get("attempts")).toString());
			
			
			return body;
		}
		catch (Exception e) {
			log.error("Failed to create notification body", e);
			return "The status of transfer " + associatedUuid.toString() +
					" has changed to " + event;
		}
	}

}
