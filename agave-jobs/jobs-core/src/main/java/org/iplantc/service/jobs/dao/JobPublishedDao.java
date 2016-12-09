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
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobQueueException;
import org.iplantc.service.jobs.model.JobPublished;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;


/** Data access object for the job_interrupts table.
 * 
 * This class issues native SQL commands through Hibernate session objects.
 * See the JobQueue class for the rationale for not using Hibernate more
 * extensively. 
 * 
 * @author rcardone
 */
public final class JobPublishedDao {
    
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(JobPublishedDao.class);
    
    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* publish:                                                               */
    /* ---------------------------------------------------------------------- */
    /** Insert a new job publish row in the database.
     * 
     * @return the number of database row affected (0 or 1).
     * @throws JobException input or operational error
     */
    public static int publish(JobPhaseType phase, String jobUuid, String creator) 
     throws JobException
    {
        // ------------------------- Check Input -------------------------
        // Null/empty checks.
        if (phase == null) {
            String msg = "Null job phase received.";
            _log.error(msg);
            throw new JobException(msg);
        }
        if (StringUtils.isBlank(jobUuid)) {
            String msg = "No job uuid specified.";
            _log.error(msg);
            throw new JobException(msg);
        }
        if (StringUtils.isBlank(creator)) {
            String msg = "No creator specified.";
            _log.error(msg);
            throw new JobException(msg);
        }
                                    
        // ------------------------- Call SQL ----------------------------
        int rows = 0; // rows affected
        try
        {
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Create the insert command.
            String sql = "insert into job_published " +
                         "(phase, job_uuid, created, creator) " +
                         "values " +
                         "(:phase, :job_uuid, :created, :creator) ";
            
            // Fill in the placeholders.           
            Query qry = session.createSQLQuery(sql);
            qry.setString("phase", phase.name());
            qry.setString("job_uuid", jobUuid);
            qry.setTimestamp("created", new Date());
            qry.setString("creator", creator);
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
            
            String msg = "Unable to insert new job published record for phase " + 
                         phase.name() + " and job " + jobUuid + ".";
            _log.error(msg);
            throw new JobException(msg, e);
        }
        finally {
            try {HibernateUtil.commitTransaction();} 
            catch (Exception e) 
            {
                String msg = "Unable to commit new job published record for phase " + 
                             phase.name() + " and job " + jobUuid + ".";
                _log.error(msg);
                throw new JobException(msg, e);
            }
        }
        
        // Return the number of rows affected.
        return rows;
    }

    /* ---------------------------------------------------------------------- */
    /* hasPublishedJob:                                                       */
    /* ---------------------------------------------------------------------- */
    /** Determine if a job exists in the published table in the specified phase.
     * 
     * @param phase the phase in which the job is published
     * @param jobUuid the job id
     * @return true if the job exists in the phase, false otherwise
     * @throws JobException on error
     */
    public static boolean hasPublishedJob(JobPhaseType phase, String jobUuid)
     throws JobException
    {
        // ------------------------- Check Input -------------------------
        // Null/empty checks.
        if (phase == null) {
            String msg = "Null job phase received.";
            _log.error(msg);
            throw new JobException(msg);
        }
        if (StringUtils.isBlank(jobUuid)) {
            String msg = "No job uuid specified.";
            _log.error(msg);
            throw new JobException(msg);
        }
        
        // ------------------------- Call SQL ----------------------------
        boolean exists = true; // assume job in table
        try
        {
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Create the insert command.
            String sql = "select count(*) from job_published " +
                         "where phase = :phase " +
                         "and job_uuid = :job_uuid";
            
            // Fill in the placeholders.           
            Query qry = session.createSQLQuery(sql);
            qry.setString("phase", phase.name());
            qry.setString("job_uuid", jobUuid);
            qry.setCacheable(false);
            qry.setCacheMode(CacheMode.IGNORE);
            
            // Issue the call.
            Object result = qry.uniqueResult();
            if (result != null) {
                int count = ((BigInteger)result).intValue();
                if (count == 0) exists = false;
            }
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){_log.error("Rollback failed.", e1);}
            
            String msg = "Unable to query existence of job published for phase " + 
                         phase.name() + " and job " + jobUuid + ".";
            _log.error(msg);
            throw new JobException(msg, e);
        }
        finally {
            try {HibernateUtil.commitTransaction();} 
            catch (Exception e) 
            {
                String msg = "Unable to commit published job existence query for phase " + 
                             phase.name() + " and job " + jobUuid + ".";
                _log.error(msg);
                throw new JobException(msg, e);
            }
        }
        
