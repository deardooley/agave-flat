package org.iplantc.service.jobs.phases.schedulers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.iplantc.service.jobs.dao.JobQueueDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobSchedulerException;
import org.iplantc.service.jobs.model.JobQueue;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.iplantc.service.jobs.phases.PhaseWorker;
import org.iplantc.service.jobs.phases.PhaseWorkerParms;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

/** Main job scheduler implementation class.
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
    public static final String TOPIC_QUEUE_NAME = "JobSchedulerTopic";
    public static final String ALL_TOPIC_BINDING_KEY = "JobScheduler.All.#";
    
    // Suffixes used in naming.
    private static final String THREADGROUP_SUFFIX = "-ThreadGroup";
    private static final String THREAD_SUFFIX = "-Thread";
    
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
    
    // This phase's queuing artifacts.
    private ConnectionFactory    _factory;
    private Connection           _connection;
    private Channel              _topicChannel;
    
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
        if (!(t instanceof PhaseWorker)) return;
        
        // Get the next available recovery thread number.
        PhaseWorker oldWorker = (PhaseWorker) t;
        int newThreadNum = _threadSeqno.incrementAndGet();
        
        // Create the new thread object.
        PhaseWorker newWorker = createWorkerThread(oldWorker.getThreadGroup(), 
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
    /* getPhaseBindingKey:                                                    */
    /* ---------------------------------------------------------------------- */
    protected String getPhaseBindingKey()
    {
        return "JobScheduler." + _phaseType.name() + ".#";
    }
    
    /* ---------------------------------------------------------------------- */
    /* getPhaseConnectionName:                                                */
    /* ---------------------------------------------------------------------- */
    protected String getPhaseConnectionName()
    {
        return _phaseType.name() + "-Connection";
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
        }
        
        return _factory;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getConnection:                                                         */
    /* ---------------------------------------------------------------------- */
    /** Return a connection to the queuing subsystem, creating the connection
     * if necessary.
     * 
     * @return this scheduler's connection
     * @throws JobSchedulerException on error.
     */
    protected Connection getConnection()
     throws JobSchedulerException
    {
        // Create the connection if necessary.
        if (_connection == null)
        {
            try {_connection = getConnectionFactory().newConnection(getPhaseConnectionName());}
            catch (IOException e) {
                String msg = "Unable to create new connection to queuing subsystem: " +
                             e.getMessage();
                _log.error(msg, e);
                throw new JobSchedulerException(msg, e);
            } catch (TimeoutException e) {
                String msg = "Timeout while creating new connection to queuing subsystem: " + 
                             e.getMessage();
                _log.error(msg, e);
                throw new JobSchedulerException(msg, e);
            }
        }
        
        return _connection;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getNewChannel:                                                         */
    /* ---------------------------------------------------------------------- */
    /** Return a new channel on the existing queuing system connection.
     * 
     * @return the new channel
     * @throws JobSchedulerException on error
     */
    protected Channel getNewChannel()
      throws JobSchedulerException
    {
        // Create a new channel in this phase's connection.
        Channel channel = null;
        try {channel = getConnection().createChannel();} 
         catch (IOException e) {
             String msg = "Unable to create channel on " + getPhaseConnectionName() + 
                          ": " + e.getMessage();
             _log.error(msg, e);
             throw new JobSchedulerException(msg, e);
         }
         if (_log.isInfoEnabled()) 
             _log.info("Created channel number " + channel.getChannelNumber() + 
                       " on " + getPhaseConnectionName() + ".");
         
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
            _topicChannel = getNewChannel();
            
            // Set prefetch.
            int prefetchCount = 1;
            try {_topicChannel.basicQos(prefetchCount);}
                catch (IOException e) {
                    String msg = "Unable to set prefech on channel on " + 
                                 getPhaseConnectionName() + 
                                 ": " + e.getMessage();
                    _log.error(msg, e);
                    throw new JobSchedulerException(msg, e);
                }
        }
        
        return _topicChannel;
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
                String msg = "Unable to create exchange on " + getPhaseConnectionName() + 
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
                             " on " + getPhaseConnectionName() + "/" + 
                             topicChannel.getChannelNumber() + ": " + e.getMessage();
                _log.error(msg, e);
                throw new JobSchedulerException(msg, e);
            }
        
        // Bind the topic queue to the topic exchange.
        try {topicChannel.queueBind(TOPIC_QUEUE_NAME, TOPIC_EXCHANGE_NAME, ALL_TOPIC_BINDING_KEY);}
            catch (IOException e) {
                String msg = "Unable to bind topic queue " + TOPIC_QUEUE_NAME +
                         " with binding key " + ALL_TOPIC_BINDING_KEY +
                         " on " + getPhaseConnectionName() + "/" + 
                         topicChannel.getChannelNumber() + ": " + e.getMessage();
                _log.error(msg, e);
                throw new JobSchedulerException(msg, e);
            }
        
        // Bind the topic queue to the topic exchange.
        try {topicChannel.queueBind(TOPIC_QUEUE_NAME, TOPIC_EXCHANGE_NAME, getPhaseBindingKey());}
            catch (IOException e) {
                String msg = "Unable to bind topic queue " + TOPIC_QUEUE_NAME +
                        " with binding key " + getPhaseBindingKey() +
                         " on " + getPhaseConnectionName() + "/" + 
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
            // Process messages read from topic.
            String message = null;
            try {message = new String(body);}
            catch (Exception e)
            {
                String msg = _phaseType.name() + 
                             " scheduler cannot decode data from " + 
                             TOPIC_QUEUE_NAME + " topic: " + e.getMessage();
                _log.error(msg, e);
            }
            
            // For now, just print what we receive.
            // TODO: create command processor 
            if (message != null)
               System.out.println(_phaseType.name() + 
                                  " scheduler received message:\n" +
                                  message);
            
            // Don't forget to send the ack!
            boolean multipleAck = false;
            _topicChannel.basicAck(envelope.getDeliveryTag(), multipleAck);
          }
        };
        
        // Tracing.
        if (_log.isDebugEnabled())
            _log.debug("[*] " + _phaseType.name() + " scheduler waiting on " + 
                        TOPIC_QUEUE_NAME + " topic.");

        // We auto-acknowledge topic broadcasts.
        boolean autoack = false;
        try {_topicChannel.basicConsume(TOPIC_QUEUE_NAME, autoack, consumer);}
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
                try {subscribeToJobTopic();}
                catch (JobSchedulerException e) {
                    String msg = getTopicThreadName() + " aborting! "  +
                         _phaseType.name() + 
                         " scheduler cannot receive any administrative requests.";
                    _log.error(msg);
                    throw new RuntimeException(msg);
                }
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
                    PhaseWorker worker = 
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
    private PhaseWorker createWorkerThread(ThreadGroup threadGroup, String tenantId,
                                           String queueName, int threadNum)
    {
        // Initialize parameter passing object.
        PhaseWorkerParms parms = new PhaseWorkerParms();
        parms.threadGroup = threadGroup;
        parms.threadName = getWorkerThreadName(tenantId, queueName, threadNum);
        parms.connection = _connection;
        parms.scheduler = this;
        parms.tenantId = tenantId;
        parms.queueName = queueName;
        parms.threadNum = threadNum;
        
        // Create worker and set attributes.
        PhaseWorker worker = new PhaseWorker(parms);
        worker.setDaemon(true);
        worker.setUncaughtExceptionHandler(this);
        return worker;
    }
    
    /* ---------------------------------------------------------------------- */
    /* schedule:                                                              */
    /* ---------------------------------------------------------------------- */
    private void schedule()
    {
        // Enter infinite scheduling loop.
        for (;;) {
            // Query the database for all candidate jobs for this phase.
            
            // Select a tenant to process.
            
            // Select a user to process.
            
            // Select a job to process.
            
            // Process the job.
            
            // Wait for more jobs to accumulate.
        }
            
    }
}
