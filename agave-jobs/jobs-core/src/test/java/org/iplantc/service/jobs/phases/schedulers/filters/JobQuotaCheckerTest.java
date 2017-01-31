package org.iplantc.service.jobs.phases.schedulers.filters;

import java.util.ArrayList;
import java.util.List;

import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.JobActiveCount;
import org.iplantc.service.jobs.model.JobQuotaInfo;
import org.testng.Assert;
import org.testng.annotations.Test;

/** This test program uses the package-scope test constructor of JobQuotaChecker
 * to avoid querying the databases for active jobs.  The active job query in
 * JobQuotaChecker is, therefore, not exercised in this test program.
 * 
 * @author rcardone
 *
 */
public class JobQuotaCheckerTest 
{
    /* ********************************************************************** */
    /*                              Test Methods                              */
    /* ********************************************************************** */    
    /* ---------------------------------------------------------------------- */
    /* zeroFail:                                                              */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void zeroFail() throws JobException 
    {   
        // Announce ourselves.
        System.out.println("-------- Running quotaFiltering");
        
        // Create the test quota checker instance that does NOT query the database.
        List<JobQuotaInfo> quotaInfoList = getZeroFailList();
        JobQuotaChecker quotaChecker = 
            new JobQuotaChecker(quotaInfoList, new ArrayList<JobActiveCount>());
        
        for (JobQuotaInfo info : quotaInfoList) {
            boolean result = quotaChecker.exceedsSystemQuota(info);
            Assert.assertTrue(result, "Unexpected exceedsSystemQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsSystemUserQuota(info);
            if (info.getUuid().equals("job1"))
                Assert.assertTrue(result, "Unexpected exceedsSystemUserQuota() result for " + info.getUuid());
            else
                Assert.assertFalse(result, "Unexpected exceedsSystemUserQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsQueueQuota(info);
            Assert.assertTrue(result, "Unexpected exceedsQueueQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsQueueUserQuota(info);
            if (info.getUuid().equals("job1"))
                Assert.assertTrue(result, "Unexpected exceedsQueueUserQuota() result for " + info.getUuid());
             else
                Assert.assertFalse(result, "Unexpected exceedsQueueUserQuota() result for " + info.getUuid());
        }
    }

    /* ---------------------------------------------------------------------- */
    /* unlimited:                                                             */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void unlimited() throws JobException 
    {   
        // Announce ourselves.
        System.out.println("-------- Running unlimited");
        
        ArrayList<JobActiveCount> activeCountList = new ArrayList<JobActiveCount>();
        JobActiveCount activeCount = new JobActiveCount();
        activeCount.setTenantId("tenant1");
        activeCount.setOwner("owner2");
        activeCount.setExecutionSystem("execSystem1");
        activeCount.setQueueRequest("queue1");
        activeCount.setCount(1);
        activeCountList.add(activeCount);
        
        // Create the test quota checker instance that does NOT query the database.
        List<JobQuotaInfo> quotaInfolList = getUnlimitedList();
        JobQuotaChecker quotaChecker = 
            new JobQuotaChecker(quotaInfolList, activeCountList);
        
        // Since Job1 has all unlimited quotas, it should pass all tests.  Jobs 2 and 3,
        // however, have a queue/user limit of 1.  We expect these to exceed quota.
        for (JobQuotaInfo info : quotaInfolList) {
            boolean result = quotaChecker.exceedsSystemQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsSystemQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsSystemUserQuota(info);
            if (info.getUuid().equals("job1"))
                Assert.assertFalse(result, "Unexpected exceedsSystemUserQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsQueueQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsQueueQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsQueueUserQuota(info);
            if (info.getUuid().equals("job1"))
                Assert.assertFalse(result, "Unexpected exceedsQueueUserQuota() result for " + info.getUuid());
             else
                Assert.assertTrue(result, "Unexpected exceedsQueueUserQuota() result for " + info.getUuid());
        }
    }

    /* ---------------------------------------------------------------------- */
    /* maxSystem:                                                             */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void maxSystem() throws JobException 
    {   
        // Announce ourselves.
        System.out.println("-------- Running maxSystem");
        
        // -------------------- Context 1 --------------------
        ArrayList<JobActiveCount> activeCountList = new ArrayList<JobActiveCount>();
        JobActiveCount activeCount = new JobActiveCount();
        activeCount.setTenantId("tenant1");
        activeCount.setOwner("owner2");
        activeCount.setExecutionSystem("execSystem1");
        activeCount.setQueueRequest("queue1");
        activeCount.setCount(1);
        activeCountList.add(activeCount);
        
        // Create the test quota checker instance that does NOT query the database.
        List<JobQuotaInfo> quotaInfolList = getMaxSystemList();
        JobQuotaChecker quotaChecker = 
            new JobQuotaChecker(quotaInfolList, activeCountList);
        
        // Since each job has a system quota of 2 and there's only 1 active job,
        // we expect all tests to return false.
        for (JobQuotaInfo info : quotaInfolList) {
            boolean result = quotaChecker.exceedsSystemQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsSystemQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsSystemUserQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsSystemUserQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsQueueQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsQueueQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsQueueUserQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsQueueUserQuota() result for " + info.getUuid());
            
            // All checks.
            result = quotaChecker.exceedsQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsQuota() result for " + info.getUuid());
        }
        
        // -------------------- Context 2 --------------------
        activeCountList = new ArrayList<JobActiveCount>();
        activeCount = new JobActiveCount();
        activeCount.setTenantId("tenant1");
        activeCount.setOwner("owner1");
        activeCount.setExecutionSystem("execSystem1");
        activeCount.setQueueRequest("queue1");
        activeCount.setCount(2);      // Increased
        activeCountList.add(activeCount);
        
        // Create the test quota checker instance that does NOT query the database.
        quotaChecker = new JobQuotaChecker(quotaInfolList, activeCountList);
        
        // Since each job has a system quota of 2 and there are now 2 active jobs,
        // we expect the system test and composite test to return true.
        for (JobQuotaInfo info : quotaInfolList) {
            boolean result = quotaChecker.exceedsSystemQuota(info);
            Assert.assertTrue(result, "Unexpected exceedsSystemQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsSystemUserQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsSystemUserQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsQueueQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsQueueQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsQueueUserQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsQueueUserQuota() result for " + info.getUuid());
            
            // All checks.
            result = quotaChecker.exceedsQuota(info);
            Assert.assertTrue(result, "Unexpected exceedsQuota() result for " + info.getUuid());
        }
        
        // -------------------- Context 3 --------------------
        activeCountList = new ArrayList<JobActiveCount>();
        activeCount = new JobActiveCount();
        activeCount.setTenantId("tenant1");
        activeCount.setOwner("owner1");
        activeCount.setExecutionSystem("execSystem1");
        activeCount.setQueueRequest("queue1");
        activeCount.setCount(3);      // Increased
        activeCountList.add(activeCount);
        
        // Create the test quota checker instance that does NOT query the database.
        quotaChecker = new JobQuotaChecker(quotaInfolList, activeCountList);
        
        // Since each job has a system quota of 2 and there are now 3 active jobs,
        // we expect the system test and composite test to return true.
        for (JobQuotaInfo info : quotaInfolList) {
            boolean result = quotaChecker.exceedsSystemQuota(info);
            Assert.assertTrue(result, "Unexpected exceedsSystemQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsSystemUserQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsSystemUserQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsQueueQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsQueueQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsQueueUserQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsQueueUserQuota() result for " + info.getUuid());
            
            // All checks.
            result = quotaChecker.exceedsQuota(info);
            Assert.assertTrue(result, "Unexpected exceedsQuota() result for " + info.getUuid());
        }
   }

    /* ---------------------------------------------------------------------- */
    /* maxSystemUser:                                                         */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void maxSystemUser() throws JobException 
    {   
        // Announce ourselves.
        System.out.println("-------- Running maxSystemUser");
        
        // -------------------- Context 1 --------------------
        ArrayList<JobActiveCount> activeCountList = new ArrayList<JobActiveCount>();
        JobActiveCount activeCount = new JobActiveCount();
        activeCount.setTenantId("tenant1");
        activeCount.setOwner("owner2");
        activeCount.setExecutionSystem("execSystem1");
        activeCount.setQueueRequest("queue1");
        activeCount.setCount(1);
        activeCountList.add(activeCount);
        
        // Create the test quota checker instance that does NOT query the database.
        List<JobQuotaInfo> quotaInfolList = getMaxSystemUserList();
        JobQuotaChecker quotaChecker = 
            new JobQuotaChecker(quotaInfolList, activeCountList);
        
        // Since each job has a system quota of 2 and there's only 1 active job,
        // we expect all tests to return false.
        for (JobQuotaInfo info : quotaInfolList) {
            boolean result = quotaChecker.exceedsSystemQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsSystemQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsSystemUserQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsSystemUserQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsQueueQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsQueueQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsQueueUserQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsQueueUserQuota() result for " + info.getUuid());
            
            // All checks.
            result = quotaChecker.exceedsQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsQuota() result for " + info.getUuid());
        }
        
        // -------------------- Context 2 --------------------
        activeCountList = new ArrayList<JobActiveCount>();
        activeCount = new JobActiveCount();
        activeCount.setTenantId("tenant1");
        activeCount.setOwner("owner1");
        activeCount.setExecutionSystem("execSystem1");
        activeCount.setQueueRequest("queue1");
        activeCount.setCount(2);      // Increased
        activeCountList.add(activeCount);
        
        // Create the test quota checker instance that does NOT query the database.
        quotaChecker = new JobQuotaChecker(quotaInfolList, activeCountList);
        
        // Since each job has a system quota of 2 and there are now 2 active jobs,
        // we expect the system test and composite test to return true.
        for (JobQuotaInfo info : quotaInfolList) {
            boolean result = quotaChecker.exceedsSystemQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsSystemQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsSystemUserQuota(info);
            Assert.assertTrue(result, "Unexpected exceedsSystemUserQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsQueueQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsQueueQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsQueueUserQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsQueueUserQuota() result for " + info.getUuid());
            
            // All checks.
            result = quotaChecker.exceedsQuota(info);
            Assert.assertTrue(result, "Unexpected exceedsQuota() result for " + info.getUuid());
        }
        
        // -------------------- Context 3 --------------------
        activeCountList = new ArrayList<JobActiveCount>();
        activeCount = new JobActiveCount();
        activeCount.setTenantId("tenant1");
        activeCount.setOwner("owner1");
        activeCount.setExecutionSystem("execSystem1");
        activeCount.setQueueRequest("queue1");
        activeCount.setCount(3);      // Increased
        activeCountList.add(activeCount);
        
        // Create the test quota checker instance that does NOT query the database.
        quotaChecker = new JobQuotaChecker(quotaInfolList, activeCountList);
        
        // Since each job has a system quota of 2 and there are now 3 active jobs,
        // we expect the system test and composite test to return true.
        for (JobQuotaInfo info : quotaInfolList) {
            boolean result = quotaChecker.exceedsSystemQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsSystemQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsSystemUserQuota(info);
            Assert.assertTrue(result, "Unexpected exceedsSystemUserQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsQueueQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsQueueQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsQueueUserQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsQueueUserQuota() result for " + info.getUuid());
            
            // All checks.
            result = quotaChecker.exceedsQuota(info);
            Assert.assertTrue(result, "Unexpected exceedsQuota() result for " + info.getUuid());
        }
   }

    /* ---------------------------------------------------------------------- */
    /* maxQueue:                                                              */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void maxQueue() throws JobException 
    {   
        // Announce ourselves.
        System.out.println("-------- Running maxQueue");
        
        // -------------------- Context 1 --------------------
        ArrayList<JobActiveCount> activeCountList = new ArrayList<JobActiveCount>();
        JobActiveCount activeCount = new JobActiveCount();
        activeCount.setTenantId("tenant1");
        activeCount.setOwner("owner2");
        activeCount.setExecutionSystem("execSystem1");
        activeCount.setQueueRequest("queue1");
        activeCount.setCount(1);
        activeCountList.add(activeCount);
        
        // Create the test quota checker instance that does NOT query the database.
        List<JobQuotaInfo> quotaInfolList = getMaxQueueList();
        JobQuotaChecker quotaChecker = 
            new JobQuotaChecker(quotaInfolList, activeCountList);
        
        // Since each job has a system quota of 2 and there's only 1 active job,
        // we expect all tests to return false.
        for (JobQuotaInfo info : quotaInfolList) {
            boolean result = quotaChecker.exceedsSystemQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsSystemQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsSystemUserQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsSystemUserQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsQueueQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsQueueQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsQueueUserQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsQueueUserQuota() result for " + info.getUuid());
            
            // All checks.
            result = quotaChecker.exceedsQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsQuota() result for " + info.getUuid());
        }
        
        // -------------------- Context 2 --------------------
        activeCountList = new ArrayList<JobActiveCount>();
        activeCount = new JobActiveCount();
        activeCount.setTenantId("tenant1");
        activeCount.setOwner("owner1");
        activeCount.setExecutionSystem("execSystem1");
        activeCount.setQueueRequest("queue1");
        activeCount.setCount(2);      // Increased
        activeCountList.add(activeCount);
        
        // Create the test quota checker instance that does NOT query the database.
        quotaChecker = new JobQuotaChecker(quotaInfolList, activeCountList);
        
        // Since each job has a system quota of 2 and there are now 2 active jobs,
        // we expect the system test and composite test to return true.
        for (JobQuotaInfo info : quotaInfolList) {
            boolean result = quotaChecker.exceedsSystemQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsSystemQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsSystemUserQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsSystemUserQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsQueueQuota(info);
            Assert.assertTrue(result, "Unexpected exceedsQueueQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsQueueUserQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsQueueUserQuota() result for " + info.getUuid());
            
            // All checks.
            result = quotaChecker.exceedsQuota(info);
            Assert.assertTrue(result, "Unexpected exceedsQuota() result for " + info.getUuid());
        }
        
        // -------------------- Context 3 --------------------
        activeCountList = new ArrayList<JobActiveCount>();
        activeCount = new JobActiveCount();
        activeCount.setTenantId("tenant1");
        activeCount.setOwner("owner1");
        activeCount.setExecutionSystem("execSystem1");
        activeCount.setQueueRequest("queue1");
        activeCount.setCount(3);      // Increased
        activeCountList.add(activeCount);
        
        // Create the test quota checker instance that does NOT query the database.
        quotaChecker = new JobQuotaChecker(quotaInfolList, activeCountList);
        
        // Since each job has a system quota of 2 and there are now 3 active jobs,
        // we expect the system test and composite test to return true.
        for (JobQuotaInfo info : quotaInfolList) {
            boolean result = quotaChecker.exceedsSystemQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsSystemQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsSystemUserQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsSystemUserQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsQueueQuota(info);
            Assert.assertTrue(result, "Unexpected exceedsQueueQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsQueueUserQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsQueueUserQuota() result for " + info.getUuid());
            
            // All checks.
            result = quotaChecker.exceedsQuota(info);
            Assert.assertTrue(result, "Unexpected exceedsQuota() result for " + info.getUuid());
        }
   }

    /* ---------------------------------------------------------------------- */
    /* maxQueueUser:                                                          */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void maxQueueUser() throws JobException 
    {   
        // Announce ourselves.
        System.out.println("-------- Running maxQueueUser");
        
        // -------------------- Context 1 --------------------
        ArrayList<JobActiveCount> activeCountList = new ArrayList<JobActiveCount>();
        JobActiveCount activeCount = new JobActiveCount();
        activeCount.setTenantId("tenant1");
        activeCount.setOwner("owner2");
        activeCount.setExecutionSystem("execSystem1");
        activeCount.setQueueRequest("queue1");
        activeCount.setCount(1);
        activeCountList.add(activeCount);
        
        // Create the test quota checker instance that does NOT query the database.
        List<JobQuotaInfo> quotaInfolList = getMaxQueueUserList();
        JobQuotaChecker quotaChecker = 
            new JobQuotaChecker(quotaInfolList, activeCountList);
        
        // Since each job has a system quota of 2 and there's only 1 active job,
        // we expect all tests to return false.
        for (JobQuotaInfo info : quotaInfolList) {
            boolean result = quotaChecker.exceedsSystemQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsSystemQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsSystemUserQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsSystemUserQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsQueueQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsQueueQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsQueueUserQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsQueueUserQuota() result for " + info.getUuid());
            
            // All checks.
            result = quotaChecker.exceedsQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsQuota() result for " + info.getUuid());
        }
        
        // -------------------- Context 2 --------------------
        activeCountList = new ArrayList<JobActiveCount>();
        activeCount = new JobActiveCount();
        activeCount.setTenantId("tenant1");
        activeCount.setOwner("owner1");
        activeCount.setExecutionSystem("execSystem1");
        activeCount.setQueueRequest("queue1");
        activeCount.setCount(2);      // Increased
        activeCountList.add(activeCount);
        
        // Create the test quota checker instance that does NOT query the database.
        quotaChecker = new JobQuotaChecker(quotaInfolList, activeCountList);
        
        // Since each job has a system quota of 2 and there are now 2 active jobs,
        // we expect the system test and composite test to return true.
        for (JobQuotaInfo info : quotaInfolList) {
            boolean result = quotaChecker.exceedsSystemQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsSystemQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsSystemUserQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsSystemUserQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsQueueQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsQueueQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsQueueUserQuota(info);
            Assert.assertTrue(result, "Unexpected exceedsQueueUserQuota() result for " + info.getUuid());
            
            // All checks.
            result = quotaChecker.exceedsQuota(info);
            Assert.assertTrue(result, "Unexpected exceedsQuota() result for " + info.getUuid());
        }
        
        // -------------------- Context 3 --------------------
        activeCountList = new ArrayList<JobActiveCount>();
        activeCount = new JobActiveCount();
        activeCount.setTenantId("tenant1");
        activeCount.setOwner("owner1");
        activeCount.setExecutionSystem("execSystem1");
        activeCount.setQueueRequest("queue1");
        activeCount.setCount(3);      // Increased
        activeCountList.add(activeCount);
        
        // Create the test quota checker instance that does NOT query the database.
        quotaChecker = new JobQuotaChecker(quotaInfolList, activeCountList);
        
        // Since each job has a system quota of 2 and there are now 3 active jobs,
        // we expect the system test and composite test to return true.
        for (JobQuotaInfo info : quotaInfolList) {
            boolean result = quotaChecker.exceedsSystemQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsSystemQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsSystemUserQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsSystemUserQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsQueueQuota(info);
            Assert.assertFalse(result, "Unexpected exceedsQueueQuota() result for " + info.getUuid());
            
            result = quotaChecker.exceedsQueueUserQuota(info);
            Assert.assertTrue(result, "Unexpected exceedsQueueUserQuota() result for " + info.getUuid());
            
            // All checks.
            result = quotaChecker.exceedsQuota(info);
            Assert.assertTrue(result, "Unexpected exceedsQuota() result for " + info.getUuid());
        }
   }

    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* getZeroFailList:                                                       */
    /* ---------------------------------------------------------------------- */
    /** Initialize a test job quota list. 
     * @throws JobException */
    private List<JobQuotaInfo> getZeroFailList() throws JobException
    {
        // Result list.
        ArrayList<JobQuotaInfo> quotaList = new ArrayList<>();
        
        // --- job1
        JobQuotaInfo info = new JobQuotaInfo();
        info.setUuid("job1");
        info.setTenantId("tenant1");
        info.setOwner("owner1");
        info.setExecutionSystem("execSystem1");
        info.setQueueRequest("queue1");
        info.setMaxSystemJobs(0);
        info.setMaxSystemUserJobs(0);
        info.setMaxQueueJobs(0);
        info.setMaxQueueUserJobs(0);
        quotaList.add(info);
        
        // --- job2
        info = new JobQuotaInfo();
        info.setUuid("job2");
        info.setTenantId("tenant1");
        info.setOwner("owner2");
        info.setExecutionSystem("execSystem1");
        info.setQueueRequest("queue1");
        info.setMaxSystemJobs(0);
        info.setMaxSystemUserJobs(1);
        info.setMaxQueueJobs(0);
        info.setMaxQueueUserJobs(1);
        quotaList.add(info);
        
        return quotaList;
    }

    /* ---------------------------------------------------------------------- */
    /* getUnlimitedList:                                                      */
    /* ---------------------------------------------------------------------- */
    /** Initialize a test job quota list. 
     * @throws JobException */
    private List<JobQuotaInfo> getUnlimitedList() throws JobException
    {
        // Result list.
        ArrayList<JobQuotaInfo> quotaList = new ArrayList<>();
        
        // --- job1
        JobQuotaInfo info = new JobQuotaInfo();
        info.setUuid("job1");
        info.setTenantId("tenant1");
        info.setOwner("owner1");
        info.setExecutionSystem("execSystem1");
        info.setQueueRequest("queue1");
        info.setMaxSystemJobs(-1);
        info.setMaxSystemUserJobs(-1);
        info.setMaxQueueJobs(-1);
        info.setMaxQueueUserJobs(-1);
        quotaList.add(info);
        
        // --- job2
        info = new JobQuotaInfo();
        info.setUuid("job2");
        info.setTenantId("tenant1");
        info.setOwner("owner2");
        info.setExecutionSystem("execSystem1");
        info.setQueueRequest("queue1");
        info.setMaxSystemJobs(-1);
        info.setMaxSystemUserJobs(-1);
        info.setMaxQueueJobs(-1);
        info.setMaxQueueUserJobs(1); // not unlimited
        quotaList.add(info);
        
        // --- job3
        info = new JobQuotaInfo();
        info.setUuid("job3");
        info.setTenantId("tenant1");
        info.setOwner("owner2");
        info.setExecutionSystem("execSystem1");
        info.setQueueRequest("queue1");
        info.setMaxSystemJobs(-1);
        info.setMaxSystemUserJobs(-1);
        info.setMaxQueueJobs(-1);
        info.setMaxQueueUserJobs(1); // not unlimited
        quotaList.add(info);
        
        return quotaList;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getMaxSystemList:                                                      */
    /* ---------------------------------------------------------------------- */
    /** Initialize a test job quota list. 
     * @throws JobException */
    private List<JobQuotaInfo> getMaxSystemList() throws JobException
    {
        // Result list.
        ArrayList<JobQuotaInfo> quotaList = new ArrayList<>();
        
        // --- job1
        JobQuotaInfo info = new JobQuotaInfo();
        info.setUuid("job1");
        info.setTenantId("tenant1");
        info.setOwner("owner1");
        info.setExecutionSystem("execSystem1");
        info.setQueueRequest("queue1");
        info.setMaxSystemJobs(2);
        info.setMaxSystemUserJobs(100);
        info.setMaxQueueJobs(100);
        info.setMaxQueueUserJobs(100);
        quotaList.add(info);
        
        // --- job2
        info = new JobQuotaInfo();
        info.setUuid("job2");
        info.setTenantId("tenant1");
        info.setOwner("owner1");
        info.setExecutionSystem("execSystem1");
        info.setQueueRequest("queue1");
        info.setMaxSystemJobs(2);
        info.setMaxSystemUserJobs(100);
        info.setMaxQueueJobs(100);
        info.setMaxQueueUserJobs(100);
        quotaList.add(info);
        return quotaList;
    }

    /* ---------------------------------------------------------------------- */
    /* getMaxSystemUserList:                                                  */
    /* ---------------------------------------------------------------------- */
    /** Initialize a test job quota list. 
     * @throws JobException */
    private List<JobQuotaInfo> getMaxSystemUserList() throws JobException
    {
        // Result list.
        ArrayList<JobQuotaInfo> quotaList = new ArrayList<>();
        
        // --- job1
        JobQuotaInfo info = new JobQuotaInfo();
        info.setUuid("job1");
        info.setTenantId("tenant1");
        info.setOwner("owner1");
        info.setExecutionSystem("execSystem1");
        info.setQueueRequest("queue1");
        info.setMaxSystemJobs(-1);
        info.setMaxSystemUserJobs(2);
        info.setMaxQueueJobs(-1);
        info.setMaxQueueUserJobs(-1);
        quotaList.add(info);
        
        // --- job2
        info = new JobQuotaInfo();
        info.setUuid("job2");
        info.setTenantId("tenant1");
        info.setOwner("owner1");
        info.setExecutionSystem("execSystem1");
        info.setQueueRequest("queue1");
        info.setMaxSystemJobs(-1);
        info.setMaxSystemUserJobs(2);
        info.setMaxQueueJobs(-1);
        info.setMaxQueueUserJobs(-1);
        quotaList.add(info);
        return quotaList;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getMaxQueueList:                                                       */
    /* ---------------------------------------------------------------------- */
    /** Initialize a test job quota list. 
     * @throws JobException */
    private List<JobQuotaInfo> getMaxQueueList() throws JobException
    {
        // Result list.
        ArrayList<JobQuotaInfo> quotaList = new ArrayList<>();
        
        // --- job1
        JobQuotaInfo info = new JobQuotaInfo();
        info.setUuid("job1");
        info.setTenantId("tenant1");
        info.setOwner("owner1");
        info.setExecutionSystem("execSystem1");
        info.setQueueRequest("queue1");
        info.setMaxSystemJobs(-1);
        info.setMaxSystemUserJobs(-1);
        info.setMaxQueueJobs(2);
        info.setMaxQueueUserJobs(-1);
        quotaList.add(info);
        
        // --- job2
        info = new JobQuotaInfo();
        info.setUuid("job2");
        info.setTenantId("tenant1");
        info.setOwner("owner1");
        info.setExecutionSystem("execSystem1");
        info.setQueueRequest("queue1");
        info.setMaxSystemJobs(-1);
        info.setMaxSystemUserJobs(-1);
        info.setMaxQueueJobs(2);
        info.setMaxQueueUserJobs(-1);
        quotaList.add(info);
        return quotaList;
    }
    
    /* ---------------------------------------------------------------------- */
    /* getMaxQueueUserList:                                                   */
    /* ---------------------------------------------------------------------- */
    /** Initialize a test job quota list. 
     * @throws JobException */
    private List<JobQuotaInfo> getMaxQueueUserList() throws JobException
    {
        // Result list.
        ArrayList<JobQuotaInfo> quotaList = new ArrayList<>();
        
        // --- job1
        JobQuotaInfo info = new JobQuotaInfo();
        info.setUuid("job1");
        info.setTenantId("tenant1");
        info.setOwner("owner1");
        info.setExecutionSystem("execSystem1");
        info.setQueueRequest("queue1");
        info.setMaxSystemJobs(100);
        info.setMaxSystemUserJobs(100);
        info.setMaxQueueJobs(100);
        info.setMaxQueueUserJobs(2);
        quotaList.add(info);
        
        // --- job2
        info = new JobQuotaInfo();
        info.setUuid("job2");
        info.setTenantId("tenant1");
        info.setOwner("owner1");
        info.setExecutionSystem("execSystem1");
        info.setQueueRequest("queue1");
        info.setMaxSystemJobs(100);
        info.setMaxSystemUserJobs(100);
        info.setMaxQueueJobs(100);
        info.setMaxQueueUserJobs(2);
        quotaList.add(info);
        return quotaList;
    }
}
