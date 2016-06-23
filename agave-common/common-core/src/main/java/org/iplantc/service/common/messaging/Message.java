package org.iplantc.service.common.messaging;

/**
 * Wrapper class to hold messages and their id.
 * 
 * @author dooley
 *
 */
public class Message {

	private Object id;
	private String message;
	
	public Message(Object id, String message)
	{
		this.id = id;
		this.message = message;
	}
	/**
	 * @return the id
	 */
	public Object getId()
	{
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(Object id)
	{
		this.id = id;
	}
	/**
	 * @return the message
	 */
	public String getMessage()
	{
		return message;
	}
	/**
	 * @param message the message to set
	 */
	public void setMessage(String message)
	{
		this.message = message;
	}
	

}
