package org.iplantc.service.jobs.dao;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.jobs.exceptions.JobQueueException;
import org.iplantc.service.jobs.exceptions.JobQueueFilterException;
import org.iplantc.service.jobs.exceptions.JobQueuePriorityException;
import org.iplantc.service.jobs.model.JobQueue;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.iplantc.service.jobs.queue.SelectorFilter;


/** Data access object for the job_queues table.
 * 
 * This class issues native SQL commands through Hibernate session objects.
 * See the JobQueue class for the rationale for not using Hibernate more
 * extensively. 
 * 
 * @author rcardone
 */
public class JobQueueDao {
    
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(JobQueueDao.class);
    
    /* ********************************************************************** */
    /*                                 Enums                                  */
    /* ********************************************************************** */
    // Used to consolidate code that only varies by lookup key.
    private enum Selector {
        NAME("name"), UUID("uuid");
        
        private String label;
        private Selector(String s){label = s;}
        @Override
        public String toString(){return label;}
    };
    
    // Used to consolidate code that only varies by the integer field to be updated.
    private enum UpdateField {
        PRIORITY("priority"), NUM_WORKERS("num_workers"), MAX_MESSAGES("max_messages");
        
        private String label;
        private UpdateField(String s){label = s;}
        @Override
        public String toString(){return label;}
    }
    
    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* createQueue:                                                           */
    /* ---------------------------------------------------------------------- */
    /** Define a new job queue in the database.
     * 
     * @throws JobQueueException input or operational error
     */
    public int createQueue(JobQueue jobQueue) 
     throws JobQueueException, JobQueueFilterException
    {
        // ------------------------- Check Input -------------------------
        // Null/empty checks.
        if (jobQueue == null) {
            String msg = "Null job queue object received.";
            _log.error(msg);
            throw new JobQueueException(msg);
        }
        if (StringUtils.isBlank(jobQueue.getName())) {
            String msg = "No job queue name specified in job queue definition.";
            _log.error(msg);
            throw new JobQueueException(msg);
        }
        if (StringUtils.isBlank(jobQueue.getTenantId())) {
            String msg = "No tenant id specified in job queue definition.";
            _log.error(msg);
            throw new JobQueueException(msg);
        }
        if (jobQueue.getPhase() == null) {
            String msg = "No phase specified in job queue definition.";
            _log.error(msg);
            throw new JobQueueException(msg);
        }
        
        // The queue name must either be "phase.tenantid" or begin with "phase.tenantid."
        String prefix = jobQueue.getPhase().name() + '.' + jobQueue.getTenantId();
        if (!jobQueue.getName().equals(prefix) && !jobQueue.getName().startsWith(prefix + '.')) {
            String msg = "Job queue names must begin with 'phase.tenantId', such as: " + prefix;
            _log.error(msg);
            throw new JobQueueException(msg);
        }
        
        // As a convenience on queue creation only, set the 
        // filter to a default value if its empty. 
        if (StringUtils.isBlank(jobQueue.getFilter())) {
            jobQueue.setFilter("phase = '" + jobQueue.getPhase().name() + 
                               "' AND tenant_id = '" + jobQueue.getTenantId().trim() + "'");
        }
        // Validate the filter.
        validateFilter(jobQueue.getFilter());
        
        // Numeric checks.
        if (jobQueue.getPriority() < 1) {
            String msg = "A priority of at least 1 must be specified in job queue definition.";
            _log.error(msg);
            throw new JobQueueException(msg);
        }
        if (jobQueue.getNumWorkers() < 1) {
            String msg = "At least 1 worker thread must be specified in job queue definition.";
            _log.error(msg);
            throw new JobQueueException(msg);
        }
        if (jobQueue.getMaxMessages() < 1) {
            String msg = "Maximum messages of at least 1 must be specified in job queue definition.";
            _log.error(msg);
            throw new JobQueueException(msg);
        }
        
        // ------------------------- Permission Checks -------------------
        // We only allow users to create queues under their own tenant. 
        String currentTenantId = TenancyHelper.getCurrentTenantId();
        if (StringUtils.isBlank(currentTenantId)) {
            String msg = "Unable to retrieve current tenant id.";
            _log.error(msg);
            throw new JobQueueException(msg);
        }
        if (!jobQueue.getTenantId().equals(currentTenantId)) {
            String msg = "Unable to create job queue " + jobQueue.getName() + " because " +
                         "the current tenant id (" + currentTenantId + ") does not match " +
                         "the job queue's tenant id (" + jobQueue.getTenantId()+ ").";
            _log.error(msg);
            throw new JobQueueException(msg);
        }
        
        // ------------------------- Complete Input ----------------------
        // Set the queue dates to the current time.
        Date curDate = new Date();
        jobQueue.setCreated(curDate);;
        jobQueue.setLastUpdated(curDate);
        
        // Set the uuid.
        jobQueue.setUuid(new AgaveUUID(UUIDType.JOB_QUEUE).toString());        
        
        // ------------------------- Call SQL ----------------------------
        int rows = 0; // rows affected
        try
        {
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Create the insert command.
            String sql = "insert into job_queues " +
                         "(uuid, name, tenant_id, phase, priority, num_workers, " +
                         "max_messages, filter, created, last_updated) " +
                         "values " +
                         "(:uuid, :name, :tenant_id, :phase, :priority, :num_workers, " +
                         ":max_messages, :filter, :created, :last_updated) ";
            
            // Fill in the placeholders.           
            Query qry = session.createSQLQuery(sql);
            qry.setString("uuid", jobQueue.getUuid().trim());
            qry.setString("name", jobQueue.getName().trim());
            qry.setString("tenant_id", jobQueue.getTenantId().trim());
            qry.setString("phase", jobQueue.getPhase().name());
            qry.setInteger("priority", jobQueue.getPriority());
            qry.setInteger("num_workers", jobQueue.getNumWorkers());
            qry.setInteger("max_messages", jobQueue.getMaxMessages());
            qry.setString("filter", jobQueue.getFilter().trim());
            qry.setTimestamp("created", jobQueue.getCreated());
            qry.setTimestamp("last_updated", jobQueue.getLastUpdated());
            
            // Issue the call.
            rows = qry.executeUpdate();
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){_log.error("Rollback failed.", e1);}
            
            String msg = "Unable to create new job queue: \n" + jobQueue.toString();
            _log.error(msg);
            throw new JobQueueException(msg, e);
        }
        finally {
            try {HibernateUtil.commitTransaction();} 
            catch (Exception e) 
            {
                String msg = "Unable to commit job queue create transaction.";
                _log.error(msg);
                throw new JobQueueException(msg, e);
            }
        }
        
