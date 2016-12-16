package org.iplantc.service.jobs.dao;

import java.math.BigInteger;
import java.util.Date;

import org.apache.log4j.Logger;
import org.hibernate.CacheMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobUpdateParameters;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** This test suite does not use any Agave services and should be run when 
 * the Agave jobs service is not running.  The set-up and tear-down methods
 * remove any test data from the database.
 * 
 * @author rcardone
 */
public class JobUpdateTest 
{
    /* ********************************************************************** */
    /*                             Static Fields                              */
    /* ********************************************************************** */ 
    // Tracing.
    private static final Logger _log = Logger.getLogger(JobUpdateTest.class);
    
    // Id of job created by this test.
    private static final String JOB_UUID = "JobUpdateTest-UUID";
    
    // The tenant id used in this test and that exists in the tenants table.
    private final static String TENANT_ID = "iplantc.org";
    
    /* ********************************************************************** */
    /*                                 Fields                                 */
    /* ********************************************************************** */
    
    /* ********************************************************************** */
    /*                            Set Up / Tear Down                          */
    /* ********************************************************************** */    
    /* ---------------------------------------------------------------------- */
    /* setup:                                                                 */
    /* ---------------------------------------------------------------------- */
    @BeforeMethod
    private void setup()
    {
        // Set up thread-local context.
        TenancyHelper.setCurrentTenantId(TENANT_ID);
        
        // Start with a clean test record in the jobs table.  
        // This will be the only record we update.
        deleteTestJob();
        insertTestJob();
    }
    
    /* ---------------------------------------------------------------------- */
    /* teardown:                                                              */
    /* ---------------------------------------------------------------------- */
    @AfterMethod
    private void teardown()
    {
        deleteTestJob();
    }
    
    /* ********************************************************************** */
    /*                              Test Methods                              */
    /* ********************************************************************** */    
    /* ---------------------------------------------------------------------- */
    /* updateNonStatusTest:                                                   */
    /* ---------------------------------------------------------------------- */
    /** Update all possible fields except for the status field. 
     * 
     * All tests start with a newly inserted job record in the database. 
     */
    @Test(enabled=true)
    public void updateNonStatusTest()
    {   
        // Sleep so that current time definition changes
        // enough so that round off differences don't matter.
        try {Thread.sleep(2000);} catch (Exception e){}
        
        // Values.
        Date now = new Date();
        String archivePath = "/archivePath";
        String localJobId  = "myLocalJobId";
        String workPath    = "/workPath";
        int retries        = 27;
        
        // Populate parms object.
        JobUpdateParameters parms = new JobUpdateParameters();
        parms.setArchivePath(archivePath);
        parms.setCreated(now);
        parms.setEndTime(now);
        parms.setLastUpdated(now);
        parms.setLocalJobId(localJobId);
        parms.setRetries(retries);
        parms.setStartTime(now);
        parms.setSubmitTime(now);
        parms.setVisible(true);
        parms.setWorkPath(workPath);
       
        // Update the job.
        try {JobDao.update(JOB_UUID, TENANT_ID, parms);}
        catch (JobException e) {
            e.printStackTrace();
            Assert.fail("update call failed: " + e.getMessage());
        }
        
        // Retrieve the job.
        Job job = null;
        try {job = JobDao.getByUuid(JOB_UUID);}
        catch (JobException e) {
            e.printStackTrace();
            Assert.fail("Unable to retrieve job: " + e.getMessage());
        }
        
        // Note that date handling between java (java.util and java.sql), hibernate and
        // mysql, the times received back using getByUuid() are rounded off to the nearest
        // second (milliseconds are dropped).  Rather than take a detour now, we live with it.   
        
        // Check values.
        Assert.assertEquals(job.getArchivePath(), archivePath, "Unexpected archivePath value.");
        Assert.assertTrue(Math.abs(now.getTime() - job.getCreated().getTime()) <= 500, "Unexpected created value.");
        Assert.assertTrue(Math.abs(now.getTime() - job.getEndTime().getTime()) <= 500, "Unexpected endTime value.");
        Assert.assertTrue(Math.abs(now.getTime() - job.getLastUpdated().getTime()) <= 500, "Unexpected lastUpdated value.");
        Assert.assertEquals(job.getLocalJobId(), localJobId, "Unexpected localJobId value.");
        Assert.assertEquals(job.getRetries(), new Integer(retries), "Unexpected retries value.");
        Assert.assertTrue(Math.abs(now.getTime() - job.getStartTime().getTime()) <= 500, "Unexpected startTime value.");
        Assert.assertTrue(Math.abs(now.getTime() - job.getSubmitTime().getTime()) <= 500, "Unexpected submitTime value.");
        Assert.assertTrue(job.isVisible(), "Unexpected visible value.");
        Assert.assertEquals(job.getWorkPath(), workPath, "Unexpected workPath value.");
    }

