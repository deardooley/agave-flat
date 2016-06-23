/**
 * 
 */
package org.iplantc.service.transfer.s3;

import java.io.IOException;
import java.io.InputStream;

import org.iplantc.service.transfer.RemoteInputStream;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.jclouds.blobstore.domain.Blob;

/**
 * @author dooley
 *
 */
public class S3InputStream extends RemoteInputStream<S3Jcloud> {

	private Blob blob;
	private InputStream stream;
	
	public S3InputStream(Blob blob) throws IOException, RemoteDataException 
	{
		this.blob = blob;
		this.stream = blob.getPayload().openStream();
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.RemoteInputStream#getSize()
	 */
	@Override
	public long getSize() {
		return blob.getMetadata().getContentMetadata().getContentLength();
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.RemoteInputStream#read()
	 */
	@Override
	public int read() throws IOException {
		return stream.read();
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#available()
	 */
	@Override
	public int available() throws IOException {
		return stream.available();
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#close()
	 */
	@Override
	public void close() throws IOException {
		stream.close();
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#mark(int)
	 */
	@Override
	public synchronized void mark(int readlimit) {
		stream.mark(readlimit);
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#markSupported()
	 */
	@Override
	public boolean markSupported() {
		return stream.markSupported();
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return stream.read(b, off, len);
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#read(byte[])
	 */
	@Override
	public int read(byte[] b) throws IOException {
		return stream.read(b);
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#reset()
	 */
	@Override
	public synchronized void reset() throws IOException {
		stream.reset();
	}

	/* (non-Javadoc)
	 * @see java.io.InputStream#skip(long)
	 */
	@Override
	public long skip(long n) throws IOException {
		return stream.skip(n);
	}
	
	
	
}