        // Return the number of rows affected.
        return rows;
    }

    /* ---------------------------------------------------------------------- */
    /* getQueueByName:                                                        */
    /* ---------------------------------------------------------------------- */
    /** Retrieve the named queue object from the database.
     * 
     * @param name full queue name
     * @param tenantId the queue's tenantId
     * @return the complete queue record
     * @throws JobQueueException input or operational error
     */
    public JobQueue getQueueByName(String name, String tenantId) 
     throws JobQueueException
    {
        return getQueue(Selector.NAME, name, tenantId);
    }
    
    /* ---------------------------------------------------------------------- */
    /* getQueueByUUID:                                                        */
    /* ---------------------------------------------------------------------- */
    /** Retrieve the queue by uuid.
     * 
     * @param uuid
     * @param tenantId the queue's tenantId
     * @return the complete queue record
     * @throws JobQueueException input or operational error
     */
    public JobQueue getQueueByUUID(String uuid, String tenantId) 
     throws JobQueueException
    {
        return getQueue(Selector.UUID, uuid, tenantId);
    }
    
    /* ---------------------------------------------------------------------- */
    /* deleteQueueByName:                                                     */
    /* ---------------------------------------------------------------------- */
    /** Delete the named queue.
     * 
     * @param name full name of the queue to delete
     * @param tenantId the queue's tenantId
     * @return number of queues affected (0 or 1)
     * @throws JobQueueException input or operational error
     */
    public int deleteQueueByName(String name, String tenantId) 
     throws JobQueueException
    {
        return deleteQueue(Selector.NAME, name, tenantId);
    }
    
    /* ---------------------------------------------------------------------- */
    /* deleteQueueByUUID:                                                     */
    /* ---------------------------------------------------------------------- */
    /** Delete a queue using its uuid.
     * 
     * @param uuid the UUID of the queue to be deleted
     * @param tenantId the queue's tenantId
     * @return number of queues affected (0 or 1)
     * @throws JobQueueException input or operational error
     */
    public int deleteQueueByUUID(String uuid, String tenantId) 
     throws JobQueueException
    {
        return deleteQueue(Selector.UUID, uuid, tenantId);
    }
    
    /* ---------------------------------------------------------------------- */
    /* getQueues:                                                             */
    /* ---------------------------------------------------------------------- */
    /** Get all queues for all tenants defined for the specified phase, which
     * cannot be null.
     * 
     * @param phase the job processing phase whose queues are to be returned or
     *              null for all phases
     * @return the list of queues for the specified phase in tenant ascending,
     *         priority descending order.  For example, the first queue listed 
     *         for each tenant has the highest priority.  The list will never 
     *         be null, but can be empty.
     * @throws JobQueueException input or operational error
     */
    public List<JobQueue> getQueues(JobPhaseType phase) 
     throws JobQueueException
    {
        // ------------------------- Check Input -------------------------
        // The phase parameter can be null.
        if (phase == null) {
            String msg = "Null job queue phase received.";
            _log.error(msg);
            throw new JobQueueException(msg);
        }
        
        // ------------------------- Call SQL ----------------------------
        ArrayList<JobQueue> list = new ArrayList<>();
        try
        {
            // Begin new transaction.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Create the insert command using table definition field order.
            // NOTE: Any changes to the job_queues table requires maintenance
            //       here and in the populate routine below.
            String sql = "select id, uuid, name, tenant_id, phase, priority, " +
                            "num_workers, max_messages, filter, created, last_updated " +
                         "from job_queues " +
                         "where phase = :phase " +
                         "order by tenant_id, phase, priority desc";
            
            // Fill in the placeholders.           
            Query qry = session.createSQLQuery(sql);
            qry.setString("phase", phase.name());
            
            // Issue the call and populate the model object.
            // (Yes, this is the tax for not using hibernate...)
            @SuppressWarnings("unchecked")
            List<Object> objList = qry.list();
            for (Object obj : objList) list.add(populateJobQueue(obj));
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){_log.error("Rollback failed.", e1);}
            
            String msg = "Unable to query job queues for phase " + phase.name() + ".";
            _log.error(msg);
            throw new JobQueueException(msg, e);
        }
        finally {
            try {HibernateUtil.commitTransaction();} 
            catch (Exception e) 
            {
                String msg = "Unable to commit query transaction " +
                             "for job queues in phase " + phase.name() + ".";
                _log.error(msg);
                throw new JobQueueException(msg, e);
            }
        }
        
        return list;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getQueues:                                                             */
    /* ---------------------------------------------------------------------- */
    /** Get all queues for the current tenant defined for any phase.
     * 
     * @param tenantId the queue's tenantId
     * @return the list of queues in (phase ASC, priority DESC) order.  The phases 
     *         appear in alphabetic order, the priorities appear in highest to lowest
     *         priority order.  For example, within each phase grouping, the first  
     *         queue listed has the highest priority.
     * @throws JobQueueException operational error
     */
    public List<JobQueue> getQueues(String tenantId) 
     throws JobQueueException
    {
        return getQueues(null, tenantId);
    }
    
    /* ---------------------------------------------------------------------- */
    /* getQueues:                                                             */
    /* ---------------------------------------------------------------------- */
    /** Get all queues for the current tenant defined for the specified phase.
     * If the phase is null, then queues defined for all phases are returned.
     * 
     * @param phase the job processing phase whose queues are to be returned or
     *              null for all phases
     * @param tenantId the queue's tenantId
     * @return the list of queues for the specified phase in highest to lowest
     *         priority order.  For example, the first queue listed has the 
     *         highest priority.  The list will never be null, but can be empty.
     * @throws JobQueueException input or operational error
     */
    public List<JobQueue> getQueues(JobPhaseType phase, String tenantId) 
     throws JobQueueException
    {
        // ------------------------- Check Input -------------------------
        // The phase parameter can be null.
        if (StringUtils.isBlank(tenantId)) {
            String msg = "Invalid job queue tenant ID received.";
            _log.error(msg);
            throw new JobQueueException(msg);
        }
        
        // ------------------------- Call SQL ----------------------------
        ArrayList<JobQueue> list = new ArrayList<>();
        try
        {
            // Begin new transaction.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Create the insert command using table definition field order.
            // NOTE: Any changes to the job_queues table requires maintenance
            //       here and in the populate routine below.
            String sql = "select id, uuid, name, tenant_id, phase, priority, " +
                            "num_workers, max_messages, filter, created, last_updated " +
                         "from job_queues " +
                         "where tenant_id = :tenant_id";
            if (phase != null) sql += " and phase = :phase";
            sql += " order by tenant_id, phase, priority desc";
            
            // Fill in the placeholders.           
            Query qry = session.createSQLQuery(sql);
            qry.setString("tenant_id", tenantId.trim());
            if (phase != null) qry.setString("phase", phase.name());
            
            // Issue the call and populate the model object.
            // (Yes, this is the tax for not using hibernate...)
            @SuppressWarnings("unchecked")
            List<Object> objList = qry.list();
            for (Object obj : objList) list.add(populateJobQueue(obj));
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){_log.error("Rollback failed.", e1);}
            
            String msg = "Unable to query job queues in tenant " + tenantId + ".";
            _log.error(msg);
            throw new JobQueueException(msg, e);
        }
        finally {
            try {HibernateUtil.commitTransaction();} 
            catch (Exception e) 
            {
                String msg = "Unable to commit query transaction " +
                             "for job queues in tenant " + tenantId + ".";
                _log.error(msg);
                throw new JobQueueException(msg, e);
            }
        }
        
        return list;
    }
    
    /* ---------------------------------------------------------------------- */
    /* updatePriority:                                                        */
    /* ---------------------------------------------------------------------- */
    /** Change the priority of a queue in the current tenant.  Queue priority
     * determines the order in which queues are considered in the queue selection 
     * algorithm.  This algorithm matches jobs to queues in each of the phases 
     * of job processing.
     * 
     * A queue's priority must be unique among all queues in a phase under the
     * same tenant.
     * 
     * @param name the full name of the target queue
     * @param tenantId the queue's tenantId
     * @param priority 1 or more
     * @return number of queues affected (0 or 1)
     * @throws JobQueueException input or operational error
     * @throws JobQueuePriorityException duplicate priority error
     */
    public int updatePriority(String name, int priority, String tenantId) 
     throws JobQueueException, JobQueuePriorityException
    {
        return updateIntegerField(name, UpdateField.PRIORITY, priority, tenantId);
    }
    
    /* ---------------------------------------------------------------------- */
    /* updateNumWorkers:                                                      */
    /* ---------------------------------------------------------------------- */
    /** Change the default number of worker threads assigned to a queue when the 
     * queue's scheduler initializes.  This method does not change the number
     * of running workers currently associated with a queue--it only affects the
     * static configuration used on scheduler restart.
     * 
     * @param name the full name of the target queue
     * @param numWorkers 1 or more
     * @param tenantId the queue's tenantId
     * @return number of queues affected (0 or 1)
     * @throws JobQueueException input or operational error
     */
    public int updateNumWorkers(String name, int numWorkers, String tenantId) 
     throws JobQueueException
    {
        return updateIntegerField(name, UpdateField.NUM_WORKERS, numWorkers, tenantId);
    }
    
    /* ---------------------------------------------------------------------- */
    /* updateMaxMessages:                                                     */
    /* ---------------------------------------------------------------------- */
    /** Change the maximum number of unseen queued messages allowed in the 
     * specified queue.  A queued message is unseen if it is not being processed
     * by a worker thread.  The maximum number of messages is a soft limit that
     * schedulers use as a goal, but this goal can be exceeded under certain
     * conditions.  For instance, if all a queue's workers are busy and the 
     * queue's maximum number of unseen requests has been reached, the 
     * unexpected termination of a worker will cause the queuing system to
     * automatically requeue the terminated worker's request even if it exceed
     * the configured maximum. 
     * 
     * @param name the full name of the target queue
     * @param maxMessages 1 or more
     * @param tenantId the queue's tenantId
     * @return number of queues affected (0 or 1)
     * @throws JobQueueException input or operational error
     *  
     */
    public int updateMaxMessages(String name, int maxMessages, String tenantId)
     throws JobQueueException
    {
        return updateIntegerField(name, UpdateField.MAX_MESSAGES, maxMessages, tenantId);
    }
    
    /* ---------------------------------------------------------------------- */
    /* updateFilter:                                                          */
    /* ---------------------------------------------------------------------- */
    /** Replace the filter used to assign jobs to their queue with one specified
     * on this call.
     * 
     * @param name the full name of the target queue
     * @param filter the new filter
     * @param tenantId the queue's tenantId
     * @throws JobQueueException input or operational error
     */
    public int updateFilter(String name, String filter, String tenantId)
     throws JobQueueException
    {
        // ------------------------- Check Input -------------------------
        // Null/empty checks.
        if (StringUtils.isBlank(name)) {
            String msg = "Invalid job queue name received.";
            _log.error(msg);
            throw new JobQueueException(msg);
        }
        if (StringUtils.isBlank(tenantId)) {
            String msg = "Invalid job queue tenant ID received.";
            _log.error(msg);
            throw new JobQueueException(msg);
        }
        
        // Validate the filter.
        validateFilter(filter);
        
        // ------------------------- Call SQL ----------------------------
        int rows = 0;
        try
        {
            // Begin new transaction.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Create the delete command.
            String sql = "update job_queues " +
                         "set filter = :filter " +
                         "where name = :name and tenant_id = :tenant_id";
            
            // Fill in the placeholders.           
            Query qry = session.createSQLQuery(sql);
            qry.setString("name", name.trim());
            qry.setString("tenant_id", tenantId.trim());
            qry.setString("filter", filter.trim());
            
            // Issue the call.
            rows = qry.executeUpdate();
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){_log.error("Rollback failed.", e1);}
            
            String msg = "Unable to update " + name + ".filter" +
                          " in tenant " + tenantId + ".";
            _log.error(msg);
            throw new JobQueueException(msg, e);
        }
        finally {
            try {HibernateUtil.commitTransaction();} 
            catch (Exception e) 
            {
                String msg = "Unable to commit update transaction " +
                             "for " + name + ".filter" +  
                             " in tenant " + tenantId + ".";
                _log.error(msg);
                throw new JobQueueException(msg, e);
            }
        }
        
        return rows;
    }
    
    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* getQueue:                                                              */
    /* ---------------------------------------------------------------------- */
    /** Retrieve the specified queue object from the database.  The selector and
     * selectorValue parameters abstract away the differences between searching
     * by queue name versus queue uuid.
     * 
     * @param selector enum value that determines the search key (name or uuid)
     * @param selectorValue the search key value
     * @param tenantId the queue's tenantId
     * @return the complete queue record
     * @throws JobQueueException input or operational error
     */
    private JobQueue getQueue(Selector selector, String selectorValue, String tenantId) 
     throws JobQueueException
    {
        // ------------------------- Check Input -------------------------
        // Null/empty checks.
        if (StringUtils.isBlank(selectorValue)) {
            String msg = "Invalid job queue " + selector.toString() + " received.";
            _log.error(msg);
            throw new JobQueueException(msg);
        }
        if (StringUtils.isBlank(tenantId)) {
            String msg = "Invalid job queue tenant ID received.";
            _log.error(msg);
            throw new JobQueueException(msg);
        }
        
        // ------------------------- Call SQL ----------------------------
        JobQueue jobQueue = null;
        try
        {
            // Begin new transaction.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Create the insert command using table definition field order.
            // NOTE: Any changes to the job_queues table requires maintenance
            //       here and in the populate routine below.
            String sql = "select id, uuid, name, tenant_id, phase, priority, " +
                            "num_workers, max_messages, filter, created, last_updated " +
                         "from job_queues " +
                         "where " + selector.toString() + " = :selectorValue " +
                            "and tenant_id = :tenant_id";
            
            // Fill in the placeholders.           
            Query qry = session.createSQLQuery(sql);
            qry.setString("selectorValue", selectorValue.trim());
            qry.setString("tenant_id", tenantId.trim());
            
            // Issue the call and populate the model object.
            // (Yes, this is the tax for not using hibernate...)
            Object obj = qry.uniqueResult();
            jobQueue = populateJobQueue(obj);
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){_log.error("Rollback failed.", e1);}
            
            String msg = "Unable to query job queue with " + selector.toString() +
                          " " + selectorValue + " in tenant " + tenantId + ".";
            _log.error(msg);
            throw new JobQueueException(msg, e);
        }
        finally {
            try {HibernateUtil.commitTransaction();} 
            catch (Exception e) 
            {
                String msg = "Unable to commit query transaction " +
                             "for job queue with " + selector.toString() + 
                             " " + selectorValue + " in tenant " + tenantId + ".";
                _log.error(msg);
                throw new JobQueueException(msg, e);
            }
        }
        
        return jobQueue;
    }
    
    /* ---------------------------------------------------------------------- */
    /* DeleteQueue:                                                           */
    /* ---------------------------------------------------------------------- */
    /** Delete the specified queue definition from the database.  The selector 
     * and selectorValue parameters abstract away the differences between using
     * queue name versus queue uuid to specify the target queue.
     * 
     * @param selector enum value that determines the search key (name or uuid)
     * @param selectorValue the search key value
     * @param tenantId the queue's tenantId
     * @return the number of table rows affected (0 or 1)
     * @throws JobQueueException input or operational error
     */
    private int deleteQueue(Selector selector, String selectorValue, String tenantId) 
     throws JobQueueException
    {
        // ------------------------- Check Input -------------------------
        // Null/empty checks.
        if (StringUtils.isBlank(selectorValue)) {
            String msg = "Invalid job queue " + selector.toString() + " received.";
            _log.error(msg);
            throw new JobQueueException(msg);
        }
        if (StringUtils.isBlank(tenantId)) {
            String msg = "Invalid job queue tenant ID received.";
            _log.error(msg);
            throw new JobQueueException(msg);
        }
        
        // ------------------------- Call SQL ----------------------------
        int rows = 0;
        try
        {
            // Begin new transaction.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Create the delete command.
            String sql = "delete from job_queues " +
                         "where " + selector.toString() + " = :selectorValue " +
                            "and tenant_id = :tenant_id";
            
            // Fill in the placeholders.           
            Query qry = session.createSQLQuery(sql);
            qry.setString("selectorValue", selectorValue.trim());
            qry.setString("tenant_id", tenantId.trim());
            
            // Issue the call.
            rows = qry.executeUpdate();
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){_log.error("Rollback failed.", e1);}
            
            String msg = "Unable to delete job queue with " + selector.toString() +
                          " " + selectorValue + " in tenant " + tenantId + ".";
            _log.error(msg);
            throw new JobQueueException(msg, e);
        }
        finally {
            try {HibernateUtil.commitTransaction();} 
            catch (Exception e) 
            {
                String msg = "Unable to commit delete transaction " +
                             "for job queue with " + selector.toString() + 
                             " " + selectorValue + " in tenant " + tenantId + ".";
                _log.error(msg);
                throw new JobQueueException(msg, e);
            }
        }
        
        return rows;
    }
    
    /* ---------------------------------------------------------------------- */
    /* updateIntegerField:                                                    */
    /* ---------------------------------------------------------------------- */
    /** Update the designated integer field's value for the named queue.  The 
     * field/value pair specifies which field gets the updated value.
     * 
     * @param name the full name of the target queue
     * @param field the integer field to be updated
     * @param value the new integer value
     * @param tenantId the queue's tenantId
     * @return number of queues affected (0 or 1)
     * @throws JobQueueException input or operational error
     * @throws JobQueuePriorityException duplicate priority error
     */
    private int updateIntegerField(String name, UpdateField field, int value, 
                                    String tenantId) 
     throws JobQueueException, JobQueuePriorityException
    {
        // ------------------------- Check Input -------------------------
        // Null/empty checks.
        if (StringUtils.isBlank(name)) {
            String msg = "Invalid job queue name received.";
            _log.error(msg);
            throw new JobQueueException(msg);
        }
        if (StringUtils.isBlank(tenantId)) {
            String msg = "Invalid job queue tenant ID received.";
            _log.error(msg);
            throw new JobQueueException(msg);
        }
        
        // Make sure the value is in range.  It just so 
        // happens that all fields have the same range.
        if (value < 1) {
            String msg = "The requested " + field.toString() + " value (" +
                         value + ") must be at least 1.";
            _log.error(msg);
            throw new JobQueueException(msg);
        }
        
        // ------------------------- Call SQL ----------------------------
        int rows = 0;
        try
        {
            // Begin new transaction.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Create the delete command.
            String sql = "update job_queues " +
                         "set " + field.toString() + " = :value " +
                         "where name = :name and tenant_id = :tenant_id";
            
            // Fill in the placeholders.           
            Query qry = session.createSQLQuery(sql);
            qry.setString("name", name.trim());
            qry.setString("tenant_id", tenantId.trim());
            qry.setInteger("value", value);
            
            // Issue the call.
            rows = qry.executeUpdate();
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){_log.error("Rollback failed.", e1);}
            
            // Detect duplicate priority error.
            if ((field == UpdateField.PRIORITY) && (e instanceof ConstraintViolationException))
            {
                // Create a custom message and exception for duplicate priorities.
                String[] components = name.split("[.]");
                String stage = "[unknown]";
                if (components.length > 0) stage = components[0];
                String msg = "A priority of value " + value + 
                             " is already assigned for a " + stage + " queue in tenant " + tenantId + 
                             ". Please choose unique priorities for queues in the same stage.";
                _log.error(msg);
                throw new JobQueuePriorityException(msg, e);
            }
            
            // General exception.
            String msg = "Unable to update " + name + "." + field.toString() +
                          " to value " + value + " in tenant " + tenantId + ".";
            _log.error(msg);
            throw new JobQueueException(msg, e);
        }
        finally {
            try {HibernateUtil.commitTransaction();} 
            catch (Exception e) 
            {
                String msg = "Unable to commit update transaction " +
                             "for " + name + "." + field.toString() + 
                             " in tenant " + tenantId + ".";
                _log.error(msg);
                throw new JobQueueException(msg, e);
            }
        }
        
        return rows;
    }
    
    /* ---------------------------------------------------------------------- */
    /* populateJobQueue:                                                      */
    /* ---------------------------------------------------------------------- */
    /** Populate a new JobQueue object with a record retrieved from the 
     * job_queues table.
     * 
     * NOTE: This method must be manually maintained whenever the job_queues
     *       table schema changes.  
     * 
     * @param obj a record from the job_queues table
     * @return a new model object
     */
    private JobQueue populateJobQueue(Object obj)
    {
        // Don't blow up.
        if (obj == null) return null;
        Object[] array = (Object[]) obj;
        
        // Populate the queue object using table definition field order,
        // which is the order specified in all calling methods.
        JobQueue jobQueue = new JobQueue();
        jobQueue.setId(((BigInteger)array[0]).longValue());
        jobQueue.setUuid((String) array[1]);
        jobQueue.setName((String) array[2]);
        jobQueue.setTenantId((String) array[3]);
        jobQueue.setPhase(JobPhaseType.valueOf((String)array[4]));
        jobQueue.setPriority(((Integer)array[5]).intValue());
        jobQueue.setNumWorkers(((Integer)array[6]).intValue());
        jobQueue.setMaxMessages(((Integer)array[7]).intValue());
        jobQueue.setFilter((String) array[8]);
        
        // Waiting for Java 8 time functions...
        Timestamp createdTS = (Timestamp)array[9];
        jobQueue.setCreated(new Date(createdTS.getTime()));
        Timestamp updatedTS = (Timestamp)array[10];
        jobQueue.setLastUpdated(new Date(updatedTS.getTime()));
        
        return jobQueue;
    }
    
    /* ---------------------------------------------------------------------- */
    /* validateFilter:                                                        */
    /* ---------------------------------------------------------------------- */
    /** Attempt to parse the (non-empty) filter.  Parser errors are contained
     * in the thrown exception if things go wrong.
     * 
     * @param filter the text of a SQL92-like filter.
     */
    private void validateFilter(String filter)
     throws JobQueueFilterException
    {
        // Make sure 
        if (StringUtils.isBlank(filter)) {
            String msg = "Empty job queue filter received.";
            _log.error(msg);
            throw new JobQueueFilterException(msg);
        }
        
        // Try to parse the filter.
        SelectorFilter.parse(filter);
    }
}