    /* ---------------------------------------------------------------------- */
    /* updateWithStatusTest:                                                  */
    /* ---------------------------------------------------------------------- */
    /** Update all possible fields including the status field. 
     * 
     * All tests start with a newly inserted job record in the database. 
     */
    @Test(enabled=true)
    public void updateWithStatusTest()
    {   
        // Sleep so that current time definition changes
        // enough so that round off differences don't matter.
        try {Thread.sleep(2000);} catch (Exception e){}
        
        // Values.
        Date now = new Date();
        String archivePath = "/archivePath";
        String localJobId  = "myLocalJobId";
        String workPath    = "/workPath";
        int retries        = 27;
        
        // Populate parms object.
        JobUpdateParameters parms = new JobUpdateParameters();
        parms.setArchivePath(archivePath);
        parms.setCreated(now);
        parms.setEndTime(now);
        parms.setLastUpdated(now);
        parms.setLocalJobId(localJobId);
        parms.setRetries(retries);
        parms.setStartTime(now);
        parms.setSubmitTime(now);
        parms.setVisible(true);
        parms.setWorkPath(workPath);
        parms.setStatus(JobStatusType.PROCESSING_INPUTS);
       
        // Update the job.
        try {JobDao.update(JOB_UUID, TENANT_ID, parms);}
        catch (JobException e) {
            e.printStackTrace();
            Assert.fail("update call failed: " + e.getMessage());
        }
        
        // Retrieve the job.
        Job job = null;
        try {job = JobDao.getByUuid(JOB_UUID);}
        catch (JobException e) {
            e.printStackTrace();
            Assert.fail("Unable to retrieve job: " + e.getMessage());
        }
        
        // Note that date handling between java (java.util and java.sql), hibernate and
        // mysql, the times received back using getByUuid() are rounded off to the nearest
        // second (milliseconds are dropped).  Rather than take a detour now, we live with it.   
        
        // Check values.
        Assert.assertEquals(job.getArchivePath(), archivePath, "Unexpected archivePath value.");
        Assert.assertTrue(Math.abs(now.getTime() - job.getCreated().getTime()) <= 500, "Unexpected created value.");
        Assert.assertTrue(Math.abs(now.getTime() - job.getEndTime().getTime()) <= 500, "Unexpected endTime value.");
        Assert.assertTrue(Math.abs(now.getTime() - job.getLastUpdated().getTime()) <= 500, "Unexpected lastUpdated value.");
        Assert.assertEquals(job.getLocalJobId(), localJobId, "Unexpected localJobId value.");
        Assert.assertEquals(job.getRetries(), new Integer(retries), "Unexpected retries value.");
        Assert.assertTrue(Math.abs(now.getTime() - job.getStartTime().getTime()) <= 500, "Unexpected startTime value.");
        Assert.assertTrue(Math.abs(now.getTime() - job.getSubmitTime().getTime()) <= 500, "Unexpected submitTime value.");
        Assert.assertTrue(job.isVisible(), "Unexpected visible value.");
        Assert.assertEquals(job.getWorkPath(), workPath, "Unexpected workPath value.");
        Assert.assertEquals(job.getStatus(), JobStatusType.PROCESSING_INPUTS, "Unexpected status value.");
    }

