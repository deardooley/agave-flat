package org.iplantc.service.jobs.queue.messaging;

import org.iplantc.service.common.messaging.model.RetryPolicy;
import org.iplantc.service.jobs.model.Job;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Simple bean to hold {@link Job} specific message context.
 * 
 * @author dooley
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobMessageContext
{
	private RetryPolicy policy;
	private int attempt;
	
	/**
	 * @param uuid
	 * @param owner
	 * @param tenant
	 */
	public JobMessageContext(RetryPolicy policy, int attempt)
	{
		setPolicy(policy);
		setAttempt(attempt);
	}

	/**
	 * @return the policy
	 */
	public RetryPolicy getPolicy() {
		return policy;
	}

	/**
	 * @param policy the policy to set
	 */
	public void setPolicy(RetryPolicy policy) {
		this.policy = policy;
	}

	/**
	 * @return the attempt
	 */
	public int getAttempt() {
		return attempt;
	}

	/**
	 * @param attempt the attempt to set
	 */
	public void setAttempt(int attempt) {
		this.attempt = attempt;
	}

    
}