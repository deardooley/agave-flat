package org.iplantc.service.jobs.phases.schedulers;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.dao.JobInterruptDao;
import org.iplantc.service.jobs.dao.JobLeaseDao;
import org.iplantc.service.jobs.dao.JobPublishedDao;
import org.iplantc.service.jobs.dao.JobQueueDao;
import org.iplantc.service.jobs.dao.JobWorkerDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobQueueFilterException;
import org.iplantc.service.jobs.exceptions.JobSchedulerException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobInterrupt;
import org.iplantc.service.jobs.model.JobQueue;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.phases.queuemessages.AbstractQueueJobMessage;
import org.iplantc.service.jobs.phases.queuemessages.AbstractQueueMessage.JobCommand;
import org.iplantc.service.jobs.phases.queuemessages.DeleteJobMessage;
import org.iplantc.service.jobs.phases.queuemessages.PauseJobMessage;
import org.iplantc.service.jobs.phases.queuemessages.ProcessJobMessage;
import org.iplantc.service.jobs.phases.queuemessages.StopJobMessage;
import org.iplantc.service.jobs.phases.schedulers.dto.JobQuotaInfo;
import org.iplantc.service.jobs.phases.schedulers.filters.JobQuotaChecker;
import org.iplantc.service.jobs.phases.schedulers.filters.PrioritizedJobs;
import org.iplantc.service.jobs.phases.schedulers.filters.ReadyJobs;
import org.iplantc.service.jobs.phases.schedulers.strategies.IJobStrategy;
import org.iplantc.service.jobs.phases.schedulers.strategies.IStrategyAccessors;
import org.iplantc.service.jobs.phases.schedulers.strategies.ITenantStrategy;
import org.iplantc.service.jobs.phases.schedulers.strategies.IUserStrategy;
import org.iplantc.service.jobs.phases.utils.QueueConstants;
import org.iplantc.service.jobs.phases.workers.AbstractPhaseWorker;
import org.iplantc.service.jobs.phases.workers.ArchivingWorker;
import org.iplantc.service.jobs.phases.workers.MonitoringWorker;
import org.iplantc.service.jobs.phases.workers.PhaseWorkerParms;
import org.iplantc.service.jobs.phases.workers.StagingWorker;
import org.iplantc.service.jobs.phases.workers.SubmittingWorker;
import org.iplantc.service.jobs.queue.SelectorFilter;
import org.iplantc.service.jobs.util.TenantQueues;
import org.iplantc.service.jobs.util.TenantQueues.UpdateResult;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

/** Main job scheduler implementation class.
 * 
 * Asynchronous Interrupts
 * -----------------------
 * 
 * Jobs need to be interrupted during any processing phase.  When worker threads
 * are blocked on certain IO calls or sleep(), a signal can be sent that wakes up 
 * the thread.  When a thread is active, however, a cooperative approach is needed
 * in which the thread checks if an interrupt has occurred.  Below is an outline 
 * of the scheduler's cooperative interrupt mechanism.
 * 
 * The main components of the interrupt mechanism are:
 * 
 *  1. All schedule, topic and worker threads
 *  2. Jobs table and DAO
 *  3. Jobs interrupt table and DAO
 *  4. Scheduler topic
 *  5. Interrupt clean up thread
 *  
 * Once a job request is received by the jobs subsystem, the job is inserted into the 
 * jobs table.  At any time, the job's in-memory representation may also be on a worker 
 * queue or executing in a worker.  When an interrupts occurs, all job artifacts must
 * transition to a new state. 
 *   
 * There are two types of interrupts, those that are job-specific and those that are 
 * not.  Job-specific interrupts need to be routed and handled by the worker thread
 * that is currently processing the job, if one exists.  Non job-specific interrupts 
 * change the state of the jobs subsystem and are typically handled by the scheduler's 
 * topic thread.  
 * 
 * See AbstractQueueMessage.JobCommand for all defined interrupt messages.  
 * 
 * A correct interrupt handling design prohibits race conditions in which interrupts
 * are lost or processed more than once.  Job-specific interrupts have to be serviced no 
 * matter what processing phase the job is in or where references to the job may exist.  
 * Non job-specific interrupts may or may not effect currently processing jobs.  
 * 
 * In addition to handling each interrupt exactly once (or at least idempotently), the 
 * other main design consideration is to integrate the new mechanism into the existing 
 * codebase as seamlessly as possible.  This requirement means that Hibernate will still 
 * be used where it exists, though new table DAOs will use Hibernate only to acquire a 
 * session.
 * 
 * ++++ Job-Specific Interrupts ++++
 * 
 * Application interrupts to STOP, DELETE or PAUSE a job continue to be processed by 
 * JobManager.  Under the previous Quartz-based design, an application interrupt resolved 
 * to an interrupt to a Quartz job.  Under the new design, an application
 * interrupt resolves to a database update, which is at some later time read by a 
 * worker thread.  Worker threads poll the database at convenient points during their
 * processing to check for a job-specific interrupt.  When a worker detects an interrupt
 * for its job, the worker ceases processing the job and waits for the next available job.
 *  
 * The JobManager class integrates new and old code.  The kill() and hide() methods continue  
 * to update a job's status and other fields in the jobs table when a job-specific interrupt
 * is received.  They also cancel in-flight transfers as before.  What's new, however, 
 * is the addition of a call to TopicMessageSender.sendJobMessage(message).  This call 
 * places a durable message on the scheduler topic.  This topic is shared by all schedulers 
 * for all phases.  
 * 
 * The scheduler topic and its messages comprise the external interface to the new interrupt 
 * mechanism.  When a job-specific interrupt message is placed on the topic, the topic 
 * thread reads it and writes an interrupt record to the job_interrupts table.  The 
 * interrupted job could be at any point in its processing.  Specifically, a job's status
 * could be marked STOPPED or PAUSED and the job could be in one of the following runtime
 * states:
 * 
 *  a. Not scheduled
 *  b. On a worker queue
 *  c. Executing in a worker thread
 *  
 * Not scheduled means a scheduler has not yet placed the job on a phase queue.  In this
 * case, the job is finished and further processing will not occur.  The interrupt record
 * for this job has an expiration time after which the Interrupt Clean Up thread will
 * remove it from the job_interrupts table.
 * 
 * If the job is queued or executing then a worker thread will eventually poll the 
 * job_interrupts table looking for interrupts for the job it's servicing.  When interrupts 
 * are found, the worker services the interrupts in the order they were created (oldest 
 * first).  Multiple interrupts to stop job processing are allowed, but once a job is 
 * stopped no further interrupts have an effect. 
 * 
 * As an aside, the old and new code are not perfectly fused:  The old code could fail
 * after changing a job's status in the database but before queuing an interrupt 
 * message on the scheduler topic.  In this case, an interrupt could be lost.  The new
 * design implements safe JobManager.updateStatus() methods that detect when a job's status 
 * is in a finished state before attempting to update the status.  If a finished state is 
 * detected, updateState() disallows the status update and throws an exception.    
 * 
 * ++++ Non Job-Specific Interrupts ++++ 
 *  
 * Non job-specific interrupt messages are administrative commands carried out by each
 * scheduler's topic thread.  These commands scheduler scheduler shutdown, assigning
 * new worker threads to a queue, and decreasing the number of worker thread dedicated
 * to a queue. 
 *  
 * @author rcardone
 */

