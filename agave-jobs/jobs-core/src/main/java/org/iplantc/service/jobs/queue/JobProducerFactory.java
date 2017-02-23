package org.iplantc.service.jobs.queue;


import static org.quartz.TriggerBuilder.newTrigger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.jobs.Settings;
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
 * @deprecated
 * @see org.iplantc.service.jobs.queue.factory.AbstractJobProducerFactory
 * @param <T>
 */
public class JobProducerFactory implements JobFactory {
    
    private final Logger log = Logger.getLogger(getClass());
    
    private static final ConcurrentLinkedDeque<String> stagingJobTaskQueue = new ConcurrentLinkedDeque<String>();
    private static final ConcurrentLinkedDeque<String> monitoringJobTaskQueue = new ConcurrentLinkedDeque<String>();
    private static final ConcurrentLinkedDeque<String> submissionJobTaskQueue = new ConcurrentLinkedDeque<String>();
    private static final ConcurrentLinkedDeque<String> archivingJobTaskQueue = new ConcurrentLinkedDeque<String>();
    
    public JobProducerFactory() {}

    public synchronized Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {

        JobDetail jobDetail = bundle.getJobDetail();
        Class<? extends WorkerWatch> jobClass = (Class<? extends WorkerWatch>) jobDetail.getJobClass();
      
        WorkerWatch worker = null;
        
        try 
        {
            worker = jobClass.newInstance();
            
            String jobUuid = worker.selectNextAvailableJob();

            if (StringUtils.isNotEmpty(jobUuid))
            {
                if (worker instanceof ArchiveWatch) {
                    if (!archivingJobTaskQueue.contains(jobUuid) && 
                            archivingJobTaskQueue.size() < Settings.MAX_ARCHIVE_TASKS) 
                    {
                        archivingJobTaskQueue.add(jobUuid);
                        produceWorker(jobClass, jobDetail.getKey().getGroup(), jobUuid);
                    }
                }
                else if (worker instanceof SubmissionWatch) {
                    if (!submissionJobTaskQueue.contains(jobUuid) && 
                            submissionJobTaskQueue.size() < Settings.MAX_SUBMISSION_TASKS) 
                    {
                        submissionJobTaskQueue.add(jobUuid);
                        produceWorker(jobClass, jobDetail.getKey().getGroup(), jobUuid);
                    }
                } else if (worker instanceof MonitoringWatch) {
                    if (!monitoringJobTaskQueue.contains(jobUuid) && 
                            monitoringJobTaskQueue.size() < Settings.MAX_MONITORING_TASKS) 
                    {
                        monitoringJobTaskQueue.add(jobUuid);
                        produceWorker(jobClass, jobDetail.getKey().getGroup(), jobUuid);
                    }
                } else if (worker instanceof StagingWatch) {
                    if (!stagingJobTaskQueue.contains(jobUuid) && 
                            stagingJobTaskQueue.size() < Settings.MAX_STAGING_TASKS) 
                    {
                        stagingJobTaskQueue.add(jobUuid);
                        produceWorker(jobClass, jobDetail.getKey().getGroup(), jobUuid);
                    }
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
     * @param jobClass
     * @param groupName
     * @param jobUuid
     * @throws SchedulerException
     */
    private void produceWorker(Class<? extends WorkerWatch> jobClass, String groupName, String jobUuid) 
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
         
         Scheduler sched = new StdSchedulerFactory().getScheduler("AgaveConsumerJobScheduler");
         
         if (!sched.isStarted()) {
             sched.start();
         }
//         log.debug("Assigning new job " + jobUuid + " to " + archiveJobDetail);
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
}