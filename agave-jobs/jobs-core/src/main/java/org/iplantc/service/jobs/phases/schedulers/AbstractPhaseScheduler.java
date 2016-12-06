package org.iplantc.service.jobs.phases.schedulers;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.dao.JobInterruptDao;
import org.iplantc.service.jobs.dao.JobLeaseDao;
import org.iplantc.service.jobs.dao.JobQueueDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobQueueFilterException;
import org.iplantc.service.jobs.exceptions.JobSchedulerException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobInterrupt;
import org.iplantc.service.jobs.model.JobQueue;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.phases.QueueConstants;
import org.iplantc.service.jobs.phases.queuemessages.AbstractQueueJobMessage;
import org.iplantc.service.jobs.phases.queuemessages.AbstractQueueMessage.JobCommand;
import org.iplantc.service.jobs.phases.queuemessages.DeleteJobMessage;
import org.iplantc.service.jobs.phases.queuemessages.PauseJobMessage;
import org.iplantc.service.jobs.phases.queuemessages.ProcessJobMessage;
import org.iplantc.service.jobs.phases.queuemessages.StopJobMessage;
import org.iplantc.service.jobs.phases.workers.AbstractPhaseWorker;
import org.iplantc.service.jobs.phases.workers.ArchivingWorker;
import org.iplantc.service.jobs.phases.workers.MonitoringWorker;
import org.iplantc.service.jobs.phases.workers.PhaseWorkerParms;
import org.iplantc.service.jobs.phases.workers.StagingWorker;
import org.iplantc.service.jobs.phases.workers.SubmittingWorker;
import org.iplantc.service.jobs.queue.SelectorFilter;

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
 * are blocked on certain IO calls or wait(), a signal can be sent that wakes up 
 * the thread.  When a thread is active, however, a cooperative approach is needed
 * in which the thread can determine if a signal has been sent.  Below is an outline 
 * of the scheduler's cooperative interrupt mechanism.
 * 
 * The main components of the interrupt mechanism are:
 * 
 *  1. Each scheduler's topic, scheduler and worker threads
 *  2. Jobs table
 *  3. Scheduler topic
 *  4. Interrupt topic
 *  5. InterruptManager singleton
 *  6. Scheduler's InterruptedList 
 *  
 * The interrupt mechanism introduces the interrupt topic and the InterruptManager
 * singleton class.  The interrupt topic is a RabbitMQ topic to which the interrupt
 * thread in the InterruptManager subscribes.  The interrupt thread binds to 
 * the topic in multiple ways to accommodate other topic readers that we might
 * conceivably want in the future but are currently unplanned.  We use the following
 * binding keys:
 * 
 *      1. QueueConstants.INTERRUPT_WORKER_KEY
 *          - For messages targeting all workers
 *      2. <queueName>
 *          - For messages targeting all workers servicing a specific queue
 *      3. <tenanId>
 *          - For messages targeting all workers servicing a specific tenant 
 * 
 * Additionally, the AbstractPhaseScheduler class maintains a list of interrupted jobs.
 * All the jobs in this InterruptedList are always scheduled the next time the 
 * scheduler wakes up before the normal scheduling regime is executed.  This allows
 * interrupts to be serviced in a timely fashion. 
 * 
 * Asynchronous requests come into the jobs subsystem via the scheduler topic.  These
 * requests are usually triggered by an action taken by an end user (e.g., stop a job)
 * or an administrator (e.g., shutdown the jobs subsystem).  The topic thread subscribes
 * to the scheduler topic and is responsible for parsing, validating and acting upon
 * the request message.  Requests are well-defined subclasses of the AbstractQueueMessage
 * class and are represented as a JSON string when queued.
 * 
 * Once a message is validated, the topic thread determines how to process it.
 * Administrative requests that are not job-specific are handled by the topic thread 
 * itself, sometimes in cooperation with worker threads.  Job-specific requests are
 * handled by worker threads through the InterruptManager.
 * 
 * Once the topic thread determines that a worker thread needs to handle a message, the
 * topic thread publishes the message on the interrupt topic using one of the binding
 * keys listed above.  Depending on the circumstance, the topic thread may also issue
 * a thread interrupt directly to all targeted threads.  For example, after publishing
 * to the interrupt topic with the <queueName> binding key, the topic thread may choose
 * to interrupt all threads servicing that queue to wake up those that are blocked.
 * 
 * The interrupt thread runs under the auspices of the InterruptManager and subscribes
 * to all messages published on the interrupt topic.  When the interrupt thread receives
 * a new message, it inserts that message into a concurrent hashmap maintained by the
 * InterruptManager.  The InterruptManager maintains a mapping of tenantId's to job
 * maps, where each job map maps job uuid to a message list.  
 * 
 * At frequent and convenient intervals, worker threads check the InterruptManager's
 * job map to discover any asynchronous messages targeting their jobs.  Any messages
 * found are process in the order they were received.
 * 
 * The correctness of the asynchronous interrupt mechanism depends on eliminating all
 * race conditions.  When worker thread is assigned a specific job (i.e., when the
 * AbstractPhaseWorker._job field is non-null), then any interrupt messages received
 * by that thread for that job can be acted upon by that thread.  Unfortunately,
 * when a job is not assigned to any worker thread, as is the case when a job  is queued, 
 * then job-specific interrupt messages cannot be immediately processed by any worker 
 * thread.  The mappings maintained by InterruptManager prevents race conditions from
 * causing messages to be lost.
 * 
 * The protocol used to avoid losing any interrupt messages is as follows:
 * 
 *      1. Topic thread conditionally updates the job status in jobs table to reflect 
 *         the message request.
 *      2. Topic thread publishes job-specific message to interrupt topic.
 *      3. InterruptManager reads message and puts it in appropriate job map.
 *      4. Topic thread adds the job to the scheduler's InterruptedList. 
 *      5. Worker currently or eventually assigned the job checks job map for messages.
 *      6. Worker processes the interrupt message.
 * 
 * Note that Step 1 is conditioned on the validity of the transition to the state request
 * in the message.  This step is important because after step 3, the database contains the
 * only persistent indication of the interrupt request. 
 * 
 *     TODO:  step 6 when state reverts from step 1 transition
 *            step 1 does other clean up work?  Maybe need intermediate states: STOP_PENDING, etc.
 *  
 * @author rcardone
 */

