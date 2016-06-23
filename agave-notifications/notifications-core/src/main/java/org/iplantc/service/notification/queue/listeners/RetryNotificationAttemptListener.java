package org.iplantc.service.notification.queue.listeners;

import org.apache.log4j.Logger;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.joda.time.DateTime;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

public class RetryNotificationAttemptListener implements JobListener{
	
	private static final Logger log = Logger
			.getLogger(RetryNotificationAttemptListener.class);
	
    @Override
    public String getName() {
        return "Retry notification attempt listener";
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException e) {
    	if (e == null) {
    		NotificationAttempt attempt = (NotificationAttempt)context.getResult();
    		if (attempt instanceof NotificationAttempt) {
	    		if (attempt.isSuccess()) {
	    			log.debug(String.format("Attempt [%d]: Successfully sent %s notification to %s",
	    					attempt.getAttemptNumber() - 1, 
	    					attempt.getEventName(),
	    					attempt.getCallbackUrl()));
	    		} 
	    		else {
	    			if (attempt.getScheduledTime() == null) {
	    				log.info(String.format("Final attempt failed to deliver %s notification to %s failed. "
	    						+ "Message will be expired according to the retry policy for this notification", 
		    					attempt.getAttemptNumber()-1, 
		    					attempt.getEventName(),
		    					attempt.getCallbackUrl(),
		    					new DateTime(attempt.getScheduledTime()).toString()));
	    			} else {
	    				log.info(String.format("Attempt [%d]: Failed to send %s notification to %s. "
	    						+ "Next retry scheduled for %s", 
		    					attempt.getAttemptNumber()-1, 
		    					attempt.getEventName(),
		    					attempt.getCallbackUrl(),
		    					new DateTime(attempt.getScheduledTime()).toString()));
	    			}
	    		}
    		} else {
    			log.error("Unexpected result returned from retry notification processing job ", e);
    		}
        } else {
        	log.error("Unexpected termination of retry notification processing job ", e);
        	
        }
    }

	@Override
	public void jobToBeExecuted(JobExecutionContext context) {
	}

	@Override
	public void jobExecutionVetoed(JobExecutionContext context) {
	}
    
}
