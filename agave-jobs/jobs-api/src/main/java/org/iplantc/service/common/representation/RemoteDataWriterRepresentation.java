package org.iplantc.service.common.representation;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Date;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.systems.exceptions.RemoteCredentialException;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.dao.TransferTaskDao;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.model.TransferTask;
import org.iplantc.service.transfer.model.enumerations.TransferStatusType;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Range;
import org.restlet.data.Request;
import org.restlet.resource.WriterRepresentation;

public class RemoteDataWriterRepresentation extends WriterRepresentation
{
	private static final Logger log = Logger.getLogger(RemoteDataWriterRepresentation.class);

	private InputStream in;
	private RemoteSystem system;
	private RemoteDataClient remoteDataClient;
	private String localPath;
	private String remotePath;
	private TransferTask transferTask = null;

	public RemoteDataWriterRepresentation(RemoteSystem system,
			String localPath, String remotePath, MediaType type,
			Range range, TransferTask transferTask) {
		super(type);
		this.setSystem(system);
		this.localPath = localPath;
		this.remotePath = remotePath;
		this.transferTask = transferTask;
		setRange(range);
		
		Form form = Request.getCurrent().getOriginalRef().getQueryAsForm();
		if (form != null && Boolean.parseBoolean(form.getFirstValue("force", "false"))) {
			setDownloadable(true);
			setDownloadName(FilenameUtils.getName(remotePath));
		}
	}

	@Override
	public long getSize() {
		try {
			if (getRange() != null) {
				if (getRange().getSize() == -1) {
					return getRemoteDataClient().length(remotePath) - getRange().getIndex();
				} else {
					return getRange().getSize();
				}
			} else {
				return getRemoteDataClient().length(remotePath);
			}
		} catch (IOException | RemoteDataException | RemoteCredentialException e) {
			return -1;
		}
	}

	@Override
	public void write(OutputStream out) throws IOException
	{
		long bytesSoFar = 0;
		BufferedOutputStream bout = new BufferedOutputStream(out);

		try
		{
			//TODO: add in transfertask and remotetransferlistener to track data movement for accounting.
			try {
				in = getRemoteDataClient().getInputStream(remotePath, true);
			} catch (RemoteDataException | RemoteCredentialException | IOException e) {
				throw new IOException("Failed to open input stream to remote file", e);
			}

			int bufferSize = 4096;
			byte[] b = new byte[bufferSize];
			int len = 0;

			// we're ready to go, persist the transfer task to record download
			// we do this before transfer so we can monitor from outside the
			// download process itself.
			TransferTaskDao.persist(transferTask);

			int callbackCount = 0;

			// If no range specified
			if (this.getRange() == null)
			{
				transferTask.setTotalSize(getRemoteDataClient().length(remotePath));
				transferTask.setBytesTransferred(0);
				transferTask.setAttempts(1);
				transferTask.setStatus(TransferStatusType.TRANSFERRING);
				transferTask.setStartTime(new Date());
				TransferTaskDao.persist(transferTask);

				while ((len = in.read(b, 0, bufferSize)) > -1) {
					bout.write(b, 0, len);
					bytesSoFar += len;
					callbackCount++;
					if (callbackCount == 8)
					{
						callbackCount = 0;
						transferTask.setBytesTransferred(bytesSoFar);
						TransferTaskDao.persist(transferTask);
					}
				}

				transferTask.setBytesTransferred(bytesSoFar);
				transferTask.setStatus(TransferStatusType.COMPLETED);
				TransferTaskDao.persist(transferTask);

			} else {
				// Skip all input data prior to the index point.
				long skipped = in.skip(this.getRange().getIndex());

                // This should never happen due to earlier bounds check
                if (skipped < this.getRange().getIndex()) {
                    throw new IOException("Requested Range out of bounds");
                }

				// Define a remaining number of bytes
				long bytesRemaining = getSize();

				transferTask.setTotalSize(bytesRemaining);
				transferTask.setBytesTransferred(0);
				transferTask.setAttempts(1);
				transferTask.setStatus(TransferStatusType.TRANSFERRING);
				transferTask.setStartTime(new Date());
				TransferTaskDao.persist(transferTask);

				// write a buffered number of bytes until all of the requested range of data is sent
				while (((len = in.read(b, 0, bufferSize)) > -1) && (bytesRemaining > 0)) {

					if (len > bytesRemaining)
						len = (int)bytesRemaining;

					bout.write(b, 0, len);

					// Reduce the remaining number of bytes by len
					bytesRemaining -= len;

					bytesSoFar += len;
					callbackCount++;
					if (callbackCount == 4)
					{
						callbackCount = 0;
						transferTask.setBytesTransferred(bytesSoFar);
						TransferTaskDao.persist(transferTask);
					}
				}

				transferTask.setBytesTransferred(bytesSoFar);
				transferTask.setStatus(TransferStatusType.COMPLETED);

                // This should never happen due to earlier bounds check
                if (bytesRemaining > 0) {
                    throw new IOException("Requested Range out of bounds");
                }

			}
			bout.flush();
			out.close();
        }
        catch (RemoteDataException e) {
        	transferTask.setStatus(TransferStatusType.FAILED);
			throw new IOException(e);
		}
        catch (IOException e) {
        	transferTask.setStatus(TransferStatusType.FAILED);
        	throw e;
        }
		catch (Exception e) {
			transferTask.setStatus(TransferStatusType.FAILED);
			throw new IOException(e);
		}
        finally
        {
			try { getRemoteDataClient().disconnect(); } catch (Exception e) {}
			try { in.close(); } catch (Exception e) {}
			try { out.close(); } catch (Exception e) {}
			try {
				transferTask.setEndTime(new Date());
				transferTask.setBytesTransferred(bytesSoFar);
				TransferTaskDao.persist(transferTask);
			} catch (Exception e) {}
		}
	}

