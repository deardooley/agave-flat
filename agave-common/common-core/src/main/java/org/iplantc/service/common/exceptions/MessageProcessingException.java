/**
 * 
 */
package org.iplantc.service.common.exceptions;

/**
 * @author dooley
 *
 */
public class MessageProcessingException extends Exception {

	private static final long serialVersionUID = 8105666799415349771L;
	private boolean expired = false;;
	/**
	 * 
	 */
	public MessageProcessingException() {}
	
	/**
	 * @param message
	 */
	public MessageProcessingException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public MessageProcessingException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public MessageProcessingException(String message, Throwable cause)
	{
		super(message, cause);
	}
	
	public MessageProcessingException(boolean expired, String message)
    {
        super(message);
        setExpired(expired);
    }

	public MessageProcessingException(boolean expired, Throwable e)
	{
		super(e);
		setExpired(expired);
	}
	
	public MessageProcessingException(boolean expired, String message, Throwable e)
    {
        super(message, e);
        setExpired(expired);
    }

	public boolean isExpired()
	{
		return expired;
	}

	public void setExpired(boolean expired)
	{
		this.expired = expired;
	}

}
