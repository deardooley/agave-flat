/**
 *
 */
package org.iplantc.service.transfer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.globus.ftp.GridFTPSession;
import org.hibernate.HibernateException;
import org.iplantc.service.systems.exceptions.RemoteCredentialException;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.transfer.dao.TransferTaskDao;
import org.iplantc.service.transfer.exceptions.RangeValidationException;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.iplantc.service.transfer.exceptions.RemoteDataSyntaxException;
import org.iplantc.service.transfer.exceptions.TransferException;
import org.iplantc.service.transfer.gridftp.GridFTP;
import org.iplantc.service.transfer.local.Local;
import org.iplantc.service.transfer.model.Range;
import org.iplantc.service.transfer.model.TransferTask;
import org.iplantc.service.transfer.model.enumerations.TransferStatusType;

/**
 * @author dooley
 *
 */
public class URLCopy
{
	private static Logger log = Logger.getLogger(URLCopy.class);
//	private static final Counter activeTransfers = CommonMetrics.addCounter(URLCopy.class, "active");
//	private static final Map<Class, Map<Class, Counter>> sourceDestCounters = new HashMap<Class, Map<Class, Counter>>();
//	private static final Map<Class, Counter> totalCounters = new HashMap<Class, Counter>();
//	static {
//		Class[] remoteDataClientClasses = {SFTP.class, GridFTP.class, IRODS.class, FTP.class, Local.class, S3.class};
//		for (Class clazz: Arrays.asList(remoteDataClientClasses)) {
//			Map<Class, Counter> sourceCounters = new HashMap<Class, Counter>();
//			for (Class clazz2: Arrays.asList(remoteDataClientClasses)) {
//				if (!StringUtils.equals(clazz.getName(), clazz2.getName())) {
//					sourceCounters.put(clazz2, CommonMetrics.addCounter(clazz, "src." + clazz2.getSimpleName()));
//				}
//			}
//			sourceDestCounters.put(clazz, sourceCounters);
//			totalCounters.put(clazz, CommonMetrics.addCounter(clazz, "total"));
//		}
//	}

	private TransferTask task;
	private RemoteDataClient sourceClient;
	private RemoteDataClient destClient;
	private AtomicBoolean killed = new AtomicBoolean(false);

//	public URLCopy(RemoteSystem sourceSystem, RemoteSystem destSystem) 
//	throws RemoteDataException, RemoteCredentialException 
//	{
//		this.sourceClient = sourceSystem.getRemoteDataClient();
//		this.destClient = destSystem.getRemoteDataClient();
//	}
	
	public URLCopy(RemoteDataClient sourceClient, RemoteDataClient destClient)
	{
		this.sourceClient = sourceClient;
		this.destClient = destClient;
		
	}

	/**
	 * @return the killed
	 */
	public synchronized boolean isKilled()
	{
		return this.killed.get();
	}

