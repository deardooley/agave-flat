package org.iplantc.service.notification.model;

import org.iplantc.service.notification.exceptions.NotificationPolicyViolationException;
import org.iplantc.service.notification.model.enumerations.RetryStrategyType;

public class NotificationAttemptBackoffCalculator {

	private NotificationAttempt attempt;
	private NotificationPolicy policy;
	
	public NotificationAttemptBackoffCalculator(NotificationPolicy policy, NotificationAttempt attempt) {
		this.policy = policy;
		this.attempt = attempt;
	}
	
	/**
	 * Calculates the number of seconds until the next attempt should be made
	 * based on the {@link NotificationPolicy} of the {@link NotificationAttempt}.
	 * 
	 * @return number of seconds until the next event. -1 implies the attempts have expired.
	 * @throws NotificationPolicyViolationException 
	 */
	public int getNextScheduledTime() throws NotificationPolicyViolationException {
		
		// if we're over the attempts, kill future attempts
		if (attempt.getAttemptNumber() >= policy.getRetryLimit() - 1 || 
				policy.getRetryStrategyType() == RetryStrategyType.NONE) {
			throw new NotificationPolicyViolationException("Number of attempts has been violated");
		}
		else if (policy.getRetryStrategyType() == RetryStrategyType.IMMEDIATE) {
			return 0;
		} else {
			// if we're on an initial delay, calculate delay
			if (policy.getRetryStrategyType() == RetryStrategyType.DELAYED) {
				// if this is the first retry, 
				if (attempt.getAttemptNumber() == 1) {
					return policy.getRetryDelay();
				} else {
					return policy.getRetryRate();
				}
			}
			// if we're on exponential backoff, calculate next attempt
			else if (policy.getRetryStrategyType() == RetryStrategyType.EXPONENTIAL) {
				// every 5 seconds for the first 4 checks 
				if (attempt.getAttemptNumber() < 5) {
					return 5;
				// every 15 seconds for the next minute 
				} else if (attempt.getAttemptNumber() < 9) {
					return 15;
				// every minute for the next 5 minutes 
				} else if (attempt.getAttemptNumber() < 14) {
					return 60;
				// every 15 minutes for the next hour 
				} else if (attempt.getAttemptNumber() < 18) {
					return 900;
				// every hour until the max retry limit is hit
				} else { //if (attempt.getAttemptNumber() < policy.getRetryLimit()) {
					return 3600;
				}  
			}
			else {
				return -1;
			} 
		}
	}

}