	@Override
	public void write(Writer writer) throws IOException
	{
		long bytesSoFar = 0;
		
		try
		{
			//TODO: add in transfertask and remotetransferlistener to track data movement for accounting.
			try {
				in = getRemoteDataClient().getInputStream(remotePath, true);
			} catch (RemoteDataException | RemoteCredentialException | IOException e) {
				throw new IOException("Failed to open input stream to remote file", e);
			}

			int bufferSize = 4096;
			byte[] b = new byte[bufferSize];
			int len = 0;

			// we're ready to go, persist the transfer task to record download
			// we do this before transfer so we can monitor from outside the
			// download process itself.
			TransferTaskDao.persist(transferTask);

			int callbackCount = 0;

			// If no range specified
			if (this.getRange() == null)
			{
				transferTask.setTotalSize(getRemoteDataClient().length(remotePath));
				transferTask.setBytesTransferred(0);
				transferTask.setAttempts(1);
				transferTask.setStatus(TransferStatusType.TRANSFERRING);
				transferTask.setStartTime(new Date());
				TransferTaskDao.persist(transferTask);

				while ((len = in.read(b, 0, bufferSize)) > -1) {
					writer.append(b.toString());
					bytesSoFar += len;
					callbackCount++;
					if (callbackCount == 8)
					{
						callbackCount = 0;
						transferTask.setBytesTransferred(bytesSoFar);
						TransferTaskDao.persist(transferTask);
					}
				}

				transferTask.setBytesTransferred(bytesSoFar);
				transferTask.setStatus(TransferStatusType.COMPLETED);
				TransferTaskDao.persist(transferTask);

			} else {
				// Skip all input data prior to the index point.
				long skipped = in.skip(this.getRange().getIndex());

                // This should never happen due to earlier bounds check
                if (skipped < this.getRange().getIndex()) {
                    throw new IOException("Requested Range out of bounds");
                }

				// Define a remaining number of bytes
				int bytesRemaining = (int)this.getRange().getSize();

				transferTask.setTotalSize(bytesRemaining);
				transferTask.setBytesTransferred(0);
				transferTask.setAttempts(1);
				transferTask.setStatus(TransferStatusType.TRANSFERRING);
				transferTask.setStartTime(new Date());
				TransferTaskDao.persist(transferTask);

				// write a buffered number of bytes until all of the requested range of data is sent
				while (((len = in.read(b, 0, bufferSize)) > -1) && (bytesRemaining > 0)) {

					if (len > bytesRemaining)
						len = bytesRemaining;

					writer.append(b.toString());

					// Reduce the remaining number of bytes by len
					bytesRemaining -= len;

					bytesSoFar += len;
					callbackCount++;
					if (callbackCount == 4)
					{
						callbackCount = 0;
						transferTask.setBytesTransferred(bytesSoFar);
						TransferTaskDao.persist(transferTask);
					}
				}

				transferTask.setBytesTransferred(bytesSoFar);
				transferTask.setStatus(TransferStatusType.COMPLETED);

                // This should never happen due to earlier bounds check
                if (bytesRemaining > 0) {
                    throw new IOException("Requested Range out of bounds");
                }

			}
        }
        catch (RemoteDataException e) {
        	transferTask.setStatus(TransferStatusType.FAILED);
			throw new IOException(e);
		}
        catch (IOException e) {
        	transferTask.setStatus(TransferStatusType.FAILED);
        	throw e;
        }
		catch (Exception e) {
			transferTask.setStatus(TransferStatusType.FAILED);
			throw new IOException(e);
		}
        finally
        {
			try { getRemoteDataClient().disconnect(); } catch (Exception e) {}
			try { in.close(); } catch (Exception e) {}
			
			try {
				transferTask.setEndTime(new Date());
				transferTask.setBytesTransferred(bytesSoFar);
				TransferTaskDao.persist(transferTask);
			} catch (Exception e) {}
		}
	}

	/**
	 * @return the remoteDataClient
	 * @throws RemoteCredentialException 
	 * @throws RemoteDataException 
	 * @throws IOException 
	 */
	public RemoteDataClient getRemoteDataClient() 
	throws RemoteDataException, RemoteCredentialException, IOException 
	{
		if (this.remoteDataClient == null) {
			this.remoteDataClient = system.getRemoteDataClient();
			this.remoteDataClient.authenticate();
		}
		
		return remoteDataClient;
	}

	/**
	 * @param remoteDataClient the remoteDataClient to set
	 */
	public void setRemoteDataClient(RemoteDataClient remoteDataClient) {
		this.remoteDataClient = remoteDataClient;
	}

	/**
	 * @return the system
	 */
	public RemoteSystem getSystem() {
		return system;
	}

	/**
	 * @param system the system to set
	 */
	public void setSystem(RemoteSystem system) {
		this.system = system;
	}
}
