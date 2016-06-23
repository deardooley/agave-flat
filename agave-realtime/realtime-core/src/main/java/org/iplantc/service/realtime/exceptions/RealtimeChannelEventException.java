/**
 * 
 */
package org.iplantc.service.realtime.exceptions;

/**
 * @author dooley
 *
 */
public class RealtimeChannelEventException extends Exception {

	private static final long serialVersionUID = -5429268122052105499L;

	public RealtimeChannelEventException() {}

	public RealtimeChannelEventException(String arg0)
	{
		super(arg0);
	}

	public RealtimeChannelEventException(Throwable arg0)
	{
		super(arg0);
	}

	public RealtimeChannelEventException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}
}
