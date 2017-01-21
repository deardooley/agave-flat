package org.iplantc.service.transfer.sftp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import org.iplantc.service.transfer.RemoteOutputStream;
import org.iplantc.service.transfer.exceptions.RemoteDataException;

import com.sshtools.sftp.SftpStatusException;
import com.sshtools.sftp.SftpClient;
import com.sshtools.ssh.SshException;

public class MaverickSFTPOutputStream extends RemoteOutputStream<MaverickSFTP> {

	private final SftpClient client;
	private final OutputStream out;
	@SuppressWarnings("unused")
	private long m_offset = 0;

	public MaverickSFTPOutputStream(SftpClient client, String remotefile) throws IOException, RemoteDataException
	{
		try
		{
			this.client = client; 
			this.out = client.getOutputStream(remotefile);
			
		}
		catch (SftpStatusException e) {
			if (e.getMessage().toLowerCase().contains("no such file")) {
				throw new FileNotFoundException("No such file or directory");
			} 
			else if (e.getMessage().toLowerCase().contains("permission denied")) {
				throw new RemoteDataException("Permission denied");
			}
			else {
				throw new RemoteDataException("Failed to obtain output stream for " + remotefile, e);
			}
		}
		catch (SshException e)
		{
			throw new IOException("Failed to obtain output stream for " + remotefile, e);
		}
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.RemoteOutputStream#write(int)
	 */
	@Override
	public void write(int b) throws IOException
	{
		byte[] bytes = new byte[] { (byte)b };
		
		out.write(bytes, 0, 1);
	}


	/* (non-Javadoc)
	 * @see java.io.OutputStream#flush()
	 */
	@Override
	public void flush() throws IOException
	{
		out.flush();
	}


	/* (non-Javadoc)
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	@Override
	public void write(byte[] b, int start, int off) throws IOException
	{
		if (off < 0 || start < 0 || start > b.length - off)
			throw new IndexOutOfBoundsException();

		out.write(b, start, off);
	}


	/* (non-Javadoc)
	 * @see java.io.OutputStream#write(byte[])
	 */
	@Override
	public void write(byte[] b) throws IOException
	{
		out.write(b, 0, b.length);
	}

	/**
	 *
	 * @throws IOException
	 */
	@Override
	public void close() throws IOException
	{
		out.close();
		try { client.quit(); } catch (Exception e) {};
	}
}
