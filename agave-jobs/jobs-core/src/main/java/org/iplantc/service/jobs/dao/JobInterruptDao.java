package org.iplantc.service.jobs.dao;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.CacheMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobQueueException;
import org.iplantc.service.jobs.model.JobInterrupt;
import org.iplantc.service.jobs.model.enumerations.JobInterruptType;
import org.joda.time.DateTime;


/** Data access object for the job_interrupts table.
 * 
 * This class issues native SQL commands through Hibernate session objects.
 * See the JobQueue class for the rationale for not using Hibernate more
 * extensively.
 * 
 * Concurrency 
 * ===========
 * 
 * By definition interrupts occur asynchronous to job processing.  A job can
 * be in any state when an interrupt occurs.  A job's state can be defined
 * by these characteristics: 
 *
 *  1. In a finished, trigger or other status
 *  2. Being or not being processed by a scheduler
 *  3. In or not in a scheduler queue
 *  4. Processing or not processing in a worker thread
 *  5. Interrupted or not interrupted
 * 
 * Not all combinations of the above characteristics will occur.  By interrupted,
 * we mean an interrupt record for the job is in the job_interrupts table.  Workers
 * poll this table to check if the job they are processing has been interrupted.
 * Currently, the only interrupt types are DELETE, PAUSE, and STOP, all of which
 * simply cause the worker thread to discontinue processing its job.  
 * 
 * The originator of an interrupt is responsible for changing the job status
 * before sending an interrupt message.  The sequence is (1) the interrupter updates
 * the job status, (2) the interrupter sends an interrupt message to the topic queue, 
 * (3) the topic queue thread reads the message, (4) the topic queue thread writes
 * an interrupt record to the interrupt table, and (5) worker threads poll the
 * interrupt table for interrupts for their current job. (See TopicMessageSender 
 * and the topic queue support in AbstractPhaseScheduler for details on interrupt 
 * message processing.)
 * 
 * When an interrupt is sent to a finished job that is not being processed by in
 * a worker, the interrupt record is never read by a worker thead and it gets cleaned 
 * up when it expires. 
 * 
 * When an interrupt is sent to a job that is in a trigger status, but the job has not
 * yet been scheduled, the interrupt will be read by the worker that picks up the
 * job after a scheduler schedules it (i.e., puts the job on the phase's scheduler queue).
 * 
 * When an interrupt is sent to a job currently being processed by a worker, the
 * worker will periodically poll the interrupt table for its job and process
 * interrupts (in chronological order) when they are found.
 * 
 * Rollback
 * --------
 * The above interrupt mechanism is relatively straightforward if jobs progress
 * in a linear fashion from one phase to the next, or from one status to the 
 * next.  Unfortunately, jobs can be rolled back to a prior status and even a 
 * prior phase.  This cyclical behavior complicates interrupt processing.
 * 
 * Consider a job that proceeds from phase A to a later phase B, for example from 
 * Staging to Monitoring.  The job itself may or may not be on phase B's queue
 * and may or may not be processing in a B worker thread.  For this scenario, let's
 * assume a B worker is processing the job.  
 * 
 * If a user or a zombie detector wants to rollback the job, it first updates the 
 * job's status to Stopped and then sends a STOP message to the topic queue as 
 * described above.  After the message is ASYNCHRONOUSLY read from the topic queue and
 * written to the interrupt table, the goal is for worker B to eventually abandon the
 * job.  In the meantime, the thread initiating the rollback updates the status of the 
 * job from Stopped to RollingBack. 
 * 
 * Since schedulers, workers and interrupters run concurrently on different threads, 
 * it is indeterminant when a worker thread actually sees an interrupt relative
 * to the processing of other threads .  In particular, once a job is assigned 
 * the RollingBack status, the RollingBackScheduler can schedule it and a worker 
 * can actually perform the rollback to a prior status.  
 * 
 * Going back to our example, it's possible that the status update to a prior status,
 * say to a phase A trigger status, takes place BEFORE the worker in phase B receives 
 * the interrupt to stop processing.  As a matter of fact, its possible that a phase
 * A worker begins processing the job while worker B is still processing it.
 * 
 * In this situation, we have two workers processing the same job, which breaks the 
 * fundamental design requirement of our design.  There's a race condition on which worker 
 * gets the interrupt, and bad things will happen if worker A is interrupted since worker B 
 * will continue doing the wrong work under the wrong status.
 * 
 * Epochs
 * ------
 * The remedy for dealing with rollback complexities is to implement the concept of
 * jobs running in an "epoch", starting with epoch 0.  Everytime a rollback is initiated
 * the job's epoch is incremented.  Interrupts target jobs in specific epochs, which 
 * avoids the race condition identified above.  Here's how epochs are used to implement
 * safe rollbacks.
 * 
 * Consider again the above job that has progressed from phase A to phase B and a B  
 * worker is processing the job.  Assume a rollback is initiated and the pathological case
 * described above occurs such that the job is being processed concurrently by an A and
 * a B worker.  Worker A, however, will be executing the job in epoch 1, while work B
 * will be in epoch 0.  The interrupt to stop processing the job targeted the job in epoch 0, 
 * so only worker B will get the interrupt.  When it does, it will stop processing the job, 
 * leaving only worker A executing.  
 * 
 * The overlap period where there are two workers processing the same job can be 
 * problematic if worker B changes the state.  Our last defense is the state machine 
 * rejecting undefined status transitions.  Even with epochs and state machine validation, 
 * however, it is possible that a roll back could put a job in an inconsistent state.  The 
 * best guidance, therefore, is to avoid rolling back jobs that have active worker threads. 
 * 
 * See JobManager.rollback() and JobDao.rollback() for implementation details.
 * 
 * @author rcardone
 */
