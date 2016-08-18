/**
 * 
 */
package org.iplantc.service.common.messaging.model;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Interface to which all message bodies must comply to 
 * properly isolate messages with or without queue and topic
 * routing.
 * 
 * @author dooley
 *
 */
public interface MessageBody<T>
{
	/**
	 * @return
	 */
	public String getUuid();
	
	/**
	 * @param uuid
	 */
	public void setUuid(String uuid);
	
	/**
	 * Returns the tenant through which to 
	 * establish the context of this message.
	 * 
	 * @return String textual tenantId 
	 */
	public String getTenant();
	
	/**
	 * Sets the tenant used to establish the 
	 * context of this message.
	 * 
	 * @param textual tenantId 
	 */
	public void setTenant(String tenantId);
	
	/**
	 * Returns the user associated with this message.
	 * 
	 * @return a valid username in the given tenant 
	 */
	public String getOwner();
	
	/**
	 * Sets the username of the user associated with 
	 * this message.
	 * 
	 * @param a valid username in the given tenant
	 */
	public void setOwner(String owner);
	
	/**
	 * Optional object to accompany this message.
	 * This can be descriptive message or json
	 * serializable data for use by the consuming 
	 * process.
	 * 
	 * @return optional custom context
	 */
	public T getContext();
	
	/**
	 * Optional object to accompany this message.
	 * This can be descriptive message or json
	 * serializable data for use by the consuming 
	 * process.
	 *
	 * <em>note:</em> Care should be taken to keep 
	 * the message size within the single message 
	 * capacity of the underlying message queue.
	 * 
	 * @param optional custom context
	 */
	public void setContext(T customContext);
	
	/**
	 * Serialized JSON representation of this object.
	 * 
	 * @return
	 */
	public String toJSON() throws JsonProcessingException;
}
