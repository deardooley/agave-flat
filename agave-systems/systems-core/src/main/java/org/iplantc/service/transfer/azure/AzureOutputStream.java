/**
 * 
 */
package org.iplantc.service.transfer.azure;

import static org.jclouds.blobstore.options.PutOptions.Builder.multipart;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.io.FilenameUtils;
import org.iplantc.service.transfer.RemoteOutputStream;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.util.MD5Checksum;
import org.jclouds.blobstore.domain.Blob;

import com.google.common.hash.HashCode;

/**
 * @author dooley
 *
 */
public class AzureOutputStream extends RemoteOutputStream<AzureJcloud> {

	private Blob blob;
	private OutputStream stream;
	private File tempFile;
	
	public AzureOutputStream(AzureJcloud client, Blob blob) throws IOException, RemoteDataException 
	{
		this.client = client;
		this.blob = blob;
		this.tempFile = File.createTempFile("jcld-" + blob.getMetadata().getName() + "-" + System.currentTimeMillis(), null);
		this.stream = new FileOutputStream(tempFile);
	}
	
	public AzureOutputStream(AzureJcloud client, String remotePath) throws IOException, RemoteDataException 
	{
		this.client = client;
		this.tempFile = File.createTempFile("jcld-" + FilenameUtils.getName(remotePath) + "-" + System.currentTimeMillis(), null);
		this.blob = client.getBlobStore().blobBuilder(remotePath)
				  .payload(tempFile)
			      .build();
		this.stream = new FileOutputStream(tempFile);
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.transfer.RemoteOutputStream#write(int)
	 */
	@Override
	public void write(int b) throws IOException {
		stream.write(b);
	}

	/* (non-Javadoc)
	 * @see java.io.OutputStream#close()
	 */
	@Override
	public void close() throws IOException {
		stream.close();
		blob.setPayload(tempFile);
		blob.getMetadata().getContentMetadata().setContentLength(tempFile.length());
		blob.getMetadata().getContentMetadata().setContentType(new MimetypesFileTypeMap().getContentType(tempFile));
		blob.getMetadata().getContentMetadata().setContentEncoding("UTF-8");
		try {
			blob.getMetadata().getContentMetadata().setContentMD5(MD5Checksum.getMD5Checksum(tempFile).getBytes());
		}
		catch (Exception e) {
			blob.getMetadata().getContentMetadata().setContentMD5((HashCode)null);
		}
		
		client.getBlobStore().putBlob(client.containerName, blob, multipart());
	}

	/* (non-Javadoc)
	 * @see java.io.OutputStream#flush()
	 */
	@Override
	public void flush() throws IOException {
		stream.flush();
	}

	/* (non-Javadoc)
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	@Override
	public void write(byte[] arg0, int arg1, int arg2) throws IOException {
		stream.write(arg0, arg1, arg2);
	}

	/* (non-Javadoc)
	 * @see java.io.OutputStream#write(byte[])
	 */
	@Override
	public void write(byte[] arg0) throws IOException {
		stream.write(arg0);
	}
}
