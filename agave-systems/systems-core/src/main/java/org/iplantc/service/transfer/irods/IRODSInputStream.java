package org.iplantc.service.transfer.irods;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.globus.ftp.RestartData;
import org.iplantc.service.transfer.RemoteInputStream;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.irods.jargon.core.exception.JargonException;

public class IRODSInputStream extends RemoteInputStream<IRODS> {
	
	private static final Logger	log	= Logger.getLogger(IRODSInputStream.class);

	protected InputStream		input;

	protected IRODS				client;

	protected String			targetFile;

	protected IRODSInputStream(){}

	public IRODSInputStream(IRODS client, String file, boolean passive)
			throws IOException, RemoteDataException
	{
		this(client, file, passive, null);
	}

	public IRODSInputStream(IRODS client, String file, boolean passive,
			RestartData restart) throws IOException, RemoteDataException
	{
		try {
			log.debug(Thread.currentThread().getName() + Thread.currentThread().getId()  + " opening input stream connection for thread");
			input = client.getRawInputStream(file);
		}
		catch (JargonException e) {
			throw new RemoteDataException("Failed to obtain input stream from " + file, e);
		}
	}

	public long getSize()
	{
		long rep = -1;

		try {
			rep = ((org.iplantc.service.transfer.irods.IRODS)client).length(this.targetFile);
		}
		catch (Exception e) {
			e.printStackTrace();
			log.debug("Failed to get size", e);
		}
		return rep;
	}

	public void abort()
	{
		try { input.close(); } catch (Exception e) {};
		try { client.disconnect(); } catch (Exception e) {}
		log.debug(Thread.currentThread().getName() + Thread.currentThread().getId()  + " aborting input stream connection for thread");
	}

	// standard InputStream methods

	public void close() throws IOException
	{
		abort();
		log.debug(Thread.currentThread().getName() + Thread.currentThread().getId()  + " closing input stream connection for thread");
	}

	public int read(byte[] msg) throws IOException
	{
		return this.input.read(msg);
	}

	public int read(byte[] buf, int off, int len) throws IOException
	{
		return this.input.read(buf, off, len);
	}

	public int read() throws IOException
	{
		return this.input.read();
	}

	public int available() throws IOException
	{
		return this.input.available();
	}

}