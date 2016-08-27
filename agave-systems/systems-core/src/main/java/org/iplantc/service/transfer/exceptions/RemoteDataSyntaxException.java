/**
 * 
 */
package org.iplantc.service.transfer.exceptions;

/**
 * @author rcardone
 * 
 */
public class RemoteDataSyntaxException extends Exception {

    private static final long serialVersionUID = 3040627949456587637L;

    /**
	 * 
	 */
	public RemoteDataSyntaxException() {}

	/**
	 * @param arg0
	 */
	public RemoteDataSyntaxException(String arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public RemoteDataSyntaxException(Throwable arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public RemoteDataSyntaxException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
