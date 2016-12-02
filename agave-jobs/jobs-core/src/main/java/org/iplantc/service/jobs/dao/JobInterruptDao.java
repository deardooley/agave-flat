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
                         "(job_uuid, tenant_id, interrupt_type, created, expires_at) " +
                         "values " +
                         "(:job_uuid, :tenant_id, :interrupt_type, :created, :expires_at) ";
            
            // Fill in the placeholders.           
            Query qry = session.createSQLQuery(sql);
            qry.setString("job_uuid", interrupt.getJobUuid().trim());
            qry.setString("tenant_id", interrupt.getTenantId());
            qry.setString("interrupt_type", interrupt.getInterruptType().name());
            qry.setTimestamp("created", interrupt.getCreated());
            qry.setTimestamp("expires_at", interrupt.getExpiresAt());
            
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
            String sql = "select id, job_uuid, tenant_id, interrupt_type, created, expires_at " +
                         "from job_interrupts " +
                         "where job_uuid = :job_uuid " +
                         "and tenant_id = :tenant_id " +
                         "order by created";
            
            // Fill in the placeholders.           
            Query qry = session.createSQLQuery(sql);
            qry.setString("job_uuid", jobUuid);
            qry.setString("tenant_id", tenantId);
            
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
            String sql = "select id, job_uuid, tenant_id, interrupt_type, created, expires_at " +
                         "from job_interrupts " +
                         "order by expires_at";
            
            // Fill in the placeholders.           
            Query qry = session.createSQLQuery(sql);
            
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
            String sql = "select id, job_uuid, tenant_id, interrupt_type, created, expires_at " +
                         "from job_interrupts " +
                         "where expires_at < :curTS "  +
                         "FOR UPDATE";
            
            // Get current timestamp.
            Date curTS = new Date();

            // Fill in the placeholders.           
            Query qry = session.createSQLQuery(sql);
            qry.setTimestamp("curTS", curTS);
            
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
        jobInterrupt.setInterruptType((JobInterruptType.valueOf((String)array[3])));
        
        // Waiting for Java 8 time functions...
        Timestamp createdTS = (Timestamp)array[4];
        jobInterrupt.setCreated(new Date(createdTS.getTime()));
        Timestamp expiresTS = (Timestamp)array[5];
        jobInterrupt.setExpiresAt(new Date(expiresTS.getTime()));
        
        return jobInterrupt;
    }
}