    /* ---------------------------------------------------------------------- */
    /* updateBadStatusTest:                                                   */
    /* ---------------------------------------------------------------------- */
    /** Try to update the status field in an invalid way.
     * 
     * All tests start with a newly inserted job record in the database. 
     */
    @Test(enabled=true)
    public void updateBadStatusTest()
    {   
        // Populate parms object.
        JobUpdateParameters parms = new JobUpdateParameters();
        parms.setStatus(JobStatusType.ARCHIVING);
       
        // Update the job.
        boolean exceptionCaught = false;
        try {JobDao.update(JOB_UUID, TENANT_ID, parms);}
        catch (JobException e) {exceptionCaught = true;}
        Assert.assertTrue(exceptionCaught, "The expected exception was not thrown.");
        
        // Retrieve the job.
        Job job = null;
        try {job = JobDao.getByUuid(JOB_UUID);}
        catch (JobException e) {
            e.printStackTrace();
            Assert.fail("Unable to retrieve job: " + e.getMessage());
        }
        
        // Check values.
        Assert.assertEquals(job.getStatus(), JobStatusType.PENDING, "Unexpected status value.");
    }

    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* insertTestJob:                                                         */
    /* ---------------------------------------------------------------------- */
//    CREATE TABLE `jobs` ( 
//            `id` bigint(20) NOT NULL AUTO_INCREMENT,
//            `archive_output` tinyint(1) DEFAULT NULL,
//            `archive_path` varchar(255) DEFAULT NULL,
//            `callback_url` varchar(255) DEFAULT NULL,
//            `charge` float DEFAULT NULL,
//            `created` datetime NOT NULL,
//            `end_time` datetime DEFAULT NULL,
//            `error_message` varchar(16384) DEFAULT NULL,
//            `inputs` varchar(16384) DEFAULT NULL,
//            `internal_username` varchar(32) DEFAULT NULL,
//            `last_updated` datetime NOT NULL,
//            `local_job_id` varchar(255) DEFAULT NULL,
//            `memory_request` int(11) NOT NULL,
//            `name` varchar(64) NOT NULL,
//            `output_path` varchar(255) DEFAULT NULL,
//            `owner` varchar(32) NOT NULL,
//            `parameters` varchar(16384) DEFAULT NULL,
//            `processor_count` int(11) NOT NULL,
//            `requested_time` varchar(19) DEFAULT NULL,
//            `retries` int(11) DEFAULT NULL,
//            `scheduler_job_id` varchar(255) DEFAULT NULL,
//            `software_name` varchar(80) NOT NULL,
//            `start_time` datetime DEFAULT NULL,
//            `status` varchar(32) NOT NULL,
//            `submit_time` datetime DEFAULT NULL,
//            `execution_system` varchar(64) NOT NULL DEFAULT '',
//            `tenant_id` varchar(128) NOT NULL,
//            `update_token` varchar(64) DEFAULT NULL,
//            `uuid` varchar(64) NOT NULL,
//            `optlock` int(11) DEFAULT NULL,
//            `visible` tinyint(1) DEFAULT NULL,
//            `work_path` varchar(255) DEFAULT NULL,
//            `archive_system` bigint(64) DEFAULT NULL,
//            `queue_request` varchar(80) NOT NULL,
//            `node_count` bigint(20) NOT NULL,
//            `status_checks` int(11) NOT NULL,
//            PRIMARY KEY (`id`),
//            UNIQUE KEY `uuid` (`uuid`),
//            KEY `FK31DC56AC7D7B60` (`archive_system`),
//            KEY `jobs_status` (`status`)
//          ) ENGINE=InnoDB AUTO_INCREMENT=59560 DEFAULT CHARSET=latin1;
    private int insertTestJob()
    {
        // Return value.
        int rows = 0;
        
        // Dump the complete table..
        try {
            // Get a hibernate session.
            Session session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.beginTransaction();

            // Fill in the required field.  This includes all fields
            // that are defined NOT NULL in the database plus fields
            // filled in by TRADITION so that retrieval code won't 
            // blow up.  This latter set includes:
            //
            //      archive_output
            //      visible
            //
            // If the above fields are null, an NPE will be thrown when
            // an attempt is made to retrieve the job entity.  
            String sql = "insert into jobs " +
                         "(archive_output, " +
                         "created, " +
                         "last_updated, " +
                         "memory_request, " +
                         "name, " +
                         "owner, " +
                         "processor_count, " +
                         "software_name, " +
                         "status, " +
                         "execution_system, " +
                         "tenant_id, " +
                         "uuid," +
                         "queue_request, " +
                         "node_count, " +
                         "visible, " +
                         "status_checks) " + 
                         "VALUES " +
                         "(:archive_output, " +
                         ":created, " +
                         ":last_updated, " +
                         ":memory_request, " +
                         ":name, " +
                         ":owner, " +
                         ":processor_count, " +
                         ":software_name, " +
                         ":status, " +
                         ":execution_system, " +
                         ":tenant_id, " +
                         ":uuid," +
                         ":queue_request, " +
                         ":node_count, " +
                         ":visible, " +
                         ":status_checks)";
            Date now = new Date();
            Query qry = session.createSQLQuery(sql);
            qry.setBoolean("archive_output", false);
            qry.setTimestamp("created", now);
            qry.setTimestamp("last_updated", now);
            qry.setInteger("memory_request", 1000000);
            qry.setString("name", "Job Update Test");
            qry.setString("owner", "rcardone");
            qry.setInteger("processor_count", 64);
            qry.setString("software_name", "JobUpdateTest.java");
            qry.setString("status", "PENDING"); 
            qry.setString("execution_system", "Bud's Laptop");
            qry.setString("tenant_id", TENANT_ID);
            qry.setString("uuid", JOB_UUID);
            qry.setString("queue_request", "fastqueue");
            qry.setBigInteger("node_count", new BigInteger("64000"));
            qry.setBoolean("visible", false);
            qry.setInteger("status_checks", 0); 
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
    
    /* ---------------------------------------------------------------------- */
    /* deleteTestJob:                                                         */
    /* ---------------------------------------------------------------------- */
    private int deleteTestJob()
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
            String sql = "delete from jobs " +
                         "where uuid = :uuid and tenant_id = :tenantId";
            Query qry = session.createSQLQuery(sql);
            qry.setString("uuid", JOB_UUID);
            qry.setString("tenantId", TENANT_ID);
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
}
