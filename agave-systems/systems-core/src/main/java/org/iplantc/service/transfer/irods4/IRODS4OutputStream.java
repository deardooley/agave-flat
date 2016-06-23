package org.iplantc.service.transfer.irods4;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.iplantc.service.transfer.RemoteOutputStream;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.irods.jargon.core.exception.JargonException;

public class IRODS4OutputStream extends RemoteOutputStream<IRODS4> {
	
	private static final Logger log = Logger.getLogger(IRODS4OutputStream.class);
	
	protected IRODS4OutputStream() {}

	public IRODS4OutputStream(IRODS4 client, String remotePath, boolean passive,
			boolean append) throws IOException, RemoteDataException
	{
		this.client = client;
		this.outFile = remotePath;
		try 
		{
			this.output = client.getRawOutputStream(remotePath);
		}
		catch (IOException e) {
			throw e;
		}
		catch (RemoteDataException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to obtain remote output stream for " + remotePath);
		}
	}

	public IRODS4OutputStream(IRODS4 client, String file, boolean passive,
			int type, boolean append) throws IOException, RemoteDataException
	{
		this.outFile = file;
		try {
			this.output = client.getRawOutputStream(file);
		}
		catch (JargonException e) {
			throw new RemoteDataException("Failed to obtain remote output stream for " + file);
		}
	}

	public void abort()
	{
		try { output.close(); } catch (Exception e) {}
		
		// We need to explicity give the user who just created this file 
		// ownership on that file because irods won't do this as that
		// would actually be a reasonable thing to do.
		try { 
			client.setOwnerPermission(client.username, outFile, false); 
		} catch (Exception e) {
			log.error("Failed to set permissions on " + outFile + " after stream was closed", e);
		}
		
		try { client.disconnect(); } catch (Exception e) {}
	}

	public void close() throws IOException
	{
		
		abort();
	}

	public void write(byte[] msg) throws IOException
	{
		this.output.write(msg);
	}

	public void write(byte[] msg, int from, int length) throws IOException
	{
		this.output.write(msg, from, length);
	}

	public void write(int b) throws IOException
	{
		this.output.write(b);
	}

	public void flush() throws IOException
	{
		this.output.flush();
	}

}