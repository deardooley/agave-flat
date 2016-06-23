package org.iplantc.service.io.manager.actions;

import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.model.TransferTask;

public abstract class FileOperation<T> implements WorkerAction<T> {

//    private static Logger log = Logger.getLogger(AbstractWorkerAction.class);
    private AtomicBoolean stopped = new AtomicBoolean(false);
    
    protected T entity;
    protected RemoteDataClient sourceClient;
    protected TransferTask rootTask;

    public FileOperation(T entity) {
        this.entity = entity;
    }

    /* (non-Javadoc)
     * @see org.iplantc.service.io.manager.actions.WorkerAction#isStopped()
     */
    @Override
    public boolean isStopped() {
        return stopped.get();
    }

    /* (non-Javadoc)
     * @see org.iplantc.service.io.manager.actions.WorkerAction#setStopped(boolean)
     */
    @Override
    public synchronized void setStopped(boolean stopped) {
        this.stopped.set(stopped);
        
        if (getSourceClient() != null) {
        	getSourceClient().disconnect();
        }
    }

    /* (non-Javadoc)
     * @see org.iplantc.service.io.manager.actions.WorkerAction#getTask()
     */
    @Override
    public synchronized T getTask() {
        return entity;
    }

    /* (non-Javadoc)
     * @see org.iplantc.service.io.manager.actions.WorkerAction#setTask(java.lang.Object)
     */
    @Override
    public synchronized void setTask(T entity) {
        this.entity = entity;
    }

    /* (non-Javadoc)
     * @see org.iplantc.service.io.manager.actions.WorkerAction#getSourceClient()
     */
    @Override
    public synchronized RemoteDataClient getSourceClient() {
        return this.sourceClient;
    }

    /* (non-Javadoc)
	 * @see org.iplantc.service.io.manager.actions.WorkerAction#setSourceClient(org.iplantc.service.transfer.RemoteDataClient)
	 */
	@Override
    public synchronized void setSourceClient(RemoteDataClient sourceClient) {
        this.sourceClient = sourceClient;
    }
    
    /* (non-Javadoc)
     * @see org.iplantc.service.jobs.queue.actions.WorkerAction#checkStopped()
     */
    @Override
    public void checkStopped() throws ClosedByInterruptException {
        if (isStopped()) {
            throw new ClosedByInterruptException();
        }
    }
}