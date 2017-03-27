package org.iplantc.service.jobs.queue.factory;


import static org.quartz.TriggerBuilder.newTrigger;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.HibernateUtil;
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
 * All synchronized methods and blocks in this class use this class's lock for serialization.  
 * This is overly aggressive because it uses the same lock for all phase queues, 
 * but this isn't worth fixing given that this code will be removed in the new jobs 
 * design.  The previous version of this code had some methods synchronized on the class
 * object and others on this instance object, which is not easy to reason about and is
 * probably not what was intended.
 * 
 * @author dooley
 *
 * @param <T>
 */
public abstract class AbstractJobProducerFactory implements JobFactory {
    
    private final Logger log = Logger.getLogger(getClass());
    
    // These structures should be sets or maps for faster search.  Mixing concurrent data
    // structures and synchronization is usually not necessary, and it's not clear using
    // both types of concurrency control is needed here.  Notice that these fields leak
    // outside this class in several ways, so concurrency considerations are complicated.
    protected static final ConcurrentLinkedDeque<String> stagingJobTaskQueue = new ConcurrentLinkedDeque<String>();
    protected static final ConcurrentLinkedDeque<String> monitoringJobTaskQueue = new ConcurrentLinkedDeque<String>();
    protected static final ConcurrentLinkedDeque<String> submissionJobTaskQueue = new ConcurrentLinkedDeque<String>();
    protected static final ConcurrentLinkedDeque<String> archivingJobTaskQueue = new ConcurrentLinkedDeque<String>();
    
    public AbstractJobProducerFactory() {}

    public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {

    	// Synchronize on the class object of this class.  This uses the same monitor
    	// as the "static synchronized" methods below.  See chapter 17.1 of the Java 7 
    	// JLS for a precise specification on which monitor is used with static
    	// synchronized.  
    	synchronized (AbstractJobProducerFactory.class) { 
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
    	} // synchronization
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
    
    /**
	 * @return the stagingjobtaskqueue
	 */
	public static ConcurrentLinkedDeque<String> getStagingjobtaskqueue() {
		return stagingJobTaskQueue;
	}

	/**
	 * @return the monitoringjobtaskqueue
	 */
	public static ConcurrentLinkedDeque<String> getMonitoringjobtaskqueue() {
		return monitoringJobTaskQueue;
	}

	/**
	 * @return the submissionjobtaskqueue
	 */
	public static ConcurrentLinkedDeque<String> getSubmissionjobtaskqueue() {
		return submissionJobTaskQueue;
	}

	/**
	 * @return the archivingjobtaskqueue
	 */
	public static ConcurrentLinkedDeque<String> getArchivingjobtaskqueue() {
		return archivingJobTaskQueue;
	}

	/**
     * Returns the contents of all the concurrent task queues
     * at the moment.
     * 
     * @return
     */
    public static synchronized Set<String> getAllActiveJobsUuids() {
    	Set<String> jobUuids = new HashSet<String>();
    	if (archivingJobTaskQueue.size() > 0) {
    		Iterator<String> iter = archivingJobTaskQueue.iterator();
    		while (iter.hasNext()) {
    			jobUuids.add(iter.next());
    		}
    	}
    	
    	if (monitoringJobTaskQueue.size() > 0) {
    		Iterator<String> iter = monitoringJobTaskQueue.iterator();
    		while (iter.hasNext()) {
    			jobUuids.add(iter.next());
    		}
    	}
    	
    	if (stagingJobTaskQueue.size() > 0) {
    		Iterator<String> iter = stagingJobTaskQueue.iterator();
    		while (iter.hasNext()) {
    			jobUuids.add(iter.next());
    		}
    	}
    	
    	if (submissionJobTaskQueue.size() > 0) {
    		Iterator<String> iter = submissionJobTaskQueue.iterator();
    		while (iter.hasNext()) {
    			jobUuids.add(iter.next());
    		}
    	}
    	
    	return jobUuids;
    }
}