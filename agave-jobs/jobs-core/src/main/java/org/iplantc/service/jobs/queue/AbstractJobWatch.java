package org.iplantc.service.jobs.queue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.dao.JobEventDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobEvent;
import org.iplantc.service.jobs.queue.actions.WorkerAction;
import org.iplantc.service.transfer.dao.TransferTaskDao;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;

public abstract class AbstractJobWatch implements WorkerWatch {

    protected static final Logger log = Logger.getLogger(AbstractJobWatch.class);
    
    protected AtomicBoolean stopped = new AtomicBoolean(false);
    protected AtomicBoolean taskComplete = new AtomicBoolean(false);
    protected Job job = null;
    private WorkerAction workerAction = null;
    protected boolean allowFailure = false;
//    private JobProducerFactory jobProducerFactory;

    private String jobUuid;
    
    public AbstractJobWatch() {
        super();
    }
    
    public AbstractJobWatch(boolean allowFailure) {
        this();
        this.allowFailure = allowFailure;
    }
    
    /* (non-Javadoc)
     * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
     */
    public void execute(JobExecutionContext context) 
    throws JobExecutionException 
    {
        try 
    	{
    		if (org.iplantc.service.common.Settings.isDrainingQueuesEnabled()) {
//    			log.debug("Queue draining has been enabled. Skipping archive task." );
    			return;
    		}
    		
    		if (context.getMergedJobDataMap().containsKey("uuid")) {
                setJobUuid(context.getMergedJobDataMap().getString("uuid"));
            }
    		
    		if (getJob() != null) 
            {
//    		    log.debug(getClass().getSimpleName() + " worker found job " + getJob().getUuid() + " for user " 
//                        + getJob().getOwner() + " to process");
    		    
    		    // this is a new thread and thus has no tenant info loaded. we set it up
                // here so things like app and system lookups will stay local to the 
                // tenant
                TenancyHelper.setCurrentTenantId(getJob().getTenantId());
                TenancyHelper.setCurrentEndUser(getJob().getOwner());
                
    			doExecute();
            } 

    	}
    	catch(JobExecutionException e) {
    	    if (allowFailure) throw e;
    	}
    	catch (Throwable e) 
    	{
    		log.error("Unexpected error during job worker execution", e);
    		if (allowFailure) 
    		    throw new JobExecutionException("Unexpected error during job worker execution",e);
    	}
    	finally {
    	    if (getJob() != null) {
//    	        log.debug("Releasing job " + getJob().getUuid() + " after task completion");
    	        JobProducerFactory.releaseJob(getJob().getUuid());
    	    }
    	}
    	
    }

    @Override
    public void interrupt() throws UnableToInterruptJobException {
    	
        setStopped(true);
        
        if (getJob() != null) 
    	{
    		// this is a new thread and thus has no tenant info loaded. we set it up
    		// here so things like app and system lookups will stay local to the 
    		// tenant
    		TenancyHelper.setCurrentTenantId(getJob().getTenantId());
    		TenancyHelper.setCurrentEndUser(getJob().getOwner());
    		
    		try {
                for (JobEvent event: JobEventDao.getByJobId(getJob().getId())) 
                {
                	if (event.getTransferTask() != null) 
                	{
                		try { 
                			TransferTaskDao.cancelAllRelatedTransfers(event.getTransferTask().getId());
                		} catch (Throwable e) {
                			log.error("Failed to cancel transfer task " + 
                					event.getTransferTask().getUuid() + " associated with job " + this.jobUuid, e);
                		}
                	}
                }
            } catch (JobException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        		
        		
    		// set status back to previous state that will allow it to 
    		// be picked up by a worker again.
    		rollbackStatus();
    	}
    }
    
    /**
     * Performs the job status rollback to a previous state allowing it 
     * to be picked up by other workers.
     */
    protected abstract void rollbackStatus();

    @Override
    public synchronized void setJob(Job job) {
    	this.job = job;
    }

    /**
     * @return the workerAction
     */
    @Override
    public synchronized WorkerAction getWorkerAction() {
        return workerAction;
    }

    /**
     * @param workerAction the workerAction to set
     */
    @Override
    public synchronized void setWorkerAction(WorkerAction workerAction) {
        this.workerAction = workerAction;
    }

    @Override
    public boolean isStopped() {
        return stopped.get();
    }

    @Override
    public void setStopped(boolean killed) throws UnableToInterruptJobException {
        this.stopped.set(killed);
        if (getWorkerAction() != null) {
//            getWorkerAction().setThreadStopped(true);
            // Nothing else is acting on behalf of this task, so just kill it immediately. 
            // all transfers will be cleaned up in the setRollaback method.
            
//            int timeout = 0;
//            while(!isTaskComplete()) {
//                try { Thread.sleep(1000); } catch (InterruptedException e) {}
//                timeout++;
//                if (timeout >= 60) {
//                    throw new UnableToInterruptJobException("Unable to interrupt archiving task for job " 
//                            + this.jobUuid + " after 30 seconds.");
//                }
//                
//            }
        }
    }

    @Override
    public synchronized void setJobUuid(String uuid) throws JobException {
        this.jobUuid = uuid;
    }

    @Override
    public boolean isTaskComplete() {
        return taskComplete.get();
    }

    @Override
    public void setTaskComplete(boolean complete) {
        this.taskComplete.set(complete);
    }
    
    /* (non-Javadoc)
     * @see org.iplantc.service.jobs.queue.WorkerWatch#getJob()
     */
    @Override
    public synchronized Job getJob() {
        if (this.job == null && StringUtils.isNotEmpty(this.jobUuid)) {
            try {
                this.job = JobDao.getByUuid(this.jobUuid);
            } catch (JobException e) {
                log.error("Unable to resolve job uuid " + this.jobUuid);
            }
        }
        
        return job;
    }

//    /**
//     * @return the jobProducerFactory
//     */
//    public synchronized JobProducerFactory getJobProducerFactory() {
//        return jobProducerFactory;
//    }
//
//    /**
//     * @param jobProducerFactory the jobProducerFactory to set
//     */
//    @Override
//    public synchronized void setJobProducerFactory(JobProducerFactory jobProducerFactory) {
//        this.jobProducerFactory = jobProducerFactory;
//    }

}