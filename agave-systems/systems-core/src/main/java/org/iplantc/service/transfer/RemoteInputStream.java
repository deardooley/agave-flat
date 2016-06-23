package org.iplantc.service.transfer;

import java.io.IOException;
import java.io.InputStream;

import org.globus.ftp.RestartData;
import org.iplantc.service.transfer.exceptions.RemoteDataException;

public class RemoteInputStream<T> extends InputStream {
	
	protected InputStream		input;

	protected T					client;
	
	protected String			targetFile;

	protected RemoteInputStream(){}

	public RemoteInputStream(T client, String file, boolean passive)
			throws IOException, RemoteDataException
	{
		this(client, file, passive, null);
	}

	public RemoteInputStream(T client, String file, boolean passive,
			RestartData restart) throws IOException, RemoteDataException
	{
		this.client = client;
		this.targetFile = file;
	}
	
	/**
     * Returns the total size of input data.
     *
     * @return -1 if size is unknown.
     */
    public long getSize() {
	return -1;
    }
    
    public int read() throws IOException {
	throw new IOException("Not implemented.");
    }

    /**
     * Aborts transfer. Usually makes sure to 
     * release all resources (sockets, file descriptors)
     * <BR><i>Does nothing by default.</i>
     */
    public void abort() {
    	// FIXME: is this still used/needed?
    }
}
