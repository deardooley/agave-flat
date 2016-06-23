package org.iplantc.service.io.exceptions;

public class MetaDataException extends Exception {

	private static final long serialVersionUID = -7037752104442269749L;

	public MetaDataException() {}

	public MetaDataException(String message) {
		super(message);
	}

	public MetaDataException(Throwable cause) {
		super(cause);
	}

	public MetaDataException(String message, Throwable cause) {
		super(message, cause);
	}

}
