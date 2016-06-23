/**
 * 
 */
package org.iplantc.service.common.quartz.util.ui;

import org.joda.time.DateTime;
import org.quartz.JobExecutionContext;

/**
 * @author dooley
 *
 */
public class ActiveJobDescription
{
	private String key;
	private boolean isActive;
	private String type;
	private String description;
	private DateTime scheduledAt;
	private DateTime firedAt;
	private DateTime nextFireAt;
	private DateTime previousFiredAt;
	private long refireCount;
	private boolean isRecovering;
	private long lastFireDuration;
	private boolean allowsConcurrentExecution;
	private String result;
	private String triggerKey;

	
	/**
	 * @param key
	 * @param isActive
	 * @param type
	 * @param description
	 * @param scheduledAt
	 * @param firedAt
	 * @param nextFireAt
	 * @param previousFiredAt
	 * @param refireCount
	 * @param isRecovering
	 * @param lastFireDuration
	 * @param allowsConcurrentExecution
	 * @param result
	 * @param triggerKey
	 */
	public ActiveJobDescription(String key, boolean isActive, String type,
			String description, DateTime scheduledAt, DateTime firedAt,
			DateTime nextFireAt, DateTime previousFiredAt, long refireCount,
			boolean isRecovering, long lastFireDuration,
			boolean allowsConcurrentExecution, String result, String triggerKey)
	{
		this.key = key;
		this.isActive = isActive;
		this.type = type;
		this.description = description;
		this.scheduledAt = scheduledAt;
		this.firedAt = firedAt;
		this.nextFireAt = nextFireAt;
		this.previousFiredAt = previousFiredAt;
		this.refireCount = refireCount;
		this.isRecovering = isRecovering;
		this.lastFireDuration = lastFireDuration;
		this.allowsConcurrentExecution = allowsConcurrentExecution;
		this.result = result;
		this.triggerKey = triggerKey;
	}

	/**
	 * Construct an Active Job Description from a 
	 * JobExecutionContext returned from the scheduler.
	 * @param job JobExecutionContext
	 */
	public ActiveJobDescription(JobExecutionContext job) 
	{
		this.key = job.getJobDetail().getKey().toString();
		this.isActive = (job.getJobRunTime() == -1);
		this.type = job.getJobDetail().getClass().getSimpleName();
		this.description = job.getMergedJobDataMap().getString("custom");
		this.scheduledAt = new DateTime(job.getScheduledFireTime());
		this.firedAt = new DateTime(job.getFireTime());
		this.nextFireAt = new DateTime(job.getNextFireTime());
		this.previousFiredAt = new DateTime(job.getPreviousFireTime());
		this.refireCount = job.getRefireCount();
		this.isRecovering = job.isRecovering();
		this.lastFireDuration = job.getJobRunTime();
		this.allowsConcurrentExecution = job.getJobDetail().isConcurrentExectionDisallowed();
		this.result = job.getResult() == null ? null : job.getResult().toString();
		this.triggerKey = job.getTrigger().getKey().getName();
	}

	/**
	 * @return the key
	 */
	public String getKey()
	{
		return key;
	}

	/**
	 * @param key the key to set
	 */
	public void setKey(String key)
	{
		this.key = key;
	}

	/**
	 * @return the isActive
	 */
	public boolean isActive()
	{
		return isActive;
	}

	/**
	 * @param isActive the isActive to set
	 */
	public void setActive(boolean isActive)
	{
		this.isActive = isActive;
	}

	/**
	 * @return the type
	 */
	public String getType()
	{
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type)
	{
		this.type = type;
	}

	/**
	 * @return the description
	 */
	public String getDescription()
	{
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description)
	{
		this.description = description;
	}

	/**
	 * @return the scheduledAt
	 */
	public DateTime getScheduledAt()
	{
		return scheduledAt;
	}

	/**
	 * @param scheduledAt the scheduledAt to set
	 */
	public void setScheduledAt(DateTime scheduledAt)
	{
		this.scheduledAt = scheduledAt;
	}

	/**
	 * @return the firedAt
	 */
	public DateTime getFiredAt()
	{
		return firedAt;
	}

	/**
	 * @param firedAt the firedAt to set
	 */
	public void setFiredAt(DateTime firedAt)
	{
		this.firedAt = firedAt;
	}

	/**
	 * @return the nextFireAt
	 */
	public DateTime getNextFireAt()
	{
		return nextFireAt;
	}

	/**
	 * @param nextFireAt the nextFireAt to set
	 */
	public void setNextFireAt(DateTime nextFireAt)
	{
		this.nextFireAt = nextFireAt;
	}

	/**
	 * @return the previousFiredAt
	 */
	public DateTime getPreviousFiredAt()
	{
		return previousFiredAt;
	}

	/**
	 * @param previousFiredAt the previousFiredAt to set
	 */
	public void setPreviousFiredAt(DateTime previousFiredAt)
	{
		this.previousFiredAt = previousFiredAt;
	}

	/**
	 * @return the refireCount
	 */
	public long getRefireCount()
	{
		return refireCount;
	}

	/**
	 * @param refireCount the refireCount to set
	 */
	public void setRefireCount(long refireCount)
	{
		this.refireCount = refireCount;
	}

	/**
	 * @return the isRecovering
	 */
	public boolean isRecovering()
	{
		return isRecovering;
	}

	/**
	 * @param isRecovering the isRecovering to set
	 */
	public void setRecovering(boolean isRecovering)
	{
		this.isRecovering = isRecovering;
	}

	/**
	 * @return the lastFireDuration
	 */
	public long getLastFireDuration()
	{
		return lastFireDuration;
	}

	/**
	 * @param lastFireDuration the lastFireDuration to set
	 */
	public void setLastFireDuration(long lastFireDuration)
	{
		this.lastFireDuration = lastFireDuration;
	}

	/**
	 * @return the allowsConcurrentExecution
	 */
	public boolean isAllowsConcurrentExecution()
	{
		return allowsConcurrentExecution;
	}

	/**
	 * @param allowsConcurrentExecution the allowsConcurrentExecution to set
	 */
	public void setAllowsConcurrentExecution(boolean allowsConcurrentExecution)
	{
		this.allowsConcurrentExecution = allowsConcurrentExecution;
	}

	/**
	 * @return the result
	 */
	public String getResult()
	{
		return result;
	}

	/**
	 * @param result the result to set
	 */
	public void setResult(String result)
	{
		this.result = result;
	}

	/**
	 * @return the triggerKey
	 */
	public String getTriggerKey()
	{
		return triggerKey;
	}

	/**
	 * @param triggerKey the triggerKey to set
	 */
	public void setTriggerKey(String triggerKey)
	{
		this.triggerKey = triggerKey;
	}
	
	
}
