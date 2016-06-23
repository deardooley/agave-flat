package org.iplantc.service.transfer.ftp;

import java.io.IOException;

import org.globus.common.ChainedIOException;
import org.globus.ftp.OutputStreamDataSource;
import org.globus.ftp.Session;
import org.globus.ftp.exception.FTPException;
import org.globus.ftp.vanilla.TransferState;
import org.iplantc.service.transfer.RemoteOutputStream;
import org.iplantc.service.transfer.exceptions.RemoteDataException;

public class FTPOutputStream extends RemoteOutputStream<FTP> 
{	
	protected TransferState		state;
	
	protected FTPOutputStream() {}

	public FTPOutputStream(FTP client, String file, boolean passive,
			boolean append) throws IOException, RemoteDataException
	{
		this(client, file, passive, Session.TYPE_IMAGE, append);
	}

	public FTPOutputStream(FTP client, String file, boolean passive,
			int type, boolean append) throws IOException, RemoteDataException
	{
		this.client = client;
		this.outFile = file;
		put(passive, type, file, append);
	}

	public void abort()
	{
		if (this.output != null) {
			try { this.output.close(); } catch (Exception e) {}
		}
		
		try { client.abort(); } catch (IOException | FTPException e) {}
	}

	public void close() throws IOException
	{

		if (this.output != null) {
			try { this.output.close(); } catch (Exception e) {}
		}

		try
		{
			if (this.state != null)
			{
				this.state.waitForEnd();
			}
		}
		catch (FTPException e)
		{
			throw new ChainedIOException("Connection already closed", e);
		}
	}

	protected void put(boolean passive, int type, String remoteFile,
			boolean append) throws IOException, RemoteDataException
	{
		OutputStreamDataSource source = null;
		try
		{
			client.setType(type);
			client.setMode(Session.MODE_STREAM);
			
			client.setDTP(false);
			
			source = new OutputStreamDataSource(2048);

			this.state = client.asynchPut(remoteFile, source, null, append);

			this.state.waitForStart();

			this.output = source.getOutputStream();
		}
		catch (FTPException e)
		{
			if (source != null)
			{
				source.close();
			}
			close();
			throw new RemoteDataException(e);
		}
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