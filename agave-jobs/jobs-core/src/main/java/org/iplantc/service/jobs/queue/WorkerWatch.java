package org.iplantc.service.jobs.queue;

import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.SchedulerException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.queue.actions.WorkerAction;
import org.quartz.InterruptableJob;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;

public interface WorkerWatch extends InterruptableJob {

    public abstract void doExecute() throws JobExecutionException;
    
    /**
     * Selects the next available job for processing by a 
     * worker process. The selection process is left up to 
     * the implementing class.
     * 
     * @return uuid of the selected job
     */
    public abstract String selectNextAvailableJob() throws JobException, SchedulerException ;

    public abstract Job getJob() throws JobException;

    public abstract void setJob(Job job);
    
    public abstract void setJobUuid(String uuid) throws JobException;

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

    /**
     * @return the workerAction
     */
    public WorkerAction getWorkerAction();

    /**
     * @param workerAction the workerAction to set
     */
    public void setWorkerAction(WorkerAction workerAction);
    
//    public void setJobProducerFactory(JobProducerFactory jobProducerFactory);

}