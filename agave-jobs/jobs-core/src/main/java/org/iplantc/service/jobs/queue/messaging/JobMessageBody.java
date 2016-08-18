/**
 * 
 */
package org.iplantc.service.jobs.queue.messaging;

import org.iplantc.service.common.messaging.model.DefaultMessageBody;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author dooley
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobMessageBody extends DefaultMessageBody<JobMessageContext>
{
	public JobMessageBody(String uuid, String owner, String tenant,
			JobMessageContext context)
	{
		super(uuid, owner, tenant, context);
	}

}

