package org.iplantc.service.common.messaging.model;

import org.iplantc.service.common.exceptions.RetryPolicyViolationException;
import org.iplantc.service.common.messaging.model.enumerations.RetryStrategyType;

public class RetryCalculator {

	private RetryPolicy policy;
	private int attempt = 0;
	
	public RetryCalculator(RetryPolicy policy, int attempt) {
		this.policy = policy;
		this.attempt = attempt;
	}
	
	/**
	 * Calculates the number of seconds until the next attempt should be made
	 * based on the {@link RetryPolicy} of the {@link RetryAttempt}.
	 * 
	 * @return number of seconds until the next event. -1 implies the attempts have expired.
	 * @throws RetryPolicyViolationException 
	 */
	public int getNextScheduledTime() throws RetryPolicyViolationException {
		
		// if we're over the attempts, kill future attempts
		if (attempt >= policy.getRetryLimit() - 1 || 
				policy.getRetryStrategyType() == RetryStrategyType.NONE) {
			throw new RetryPolicyViolationException("Number of attempts has been violated");
		}
		else if (policy.getRetryStrategyType() == RetryStrategyType.IMMEDIATE) {
			return 0;
		} else {
			// if we're on an initial delay, calculate delay
			if (policy.getRetryStrategyType() == RetryStrategyType.DELAYED) {
				// if this is the first retry, 
				if (attempt == 1) {
					return policy.getRetryDelay();
				} else {
					return policy.getRetryRate();
				}
			}
			// if we're on exponential backoff, calculate next attempt
			else if (policy.getRetryStrategyType() == RetryStrategyType.EXPONENTIAL) {
				// every 5 seconds for the first 4 checks 
				if (attempt < 5) {
					return 5;
				// every 15 seconds for the next minute 
				} else if (attempt < 9) {
					return 15;
				// every minute for the next 5 minutes 
				} else if (attempt < 14) {
					return 60;
				// every 15 minutes for the next hour 
				} else if (attempt < 18) {
					return 900;
				// every hour until the max retry limit is hit
				} else { //if (attempt < policy.getRetryLimit()) {
					return 3600;
				}  
			}
			else {
				return -1;
			} 
		}
	}

}
