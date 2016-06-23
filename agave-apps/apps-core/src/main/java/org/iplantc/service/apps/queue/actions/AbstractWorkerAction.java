package org.iplantc.service.apps.queue.actions;

import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.iplantc.service.transfer.URLCopy;
import org.iplantc.service.transfer.model.TransferTask;

public abstract class AbstractWorkerAction<T> implements WorkerAction<T> {

//    private static Logger log = Logger.getLogger(AbstractWorkerAction.class);
    private AtomicBoolean stopped = new AtomicBoolean(false);
    
    protected T entity;
    protected URLCopy urlCopy;
    protected TransferTask rootTask;

    public AbstractWorkerAction(T entity) {
        this.entity = entity;
    }

    /**
     * @return the stopped
     */
    @Override
    public boolean isStopped() {
        return stopped.get();
    }

    /**
     * @param stopped the stopped to set
     */
    @Override
    public synchronized void setStopped(boolean stopped) {
        this.stopped.set(stopped);
        
        if (getUrlCopy() != null) {
            getUrlCopy().setKilled(true);
        }
    }

    @Override
    public synchronized T getEntity() {
        return entity;
    }

    @Override
    public synchronized void setEntity(T entity) {
        this.entity = entity;
    }

    /**
     * @return the urlCopy
     */
    @Override
    public synchronized URLCopy getUrlCopy() {
        return urlCopy;
    }

    /**
     * @param urlCopy the urlCopy to set
     */
    @Override
    public synchronized void setUrlCopy(URLCopy urlCopy) {
        this.urlCopy = urlCopy;
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