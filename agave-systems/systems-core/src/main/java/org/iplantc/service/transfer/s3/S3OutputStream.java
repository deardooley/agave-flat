/**
 * 
 */
package org.iplantc.service.transfer.s3;

import static org.jclouds.blobstore.options.PutOptions.Builder.multipart;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.iplantc.service.systems.Settings;
import org.iplantc.service.transfer.RemoteOutputStream;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.util.MD5Checksum;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.io.payloads.FilePayload;

import com.google.common.hash.HashCode;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;

/**
 * @author dooley
 *
 */
public class S3OutputStream extends RemoteOutputStream<S3Jcloud> {
	private boolean hasWrittenContent;
	private Blob blob;
	private OutputStream stream;
	private File tempFile;
	private String remotePath;
	private Map<String, String> userMetadata;
	
	public S3OutputStream(S3Jcloud client, Blob blob) throws IOException, RemoteDataException 
	{
		hasWrittenContent = false;
		this.client = client;
		this.blob = blob;
		this.remotePath = blob.getMetadata().getName();
		this.userMetadata = blob.getMetadata().getUserMetadata();
		
		this.tempFile = new File(Settings.TEMP_DIRECTORY, "jcld-" + System.currentTimeMillis() + "-" + FilenameUtils.getName(blob.getMetadata().getName()));
		tempFile.createNewFile();
		this.stream = new FileOutputStream(tempFile);
	}
	
	public S3OutputStream(S3Jcloud client, String remotePath) throws IOException, RemoteDataException 
	{
		hasWrittenContent = false;
		this.client = client;
		this.remotePath = remotePath;
		this.tempFile = new File(Settings.TEMP_DIRECTORY, "jcld-" + System.currentTimeMillis() + "-" + FilenameUtils.getName(remotePath));
		tempFile.createNewFile();
		
//		this.blob = client.getBlobStore().blobBuilder(remotePath)
//				  .payload(tempFile)
//			      .build();
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
		if (!hasWrittenContent)
		{
			InputStream in = null;
			try {
//				File test = new File("/Users/dooley/workspace/agave/agave-systems/systems-core/src/test/resources/transfer/test_upload.txt");
				ByteSource payload = Files.asByteSource(tempFile);
				if (this.blob == null)
				{
					this.blob = client.getBlobStore().blobBuilder(remotePath)
						  .payload(payload)
						  .contentDisposition(FilenameUtils.getName(remotePath))
						  .contentLength(tempFile.length())
						  .contentType(new MimetypesFileTypeMap().getContentType(tempFile))
						  .contentMD5((HashCode)null)
//						  .contentMD5(MD5Checksum.getMD5Checksum(tempFile).getBytes())
						  .userMetadata(userMetadata)
						  .build();
				}
				else
				{
					in = payload.openBufferedStream();
					this.blob.setPayload(in);
				}
				
				client.getBlobStore().putBlob(client.containerName, blob, multipart());
			}
			catch (Throwable e) {
				throw new IOException("Failed to write content to S3", e);
			}
			finally {
				hasWrittenContent = true;
				try { in.close(); } catch (Exception e) {}
				FileUtils.deleteQuietly(tempFile);
				
			}
				
		}
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
