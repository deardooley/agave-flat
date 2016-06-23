/**
 * 
 */
package org.iplantc.service.transfer;

import java.io.IOException;
import java.io.OutputStream;

import org.iplantc.service.transfer.exceptions.RemoteDataException;

/**
 * @author dooley
 *
 */
public abstract class RemoteOutputStream<T> extends OutputStream {
	
	protected OutputStream		output;

	protected T					client;
	
	protected String			outFile	= "";

	protected RemoteOutputStream() {}
	
	public RemoteOutputStream(T client, String file, boolean passive,
			boolean append) throws IOException, RemoteDataException {
		this.client = client;
		this.outFile = file;
	}
	
	public RemoteOutputStream(T client, String file, boolean passive,
			int type, boolean append) throws IOException, RemoteDataException {
		this.client = client;
		this.outFile = file;
	}
	
    /**
     * Aborts transfer. Usually makes sure to
     * release all resources (sockets, file descriptors)
     * <BR><i>Does nothing by default.</i>
     */
    public void abort() {
        // FIXME: is this still used/needed?
    }

    public void write(int b) throws IOException {
    	throw new IOException("Not implemented.");
    }

}
