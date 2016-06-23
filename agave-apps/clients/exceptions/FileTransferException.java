package org.iplantc.service.clients.exceptions;

public class FileTransferException extends Exception {

	private static final long serialVersionUID = 717487845011229670L;

	public FileTransferException()
	{
		super();
	}

	public FileTransferException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

	public FileTransferException(String arg0)
	{
		super(arg0);
	}

	public FileTransferException(Throwable arg0)
	{
		super(arg0);
	}

}
