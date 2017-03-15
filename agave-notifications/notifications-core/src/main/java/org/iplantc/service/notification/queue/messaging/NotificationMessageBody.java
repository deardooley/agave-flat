/**
 * 
 */
package org.iplantc.service.notification.queue.messaging;

import org.iplantc.service.common.messaging.DefaultMessageBody;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author dooley
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationMessageBody extends DefaultMessageBody<NotificationMessageContext>
{

    /**
     * Default no-args contructor
     */
    public NotificationMessageBody() {
        super();
    }
    
    /**
     * Creates a notification message iwthout additional context.
     * @param uuid
     * @param owner
     * @param tenant
     */
    public NotificationMessageBody(String uuid, String owner, String tenant)
    {
        super(uuid, owner, tenant);
    }
    
	/** 
	 * Full constructor setting context for this notification mesage
	 * @param uuid
	 * @param owner
	 * @param tenant
	 * @param context
	 */
	public NotificationMessageBody(String uuid, String owner, String tenant,
			NotificationMessageContext context)
	{
		super(uuid, owner, tenant, context);
	}
	
	/* (non-Javadoc)
     * @see org.iplantc.service.common.messaging.MessageBody#getContext()
     */
    @Override
    public NotificationMessageContext getContext() {
        return this.context;
    }
    
    /* (non-Javadoc)
     * @see org.iplantc.service.common.messaging.MessageBody#setContext(java.lang.Object)
     */
    @Override
    public void setContext(NotificationMessageContext context)
    {
        this.context = context;
    }

}

