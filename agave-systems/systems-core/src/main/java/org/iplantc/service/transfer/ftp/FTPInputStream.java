package org.iplantc.service.transfer.ftp;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.globus.ftp.InputStreamDataSink;
import org.globus.ftp.RestartData;
import org.globus.ftp.Session;
import org.globus.ftp.exception.FTPException;
import org.globus.ftp.vanilla.TransferState;
import org.iplantc.service.transfer.RemoteInputStream;

public class FTPInputStream extends RemoteInputStream<FTP> {
	private static final Logger	log	= Logger.getLogger(FTPInputStream.class);

	protected InputStream		input;

	protected TransferState		state;

	protected String			targetFile;

	protected FTPInputStream(){}

	public FTPInputStream(FTP client, String file, boolean passive)
			throws IOException, FTPException
	{
		this(client, file, passive, null);
	}

	public FTPInputStream(FTP client, String file, boolean passive,
			RestartData restart) throws IOException, FTPException
	{
		this.client = client;
		this.targetFile = file;
		get(passive, restart, file);
	}

	protected void get(boolean passive, RestartData restart, String remoteFile)
			throws IOException, FTPException
	{
		InputStreamDataSink sink = null;

		try
		{
			client.setType(Session.TYPE_IMAGE);
			client.setMode(Session.MODE_STREAM);
			
			if (passive)
			{
				client.setPassive();
				client.setLocalActive();
			}
			else
			{
				try {
					client.setActive(client.setLocalPassive());
				} catch (Exception e) {
					client.setPassive();
					client.setLocalActive();
				}
			}
			
			if (null != restart)
			{
				client.setRestartMarker(restart);
			}

			sink = new InputStreamDataSink();
			this.input = sink.getInputStream();
			this.state = client.asynchGet(remoteFile, sink, null);
			this.state.waitForStart();
		}
		catch (FTPException e)
		{
			if (sink != null)
			{
				sink.close();
			}
			close();
			throw e;
		}
	}

	public long getSize()
	{
		long rep = -1;

		try
		{
			rep = client.getSize(this.targetFile);
		}
		catch (Exception e)
		{
			log.debug("Failed to get size", e);
		}
		// System.out.println(rep+" is the size");
		return rep;
	}

	public void abort()
	{
		if (this.input != null) {
			try { this.input.close(); } catch (Exception e) {}
		}
		/*
		 * try { (((org.iplantc.service.jobs.io.ftp.FTP)client)).abort(); } catch (IOException e) { } catch
		 * (FTPException e) { }
		 */
	}

	// standard InputStream methods

	public void close() throws IOException
	{

		if (this.input != null)
		{
			try { this.input.close(); } catch (Exception e) {}
		}

		try
		{
			if (this.state != null)
			{
				long s = System.currentTimeMillis();
				// this.state.waitForEnd();
				wait(1000);
				System.out.println(this.targetFile + " wait for: "
						+ ( System.currentTimeMillis() - s ));
			}
		}
		catch (Exception e)
		{
			// throw new ChainedIOException(
			// SGGCResourceBundle
			// .getResourceString(ResourceName.KEY_EXCEPTION_IOSTREAM_CLOSE),
			// e);
		}

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