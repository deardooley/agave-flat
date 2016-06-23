package org.iplantc.service.jobs.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.iplantc.service.apps.util.ServiceUtils;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.model.Job;

public class CallbackMacroResolver {
	
	/**
	 * Resolves the macros in a url to the relevant job attributes 
	 * @param job
	 * @param callback
	 * @return
	 */
	public static String resolve(Job job, String value)
	{
		if (ServiceUtils.isEmailAddress(value)) return value;
		
		value = value.replaceAll("(?i)\\$\\{JOB_STATUS\\}", 
				(job.getStatus() != null ? job.getStatus().name() : "null"));
		
		value = value.replaceAll("(?i)\\$\\{JOB_URL\\}", 
				TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE, job.getTenantId()) + "job/" + job.getUuid());
		
		value = value.replaceAll("(?i)\\$\\{JOB_ID\\}", 
				(ServiceUtils.isValid(job.getUuid()) ? job.getUuid().toString() : "null"));
		
		value = value.replaceAll("(?i)\\$\\{JOB_EXECUTION_SYSTEM\\}", 
				(ServiceUtils.isValid(job.getSystem()) ? job.getSystem() : "null"));
		
		value = value.replaceAll("(?i)\\$\\{JOB_NAME\\}", 
				(ServiceUtils.isValid(job.getName()) ? job.getName() : ""));
		
		value = value.replaceAll("(?i)\\$\\{JOB_START_TIME\\}", formatDate(job.getStartTime()));
		
		value = value.replaceAll("(?i)\\$\\{JOB_END_TIME\\}", formatDate(job.getEndTime()));
		
		value = value.replaceAll("(?i)\\$\\{JOB_SUBMIT_TIME\\}", formatDate(job.getSubmitTime()));
		
		value = value.replaceAll("(?i)\\$\\{JOB_ARCHIVE_SYSTEM\\}", 
				(job.getArchiveSystem() == null ? "null" : job.getArchiveSystem().getSystemId()));
		
		value = value.replaceAll("(?i)\\$\\{JOB_ARCHIVE_PATH\\}", 
				(ServiceUtils.isValid(job.getArchivePath()) ? job.getArchivePath() : ""));
		
		value = value.replaceAll("(?i)\\$\\{JOB_ARCHIVE_URL\\}", 
				(ServiceUtils.isValid(job.getArchiveUrl()) ? job.getArchiveUrl() : ""));
		
		value = value.replaceAll("(?i)\\$\\{JOB_ERROR\\}", 
				(ServiceUtils.isValid(job.getErrorMessage()) ? job.getErrorMessage() : ""));
		
		return value;
		
	}
	
	private static String formatDate(Date date) 
	{
		//SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd'T'HHmmssZ");
		SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
		return ServiceUtils.isValid(date) ? formatter.format(date) : "n/a";
	}
}

