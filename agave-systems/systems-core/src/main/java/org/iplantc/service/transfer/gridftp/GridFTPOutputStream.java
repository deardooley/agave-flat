package org.iplantc.service.transfer.gridftp;

import java.io.IOException;
import java.io.OutputStream;

import org.globus.ftp.OutputStreamDataSource;
import org.globus.ftp.Session;
import org.globus.ftp.exception.FTPException;
import org.globus.ftp.vanilla.TransferState;
import org.iplantc.service.transfer.RemoteOutputStream;
import org.iplantc.service.transfer.exceptions.RemoteDataException;

public class GridFTPOutputStream extends RemoteOutputStream<GridFTP>
{
	protected OutputStream output;

	protected GridFTP ftp;

	protected TransferState state;

	protected String outFile = "";
	
	public GridFTPOutputStream(GridFTP client, String file, boolean passive,
			boolean append) throws IOException, RemoteDataException
	{
		this(client, file, passive, Session.TYPE_IMAGE, append);
	}

	public GridFTPOutputStream(GridFTP client, String file, boolean passive, int type, boolean append) 
	throws IOException, RemoteDataException
	{
		this.ftp = client;
		this.outFile = file;
		try {
			put(passive, type, file, append);
		} catch (FTPException e) {
			throw new RemoteDataException("Failed to create output stream",e);
		}
	}
	
	public void abort() 
	{
		if (this.output != null) {
			try {
				this.output.close();
			} catch (Exception e) {
			}
		}
		try {
			//this.ftp.close();
			this.ftp.abort();
		} catch (IOException e) {
		} catch (FTPException e) {
		}
	}

	public void close() throws IOException 
	{
		if (this.output != null) {
			try { this.output.close(); } catch (Exception e) {}
			try { this.output.flush(); } catch (Exception e) {}
		}

		try {
			if (this.state != null) {
				this.state.waitForEnd();
			}
		} catch (FTPException e) {
			throw new IOException(e);
		}
	}

	protected void put(boolean passive, int type, String remoteFile, boolean append) 
	throws IOException, FTPException 
	{
		OutputStreamDataSource source = null;
		try {
			this.ftp.setType(type);

			if (passive) {
				this.ftp.setPassive();
				this.ftp.setLocalActive();
			} else {
				this.ftp.setLocalPassive();
				this.ftp.setActive();
			}

			source = new OutputStreamDataSource(GridFTP.MAX_BUFFER_SIZE);

			this.state = this.ftp.asynchPut(remoteFile, source, null, append);

			this.state.waitForStart();

			this.output = source.getOutputStream();
		} catch (FTPException e) {
			if (source != null) {
				source.close();
			}
			close();
			throw e;
		}
	}

	public void write(byte[] msg) throws IOException {
		this.output.write(msg);
	}

	public void write(byte[] msg, int from, int length) throws IOException {
		this.output.write(msg, from, length);
	}

	public void write(int b) throws IOException {
		this.output.write(b);
	}

	public void flush() throws IOException {
		this.output.flush();
	}

}