public final class JobInterruptDao {
    
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(JobInterruptDao.class);
    
    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* createInterrupt:                                                       */
    /* ---------------------------------------------------------------------- */
    /** Define a new job interrupt row in the database.
     * 
     * @return the number of database row affected (0 or 1).
     * @throws JobException input or operational error
     */
    public static int createInterrupt(JobInterrupt interrupt) 
     throws JobException
    {
        // ------------------------- Check Input -------------------------
        // Null/empty checks.
        if (interrupt == null) {
            String msg = "Null job interrupt object received.";
            _log.error(msg);
            throw new JobException(msg);
        }
        if (StringUtils.isBlank(interrupt.getJobUuid())) {
            String msg = "No job uuid specified in job interrupt definition.";
            _log.error(msg);
            throw new JobException(msg);
        }
        if (StringUtils.isBlank(interrupt.getTenantId())) {
            String msg = "No tenant id specified in job interrupt definition.";
            _log.error(msg);
            throw new JobException(msg);
        }
        if (interrupt.getInterruptType() == null) {
            String msg = "No interrupt type specified in job interrtupt definition.";
            _log.error(msg);
            throw new JobException(msg);
        }
        if (interrupt.getEpoch() < 0) {
            String msg = "Negative epoch value specified in job interrtupt definition.";
            _log.error(msg);
            throw new JobException(msg);
        }
                                    
        // ------------------------- Permission Checks -------------------
        // We only allow users to create interrupts under their own tenant. 
        String currentTenantId = TenancyHelper.getCurrentTenantId();
        if (StringUtils.isBlank(currentTenantId)) {
            String msg = "Unable to retrieve current tenant id.";
            _log.error(msg);
            throw new JobException(msg);
        }
        if (!interrupt.getTenantId().equals(currentTenantId)) {
            String msg = "Unable to create job interrupt for job " + interrupt.getJobUuid() + 
                         " because the current tenant id (" + currentTenantId + ") does not match " +
                         "the job interrupt's tenant id (" + interrupt.getTenantId()+ ").";
            _log.error(msg);
            throw new JobException(msg);
        }
        
        // ------------------------- Complete Input ----------------------
        // Set the interrupt created date to the current time.
        Date curDate = new Date();
        interrupt.setCreated(curDate);
        
        // Add the interrupt time-to-live duration to the 
        // current time and use it as the expiration date.
        DateTime dt = new DateTime(curDate.getTime());
        dt = dt.plusSeconds(JobInterrupt.JOB_INTERRUPT_TTL_SECONDS);
        interrupt.setExpiresAt(dt.toDate());
        
        // ------------------------- Call SQL ----------------------------
        int rows = 0; // rows affected
        try
        {
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Create the insert command.
            String sql = "insert into job_interrupts " +
                         "(job_uuid, tenant_id, epoch, interrupt_type, created, expires_at) " +
                         "values " +
                         "(:job_uuid, :tenant_id, :epoch, :interrupt_type, :created, :expires_at) ";
            
            // Fill in the placeholders.           
            Query qry = session.createSQLQuery(sql);
            qry.setString("job_uuid", interrupt.getJobUuid().trim());
            qry.setString("tenant_id", interrupt.getTenantId());
            qry.setInteger("epoch", interrupt.getEpoch());
            qry.setString("interrupt_type", interrupt.getInterruptType().name());
            qry.setTimestamp("created", interrupt.getCreated());
            qry.setTimestamp("expires_at", interrupt.getExpiresAt());
            qry.setCacheable(false);
            qry.setCacheMode(CacheMode.IGNORE);
            
            // Issue the call.
            rows = qry.executeUpdate();
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){_log.error("Rollback failed.", e1);}
            
            String msg = "Unable to create new job interrupt: \n" + interrupt.toString();
            _log.error(msg);
            throw new JobException(msg, e);
        }
        finally {
            try {HibernateUtil.commitTransaction();} 
            catch (Exception e) 
            {
                String msg = "Unable to commit job interrupt create transaction.";
                _log.error(msg);
                throw new JobException(msg, e);
            }
        }
        
