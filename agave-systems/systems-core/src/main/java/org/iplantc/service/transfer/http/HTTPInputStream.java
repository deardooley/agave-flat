/**
 * 
 */
package org.iplantc.service.transfer.http;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.log4j.lf5.util.StreamUtils;
import org.globus.ftp.RestartData;
import org.iplantc.service.transfer.RemoteInputStream;
import org.iplantc.service.transfer.exceptions.RemoteDataException;

/**
 * @author dooley
 *
 */
public class HTTPInputStream extends RemoteInputStream<HTTP> 
{
	private CloseableHttpResponse response = null;
	
	private long length = -1;
	
	public HTTPInputStream(HTTP client, String remotepath, boolean passive, RestartData restart) 
	throws IOException, RemoteDataException
	{
		try 
		{
			response = client.doGet(remotepath);
			
		    StatusLine statusLine = response.getStatusLine();
	    	if (statusLine.getStatusCode() >= 200 && statusLine.getStatusCode() < 300) {
	    		if (response.containsHeader("Content-Length")) {
	    			length = NumberUtils.toLong(response.getFirstHeader("Content-Length").getValue(), -1);
	    			client.setLength(remotepath, length);
	    		}
	    		this.input = response.getEntity().getContent();
	    	} else if (statusLine.getStatusCode() == 404) {
	    		throw new FileNotFoundException("File or folder ");
	    	} else if (statusLine.getStatusCode() == 401 || statusLine.getStatusCode() == 403) {
	    		InputStream in = null;
	    		try {
	    			in = response.getEntity().getContent();
	    			String content = new String(StreamUtils.getBytes(in));
	    			throw new RemoteDataException("Failed to get " + remotepath + 
	    					" due to insufficient privileges: " + content);
	    		} catch (RemoteDataException e) {
	    			throw e;
	    		} catch(Throwable e) {
	    			throw new RemoteDataException("Failed to get " + remotepath + 
	    					" due to insufficient privileges.");
	    		}
	    		finally {
	    			try { in.close(); } catch (Exception e) {}
	    		}
	    	} else {
	    		throw new IOException(statusLine.getReasonPhrase());
	    	}
		}
		catch (IOException e) {
			throw e;
		}
		catch (RemoteDataException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RemoteDataException("Failed to establish input stream to " + remotepath, e);
		}
	}

	public HTTPInputStream(HTTP client, String remotepath, boolean passive)
			throws IOException, RemoteDataException
	{
		this(client, remotepath, passive, null);
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

	/* (non-Javadoc)
	 * @see java.io.InputStream#close()
	 */
	@Override
	public void close() throws IOException
	{
		super.close();
		
		try { input.close(); } catch (Exception e) {}
		
		try { 
			response.close(); 
		} 
		catch (Exception e) {
			throw new IOException ("Failed to close input stream and response.", e);
		}
	}
}
