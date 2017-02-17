package org.iplantc.service.jobs.dao;

import java.util.List;

import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.JobWorkerException;
import org.iplantc.service.jobs.model.JobClaim;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/** Tests in this class run when jobs subsystem is NOT RUNNING and the
 * job_interrupts table is empty.  These tests access the database directly 
 * through a DAO and will conflict with concurrently running application code.
 * 
 * @author rcardone
 */
public class JobWorkerDaoTest 
{
    /* ********************************************************************** */
    /*                                Constants                               */
    /* ********************************************************************** */
    
    /* ********************************************************************** */
    /*                            Set Up / Tear Down                          */
    /* ********************************************************************** */
    // NOTE: If you add more tests, you may need revisit before/after time.s 
    
    /* ---------------------------------------------------------------------- */
    /* setup:                                                                 */
    /* ---------------------------------------------------------------------- */
    @BeforeSuite
    private void setup() throws JobException
    {
        JobWorkerDao.clearClaims();
    }
    
    /* ---------------------------------------------------------------------- */
    /* teardown:                                                              */
    /* ---------------------------------------------------------------------- */
    @AfterSuite
    private void teardown() throws JobException
    {
        JobWorkerDao.clearClaims();
    }
    
    /* ********************************************************************** */
    /*                              Test Methods                              */
    /* ********************************************************************** */    
    /* ---------------------------------------------------------------------- */
    /* workerTest:                                                            */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void workerTest() throws JobException
    {   
        // Get all the claims.
        List<JobClaim> claimList = JobWorkerDao.getJobClaims();
        Assert.assertEquals(claimList.size(), 0, "Expected no claims at start");
        
        // Add a claim.
        JobWorkerDao.claimJob("jobUuid1", "workerUuid1", "host1", "containerId1");
        
        // Get all the claims.
        claimList = JobWorkerDao.getJobClaims();
        Assert.assertEquals(claimList.size(), 1, "Unexpected number of claims returned");
        
        // Retrieve the claim by job uuid.
        JobClaim claim = JobWorkerDao.getJobClaimByJobUuid("jobUuid1");
        Assert.assertNotNull(claim, "Unable to retrieve claim using jobUuid1");
        Assert.assertEquals(claim.getJobUuid(), "jobUuid1", "Unexpected job uuid returned");
        Assert.assertEquals(claim.getWorkerUuid(), "workerUuid1", "Unexpected worker uuid returned");
        Assert.assertEquals(claim.getHost(), "host1", "Unexpected host returned");
        Assert.assertEquals(claim.getContainerId(), "containerId1", "Unexpected container id returned");
        
        // Retrieve the claim by worker uuid.
        claim = JobWorkerDao.getJobClaimByWorkerUuid("workerUuid1");
        Assert.assertNotNull(claim, "Unable to retrieve claim using workerUuid1");
        Assert.assertEquals(claim.getJobUuid(), "jobUuid1", "Unexpected job uuid returned");
        Assert.assertEquals(claim.getWorkerUuid(), "workerUuid1", "Unexpected worker uuid returned");
        Assert.assertEquals(claim.getHost(), "host1", "Unexpected host returned");
        Assert.assertEquals(claim.getContainerId(), "containerId1", "Unexpected container id returned");
        
        // Try to add a duplicate claim.
        boolean exceptionCaught = false;
        try {JobWorkerDao.claimJob("jobUuid1", "workerUuid1", "host1", "containerId1");}
        catch (JobWorkerException e){exceptionCaught = true;}
        Assert.assertTrue(exceptionCaught, "Expected duplicate record exception");
        
        // Add another claim.
        JobWorkerDao.claimJob("jobUuid2", "workerUuid2", "host2", "containerId2");
        
        // Get all the claims.
        claimList = JobWorkerDao.getJobClaims();
        Assert.assertEquals(claimList.size(), 2, "Unexpected number of claims returned");
        
        // Retrieve the claim 2 by job uuid.
        claim = JobWorkerDao.getJobClaimByJobUuid("jobUuid2");
        Assert.assertNotNull(claim, "Unable to retrieve claim using jobUuid1");
        Assert.assertEquals(claim.getJobUuid(), "jobUuid2", "Unexpected job uuid returned");
        Assert.assertEquals(claim.getWorkerUuid(), "workerUuid2", "Unexpected worker uuid returned");
        Assert.assertEquals(claim.getHost(), "host2", "Unexpected host returned");
        Assert.assertEquals(claim.getContainerId(), "containerId2", "Unexpected container id returned");
        
        // Retrieve the claim 2 by worker uuid.
        claim = JobWorkerDao.getJobClaimByWorkerUuid("workerUuid2");
        Assert.assertNotNull(claim, "Unable to retrieve claim using workerUuid1");
        Assert.assertEquals(claim.getJobUuid(), "jobUuid2", "Unexpected job uuid returned");
        Assert.assertEquals(claim.getWorkerUuid(), "workerUuid2", "Unexpected worker uuid returned");
        Assert.assertEquals(claim.getHost(), "host2", "Unexpected host returned");
        Assert.assertEquals(claim.getContainerId(), "containerId2", "Unexpected container id returned");
        
        // Delete the first claim by job uuid.
        int deleted = JobWorkerDao.unclaimJobByJobUuid("jobUuid1");
        Assert.assertEquals(deleted, 1);
        
        // Get all the claims.
        claimList = JobWorkerDao.getJobClaims();
        Assert.assertEquals(claimList.size(), 1, "Unexpected number of claims returned");
        
        // Try to retrieve the first claim.
        claim = JobWorkerDao.getJobClaimByJobUuid("jobUuid1");
        Assert.assertNull(claim, "Able to retrieve deleted claim using jobUuid1");
        
        // Delete the first claim again.
        deleted = JobWorkerDao.unclaimJobByJobUuid("jobUuid1");
        Assert.assertEquals(deleted, 0);
        
        // Delete the second claim by worker uuid.
        deleted = JobWorkerDao.unclaimJobByWorkerUuid("workerUuid2");
        Assert.assertEquals(deleted, 1);
        
        // Get all the claims.
        claimList = JobWorkerDao.getJobClaims();
        Assert.assertEquals(claimList.size(), 0, "Unexpected number of claims returned");
        
        // Try to retrieve the second claim.
        claim = JobWorkerDao.getJobClaimByWorkerUuid("workerUuid2");
        Assert.assertNull(claim, "Able to retrieve deleted claim using jobUuid1");
        
        // Delete the second claim again.
        deleted = JobWorkerDao.unclaimJobByWorkerUuid("workerUuid2");
        Assert.assertEquals(deleted, 0);
    }

}
