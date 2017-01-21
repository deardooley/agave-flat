package org.iplantc.service.transfer.sftp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.iplantc.service.transfer.RemoteInputStream;
import org.iplantc.service.transfer.exceptions.RemoteDataException;

import com.sshtools.sftp.SftpStatusException;
import com.sshtools.sftp.SftpClient;

public class MaverickSFTPInputStream extends RemoteInputStream<MaverickSFTP> {

	private final SftpClient client;
	private final InputStream in;
	@SuppressWarnings("unused")
	private long m_offset = 0;

	public MaverickSFTPInputStream(SftpClient client, String remotefile) throws IOException, RemoteDataException
	{
		try
		{
			this.client = client; 
			in = client.getInputStream(remotefile);
		}
		catch (SftpStatusException e) {
			if (e.getMessage().toLowerCase().contains("no such file")) {
				throw new FileNotFoundException("No such file or directory");
			} 
			else if (e.getMessage().toLowerCase().contains("permission denied")) {
				throw new RemoteDataException("Permission denied");
			}
			else {
				throw new RemoteDataException("Failed to obtain input stream for " + remotefile, e);
			}
		}
		catch (Exception e)
		{
			throw new IOException("Failed to obtain input stream for " + remotefile, e);
		}
	}


	// public methods
	/**
	 *
	 * @throws IOException
	 */
	@Override
	public void close() throws IOException
	{
		in.close();
		try { client.quit(); } catch (Exception e) {};
	}

	/**
	 *
	 * @return
	 * @throws IOException
	 */
	public int read() throws IOException
	{
		byte[] readBuffer = new byte[1];

		if (read(readBuffer, 0, 1) <= 0)
			return -1;

		return readBuffer[0] & 0xff;
	}

	/**
	 *
	 * @param b
	 * @return
	 * @throws IOException
	 */
	@Override
	public int read(byte[] b) throws IOException
	{
		return read(b, 0, b.length);
	}

	/**
	 *
	 * @param b
	 * @param off
	 * @param len
	 * @return
	 * @throws IOException
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException
	{
		if (off < 0 || len < 0 || len > b.length - off)
			throw new IndexOutOfBoundsException();

		int bytesRead = in.read(b, off, len);

		if (bytesRead <= 0)
			return -1;

		m_offset += bytesRead;

		return bytesRead;
	}
}
