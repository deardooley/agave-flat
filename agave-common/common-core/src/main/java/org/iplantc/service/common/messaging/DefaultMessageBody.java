/**
 * 
 */
package org.iplantc.service.common.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Abstract {@link org.iplantc.service.common.messaging.MessageBody} implementation
 * class providing the minimum information needed to process any message
 * send in the API.
 * 
 * @author dooley
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DefaultMessageBody<E> implements MessageBody<E>
{
    protected String uuid;
	protected String owner;
	protected String tenant;
	protected E context;
	
	public DefaultMessageBody() {
	    super();
	}
	
	public DefaultMessageBody(String uuid, String owner, String tenant)
	{
		super();
		this.uuid = uuid;
		this.owner = owner;
		this.tenant = tenant;
	}

	public DefaultMessageBody(String uuid, String owner, String tenant,
			E context)
	{
		super();
		this.uuid = uuid;
		this.owner = owner;
		this.tenant = tenant;
		this.context = context;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.messaging.MessageBody#getUuid()
	 */
	@Override
	public String getUuid()
	{
		return uuid;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.messaging.MessageBody#setUuid(java.lang.String)
	 */
	@Override
	public void setUuid(String uuid)
	{
		this.uuid = uuid;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.messaging.MessageBody#getOwner()
	 */
	@Override
	public String getOwner()
	{
		return owner;
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.messaging.MessageBody#setOwner(java.lang.String)
	 */
	@Override
	public void setOwner(String owner)
	{
		this.owner = owner;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.messaging.MessageBody#getTenant()
	 */
	@Override
	public String getTenant()
	{
		return tenant;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.messaging.MessageBody#setTenant(java.lang.String)
	 */
	@Override
	public void setTenant(String tenant)
	{
		this.tenant = tenant;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() 
	{
		return String.format("%s, %s, %s", getUuid(), getOwner(), getTenant());
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.messaging.MessageBody#getContext()
	 */
	@Override
	public E getContext() {
		return this.context;
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.common.messaging.MessageBody#setContext(java.lang.Object)
	 */
	@Override
	public void setContext(E context)
	{
		this.context = context;
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.common.messaging.MessageBody#toJSON()
	 */
	@Override
	public String toJSON() throws JsonProcessingException
	{
		return new ObjectMapper().writeValueAsString(this);
	}

}
