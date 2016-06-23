package org.iplantc.service.transfer.local;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.iplantc.service.transfer.RemoteOutputStream;
import org.iplantc.service.transfer.exceptions.RemoteDataException;

public class LocalOutputStream extends RemoteOutputStream<Local> {
	
	protected LocalOutputStream() {}

	public LocalOutputStream(Local client, String path) throws RemoteDataException, IOException
	{
	    File outFile = new File(client.resolvePath(path));
	    try {
	        output = new BufferedOutputStream(new FileOutputStream(outFile));
	    } catch (FileNotFoundException e) {
	        if (StringUtils.containsIgnoreCase(e.getMessage(), "permission")) {
	            throw new RemoteDataException("Permission denied");
	        } else if (StringUtils.containsIgnoreCase(e.getMessage(), "no such")) {
	            throw e;
	        } else if (StringUtils.containsIgnoreCase(e.getMessage(), "directory")) {
	            throw new RemoteDataException("Failed to obtain input stream for " + path, e);
	        } else {
	            throw e;
	        }
	    }
	}

	public void abort()
	{
		if (this.output != null)
		{
			try {
				this.output.close();
			} catch (Exception e) {}
		}
	}

	public void close() throws IOException
	{
		if (this.output != null)
		{
			try
			{
				this.output.close();
			}
			catch (Exception e)
			{
			}
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