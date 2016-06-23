package org.iplantc.service.jobs.queue.messaging;

/**
 * Simple bean to hold {@link Job} specific message context.
 * 
 * @author dooley
 *
 */
public class JobMessageContext
{
	private JobMessageType type;
	
	/**
	 * @param uuid
	 * @param owner
	 * @param tenant
	 */
	public JobMessageContext(JobMessageType type)
	{
		setType(type);
	}

    public JobMessageType getType() {
        return type;
    }

    public void setType(JobMessageType type) {
        this.type = type;
    }
	
	
}