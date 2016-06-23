package org.iplantc.service.io.exceptions;

public class FileEventProcessingException extends Exception {

	private static final long serialVersionUID = 5679611012196007611L;

	public FileEventProcessingException() {}

	public FileEventProcessingException(String arg0)
	{
		super(arg0);
	}

	public FileEventProcessingException(Throwable arg0)
	{
		super(arg0);
	}

	public FileEventProcessingException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}

}
