package org.iplantc.service.common.representation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.restlet.data.MediaType;
import org.restlet.representation.WriterRepresentation;

public class RemoteDataWriterRepresentation extends WriterRepresentation
{
	private static final Logger log = Logger.getLogger(RemoteDataWriterRepresentation.class);
	
	private InputStream in;
	private RemoteDataClient remoteClient;
	private String localPath;
	private String remotePath;
	
	public RemoteDataWriterRepresentation(RemoteDataClient client, String localPath, String remotePath, MediaType type) {
		super(type);
		this.remoteClient = client;
		this.localPath = localPath;
		this.remotePath = remotePath;
	}
	
	@Override
	public void write(OutputStream out) throws IOException 
	{
		try {
			
			try {
				in = remoteClient.getInputStream(remotePath, true);
			} catch (RemoteDataException e) {
				throw new IOException("Failed to open input stream to remote file", e);
			}
			
			byte[] b = new byte[4096];
			int len = 0;
			
			try {
				while ((len = in.read(b)) >= 0) {
					out.write(b, 0, len);
				}
			} catch (IOException e) {
				log.error("Failed to stream temp file " + localPath, e);
				throw e;
			} 
			
			try {
				remoteClient.delete(remotePath);
			} catch (Exception e1) {
				log.error("Failed to delete temp file " + remotePath);
			}
		} finally {
			try { in.close(); } catch (Exception e) {}
			try { out.close(); } catch (Exception e) {}
			try { remoteClient.disconnect(); } catch(Exception e) {}
		}
	}

	@Override
	public void write(Writer writer) throws IOException 
	{
		throw new NotImplementedException("This is too slow to use.");
//		try 
//		{
//			try {
//				in = remoteClient.getInputStream(remotePath, true);
//			} catch (RemoteDataException e) {
//				throw new IOException("Failed to open input stream to remote file", e);
//			}
//			
//			byte[] b = new byte[4096];
//			int len = 0;
//			
//			while ((len = in.read(b)) >= 0) {
//				writer.write((new String(b)).toCharArray());
//			}
//			
//			
//			try {
//				remoteClient.delete(remotePath);
//			} catch (Exception e1) {
//				log.error("Failed to delete temp file " + remotePath);
//			}
//		} finally {
//			try { in.close(); } catch (Exception e) {}
//			try { writer.close(); } catch (Exception e) {}
//			try { remoteClient.disconnect(); } catch(Exception e) {}
//		}
	}
}