package org.iplantc.service.jobs.dao;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.CacheMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.JobLease;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.joda.time.DateTime;

/** This class is the interface to the job_leases table.  The main purpose of 
 * the table is to allow job scheduler threads to acquire and release leases
 * on the jobs table.  Scheduler threads are responsible for polling the jobs table
 * for work in one of the four job processing phases (see JobPhaseType).  The
 * acquireLease() method in this class ensures that only one thread in the whole
 * system can schedule work for its phase.  No matter where scheduler instances 
 * run, only one of them at a time can have the lease for a specific phase.
 * 
 * This class, in cooperation with AbstractPhaseScheduler, implements the leasing 
 * protocol.  The protocol involves acquiring, renewing and releasing leases
 * that have relatively short lease times.  The protocol guarantees (1) phase 
 * lease assignment to at most one thread at a time and (2) liveness even under 
 * catastrophic scheduler failures.  
 * 
 * Liveness is effectuated by a renewal algorithm that requires the lessee to 
 * renew its lease before it expires.  The expiration time is on the order of a
 * few minutes so that if the lessee fails, another scheduler thread can obtain  
 * the lease before the system is disrupted.  The current algorithm is for
 * renewal to occur after 25% of the lease has transpired.  This renewal 
 * frequency gives the lessee ample opportunity to maintain its lease even
 * under various network traffic, server load and clock skew conditions.  
 * 
 * With regard to clock skew, the lease protocol calls for different scheduler
 * threads to compare the current time to a lease's expiration time.  These
 * threads may be running on different virtual or real machines, so excessive
 * clock skew will disrupt operations.  Skew limited to a few seconds or 
 * even a few 10's of seconds is manageable.
 * 
 * @author rcardone
 */
