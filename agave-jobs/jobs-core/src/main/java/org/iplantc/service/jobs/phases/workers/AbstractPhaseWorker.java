package org.iplantc.service.jobs.phases.workers;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobWorkerException;
import org.iplantc.service.jobs.exceptions.QuotaViolationException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.managers.JobQuotaCheck;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.phases.schedulers.AbstractPhaseScheduler;
import org.iplantc.service.jobs.phases.schedulers.AbstractPhaseScheduler.QueueableJob;
import org.iplantc.service.jobs.queue.actions.WorkerAction;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.model.enumerations.StorageProtocolType;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
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
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(AbstractPhaseWorker.class);
    
    // Communication constants.
    public static final String WORKER_EXCHANGE_NAME = "JobWorkerExchange";

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
    private Channel                      _channel;
    private WorkerAction                 _workerAction;
    
    // The job currently being processed.  Updated by subclasses directly.
    protected Job                        _job;
    
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
        _scheduler = parms.scheduler;
        _tenantId = parms.tenantId;
        _queueName = parms.queueName;
        _threadNum = parms.threadNum;
    }
    
    /* ********************************************************************** */
    /*                            Abstract Methods                            */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* processJob:                                                            */
    /* ---------------------------------------------------------------------- */
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
        
        // ----------------- Queue Set Up -----------------------
        // Create the channel and bind our queue to it.
        _channel = initChannel();
        
        // Exit without a fuss if something's wrong.
        if (_channel == null) return;
        
        // Use a queuing consumer so that we control the threading.  This
        // consumer implements a blocking read call, which allows us to
        // process requests on this thread.
        QueueingConsumer consumer = new QueueingConsumer(_channel);
        
        // We explicitly acknowledge message receipt after processing them.
        boolean autoack = false;
        try {_channel.basicConsume(_queueName, autoack, consumer);}
        catch (IOException e) {
            String msg = "Worker " + getName() + " is unable consume messages from queue " + 
                        _queueName + ".";
           _log.error(msg, e);
           try {_channel.abort(AMQP.CHANNEL_ERROR, msg);} catch (Exception e1){}
           return; // TODO: figure out better longterm strategy.
        }
        
        // Tracing.
        if (_log.isDebugEnabled())
            _log.debug("[*] Worker " + getName() + " consuming queue " + _queueName + ".");
        
        // ----------------- Queue Read -------------------------
        // Read loop.
        while (true) {
            // Wait for next request.
            QueueingConsumer.Delivery delivery = null;
            try {
                // TODO: figure out how to handle interruptions; for now we just exit.
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
            }
            
            // Decompose the received message.
            Envelope envelope = delivery.getEnvelope();
            AMQP.BasicProperties properties = delivery.getProperties();
            byte[] body = delivery.getBody();

            // Tracing.
            if (_log.isDebugEnabled()) dumpMessageInfo(envelope, properties, body);
            
            // We expect only json messages.
            boolean ack = true; // assume success
            ObjectMapper m = new ObjectMapper();
            QueueableJob qjob = null;
            try {qjob = m.readValue(body, QueueableJob.class);}
            catch (IOException e) {
                // Log error message.
                String msg = "Worker " + getName() + 
                             " cannot decode data from queue " + 
                             _queueName + ": " + e.getMessage();
                _log.error(msg, e);
                
                // Reject this input.
                ack = false;
            }
            
            // Allow graceful processing. 
            if (qjob == null) qjob = new QueueableJob();
            
            // ----------------- Message Processing -----------------
            // ----- Test message input case
            if (!StringUtils.isBlank(qjob.testMessage)) {
                System.out.println("Worker " + getName() +  
                        " received message:\n > " + qjob.testMessage + "\n");
            }
            // ----- Job input case
            else if (!StringUtils.isBlank(qjob.uuid)) {
                // We have a job reference to process.
                Job job = null;
                try {job = JobDao.getByUuid(qjob.uuid);}
                catch (JobException e) {
                    String msg = "Worker " + getName() + 
                                 " unable to retrieve Job with UUID " + qjob.uuid +
                                 " (" + qjob.name + ") from database.";
                    _log.error(msg, e);
                    ack = false;
                }
                
                // Make sure we got a job.
                if (job == null) {
                    String msg = "Worker " + getName() + 
                                 " unable to find Job with UUID " + qjob.uuid +
                                 " (" + qjob.name + ") from database.";
                    _log.error(msg);
                    ack = false;
                }
                else {
                    // This has no tenant info loaded.  We set it up here so things 
                    // like app and system lookups will stay local to the tenant.
                    TenancyHelper.setCurrentTenantId(job.getTenantId());
                    TenancyHelper.setCurrentEndUser(job.getOwner());
                
                    // Invoke the phase processor.
                    try {processJob(job);}
                    catch (Exception e)
                    {
                        String msg = "Worker " + getName() + 
                                     " unable to process Job with UUID " + qjob.uuid +
                                     " (" + qjob.name + ").";
                        _log.error(msg);
                        ack = false;
                    }
                }
            }
            // ----- Invalid input case
            else {
                // Log the invalid input.
                String msg = "Worker " + getName() + " received invalid input: " +
                (new String(body));
                _log.error(msg);
                
                // Reject this input.
                ack = false;
            }
            
            // ----------------- Clean Up ---------------------------
            // Don't leave stale state around.
            TenancyHelper.setCurrentTenantId(null);
            TenancyHelper.setCurrentEndUser(null);
            
            // Determine whether to ack or nack the request.
            if (ack) {
                // Don't forget to send the ack!
                boolean multipleAck = false;
                try {_channel.basicAck(envelope.getDeliveryTag(), multipleAck);}
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
                boolean requeue = false;
                try {_channel.basicReject(envelope.getDeliveryTag(), requeue);} 
                catch (IOException e) {
                    // We're in trouble if we cannot reject a message.
                    String msg = "Worker " + getName() + 
                            " cannot reject a message received on queue " + 
                            _queueName + ": " + e.getMessage();
                    _log.error(msg, e);
                }
            }
        } // polling loop
        
        // This thread is terminating.
        if (_log.isDebugEnabled())
            _log.debug("<- Exiting worker thread " + getName() + ".");
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
    
    /* ********************************************************************** */
    /*                            Protected Methods                           */
    /* ********************************************************************** */
    protected WorkerAction getWorkerAction() {
        return _workerAction;
    }

    protected void setWorkerAction(WorkerAction _workerAction) {
        this._workerAction = _workerAction;
    }
    
    /* ---------------------------------------------------------------------- */
    /* isStopped:                                                             */
    /* ---------------------------------------------------------------------- */
    protected boolean isStopped()
    {
        // TODO: Implement the interrupt mechanism described in the class 
        //       comment of AbstractPhaseScheduler.  The solution must also
        //       take into account StagingAction.  Also consider what state
        //       hibernate is in when we receive the interrupt (see 
        //       StageWatch.rollback()).
        return false;
    }
    
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
    /* initChannel:                                                           */
    /* ---------------------------------------------------------------------- */
    private Channel initChannel() {
        
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
             _log.info("Created channel number " + channel.getChannelNumber() + 
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