public abstract class AbstractPhaseScheduler 
  implements Runnable, Thread.UncaughtExceptionHandler, IStrategyAccessors
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(AbstractPhaseScheduler.class);
    
    // Inbound topic queuing information.
    private static final String TOPIC_EXCHANGE_NAME = QueueConstants.TOPIC_EXCHANGE_NAME;
    private static final String TOPIC_QUEUE_NAME = QueueConstants.TOPIC_QUEUE_NAME;
    private static final String TOPIC_ALL_BINDING_KEY = QueueConstants.TOPIC_ALL_BINDING_KEY;
    
    // Suffixes used in naming.
    private static final String THREADGROUP_SUFFIX = "-ThreadGroup";
    private static final String THREAD_SUFFIX = "-Thread";
    
    // Number of milliseconds to wait in various polling situations.
    private static final int POLLING_NORMAL_DELAY  = 10000;
    private static final int POLLING_FAILURE_DELAY = 15000;
    private static final int LEASE_RENEWAL_DELAY = (JobLeaseDao.LEASE_SECONDS / 4) * 1000;
    
    // Milliseconds between attempts to delete expired interrupts.
    private static final int INTERRUPT_DELETE_DELAY = 240000; // 4 minutes
    
    // Milliseconds between attempts to roll back zombie jobs.
    protected static final int ZOMBIE_MONITOR_DELAY = 600000; // 10 minutes
    
    // Milliseconds to wait (or poll) for threads to terminate 
    // after being interrupted.
    private static final int THREAD_DEATH_DELAY = 10000;
    private static final int THREAD_DEATH_POLL_DELAY  = 100;
    
    // RabbitMQ connection close delay in milliseconds.
    private static final int CONNECTION_CLOSE_DELAY = 5000;
    
    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    // The only job phase that this instance services.
    protected final JobPhaseType _phaseType;
    
    // Strategy fields.
    private final ITenantStrategy _tenantStrategy;
    private final IUserStrategy   _userStrategy;
    private final IJobStrategy    _jobStrategy;

    // A unique name for this scheduler.
    private final String _name;
    
    // The parent thread group of all thread groups created by this scheduler.
    // By default, this thread group is not a daemon, so it will not be destroyed
    // if it becomes empty.
    private final ThreadGroup _phaseThreadGroup;
    
    // This phase's tenant/queue mapping. The keys are tenant ids, the values
    // are the lists of job queues defined for that tenant.  The queues are 
    // listed in priority order.
    private final HashMap<String,List<JobQueue>> _tenantQueues = new HashMap<>();
    
    // Monotonically increasing sequence number generator used as part of thread names.
    private static final AtomicInteger _threadSeqno = new AtomicInteger(0);
    
    // This flag is set by the one scheduler instance that 
    // updates the tenant queues on invocation.
    private static boolean _updatedTenantQueue = false;
    
    // A thread that listens on the TOPIC_QUEUE_NAME for AbstractQueueMessage.JobCommand
    // messages.  These messages include worker interrupts and scheduler interrupts.  
    private Thread _topicThread;
    
    // A thread that periodically removes expired interrupts from database.
    // This thread is only created and started on the single STAGING scheduler
    // that obtains a lease.
    private Thread _interruptCleanUpThread;
    
    // A thread that periodically detects and tries to recover jobs that are not
    // making progress.  This thread is only created and started on the single
    // MONITORING scheduler that obtains a lease.
    private Thread _zombieCleanUpThread;
    
    // Initialize a reusable mapper for writing queue messages.
    private ObjectMapper queueMessageMapper = new ObjectMapper();
        
    // This phase's queuing artifacts.
    private ConnectionFactory    _factory;
    private Connection           _inConnection;
    private Connection           _outConnection;
    private Channel              _topicChannel;
    private Channel              _schedulerChannel;
    private String               _topicChannelConsumerTag;
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    /** Create the phase-specific scheduler with its thread group.
     * 
     * @param phaseType the phase that this scheduler services
     * @throws JobException on error
     */
    protected AbstractPhaseScheduler(JobPhaseType phaseType,
                                     ITenantStrategy tenantStrategy,
                                     IUserStrategy userStrategy,
                                     IJobStrategy jobStrategy)
     throws JobException
    {
        // Check input.
        if (phaseType == null) {
            String msg = "A non-null phase type is required for PhaseScheduler initialization.";
            _log.error(msg);
            throw new JobException(msg);
        }
        if (tenantStrategy == null) {
            String msg = "A non-null tenant strategy is required for PhaseScheduler initialization.";
            _log.error(msg);
            throw new JobException(msg);
        }
        if (userStrategy == null) {
            String msg = "A non-null user strategy is required for PhaseScheduler initialization.";
            _log.error(msg);
            throw new JobException(msg);
        }
        if (jobStrategy == null) {
            String msg = "A non-null job strategy is required for PhaseScheduler initialization.";
            _log.error(msg);
            throw new JobException(msg);
        }
        
        // Set the strategy fields.
        _tenantStrategy = tenantStrategy;
        _userStrategy = userStrategy;
        _jobStrategy = jobStrategy;
        
        // Assign our phase identity.
        _phaseType = phaseType;
        
        // Assign our unique name.
        _name = phaseType.name() + "-" + UUID.randomUUID();
        
        // Create parent thread group.
        _phaseThreadGroup = new ThreadGroup(phaseType.name() + THREADGROUP_SUFFIX);
    }
    
    /* ********************************************************************** */
    /*                            Abstract Methods                            */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* getPhaseTriggerStatus:                                                 */
    /* ---------------------------------------------------------------------- */
    /** Define the status that causes this phase scheduler to begin processing
     * a job.  The returned status list is the first filter the scheduler
     * uses to identify new work. 
     * 
     * @return the trigger status for new work in this phase.
     */
    protected abstract List<JobStatusType> getPhaseTriggerStatuses();
    
    /* ---------------------------------------------------------------------- */
    /* getPhaseCandidateJobs:                                                 */
    /* ---------------------------------------------------------------------- */
    /** Get a list of jobs that might be ready to run in this phase.  Some 
     * initial filtering that takes advantage of database access should take 
     * place here.  In addition the job list, the result object may contain
     * a post priority filter class used to further reduce the number of 
     * candidate jobs.   
     * 
     * @param statuses this phase's trigger statuses 
     * @return the initial list of vetted jobs from the database plus an optional
     *         post-priority filter
     * @throws JobSchedulerException on error
     */
    protected abstract ReadyJobs getPhaseCandidateJobs(List<JobStatusType> statuses)
      throws JobSchedulerException;
    
    /* ---------------------------------------------------------------------- */
    /* isFilteringPublishedJobs:                                              */
    /* ---------------------------------------------------------------------- */
    /** Each phase can specify if it allows jobs to be rescheduled multiple 
     * times by overriding this method.  Scheduling involves placing a job on an 
     * appropriate queue.  Some phases expect that a job is published to its 
     * queue exactly one time, other phases may republish the job until its 
     * status changes.
     * 
     * @return return false if republishing is prohibited (i.e., a job is 
     *                expected to be queued once in the phase); true if the 
     *                phase scheduler republishes the job until the job's
     *                status changes.
     */
    public boolean allowsRepublishing(){return false;}
    
    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* getPhaseType:                                                          */
    /* ---------------------------------------------------------------------- */
    /** Get the scheduler's phase.
     * 
     * @return the phased specified during construction.
     */
    public JobPhaseType getPhaseType(){return _phaseType;}
    
    /* ---------------------------------------------------------------------- */
    /* run:                                                                   */
    /* ---------------------------------------------------------------------- */
    /** Initialize all thread groups, threads, channels, queues and exchanges
     * used by this scheduler on start up.  When initialization completes, the 
     * executing thread will begin an infinite read loop on the scheduler topic.
     */
    @Override
    public void run() 
    {
        try {
            // Update tenant queues.
            updateTenantQueues();
            
            // Connect to the scheduler topic (incoming).
            initJobTopic();
            
            // Connect the database.
            initQueueCache();
            
            // Start each queue's workers.
            startQueueWorkers();
            
            // Subscribe to job topic and continuously monitor it.
            startTopicThread();
            
            // Initialize scheduler communication.
            initScheduler();
            
            // Begin scheduling read loop.
            schedule();
        }
        catch (Exception e)
        {
            // Let's try to shutdown the JVM.
            String msg = _phaseType.name() + 
                         " phase scheduler initialization failure.  Aborting.";
            _log.error(msg); // Already logged initial exception message.
            throw new RuntimeException(msg, e);
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* uncaughtException:                                                     */
    /* ---------------------------------------------------------------------- */
    /** Recover worker threads from an unexpected exceptions.  The JVM calls 
     * this method when a worker dies.  The intent is to start another worker 
     * thread with the same parameters as the dying thread after we log the 
     * incident.  
     */
    @Override
    public void uncaughtException(Thread t, Throwable e) 
    {
        // Print basic exception information.
        _log.error(e.toString());
        e.printStackTrace(); // stderr
        
        // Restart worker threads only.
        if (!(t instanceof AbstractPhaseWorker)) return;
        
        // Get the dead worker.
        AbstractPhaseWorker oldWorker = (AbstractPhaseWorker) t;
        
        // Do our best to release any jobs listed in the published table.
        releasePublishedJob(oldWorker);
        
        // Do our best to release any job claim.
        releaseJobClaim(oldWorker);
        
        // Create the new thread object.
        int newThreadNum = _threadSeqno.incrementAndGet();
        AbstractPhaseWorker newWorker = createWorkerThread(oldWorker.getThreadGroup(), 
                                                           oldWorker.getTenantId(), 
                                                           oldWorker.getQueueName(), 
                                                           newThreadNum);
          
        // Log more information.
        String msg = "Phase worker thread " + oldWorker.getName() + 
                     " (" + oldWorker.getThreadUuid() + ") died unexpectedly. " +
                     "Starting new worker " + newWorker.getName() + 
                     " (" + newWorker.getThreadUuid() + ").";
        _log.error(msg);
        
        // Let it rip.
        newWorker.start();
    }
    
    /* ********************************************************************** */
    /*                           Strategy Accessors                           */
    /* ********************************************************************** */
    @Override
    public ITenantStrategy getTenantStrategy(){return _tenantStrategy;}
    
    @Override
    public IUserStrategy getUserStrategy(){return _userStrategy;}
    
    @Override
    public IJobStrategy getJobStrategy(){return _jobStrategy;}

    /* ********************************************************************** */
    /*                           Protected Methods                            */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* getSchedulerName:                                                      */
    /* ---------------------------------------------------------------------- */
    protected String getSchedulerName(){return _name;}
    
    /* ---------------------------------------------------------------------- */
    /* getPhaseThreadGroup:                                                   */
    /* ---------------------------------------------------------------------- */
    protected ThreadGroup getPhaseThreadGroup(){return _phaseThreadGroup;}
    
    /* ---------------------------------------------------------------------- */
    /* getPhaseBindingKey:                                                    */
    /* ---------------------------------------------------------------------- */
    protected String getPhaseBindingKey()
    {
        return TOPIC_QUEUE_NAME + "." + _phaseType.name() + ".#";
    }
    
    /* ---------------------------------------------------------------------- */
    /* getPhaseInConnectionName:                                              */
    /* ---------------------------------------------------------------------- */
    protected String getPhaseInConnectionName()
    {
        return _phaseType.name() + "-InConnection";
    }
    
    /* ---------------------------------------------------------------------- */
    /* getPhaseOutConnectionName:                                             */
    /* ---------------------------------------------------------------------- */
    protected String getPhaseOutConnectionName()
    {
        return _phaseType.name() + "-OutConnection";
    }
    
    /* ---------------------------------------------------------------------- */
    /* getWorkerThreadGroupName:                                              */
    /* ---------------------------------------------------------------------- */
    protected String getWorkerThreadGroupName(String tenantId, String queueName)
    {
        // The JobQueueDao enforces that queue names begin with phase.tenantId.
        return queueName + THREADGROUP_SUFFIX;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getWorkerThreadName:                                                   */
    /* ---------------------------------------------------------------------- */
    protected String getWorkerThreadName(String tenantId, String queueName, int threadNum)
    {
        // The JobQueueDao enforces that queue names begin with phase.tenantId.
        return queueName + "_" + threadNum + THREAD_SUFFIX;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getTopicThreadName:                                                    */
    /* ---------------------------------------------------------------------- */
    protected String getTopicThreadName()
    {
        // Topic thread processing spans all tenants and queues.
        return _phaseType.name() + "_topic" + THREAD_SUFFIX;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getInterruptCleanUpThreadName:                                         */
    /* ---------------------------------------------------------------------- */
    protected String getInterruptCleanUpThreadName()
    {
        return _phaseType.name() + "_interruptCleanUp" + THREAD_SUFFIX;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getZombieCleanUpThreadName:                                            */
    /* ---------------------------------------------------------------------- */
    protected String getZombieCleanUpThreadName()
    {
        return _phaseType.name() + "_zombieCleanUp" + THREAD_SUFFIX;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getDefaultQueue:                                                       */
    /* ---------------------------------------------------------------------- */
    protected String getDefaultQueue(String tenantId)
    {
        return _phaseType.name() + "." + tenantId;
    }
    
    /* ---------------------------------------------------------------------- */
    /* toQueueableJSON:                                                       */
    /* ---------------------------------------------------------------------- */
    /** Create the json representation of a job on a worker queue.
     * 
     * @param job the job to be queued
     * @return json reference to the job
     * @throws IOException 
     * @throws JsonMappingException 
     * @throws JsonGenerationException 
     */
    protected String toQueueableJSON(Job job) 
      throws IOException
    {
        // Initialize the queueable object.
        ProcessJobMessage qjob = new ProcessJobMessage();
        qjob.name  = job.getName();
        qjob.uuid  = job.getUuid();
        qjob.epoch = job.getEpoch();
        
        // Write the object as a JSON string.
        return queueMessageMapper.writeValueAsString(qjob);
    }
    
    /* ---------------------------------------------------------------------- */
    /* getConnectionFactory:                                                  */
    /* ---------------------------------------------------------------------- */
    /** Return a connection factory, creating it if necessary.
     * 
     * @return this scheduler's queue connection factory.
     */
    protected ConnectionFactory getConnectionFactory()
    {
        // Create the factory if necessary.
        if (_factory == null) 
        {
            // Get a rabbitmq connection factory.
            _factory = new ConnectionFactory();
            
            // Set the factory parameters.
            // TODO: generalize w/auth & network info & heartbeat
            _factory.setHost("localhost");
            
            // Set automatic recover on.
            // TODO: Consider adding shutdown, cancel, recovery, etc. listeners.
            // TODO: Also consider how to handle unroutable messages
            _factory.setAutomaticRecoveryEnabled(true);
        }
        
        return _factory;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getInConnection:                                                       */
    /* ---------------------------------------------------------------------- */
    /** Return a inbound connection to the queuing subsystem, creating the 
     * connection if necessary.
     * 
     * @return this scheduler's connection
     * @throws JobSchedulerException on error.
     */
    protected Connection getInConnection()
     throws JobSchedulerException
    {
        // Create the connection if necessary.
        if (_inConnection == null)
        {
            try {_inConnection = getConnectionFactory().newConnection(getPhaseInConnectionName());}
            catch (IOException e) {
                String msg = "Unable to create new inbound connection to queuing subsystem: " +
                             e.getMessage();
                _log.error(msg, e);
                throw new JobSchedulerException(msg, e);
            } catch (TimeoutException e) {
                String msg = "Timeout while creating new inbound connection to queuing subsystem: " + 
                             e.getMessage();
                _log.error(msg, e);
                throw new JobSchedulerException(msg, e);
            }
        }
        
        return _inConnection;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getNewInChannel:                                                       */
    /* ---------------------------------------------------------------------- */
    /** Return a new inbound channel on the existing queuing system connection.
     * 
     * @return the new channel
     * @throws JobSchedulerException on error
     */
    protected Channel getNewInChannel()
      throws JobSchedulerException
    {
        // Create a new channel in this phase's connection.
        Channel channel = null;
        try {channel = getInConnection().createChannel();} 
         catch (IOException e) {
             String msg = "Unable to create channel on " + getPhaseInConnectionName() + 
                          ": " + e.getMessage();
             _log.error(msg, e);
             throw new JobSchedulerException(msg, e);
         }
         if (_log.isInfoEnabled()) 
             _log.info("Created channel number " + channel.getChannelNumber() + 
                       " on " + getPhaseInConnectionName() + ".");
         
         return channel;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getTopicChannel:                                                       */
    /* ---------------------------------------------------------------------- */
    /** Return the main scheduler thread's topic channel.  This channel should
     * never be used by any other thread than the thread on which the run()
     * method is invoked.  The topic is defined as durable, so it should outlast
     * any execution of the jobs application.  All phases share the the same
     * topic 
     * 
     * @return the topic channel
     * @throws JobSchedulerExceptionj on error
     */
    protected Channel getTopicChannel()
      throws JobSchedulerException
    {
        // Create the channel if necessary.
        if (_topicChannel == null)
        {
            // Create the channel.
            _topicChannel = getNewInChannel();
            
            // Set prefetch.
            int prefetchCount = 1;
            try {_topicChannel.basicQos(prefetchCount);}
                catch (IOException e) {
                    String msg = "Unable to set prefech on channel on " + 
                                 getPhaseInConnectionName() + 
                                 ": " + e.getMessage();
                    _log.error(msg, e);
                    throw new JobSchedulerException(msg, e);
                }
        }
        
        return _topicChannel;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getOutConnection:                                                      */
    /* ---------------------------------------------------------------------- */
    /** Return a outbound connection to the queuing subsystem, creating the 
     * connection if necessary.
     * 
     * @return this scheduler's connection
     * @throws JobSchedulerException on error.
     */
    protected Connection getOutConnection()
     throws JobSchedulerException
    {
        // Create the connection if necessary.
        if (_outConnection == null)
        {
            try {_outConnection = getConnectionFactory().newConnection(getPhaseOutConnectionName());}
            catch (IOException e) {
                String msg = "Unable to create new outbound connection to queuing subsystem: " +
                             e.getMessage();
                _log.error(msg, e);
                throw new JobSchedulerException(msg, e);
            } catch (TimeoutException e) {
                String msg = "Timeout while creating new outbound connection to queuing subsystem: " + 
                             e.getMessage();
                _log.error(msg, e);
                throw new JobSchedulerException(msg, e);
            }
        }
        
        return _outConnection;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getNewOutChannel:                                                      */
    /* ---------------------------------------------------------------------- */
    /** Return a new outbound channel on the existing queuing system connection.
     * 
     * @return the new channel
     * @throws JobSchedulerException on error
     */
    protected Channel getNewOutChannel()
      throws JobSchedulerException
    {
        // Create a new channel in this phase's connection.
        Channel channel = null;
        try {channel = getOutConnection().createChannel();} 
         catch (IOException e) {
             String msg = "Unable to create channel on " + getPhaseOutConnectionName() + 
                          ": " + e.getMessage();
             _log.error(msg, e);
             throw new JobSchedulerException(msg, e);
         }
         if (_log.isInfoEnabled()) 
             _log.info("Created channel number " + channel.getChannelNumber() + 
                       " on " + getPhaseOutConnectionName() + ".");
         
         return channel;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getQuotaCheckedJobs:                                                   */
    /* ---------------------------------------------------------------------- */
    /** Get jobs for Staging and Submitting phases and perform quota checks on
     * them. Quota checking only considers visible jobs, available systems and
     * dedicated configuration information, so these types of filtering are 
     * implicit in quota checking (see JobDao quota call for details).
     * 
     * All quota checking is performed here by schedulers.  Once a scheduler
     * determines that a candidate job would not exceed a quota if it were
     * scheduled, the job is allowed to proceed.  Quota checking uses a data
     * snapshot that is only an approximation of the number of jobs that could
     * be active when a worker starts processing a newly scheduled job.  For
     * example, queued jobs that have not yet been seen by a worker are not
     * counted as "active" by JobQuotaChecker and, therefore, are not included
     * in the quota check.
     * 
     * In the pathological case, a large number of jobs can be queued and not
     * counted as active during quota checking.  The number of jobs in any
     * quota category that could concurrently run is bounded by the number of 
     * all workers on all queues assigned to that phase, a number which has
     * no relationship to any quota.  If such pathological cases are actually
     * seen in practice, then a mechanism to track both the number of active
     * jobs plus the number of queued jobs might be needed.
     * 
     * @param statuses phase-specific trigger statuses
     * @return the list of jobs that do not violate any quotas
     * @throws JobSchedulerException on error
     */
    protected ReadyJobs getQuotaCheckedJobs(List<JobStatusType> statuses) 
      throws JobSchedulerException
    {
        // Get candidate job uuids with quota information.
        List<JobQuotaInfo> quotaInfoList = null;
        try {quotaInfoList = JobDao.getSchedulerJobQuotaInfo(_phaseType, statuses);}
        catch (Exception e) {
            String msg = _phaseType.name() + " scheduler unable to retrieve job quota information.";
            _log.error(msg, e);
            throw new JobSchedulerException(msg, e);
        }
        
        // Quit now if there's nothing to do.
        if (quotaInfoList.isEmpty()) return new ReadyJobs(new LinkedList<Job>());
        
        // Retrieve active job summary information to check quotas.
        JobQuotaChecker quotaChecker = null;
        try {
            // Create the checker object used to check quotas.
            quotaChecker = new JobQuotaChecker(quotaInfoList);
        
            // Remove records that already exceed their quotas.
            ListIterator<JobQuotaInfo> it = quotaInfoList.listIterator();
            while (it.hasNext()) {
                JobQuotaInfo info = it.next();
                if (quotaChecker.exceedsQuota(info)) {
                    _log.warn("Quota exceeded for job " + info.getUuid() + ", skipping.");
                    it.remove(); 
                }
            }
        }
        catch (JobException e) {
            String msg = _phaseType.name() + " scheduler unable to retrieve active job information.";
            _log.error(msg, e);
            throw new JobSchedulerException(msg, e);
        }
        
        // Quit now if there's nothing to do.
        if (quotaInfoList.isEmpty()) return new ReadyJobs(new LinkedList<Job>());
        
        // Create list of job uuids still in play. 
        List<String> uuids = new ArrayList<String>(quotaInfoList.size());
        for (JobQuotaInfo info : quotaInfoList) uuids.add(info.getUuid());
            
        // Retrieve all jobs that passed quota.   Note that the called routine will
        // silently limit on the number of uuids per request.  We'll pick up any 
        // unserviced jobs on the next cycle, so this shouldn't be a problem.  
        // We will, however, have to revisit the issue in the unlikely event that
        // certain jobs starve because they always appear beyond the cut off point.
        List<Job> jobs = null;
        try {jobs = JobDao.getSchedulerJobsByUuids(uuids);}
            catch (Exception e) {
                // Log and continue.
                String msg = "Scheduler for phase " + _phaseType.name() +
                             " is unable to retrieve jobs by UUID for " + 
                             jobs.size() + " jobs.";
                _log.error(msg, e);
                throw new JobSchedulerException(msg, e);
            }
        
        return new ReadyJobs(jobs, quotaChecker);
    }
    
    /* ---------------------------------------------------------------------- */
    /* getUnfilteredJobs:                                                     */
    /* ---------------------------------------------------------------------- */
    /** Get the list of jobs in one of this phase's trigger states that have not
     * already been published by this phase.  This method is appropriate in 
     * circumstances where all jobs returned will be processed.  In cases where
     * additional filtering is best performed in memory, such as for the staging,
     * submitting or monitoring phases, then taking the approach of initially 
     * retrieving a small subset of fields from each job and then issuing another
     * query for a smaller number of full job objects may be more efficient.  
     * 
     * @param statuses the trigger statuses for this phase
     * @return the list of unpublished job in this phase that have a trigger status
     * @throws JobSchedulerException on error
     */
    protected ReadyJobs getUnfilteredJobs(List<JobStatusType> statuses) 
     throws JobSchedulerException
    {
        // Since we don't need to do any extra in-memory filtering of the
        // results, make a call that returns all non-published jobs for the phase.
        List<Job> jobs = null;
        try {
            jobs = JobDao.getSchedulerJobs(_phaseType, statuses);
        } catch (Exception e) {
            String msg = _phaseType.name() + 
                         " scheduler unable to retrieve ready jobs.";
            _log.error(msg, e);
            throw new JobSchedulerException(msg, e);
        }

        return new ReadyJobs(jobs);
    }
    
    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* updateTenantQueues:                                                    */
    /* ---------------------------------------------------------------------- */
    /** Read the queue configuration resource file and add any new queues to the
     * database.
     */
    private static synchronized void updateTenantQueues()
    {
        // Only one scheduler gets to update the tenant queues
        // on any jobs-core invocation.
        if (_updatedTenantQueue) return;
        
        // Call the update utility.
        TenantQueues tenantQueues = new TenantQueues();
        try {
            // Perform the update.
            UpdateResult result = tenantQueues.updateAll();
            if (_log.isDebugEnabled()) {
                StringBuilder buf = new StringBuilder(512);
                buf.append("\n-------------- Queue Update Results -------------\n");
                buf.append(result.toString());
                buf.append("-------------------------------------------------\n");
                _log.debug(buf.toString());
            }
        }
        catch (Exception e) {
            _log.error("Tenant queue definition not refreshed, using existing definitions.");
        }
        finally {
            // We only get one shot at queue refresh.
            _updatedTenantQueue = true;
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* initJobTopic:                                                          */
    /* ---------------------------------------------------------------------- */
    /** Create the queuing system artifacts needed to read and manage the
     * job topic.  Bind the topic using the All binding key and the 
     * phase-specific key.  The topic thread reads from the job topic
     * and writes to the interrupt topic.
     * 
     * @throws JobSchedulerException on error
     */
    private void initJobTopic() throws JobSchedulerException
    {
        // Get a local reference to the topic channel field.
        Channel topicChannel = getTopicChannel();
        
        // Create the durable, non-autodelete topic exchange.
        boolean durable = true;
        try {topicChannel.exchangeDeclare(TOPIC_EXCHANGE_NAME, "topic", durable);}
            catch (IOException e) {
                String msg = "Unable to create exchange on " + getPhaseInConnectionName() + 
                        "/" + topicChannel.getChannelNumber() + ": " + e.getMessage();
                _log.error(msg, e);
                throw new JobSchedulerException(msg, e);
            }
        
        // Create the durable topic with a well-known name.
        durable = true;
        boolean exclusive = false;
        boolean autoDelete = false;
        try {topicChannel.queueDeclare(TOPIC_QUEUE_NAME, durable, exclusive, autoDelete, null);}
            catch (IOException e) {
                String msg = "Unable to declare topic queue " + TOPIC_QUEUE_NAME +
                             " on " + getPhaseInConnectionName() + "/" + 
                             topicChannel.getChannelNumber() + ": " + e.getMessage();
                _log.error(msg, e);
                throw new JobSchedulerException(msg, e);
            }
        
        // Bind the topic queue to the topic exchange with the All binding key.
        try {topicChannel.queueBind(TOPIC_QUEUE_NAME, TOPIC_EXCHANGE_NAME, TOPIC_ALL_BINDING_KEY);}
            catch (IOException e) {
                String msg = "Unable to bind topic queue " + TOPIC_QUEUE_NAME +
                         " with binding key " + TOPIC_ALL_BINDING_KEY +
                         " on " + getPhaseInConnectionName() + "/" + 
                         topicChannel.getChannelNumber() + ": " + e.getMessage();
                _log.error(msg, e);
                throw new JobSchedulerException(msg, e);
            }
        
        // Bind the topic queue to the topic exchange with the stage-specific binding key.
        try {topicChannel.queueBind(TOPIC_QUEUE_NAME, TOPIC_EXCHANGE_NAME, getPhaseBindingKey());}
            catch (IOException e) {
                String msg = "Unable to bind topic queue " + TOPIC_QUEUE_NAME +
                        " with binding key " + getPhaseBindingKey() +
                         " on " + getPhaseInConnectionName() + "/" + 
                         topicChannel.getChannelNumber() + ": " + e.getMessage();
                _log.error(msg, e);
                throw new JobSchedulerException(msg, e);
            }
    }

    /* ---------------------------------------------------------------------- */
    /* initQueueCache:                                                        */
    /* ---------------------------------------------------------------------- */
    /** Initialize the mapping of tenants to their prioritized queues.
     * 
     * @throws JobSchedulerException
     */
    private void initQueueCache() throws JobSchedulerException
    {
        // Retrieve all queues defined for this tenant for this stage.
        JobQueueDao dao = new JobQueueDao();
        
        // Query all for this phase.  The results are listed in 
        // (tenant, phase, priority desc) order.
        List<JobQueue> queues = null;
        try {queues = dao.getQueues(_phaseType);}
            catch (Exception e)
            {
                String msg = "Unable to retrieve job queue definitions from database: " +
                             e.getMessage();
                _log.error(msg, e);
                throw new JobSchedulerException(msg, e); 
            }
        
        // Split list into a hash of lists.  We take advantage of the 
        // order of the queues in the list to segregate each tenant's 
        // queues into priority-ordered lists.
        for (JobQueue queue : queues) {
            
            // Get the tenant list for this queue.
            String tenantId = queue.getTenantId();
            List<JobQueue> tenantList = _tenantQueues.get(tenantId);
            
            // Create this tenant's list if it doesn't exist yet.
            if (tenantList == null) {
               tenantList = new ArrayList<JobQueue>();
               _tenantQueues.put(tenantId, tenantList);
            }
            
            // Add the queue to the end of the tenant list.
            tenantList.add(queue);
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* subscribeToJobTopic:                                                   */
    /* ---------------------------------------------------------------------- */
    /** Process messages from the topic queue.
     * 
     * @throws JobSchedulerException
     */
    private void subscribeToJobTopic() throws JobSchedulerException
    {
        // Reusable json mapper.
        final ObjectMapper jsonMapper = new ObjectMapper();
        
        // Create the topic queue consumer.
        Consumer consumer = new DefaultConsumer(_topicChannel) {
          @Override
          public void handleDelivery(String consumerTag, Envelope envelope,
                                     AMQP.BasicProperties properties, byte[] body) 
            throws IOException 
          {
              // Tracing.
              if (_log.isDebugEnabled()) 
                  dumpMessageInfo(consumerTag, envelope, properties, body);
              
              // ---------------- Decode Message ----------------
              // Once we receive a message, we're on the hook to ack or reject it.
              boolean ack = true;  // assume success
              
              // Read the queued json generically.
              // Null body is caught here.
              JsonNode node = null;
              try {node = jsonMapper.readTree(body);}
              catch (IOException e) {
                  // Log error message.
                  String msg = _phaseType.name() +  
                     " topic reader cannot decode data from topic " + 
                     TOPIC_QUEUE_NAME + ": " + e.getMessage();
                  _log.error(msg, e);
                  ack = false;
              }
              if (node == null) ack = false;
              
              // Get the command.
              JobCommand command = null;
              if (ack)
              {
                  // Get the command field from the queued json.
                  String cmd = node.path("command").asText();
                  try {command = JobCommand.valueOf(cmd);}
                  catch (Exception e) {
                      String msg = _phaseType.name() + 
                           " topic reader decoded an invalid command (" + cmd + 
                           ") from topic " + TOPIC_QUEUE_NAME + ": " + e.getMessage();
                      _log.error(msg, e);
                      ack = false;
                  }
              }
              
              // ---------------- Execute Request ---------------
              // Process the command.
              if (ack)
              {
                  try {
                      switch (command)
                      {
                          // ################### Worker Interrupts ###################
                          // ----- Job interrupts
                          case TPC_DELETE_JOB:
                          case TPC_PAUSE_JOB:
                          case TPC_STOP_JOB:
                              ack = doJobInterrupt(command, envelope, properties, body);
                              break;
                              
                          // ################# Scheduler Interrupts ##################
                          // ----- Shutdown scheduler
                          case TPC_SHUTDOWN:
                              _log.info("Topic message TPC_SHUTDOWN not implemented yet.");
                              ack = false;
                              break;
                          // ----- Reset the number of worker threads servicing a queue
                          case TPC_RESET_NUM_WORKERS:
                              _log.info("Topic message TPC_RESET_NUM_WORKERS not implemented yet.");
                              ack = false;
                              break;
                          // ----- Reset a queue's priority 
                          case TPC_RESET_PRIORITY:
                              _log.info("Topic message TPC_RESET_PRIORITY not implemented yet.");
                              ack = false;
                              break;
                          // ----- Reset a queue's maximum message bound 
                          case TPC_RESET_MAX_MESSAGES:
                              _log.info("Topic message TPC_RESET_MAX_MESSAGES not implemented yet.");
                              ack = false;
                              break;
                          // ----- Test message input case   
                          case NOOP:
                              ack = doNoop(node);
                              break;
                           // ----- Invalid input case    
                          default:
                              // Log the invalid input (we know the body is not null).
                              String msg = _phaseType.name() + 
                                  " topic reader received an invalid command: " + (new String(body));
                              _log.error(msg);
                      
                              // Reject this input.
                              ack = false;
                              break;
                      }
                  }
                  catch (Exception e) {
                      // Command processor are not supposed to throw exceptions,
                      // but we double check anyway.
                      String msg = _phaseType.name() + 
                               " topic reader caught an unexpected command processor exception: " + 
                               e.getMessage();
                      _log.error(msg, e);
                      ack = false;
                  }
              }
            
              // ----------------- Clean Up ---------------------------
              // Don't leave stale state around.
              TenancyHelper.setCurrentTenantId(null);
              TenancyHelper.setCurrentEndUser(null);
              
              // Determine whether to ack or nack the request.
              if (ack) {
                  // Don't forget to send the ack!
                  boolean multipleAck = false;
                  try {_topicChannel.basicAck(envelope.getDeliveryTag(), multipleAck);}
                  catch (IOException e) {
                      // We're in trouble if we cannot acknowledge a message.
                      String msg = _phaseType.name() +  
                            " topic reader cannot acknowledge a message received on topic " + 
                            TOPIC_QUEUE_NAME + ": " + e.getMessage();
                      _log.error(msg, e);
                  }
              }
              else {
                  // Reject this unreadable message so that
                  // it gets discarded or dead-lettered.
                  boolean requeue = false;
                  try {_topicChannel.basicReject(envelope.getDeliveryTag(), requeue);} 
                  catch (IOException e) {
                      // We're in trouble if we cannot reject a message.
                      String msg = _phaseType.name() +  
                            " topic reader cannot reject a message received on topic " + 
                            TOPIC_QUEUE_NAME + ": " + e.getMessage();
                      _log.error(msg, e);
                  }
              }
          }
        };
        
        // Tracing.
        if (_log.isDebugEnabled())
            _log.debug("[*] " + _phaseType.name() + " scheduler consuming " + 
                    TOPIC_QUEUE_NAME + " topic.");

        // We don't auto-acknowledge topic broadcasts.
        boolean autoack = false;
        try {
            // Save the server generated tag for this consumer.  The tag can be used
            // as input on other APIs, such as basicCancel.
            _topicChannelConsumerTag = _topicChannel.basicConsume(TOPIC_QUEUE_NAME, 
                                                                  autoack, consumer);
        }
        catch (IOException e) {
            String msg = _phaseType.name() + " scheduler is unable consume messages from " + 
                    TOPIC_QUEUE_NAME + " topic.";
            _log.error(msg, e);
            throw new JobSchedulerException(msg, e);
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* startTopicThread:                                                      */
    /* ---------------------------------------------------------------------- */
    /** Start a thread to read from this phase's topic.
     * 
     */
    private void startTopicThread()
    {
        // Create the topic thread.
        _topicThread = 
           new Thread(_phaseThreadGroup, getTopicThreadName()) {
            @Override
            public void run() {
                
                // This thread is starting.
                if (_log.isDebugEnabled())
                    _log.debug("-> Starting topic thread " + getName() + ".");
                
                try {subscribeToJobTopic();}
                catch (JobSchedulerException e) {
                    String msg = getTopicThreadName() + " aborting! "  +
                         _phaseType.name() + 
                         " scheduler cannot receive any administrative requests.";
                    _log.error(msg);
                    throw new RuntimeException(msg);
                }
                
                // This thread is terminating.
                if (_log.isDebugEnabled())
                    _log.debug("<- Exiting topic thread " + getName() + ".");
                
                // Clear the thread reference.
                _topicThread = null;
            }
        };
        
        // Configure and start the thread.
        _topicThread.setDaemon(true);
        _topicThread.start();
    }
    
    /* ---------------------------------------------------------------------- */
    /* startQueueWorkers:                                                     */
    /* ---------------------------------------------------------------------- */
    /** Start the configured number of workers dedicated to each queue.  Each
     * worker thread services exactly one queue, though a queue can have multiple
     * workers.  
     */
    private void startQueueWorkers()
    {
        // Iterator through each tenant's queue list.
        for (Entry<String, List<JobQueue>> tenant : _tenantQueues.entrySet())
        {
            // Create the tenant thread group as a child group of the phase group.
            String tenantId = tenant.getKey();
            
            // Create each queue's worker threads.
            for (JobQueue jobQueue : tenant.getValue()) 
            {
                // Create the phase/tenant/queue thread group.
                ThreadGroup queueThreadGroup = 
                    new ThreadGroup(_phaseThreadGroup, 
                         getWorkerThreadGroupName(tenantId, jobQueue.getName()));      
                
                // Create the number of worker threads configured for this queue.
                for (int i = 0; i < jobQueue.getNumWorkers(); i++)
                {
                    // Create a new worker daemon thread in the queue-specific group.
                    AbstractPhaseWorker worker = 
                      createWorkerThread(queueThreadGroup, 
                                         tenantId, 
                                         jobQueue.getName(), 
                                         _threadSeqno.incrementAndGet());
                    
                    // Start the thread.
                    worker.start();
                }
            }
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* createWorkerThread:                                                    */
    /* ---------------------------------------------------------------------- */
    /** Create a worker thread instance.  The threadNum is a monotonically 
     * increasing integer that will not be reused or recycled, thus is unique
     * for this phase scheduler, though not unique across schedulers.
     * 
     * @param threadGroup the thread group of the worker thread
     * @param tenantId the tenant associatd with the queue
     * @param queueName the name of the queue serviced by the worker
     * @param threadNum the thread sequence number
     * @return
     */
    private AbstractPhaseWorker createWorkerThread(ThreadGroup threadGroup, String tenantId,
                                           String queueName, int threadNum)
    {
        // Initialize parameter passing object.
        PhaseWorkerParms parms = new PhaseWorkerParms();
        parms.threadGroup = threadGroup;
        parms.threadName = getWorkerThreadName(tenantId, queueName, threadNum);
        parms.connection = _inConnection;
        parms.scheduler = this;
        parms.tenantId = tenantId;
        parms.queueName = queueName;
        parms.threadNum = threadNum;
        
        // Create a worker any phase type.
        AbstractPhaseWorker worker = null;
        if (_phaseType == JobPhaseType.STAGING) worker = new StagingWorker(parms);
        else if (_phaseType == JobPhaseType.SUBMITTING) worker = new SubmittingWorker(parms);
        else if (_phaseType == JobPhaseType.MONITORING) worker = new MonitoringWorker(parms);
        else if (_phaseType == JobPhaseType.ARCHIVING) worker = new ArchivingWorker(parms);
        else throw new RuntimeException("Unknown JobPhaseType: " + _phaseType);
        
        // Set attributes.
        worker.setDaemon(true);
        worker.setUncaughtExceptionHandler(this);
        return worker;
    }
    
    /* ---------------------------------------------------------------------- */
    /* initScheduler:                                                         */
    /* ---------------------------------------------------------------------- */
    private void initScheduler() throws JobSchedulerException
    {
        // Get the channel the schedule thread uses to write to queues.
        _schedulerChannel = getNewOutChannel();
        
        // Create the exchange to publish to.
        boolean durable = true;
        try {_schedulerChannel.exchangeDeclare(QueueConstants.WORKER_EXCHANGE_NAME, 
                                               "direct", durable);}
        catch (IOException e) {
            String msg = "Unable to create exchange on " + getPhaseOutConnectionName() + 
                    "/" + _schedulerChannel.getChannelNumber() + ": " + e.getMessage();
            _log.error(msg, e);
            throw new JobSchedulerException(msg, e);
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* startInterruptCleanUpThread:                                           */
    /* ---------------------------------------------------------------------- */
    /** Start a thread to indefinitely search and destroy stale interrupts. */
    private void startInterruptCleanUpThread()
    {
        // Create the interrupt clean up thread.
        _interruptCleanUpThread = 
           new Thread(_phaseThreadGroup, getInterruptCleanUpThreadName()) {
            @Override
            public void run() {
                
                // This thread is starting.
                if (_log.isDebugEnabled())
                    _log.debug("-> Starting interrupt clean up thread " + getName() + ".");
                
                deleteExpiredInterrupts();
                
                // This thread is terminating.
                if (_log.isDebugEnabled())
                    _log.debug("<- Exiting interrupt clean up thread " + getName() + ".");
                
                // Clear the thread reference.
                _interruptCleanUpThread = null;
            }
        };
        
        // Configure and start the thread.
        _interruptCleanUpThread.setDaemon(true);
        _interruptCleanUpThread.start();
    }
    
    /* ---------------------------------------------------------------------- */
    /* startZombieCleanUpThread:                                              */
    /* ---------------------------------------------------------------------- */
    /** Start a thread to indefinitely search and destroy zombie jobs. */
    private void startZombieCleanUpThread()
    {
        // Create the zombie clean up thread.
        _zombieCleanUpThread = 
           new Thread(_phaseThreadGroup, getZombieCleanUpThreadName()) {
            @Override
            public void run() {
                
                // This thread is starting.
                if (_log.isDebugEnabled())
                    _log.debug("-> Starting zombie clean up thread " + getName() + ".");
                
                monitorZombies();
                
                // This thread is terminating.
                if (_log.isDebugEnabled())
                    _log.debug("<- Exiting zombie clean up thread " + getName() + ".");
                
                // Clear the thread reference.
                _zombieCleanUpThread = null;
            }
        };
        
        // Configure and start the thread.
        _zombieCleanUpThread.setDaemon(true);
        _zombieCleanUpThread.start();
    }
    
   /* ---------------------------------------------------------------------- */
    /* schedule:                                                              */
    /* ---------------------------------------------------------------------- */
    /** Main loop that polls database for new work for this phase.
     * 
     */
    private void schedule()
    {
        // Tracing.
        if (_log.isDebugEnabled())
            _log.debug(_phaseType.name() + " scheduler entering polling loop on thread " + 
                       Thread.currentThread().getName() + ".");
        
        // Create the lease access object.
        JobLeaseDao jobLeaseDao = new JobLeaseDao(_phaseType, _name);
        
        // Enter infinite scheduling loop.
        try {
            for (;;) {
                // ------------------- Acquire Lease ----------------------------
                // Acquire or reacquire the lease that grants this scheduler
                // instance the permission to query the jobs table.  If we don't
                // get the lease, we keep retrying until we do or are interrupted.
                // There should only be one active scheduler for each phase in
                // the system at a time.
                if (!jobLeaseDao.acquireLease()) waitToAcquireLease(jobLeaseDao);
                
                // ------------------- Begin Job Processing ---------------------
                // Only the staging scheduler can start an interrupt clean up thread.
                // There's nothing special about the staging scheduler; we only need
                // one clean up thread in the whole system.
                if ((this instanceof StagingScheduler) && _interruptCleanUpThread == null)
                    startInterruptCleanUpThread();
                
                // We assign zombie clean up to the monitoring scheduler.
                if ((this instanceof MonitoringScheduler) && _zombieCleanUpThread == null)
                    startZombieCleanUpThread();
                
                // Query the database for all candidate jobs for this phase.  This
                // method also maintains the published jobs set and filters the list
                // of candidate jobs using that set.
                ReadyJobs readyJobs = null;
                try {readyJobs = getJobsReadyForPhase();}
                    catch (Exception e) 
                    {
                        String msg = _phaseType.name() + " scheduler database polling " +
                                     "failure. Retrying after a short delay.";
                        _log.info(msg, e);
                        
                        // Wait for some period of time before trying again.
                        // Interrupt exceptions can be thrown from here.
                        waitForWork(jobLeaseDao, POLLING_FAILURE_DELAY);
                        continue;
                    }
            
                if (_log.isDebugEnabled())
                    _log.debug(_phaseType.name() + " scheduler retrieved " +
                               readyJobs.getJobs().size() + " jobs.");
            
                // ------------------- Check for Interrupts ---------------------
                // See if this thread was interrupted before doing a lot of processing.
                // The interrupt status gets cleared as a side-effect.
                if (Thread.interrupted()) {
                    String msg = "Scheduler thread " + Thread.currentThread().getName() +
                                 " for phase " + _phaseType.name() + 
                                 " interrupted while processing new work.";
                    throw new InterruptedException(msg);
                }
                
                // ------------------- Prioritize and Publish -------------------
                // We need to pick a tenant, pick a user, and then pick a job.  
                // We use plugable strategies for flexibility.  The default strategy
                // choices are to randomly pick a tenant, randomly pick a user, and
                // and then pick jobs from oldest to newest with regard to create time.
                if (!readyJobs.getJobs().isEmpty()) {
                    PrioritizedJobs prioritizedJobs = 
                        new PrioritizedJobs(getTenantStrategy(), getUserStrategy(), 
                                            getJobStrategy(), readyJobs);
                
                    // Publish the jobs in priority order as defined in the priority job object.
                    Iterator<Job> jobIterator = prioritizedJobs.iterator();
                    while (jobIterator.hasNext()) {
                        Job job = jobIterator.next();
                        publishJob(job);
                    }
                }
                
                // Wait for more jobs to accumulate while maintaining our job lease.
                // We add up to 999 milliseconds of random skew to avoid scheduler
                // contention at the database.
                int skew = (int) ThreadLocalRandom.current().nextDouble(0, 1000);
                waitForWork(jobLeaseDao, POLLING_NORMAL_DELAY + skew);
                
            } // End polling loop.
        } catch (InterruptedException e) {
            // Interrupts cause a graceful shutdown.
        } catch (Exception e) {
            // All other exceptions indicate some sort of problem.
            String msg = "Scheduler " + _name + " is shutting down because of an unexpected exception.";
            _log.error(msg, e);
        }
        finally {
            // Always try to release the lease since we might be holding it.
            jobLeaseDao.releaseLease();
            
            // Interrupt all threads in the phase's threadgroup.
            interruptThreads();
            
            // Close all dedicated connections.
            closeConnections();
            
            // Announce our termination.
            if (_log.isInfoEnabled()) {
                String msg = "Terminating thread " + Thread.currentThread().getName() +
                             " with scheduler name " + _name + ".";
                _log.info(msg);
            }
        }
    }

    /* ---------------------------------------------------------------------- */
    /* publishJob:                                                            */
    /* ---------------------------------------------------------------------- */
    /** Determine on which queue the job should be published and insert a 
     * job request message on that queue. 
     * 
     * @param job the job to be published
     */
    private void publishJob(Job job)
    {
        // Select a target queue name for this job.
        // The name is used as the routing key to a 
        // direct exchange.
        String routingKey = selectQueueName(job);
        
        // Publish the job.
        try {
            // Write the job to the selected worker queue.
            _schedulerChannel.basicPublish(QueueConstants.WORKER_EXCHANGE_NAME, 
                routingKey, QueueConstants.PERSISTENT_JSON, 
                toQueueableJSON(job).getBytes("UTF-8"));
           
            // Let's not publish this job to a queue more than once in this phase.
            // NOTE: A failure between the last statement leaves information about this
            //       job in an inconsistent state.  The worker thread that eventually
            //       services this job can resolve the situation.  See 
            //       AbstractPhaseWorker.detectDuplicateJob() for details.
            if (!allowsRepublishing())
                try {JobPublishedDao.publish(_phaseType, job.getUuid(), getSchedulerName());}
                catch (Exception e) {
                    String msg = _phaseType.name() + " scheduler failed to insert " +
                        job.getName() + " (" + job.getUuid() + ") into published jobs table. " + 
                        "It is possible that this job will be processed multiple times by the " +
                        _phaseType.name() + " scheduler.";
                    _log.error(msg, e);
                }
        
            // Tracing.
            if (_log.isDebugEnabled()) {
                String msg = _phaseType.name() + " scheduler published " +
                    job.getName() + " (" + job.getUuid() + ") to queue " + 
                    routingKey + ".";
                _log.debug(msg);
            }
        }
        catch (Exception e) {
            // TODO: Probably need better failure remedy when publish fails.
            String msg = _phaseType.name() + " scheduler failed to publish " +
                job.getName() + " (" + job.getUuid() + ") to queue " + 
                routingKey + ".  Retrying later.";
            _log.warn(msg, e);
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* getTenantUserJobMap:                                                   */
    /* ---------------------------------------------------------------------- */
    /** Create a map of tenants->users->jobs for use by scheduling algorithms.
     * The outermost keys are tenant IDs and their values is a map with user
     * keys.  The value associated with each user key is a list of jobs ready
     * to be processed for that user.
     * 
     * @param jobs all jobs ready to process in this phase
     * @return the ready jobs reorganized into a 2-level hash map
     */
    private HashMap<String,HashMap<String,List<Job>>> getTenantUserJobMap(List<Job> jobs)
    {
        // A map of tenants->users->jobs.
        HashMap<String,HashMap<String,List<Job>>> tenantMap = new HashMap<>();
        
        // Cycle through all the jobs placing them in their proper place in the map.
        for (Job job : jobs) {
            // Get the tenant bucket for this job.
            HashMap<String,List<Job>> userMap = tenantMap.get(job.getTenantId());
            if (userMap == null) {
                userMap = new HashMap<>();
                tenantMap.put(job.getTenantId(), userMap);
            }
            
            // Get the user bucket for this job.
            List<Job> userJobList = userMap.get(job.getOwner());
            if (userJobList == null) {
                userJobList = new ArrayList<Job>();
                userMap.put(job.getOwner(), userJobList);
            }
            
            // Add the job to its list.
            userJobList.add(job);
        }
        
        return tenantMap;
    }

    /* ---------------------------------------------------------------------- */
    /* interruptThreads:                                                      */
    /* ---------------------------------------------------------------------- */
    /** Interrupt all threads spawned by this thread and wait a configured 
     * amount of time for the threads to terminate.
     */
    private void interruptThreads()
    {
        // Interrupt all threads in this phase's threadgroup and all of
        // its subgroups.  These threads include the interrupt clean up
        // thread, the topic thread and all worker threads.
        _phaseThreadGroup.interrupt();
        
        // Wait a limited amount of time for the number of active threads 
        // to go to zero.  This approach is simple but may give unexpected
        // results if the system implicitly adds threads to the group.
        for (int i = THREAD_DEATH_DELAY; i > 0; i -= THREAD_DEATH_POLL_DELAY)
        {
            // We can immediately return when no descendent thread are still active.
            int activeThreads = _phaseThreadGroup.activeCount();
            if (activeThreads == 0) break;
            try {Thread.sleep(THREAD_DEATH_POLL_DELAY);} catch (Exception e){return;}
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* closeConnections:                                                      */
    /* ---------------------------------------------------------------------- */
    private void closeConnections()
    {
        // Close all rabbitmq connections.
        if (_inConnection != null)
            try {_inConnection.close(CONNECTION_CLOSE_DELAY);}
            catch (Exception e) {
                String msg = "Error closing inbound connection " + 
                             _inConnection.getClientProvidedName() + ".";
                _log.error(msg, e);
            }
        if (_outConnection != null)
            try {_outConnection.close(CONNECTION_CLOSE_DELAY);}
            catch (Exception e) {
                String msg = "Error closing outbound connection " + 
                             _outConnection.getClientProvidedName() + ".";
                _log.error(msg, e);
            }
    }
    
    /* ---------------------------------------------------------------------- */
    /* waitToAcquireLease:                                                    */
    /* ---------------------------------------------------------------------- */
    /** Wait for period of time between lease acquisition retries. The wait
     * time is the average time left on an existing time.  The period is short 
     * enough to provide responsiveness if the lease holder abruptly terminates or
     * becomes inaccessible. This method checks the thread's interrupted flag 
     * before sleeping. The method only terminates if the lease is acquired or
     * on interrupt.   
     * 
     * @param jobLeaseDao access object for lease renewal
     * @return true if the job lease is acquired.
     * @throws InterruptedException when the thread detects an interrupt
     */
    private boolean waitToAcquireLease(JobLeaseDao jobLeaseDao) 
     throws InterruptedException
    {
        // If we lost the lease, shutdown the interrupt clean up thread.
        // Get a local reference to the thread so it doesn't disappear
        // underneath us.
        Thread interruptCleanUpThread = _interruptCleanUpThread;
        if (interruptCleanUpThread != null) 
            try {interruptCleanUpThread.interrupt();}
                catch (Exception e){}
        
        // Calculate retry interval to be the average time a lease might
        // be held by a defunct scheduler.
        final int millis = (JobLeaseDao.LEASE_SECONDS / 2) * 1000;
        
        // Keep trying to acquire lease.
        while (true) {
            
            // See if this thread was interrupted before sleeping.
            // Note that the interrupt flag is cleared just like when
            // an interrupt exception is thrown.
            if (Thread.interrupted()) {
                String msg = "Scheduler thread " + Thread.currentThread().getName() +
                             " for phase " + _phaseType.name() + 
                             " interrupted while waiting to acquire lease.";
                throw new InterruptedException(msg);
            }
            
            // Wait before retrying to acquire the job lease.
            // We rethrow any interruption exceptions.
            try {Thread.sleep(millis);}
            catch (InterruptedException e) {
                String msg = "Scheduler thread " + Thread.currentThread().getName() +
                             " interrupted while waiting to acquire job lease.";
                _log.info(msg);
                throw e;
            }
            
            // We're done if we get the lease.
            if (jobLeaseDao.acquireLease()) break;
        }
        
        // We got the lease if we're here.
        return true;
    }
    
    /* ---------------------------------------------------------------------- */
    /* waitForWork:                                                           */
    /* ---------------------------------------------------------------------- */
    /** Sleep for the configured period of time to allow new work to accumulate 
     * in the job table.  Sleep may be interrupted to renew our job lease. This
     * method checks the thread's interrupted flag before sleeping.
     * 
     * @param jobLeaseDao access object for lease renewal
     * @param sleepMillis the total time in milliseconds to sleep
     * @throws InterruptedException when the thread detects an interrupt
     */
    private void waitForWork(JobLeaseDao jobLeaseDao, int sleepMillis) 
     throws InterruptedException
    {
        // Set up the overall sleep interval not counting lease renewal.
        int windDown = sleepMillis;
        while (windDown > 0) {
            
            // See if this thread was interrupted before sleeping.
            if (Thread.interrupted()) {
                String msg = "Scheduler thread " + Thread.currentThread().getName() +
                             " for phase " + _phaseType.name() + 
                             " interrupted while waiting for new work.";
                throw new InterruptedException(msg);
            }
            
            // Calculate the wake up time to allow for lease renewal.
            int delay = Math.min(windDown, LEASE_RENEWAL_DELAY);
            Thread.sleep(delay);
            windDown -= delay;
            
            // Determine if this is a lease renewal wake up.
            if (windDown > 0) jobLeaseDao.acquireLease();
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* deleteExpiredInterrupts:                                               */
    /* ---------------------------------------------------------------------- */
    /** Delete expired interrupts periodically. */
    private void deleteExpiredInterrupts()
    {
        // Check for expired interrupts indefinitely.
        while (true) 
        {
            // Attempt to delete any expired interrupts and swallow any exceptions.
            try {
                int deleted = JobInterruptDao.deleteExpiredInterrupts();
                if (_log.isDebugEnabled()) {
                    _log.debug("Scheduler " + getSchedulerName() + 
                               " deleted " + deleted + " expired interrupts.");
                }
            }
            catch (Exception e) {
                // Just log the problem.
                String msg = getInterruptCleanUpThreadName() + 
                             " failed to delete expired interrupts but will try again.";
                _log.error(msg);
            }
            
            // Check for interrupts before sleeping.
            if (Thread.interrupted()) {
                if (_log.isInfoEnabled()) {
                    String msg = getInterruptCleanUpThreadName() + 
                                 " terminating because of an interrupt during processing.";
                    _log.info(msg);
                }
                break;
            }
            
            // Sleep for the prescribed amount of time before trying again.
            try {Thread.sleep(INTERRUPT_DELETE_DELAY);}
            catch (InterruptedException e) {
                // Terminate this thread.
                if (_log.isInfoEnabled()) {
                    String msg = getInterruptCleanUpThreadName() + 
                                 " terminating because of an interrupt during sleep.";
                    _log.info(msg);
                }
                break;
            }
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* monitorZombies:                                                        */
    /* ---------------------------------------------------------------------- */
    /** Call the zombie detection method in our subclass. */
    private void monitorZombies()
    {
        // Call down to our subclass where the real implementation is.
        ((MonitoringScheduler)this).monitorZombies();
    }
    
    /* ---------------------------------------------------------------------- */
    /* getJobsReadyForPhase:                                                  */
    /* ---------------------------------------------------------------------- */
    /** Query the database for jobs that execute in this phase.  Filter those
     * job using the published job set to avoid writing the same job more than
     * once to a queue for this phase.  This method also preens the published job
     * set by removing jobs that have progressed out of this phase. 
     * 
     * @return the list of jobs that have yet to be published in this phase
     * @throws JobSchedulerException
     */
    private ReadyJobs getJobsReadyForPhase()
      throws JobSchedulerException
    {
        // Is new work being accepted?
        if (org.iplantc.service.common.Settings.isDrainingQueuesEnabled()) {
            _log.debug("Queue draining is enabled. Skipping " + _phaseType + " tasks." );
            return new ReadyJobs(new LinkedList<Job>());
        }
        
        // -------------------- Clean Published Jobs -----------
        // Remove jobs from the published table that are no longer in one of this phase's 
        // trigger's statuses.  Note that jobs may change status between this call and 
        // the next call to get candidate jobs.
        // 
        // When a job exits a trigger status for this phase, but its published record still 
        // exists for this phase, the next query will not return the job and the record 
        // will be removed in the next clean cycle.  On the other hand, if a job moves into a 
        // trigger status of this phase, it will be picked up in the query below as long as 
        // it doesn't have a publish record.
        //
        // It is possible for a job to regress from a later status to an earlier one.
        // For example, from staged to pending.  If this rollback occurs before an execution 
        // of cleanPublishedJobs occurs, the job will become orphaned (i.e., it will have a 
        // published record but not actually be posted in any worker queue).  If this becomes 
        // a problem in practice, the remedy would be to remove publish records for jobs 
        // being rolled back. 
        try {JobPublishedDao.cleanPublishedJobs(_phaseType, getPhaseTriggerStatuses());}
            catch (Exception e)
            {
                String msg = _phaseType.name() + " scheduler unable clean job_published table.";
                _log.error(msg, e);
                throw new JobSchedulerException(msg, e);
            }
        
        // -------------------- Query Jobs ---------------------
        // Query all jobs that are ready for this state.
        ReadyJobs readyJobs = null;
        try {readyJobs = getPhaseCandidateJobs(getPhaseTriggerStatuses());}
            catch (Exception e)
            {
                String msg = _phaseType.name() + " scheduler unable to retrieve jobs.";
                _log.error(msg, e);
                throw new JobSchedulerException(msg, e);
            }
                
        return readyJobs;
    }

    /* ---------------------------------------------------------------------- */
    /* selectQueueName:                                                       */
    /* ---------------------------------------------------------------------- */
    /** Given a job, select the highest priority queue on which to place the 
     * job.  Queue selection is based on the value of the queue's filter when
     * values from the job's runtime context are used.
     * 
     * Each phase has a default queue for each tenant named <phase>.<tenantId>.
     * If no queue filter evaluates to true, the default queue is chosen.
     * 
     * @param job the job to be executed.
     * @return
     */
    private String selectQueueName(Job job)
    {
        // Populate substitution values.
        //
        // These values can only be class Boolean, Byte, Short, Integer, Long, 
        // Float, Double, and String; any other values will cause an exception.
        // Property names cannot be null or the empty string.
        Map<String, Object> properties = new HashMap<>();
        
        // Used by system to retrieve queue set and available for use in filters.
        properties.put("phase", _phaseType.name());
        properties.put("tenant_id", job.getTenantId());
        
        // Other properties from Job object available for use in filters.
        // See the Job class for the meaning of each value.
        if (job.getArchiveCanonicalUrl() != null)
            properties.put("archiveCanonicalUrl", job.getArchiveCanonicalUrl());
        if (job.getArchivePath() != null)
            properties.put("archivePath", job.getArchivePath());
        if (job.getArchiveSystem() != null) {
            if (job.getArchiveSystem().getName() != null)
                properties.put("archiveSystemName", job.getArchiveSystem().getName());
            if (job.getArchiveSystem().getSite() != null)
                properties.put("archiveSystemSite", job.getArchiveSystem().getSite());
            if (job.getArchiveSystem().getStatus() != null)
                properties.put("archiveSystemStatus", job.getArchiveSystem().getStatus().name());
            if (job.getArchiveSystem().getUserRole(job.getOwner()) != null)
                properties.put("archiveSystemUserRole", job.getArchiveSystem().getUserRole(job.getOwner()).toString());
        }
        if (job.getBatchQueue() != null)
            properties.put("batchQueue", job.getBatchQueue());
        if (job.getCharge() != null)
            properties.put("charge", job.getCharge());
        if (job.getCreated() != null) {
            DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            properties.put("created", formatter.format(job.getCreated()));
        }
        if (job.getInternalUsername() != null)
            properties.put("internalUsername", job.getInternalUsername());
        if (job.getMaxRunTime() != null)
            properties.put("maxRunTime", job.getMaxRunTime());
        if (job.getMemoryPerNode() != null)
            properties.put("memoryPerNode", job.getMemoryPerNode());
        if (job.getName() != null)
            properties.put("name", job.getName());
        if (job.getNodeCount() != null)
            properties.put("nodeCount", job.getNodeCount());
        if (job.getOutputPath() != null)
            properties.put("outputPath", job.getOutputPath());
        if (job.getOwner() != null)
            properties.put("owner", job.getOwner());
        if (job.getProcessorsPerNode() != null)
            properties.put("processorsPerNode", job.getProcessorsPerNode());
        if (job.getRetries() != null)
            properties.put("retries", job.getRetries());
        if (job.getSoftwareName() != null)
            properties.put("softwareName", job.getSoftwareName());
        if (job.getSystem() != null)
            properties.put("system", job.getSystem());
        if (job.getWorkPath() != null)
            properties.put("workPath", job.getWorkPath());
        
        // Evaluate each of this tenant's queues in priority order.
        String selectedQueueName = null;
        List<JobQueue> queues = _tenantQueues.get(job.getTenantId());
        for (JobQueue queue : queues) {
            if (runFilter(queue, properties)) {
                selectedQueueName = queue.getName();
                break;
            }
        }
          
        // Make sure we select some queue.
        if (selectedQueueName == null) {
            _log.warn("No " + _phaseType.name() + 
                      " queue filter evaluated to true for tenant " + 
                      job.getTenantId() + ".");
            
            // Select the default queue.
            selectedQueueName = getDefaultQueue(job.getTenantId());
        }
        
        return selectedQueueName;
    }
    
    /* ---------------------------------------------------------------------- */
    /* runFilter:                                                             */
    /* ---------------------------------------------------------------------- */
    /** Given a job queue and a map of key/value pair, substitute the values
     * in for their keys in the queue's filter and evaluate the filter.  True
     * is only returned if the filter's boolean expression evaluates to true.
     * Evaluation exceptions cause false to be returned.
     * 
     * @param jobQueue the queue whose filter is being evaluated
     * @param properties the substitution values used to evaluate the filter
     * @return true if the filter evaluates to true, false otherwise
     */
    private boolean runFilter(JobQueue jobQueue, Map<String, Object> properties)
    {
        // Evaluate the filter field using the properties field values.
        boolean matched = false;
        try {matched = SelectorFilter.match(jobQueue.getFilter(), properties);}
          catch (JobQueueFilterException e) {
            String msg = "Error processing filter for " + jobQueue.getName() + "."; 
            _log.error(msg, e);
        }
        return matched;
    }
    
    /* ---------------------------------------------------------------------- */
    /* releasePublishedJob:                                                   */
    /* ---------------------------------------------------------------------- */
    /** Make a best effort attempt to remove a job that was being serviced by 
     * a thread that died unexpectedly.  The thread may not have working on 
     * a job, in which case we don't do anything.  Otherwise, we fire and
     * forget a database delete command.
     * 
     * @param deadWorker a worker thread that died unexpectedly.
     */
    private void releasePublishedJob(AbstractPhaseWorker deadWorker)
    {
        // Should we even try?
        if (deadWorker == null || 
            deadWorker.getJob() == null || 
            deadWorker.getPhaseType() == null) 
           return;
        
        // Try to remove the job the dead worker was processing from the
        // published table so that the job may be rescheduled if necessary.
        try {JobPublishedDao.deletePublishedJob(deadWorker.getPhaseType(), 
                                                deadWorker.getJob().getUuid());}
            catch (Exception e) {
                String msg = "Worker thread uncaughtException handler unable to " +
                             "delete job publish record for phase " + 
                             deadWorker.getPhaseType().name() + " and job " +
                             deadWorker.getJob().getUuid() + ".";
                _log.error(msg, e);
            }
    }
    
    /* ---------------------------------------------------------------------- */
    /* releasePublishedJob:                                                   */
    /* ---------------------------------------------------------------------- */
    /** Make a best effort attempt to remove a job that was being serviced by 
     * a thread that died unexpectedly.  The thread may not have working on 
     * a job, in which case we don't do anything.  Otherwise, we fire and
     * forget a database delete command.
     * 
     * @param deadWorker a worker thread that died unexpectedly.
     */
    private void releaseJobClaim(AbstractPhaseWorker deadWorker)
    {
        // Should we even try?
        if (deadWorker == null || 
            deadWorker.getThreadUuid() == null) 
           return;
        
        // Try to remove the job the dead worker was processing from the
        // published table so that the job may be rescheduled if necessary.
        try {JobWorkerDao.unclaimJobByWorkerUuid(deadWorker.getThreadUuid());}
            catch (Exception e) {
                String msg = "Worker thread uncaughtException handler unable to " +
                            "unclaim job for worker " + deadWorker.getThreadUuid() + ".";
               _log.error(msg, e);
            }
    }
    
    /* ---------------------------------------------------------------------- */
    /* dumpMessageInfo:                                                       */
    /* ---------------------------------------------------------------------- */
    /** Write debug message and threading information.  This methods should
     * only be called after checking that debugging is enabled.
     * 
     * @param consumerTag the tag associated with the receiving consumer
     * @param envelope the message envelope
     * @param properties the message properties
     * @param body the message
     */
    private void dumpMessageInfo(String consumerTag, Envelope envelope, 
                                 AMQP.BasicProperties properties, byte[] body)
    {
        // We assume all input parameters are non-null.
        Thread curthd = Thread.currentThread();
        ThreadGroup curgrp = curthd.getThreadGroup();
        String msg = "\n------------------- Topic Bytes Received: " + body.length + "\n";
        msg += "Consumer tag: " + consumerTag + "\n";
        msg += "Thread(name=" +curthd.getName() + ", isDaemon=" + curthd.isDaemon() + ")\n";
        msg += "ThreadGroup(name=" + curgrp.getName() + ", parentGroup=" + curgrp.getParent().getName() +
                    ", activeGroupCount=" + curgrp.activeGroupCount() + ", activeThreadCount=" + 
                    curgrp.activeCount() + ", isDaemon=" + curgrp.isDaemon() + ")\n";
        
        // Output is truncated at array size.
        Thread[] thdArray = new Thread[200];
        int thdArrayLen = curgrp.enumerate(thdArray, false); // non-recursive 
        msg += "ThreadArray(length=" + thdArrayLen + ", names=";
        for (int i = 0; i < thdArrayLen; i++) msg += thdArray[i].getName() + ", ";
        msg += "\n";
        
        // Output is truncated at array size.
        ThreadGroup[] grpArray = new ThreadGroup[200];
        int grpArrayLen = curgrp.enumerate(grpArray, false); // non-recursive 
        msg += "ThreadGroupArray(length=" + grpArrayLen + ", names=";
        for (int i = 0; i < grpArrayLen; i++) msg += grpArray[i].getName() + ", ";
        msg += "\n";
        
        msg += envelope.toString() + "\n"; 
        StringBuilder buf = new StringBuilder(512);
        properties.appendPropertyDebugStringTo(buf);
        msg += "Properties" + buf.toString() + "\n";
        msg += "-------------------------------------------------\n";
        _log.debug(msg);
    }
    
    /* ********************************************************************** */
    /*                        Topic Command Processors                        */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* doJobInterrupt:                                                        */
    /* ---------------------------------------------------------------------- */
    /** Process pause job message.
     * 
     * @param node a parsed json node representation of the message
     * @return true to acknowledge the message, false to reject it
     */
    protected boolean doJobInterrupt(JobCommand jobCommand,
                                     Envelope envelope, 
                                     BasicProperties properties, 
                                     byte[] body)
    {
        // ---------------------- Marshalling ----------------------
        // Marshal the json message into it message object.
        AbstractQueueJobMessage qjob = null;
        try {
            if (jobCommand == JobCommand.TPC_PAUSE_JOB)
                qjob = PauseJobMessage.fromJson(body.toString());
            else if (jobCommand == JobCommand.TPC_STOP_JOB)
                qjob = StopJobMessage.fromJson(body.toString());
            else if (jobCommand == JobCommand.TPC_DELETE_JOB)
                qjob = DeleteJobMessage.fromJson(body.toString());
            else
            {
                // This should never happen.
                String msg = "Invalid job interrupt command received: " + jobCommand;
                _log.error(msg);
                return false;
            }
        }
        catch (IOException e) {
            // Log error message.
            String msg = _phaseType.name() + 
                         " topic reader cannot decode data from queue " + 
                         TOPIC_QUEUE_NAME + ": " + e.getMessage();
            _log.error(msg, e);
            return false;
        }
            
        // ---------------------- Get Job --------------------------
        // Retrieve the job from the database and validate.
        Job job = getInterruptedJob(qjob, body);
        if (job == null)
        {
            // Log warning message.
            String msg = _phaseType.name() + 
                         " topic reader skipping interrupt message: " + 
                         (new String(body));
            _log.warn(msg);
            return false;
        }
         
        // ---------------------- Create Interrupt -----------------
        // Insert an interrupt record into the interrupt table.  At some later point,
        // either a worker thread assigned the job will process the interrupt or the
        // interrupt clean up thread will remove the interrupt after it expires.
        JobInterrupt jobInterrupt = 
           new JobInterrupt(qjob.jobUuid, qjob.tenantId, qjob.command.toInterruptType(),
                            qjob.epoch);
        try {
            // We expect to insert one row in the interrupts table.
            int rows = JobInterruptDao.createInterrupt(jobInterrupt);
            if (rows != 1) 
                throw new JobException("JobInterruptDao.createInterrupt() failed to insert row.");
        }
        catch (JobException e) {
            // Log error message.
            String msg = _phaseType.name() + 
                         " topic reader cannot create a new job interrupt: " + 
                         e.getMessage();
            _log.error(msg, e);
            return false;
        }
        
        // Success.
        return true;
    }
    
    /* ---------------------------------------------------------------------- */
    /* doNoop:                                                                */
    /* ---------------------------------------------------------------------- */
    /** Process a command that only logs an informational message.  If test 
     * text is included in the message, it is also logged.
     * 
     * @param node a parsed json node representation of the message
     * @return true to acknowledge the message, false to reject it
     */
    protected boolean doNoop(JsonNode node)
    {
        // No-op can have a test message
        String testMessage = node.path("testMessage").asText();
        if (StringUtils.isBlank(testMessage)) {
            _log.info(_phaseType.name() + " topic reader received NOOP message.");
        }
        else {
            _log.info(_phaseType.name() +  
                    " topic reader received NOOP test message:\n > " + testMessage + "\n");
        }
        
        // Always release message from queue.
        return true;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getInterruptedJob:                                                     */
    /* ---------------------------------------------------------------------- */
    /** Retrieves job records from the database and validates them in the 
     * context of a topic message.
     * 
     * @param qjob the job interrupt message read from the scheduler topic
     * @param body the original topic message
     * @return the retrieved job or null if retrieval or validation fails
     */
    private Job getInterruptedJob(AbstractQueueJobMessage qjob, byte[] body)
    {
        // Make sure we got a message tenant id.
        if (StringUtils.isBlank(qjob.tenantId)) {
            String msg = _phaseType.name() + 
                         " topic reader received a message with no tenantId.";
            _log.error(msg);
            return null;
        }
        
        // At a minimum we need the unique job id.
        if (StringUtils.isBlank(qjob.jobUuid))
        {
            // Log the invalid input and quit.
            String msg = _phaseType.name() + 
                         " topic reader received a WKR_PROCESS_JOB message with an invalid uuid: " +
                         (new String(body));
            _log.error(msg);
            return null;
        }
        
        // We have a job reference to process.
        Job job = null;
        try {job = JobDao.getByUuid(qjob.jobUuid);}
        catch (JobException e) {
            String msg = _phaseType.name() + 
                         " topic reader unable to retrieve Job with UUID " + qjob.jobUuid +
                         " (" + qjob.jobName + ") from database.";
            _log.error(msg, e);
            return null;
        }
        
        // Make sure we got a job.
        if (job == null) {
            String msg = _phaseType.name() + 
                         " topic reader unable to find Job with UUID " + qjob.jobUuid +
                         " (" + qjob.jobName + ") from database.";
            _log.error(msg);
            return null;
        }
        
        // Make sure the job tenant matches this worker's assigned tenant.
        if (!qjob.tenantId.equals(job.getTenantId())) {
            String msg = _phaseType.name() + " topic message with tenantId " +
                    qjob.tenantId + " specified a job with UUID " + qjob.jobUuid +
                    " (" + qjob.jobName + ") with tenantId " + 
                    job.getTenantId() + ".";
            _log.error(msg);
            return null;
        }
        
        // Make sure the job is not in a final state.
        if (JobStatusType.isFinished(job.getStatus()))
        {
            String msg = _phaseType.name() + " topic message cannot interrupt job " + 
                          qjob.jobUuid + " (" + qjob.jobName + 
                          ") because the job is already in finished state " + 
                          job.getStatus().name() + "."; 
            _log.warn(msg);
            return null;
        }
        
        // Success
        return job;
    }
    
}