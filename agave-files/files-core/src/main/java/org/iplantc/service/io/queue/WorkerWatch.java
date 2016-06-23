package org.iplantc.service.io.queue;

import org.iplantc.service.io.exceptions.TaskException;
import org.iplantc.service.io.model.QueueTask;
import org.quartz.InterruptableJob;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;

public interface WorkerWatch<T extends QueueTask> extends InterruptableJob {

    public abstract void doExecute() throws JobExecutionException;
    
    /**
     * Selects the next available job for processing by a 
     * worker process. The selection process is left up to 
     * the implementing class.
     * 
     * @return
     */
    public abstract Long selectNextAvailableQueueTask() throws TaskException ;

    public abstract T getQueueTask();

    public abstract void setQueueTask(T queueTask);

    /**
     * @return the stopped
     */
    public abstract boolean isStopped();

    /**
     * @param stopped the stopped to set
     * @throws UnableToInterruptJobException 
     */
    public abstract void setStopped(boolean killed) throws UnableToInterruptJobException;

    /**
     * @return true of the task has completed, false otherwise
     */
    public abstract boolean isTaskComplete();

    /**
     * @param taskComplete the taskComplete to set
     */
    public abstract void setTaskComplete(boolean complete);

    public abstract void setQueueTaskId(Long queueTaskId);

}