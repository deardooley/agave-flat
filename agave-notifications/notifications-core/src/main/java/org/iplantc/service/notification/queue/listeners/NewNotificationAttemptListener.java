package org.iplantc.service.notification.queue.listeners;

import org.apache.log4j.Logger;
import org.iplantc.service.notification.model.NotificationAttempt;
import org.joda.time.DateTime;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

public class NewNotificationAttemptListener implements JobListener{
	
	private static final Logger log = Logger
			.getLogger(NewNotificationAttemptListener.class);
	
    @Override
    public String getName() {
        return "New notification attempt listener";
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException e) {
    	if (e == null) {
    		NotificationAttempt attempt = (NotificationAttempt)context.getResult();
    		if (attempt instanceof NotificationAttempt) {
	    		if (attempt.isSuccess()) {
	    			log.debug("Successfully sent " + attempt.getEventName() + " notification to " + attempt.getCallbackUrl());
	    		} 
	    		else {
	    			if (attempt.getScheduledTime() == null) {
	    				log.info(String.format("Failed to send %s notification to %s on initial attempt. "
	    						+ "No retry will be attempted according to the retry policy for this notification", 
		    					attempt.getEventName(),
		    					attempt.getCallbackUrl(),
		    					new DateTime(attempt.getScheduledTime()).toString()));
	    			} else {
	    				log.info(String.format("Failed to send %s notification to %s on initial attempt. "
	    						+ "Next retry is scheduled for %s.", 
		    					attempt.getEventName(),
		    					attempt.getCallbackUrl(),
		    					new DateTime(attempt.getScheduledTime()).toString()));
	    			}
	    		}
    		} else {
    			log.error("Unexpected result returned from new notification processing job ", e);
    		}
        } else {
        	log.error("Unexpected termination of new notification processing job ", e);
        	
        }
    }

	@Override
	public void jobToBeExecuted(JobExecutionContext context) {
	}

	@Override
	public void jobExecutionVetoed(JobExecutionContext context) {
	}
    
}
