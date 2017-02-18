package org.iplantc.service.jobs.queue.factory;


import static org.quartz.TriggerBuilder.newTrigger;

import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.queue.WorkerWatch;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

/**
 * Provides an implementation of the {@link JobFactory} interface
 * to maintain a priority queue of jobs across the jvm.
 * 
 * @author dooley
 *
 * @param <T>
 */
public abstract class AbstractJobProducerFactory implements JobFactory {
    
    private final Logger log = Logger.getLogger(getClass());
    
    protected static final ConcurrentLinkedDeque<String> stagingJobTaskQueue = new ConcurrentLinkedDeque<String>();
    protected static final ConcurrentLinkedDeque<String> monitoringJobTaskQueue = new ConcurrentLinkedDeque<String>();
    protected static final ConcurrentLinkedDeque<String> submissionJobTaskQueue = new ConcurrentLinkedDeque<String>();
    protected static final ConcurrentLinkedDeque<String> archivingJobTaskQueue = new ConcurrentLinkedDeque<String>();
    
    public AbstractJobProducerFactory() {}

    public synchronized Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {

        JobDetail jobDetail = bundle.getJobDetail();
        
        WorkerWatch worker = null;
        
        try 
        {
            worker = getJobInstance();
            
            String jobUuid = worker.selectNextAvailableJob();

            if (StringUtils.isNotEmpty(jobUuid))
            {
                if (!getTaskQueue().contains(jobUuid) && 
                		getTaskQueue().size() < getMaxTasks()) 
                {
                	getTaskQueue().add(jobUuid);
                    produceWorker(worker.getClass(), jobDetail.getKey().getGroup(), jobUuid, scheduler);
                }
            }

            return worker;

        } catch (Throwable e) {
            log.error("Failed to create new " +  jobDetail.getJobClass().getName() + " task", e);
            return worker;
        }
        finally {
        	try {
        		HibernateUtil.flush();
        		HibernateUtil.disconnectSession();
        	}
        	catch (Throwable e) {}
        }
    }
    
    /**
     * Creates a new job on the consumer queue with a unique name based on the job uuid,
     * thus guaranteeing single execution within a JVM. If clustered, this should ensure 
     * single execution across the cluster.
     * 
     * @param jobClass the class of the {@link Job} to run
     * @param groupName phase name
     * @param jobUuid the Agave job UUID to be run by the Quartz {@link Job}
     * @param sched the current scheduler
     * @throws SchedulerException
     */
    protected void produceWorker(Class<? extends WorkerWatch> jobClass, String groupName, String jobUuid, Scheduler sched) 
    throws SchedulerException 
    {
        JobDetail jobDetail = org.quartz.JobBuilder.newJob(jobClass)
            .usingJobData("uuid", jobUuid)
            .withIdentity(jobUuid, groupName + "Workers")
            .storeDurably(false)
            .build();

         SimpleTrigger trigger = (SimpleTrigger)newTrigger()
                            .withIdentity(jobUuid, groupName + "Workers")
                            .startNow()
                            .build();
         
         sched.scheduleJob(jobDetail, trigger);
    }
    
   /**
     * Releases the job from the relevant {@link ConcurrentLinkedDeque} so it can be
     * consumed by another thread.
     * 
     * @param uuid
     */
    public static synchronized void releaseJob(String uuid) {
        if (archivingJobTaskQueue.contains(uuid)) {
            archivingJobTaskQueue.remove(uuid);
        }
        
        if (monitoringJobTaskQueue.contains(uuid)) {
            monitoringJobTaskQueue.remove(uuid);
        }
        
        if (stagingJobTaskQueue.contains(uuid)) {
            stagingJobTaskQueue.remove(uuid);
        }
        
        if (submissionJobTaskQueue.contains(uuid)) {
            submissionJobTaskQueue.remove(uuid);
        }
    }
    
    /**
     * Returns the task queue for the given scheduler.
     * @return
     */
    protected abstract ConcurrentLinkedDeque<String> getTaskQueue();
    
    protected abstract WorkerWatch getJobInstance();
    
    protected abstract int getMaxTasks();
}