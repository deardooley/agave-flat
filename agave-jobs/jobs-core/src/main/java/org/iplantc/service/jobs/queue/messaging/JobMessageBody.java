/**
 * 
 */
package org.iplantc.service.jobs.queue.messaging;

import org.iplantc.service.common.messaging.DefaultMessageBody;

/**
 * @author dooley
 *
 */
public class JobMessageBody extends DefaultMessageBody<JobMessageContext>
{

	public JobMessageBody(String uuid, String owner, String tenant,
			JobMessageContext context)
	{
		super(uuid, owner, tenant, context);
	}

}

