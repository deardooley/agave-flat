/**
 * 
 */
package org.iplantc.service.notification.events;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.notification.Settings;
import org.iplantc.service.notification.model.Notification;
import org.joda.time.DateTime;

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
				" has changed status to ${EVENT}", false);
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
			
			return body;
		}
		catch (Exception e) {
			log.error("Failed to create notification body", e);
			return "The status of job " + associatedUuid.toString() +
					" has changed to " + associatedUuid.toString();
		}
	}
}