public final class JobLeaseDao 
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = Logger.getLogger(JobLeaseDao.class);
    
    // The number of seconds before a lease expires.
    public static final int LEASE_SECONDS = 240;

    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    // Runtime leasing parameters.
    private final JobPhaseType _phase;
    private final String       _lessee;
    
    /* ********************************************************************** */
    /*                             Constructors                               */
    /* ********************************************************************** */
    public JobLeaseDao(JobPhaseType phase, String lessee)
    {
        // This should never happen in production so we can throw
        // a runtime exception to indicate a coding problem.
        if (phase == null || lessee == null)
        {
            String msg = "JobLeaseDao objects cannot be " +
                         "constructed with non-null parameters";
            _log.error(msg);
            throw new RuntimeException(msg);
        }
        
        // Assign all fields.
        _phase = phase;
        _lessee = lessee;
    }
    
    /* ********************************************************************** */
    /*                             Public Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* acquireLease:                                                          */
    /* ---------------------------------------------------------------------- */
    /** Acquire the phase lease for the lessee assigned at construction time.
     * If the lease is acquired, it expires in LEASE_SECONDS seconds.  To keep
     * the lease, the lessee should call this method again before the expiration 
     * to renew the lease. 
     * 
     * @return true if the lease was acquired or renewed, false otherwise.
     */
    public boolean acquireLease() 
    {
        // Initialize result value.
        boolean leaseAcquired = false;
        
        // Try to acquire the lock.
        try {
            // Get a hibernate session.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // --------------------- Lock the phase record
            // Get an exclusive row lock on the phase record in the lease table.
            // Note that row-level locking in the database depends on proper
            // index definition.  See MySQL FOR UPDATE documentation for details.
            String sql = "select lease, last_updated, expires_at, lessee" +
                         " from job_leases" +
                         " where lease = :phase FOR UPDATE";
            
            // Fill in the placeholders.           
            Query qry = session.createSQLQuery(sql);
            qry.setString("phase", _phase.name());                  
            qry.setCacheable(false);
            qry.setCacheMode(CacheMode.IGNORE);

            
            // Issue the call and populate the lease object.
            Object obj = qry.uniqueResult();
            LeaseInfo leaseInfo = extractLeaseInfo(obj);

            // --------------------- Determine the state of the lease
            // The current time is used to determine expiry.
            // Note that clock skew might be a problem
            // in unusual circumstances.
            long currentMillis = System.currentTimeMillis();
            
            // --- Case 1: No scheduler holds the lease.
            boolean acquireCalled = false;
            if (currentMillis > leaseInfo._expiresAt)
            {
                // We acquire the lease whether it's expired or released.
                acquireCalled = acquireLease(session, currentMillis);
                
                // Issue warning if lease had expired.
                if (_lessee.equals(leaseInfo._lessee))
                {
                    String msg = "Lease expired for phase " + _phase.name() + 
                                 " with lessee " + _lessee + ".";
                    _log.warn(msg);
                }
            }
            // --- Case 2: The current scheduler holds the valid lease
            else if (_lessee.equals(leaseInfo._lessee))
            {
                // We renew the lease to advance the expiration date.
                acquireCalled = acquireLease(session, currentMillis);                
            }
            // --- Case 3: Some other scheduler owns the lease.
            else 
            {
                // Do nothing.
            }

            // Lock in any updates and release the database lock.
            HibernateUtil.commitTransaction();
            
            // Indicate to the caller that we own the lease
            // if we attempted to get the lease and succeeded.
            if (acquireCalled) leaseAcquired = true;
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){_log.error("Rollback failed.", e1);}
            
            String msg = "Unable to acquire lease for phase " + _phase.name() + 
                         " with lessee " + _lessee + ".";
            _log.error(msg, e);
        }
        
        // Return the handle with the session that has an uncommitted transaction. 
        return leaseAcquired;
    }
    
    /* ---------------------------------------------------------------------- */
    /* releaseLease:                                                          */
    /* ---------------------------------------------------------------------- */
    /** The lessee can release the phase lease if it is assigned that lease.
     * If the lessee does not hold the lease, this method has no effect on the
     * lease's state.
     * 
     * @return true if the lease was released or an expired leases was removed
     *         from the database, false otherwise.  A false return value indicates
     *         that no database change occurred.
     */
    public boolean releaseLease() 
    {
        // Initialize result.
        boolean leaseReleased = false;
        
        // End the long-running transaction started in the acquire method.
        try {
            // Get a hibernate session.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();
            
            // ------- Lock the phase record
            // Get an exclusive row lock on the phase record in the lease table.
            // Note that row-level locking in the database depends on proper
            // index definition.  See MySQL FOR UPDATE documentation for details.
            String sql = "select lessee from job_leases" +
                         " where lease = :phase FOR UPDATE";
            
            // Fill in the placeholders.           
            Query qry = session.createSQLQuery(sql);
            qry.setString("phase", _phase.name());
            qry.setCacheable(false);
            qry.setCacheMode(CacheMode.IGNORE);
            
            // Issue the call and populate the model object.
            // (Yes, this is the tax for not using hibernate...)
            String lessee = (String) qry.uniqueResult();
            
            // Only release a lease own by this object's lessee.
            boolean releaseCalled = false;
            if (_lessee.equals(lessee))
            {
                // ------- Update the phase record
                // Advance the last updated timestamp.
                Date lastDate = new Date();
                
                // Release the lease.
                sql = "Update job_leases " +
                      "set last_updated = :lastdate, expires_at = NULL, lessee = NULL" +
                      " where lease = :phase";
                qry = session.createSQLQuery(sql);
                qry.setTimestamp("lastdate", lastDate);
                qry.setString("phase", _phase.name());
                qry.setCacheable(false);
                qry.setCacheMode(CacheMode.IGNORE);
                int rows = qry.executeUpdate();
                
                // Sanity check.
                if (rows != 1)
                {
                    // A problem here indicates some type of mismatch between this 
                    // version of the code and the job_leases table.  We make 
                    // some noise, but there's no real way to recover:  Every phase
                    // type needs to have exactly 1 record in the job_leases table.
                    String msg = "Code/database mismatch - Expected 1 row to be updated " +
                                 "in job_leases when releasing a lease for " + _lessee +
                                 ", but the number of rows updated was " + rows + ".";
                    _log.error(msg);
                }
                
                // The release call was made.
                releaseCalled = true; 
            }
            
            // Lock in any updates and release the database lock.
            HibernateUtil.commitTransaction();
            
            // Indicate to the caller that we successfully released the lease. 
            if (releaseCalled) leaseReleased = true;
        }
        catch (Exception e) 
        {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){_log.error("Rollback failed.", e1);}
            
            String msg = "Unable to release lease " + "for phase " + 
                         _phase.name() + " with lessee " + _lessee + ".";
            _log.error(msg);
        }
        
        return leaseReleased;
    }

    /* ---------------------------------------------------------------------- */
    /* getLeases:                                                             */
    /* ---------------------------------------------------------------------- */
    /** Dump the current state of all leases.  This is an administrative command 
     * that is useful in testing but will probably never be used in production.
     *  
     * @return the list of all leases in ascending lease order.
     */
    public static List<JobLease> getLeases()
    {
        // Initialize result list.
        List<JobLease> list = new ArrayList<>(4);
        
        // Dump the complete table..
        try {
            // Get a hibernate session.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Dump all rows in the table.
            String sql = "select lease, last_updated, expires_at, lessee from job_leases " +
                         "order by lease";
            
            // Issue the call and populate the lease object.
            Query qry = session.createSQLQuery(sql);
            qry.setCacheable(false);
            qry.setCacheMode(CacheMode.IGNORE);
            @SuppressWarnings("rawtypes")
            List qryResuts = qry.list();
            
            // Commit the transaction.
            HibernateUtil.commitTransaction();
            
            // Populate the list.
            populateList(list, qryResuts);
        }
        catch (Exception e)
        {
            // Rollback transaction.
            try {HibernateUtil.rollbackTransaction();}
             catch (Exception e1){_log.error("Rollback failed.", e1);}
            
            String msg = "Unable to retrieve leases.";
            _log.error(msg, e);
        }
        
        return list;
    }
    
    /* ---------------------------------------------------------------------- */
    /* clearLeases:                                                           */
    /* ---------------------------------------------------------------------- */
    /** Release all leases.  This is an administrative command that is useful 
     * in testing but will probably never be used in production.  
     * 
     * @return the number of leases affected.
     */
    public static int clearLeases()
    {
        // Return value.
        int rows = 0;
        
        // Dump the complete table..
        try {
            // Get a hibernate session.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Advance the last updated timestamp.
            Date lastDate = new Date();
            
            // Release the lease.
            String sql = "Update job_leases " +
                         "set last_updated = :lastdate, expires_at = NULL, lessee = NULL";
            Query qry = session.createSQLQuery(sql);
            qry.setTimestamp("lastdate", lastDate);
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
            
            String msg = "Unable to clear all leases.";
            _log.error(msg, e);
        }
        
        return rows;
    }
    
    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* acquireLease:                                                          */
    /* ---------------------------------------------------------------------- */
    /** Acquire a lease using an existing session where a transaction is in
     * progress.  This method can throw runtime hibernate exceptions.
     * 
     * @param session the session containing the in-progress transaction
     * @param millis the base time used to calculate expiry
     * @return true if we made the SQL update call without error, otherwise a
     *         runtime exception is thrown.
     */
    private boolean acquireLease(Session session, long millis)
    {
        // Create the date values.
        Date lastDate = new Date(millis);
        DateTime dt = new DateTime(millis);
        Date expireDate = dt.plusSeconds(LEASE_SECONDS).toDate();
        
        // Update the phase record.
        String sql = "Update job_leases set last_updated = :lastdate," +
                     " expires_at = :expiredate, lessee = :lessee" +
                     " where lease = :phase";
        Query qry = session.createSQLQuery(sql);
        qry.setTimestamp("lastdate", lastDate);
        qry.setTimestamp("expiredate", expireDate);
        qry.setString("lessee", _lessee);
        qry.setString("phase", _phase.name());
        qry.setCacheable(false);
        qry.setCacheMode(CacheMode.IGNORE);
        
        // Execute the update command and perform sanity check.
        int rows = qry.executeUpdate();
        if (rows != 1)
        {
            // A problem here indicates some type of mismatch between this 
            // version of the code and the job_leases table.  We make 
            // some noise, but there's no real way to recover:  Every phase
            // type needs to have exactly 1 record in the job_leases table.
            String msg = "Code/database mismatch - Expected 1 row to be updated " +
                         "in job_leases when acquiring a lease for " + _lessee +
                         ", but the number of rows updated was " + rows + ".";
            _log.error(msg);
        }
        
        // We made it through.
        return true;
    }
    
    /* ---------------------------------------------------------------------- */
    /* extractLeaseInfo:                                                      */
    /* ---------------------------------------------------------------------- */
    /** Extract the useful fields from a raw database record.
     * 
     * @param obj a single database row returned as an array of Object
     * @return the populated info object
     * @throws JobException if the input is null
     */
    private LeaseInfo extractLeaseInfo(Object obj) throws JobException
    {
        // We should always get a record back.
        if (obj == null)
        {
            String msg = "Internal error - Phase not found: " + _phase.name() + ".";
            _log.error(msg);
            throw new JobException(msg);
        }
        
        // The row is returned as an array of object.
        Object[] array = (Object[]) obj;
        
        // Extract only the information we are going to use.
        // Note that both fields can be null.
        LeaseInfo leaseInfo = new LeaseInfo();
        leaseInfo._lessee = (String) array[3];
        
        // Waiting for Java 8 time functions...
        Timestamp updatedTS = (Timestamp) array[2];
        if (updatedTS != null) leaseInfo._expiresAt = updatedTS.getTime();
        
        return leaseInfo;
    }
    
    /* ---------------------------------------------------------------------- */
    /* populateList:                                                          */
    /* ---------------------------------------------------------------------- */
    @SuppressWarnings("rawtypes")
    private static void populateList(List<JobLease> outList, List qryResults)
     throws JobException
    {
       // There should always be the initialized rows for each phase.
        // We should always get a record back.
        if (qryResults == null)
        {
            String msg = "Configuration error - No lease records found.";
            _log.error(msg);
            throw new JobException(msg);
        }
       
        // Marshal each row from the query results.
        for (Object rowobj : qryResults)
        {
            // Access row as an array and create new lease.
            Object[] row = (Object[]) rowobj;
            JobLease jobLease = new JobLease();
            
            // Marshal strings.
            jobLease.setLease((String) row[0]);
            jobLease.setLessee((String) row[3]);
            
            // Marshal the timestamps.
            Timestamp lastUpdatedTS = (Timestamp) row[1];
            if (lastUpdatedTS != null) jobLease.setLastUpdated(new Date(lastUpdatedTS.getTime()));
            Timestamp expiresAtTS = (Timestamp) row[2];
            if (expiresAtTS != null) jobLease.setExpiresAt(new Date(expiresAtTS.getTime()));

            // Add the lease to the result list.
            outList.add(jobLease);
        }
    }
    
    /* ********************************************************************** */
    /*                            LeaseInfo Class                             */
    /* ********************************************************************** */
    // Container into which lease information is extracted.
    private final class LeaseInfo
    {
        private long   _expiresAt;
        private String _lessee;
    }
}
