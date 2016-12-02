package org.iplantc.service.jobs.phases.workers;

import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobFinishedException;
import org.iplantc.service.jobs.exceptions.JobWorkerException;
import org.iplantc.service.jobs.exceptions.QuotaViolationException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.managers.JobQuotaCheck;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.phases.JobInterruptUtils;
import org.iplantc.service.jobs.phases.QueueConstants;
import org.iplantc.service.jobs.phases.queuemessages.AbstractQueueMessage.JobCommand;
import org.iplantc.service.jobs.phases.queuemessages.ProcessJobMessage;
import org.iplantc.service.jobs.phases.schedulers.AbstractPhaseScheduler;
import org.iplantc.service.jobs.queue.actions.WorkerAction;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.model.enumerations.StorageProtocolType;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

/**
 * @author rcardone
 *
 */
public abstract class AbstractPhaseWorker 
 extends Thread
 implements IPhaseWorker
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(AbstractPhaseWorker.class);
    
    // Communication constants.
    private static final String WORKER_EXCHANGE_NAME = QueueConstants.WORKER_EXCHANGE_NAME ;

    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    // Input fields.
    private final Connection             _connection;
    private final AbstractPhaseScheduler _scheduler;
    private final String                 _tenantId;
    private final String                 _queueName;
    private final int                    _threadNum;
    
    // Calculated fields.
    private Channel                      _jobChannel;
    private String                       _jobChannelConsumerTag;
    private Channel                      _interruptChannel;
    private WorkerAction                 _workerAction;
    
    // The job currently being processed.  Updated by subclasses directly.
    protected Job                        _job;
    
    // Sticky state variable indicating whether the job status
    // has been assigned a finished state.
    private AtomicBoolean _jobStopped = new AtomicBoolean(false);
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    public AbstractPhaseWorker(PhaseWorkerParms parms) 
    {
        // Unpack the parameters.
        super(parms.threadGroup, parms.threadName);
        _connection = parms.connection;
        _scheduler  = parms.scheduler;
        _tenantId   = parms.tenantId;
        _queueName  = parms.queueName;
        _threadNum  = parms.threadNum;
    }
    
    /* ********************************************************************** */
    /*                            Abstract Methods                            */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* processJob:                                                            */
    /* ---------------------------------------------------------------------- */
    /** Subclasses implement this method to perform their phase's work for the
     * specified job.  Problems that should cause the job to be rejected are
     * communicated back to the command processor by throwing an exception.
     * 
     * @param job the job requiring phase processing
     * @throws JobWorkerException if the job's queued message should be rejected
     */
    protected abstract void processJob(Job job) throws JobWorkerException;
    
    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* run:                                                                   */
    /* ---------------------------------------------------------------------- */
    @Override
    public void run() {
        
        // This thread is starting.
        if (_log.isDebugEnabled())
            _log.debug("-> Starting worker thread " + getName() + "");
        
        // ----------------- Job Queue Set Up -------------------
        // Create the channel and bind our queue to it.
        _jobChannel = initJobChannel();
        
        // Exit without a fuss if something's wrong.
        if (_jobChannel == null) return;
        
        // Use a queuing consumer so that we control the threading.  This
        // consumer implements a blocking read call, which allows us to
        // process requests on this thread.
        QueueingConsumer consumer = new QueueingConsumer(_jobChannel);
        
        // We explicitly acknowledge message receipt after processing them.
        boolean autoack = false;
        try {_jobChannelConsumerTag = _jobChannel.basicConsume(_queueName, autoack, consumer);}
        catch (IOException e) {
            String msg = "Worker " + getName() + " is unable consume messages from queue " + 
                        _queueName + ".";
           _log.error(msg, e);
           try {_jobChannel.abort(AMQP.CHANNEL_ERROR, msg);} catch (Exception e1){}
           return; // TODO: figure out better longterm strategy.
        }
        
        // Reusable json mapper.
        ObjectMapper jsonMapper = new ObjectMapper();
        
        // Tracing.
        if (_log.isDebugEnabled())
            _log.debug("[*] Worker " + getName() + " consuming queue " + _queueName + ".");
        
        // ----------------- Queue Read -------------------------
        // Read loop.
        while (true) {
            // Check interrupt status.
            // TODO: Maybe this shouldn't be checked here.
            //       Check for thread interrupts and do clean up.
            //       Note the window here before we wait on I/O
            
            // Wait for next request.
            QueueingConsumer.Delivery delivery = null;
            try {
                // TODO: figure out how to handle interruptions; for now we just exit.
                //       Also, might need a timeout to check on interrupts that occur before I/O wait.
                delivery = consumer.nextDelivery();
            } catch (ShutdownSignalException e) {
                _log.info("Shutdown signal received by thread " + getName(), e);
                break;
            } catch (ConsumerCancelledException e) {
                _log.info("Cancelled signal received by thread " + getName(), e);
                break;
            } catch (InterruptedException e) {
                _log.info("Interrupted signal received by thread " + getName(), e);
                break;
            } catch (Exception e) {
                _log.info("Unexpected exception received by thread " + getName(), e);
                break;
            }
            
            // Decompose the received message.
            Envelope envelope = delivery.getEnvelope();
            AMQP.BasicProperties properties = delivery.getProperties();
            byte[] body = delivery.getBody();

            // Tracing.
            if (_log.isDebugEnabled()) dumpMessageInfo(envelope, properties, body);

            // Once we receive a message, we're on the hook to ack or reject it.
            boolean ack = true;  // assume success
            
            // ----------------- Extract Command --------------------
            // Read the queued json generically.
            // Null body is caught here.
            JsonNode node = null;
            try {node = jsonMapper.readTree(body);}
            catch (IOException e) {
                // Log error message.
                String msg = "Worker " + getName() + 
                     " cannot decode data from queue " + 
                     _queueName + ": " + e.getMessage();
                _log.error(msg, e);
                ack = false;
            }
            if (node == null) ack = false;
            
            // Navigate to the command field.
            JobCommand command = null;
            if (ack)
            {
                // Get the command field from the queued json.
                String cmd = node.path("command").asText();
                try {command = JobCommand.valueOf(cmd);}
                catch (Exception e) {
                    String msg = "Worker " + getName() + 
                         " decoded an invalid command (" + cmd + 
                         ") from queue " + _queueName + ": " + e.getMessage();
                    _log.error(msg, e);
                    ack = false;
                }
            }
            
            // ----------------- Message Processing -----------------
            // All message processors return the ack boolean value that
            // determines the final disposition of the queued message.
            //
            // Process the command.
            if (ack)
            {
                try {
                    // ----- Job input case
                    if (command == JobCommand.WKR_PROCESS_JOB) {
                        ack = doProcessJob(envelope, properties, body);
                    }
                    // ----- Test message input case
                    else if (command == JobCommand.NOOP) {
                        ack = doNoop(node);
                    }
                    // ----- Invalid input case
                    else {
                        // Log the invalid input (we know the body is not null).
                        String msg = "Worker " + getName() + 
                            " received an invalid command: " + (new String(body));
                        _log.error(msg);
                
                        // Reject this input.
                        ack = false;
                    }
                }
                catch (Exception e) {
                    // Command processor are not supposed to throw exceptions,
                    // but we double check anyway.
                    String msg = "Worker " + getName() + 
                             " caught an unexpected command processor exception: " + 
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
                try {_jobChannel.basicAck(envelope.getDeliveryTag(), multipleAck);}
                catch (IOException e) {
                    // We're in trouble if we cannot acknowledge a message.
                    String msg = "Worker " + getName() + 
                            " cannot acknowledge a message received on queue " + 
                            _queueName + ": " + e.getMessage();
                    _log.error(msg, e);
                }
            }
            else {
                // Reject this unreadable message so that
                // it gets discarded or dead-lettered.
                // TODO: Double check reject semantics
                boolean requeue = false;
                try {_jobChannel.basicReject(envelope.getDeliveryTag(), requeue);} 
                catch (IOException e) {
                    // We're in trouble if we cannot reject a message.
                    String msg = "Worker " + getName() + 
                            " cannot reject a message received on queue " + 
                            _queueName + ": " + e.getMessage();
                    _log.error(msg, e);
                }
            }
        } // polling loop
        
        // Best effort try to close channels and cancel consumers.
        closeChannels();
        
        // This thread is terminating.
        if (_log.isDebugEnabled())
            _log.debug("<- Exiting worker thread " + getName() + ".");
    }

    /* ********************************************************************** */
    /*                          Command Processors                            */
    /* ********************************************************************** */
    /* Each command processor handles a specific worker command as defined in
     * AbstractQueueMessage.JobCommand.  Command processors should never throw
     * exception and always return the ack boolean that determines whether
     * a basicAck or basicReject is communicated to the queue from which the 
     * messge was read.
     * 
     * On a case by case basis, command processor implementations can reside
     * in this class (if all phases can share the same code), or in subclasses
     * (if some phase need customized processing) or in both.  If a processor
     * is only implemented in this class, then it can be made private and 
     * reference     
     */
    
    /* ---------------------------------------------------------------------- */
    /* doProcessJob:                                                          */
    /* ---------------------------------------------------------------------- */
    /** Process WKR_PROCESS_JOB message, which are central focus of the jobs
     * subsystem since they cause jobs to execute. 
     * 
     * @param envelope message envelop
     * @param properties message properties
     * @param body the raw bytes of the message
     * @return true to acknowledge the message, false to reject it
     */
    protected boolean doProcessJob(Envelope envelope, 
                                   BasicProperties properties, 
                                   byte[] body) 
    {
        // ---------------------- Marshalling ----------------------
        // Marshal the json message into it message object.
        ProcessJobMessage qjob = null;
        try {qjob = ProcessJobMessage.fromJson(new String(body));}
        catch (IOException e) {
            // Log error message.
            String msg = "Worker " + getName() + 
                         " cannot decode data from queue " + 
                         getQueueName() + ": " + e.getMessage();
            _log.error(msg, e);
            
            // Reject this input.
            return false;
        }
        
        // ---------------------- Get Job --------------------------
        // At a minimum we need the unique job id.
        if (StringUtils.isBlank(qjob.uuid))
        {
            // Log the invalid input and quit.
            String msg = "Worker " + getName() + 
                         " received a WKR_PROCESS_JOB message with an invalid uuid: " +
                         (new String(body));
            _log.error(msg);
            return false;
        }
        
        // We have a job reference to process.
        Job job = null;
        try {job = JobDao.getByUuid(qjob.uuid);}
        catch (JobException e) {
            String msg = "Worker " + getName() + 
                         " unable to retrieve Job with UUID " + qjob.uuid +
                         " (" + qjob.name + ") from database.";
            _log.error(msg, e);
            return false;
        }
        
        // Make sure we got a job.
        if (job == null) {
            String msg = "Worker " + getName() + 
                         " unable to find Job with UUID " + qjob.uuid +
                         " (" + qjob.name + ") in database.";
            _log.error(msg);
            return false;
        }
        
        // TODO: Do we need to set the status on this job?
        // Make sure the job tenant matches this worker's assigned tenant.
        if (!getTenantId().equals(job.getTenantId())) {
            String msg = "Worker " + getName() + " assigned tenantId " +
                    getTenantId() + " received a job with UUID " + qjob.uuid +
                    " (" + qjob.name + ") with tenantId " + 
                    job.getTenantId() + ".";
            _log.error(msg);
            return false;
        }
        
        // ---------------------- Phase Processing -----------------
        // This thread should have no tenant info loaded.  We set it 
        // up here so things like app and system lookups will stay 
        // local to the tenant.  The calling method resets these values.
        TenancyHelper.setCurrentTenantId(job.getTenantId());
        TenancyHelper.setCurrentEndUser(job.getOwner());
        
        // Invoke the phase processor.
        boolean jobProcessed = true;
        try {processJob(job);}
        catch (Exception e)
        {
            String msg = "Worker " + getName() + 
                         " unable to process Job with UUID " + qjob.uuid +
                         " (" + qjob.name + ").";
            _log.error(msg, e);
            jobProcessed = false;
        }
        
        // Successful processing.
        return jobProcessed;
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
            _log.info("Worker " + getName() + " received NOOP message.");
        }
        else {
            _log.info("Worker " + getName() +  
                    " received NOOP test message:\n > " + testMessage + "\n");
        }
        
        // Always release message from queue.
        return true;
    }
    
    /* ********************************************************************** */
    /*                               Accessors                                */
    /* ********************************************************************** */
    public AbstractPhaseScheduler getScheduler() {
        return _scheduler;
    }

    public String getTenantId() {
        return _tenantId;
    }

    public String getQueueName() {
        return _queueName;
    }

    public int getThreadNum() {
        return _threadNum;
    }

    public JobPhaseType getPhaseType() {
        return _scheduler.getPhaseType();
    }
    
    protected WorkerAction getWorkerAction() {
        return _workerAction;
    }

    protected void setWorkerAction(WorkerAction _workerAction) {
        this._workerAction = _workerAction;
    }
    
    /* ---------------------------------------------------------------------- */
    /* checkStopped:                                                          */
    /* ---------------------------------------------------------------------- */
    /** This method is called at convenient points during execution in which
     * exception processing will not leave the system in an inconsistent state.
     * This call is equivalent to calling:
     * 
     *      checkStopped(false)
     * 
     * @throws ClosedByInterruptException when the worker thread has been interrupted
     * @throws JobFinishedException when the job has transitioned to a finished state
     */
    @Override
    public void checkStopped() 
     throws ClosedByInterruptException, JobFinishedException
    {
        checkStopped(false);
    }
    
    /* ---------------------------------------------------------------------- */
    /* checkStopped:                                                          */
    /* ---------------------------------------------------------------------- */
    /** This method is called at convenient points during execution in which
     * exception processing will not leave the system in an inconsistent state.
     * The logging flag causes debug records to be written to the log when an
     * exception is going to be thrown.
     * 
     * @param logException true causes this method to log before throwing an exception
     * @throws ClosedByInterruptException when the worker thread has been interrupted
     * @throws JobFinishedException when the job has transitioned to a finished state
     */
    @Override
    public void checkStopped(boolean logException) 
     throws ClosedByInterruptException, JobFinishedException
    {
        // See if the thread was interrupted and leave the flag unchanged.
        if (Thread.currentThread().isInterrupted()) {
            if (logException && _log.isDebugEnabled()) {
                String msg = "Worker " + Thread.currentThread().getName() +
                             " interrupted while processing job " + 
                             _job.getUuid() + " (" + _job.getName() + ").";
                _log.debug(msg);
            }
            // Always throw the identifying exception.
            throw new ClosedByInterruptException();
        }
        
        // See if the job was stopped.
        if (isJobStopped()) {
            String msg = "Worker " + Thread.currentThread().getName() +
                         " stopping job " + _job.getUuid() + 
                         " (" + _job.getName() + ").";
            if (logException && _log.isDebugEnabled()) _log.debug(msg);
            
            // Always throw the identifying exception.
            throw new JobFinishedException(msg);
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* isJobStopped:                                                          */
    /* ---------------------------------------------------------------------- */
    /** Determine if the job was explicitly stopped using the topic interrupt
     * mechanism.  If so, we set a sticky flag so that subsequent checks can
     * quickly discover that the job was moved into a stopped state.
     * 
     * @return true if the job is in a finished state; false otherwise.
     */
    @Override
    public boolean isJobStopped()
    {
        // Did we previously encounter a job interrupt?
        if (_jobStopped.get()) return true;
        
        // We shouldn't be called when there's no 
        // current job, but we play it safe.
        if (_job == null) return true;
        
        // Query the database for changes to a finished status.
        boolean interrupted = JobInterruptUtils.isJobInterrupted(_job);
        if (interrupted) _jobStopped.set(true);
        return interrupted;
    }
    
    /* ********************************************************************** */
    /*                            Protected Methods                           */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* reset:                                                                 */
    /* ---------------------------------------------------------------------- */
    /** Reset all fields that are specific to a particular job.  This method
     * resets the state of this class instance back to it initial state just
     * after queue communication has been initialized. 
     */
    protected void reset()
    {
        // Reset the job state.
        _job = null;
        
        // Reset the job stopped flag.
        _jobStopped.set(false);
        
        // Remove the action associated with a job.
        setWorkerAction(null);
    }

    /* ---------------------------------------------------------------------- */
    /* checkJobQuota:                                                         */
    /* ---------------------------------------------------------------------- */
    protected void checkJobQuota() throws JobWorkerException
    {
        // verify the user is within quota to run the job before staging the data.
        // this should have been caught by the original job selection, but could change
        // due to high concurrency. 
        try 
        {
            JobQuotaCheck quotaValidator = new JobQuotaCheck(_job);
            quotaValidator.check();
        } 
        catch (QuotaViolationException e) 
        {
            try
            {
                if (_log.isDebugEnabled())
                    _log.debug("Input staging for job " + _job.getUuid() + 
                               " is current paused due to quota restrictions. " + e.getMessage());
                _job = JobManager.updateStatus(_job, JobStatusType.PENDING, 
                    "Input staging for job is current paused due to quota restrictions. " + 
                    e.getMessage() + ". This job will resume staging once one or more current jobs complete.");
            }
            catch (Throwable e1) {
                _log.error("Failed to update job " + _job.getUuid() + " status to PENDING");
            }   
            throw new JobWorkerException(e);
        }
        catch (SystemUnavailableException e) 
        {
            try
            {
                if (_log.isDebugEnabled())
                    _log.debug("System for job " + _job.getUuid() + 
                               " is currently unavailable. " + e.getMessage());
                _job = JobManager.updateStatus(_job, JobStatusType.PENDING, 
                    "Input staging is current paused waiting for a system containing " + 
                    "input data to become available. If the system becomes available " +
                    "again within 7 days, this job " + 
                    "will resume staging. After 7 days it will be killed.");
            }
            catch (Throwable e1) {
                _log.error("Failed to update job " + _job.getUuid() + " status to PENDING");
            }
            throw new JobWorkerException(e);
        }
        catch (Throwable e) 
        {
            try
            {
                _log.error("Failed to stage inputs for job " + _job.getUuid(), e);
                _job = JobManager.updateStatus(_job, JobStatusType.FAILED, e.getMessage());
            }
            catch (Throwable e1) {
                _log.error("Failed to update job " + _job.getUuid() + " status to FAILED");
            }
            throw new JobWorkerException(e);
        }
    }
        
    /* ---------------------------------------------------------------------- */
    /* checkSoftwareLocality:                                                 */
    /* ---------------------------------------------------------------------- */
    protected void checkSoftwareLocality() throws JobWorkerException
    {
        // Get the software system.
        Software software = SoftwareDao.getSoftwareByUniqueName(_job.getSoftwareName());
        
        // if the execution system for this job has a local storage config,
        // all other transfer workers will pass on it.
        if (!StringUtils.equals(Settings.LOCAL_SYSTEM_ID, _job.getSystem()) &&
            software.getExecutionSystem().getStorageConfig().getProtocol().equals(StorageProtocolType.LOCAL))
        {
            // This is not really an error, but we need to throw some exception
            // to signal that this phase's processing should end for this job.
            String msg = "Job " + _job.getName() + " (" + _job.getUuid() +
                         ") failed the software locality check.";
            throw new JobWorkerException(msg);
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* checkRetryPeriod:                                                      */
    /* ---------------------------------------------------------------------- */
    protected void checkRetryPeriod(int days) throws JobWorkerException, JobException
    {
        // We will only retry for the specified number of days
        if (new DateTime(_job.getCreated()).plusDays(days).isBeforeNow()) 
        {
            _job = JobManager.updateStatus(_job, JobStatusType.KILLED, 
                    "Removing job from queue after " + days + " days attempting to stage inputs.");
            _job = JobManager.updateStatus(_job, JobStatusType.FAILED, 
                    "Unable to stage inputs for job after " + days + " days. Job cancelled.");
            
            // This is not really an error, but we need to throw some exception
            // to signal that this phase's processing should end for this job.
            String msg = "Job " + _job.getName() + " (" + _job.getUuid() +
                         ") failed the software locality check.";
            throw new JobWorkerException(msg);
        } 
    }
    
    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* initJobChannel:                                                        */
    /* ---------------------------------------------------------------------- */
    private Channel initJobChannel() 
    {
        // Create this thread's channel.
        Channel channel = null;
        try {channel = _connection.createChannel();} 
             catch (IOException e) {
                 String msg = "Worker " + getName() + " aborting!  " + 
                              "Unable to create channel for queue " + _queueName + 
                              ": " + e.getMessage();
                 _log.error(msg, e);
             
                 // TODO: We need to raise an exception in a way that does not
                 //       cause an infinite cascade of new thread launches, but
                 //       does signal that there's a serious problem.
                 return null;
         }
         if (_log.isInfoEnabled()) 
             _log.info("Created job channel number " + channel.getChannelNumber() + 
                       " for worker " + getName() + ".");
         
         // Set the prefetch count so that the consumer using this 
         // channel only receieves the next request after the previous
         // request has been acknowledged.
         int prefetchCount = 1;
         try {channel.basicQos(prefetchCount);}
             catch (IOException e) {
                 String msg = "Worker " + getName() + " aborting!  " + 
                         "Unable to set prefetch count for queue " + _queueName + 
                         ": " + e.getMessage();
                 _log.error(msg, e);
            
                 // TODO: We need to raise an exception in a way that does not
                 //       cause an infinite cascade of new thread launches, but
                 //       does signal that there's a serious problem.
                 return null;
             }
         
         // Create this thread's exchange.
         boolean durable = true;
         try {channel.exchangeDeclare(WORKER_EXCHANGE_NAME, "direct", durable);}
             catch (IOException e) {
                 String msg = "Worker " + getName() + " aborting!  " + 
                         "Unable to create exchange for queue " + _queueName + 
                         ": " + e.getMessage();
                 _log.error(msg, e);
            
                 // TODO: We need to raise an exception in a way that does not
                 //       cause an infinite cascade of new thread launches, but
                 //       does signal that there's a serious problem.
                 try {channel.abort(AMQP.CHANNEL_ERROR, msg);} catch (Exception e1){}
                 return null;
             }
         
         // Create the queue with the configured name.
         durable = true;
         boolean exclusive = false;
         boolean autoDelete = false;
         try {channel.queueDeclare(_queueName, durable, exclusive, autoDelete, null);}
             catch (IOException e) {
                 String msg = "Worker " + getName() + " aborting!  " + 
                         "Unable to create queue " + _queueName + 
                         ": " + e.getMessage();
                 _log.error(msg, e);
            
                 // TODO: We need to raise an exception in a way that does not
                 //       cause an infinite cascade of new thread launches, but
                 //       does signal that there's a serious problem.
                 try {channel.abort(AMQP.CHANNEL_ERROR, msg);} catch (Exception e1){}
                 return null;
             }
        
         // Bind the queue to the exchange using the queue name as the binding key.
         try {channel.queueBind(_queueName, WORKER_EXCHANGE_NAME, _queueName);}
             catch (IOException e) {
                 String msg = "Worker " + getName() + " aborting!  " + 
                         "Unable to bind queue " + _queueName + 
                         ": " + e.getMessage();
                 _log.error(msg, e);
                
                 // TODO: We need to raise an exception in a way that does not
                 //       cause an infinite cascade of new thread launches, but
                 //       does signal that there's a serious problem.
                 try {channel.abort(AMQP.CHANNEL_ERROR, msg);} catch (Exception e1){}
                 return null;
             }
         
         // Success.
         return channel;
    }
    
   /* ---------------------------------------------------------------------- */
    /* closeChannels:                                                         */
    /* ---------------------------------------------------------------------- */
    /** Make a best-effort attempt to release all channels and cancel all queue 
     * consumers for which we've registered. 
     */
    private void closeChannels()
    {
        // Try to close job input channel.
        if (_jobChannel != null) 
            try {_jobChannel.close();}
            catch (Exception e)
            {
                String msg = "Worker " + getName() + " unable to close job channel " + 
                             _jobChannel.getChannelNumber() + ".";
                _log.error(msg, e);
            }
        
        // Try to close the interrupt channel.
        // Try to close job input channel.
        if (_interruptChannel != null) 
            try {_interruptChannel.close();}
            catch (Exception e)
            {
                String msg = "Worker " + getName() + " unable to close interrupt channel " + 
                             _interruptChannel.getChannelNumber() + ".";
                _log.error(msg, e);
            }
    }
    
    /* ---------------------------------------------------------------------- */
    /* dumpMessageInfo:                                                       */
    /* ---------------------------------------------------------------------- */
    /** Write debug message and threading information.  This methods should
     * only be called after checking that debugging is enabled.
     * 
     * @param envelope the message envelope
     * @param properties the message properties
     * @param body the message
     */
    private void dumpMessageInfo(Envelope envelope, AMQP.BasicProperties properties,
                                 byte[] body)
    {
        // We assume all input parameters are non-null.
        Thread curthd = Thread.currentThread();
        ThreadGroup curgrp = curthd.getThreadGroup();
        String msg = "\n------------------- Worker Bytes Received: " + body.length + "\n";
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

}
