package org.iplantc.service.jobs.phases.schedulers;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.dao.JobQueueDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobQueueFilterException;
import org.iplantc.service.jobs.exceptions.JobSchedulerException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobQueue;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.phases.PhaseWorkerParms;
import org.iplantc.service.jobs.phases.workers.AbstractPhaseWorker;
import org.iplantc.service.jobs.phases.workers.ArchivingWorker;
import org.iplantc.service.jobs.phases.workers.MonitoringWorker;
import org.iplantc.service.jobs.phases.workers.StagingWorker;
import org.iplantc.service.jobs.phases.workers.SubmittingWorker;
import org.iplantc.service.jobs.queue.SelectorFilter;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
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
 * TODO: The design described here still needs to be implemented.
 * 
 * Jobs need to be interrupted during any processing phase.  When worker threads
 * are blocked on certain IO calls or wait(), a signal can be sent that wakes up 
 * the thread.  When a thread is active, however, a cooperative approach is needed
 * in which the thread checks some condition variable to determine if a signal has 
 * been sent.  Below is an outline of the scheduler's cooperative interrupt 
 * mechanism.
 * 
 * The main components of the interrupt mechanism are:
 * 
 *  1. Each scheduler's topic and scheduler threads
 *  2. Worker threads
 *  3. The jobs table
 *  4. Shared condition variables
 *  
 * Interrupts are posted to scheduler topic using the phase-specific or "all" routing
 * key.  The topic thread(s) receive the interrupt and set the appropriate condition
 * variable.  These variables are usually implemented in this base class so they can 
 * be accessed from all concrete schedulers.  
 * 
 * Depending on the interrupt the topic thread may also update a job's status in the 
 * database.  This database update is performed according to a state machine and forces
 * all subsequent attempts to update the job's status to conform to the same state 
 * machine.  
 * 
 * Worker threads are expected to frequently check the condition variables pertinent
 * to their phase and take action if necessary.  Checking a condition variable 
 * should be a fast operation that does not include a database or network call.
 * For example, workers could check a ConcurrentHashMap in this base scheduler class 
 * to detect if their job has been stopped.  If so, the worker would discontinue 
 * processing the job and perform any job-related clean up, including removing the 
 * job entry from the ConcurrentHashMap.
 * 
 * When state changes are delivered first through the database and then via the
 * topic thread, there's a chance that the job completed on its own so that no
 * worker is responsible for it any longer.  When something like a ConcurrentHashMap
 * is used, the result can be orphaned entries.  The address leaks such as that,
 * the scheduler thread can periodically clean up any orphaned entries.  For 
 * example, once a day when the scheduler thread wakes up it can run an orphan
 * clean up routine.   
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
    
    // Queuing information.
    public static final String TOPIC_EXCHANGE_NAME = "JobTopicExchange";
    public static final String TOPIC_QUEUE_PREFIX = "JobSchedulerTopic";
    public static final String ALL_TOPIC_BINDING_KEY = "JobScheduler.All.#";
    
    // Suffixes used in naming.
    private static final String THREADGROUP_SUFFIX = "-ThreadGroup";
    private static final String THREAD_SUFFIX = "-Thread";
    
    // Number of milliseconds to wait in various polling situations.
    private static final int POLLING_NORMAL_DELAY  = 10000;
    private static final int POLLING_FAILURE_DELAY = 15000;
    
    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    // The only job phase that this instance services.
    protected final JobPhaseType _phaseType;
    
    // The parent thread group of all thread groups created by this scheduler.
    // By default, this thread group is not a daemon, so it will not be destroyed
    // if it becomes empty.
    private final ThreadGroup    _phaseThreadGroup;
    
    // This phase's tenant/queue mapping. The keys are tenant ids, the values
    // are the lists of job queues defined for that tenant.  The queues are 
    // listed in priority order.
    private final HashMap<String,List<JobQueue>> _tenantQueues = new HashMap<>();
    
    // Monotonically increasing sequence number generator used as part of thread names.
    private static final AtomicInteger _threadSeqno = new AtomicInteger(0);
    
    // The single mapping of stopped job uuids to stop messages with initial 
    // capacity specified.  The uuids are jobs that need to be stopped;
    // the message is intending to be any string appropriate for logging.
    // TODO: This field is part of the not-yet-implemented interrupt mechanism.
    private static final ConcurrentHashMap<String,String> _stoppedJobs = new ConcurrentHashMap<>(23);
    
    // This phase's queuing artifacts.
    private ConnectionFactory    _factory;
    private Connection           _inConnection;
    private Connection           _outConnection;
    private Channel              _topicChannel;
    private Channel              _schedulerChannel;
    
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
    protected abstract JobStatusType getPhaseTriggerStatus();
    
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
            // Connect to the queuing subsystem.
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
    /* getPhaseThreadGroup:                                                   */
    /* ---------------------------------------------------------------------- */
    protected ThreadGroup getPhaseThreadGroup(){return _phaseThreadGroup;}
    
    /* ---------------------------------------------------------------------- */
    /* getTopicQueueName:                                                    */
    /* ---------------------------------------------------------------------- */
    protected String getTopicQueueName()
    {
        return TOPIC_QUEUE_PREFIX + "-" + _phaseType.name();
    }
    
    /* ---------------------------------------------------------------------- */
    /* getPhaseBindingKey:                                                    */
    /* ---------------------------------------------------------------------- */
    protected String getPhaseBindingKey()
    {
        return "JobScheduler." + _phaseType.name() + ".#";
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
    /* getDefaultQueue:                                                       */
    /* ---------------------------------------------------------------------- */
    protected String getDefaultQueue(String tenantId)
    {
        return _phaseType.name() + "." + tenantId;
    }
    
    /* ---------------------------------------------------------------------- */
    /* toQueuableJSON:                                                        */
    /* ---------------------------------------------------------------------- */
    /** Create the json representation of a job on a worker queue.
     * 
     * @param job the job to be queued
     * @return json reference to the job
     * @throws IOException 
     * @throws JsonMappingException 
     * @throws JsonGenerationException 
     */
    protected String toQueuableJSON(Job job) 
      throws IOException
    {
        // Initialize the queueable object.
        QueueableJob qjob = new QueueableJob();
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
     * scheduler topic.  Bind the topic using the All binding key and the 
     * phase-specific key.
     * 
     * @throws JobSchedulerException on error
     */
    private void initJobTopic() throws JobSchedulerException
    {
        // Get this topic's channel.
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
        try {topicChannel.queueDeclare(getTopicQueueName(), durable, exclusive, autoDelete, null);}
            catch (IOException e) {
                String msg = "Unable to declare topic queue " + getTopicQueueName() +
                             " on " + getPhaseInConnectionName() + "/" + 
                             topicChannel.getChannelNumber() + ": " + e.getMessage();
                _log.error(msg, e);
                throw new JobSchedulerException(msg, e);
            }
        
        // Bind the topic queue to the topic exchange.
        try {topicChannel.queueBind(getTopicQueueName(), TOPIC_EXCHANGE_NAME, ALL_TOPIC_BINDING_KEY);}
            catch (IOException e) {
                String msg = "Unable to bind topic queue " + getTopicQueueName() +
                         " with binding key " + ALL_TOPIC_BINDING_KEY +
                         " on " + getPhaseInConnectionName() + "/" + 
                         topicChannel.getChannelNumber() + ": " + e.getMessage();
                _log.error(msg, e);
                throw new JobSchedulerException(msg, e);
            }
        
        // Bind the topic queue to the topic exchange.
        try {topicChannel.queueBind(getTopicQueueName(), TOPIC_EXCHANGE_NAME, getPhaseBindingKey());}
            catch (IOException e) {
                String msg = "Unable to bind topic queue " + getTopicQueueName() +
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
              
            // Process messages read from topic.
            String message = null;
            try {message = new String(body);}
            catch (Exception e)
            {
                String msg = _phaseType.name() + 
                             " scheduler cannot decode data from " + 
                             getTopicQueueName() + " topic: " + e.getMessage();
                _log.error(msg, e);
            }
            
            // For now, just print what we receive.
            // TODO: create command processor 
            if (message != null)
               System.out.println(_phaseType.name() + 
                                  " scheduler received topic message:\n > " +
                                  message + "\n");
            
            // Don't forget to send the ack!
            boolean multipleAck = false;
            _topicChannel.basicAck(envelope.getDeliveryTag(), multipleAck);
          }
        };
        
        // Tracing.
        if (_log.isDebugEnabled())
            _log.debug("[*] " + _phaseType.name() + " scheduler consuming " + 
                    getTopicQueueName() + " topic.");

        // We auto-acknowledge topic broadcasts.
        boolean autoack = false;
        try {_topicChannel.basicConsume(getTopicQueueName(), autoack, consumer);}
        catch (IOException e) {
            String msg = _phaseType.name() + " scheduler is unable consume messages from " + 
                    getTopicQueueName() + " topic.";
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
        try {_schedulerChannel.exchangeDeclare(AbstractPhaseWorker.WORKER_EXCHANGE_NAME, 
                                               "direct", durable);}
        catch (IOException e) {
            String msg = "Unable to create exchange on " + getPhaseOutConnectionName() + 
                    "/" + _schedulerChannel.getChannelNumber() + ": " + e.getMessage();
            _log.error(msg, e);
            throw new JobSchedulerException(msg, e);
        }
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
        
        // Enter infinite scheduling loop.
        for (;;) {
            // Query the database for all candidate jobs for this phase.
            List<Job> jobs = null;
            try {jobs = getJobsReadyForPhase();}
                catch (Exception e) 
                {
                    String msg = _phaseType.name() + " scheduler database polling " +
                                 "failure. Retrying after a short delay.";
                    _log.info(msg);
                    try {Thread.sleep(POLLING_FAILURE_DELAY);}
                        catch (InterruptedException e1){}
                    continue;
                }
            
            if (_log.isDebugEnabled())
               _log.debug(_phaseType.name() + " scheduler retrieved " +
                          jobs.size() + " jobs in " + 
                          getPhaseTriggerStatus().name() + " status.");
            
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
                    _schedulerChannel.basicPublish(AbstractPhaseWorker.WORKER_EXCHANGE_NAME, 
                            routingKey, null, toQueuableJSON(job).getBytes("UTF-8"));
                    
                    // Tracing.
                    if (_log.isDebugEnabled()) {
                        String msg = _phaseType.name() + " scheduler failed to publish " +
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
            
            // Wait for more jobs to accumulate.
            // TODO: handle interrupts
            try {Thread.sleep(POLLING_NORMAL_DELAY);}
                catch (InterruptedException e1){}
        } // End polling loop.
    }

    /* ---------------------------------------------------------------------- */
    /* getJobsReadyForPhase:                                                  */
    /* ---------------------------------------------------------------------- */
    private List<Job> getJobsReadyForPhase()
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
        try {jobs = JobDao.getByStatus(getPhaseTriggerStatus());}
            catch (Exception e)
            {
                String msg = _phaseType.name() + " scheduler unable to retrieve " +
                             "jobs with status " + getPhaseTriggerStatus() + ".";
                _log.error(msg, e);
                throw new JobSchedulerException(msg, e);
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
        // Populate substitution values.
        Map<String, Object> properties = new HashMap<>();
        properties.put("phase", _phaseType);
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
    /*                           QueueableJob Class                           */
    /* ********************************************************************** */
    /** Job information written to queues by the scheduler thread and read
     * from queues by worker threads.
     */
    public static final class QueueableJob
    {
        // The test message field can be used for connectivity testing;
        // when it is not null, it takes precedence over the other fields.
        public String name;
        public String uuid;
        public String testMessage;
    }
}
