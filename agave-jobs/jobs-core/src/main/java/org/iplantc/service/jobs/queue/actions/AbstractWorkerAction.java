package org.iplantc.service.jobs.queue.actions;

import java.nio.channels.ClosedByInterruptException;

import org.iplantc.service.jobs.exceptions.JobFinishedException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.phases.workers.IPhaseWorker;
import org.iplantc.service.transfer.URLCopy;
import org.iplantc.service.transfer.model.TransferTask;

public abstract class AbstractWorkerAction implements WorkerAction {

    protected Job job;
    protected IPhaseWorker worker;
    protected URLCopy urlCopy;
    protected TransferTask rootTask;

    public AbstractWorkerAction(Job job, IPhaseWorker worker) {
        this.job = job;
        this.worker = worker;
    }

    /**
     * @return the stopped
     */
    @Override
    public boolean isStopped() {
        return worker.isJobStopped();
    }

    /**
     * @return the job
     */
    @Override
    public synchronized Job getJob() {
        return job;
    }

    /**
     * @param job the job to set
     */
    @Override
    public synchronized void setJob(Job job) {
        this.job = job;
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
    
    /** 
     * @see org.iplantc.service.jobs.queue.actions.WorkerAction#checkStopped()
     */
    @Override
    public void checkStopped() throws ClosedByInterruptException, JobFinishedException
    {
        worker.checkStopped();
    }
}