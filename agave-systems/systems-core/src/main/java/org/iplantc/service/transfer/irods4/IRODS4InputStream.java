package org.iplantc.service.transfer.irods4;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.globus.ftp.RestartData;
import org.iplantc.service.transfer.RemoteInputStream;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.irods.jargon.core.exception.JargonException;

public class IRODS4InputStream extends RemoteInputStream<IRODS4> {
	private static final Logger	logger	= Logger.getLogger(IRODS4InputStream.class);

	protected InputStream		input;

	protected IRODS4			client;

	protected String			targetFile;

	protected IRODS4InputStream(){}

	public IRODS4InputStream(IRODS4 client, String file, boolean passive)
			throws IOException, RemoteDataException
	{
		this(client, file, passive, null);
	}

	public IRODS4InputStream(IRODS4 client, String file, boolean passive,
			RestartData restart) throws IOException, RemoteDataException
	{
		try
		{
			input = client.getRawInputStream(file);
		}
		catch (JargonException e)
		{
			throw new RemoteDataException("Failed to obtain input stream from " + file, e);
		}
	}

	public long getSize()
	{
		long rep = -1;

		try
		{
			rep = ((org.iplantc.service.transfer.irods4.IRODS4)client).length(this.targetFile);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			logger.debug("Failed to get size", e);
		}
		return rep;
	}

	public void abort()
	{
		try { input.close(); } catch (Exception e) {};
		try { client.disconnect(); } catch (Exception e) {}
	}

	// standard InputStream methods

	public void close() throws IOException
	{
		abort();
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