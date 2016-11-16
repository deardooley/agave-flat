package org.iplantc.service.jobs.dao;

import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.testng.Assert;
import org.testng.annotations.Test;

public class JobLeaseDaoTest {
    /* ********************************************************************** */
    /*                             Static Fields                              */
    /* ********************************************************************** */   
    // Special purpose queue names used in multiple places.
    private static final String LESSEE_1 = "JobLeaseDaoTest-Lessee-1";
    
    /* ********************************************************************** */
    /*                              Test Methods                              */
    /* ********************************************************************** */    
    /* ---------------------------------------------------------------------- */
    /* createIplantQueues:                                                    */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void getAndReleaseLease()
    {   
        // Initialize the dao object.
        JobLeaseDao dao = new JobLeaseDao(JobPhaseType.STAGING, LESSEE_1);
        
        // Acquire the lease.
        boolean haveLease = dao.acquireLease();
        Assert.assertTrue(haveLease, "FAILED to acquire lease for " + LESSEE_1);
        
        // Sleep for a little while.
        try {Thread.sleep(3000);}
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Renew the lease.
        haveLease = dao.acquireLease();
        Assert.assertTrue(haveLease, "FAILED to renew lease for " + LESSEE_1);
        
        // Release lease.
        boolean releasedLease = dao.releaseLease();
        Assert.assertTrue(releasedLease, "FAILED to acquire release for " + LESSEE_1);
    }

}
