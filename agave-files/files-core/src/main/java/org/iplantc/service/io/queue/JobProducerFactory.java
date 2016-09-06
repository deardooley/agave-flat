package org.iplantc.service.io.queue;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.iplantc.service.common.exceptions.TaskQueueException;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.queue.DistributedBlockingTaskQueue;
import org.iplantc.service.common.queue.RedLockTaskQueue;
import org.iplantc.service.io.Settings;
import org.iplantc.service.io.model.EncodingTask;
import org.iplantc.service.io.model.StagingTask;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdSchedulerFactory;
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
public class JobProducerFactory implements JobFactory {
    
    private final Logger log = Logger.getLogger(getClass());
 
    private static final RedLockTaskQueue stagingJobTaskQueue = new RedLockTaskQueue("dataStagingQueue");
    private static final AtomicLong activeStagingTasks = new AtomicLong(0);
    private static final RedLockTaskQueue encodingJobTaskQueue = new RedLockTaskQueue("dataEncodingQueue");
    private static final AtomicLong activeEncodingTasks = new AtomicLong(0);
    
    public JobProducerFactory() {}

    public synchronized Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {

        JobDetail jobDetail = bundle.getJobDetail();
        Class<? extends WorkerWatch> jobClass = (Class<? extends WorkerWatch>) jobDetail.getJobClass();
       
        WorkerWatch worker = null;
        try 
        {
            worker = jobClass.newInstance();
            
            String queueTaskId = worker.selectNextAvailableQueueTask();

            if (queueTaskId != null)
            {
                if (worker instanceof StagingJob  
                        && !stagingJobTaskQueue.contains(queueTaskId) 
                        && activeStagingTasks.get() < Settings.MAX_STAGING_TASKS) 
                {
                    stagingJobTaskQueue.push(queueTaskId);
                    produceWorker(jobClass, jobDetail.getKey().getGroup(), queueTaskId);
                }
                else if (worker instanceof EncodingJob  
                        && !encodingJobTaskQueue.contains(queueTaskId) 
                        && activeEncodingTasks.get() < Settings.MAX_TRANSFORM_TASKS) 
                {
                    encodingJobTaskQueue.push(queueTaskId);
                    produceWorker(jobClass, jobDetail.getKey().getGroup(), queueTaskId);
                }
                else
                {
                    log.debug("Unknown file processing task " + jobDetail.getKey() + 
                            " attempting to process task " + queueTaskId + ". Ignoring...");
                }
            }

            return worker;

        } catch (Throwable e) {
            log.error("Failed to create new " +  jobDetail.getJobClass().getName() + " task", e);
            return worker;
        }
        finally {
            try {
//                HibernateUtil.commitTransaction();
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
     * @param jobClass
     * @param groupName
     * @param jobUuid
     * @throws SchedulerException
     */
    private void produceWorker(Class<? extends WorkerWatch> jobClass, String groupName, String queueTaskId) 
    throws SchedulerException 
    {
        JobDetail jobDetail = org.quartz.JobBuilder.newJob(jobClass)
            .usingJobData("queueTaskId", queueTaskId)
            .withIdentity(jobClass.getName() + "-" + queueTaskId, groupName + "Workers-"+queueTaskId)
            .build();

         SimpleTrigger trigger = (SimpleTrigger)newTrigger()
                            .withIdentity(jobClass.getName() + "-" + queueTaskId, groupName + "Workers-"+queueTaskId)
                            .startNow()
                            .withSchedule(simpleSchedule()
                                .withMisfireHandlingInstructionNextWithExistingCount()
                                .withIntervalInSeconds(1)
                                .withRepeatCount(0))
                            .build();

         Scheduler sched = new StdSchedulerFactory().getScheduler("AgaveConsumerTransferScheduler");
         if (!sched.isStarted()) {
             sched.start();
         }
         
         log.debug("Assigning " + jobClass.getSimpleName() + " " + queueTaskId + " for processing");
         
         sched.scheduleJob(jobDetail, trigger);
    }

    /**
     * Releases the {@link StagingTask} from the {@link ConcurrentLinkedDeque} so it can be
     * consumed by another thread.
     * 
     * @param id of staging entity
     * @throws TaskQueueException 
     * @throws IllegalArgumentException 
     */
    public static void releaseStagingJob(String taskIdentifier) 
    throws IllegalArgumentException, TaskQueueException 
    {
        if (taskIdentifier != null) {
        	stagingJobTaskQueue.unlock(taskIdentifier);
        }
    }
    
    /**
     * Releases the {@link EncodingTask}  from the {@link ConcurrentLinkedDeque} so it can be
     * consumed by another thread.
     * 
     * @param id of encoding entity
     * @throws TaskQueueException 
     * @throws IllegalArgumentException 
     */
    public static void releaseEncodingJob(String taskIdentifier) 
	throws IllegalArgumentException, TaskQueueException 
    {
        if (taskIdentifier != null) {
        	encodingJobTaskQueue.unlock(taskIdentifier);
        }
    }
}