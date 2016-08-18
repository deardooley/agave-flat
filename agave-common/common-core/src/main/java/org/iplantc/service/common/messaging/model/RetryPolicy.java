package org.iplantc.service.common.messaging.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.iplantc.service.common.messaging.model.constraints.ValidRetryPolicy;
import org.iplantc.service.common.messaging.model.enumerations.RetryStrategyType;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the behavior the service will model in the event of
 * notification failure. By default, a notification will retry once
 * immediately after it fails. If it does not succeed on the initial
 * retry, it will be then become subject to this {@link NotificationPolicy}.
 *  
 * @author dooley
 *
 */
@Embeddable
@JsonAutoDetect(fieldVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
@ValidRetryPolicy
@JsonIgnoreProperties(ignoreUnknown = true)
public class RetryPolicy
{
	@Enumerated(EnumType.STRING)
	@Column(name = "retry_strategy", nullable=false, length = 12)
	@NotNull(message="Invalid notification policy retryStrategy. Strategy must be one of: NONE, IMMEDIATE, DELAYED, or EXPONENTIAL")
	@JsonProperty("retryStrategy")
	private RetryStrategyType retryStrategyType = RetryStrategyType.IMMEDIATE;
	
	/**
	 * Maximum number of retries before failing and terminating
	 * subsequent attempts. Max 1440 or once per minute for an
	 * entire day.
	 */
	@Column(name = "retry_limit", nullable=false, length = 12)
	@Min(0)
	@Max(1440)
	private int retryLimit = 1440;
	
	/**
	 * Number of seconds between retries. A value of zero indicates
	 * the attempt should be retired immediately. Max 86400 (1 day).
	 */
	@Column(name = "retry_rate", nullable=false, length = 12)
	@Min(0)
	@Max(86400)
	private int retryRate = 5;
	
	/**
	 * The number of seconds to delay after the initial notification 
	 * attempt before beginning retry attempts. Max is 1 day.
	 */
	@Column(name = "retry_delay", nullable=false, length = 12)
	@Min(0)
	@Max(86400)
	private int retryDelay = 0;
	
	/**
	 * Whether a failed {@link MessageBody} will be stored 
	 * if delivery cannot be made before this {@link RetryPolicy} expires.
	 */
	@Column(name = "save_on_failure", columnDefinition = "TINYINT(1)")
	private boolean saveOnFailure = false; 
	
	public RetryPolicy() {}
	
	/**
	 * @param retryStrategyType
	 * @param retryLimit
	 * @param retryRate
	 * @param retryDelay
	 * @param saveOnFailure
	 */
	public RetryPolicy(RetryStrategyType retryStrategyType, int retryLimit,
			int retryRate, int retryDelay, boolean saveOnFailure) {
		this.retryStrategyType = retryStrategyType;
		this.retryLimit = retryLimit;
		this.retryRate = retryRate;
		this.retryDelay = retryDelay;
		this.saveOnFailure = saveOnFailure;
	}

	/**
	 * @return the retryStrategyType
	 */
	public RetryStrategyType getRetryStrategyType() {
		return retryStrategyType;
	}


	/**
	 * @param retryStrategyType the retryStrategyType to set
	 */
	public void setRetryStrategyType(RetryStrategyType retryStrategyType) {
		this.retryStrategyType = retryStrategyType;
	}


	/**
	 * @return the retryLimit
	 */
	public int getRetryLimit() {
		return retryLimit;
	}


	/**
	 * @param retryLimit the retryLimit to set
	 */
	public void setRetryLimit(int retryLimit) {
		this.retryLimit = retryLimit;
	}


	/**
	 * @return the retryRate
	 */
	public int getRetryRate() {
		return retryRate;
	}


	/**
	 * @param retryRate the retryRate to set
	 */
	public void setRetryRate(int retryRate) {
		this.retryRate = retryRate;
	}


	/**
	 * @return the retryDelay
	 */
	public int getRetryDelay() {
		return retryDelay;
	}


	/**
	 * @param retryDelay the retryDelay to set
	 */
	public void setRetryDelay(int retryDelay) {
		this.retryDelay = retryDelay;
	}


	/**
	 * @return the saveOnFailure
	 */
	public boolean isSaveOnFailure() {
		return saveOnFailure;
	}


	/**
	 * @param saveOnFailure the saveOnFailure to set
	 */
	public void setSaveOnFailure(boolean saveOnFailure) {
		this.saveOnFailure = saveOnFailure;
	}

	@Override
	public String toString() {
		return String.format("%s - %d/%s/%d/%s", 
				retryStrategyType.name(),
				retryLimit,
				retryRate,
				retryDelay,
				Boolean.toString(saveOnFailure));
	}
}
