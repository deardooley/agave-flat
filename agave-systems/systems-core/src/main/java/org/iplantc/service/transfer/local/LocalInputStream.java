/**
 * 
 */
package org.iplantc.service.transfer.local;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.transfer.RemoteInputStream;
import org.iplantc.service.transfer.exceptions.RemoteDataException;

/**
 * @author dooley
 *
 */
public class LocalInputStream extends RemoteInputStream<Local> {

	private static final Logger	log	= Logger.getLogger(LocalInputStream.class);

	protected File targetFile;

	protected LocalInputStream(){}

	public LocalInputStream(Local client, String path) throws RemoteDataException, IOException
	{
		this.targetFile = new File(client.resolvePath(path));
		try {
    		if (targetFile.exists()) {
    		    if (targetFile.canRead()) {
    		        this.input = new BufferedInputStream(new FileInputStream(targetFile));
    		    } else {
    		        throw new RemoteDataException("Permission denied");
    		    }
    		} else {
    		    throw new FileNotFoundException("No such file or folder.");
    		}
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

	public long getSize()
	{
		long rep = -1;

		try
		{
			rep = targetFile.length();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			log.debug("Failed to get size", e);
		}
		return rep;
	}

	public void abort()
	{
		if (this.input != null)
		{
			try {
				this.input.close();
			}
			catch (Exception e) {}
		}
	}

	// standard InputStream methods

	public void close() throws IOException
	{
		if (this.input != null)
		{
			try
			{
				this.input.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
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