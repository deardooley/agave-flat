package org.iplantc.service.transfer;

import java.util.Date;
import java.util.Observable;

import org.apache.log4j.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.globus.ftp.ByteRange;
import org.globus.ftp.ByteRangeList;
import org.globus.ftp.GridFTPRestartMarker;
import org.globus.ftp.Marker;
import org.globus.ftp.MarkerListener;
import org.globus.ftp.PerfMarker;
import org.globus.ftp.exception.PerfMarkerException;
import org.hibernate.StaleObjectStateException;
import org.iplantc.service.transfer.dao.TransferTaskDao;
import org.iplantc.service.transfer.model.TransferTask;
import org.iplantc.service.transfer.model.enumerations.TransferStatusType;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.transfer.TransferStatus;
import org.irods.jargon.core.transfer.TransferStatusCallbackListener;

import com.maverick.sftp.FileTransferProgress;

public class RemoteTransferListener extends Observable  
implements MarkerListener, FileTransferProgress, TransferStatusCallbackListener
{	
	private static final Logger log = Logger.getLogger(RemoteTransferListener.class);
	
	public ByteRangeList list = new ByteRangeList();
//    private ByteRange range;
    private TransferTask transferTask;
    private String firstRemoteFilepath;
    private TransferStatus persistentTransferStatus;
	private long aggregateStripedDateTransferred = 0;
	private long lastUpdated = System.currentTimeMillis();
	private long bytesLastCheck = 0;
	
	public RemoteTransferListener(TransferTask transferTask) 
	{
	    this.transferTask = transferTask;
	}
	
	/**
	 * @return the transferTask
	 */
	public synchronized TransferTask getTransferTask()
	{
	    
		return this.transferTask;
	}

	/**
	 * @param transferTask the transferTask to set
	 * @throws InterruptedException 
	 */
	private synchronized void setTransferTask(TransferTask transferTask)
	{
		try {
//		    log.debug("Saving transfer task");
			this.transferTask = transferTask == null ? 
					null : TransferTaskDao.merge(transferTask);
			
		} catch (StaleObjectStateException ex) {
//		    notify();
			// just ignore these. 
		} catch (Throwable e) {
			log.error("Failed to update transfer task " + transferTask.getUuid()  
					+ " in callback listener.", e);
//			try { this.transferTask = TransferTaskDao.findById(transferTask.getId()); } catch (Throwable t) {}
		}
	}

	public TransferStatus getOverallStatusCallback() {
		return persistentTransferStatus;
	}
	
	public void skipped(long totalSize, String remoteFile)
	{
		if (StringUtils.isEmpty(firstRemoteFilepath)) {
			firstRemoteFilepath = remoteFile;
		}
		
		TransferTask task = getTransferTask();
		if (task != null) 
		{
			task.setStatus(TransferStatusType.COMPLETED);
			task.setStartTime(new Date());
			task.setEndTime(new Date());
			task.setTotalSize(totalSize);
			task.setBytesTransferred(0);
			task.setLastUpdated(new Date());
			setTransferTask(task);
		}
	}
	
	/*************************************************************
	 * 		IRODS - TransferStatusCallbackListener methods
	 *************************************************************/
	
	@Override
	public void overallStatusCallback(TransferStatus transferStatus)
			throws JargonException
	{
		if (transferStatus.getTransferException() != null) {
			persistentTransferStatus = transferStatus;
		} else if (isCancelled()) {
        	notifyObservers(TransferStatusType.CANCELLED);
        }
		
		TransferTask task = getTransferTask();
        if (task != null)
		{
			if (transferStatus.getTransferState().equals(TransferStatus.TransferState.OVERALL_INITIATION)) {
				task.setStatus(TransferStatusType.TRANSFERRING);
				task.setStartTime(new Date());
				task.setAttempts(task.getAttempts() + 1);
				if (task.getTotalSize() == 0) {
					task.setTotalSize(transferStatus.getTotalSize());
				} else {
					task.setTotalSize(task.getTotalSize() + transferStatus.getTotalSize());
				}
			} else if (transferStatus.getTransferState().equals(TransferStatus.TransferState.OVERALL_COMPLETION)) {
				task.setStatus(TransferStatusType.COMPLETED);
				task.setEndTime(new Date());
			}
			
			setTransferTask(task);
		}
        
        if (hasChanged()) {
			throw new JargonException("Listener received a cancel request for transfer " 
					+ transferTask.getUuid());
		}
	}

	@Override
	public FileStatusCallbackResponse statusCallback(TransferStatus transferStatus)
			throws JargonException
	{
		if (transferStatus.getTransferException() != null) {
			persistentTransferStatus = transferStatus;
		} else if (isCancelled()) {
        	notifyObservers(TransferStatusType.CANCELLED);
        }
		
		TransferTask task = getTransferTask();
        if (task != null)
		{
			if (transferStatus.getTransferState().equals(TransferStatus.TransferState.OVERALL_INITIATION)) {
				task.setStatus(TransferStatusType.TRANSFERRING);
				task.setStartTime(new Date());
				task.setTotalSize(task.getTotalSize() + transferStatus.getTotalSize());
			} else if (transferStatus.getTransferState().equals(TransferStatus.TransferState.IN_PROGRESS_START_FILE)) {
				task.setStatus(TransferStatusType.TRANSFERRING);
				task.setStartTime(new Date());
				task.setTotalSize(task.getTotalSize() + transferStatus.getTotalSize());
				task.setTotalFiles(task.getTotalFiles() + 1);
			} else if (transferStatus.getTransferState().equals(TransferStatus.TransferState.CANCELLED)) {
				task.setStatus(TransferStatusType.CANCELLED);
				task.setEndTime(new Date());
			} else if (transferStatus.getTransferState().equals(TransferStatus.TransferState.FAILURE) || 
					transferStatus.getTransferException() != null) {
				task.setStatus(TransferStatusType.FAILED);
				task.setEndTime(new Date());
				task.setBytesTransferred(task.getBytesTransferred() + transferStatus.getBytesTransfered());
			} else if (transferStatus.getTransferState().equals(TransferStatus.TransferState.PAUSED)) {
				task.setStatus(TransferStatusType.PAUSED);
			} else if (transferStatus.getTransferState().equals(TransferStatus.TransferState.OVERALL_COMPLETION) || 
					transferStatus.getTransferState().equals(TransferStatus.TransferState.SUCCESS)) {
				task.setStatus(TransferStatusType.COMPLETED);
				task.setEndTime(new Date());
				task.setBytesTransferred(task.getBytesTransferred() + transferStatus.getBytesTransfered());
			} else if (transferStatus.getTransferState().equals(TransferStatus.TransferState.RESTARTING)) {
				task.setAttempts(task.getAttempts() + 1);
				task.setStatus(TransferStatusType.RETRYING);
				task.setEndTime(null);
				task.setStartTime(new Date());
			} else if (transferStatus.getTransferState().equals(TransferStatus.TransferState.IN_PROGRESS_COMPLETE_FILE)) {
				task.setBytesTransferred(task.getBytesTransferred() + transferStatus.getBytesTransfered());
			} else {
				log.debug("Unrecognized transfer status during transfer for task " + task.getUuid());
			}
			
			task.setLastUpdated(new Date());
			
			setTransferTask(task);
//			try {
//				transferTask);
//			} catch (TransferException e) {
//				log.error("Failed to update status of irods transfer task " + task.getUuid() + " to " + task.getStatus(), e);
//			}
		}
		
        return FileStatusCallbackResponse.CONTINUE;
	}

	@Override
	public CallbackResponse transferAsksWhetherToForceOperation(
			String irodsAbsolutePath, boolean isCollection)
	{
		return CallbackResponse.YES_FOR_ALL;
	}

	/*************************************************************
	 * 		Sftp - FileTransferProgress listener methods
	 *************************************************************/
	
	@Override
	public void started(long bytesTotal, String remoteFile)
	{
		if (StringUtils.isEmpty(firstRemoteFilepath)) {
			firstRemoteFilepath = remoteFile;
		}
		
		TransferTask task = getTransferTask();
        if (task != null)
		{
			task.setTotalFiles(task.getTotalFiles() + 1);
			task.setStatus(TransferStatusType.TRANSFERRING);
			task.setStartTime(new Date());
			
			if (remoteFile.equals(firstRemoteFilepath)) {
				task.setTotalSize(bytesTotal);
				task.setAttempts(task.getAttempts() + 1);
			} else {
				task.setTotalSize(task.getTotalSize() + bytesTotal);
			}
			
			setTransferTask(task);
		}
	}

	@Override
	public synchronized boolean isCancelled()
	{
		return hasChanged() || 
				(getTransferTask() != null && getTransferTask().getStatus().isCancelled());
	}

	/* (non-Javadoc)
	 * @see com.maverick.sftp.FileTransferProgress#progressed(long)
	 */
	@Override
	public void progressed(long bytesSoFar)
	{      
	    TransferTask task = getTransferTask();
        if (task != null)
		{
//			if (task.getBytesTransferred() > bytesSoFar) {
//				task.setBytesTransferred(task.getBytesTransferred() + bytesSoFar);
//			} else {
                task.setBytesTransferred(bytesSoFar);
//			}
			long currentTime = System.currentTimeMillis();
			if (( currentTime - lastUpdated) >= 15000) {
			    double progress = bytesSoFar - bytesLastCheck;
			    task.setTransferRate(((double)progress / ((double)(currentTime - lastUpdated) /  1000.0) ));
                setTransferTask(task);
                lastUpdated = currentTime;
                bytesLastCheck = bytesSoFar;
//			    log.debug("Moved " + bytesSoFar + " bytes @ " + progress + "B/s");
		        
			}
		}
	}
	
	/**
	 * Updates the current transfer task to cancelled. The actual
	 * workers will pick up on this the next time they check the
	 * callback status and send notifications to observers as
	 * needed.
	 */
	public void cancel() {
		TransferTask task = getTransferTask();
        if (task != null)
		{
			task.setStatus(TransferStatusType.CANCELLED);
			task.setEndTime(new Date());
			task.updateTransferRate();
			setTransferTask(task);
		}
        
        // set this listener to dirty
        log.debug("RemoteTransferListender for " + (task == null ? " anonymous transfer " : task.getUuid()) + 
					" was notified of an interrupt");
		setChanged();
	}

	/* (non-Javadoc)
	 * @see com.maverick.sftp.FileTransferProgress#completed()
	 */
	@Override
	public void completed()
	{
		TransferTask task = getTransferTask();
        if (task != null)
		{
			task.setStatus(TransferStatusType.COMPLETED);
			task.setEndTime(new Date());
			task.updateTransferRate();
			setTransferTask(task);
		}
	}
	
	public void failed()
	{
		TransferTask task = getTransferTask();
        if (task != null)
		{
			task.setStatus(TransferStatusType.FAILED);
			task.setEndTime(new Date());
			setTransferTask(task);
		}
	}
	
	/*************************************************************
	 * 		JGlobus - MarkerListener methods
	 *************************************************************/

	/* (non-Javadoc)
     * @see org.globus.ftp.MarkerListener#markerArrived(org.globus.ftp.Marker)
     */
	@Override
    public void markerArrived(Marker m) {
        if (isCancelled()) {
        	notifyObservers(TransferStatusType.CANCELLED);
        }
        
        if (m instanceof GridFTPRestartMarker) {
            restartMarkerArrived((GridFTPRestartMarker) m);
        } else if (m instanceof PerfMarker) {
            perfMarkerArrived((PerfMarker) m);
        } else {
            log.error("Received unsupported marker type");
        }
    };

    private void restartMarkerArrived(GridFTPRestartMarker marker) 
    {
        list.merge(marker.toVector());
        
        TransferTask task = getTransferTask();
        if (task != null)
		{
        	ByteRange ef = (ByteRange)list.toVector().lastElement();
        	
        	try
			{
        		TransferStatus.TransferState status = null;
        		if (ef.to == task.getTotalSize()) {
        			status = TransferStatus.TransferState.SUCCESS;
        		} else {
        			status = TransferStatus.TransferState.RESTARTING;
        		}
        		
				statusCallback(TransferStatus.instance(TransferStatus.TransferType.PUT, 
						task.getSource(), task.getDest(), "marker", 
						task.getTotalSize(), 0, 
						(int)task.getTotalFiles(), (int)task.getTotalSkippedFiles(), 
						(int)task.getTotalFiles(), status, 
						"marker", "marker"));
			}
			catch (JargonException e) {
				log.error("Failed to register restart marker on gridftp transfer " + task.getUuid(), e);
			}	
		}
    }

    private void perfMarkerArrived(PerfMarker marker) 
    {   
        long transferedBytes = 0;
        
        // stripe index
//        long index = -1;
//        if (marker.hasStripeIndex()) {
//            try {
//                index = marker.getStripeIndex();
//            } catch (PerfMarkerException e) {
//                log.error(e.toString());
//            } 
//        }
        
        try {
            transferedBytes = marker.getStripeBytesTransferred();
        } catch (PerfMarkerException e) {
            log.error("Failed to handle perf marker.",e);
        }
        
        this.aggregateStripedDateTransferred  += transferedBytes;
        
//        if (index == 0) {
//            for(UrlCopyListener listener: listeners) {
//                listener.transfer(aggregateStripedDateTransferred - this.range.from, this.range.to - this.range.from);
//                this.aggregateStripedDateTransferred = 0;
//                task.setStartTime(new Date());
//            }
//        }
        
        TransferTask task = getTransferTask();
        if (task != null) 
		{
        	// if this is the first marker to arrive
        	if (aggregateStripedDateTransferred == transferedBytes) {
//	        	task.setTotalFiles(task.getTotalFiles() + 1);
	        	task.setAttempts(1);
	        	task.setStatus(TransferStatusType.TRANSFERRING);
        	}
        	task.setBytesTransferred(aggregateStripedDateTransferred);
//	        task.setTotalSize(aggregateStripedDateTransferred);
        	setTransferTask(task);
		}
        
        // total stripe count
        if (marker.hasTotalStripeCount()) {
            try {
                log.info("Total stripe count = " 
                             + marker.getTotalStripeCount());
            } catch (PerfMarkerException e) {
                log.error(e.toString());
            } 
        }else {
        	log.info("Total stripe count: not present");
        }
    }//PerfMarkerArrived   

//    
//	/* 
//	 * Handles the notification of state change for a given transfer. A transfer may
//	 * be one or more tasks. Calling this method will mark this Observable as changed
//	 * and throw exceptions on any subsequent callback. 
//	 * (non-Javadoc)
//	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
//	 */
//	@Override
//	public void update(Observable o, Object arg)
//	{
////		// avoid redundant loops and multiple logging
////		if (!hasChanged()) 
////		{
//				TransferTask task = getTransferTask();
//	if (o instanceof URLCopy) {
//		log.debug("RemoteTransferListender for " + (task == null ? " anonymous transfer " : task.getUuid()) + 
//				" was notified by URLCopy of an interrupt");
//	} 
//	else {
//		log.debug("RemoteTransferListender for " + (task == null ? " anonymous transfer " : task.getUuid()) + 
//				" was notified by " + o.getClass().getName() + " of an interrupt");
//	}
//			
//				setChanged();
////		}
//	}
}
