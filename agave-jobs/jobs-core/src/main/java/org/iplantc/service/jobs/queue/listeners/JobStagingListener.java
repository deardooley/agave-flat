package org.iplantc.service.jobs.queue.listeners;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.exceptions.RetryPolicyViolationException;
import org.iplantc.service.common.messaging.model.RetryCalculator;
import org.iplantc.service.common.messaging.model.RetryPolicy;
import org.iplantc.service.common.messaging.model.RetryPolicyHelper;
import org.iplantc.service.common.messaging.model.enumerations.RetryStrategyType;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.joda.time.DateTime;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

public class JobStagingListener implements JobListener{
	
	private static final Logger log = Logger
			.getLogger(JobStagingListener.class);
	
    @Override
    public String getName() {
        return "Job staging attempt listener";
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException e) {
    	if (e == null) {
    		int attempt = NumberUtils.toInt((String)context.getMergedJobDataMap().get("attempt"));
    		String jobUuid = (String)context.getMergedJobDataMap().get("uuid");
    		String messageId = (String)context.getMergedJobDataMap().get("messageId");
    		
    		Object result = context.getResult();
    		if (result instanceof JobStatusType) {
    			JobStatusType status = (JobStatusType)result;
	    		if (status == JobStatusType.STAGED) {
	    			log.debug("Successfully completed staging inputs for job " + jobUuid);
	    		} 
	    		else {
	    			RetryPolicy policy = RetryPolicyHelper.getDefaultRetryPolicyForRetryStrategyType(RetryStrategyType.EXPONENTIAL);
	    			try {
	    				int secondsToNextAttempt = new RetryCalculator(policy, attempt).getNextScheduledTime();
	    				log.info(String.format("Failed to stage inputs for %s. "
	    						+ "Next retry is scheduled for %s.", 
		    					jobUuid,
		    					new DateTime().plusSeconds(secondsToNextAttempt).toString()));
	    			}
	    			catch (RetryPolicyViolationException e1) {
	    				log.info(String.format("Failed to stage inputs for job %s. "
	    						+ "No retry will be attempted according to the staging retry policy for this job", 
		    					jobUuid));
	    			}
	    		}
    		} else {
    			log.error("Unexpected result returned from job " + 
    					jobUuid + " staging attempt " + attempt);
    		}
        } else {
        	log.error("Unexpected termination of job staging attempt", e);
        }
    }

	@Override
	public void jobToBeExecuted(JobExecutionContext context) {
	}

	@Override
	public void jobExecutionVetoed(JobExecutionContext context) {
	}
    
}