	/**
	 * @param killed the killed to set
	 */
	public synchronized void setKilled(boolean killed)
	{
		this.killed.set(killed);
		if ((sourceClient instanceof GridFTP) && (destClient instanceof GridFTP)) {
			try { ((GridFTP)sourceClient).abort(); } catch (Exception e) { }
			try { ((GridFTP)destClient).abort(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Threadsafe check for killed copy command either through the
	 * thread being explicitly killed or the transfertask status
	 * being set to cancelled.
	 * @param listener
	 * @throws ClosedByInterruptException
	 */
	protected void checkCancelled(RemoteTransferListener listener) 
	throws ClosedByInterruptException 
	{
		if (isKilled() || listener.isCancelled()) {
			throw new ClosedByInterruptException();
		}
	}

	/**
     * Copies one file item to another leveraging the {@link RemoteDataClient} interface.
     * Directory copy is supported and authentication is handled automatically.The algorithm 
     * used to copy is chosen based on the 
     * protocol, file size, and locality of the data. Progress is written to the transfer task
     * via a {@link RemoteTransferListener}
     *
     * @param srcPath
     * @param destPath
     * @param transferTask
     * @throws RemoteDataException
     * @throws IOException
     * @throws TransferException
     * @throws ClosedByInterruptException
     */
    public TransferTask copy(String srcPath, String destPath, TransferTask transferTask)
    throws RemoteDataException, RemoteDataSyntaxException, IOException, TransferException, ClosedByInterruptException
    {
        return copy(srcPath, destPath, transferTask, null);
    }
    
	/**
	 * Copies one file item to another leveraging the {@link RemoteDataClient} interface.
	 * Directory copy is supported and authentication is handled automatically.The algorithm 
	 * used to copy is chosen based on the 
	 * protocol, file size, and locality of the data. Progress is written to the transfer task
	 * via a {@link RemoteTransferListener}
	 *
	 * @param srcPath
	 * @param destPath
	 * @param transferTask
	 * @param exclusions blacklist of paths relative to {@code srcPath} not to copy
     * @throws RemoteDataException
	 * @throws IOException
	 * @throws TransferException
	 * @throws ClosedByInterruptException
	 */
	public TransferTask copy(String srcPath, String destPath, TransferTask transferTask, List<String> exclusions)
	throws RemoteDataException, RemoteDataSyntaxException, IOException, TransferException, ClosedByInterruptException
	{
		if (transferTask == null) {
			throw new TransferException("TransferTask cannot be null. Please provide"
					+ "a valid transfer task to track this operation.");
		} else if (transferTask.getId() == null) {
			throw new TransferException("TransferTask does not have a valid id. "
					+ "Please persiste the transfer taks and attempt the operation again.");
		}
		
		if (exclusions == null) {
		    exclusions = new ArrayList<String>();
		}
		
		try
		{
		    // if we are transferring a directory
			if (sourceClient.isDirectory(srcPath))
			{    
			    // now we start the transfer. This will keep the transfers and history apis up to date with 
				// what is going on in this transfer as an aggregate and as a unique transfer.
				if (transferTask != null) {
					transferTask.setStatus(TransferStatusType.TRANSFERRING);
					TransferTaskDao.persist(transferTask);
				}
				
				// if this is a server-side transfer
				if (sourceClient.equals(destClient))
                {   
				    RemoteTransferListener listener = null;
	                listener = new RemoteTransferListener(transferTask);
	                
	                // we can potentially make a server-side copy here. attempt that first 
	                // before making an unnecessary round-trip
                    sourceClient.copy(srcPath + "/", destPath, listener);
                    
                    // everything was copied over server side, so delete whatever was in the 
                    // list of exclusions
                    for (String excludedOutputFile: exclusions) {
                        try {
                            destClient.delete(destPath + "/" + excludedOutputFile);
                        } catch (Exception e) {}
                    }
	                
                    transferTask = listener.getTransferTask();
                    
                    if (transferTask != null) {
                        transferTask = TransferTaskDao.getById(transferTask.getId());
                        transferTask.setEndTime(new Date());
                        transferTask.setTotalSize(sourceClient.length(srcPath));
                        transferTask.setBytesTransferred(transferTask.getTotalSize());
                        transferTask.setLastUpdated(new Date());
                        transferTask.setStatus(TransferStatusType.COMPLETED);
                        TransferTaskDao.persist(transferTask);
                    }
                    
                    return transferTask;
                }
				else
				{
				    // create remote directory if it does not exist
	                if (!destClient.doesExist(destPath))
	                {
	                    destClient.mkdirs(destPath, transferTask.getOwner());
//	                    
//	                    // be progressive about maintaining permission mirroring
//	                    if (destClient.isPermissionMirroringRequired()) {
//	                        destClient.setOwnerPermission(destClient.getUsername(), destPath, true);
//	                        destClient.setOwnerPermission(transferTask.getOwner(), destPath, true);
//	                    }
	                }
								
    				// we need to thread this transfer or throw each transfer into queue for processing
    				// via distributed workers. Serially doing this is too slow.
    				for(RemoteFileInfo fileInfo: sourceClient.ls(srcPath))
    				{
    					if (isKilled()) {
    						if (transferTask != null) {
    							log.debug("Transfer task " + task.getUuid() + " was killed by an external thread. Aborting traversal at " + srcPath + File.separator + fileInfo.getName());
    							
    							throw new ClosedByInterruptException();
    						} else {
    							log.debug("Transfer of " + srcPath + " to " + destPath + " was killed by an external thread. Aborting traversal at " + srcPath + File.separator + fileInfo.getName());
    						}
    						break;
    					} 
    					
    					if (StringUtils.equals(fileInfo.getName(), ".") || StringUtils.equals(fileInfo.getName(), "..")) continue;
    
    					String childSrcPath = srcPath + File.separator + fileInfo.getName();
    					
    					if (exclusions.contains(childSrcPath)) continue;
    					
    					String childDestPath = destPath + File.separator + fileInfo.getName();
    
    					String srcUri = "agave://" + getSystemId(transferTask.getSource()) + "/" + childSrcPath;
    					String destUri = "agave://" + getSystemId(transferTask.getDest()) + "/" + childDestPath;
    
    					TransferTask childTransferTask = TransferTaskDao.getChildTransferTask(transferTask.getId(), srcUri, destUri, transferTask.getOwner());
    
    					if (childTransferTask == null)
    					{
    						childTransferTask = new TransferTask(
    							srcUri,
    							destUri,
    							transferTask.getOwner(),
    							transferTask,
    							transferTask.getRootTask() == null ? transferTask : transferTask.getRootTask());
    					}
    					else if (childTransferTask.getEndTime() == null || !fileInfo.isDirectory())
    					{
    						// file was already copied successfully
    						continue;
    					}
    					else
    					{
    						// file may or may not have started, but it did not complete
    						// TODO: support restart where possible
    						childTransferTask.setAttempts(childTransferTask.getAttempts() + 1);
    					}
    
    					TransferTaskDao.persist(childTransferTask);
    					childTransferTask = copy(childSrcPath, childDestPath, childTransferTask);
    					childTransferTask = TransferTaskDao.getById(childTransferTask.getId());
    					
    					transferTask.updateSummaryStats(childTransferTask);
    					try {
    						// this should always succeed.
    						TransferTaskDao.updateProgress(transferTask);
    					}
    					catch (HibernateException | TransferException e) {
    						// on error, the parent task is likely stale. 
    						// update the transferTask by merging with the official
    						// record and persist again.
    						transferTask = TransferTaskDao.merge(transferTask);
    						TransferTaskDao.persist(transferTask);
    					}
    					
    					if (childTransferTask != null && 
    							TransferStatusType.CANCELLED == childTransferTask.getStatus()) {
    						transferTask.setStatus(TransferStatusType.CANCELLED);
    						break;
    					}
    				}
    				
    				if (transferTask != null) {
    				    if (isKilled()) {
    				        transferTask.setStatus(TransferStatusType.CANCELLED);
    				    } else {
    				        transferTask.setStatus(TransferStatusType.COMPLETED);
    				    }
    				    
    					transferTask.setEndTime(new Date());
    					
    					TransferTaskDao.persist(transferTask);
    				}
    				
    				return transferTask;
				}
			}
			else
			{
			    RemoteTransferListener listener = null;
			    listener = new RemoteTransferListener(transferTask);
				if (StringUtils.equals(FilenameUtils.getName(srcPath), ".") ||
						StringUtils.equals(FilenameUtils.getName(srcPath), ".."))
				{
					// skip current directory and parent to avoid infinite loops and
					// full file system copies.
				}
				else if (sourceClient.equals(destClient))
				{
					
					sourceClient.copy(srcPath, destPath, listener);
					
					transferTask = listener.getTransferTask();
					
					if (transferTask != null) {
						transferTask = TransferTaskDao.getById(transferTask.getId());
						transferTask.setEndTime(new Date());
						transferTask.setTotalSize(sourceClient.length(srcPath));
						transferTask.setBytesTransferred(transferTask.getTotalSize());
						transferTask.setLastUpdated(new Date());
						transferTask.setStatus(TransferStatusType.COMPLETED);
						TransferTaskDao.persist(transferTask);
					}
				}
				else if (sourceClient.isThirdPartyTransferSupported() &&
						destClient.isThirdPartyTransferSupported() &&
						sourceClient.getClass().equals(destClient.getClass()))
				{
					dothirdPartyTransfer(srcPath, destPath, listener);
				}
				else
				{
				    listener = new RemoteTransferListener(transferTask);
					
				    try
					{
				        long srcFileLength = sourceClient.length(srcPath);
				        long availableBytes = new File("/").getUsableSpace();
                        
				        if (Settings.ALLOW_RELAY_TRANSFERS 
				                && srcFileLength < (Settings.MAX_RELAY_TRANSFER_SIZE * Math.pow(2, 30))) 
						{
				            if (availableBytes > (srcFileLength + (5*Math.pow(2, 30)))) 
				            {
				                log.debug("Local disk has " + availableBytes + " unused bytes  prior to "
				                        + "relay transfer of " + srcFileLength + " bytes for transfer task " 
				                        + transferTask.getUuid() + ". Relay transfer will be allowed.");
				                relayTransfer(srcPath, destPath, transferTask);
						    } 
				            else 
				            {
						        log.debug("Local disk has insufficient space (" + availableBytes + 
						                " < " + srcFileLength + ") for relay transfer of transfer task " 
						                + transferTask.getUuid() + ". Switching to proxy transfer instead.");
						        proxyTransfer(srcPath, destPath, listener);
						    }
						 } 
				        else {
						 	proxyTransfer(srcPath, destPath, listener);
						}
				        
				        transferTask = listener.getTransferTask();
				        
				        if (transferTask != null) {
	    				    if (isKilled()) {
	    				        transferTask.setStatus(TransferStatusType.CANCELLED);
	    				    } else {
	    				        transferTask.setStatus(TransferStatusType.COMPLETED);
	    				    }
	    				    
	    					transferTask.setEndTime(new Date());
	    					
	    					TransferTaskDao.persist(transferTask);
	    				}
					}
					catch (ClosedByInterruptException e) 
					{
						if (transferTask != null) {
							try {
								TransferTaskDao.cancelAllRelatedTransfers(transferTask.getId());
							} catch (TransferException e1) {
								Thread.currentThread().interrupt();
								throw new RemoteDataException("Failed to cancel related transfer tasks.", e1);
							}
						}
						Thread.currentThread().interrupt();
						throw e;
					}
					catch (TransferException e)
					{
						throw new RemoteDataException("Failed to udpate transfer record.", e);
					}
				}
				
				return listener.getTransferTask();
			}
		}
		finally
		{
			try
			{
				if (destClient.isPermissionMirroringRequired()) {
					destClient.setOwnerPermission(destClient.getUsername(), destPath, true);
					destClient.setOwnerPermission(transferTask.getOwner(), destPath, true);
				}
			}
			catch (Exception e)
			{
				log.error("Failed to set permissions on " + destClient.getHost() + " for user " + transferTask.getOwner(), e);
			}
		}
	}

	/**
	 * Proxies a file/folder transfer from source to destination by using the underlying
	 * {@link RemoteDataClient#get(String,String, RemoteTransferListener)} and {@link RemoteDataClient#put(String,String, RemoteTransferListener)} 
	 * methods to stage the data to the local host, then push to the destination system.
	 * This can be significantly faster than the standard {@link #proxyTransfer(String, String, RemoteTransferListener)}
	 * method when the underlying protocols support parallelism and/or threading. Care must
	 * be taken with this approach to properly check that there is available disk space to
	 * perform the copy.
	 * 
	 * @param srcPath
	 * @param destPath
	 * @param remoteTransferListener
	 * @throws RemoteDataException
	 * @throws ClosedByInterruptException
	 */
	private void relayTransfer(String srcPath, String destPath, TransferTask aggregateTransferTask) 
	throws RemoteDataException, ClosedByInterruptException 
	{
		File tmpFile = null;
		File tempDir = null;
		TransferTask srcChildTransferTask = null;
        RemoteTransferListener srcChildRemoteTransferListener = null;
        TransferTask destChildTransferTask = null;
        RemoteTransferListener destChildRemoteTransferListener = null;
        
		try
		{
		    if (sourceClient instanceof Local) 
		    {
		        tmpFile = new File(sourceClient.resolvePath(srcPath));
		        tempDir = tmpFile.getParentFile();
		        
		        log.debug(String.format(
		                "Skipping first leg of relay transfer for task %s. %s to %s . Protocol: %s => %s", 
                        aggregateTransferTask.getUuid(),
                        aggregateTransferTask.getSource(), 
                        "file://" + tmpFile.getAbsolutePath(),
                        getProtocolForClass(destClient.getClass()),
                        "local"));
		    } 
		    else 
		    {
    			tempDir = new File(FileUtils.getTempDirectory(), 
    					DigestUtils.md5Hex(srcPath) + "-" + System
    					.currentTimeMillis() + ".relay.tmp");
    			
    			if (destClient instanceof Local) 
    			{
    			    tmpFile = new File(destClient.resolvePath(destPath));
    			    tempDir = tmpFile.getParentFile();
    			} 
    			else 
    			{
        			tempDir.mkdirs();
        			tmpFile = new File(tempDir, FilenameUtils.getName(srcPath));
        			tmpFile.createNewFile();
    			}
    			
    			log.debug(String.format(
    			        "Beginning first leg of relay transfer for task %s. %s to %s . Protocol: %s => %s", 
    			        aggregateTransferTask.getUuid(),
    			        aggregateTransferTask.getSource(),
    			        "file://" + tmpFile.getAbsolutePath(), 
    					getProtocolForClass(sourceClient.getClass()),
    					"local"));
    			try
    			{
    			    srcChildTransferTask = new TransferTask(
    			            aggregateTransferTask.getSource(),
    			            "https://workers.prod.agaveapi.co/" + tmpFile.getAbsolutePath(),
    			            aggregateTransferTask.getOwner(),
    			            aggregateTransferTask,
    			            aggregateTransferTask);
    			    
    			    TransferTaskDao.persist(srcChildTransferTask);
    			    srcChildRemoteTransferListener = new RemoteTransferListener(srcChildTransferTask);
    			    
    				sourceClient.get(srcPath, tmpFile.getAbsolutePath(), 
    				        srcChildRemoteTransferListener);
    				
    				srcChildTransferTask = srcChildRemoteTransferListener.getTransferTask();
    				
    				aggregateTransferTask.updateSummaryStats(srcChildTransferTask);
    				
    				if (isKilled()) {
    					srcChildTransferTask.setStatus(TransferStatusType.CANCELLED);
				    } else {
				    	srcChildTransferTask.setStatus(TransferStatusType.COMPLETED);
				    }
				    
    				srcChildTransferTask.setEndTime(new Date());
					
					TransferTaskDao.updateProgress(srcChildTransferTask);
					
				    // must be in here as the LOCAL files will not have a src transfer listener associated with them.
    				checkCancelled(srcChildRemoteTransferListener); 
    	            
    			}
    			catch (RemoteDataException e) {
    			    
    				try {
    					if (srcChildTransferTask != null) {
    						srcChildTransferTask.setStatus(TransferStatusType.FAILED);
    						srcChildTransferTask.setEndTime(new Date());
    						TransferTaskDao.updateProgress(srcChildTransferTask);
    					}
    				}
    				catch (Throwable t) {
    					log.error("Failed to set status of relay source child task to failed.", t);
    				}
    				
    				log.debug(String.format(
                            "Failed first leg of relay transfer for task %s. %s to %s . Protocol: %s => %s", 
                            aggregateTransferTask.getUuid(),
                            aggregateTransferTask.getSource(),
                            "file://" + tmpFile.getAbsolutePath(), 
                            getProtocolForClass(sourceClient.getClass()),
                            "local"));
    				throw e;
    			}
    			catch (Throwable e)
    			{
    				try {
    					if (srcChildTransferTask != null) {
    						srcChildTransferTask.setStatus(TransferStatusType.FAILED);
    						srcChildTransferTask.setEndTime(new Date());
    						TransferTaskDao.updateProgress(srcChildTransferTask);
    					}
    				}
    				catch (Throwable t) {
    					log.error("Failed to set status of relay source child task to failed.", t);
    				}
    				
    			    log.debug(String.format(
                            "Failed first leg of relay transfer for task %s. %s to %s . Protocol: %s => %s", 
                            aggregateTransferTask.getUuid(),
                            aggregateTransferTask.getSource(),
                            "file://" + tmpFile.getAbsolutePath(), 
                            getProtocolForClass(sourceClient.getClass()),
                            "local"));
    				// stuff happens, what are you going to do.
    				throw new RemoteDataException("Transfer failed from " + sourceClient.getUriForPath(srcPath), e);
    			}
		    }
			
			if (!((sourceClient instanceof Local) && (destClient instanceof Local))) 
            {
			    try
    			{
			        log.debug(String.format(
	                        "Beginning second leg of relay transfer for task %s. %s to %s . Protocol: %s => %s", 
	                        aggregateTransferTask.getUuid(),
	                        "file://" + tmpFile.getAbsolutePath(), 
                            aggregateTransferTask.getDest(),
	                        "local",
	                        getProtocolForClass(destClient.getClass())));
			        
			        destChildTransferTask = new TransferTask(
    				        "https://workers.prod.agaveapi.co/" + tmpFile.getAbsolutePath(),
                            aggregateTransferTask.getDest(),
                            aggregateTransferTask.getOwner(),
                            aggregateTransferTask,
                            aggregateTransferTask);
                    
                    TransferTaskDao.persist(destChildTransferTask);
                    
                    destChildRemoteTransferListener = new RemoteTransferListener(destChildTransferTask);
                    
    				destClient.put(tmpFile.getAbsolutePath(), destPath, 
    				        destChildRemoteTransferListener);
    				
    				destChildTransferTask = destChildRemoteTransferListener.getTransferTask();
    				
    				aggregateTransferTask.updateSummaryStats(destChildTransferTask);
    				
    				if (isKilled()) {
    					destChildTransferTask.setStatus(TransferStatusType.CANCELLED);
				    } else {
				    	destChildTransferTask.setStatus(TransferStatusType.COMPLETED);
				    }
				    
    				destChildTransferTask.setEndTime(new Date());
					
					TransferTaskDao.updateProgress(destChildTransferTask);
					
    			}
    			catch (RemoteDataException e) {
    				try {
    					if (destChildTransferTask != null) {
    						destChildTransferTask.setStatus(TransferStatusType.FAILED);
    						destChildTransferTask.setEndTime(new Date());
    						TransferTaskDao.updateProgress(destChildTransferTask);
    					}
    				}
    				catch (Throwable t) {
    					log.error("Failed to set status of relay dest child task to failed.", t);
    				}
    				
    			    log.debug(String.format(
                            "Failed second leg of relay transfer for task %s. %s to %s . Protocol: %s => %s", 
                            aggregateTransferTask.getUuid(),
                            "file://" + tmpFile.getAbsolutePath(), 
                            aggregateTransferTask.getDest(),
                            "local",
                            getProtocolForClass(destClient.getClass())));
    				throw e;
    			}
    			catch (Throwable e)
    			{
    				// fail the destination transfer task
    				try {
    					if (destChildTransferTask != null) {
    						destChildTransferTask.setStatus(TransferStatusType.FAILED);
    						destChildTransferTask.setEndTime(new Date());
    						TransferTaskDao.updateProgress(destChildTransferTask);
    					}
    				}
    				catch (Throwable t) {
    					log.error("Failed to set status of relay dest child task to failed.", t);
    				}
    				
    			    log.debug(String.format(
                            "Failed second leg of relay transfer for task %s. %s to %s . Protocol: %s => %s", 
                            aggregateTransferTask.getUuid(),
                            "file://" + tmpFile.getAbsolutePath(), 
                            aggregateTransferTask.getDest(),
                            "local",
                            getProtocolForClass(destClient.getClass())));
    				throw new RemoteDataException("Transfer failed to " + sourceClient.getUriForPath(srcPath) + 
                            " using " + destClient.getClass().getSimpleName(), e);
    			}
            }
			else {
			    log.debug(String.format(
                        "Skipping second leg of relay transfer for task %s. %s to %s. Protocol: %s => %s", 
                        aggregateTransferTask.getUuid(),
                        "file://" + tmpFile.getAbsolutePath(),
                        aggregateTransferTask.getDest(), 
                        "local",
                        getProtocolForClass(destClient.getClass())));
			    
			    destChildTransferTask = new TransferTask(
				        "https://workers.prod.agaveapi.co/" + tmpFile.getAbsolutePath(),
                        aggregateTransferTask.getDest(),
                        aggregateTransferTask.getOwner(),
                        aggregateTransferTask,
                        aggregateTransferTask);
			    
			    destChildTransferTask.setStartTime(destChildTransferTask.getCreated());
			    destChildTransferTask.setEndTime(destChildTransferTask.getCreated());
			    destChildTransferTask.setBytesTransferred(0);
			    destChildTransferTask.setAttempts(0);
			    destChildTransferTask.setLastUpdated(destChildTransferTask.getCreated());
			    if (srcChildTransferTask != null) {
			    	destChildTransferTask.setTotalFiles(srcChildTransferTask.getTotalFiles());
			    	destChildTransferTask.setTotalSize(srcChildTransferTask.getTotalSize());
			    	destChildTransferTask.setTotalSkippedFiles(srcChildTransferTask.getTotalSkippedFiles());
			    	destChildTransferTask.setTransferRate(srcChildTransferTask.getTransferRate());
			    }
			    else {
			    	destChildTransferTask.setTotalFiles(1);
			    	destChildTransferTask.setTotalSize(tmpFile.length());
			    	destChildTransferTask.setTotalSkippedFiles(0);
			    	destChildTransferTask.setTransferRate(0);
			    }
			    
                TransferTaskDao.persist(destChildTransferTask);
                
                aggregateTransferTask.updateSummaryStats(destChildTransferTask);
			}
		}
		catch (ClosedByInterruptException e) 
		{
		    log.debug(String.format(
                    "Aborted relay transfer for task %s. %s to %s . Protocol: %s => %s", 
                    aggregateTransferTask.getUuid(),
                    aggregateTransferTask.getSource(), 
                    aggregateTransferTask.getDest(),
                    getProtocolForClass(sourceClient.getClass()),
                    getProtocolForClass(destClient.getClass())));
		    Thread.currentThread().interrupt();
            throw e;
		}
		catch (RemoteDataException e)
		{
		    try {
                aggregateTransferTask.setEndTime(new Date());
                aggregateTransferTask.setStatus(TransferStatusType.FAILED);
                TransferTaskDao.persist(aggregateTransferTask);
            } catch (TransferException e1) {
                log.error("Failed to update parent transfer task " 
                        + aggregateTransferTask.getUuid() + " status to FAILED");
            }
		    
//			checkCancelled(remoteTransferListener);
			
			throw e;
		}
		catch (Exception e)
		{
		    try {
                aggregateTransferTask.setEndTime(new Date());
                aggregateTransferTask.setStatus(TransferStatusType.FAILED);
                TransferTaskDao.persist(aggregateTransferTask);
            } catch (TransferException e1) {
                log.error("Failed to update parent transfer task " 
                        + aggregateTransferTask.getUuid() + " status to FAILED");
            }
			
//			checkCancelled(remoteTransferListener);
			
			throw new RemoteDataException(
			        getDefaultErrorMessage(
			                srcPath, new RemoteTransferListener(aggregateTransferTask)), e);
		}
		finally
		{
			if (aggregateTransferTask != null) {
				log.info(String.format(
				        "Total of %d bytes transferred in task %s . Protocol %s => %s",
				        aggregateTransferTask.getBytesTransferred(),
				        aggregateTransferTask.getUuid(),
				        getProtocolForClass(sourceClient.getClass()),
				        getProtocolForClass(destClient.getClass())));
			}
			if (sourceClient instanceof Local) {
				log.info("Skipping deleting relay cache file " + tempDir.getAbsolutePath() + " as source originated from this host.");
			}
			else {
				log.info("Deleting relay cache file " + tempDir.getAbsolutePath());
				FileUtils.deleteQuietly(tempDir);
			}
		}
		
	}

	/**
	 * Returns shortname for package containing a {@link RemoteDataClient}. This
	 * allows us to determine the protocol used by that client quickly for logging 
	 * purposes. For example S3JCloud => s3, MaverickSFTP => sftp.
	 * 
	 * @param clientClass
	 * @return data protocol shortname used by a client.
	 */
	private Object getProtocolForClass(Class<? extends RemoteDataClient> clientClass) {
	    String fullName = clientClass.getName();
	    String[] tokens = fullName.split("\\.");
	    return tokens[tokens.length-2];
    }
	

    /**
	 * Parses the hostname out of a URI. This is used to extract systemId info from
	 * the TransferTask.rootTask.source and TransferTask.rootTask.dest fields and
	 * create the child source and dest values.
	 *
	 * @param transferPath
	 * @return
	 */
	private String getSystemId(String transferPath) {
		URI uri = null;
		try {
			uri = URI.create(transferPath);
			return uri.getHost();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Performs the transfer by streaming data from one system to another through
	 * the local server.
	 *
     * @param srcPath agave source path of the file on the remote system
     * @param destPath the destination agave path of the transfer on the remote system  
     * @param listener the listener to track the transfer info
     * @throws RemoteDataException
     * @throws IOException
     * @throws TransferException
     * @throws ClosedByInterruptException
     */
	private void proxyTransfer(String srcPath, String destPath, RemoteTransferListener listener) 
	throws RemoteDataException, IOException, TransferException, ClosedByInterruptException
	{
		InputStream in = null;
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		OutputStream out = null;
		long bytesSoFar = 0;
		try
		{
			log.debug(String.format(
			        "Beginning proxy transfer for task %s. %s to %s . Protocol: %s => %s", 
					listener.getTransferTask().getUuid(), 
					listener.getTransferTask().getSource(), 
					listener.getTransferTask().getDest(),
                    getProtocolForClass(sourceClient.getClass()),
                    getProtocolForClass(destClient.getClass())));

			long totalSize = sourceClient.length(srcPath);
			
			in = getInputStream(sourceClient, srcPath);
			bis = new BufferedInputStream(in);
			checkCancelled(listener);
			
			out = getOutputStream(destClient, destPath);
			bos = new BufferedOutputStream(out);
			
			checkCancelled(listener);
			
			int length = 0;
			long callbackTime = System.currentTimeMillis();
			int bufferSize = Math.min(sourceClient.getMaxBufferSize(), destClient.getMaxBufferSize());
			byte[] b = new byte[bufferSize];
			
			listener.started(totalSize, srcPath);
			
			while (( length = bis.read(b, 0, bufferSize)) != -1) 
			{
				bytesSoFar += length;
				
				bos.write(b, 0, length);
				
				
				// update the progress every 15 seconds buffer cycle. This reduced the impact
				// from the observing process while keeping the update interval at a 
				// rate the user can somewhat trust
				if (System.currentTimeMillis() > (callbackTime + 10000))
				{
				    // check to see if this transfer has been cancelled due to outside
                    // intervention such as updating the TransferTask record or the parent
                    // thread interrupting this one and setting the AbstractTransferTask.cancelled
                    // field to true
                    checkCancelled(listener);
                    
				    callbackTime = System.currentTimeMillis();
					
				    listener.progressed(bytesSoFar);
				}
			}
			
			// update with the final transferred blocks and wrap the transfer.
			listener.progressed(bytesSoFar);
			listener.completed();
			
		    log.debug(String.format(
                    "Completed proxy transfer for task %s. %s to %s . Protocol: %s => %s", 
                    listener.getTransferTask().getUuid(), 
                    listener.getTransferTask().getSource(), 
                    listener.getTransferTask().getDest(),
                    getProtocolForClass(sourceClient.getClass()),
                    getProtocolForClass(destClient.getClass())));
		    
		}
		catch (ClosedByInterruptException e) 
		{
		    log.debug(String.format(
                    "Aborted proxy transfer for task %s. %s to %s . Protocol: %s => %s", 
                    listener.getTransferTask().getUuid(),
                    listener.getTransferTask().getSource(), 
                    listener.getTransferTask().getDest(),
                    getProtocolForClass(destClient.getClass()),
                    getProtocolForClass(destClient.getClass())));
		    
			log.info("Transfer task " + listener.getTransferTask().getUuid() + " killed by worker shutdown.");
			setKilled(true);
			
			listener.progressed(bytesSoFar);
			listener.cancel();
			
			Thread.currentThread().interrupt();
			
			throw e;
		}
		catch (RemoteDataException | IOException e)
		{
		    log.debug(String.format(
                    "Failed proxy transfer for task %s. %s to %s . Protocol: %s => %s", 
                    listener.getTransferTask().getUuid(),
                    listener.getTransferTask().getSource(), 
                    listener.getTransferTask().getDest(),
                    getProtocolForClass(sourceClient.getClass()),
                    getProtocolForClass(destClient.getClass())));
		    
			// transfer failed due to connectivity issue
			listener.failed();
			
			throw e;
		}
		catch (Throwable e)
		{
		    log.debug(String.format(
                    "Failed proxy transfer for task %s. %s to %s . Protocol: %s => %s", 
                    listener.getTransferTask().getUuid(),
                    listener.getTransferTask().getSource(), 
                    listener.getTransferTask().getDest(),
                    getProtocolForClass(sourceClient.getClass()),
                    getProtocolForClass(destClient.getClass())));
		    
			// stuff happens, what are you going to do.
			listener.failed();
			
			throw new RemoteDataException("Transfer task %s failed.", e);
		}
		finally
		{
			if (listener != null && listener.getTransferTask() != null) {
			    log.info(String.format(
                        "Total of %d bytes transferred in task %s . Protocol %s => %s",
                        listener.getTransferTask().getBytesTransferred(),
                        listener.getTransferTask().getUuid(),
                        getProtocolForClass(sourceClient.getClass()),
                        getProtocolForClass(destClient.getClass())));
			}
			
			try { bis.close(); } catch(Throwable e) {}
			try { bos.close(); } catch(Throwable e) {}
			try { in.close(); } catch(Throwable e) {}
			try { out.close(); } catch(Throwable e) {}
		}
	}
	
	/**
     * Performs the transfer by streaming data from one system to another through
     * the local server. This option honors range requests, so only 
     *
     * @param srcPath agave source path of the file on the remote system
     * @param srcRangeOffset offset to start reading from the source file
     * @param srcRangeSize length of the range to read, -1 represents remainder of file
     * @param destPath the destination agave path of the transfer on the remote system  
     * @param destRangeOffset offset to start writing to the dest file
     * @param transferTask the transfer task tracking the transfer
     * @return transferTask updated with results of the transfer
     * @throws RemoteDataException
     * @throws IOException
     * @throws TransferException
     * @throws ClosedByInterruptException
     */
    public TransferTask copyRange(String srcPath, long srcRangeOffset, long srcRangeSize, 
                                    String destPath, long destRangeOffset, TransferTask transferTask) 
    throws RemoteDataException, IOException, TransferException, ClosedByInterruptException
    {
        if (transferTask == null) {
            throw new TransferException("TransferTask cannot be null. Please provide"
                    + "a valid transfer task to track this operation.");
        } else if (transferTask.getId() == null) {
            throw new TransferException("TransferTask does not have a valid id. "
                    + "Please persiste the transfer taks and attempt the operation again.");
        }
        
        try
        {
            // if we are transferring a directory
            if (sourceClient.isDirectory(srcPath))
            {    
                throw new TransferException("Range transfers are not supported on directories");
            }
            else
            {
                RemoteTransferListener listener = null;
                listener = new RemoteTransferListener(transferTask);
                if (StringUtils.equals(FilenameUtils.getName(srcPath), ".") ||
                        StringUtils.equals(FilenameUtils.getName(srcPath), ".."))
                {
                    // skip current directory and parent to avoid infinite loops and
                    // full file system copies.
                }
//                else if (sourceClient.isThirdPartyTransferSupported() &&
//                        destClient.isThirdPartyTransferSupported() &&
//                        sourceClient.getClass().equals(destClient.getClass()))
//                {
//                    dothirdPartyTransfer(srcPath, srcRangeOffset, srcRangeSize, destPath, destRangeOffset, listener);
//                }
                else
                {
                    RangeValidator sourceRangeValidator = new RangeValidator(srcRangeOffset, srcRangeSize, sourceClient.length(srcPath));
                    RangeValidator destRangeValidator = new RangeValidator(destRangeOffset, Range.SIZE_MAX, sourceClient.length(srcPath));
                    
                    try
                    {
                        long absoluteSourceIndex = sourceRangeValidator.getAbsoluteIndex();
                        long absoluteDestIndex = destRangeValidator.getAbsoluteIndex();
                        
                        proxyRangeTransfer(srcPath, absoluteSourceIndex, srcRangeSize, destPath, absoluteDestIndex, listener);
                    }
                    catch (ClosedByInterruptException e) 
                    {
                        if (transferTask != null) {
                            try {
                                TransferTaskDao.cancelAllRelatedTransfers(transferTask.getId());
                            } catch (TransferException e1) {
                            	Thread.currentThread().interrupt();
                                throw new RemoteDataException("Failed to cancel related transfer tasks.", e1);
                            }
                        }
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                    catch (RangeValidationException e) {
                        throw new RemoteDataException(e.getMessage(), e);
                    }
                    catch (TransferException e)
                    {
                        throw new RemoteDataException("Failed to udpate transfer record.", e);
                    }
                }
                
                return listener.getTransferTask();
            }
        }
        finally
        {
            try
            {
                if (destClient.isPermissionMirroringRequired()) {
                    destClient.setOwnerPermission(destClient.getUsername(), destPath, true);
                    destClient.setOwnerPermission(transferTask.getOwner(), destPath, true);
                }
            }
            catch (Exception e)
            {
                log.error("Failed to set permissions on " + destClient.getHost() + " for user " + transferTask.getOwner(), e);
            }
        } 
    }
    
	/**
	 * Performs the transfer by streaming data from one system to another through
     * the local server. This option honors range requests, so only 
     *
	 * @param srcPath agave source path of the file on the remote system
	 * @param srcRangeOffset offset to start reading from the source file
	 * @param srcRangeSize length of the range to read, -1 represents remainder of file
	 * @param destPath the destination agave path of the transfer on the remote system  
	 * @param destRangeOffset offset to start writing to the dest file
	 * @param listener the listener to track the transfer info
	 * @throws RemoteDataException
	 * @throws IOException
	 * @throws TransferException
	 * @throws ClosedByInterruptException
	 */
	public void proxyRangeTransfer(String srcPath, long srcRangeOffset, long srcRangeSize, 
	                                String destPath, long destRangeOffset, RemoteTransferListener listener) 
    throws RemoteDataException, IOException, TransferException, ClosedByInterruptException
    {
	    if (listener == null) {
	        throw new RemoteDataException("Transfer listener cannot be null");
	    } 
	    
	    InputStream in = null;
	    BufferedInputStream bis = null;
        
	    InputStream originalIn = null;
        BufferedInputStream originalBis = null;
        
        BufferedOutputStream bos = null;
        OutputStream out = null;
        
        long bytesSoFar = 0;
        try
        {
            log.debug(String.format(
                    "Beginning proxy transfer for task %s. %s to %s . Protocol: %s => %s", 
                    listener.getTransferTask().getUuid(), 
                    listener.getTransferTask().getSource(), 
                    listener.getTransferTask().getDest(),
                    getProtocolForClass(sourceClient.getClass()),
                    getProtocolForClass(destClient.getClass())));
            
            if (sourceClient.isDirectory(srcPath)) {
                throw new RemoteDataException("Cannot perform range query on directories"); 
            }
            
            long totalSize = srcRangeSize;
            if (totalSize == Range.SIZE_MAX) {
                totalSize = sourceClient.length(srcPath) - srcRangeOffset;
            }
            
            in = getInputStream(sourceClient, srcPath);
            in.skip(srcRangeOffset);
            bis = new BufferedInputStream(in);
            
            originalIn = getInputStream(destClient, destPath);
            originalBis = new BufferedInputStream(originalIn);
            
            String tmpFilename = destPath + ".tmp-" + System.currentTimeMillis();
            out = getOutputStream(destClient, tmpFilename);
            bos = new BufferedOutputStream(out);
            
            checkCancelled(listener);
            
            int length = 0;
            long remainingOffset = destRangeOffset;
            
            long callbackTime = System.currentTimeMillis();
            int bufferSize = sourceClient.getMaxBufferSize();
            byte[] newBytes = new byte[bufferSize];
            byte[] originalBytes = new byte[bufferSize];
            
            listener.started(totalSize, srcPath);
                 
            // skip ahead in the file to get to the position we want to begin overwriting.
            // this is generally not supported in most protocols, so we instead manually write
            // the first destRangeOffset bytes to a temp file, then append the input stream,
            // then write whatever is left of the file.
            while (( length = originalBis.read(originalBytes, 0, (int)Math.min(bufferSize, remainingOffset))) != -1) 
            {
                remainingOffset -= length;
                bytesSoFar += length;
                
                bos.write(originalBytes);
                
                // update the progress every 15 seconds buffer cycle. This reduced the impact
                // from the observing process while keeping the update interval at a 
                // rate the user can somewhat trust
                if (System.currentTimeMillis() > (callbackTime + 10000))
                {
                    // check to see if this transfer has been cancelled due to outside
                    // intervention such as updating the TransferTask record or the parent
                    // thread interrupting this one and setting the AbstractTransferTask.cancelled
                    // field to true
                    checkCancelled(listener);
                    
                    callbackTime = System.currentTimeMillis();
                    
                    listener.progressed(bytesSoFar);
                }
            }
            
            // check to see if this transfer has been cancelled due to outside
            // intervention such as updating the TransferTask record or the parent
            // thread interrupting this one and setting the AbstractTransferTask.cancelled
            // field to true
            checkCancelled(listener);
            callbackTime = System.currentTimeMillis();
            listener.progressed(bytesSoFar);
            
            long remainingBytes = totalSize;
            boolean haveReachedTheEndOfOriginalDesinationFile = false;
            
            // now we are at the destination, so start writing the input stream to the temp file
            while (remainingBytes < totalSize && 
                    (length = bis.read(newBytes, 0, (int)Math.min(bufferSize, remainingOffset))) != -1) 
            {
                // write the new input from the source stream to the destination. 
                // This is essentially an append operation until we run out of input.
                bos.write(newBytes);
                
                remainingBytes -= length;
                bytesSoFar += length;
            
                // we need to keep up with the copy on the original, so read along here byte for byte
                // until we hit the end of the file at which point we stop reading from the original.
                if (!haveReachedTheEndOfOriginalDesinationFile) {
                    haveReachedTheEndOfOriginalDesinationFile = (originalBis.read(originalBytes, 0, length) == -1);
                }
                
                // update the progress every 15 seconds buffer cycle. This reduced the impact
                // from the observing process while keeping the update interval at a 
                // rate the user can somewhat trust
                if (System.currentTimeMillis() > (callbackTime + 10000))
                {
                    // check to see if this transfer has been cancelled due to outside
                    // intervention such as updating the TransferTask record or the parent
                    // thread interrupting this one and setting the AbstractTransferTask.cancelled
                    // field to true
                    checkCancelled(listener);
                    
                    
                    callbackTime = System.currentTimeMillis();
                    
                    listener.progressed(bytesSoFar);
                }
            }
            
            // check to see if this transfer has been cancelled due to outside
            // intervention such as updating the TransferTask record or the parent
            // thread interrupting this one and setting the AbstractTransferTask.cancelled
            // field to true
            checkCancelled(listener);
            callbackTime = System.currentTimeMillis();
            listener.progressed(bytesSoFar);
            
            
            // finally, we are done reading from the input stream and we have no overwritten
            // the end of the output stream, so we finish writing the rest of the file.
            if (!haveReachedTheEndOfOriginalDesinationFile) 
            {
                while ((length = originalBis.read(originalBytes, 0, bufferSize)) != -1) 
                {
                    bytesSoFar += length;
                    
                    bos.write(originalBytes);
                    
                    // update the progress every 15 seconds buffer cycle. This reduced the impact
                    // from the observing process while keeping the update interval at a 
                    // rate the user can somewhat trust
                    if (System.currentTimeMillis() > (callbackTime + 10000))
                    {
                        // check to see if this transfer has been cancelled due to outside
                        // intervention such as updating the TransferTask record or the parent
                        // thread interrupting this one and setting the AbstractTransferTask.cancelled
                        // field to true
                        checkCancelled(listener);
                        
                        
                        callbackTime = System.currentTimeMillis();
                        
                        listener.progressed(bytesSoFar);
                    }
                }
            }
            
            // update with the final transferred blocks and wrap the transfer.
            listener.progressed(bytesSoFar);
            listener.completed();
            
            // now replace the original with the patched temp file
            destClient.doRename(tmpFilename, destPath);
            
            // and we're spent
            log.debug(String.format(
                    "Completed proxy transfer for task %s. %s to %s . Protocol: %s => %s", 
                    listener.getTransferTask().getUuid(), 
                    listener.getTransferTask().getSource(), 
                    listener.getTransferTask().getDest(),
                    getProtocolForClass(sourceClient.getClass()),
                    getProtocolForClass(destClient.getClass())));
        }
        catch (ClosedByInterruptException e) 
        {
            log.debug(String.format(
                    "Aborted proxy transfer for task %s. %s to %s . Protocol: %s => %s", 
                    listener.getTransferTask().getUuid(),
                    listener.getTransferTask().getSource(), 
                    listener.getTransferTask().getDest(),
                    getProtocolForClass(destClient.getClass()),
                    getProtocolForClass(destClient.getClass())));
            
            log.info("Transfer task " + listener.getTransferTask().getUuid() + " killed by worker shutdown.");
            setKilled(true);
            
            listener.progressed(bytesSoFar);
            listener.cancel();
            Thread.currentThread().interrupt();
            throw e;
        }
        catch (RemoteDataException | IOException e)
        {
            log.debug(String.format(
                    "Failed proxy transfer for task %s. %s to %s . Protocol: %s => %s", 
                    listener.getTransferTask().getUuid(),
                    listener.getTransferTask().getSource(), 
                    listener.getTransferTask().getDest(),
                    getProtocolForClass(sourceClient.getClass()),
                    getProtocolForClass(destClient.getClass())));
            
            // transfer failed due to connectivity issue
            listener.failed();
            
            throw e;
        }
        catch (Throwable e)
        {
            log.debug(String.format(
                    "Failed proxy transfer for task %s. %s to %s . Protocol: %s => %s", 
                    listener.getTransferTask().getUuid(),
                    listener.getTransferTask().getSource(), 
                    listener.getTransferTask().getDest(),
                    getProtocolForClass(sourceClient.getClass()),
                    getProtocolForClass(destClient.getClass())));
            
            // stuff happens, what are you going to do.
            listener.failed();
            
            throw new RemoteDataException("Transfer task " + listener.getTransferTask().getUuid() 
                    + " failed.", e);
        }
        finally
        {
            if (listener != null && listener.getTransferTask() != null) {
                log.info(String.format(
                        "Total of %d bytes transferred in task %s . Protocol %s => %s",
                        listener.getTransferTask().getBytesTransferred(),
                        listener.getTransferTask().getUuid(),
                        getProtocolForClass(sourceClient.getClass()),
                        getProtocolForClass(destClient.getClass())));
            }
            
            try { bis.close(); } catch(Throwable e) {}
            try { bos.close(); } catch(Throwable e) {}
            try { in.close(); } catch(Throwable e) {}
            try { out.close(); } catch(Throwable e) {}
        }
    }
	
	/**
	 * Creates a default error message for use in exceptions saying
	 * this {@link TransferTask} failed and optionally including the uuid.
	 *  
	 * @return message
	 */
	private String getDefaultErrorMessage(String srcPath, RemoteTransferListener listener) {
		return String.format(
				"Transfer %s cancelled while copying from source.",
				(listener.getTransferTask() == null ? 
						"of " + srcPath : 
						listener.getTransferTask().getUuid()));
	}
	
	/**
	 * Convenience method to get the output stream for the destination.
	 * @param client remote data client to remote system
     * @param destPath Agave path to file on remote system
     * @return output stream to source
	 * @throws IOException
	 * @throws RemoteDataException
	 */
	private OutputStream getOutputStream(RemoteDataClient client, String destPath) 
	throws IOException, RemoteDataException 
	{
		try 
		{
			return client.getOutputStream(destPath, true, false);
		} 
		catch (Exception e) 
		{
			// reauthenticate and retry in case of something weird
		    client.disconnect();
			
			try {
			    client.authenticate();
				return client.getOutputStream(destPath, true, true);
			} catch (RemoteDataException e1) {
				throw new RemoteDataException("Failed to open an output stream to " + destPath, e1);
			}
		}
	}
	
	/**
	 * Convenience method to get the input stream for the destination.
	 * @param client remote data client to remote system
	 * @param srcPath Agave path to file on remote system
	 * @return input stream from source
	 * @throws IOException
	 * @throws RemoteDataException
	 */
	private InputStream getInputStream(RemoteDataClient client, String srcPath) throws IOException, RemoteDataException
	{
		try 
		{
			return client.getInputStream(srcPath, true);
		} 
		catch (RemoteDataException e) {
			throw e;
		}
		catch (Exception e) 
		{
			// reauthenticate and retry in case of something weird
		    client.disconnect();
			client.authenticate();
			return client.getInputStream(srcPath, false);
		}
	}

   /**
    * This performs third party transfer only if source and destination urls
    * have a matching protocol that support third party transfers.
    */
	private void dothirdPartyTransfer(String srcPath, String destPath, RemoteTransferListener listener) throws RemoteDataException, IOException
	{
		try
	    {
		    log.debug(String.format(
                    "Beginning third party transfer for task %s. %s to %s . Protocol: %s => %s", 
                    listener.getTransferTask().getUuid(), 
                    listener.getTransferTask().getSource(), 
                    listener.getTransferTask().getDest(),
                    getProtocolForClass(sourceClient.getClass()),
                    getProtocolForClass(destClient.getClass())));
		    
	        ((GridFTP)destClient).setProtectionBufferSize(16384);
	        ((GridFTP)destClient).setType(GridFTPSession.TYPE_IMAGE);
	        ((GridFTP)destClient).setMode(GridFTPSession.MODE_EBLOCK);
	        ((GridFTP)destClient).setTCPBufferSize(destClient.getMaxBufferSize());

	        ((GridFTP)sourceClient).setProtectionBufferSize(16384);
	        ((GridFTP)sourceClient).setType(GridFTPSession.TYPE_IMAGE);
	        ((GridFTP)sourceClient).setMode(GridFTPSession.MODE_EBLOCK);
	        ((GridFTP)sourceClient).setTCPBufferSize(sourceClient.getMaxBufferSize());

//	        log.info("Enabling striped transfer.");
	        ((GridFTP)sourceClient).setStripedActive(((GridFTP)destClient).setStripedPassive());

//	        if (task != null)
//	        {
//	            try {
//	          	   task.setTotalSize(sourceClient.length(srcPath));
//	            } catch (Exception e) {}
//
//	            task.setBytesTransferred(0);
//	            task.setAttempts(task.getAttempts() + 1);
//	            task.setStatus(TransferStatusType.TRANSFERRING);
//	            task.setStartTime(new Date());
//	            TransferTaskDao.persist(task);
//	        }

	        if (((GridFTP)sourceClient).getHost().equals(((GridFTP)destClient).getHost()))
	        {
	        	((GridFTP)sourceClient).extendedTransfer(srcPath,
	      			  (GridFTP)destClient,
	      			  ((GridFTP)destClient).resolvePath(destPath),
	      			listener);
	        } else {
	      	  	((GridFTP)sourceClient).extendedTransfer(srcPath,
	      			  (GridFTP)destClient,
	      			  ((GridFTP)destClient).resolvePath(destPath),
	      			listener);
	        }
	        
	        log.debug(String.format(
                    "Completed third party transfer for task %s. %s to %s . Protocol: %s => %s", 
                    listener.getTransferTask().getUuid(), 
                    listener.getTransferTask().getSource(), 
                    listener.getTransferTask().getDest(),
                    getProtocolForClass(sourceClient.getClass()),
                    getProtocolForClass(destClient.getClass())));
	    }
		catch (ClosedByInterruptException e) 
		{
		    log.debug(String.format(
                    "Aborted third party transfer for task %s. %s to %s . Protocol: %s => %s", 
                    listener.getTransferTask().getUuid(),
                    listener.getTransferTask().getSource(), 
                    listener.getTransferTask().getDest(),
                    getProtocolForClass(sourceClient.getClass()),
                    getProtocolForClass(destClient.getClass())));
            
            log.info("Transfer task " + listener.getTransferTask().getUuid() + " killed by worker shutdown.");
			setKilled(true);
			
			try { ((GridFTP)sourceClient).abort(); } catch (Exception e1) {}
			try { ((GridFTP)destClient).abort(); } catch (Exception e1) {}
			Thread.currentThread().interrupt();
			throw e;
		}
		catch (IOException e)
		{
		    log.debug(String.format(
                    "Failed third party transfer for task %s. %s to %s . Protocol: %s => %s", 
                    listener.getTransferTask().getUuid(),
                    listener.getTransferTask().getSource(), 
                    listener.getTransferTask().getDest(),
                    getProtocolForClass(sourceClient.getClass()),
                    getProtocolForClass(destClient.getClass())));
            
            // transfer failed due to connectivity issue
			listener.failed();
			
			throw e;
		}
	    catch(Throwable e)
	    {
	        log.debug(String.format(
                    "Failed third party transfer for task %s. %s to %s . Protocol: %s => %s", 
                    listener.getTransferTask().getUuid(),
                    listener.getTransferTask().getSource(), 
                    listener.getTransferTask().getDest(),
                    getProtocolForClass(sourceClient.getClass()),
                    getProtocolForClass(destClient.getClass())));
            
            // stuff happens, what are you going to do.
			listener.failed();
			
			throw new RemoteDataException("Transfer task " + listener.getTransferTask().getUuid() 
					+ " failed.", e);
	    }
	    finally {
	    	if (listener != null && listener.getTransferTask() != null) {
	    	    log.info(String.format(
                        "Total of %d bytes transferred in task %s . Protocol %s => %s",
                        listener.getTransferTask().getBytesTransferred(),
                        listener.getTransferTask().getUuid(),
                        getProtocolForClass(sourceClient.getClass()),
                        getProtocolForClass(destClient.getClass())));
			}
	    }
	}
}