        // Return the number of rows affected.
        return exists;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getPublishedJobs:                                                      */
    /* ---------------------------------------------------------------------- */
    /** Get all job published for the specified phase.
     * 
     * @param phase the phase whose published jobs are of interest
     * @return all cudrrently published job for the named phase
     * @throws JobException on error
     */
    public static List<JobPublished> getPublishedJobs(JobPhaseType phase)
     throws JobException
    {
        // ------------------------- Check Input -------------------------
        // Null/empty checks.
        if (phase == null) {
            String msg = "Null job phase received.";
            _log.error(msg);
            throw new JobException(msg);
        }
        
        // ------------------------- Call SQL ----------------------------
        ArrayList<JobPublished> list = new ArrayList<>();
        try
        {
            // Begin new transaction.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Create the insert command using table definition field order.
            // NOTE: Any changes to the job_interrupts table requires maintenance
            //       here and in the populate routine below.
            String sql = "select phase, job_uuid, created, creator " +
                         "from job_published " +
                         "where phase = :phase " +
                         "order by job_uuid";
            
            // Fill in the placeholders.           
            Query qry = session.createSQLQuery(sql);
            qry.setString("phase", phase.name());
            qry.setCacheable(false);
            qry.setCacheMode(CacheMode.IGNORE);
            
            // Issue the call and populate the model object.
            // (Yes, this is the tax for not using hibernate...)
            @SuppressWarnings("unchecked")
            List<Object> objList = qry.list();
            for (Object obj : objList) list.add(populateJobPublished(obj));
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){_log.error("Rollback failed.", e1);}
            
            String msg = "Unable to retrieve jobs published records for phase " + 
                         phase.name() + ".";
            _log.error(msg);
            throw new JobQueueException(msg, e);
        }
        finally {
            try {HibernateUtil.commitTransaction();} 
            catch (Exception e) 
            {
                String msg = "Unable to commit selection of jobs published records for phase " + 
                             phase.name() + ".";
                _log.error(msg);
                throw new JobQueueException(msg, e);
            }
        }
        
        return list;
    }
    
    /* ---------------------------------------------------------------------- */
    /* deletePublishedJob:                                                    */
    /* ---------------------------------------------------------------------- */
    /** Delete specific published job for the name phase.
     * 
     * @param phase the phase in which the job was published
     * @param jobUuid the job id
     * @return the number of records affect (0 or 1)
     * @throws JobException on error
     */
    public static int deletePublishedJob(JobPhaseType phase, String jobUuid)
     throws JobException
    {
        // ------------------------- Check Input -------------------------
        // Null/empty checks.
        if (phase == null) {
            String msg = "Null job phase received.";
            _log.error(msg);
            throw new JobException(msg);
        }
        if (StringUtils.isBlank(jobUuid)) {
            String msg = "No job uuid specified.";
            _log.error(msg);
            throw new JobException(msg);
        }
        
        // ------------------------- Call SQL ----------------------------
        int rows = 0; // rows affected
        try
        {
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Create the insert command.
            String sql = "delete from job_published " +
                         "where phase = :phase " +
                         "and job_uuid = :job_uuid";
            
            // Fill in the placeholders.           
            Query qry = session.createSQLQuery(sql);
            qry.setString("phase", phase.name());
            qry.setString("job_uuid", jobUuid);
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
            
            String msg = "Unable to delete job published record for phase " + 
                         phase.name() + " and job " + jobUuid + ".";
            _log.error(msg);
            throw new JobException(msg, e);
        }
        finally {
            try {HibernateUtil.commitTransaction();} 
            catch (Exception e) 
            {
                String msg = "Unable to commit job published record deletion for phase " + 
                             phase.name() + " and job " + jobUuid + ".";
                _log.error(msg);
                throw new JobException(msg, e);
            }
        }
        
        // Return the number of rows affected.
        return rows;
    }
    
    /* ---------------------------------------------------------------------- */
    /* clearPublishedJobs:                                                    */
    /* ---------------------------------------------------------------------- */
    /** This is an administrative command useful in testing but not likely to
     * be used in production.  This command deletes all rows from the published 
     * table.
     * 
     * @return number of rows affected.
     */
    public static int clearPublishedJobs()
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
            String sql = "delete from job_published";
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
            
            String msg = "Unable to clear all published jobs.";
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
    /** Populate a new JobPublished object with a record retrieved from the 
     * job_published table.
     * 
     * NOTE: This method must be manually maintained whenever the job_interrupts
     *       table schema changes.  
     * 
     * @param obj a record from the job_published table
     * @return a new model object
     */
    private static JobPublished populateJobPublished(Object obj)
    {
        // Don't blow up.
        if (obj == null) return null;
        Object[] array = (Object[]) obj;

        // Populate the queue object using table definition field order,
        // which is the order specified in all calling methods.
        JobPublished jobPublished = new JobPublished();
        jobPublished.setPhase(JobPhaseType.valueOf((String)array[0]));
        jobPublished.setJobUuid((String) array[1]);
        Timestamp createdTS = (Timestamp)array[2];
        jobPublished.setCreated(new Date(createdTS.getTime()));
        jobPublished.setCreator((String) array[3]);
        
        return jobPublished;
    }
}