public abstract class AbstractPhaseScheduler 
  implements Runnable, Thread.UncaughtExceptionHandler
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
    
    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    // The only job phase that this instance services.
    protected final JobPhaseType _phaseType;
    
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
    
    // The running set of job uuids that represent in one of this phase's
    // trigger states that have already been published to a RabbitMQ queue
    // by this phase's scheduler.  This set prevents a job from being published
    // multiple time in the same stage.
    private final HashSet<String> _publishedJobs = new HashSet<>();
    
    // Monotonically increasing sequence number generator used as part of thread names.
    private static final AtomicInteger _threadSeqno = new AtomicInteger(0);
    
    // A thread that periodically removes expired interrupts from database.
    private Thread _interruptCleanUpThread;
        
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
    protected AbstractPhaseScheduler(JobPhaseType phaseType)
     throws JobException
    {
        // Check input.
        if (phaseType == null) {
            String msg = "A non-null phase type is required PhaseScheduler initialization.";
            _log.error(msg);
            throw new JobException(msg);
        }
        
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
     * a job.  The returned status is how the scheduler identifies new work. 
     * 
     * @return the trigger status for new work in this phase.
     */
    protected abstract List<JobStatusType> getPhaseTriggerStatuses();
    
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
        
        // Get the next available recovery thread number.
        AbstractPhaseWorker oldWorker = (AbstractPhaseWorker) t;
        int newThreadNum = _threadSeqno.incrementAndGet();
        
        // Create the new thread object.
        AbstractPhaseWorker newWorker = createWorkerThread(oldWorker.getThreadGroup(), 
                                                   oldWorker.getTenantId(), 
                                                   oldWorker.getQueueName(), 
                                                   newThreadNum);
          
        // Log more information.
        String msg = "Phase worker thread " + t.getName() + " died unexpectedly. " +
                     "Starting new worker " + newWorker.getName();
        _log.error(msg);
        
        // Let it rip.
        newWorker.start();
    }
    
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
        qjob.name = job.getName();
        qjob.uuid = job.getUuid();
        
        // Write the object as a JSON string.
        ObjectMapper m = new ObjectMapper();
        StringWriter writer = new StringWriter(150);
        m.writeValue(writer, qjob);
        return writer.toString();
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
    
    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
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
                          case TCP_DELETE_JOB:
                          case TCP_PAUSE_JOB:
                          case TCP_STOP_JOB:
                              ack = doJobInterrupt(command, envelope, properties, body);
                              break;
                              
                          // ################# Scheduler Interrupts ##################
                          // ----- Shutdown scheduler
                          case TPC_SHUTDOWN:
                              break;
                          // ----- Start specified number of worker threads
                          case TCP_START_WORKERS:
                              break;
                          // ----- Terminate specified number of worker threads  
                          case TCP_TERMINATE_WORKERS:
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
        Thread topicThread = 
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
            }
        };
        
        // Configure and start the thread.
        topicThread.setDaemon(true);
        topicThread.start();
    }
    
    /* ---------------------------------------------------------------------- */
    /* startQueueWorkers:                                                     */
    /* ---------------------------------------------------------------------- */
    /** Start the configured number of workers dedicated to each queue.  Each
     * worker thread services exactly one queue, though a queue can have multiple
     * workers.  
     * 
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
    /** Start a thread to read from this phase's topic.
     * 
     */
    private void startInterruptCleanUpThread()
    {
        // Create the topic thread.
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
                
                // Acquire or reacquire the lease that grants this scheduler
                // instance the permission to query the jobs table.  If we don't
                // get the lease, we keep retrying until we do or are interrupted.
                if (!jobLeaseDao.acquireLease()) waitToAcquireLease(jobLeaseDao);
                
                // Only the staging scheduler can start an interrupt clean up thread.
                // There's nothing special about the staging scheduler; we only need
                // one clean up thread in the whole system.
                if ((this instanceof StagingScheduler) && _interruptCleanUpThread == null)
                    startInterruptCleanUpThread();
                
                // Query the database for all candidate jobs for this phase.  This
                // method also maintains the published jobs set and filters the list
                // of candidate jobs using that set.
                List<Job> jobs = null;
                try {jobs = getJobsReadyForPhase(_publishedJobs);}
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
                               jobs.size() + " jobs.");
            
                // See if this thread was interrupted before doing a lot of processing.
                if (Thread.currentThread().isInterrupted()) {
                    String msg = "Scheduler thread " + Thread.currentThread().getName() +
                                 " for phase " + _phaseType.name() + 
                                 " interrupted while processing new work.";
                    throw new InterruptedException(msg);
                }
                
                // Select a tenant to process.
            
                // Select a user to process.
            
                // Select a job to process.
            
                // Process the selected job.
                // TODO: Replace scaffolding code with real job selection code.
                if (!jobs.isEmpty()) {
                    Job job = jobs.get(0); // temp code
                
                    // Select a target queue name for this job.
                    // The name is used as the routing key to a 
                    // direct exchange.
                    String routingKey = selectQueueName(job);
                    try {
                        // Write the job to the selected worker queue.
                        _schedulerChannel.basicPublish(QueueConstants.WORKER_EXCHANGE_NAME, 
                            routingKey, QueueConstants.PERSISTENT_JSON, 
                            toQueueableJSON(job).getBytes("UTF-8"));
                       
                        // Let's not publish this job to a queue more than once in this phase.
                        _publishedJobs.add(job.getUuid());
                    
                        // Tracing.
                        if (_log.isDebugEnabled()) {
                            String msg = _phaseType.name() + " scheduler published " +
                                job.getName() + " (" + job.getUuid() + ") to queue " + 
                                routingKey + ".";
                            _log.debug(msg);
                        }
                    }
                    catch (IOException e) {
                        // TODO: Probably need better failure remedy when publish fails.
                        String msg = _phaseType.name() + " scheduler failed to publish " +
                            job.getName() + " (" + job.getUuid() + ") to queue " + 
                            routingKey + ".  Retrying later.";
                        _log.info(msg, e);
                    }
                }
            
                // Wait for more jobs to accumulate
                // while maintaining our job lease.
                waitForWork(jobLeaseDao, POLLING_NORMAL_DELAY);
                
            } // End polling loop.
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally {
            // Always try to release the lease since we might be holding it.
            jobLeaseDao.releaseLease();
            
            // TODO: Should the scheduler thread take down all other threads
            //       and close all channels?
            
            // Announce our termination.
            if (_log.isInfoEnabled()) {
                String msg = "Scheduler thread " + Thread.currentThread().getName() +
                    " for phase " + _phaseType.name() + 
                    " is terminating.";
                _log.info(msg);
            }
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
    /* getJobsReadyForPhase:                                                  */
    /* ---------------------------------------------------------------------- */
    /** Query the database for jobs that execute in this phase.  Filter those
     * job using the published job set to avoid writing the same job more than
     * once to a queue for this phase.  This method also preens the published job
     * set by removing jobs that have progress out of this phase. 
     * 
     * @param publishedJobs the set of jobs still in this phase that have already
     *          been published to a queue.
     * @return the list of jobs that have yet to be published in this phase
     * @throws JobSchedulerException
     */
    private List<Job> getJobsReadyForPhase(HashSet<String> publishedJobs)
      throws JobSchedulerException
    {
        // Initialize result list.
        List<Job> jobs = null;
        
        // Is new work being accepted?
        if (org.iplantc.service.common.Settings.isDrainingQueuesEnabled()) {
            _log.debug("Queue draining is enabled. Skipping " + _phaseType + " tasks." );
            return jobs;
        }
        
        // Query all jobs that are ready for this state.
        try {jobs = JobDao.getByStatus(getPhaseTriggerStatuses());}
            catch (Exception e)
            {
                String msg = _phaseType.name() + " scheduler unable to retrieve jobs.";
                _log.error(msg, e);
                throw new JobSchedulerException(msg, e);
            }
        
        // Maintain the published job set by removing obsolete jobs from the set.
        // Obsolete job are those that are no longer in any of this phase's trigger
        // states.  We do not need to guard against republishing jobs that progressed
        // beyond this phase's purview, so we remove them from the publish job set.
        //
        // Put the job uuids in an easily searched set.
        HashSet<String> jobUuids = new HashSet<>((jobs.size() * 2) + 1);
        for (Job job : jobs)  jobUuids.add(job.getUuid());
            
        // Remove uuids in the published set that are not in the current job set.
        Iterator<String> publishedIt = publishedJobs.iterator();
        while (publishedIt.hasNext()) {
            String publishedUuid = publishedIt.next();
            if (!jobUuids.contains(publishedUuid)) publishedIt.remove();
        }
        
        // Remove jobs that have already been published to a RabbitMQ queue
        // and are still in one of this phase's trigger states.
        ListIterator<Job> jobIt = jobs.listIterator();
        while (jobIt.hasNext()) {
            // Remove the job if it has been published.
            Job job = jobIt.next();
            if (publishedJobs.contains(job.getUuid()))
                jobIt.remove();
        }
        
        return jobs;
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
        // TODO: Expand the sources in properties.
        // Populate substitution values.
        //
        // These values can only be class Boolean, Byte, Short, Integer, Long, 
        // Float, Double, and String; any other values will cause an exception.
        // Property names cannot be null or the empty string.
        Map<String, Object> properties = new HashMap<>();
        properties.put("phase", _phaseType.name());
        properties.put("tenant_id", job.getTenantId());
        
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
            if (jobCommand == JobCommand.TCP_PAUSE_JOB)
                qjob = PauseJobMessage.fromJson(body.toString());
            else if (jobCommand == JobCommand.TCP_STOP_JOB)
                qjob = StopJobMessage.fromJson(body.toString());
            else if (jobCommand == JobCommand.TCP_DELETE_JOB)
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
           new JobInterrupt(qjob.jobUuid, qjob.tenantId, qjob.command.toInterruptType());
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