        // Return the number of rows affected.
        return rows;
    }

    /* ---------------------------------------------------------------------- */
    /* getInterrupts:                                                         */
    /* ---------------------------------------------------------------------- */
    /** Retrieve all interrupt objects for a job ordered by ascending 
     * created time.
     * 
     * @param jobUuid the job uuid
     * @param tenantId the job's tenantId
     * @return the list of interrupts ordered by creation time
     * @throws JobException input or operational error
     */
    public static List<JobInterrupt> getInterrupts(String jobUuid, String tenantId) 
     throws JobException
    {
        // ------------------------- Check Input -------------------------
        if (StringUtils.isBlank(jobUuid)) {
            String msg = "No job uuid specified for job interrupt query.";
            _log.error(msg);
            throw new JobException(msg);
        }
        if (StringUtils.isBlank(tenantId)) {
            String msg = "No tenant id specified in job interrupt query.";
            _log.error(msg);
            throw new JobException(msg);
        }
        
        // ------------------------- Permission Checks -------------------
        // We only allow users to create interrupts under their own tenant. 
        String currentTenantId = TenancyHelper.getCurrentTenantId();
        if (StringUtils.isBlank(currentTenantId)) {
            String msg = "Unable to retrieve current tenant id.";
            _log.error(msg);
            throw new JobException(msg);
        }
        if (!currentTenantId.equals(tenantId)) {
            String msg = "Unable to query interrupts " + 
                         "because the current tenant id (" + currentTenantId + 
                         ") does not match the query tenant id (" + tenantId + ").";
            _log.error(msg);
            throw new JobException(msg);
        }
        
        // ------------------------- Call SQL ----------------------------
        ArrayList<JobInterrupt> list = new ArrayList<>();
        try
        {
            // Begin new transaction.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Create the insert command using table definition field order.
            // NOTE: Any changes to the job_interrupts table requires maintenance
            //       here and in the populate routine below.
            String sql = "select id, job_uuid, tenant_id, epoch, interrupt_type, created, expires_at " +
                         "from job_interrupts " +
                         "where job_uuid = :job_uuid " +
                         "and tenant_id = :tenant_id " +
                         "order by created";
            
            // Fill in the placeholders.           
            Query qry = session.createSQLQuery(sql);
            qry.setString("job_uuid", jobUuid);
            qry.setString("tenant_id", tenantId);
            qry.setCacheable(false);
            qry.setCacheMode(CacheMode.IGNORE);
            
            // Issue the call and populate the model object.
            // (Yes, this is the tax for not using hibernate...)
            @SuppressWarnings("unchecked")
            List<Object> objList = qry.list();
            for (Object obj : objList) list.add(populateJobInterrupt(obj));
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){_log.error("Rollback failed.", e1);}
            
            String msg = "Unable to query job interrupts for tenant " + tenantId + ".";
            _log.error(msg);
            throw new JobQueueException(msg, e);
        }
        finally {
            try {HibernateUtil.commitTransaction();} 
            catch (Exception e) 
            {
                String msg = "Unable to commit query transaction " +
                             "for job interrupts for tenant " + tenantId + ".";
                _log.error(msg);
                throw new JobQueueException(msg, e);
            }
        }
        
        return list;
    }
    
    /* ---------------------------------------------------------------------- */
    /* deleteInterrupt:                                                       */
    /* ---------------------------------------------------------------------- */
    /** Delete the interrupt with the specified unique sequence number.  The
     * current user's tenant id must match the tenant id of the interrupt for
     * the delete to succeed.
     * 
     * @param id the sequence number of the interrupt record
     * @param tenantId the tenant id of the current user and the interrupt
     * @return the number of row affected (0 or 1)
     */
    public static int deleteInterrupt(long id, String tenantId)
     throws JobException
    {
        // ------------------------- Check Input -------------------------
        if (StringUtils.isBlank(tenantId)) {
            String msg = "No tenant id specified on job interrupt deletion.";
            _log.error(msg);
            throw new JobException(msg);
        }
        
        // ------------------------- Permission Checks -------------------
        // We only allow users to create interrupts under their own tenant. 
        String currentTenantId = TenancyHelper.getCurrentTenantId();
        if (StringUtils.isBlank(currentTenantId)) {
            String msg = "Unable to retrieve current tenant id.";
            _log.error(msg);
            throw new JobException(msg);
        }
        if (!currentTenantId.equals(tenantId)) {
            String msg = "Unable to delete interrupts " + 
                         "because the current tenant id (" + currentTenantId + 
                         ") does not match the deletion tenant id (" + tenantId + ").";
            _log.error(msg);
            throw new JobException(msg);
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
            String sql = "delete from job_interrupts " +
                         "where id = :id " +
                         "and tenant_id = :tenant_id";
            
            // Fill in the placeholders.           
            Query qry = session.createSQLQuery(sql);
            qry.setLong("id", id);
            qry.setString("tenant_id", tenantId);
            qry.setCacheable(false);
            qry.setCacheMode(CacheMode.IGNORE);
            
            // Issue the call.
            rows = qry.executeUpdate();
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){_log.error("Rollback failed.", e1);}
            
            String msg = "Unable to delete job interrupt id = " + id +
                         " for tenant " + tenantId + ".";
            _log.error(msg);
            throw new JobQueueException(msg, e);
        }
        finally {
            try {HibernateUtil.commitTransaction();} 
            catch (Exception e) 
            {
                String msg = "Unable to commit delete transaction " +
                             "for job interrupt with id = " + id + 
                             " for tenant " + tenantId + ".";
                _log.error(msg);
                throw new JobQueueException(msg, e);
            }
        }
        
        return rows;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getInterrupts:                                                         */
    /* ---------------------------------------------------------------------- */
    /** Retrieve all interrupt objects for all tenants ordered by ascending 
     * expiration time.  This method should only be called by scheduler threads
     * or test programs that don't disclose tenant information improperly.
     * 
     *      **** This is a cross-tenant call ****
     * 
     * @return the list of interrupts ordered by expiration time
     * @throws JobException input or operational error
     */
    public static List<JobInterrupt> getInterrupts() 
     throws JobException
    {
        // ------------------------- Call SQL ----------------------------
        ArrayList<JobInterrupt> list = new ArrayList<>();
        try
        {
            // Begin new transaction.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Create the insert command using table definition field order.
            // NOTE: Any changes to the job_interrupts table requires maintenance
            //       here and in the populate routine below.
            String sql = "select id, job_uuid, tenant_id, epoch, interrupt_type, created, expires_at " +
                         "from job_interrupts " +
                         "order by expires_at";
            
            // Fill in the placeholders.           
            Query qry = session.createSQLQuery(sql);
            qry.setCacheable(false);
            qry.setCacheMode(CacheMode.IGNORE);
            
            // Issue the call and populate the model object.
            // (Yes, this is the tax for not using hibernate...)
            @SuppressWarnings("unchecked")
            List<Object> objList = qry.list();
            for (Object obj : objList) list.add(populateJobInterrupt(obj));
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){_log.error("Rollback failed.", e1);}
            
            String msg = "Unable to query job interrupts.";
            _log.error(msg);
            throw new JobQueueException(msg, e);
        }
        finally {
            try {HibernateUtil.commitTransaction();} 
            catch (Exception e) 
            {
                String msg = "Unable to commit query transaction for job interrupts.";
                _log.error(msg);
                throw new JobQueueException(msg, e);
            }
        }
        
        return list;
    }
    
    /* ---------------------------------------------------------------------- */
    /* deleteExpiredInterrupts:                                               */
    /* ---------------------------------------------------------------------- */
    /** Delete all interrupts whose expiration time has passed.  The expired
     * interrupt records are logged before being deleted.  This method should
     * only be called by a clean up thread in the scheduler and never as the
     * direct or indirect result of user action.
     * 
     *      **** This is a cross-tenant call ****
     * 
     * @return the number of rows deleted.
     * @throws JobException 
     */
    public static int deleteExpiredInterrupts() throws JobException
    {
        // ------------------------- Call SQL ----------------------------
        ArrayList<JobInterrupt> list = new ArrayList<>();
        int totalRowsDeleted = 0;
        try
        {
            // Begin new transaction.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();
            
            // -------- Select For Update
            // Issue the select for update query to lock the expired records.
            // NOTE: Any changes to the job_interrupts table requires maintenance
            //       here and in the populate routine below.
            String sql = "select id, job_uuid, tenant_id, epoch, interrupt_type, created, expires_at " +
                         "from job_interrupts " +
                         "where expires_at < :curTS "  +
                         "FOR UPDATE";
            
            // Get current timestamp.
            Date curTS = new Date();

            // Fill in the placeholders.           
            Query qry = session.createSQLQuery(sql);
            qry.setTimestamp("curTS", curTS);
            qry.setCacheable(false);
            qry.setCacheMode(CacheMode.IGNORE);
            
            // Issue the call and populate the model object.
            // (Yes, this is the tax for not using hibernate...)
            @SuppressWarnings("unchecked")
            List<Object> objList = qry.list();
            for (Object obj : objList) list.add(populateJobInterrupt(obj));
            
            // -------- Delete Locked Records
            // Delete each expired interrupt and log the occurrence.
            // We only try to delete locked interrupts.
            for (JobInterrupt interrupt : list) {
                sql = "delete from job_interrupts where id = :id";
                qry = session.createSQLQuery(sql);
                qry.setLong("id", interrupt.getId());
                
                // Issue the call.
                int rows = qry.executeUpdate();
                
                // Log the outcome.
                String msg;
                if (rows == 0) msg = "Unable to delete expired interrupt: " + interrupt.toJSON();
                  else msg = "Deleted expired interrupt: " + interrupt.toJSON();
                _log.warn(msg);
                
                // Possibly increment the deletion count.
                totalRowsDeleted += rows;
            }
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){_log.error("Rollback failed.", e1);}
            
            String msg = "Unable to delete expired job interrupts.";
            _log.error(msg);
            throw new JobException(msg, e);
        }
        finally {
            try {HibernateUtil.commitTransaction();} 
            catch (Exception e) 
            {
                String msg = "Unable to commit deletion transaction " +
                             "for job interrupts.";
                _log.error(msg);
                throw new JobException(msg, e);
            }
        }
        
        return totalRowsDeleted;
    }

    /* ---------------------------------------------------------------------- */
    /* updateExpiresAtDate:                                                   */
    /* ---------------------------------------------------------------------- */
    /** Update the expiration date for a specific interrupt.
     * 
     * @param id the sequence number of the interrupt record
     * @param tenantId the job's tenantId
     * @param expiresAt the new expiration date and time
     * @return the list of interrupts ordered by creation time
     * @throws JobException input or operational error
     */
    public static int updateExpiresAtDate(long id, String tenantId, Date expiresAt) 
     throws JobException
    {
        // ------------------------- Check Input -------------------------
        if (StringUtils.isBlank(tenantId)) {
            String msg = "No tenant id specified in job update call.";
            _log.error(msg);
            throw new JobException(msg);
        }
        if (expiresAt == null) {
            String msg = "No expiration date specified in job update call.";
            _log.error(msg);
            throw new JobException(msg);
        }
        
        // ------------------------- Permission Checks -------------------
        // We only allow users to create interrupts under their own tenant. 
        String currentTenantId = TenancyHelper.getCurrentTenantId();
        if (StringUtils.isBlank(currentTenantId)) {
            String msg = "Unable to retrieve current tenant id.";
            _log.error(msg);
            throw new JobException(msg);
        }
        if (!currentTenantId.equals(tenantId)) {
            String msg = "Unable to query interrupts " + 
                         "because the current tenant id (" + currentTenantId + 
                         ") does not match the query tenant id (" + tenantId + ").";
            _log.error(msg);
            throw new JobException(msg);
        }
        
        // ------------------------- Call SQL ----------------------------
        int rows = 0;
        try
        {
            // Begin new transaction.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Create the update command using table definition field order.
            String sql = "update job_interrupts set expires_at = :expires_at " +
                         "where id = :id " +
                         "and tenant_id = :tenant_id";
            
            // Fill in the placeholders.           
            Query qry = session.createSQLQuery(sql);
            qry.setTimestamp("expires_at", expiresAt);
            qry.setLong("id", id);
            qry.setString("tenant_id", tenantId);
            qry.setCacheable(false);
            qry.setCacheMode(CacheMode.IGNORE);
            
            // Issue the call.
            rows = qry.executeUpdate();
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){_log.error("Rollback failed.", e1);}
            
            String msg = "Unable to update expiration date for interrupt " +
                         id + " and tenant " + tenantId + ".";
            _log.error(msg);
            throw new JobQueueException(msg, e);
        }
        finally {
            try {HibernateUtil.commitTransaction();} 
            catch (Exception e) 
            {
                String msg = "Unable to commit update expiration data transaction " +
                             "for interrupt " + id + ", tenant " + tenantId + ".";
                _log.error(msg);
                throw new JobQueueException(msg, e);
            }
        }
        
        return rows;
    }
    
    /* ---------------------------------------------------------------------- */
    /* clearInterrupts:                                                       */
    /* ---------------------------------------------------------------------- */
    /** This is an administrative command useful in testing but not likely to
     * be used in production.  This command deletes all rows from the interrupts 
     * table.
     * 
     * @return number of rows affected.
     */
    public static int clearInterrupts()
    {
        // Return value.
        int rows = 0;
        
        // Dump the complete table..
        try {
            // Get a hibernate session.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Release the lease.
            String sql = "delete from job_interrupts";
            Query qry = session.createSQLQuery(sql);
            qry.setCacheable(false);
            qry.setCacheMode(CacheMode.IGNORE);
            rows = qry.executeUpdate();
            
            // Commit the transaction.
            HibernateUtil.commitTransaction();
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){_log.error("Rollback failed.", e1);}
            
            String msg = "Unable to clear all interrupts.";
            _log.error(msg, e);
        }
        
        return rows;
    }
    
    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* populateJobInterrupt:                                                  */
    /* ---------------------------------------------------------------------- */
    /** Populate a new JobInterrupt object with a record retrieved from the 
     * job_interrupts table.
     * 
     * NOTE: This method must be manually maintained whenever the job_interrupts
     *       table schema changes.  
     * 
     * @param obj a record from the job_interrupts table
     * @return a new model object
     */
    private static JobInterrupt populateJobInterrupt(Object obj)
    {
        // Don't blow up.
        if (obj == null) return null;
        Object[] array = (Object[]) obj;

        // Populate the queue object using table definition field order,
        // which is the order specified in all calling methods.
        JobInterrupt jobInterrupt = new JobInterrupt();
        jobInterrupt.setId(((BigInteger)array[0]).longValue());
        jobInterrupt.setJobUuid((String) array[1]);
        jobInterrupt.setTenantId((String) array[2]);
        jobInterrupt.setEpoch((Integer) array[3]);
        jobInterrupt.setInterruptType((JobInterruptType.valueOf((String)array[4])));
        
        // Waiting for Java 8 time functions...
        Timestamp createdTS = (Timestamp)array[5];
        jobInterrupt.setCreated(new Date(createdTS.getTime()));
        Timestamp expiresTS = (Timestamp)array[6];
        jobInterrupt.setExpiresAt(new Date(expiresTS.getTime()));
        
        return jobInterrupt;
    }
}
