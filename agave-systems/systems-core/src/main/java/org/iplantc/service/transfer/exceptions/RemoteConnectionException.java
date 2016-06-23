/**
 * 
 */
package org.iplantc.service.transfer.exceptions;

/**
 * @author dooley
 * 
 */
public class RemoteConnectionException extends RemoteDataException {

	private static final long serialVersionUID = 7209178499933585523L;

    /**
	 * 
	 */
	public RemoteConnectionException() {}

	/**
	 * @param arg0
	 */
	public RemoteConnectionException(String arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public RemoteConnectionException(Throwable arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public RemoteConnectionException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
