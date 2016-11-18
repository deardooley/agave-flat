package org.iplantc.service.jobs.dao;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.iplantc.service.jobs.model.JobLease;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** This test suite does not use any Agave services and should be run when 
 * the Agave jobs service is not running.  The set-up and tear-down methods
 * release any existing leases.
 * 
 * @author rcardone
 */
public class JobLeaseDaoTest {
    /* ********************************************************************** */
    /*                             Static Fields                              */
    /* ********************************************************************** */   
    // Special purpose queue names used in multiple places.
    private static final String LESSEE_1 = "JobLeaseDaoTest-Lessee-1";
    private static final String LESSEE_THREAD_1 = "JobLeaseDaoTest-Lessee-Thread-1";
    private static final String LESSEE_THREAD_2 = "JobLeaseDaoTest-Lessee-Thread-2";
    
    // AlternateThread test timeouts.
    private static final int LEASE_HOLD_MILLIS = 5000;
    
    /* ********************************************************************** */
    /*                                 Fields                                 */
    /* ********************************************************************** */
    // ------------------ alternateThreadTest Fields ------------------
    // Lock used for thread synchronization in alternateThreadTest.
    private final ReentrantLock _altThreadLock = new ReentrantLock();
    
    // Timestamps used for thread checking in alternateThreadTest.
    private long _thread1Timestamp1;
    private Throwable _thread1Exception;
    private Throwable _thread2Exception;
    
    /* ********************************************************************** */
    /*                            Set Up / Tear Down                          */
    /* ********************************************************************** */    
    /* ---------------------------------------------------------------------- */
    /* setup:                                                                 */
    /* ---------------------------------------------------------------------- */
    @BeforeMethod
    private void setup()
    {
        // Destroy existing leases.
        JobLeaseDao.clearLeases();
    }
    
    /* ---------------------------------------------------------------------- */
    /* teardown:                                                              */
    /* ---------------------------------------------------------------------- */
    @BeforeMethod
    private void teardown()
    {
        // Destroy existing leases.
        JobLeaseDao.clearLeases();
    }
    
    /* ********************************************************************** */
    /*                              Test Methods                              */
    /* ********************************************************************** */    
    /* ---------------------------------------------------------------------- */
    /* getAndReleaseLease:                                                    */
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

    /* ---------------------------------------------------------------------- */
    /* alternateThreadTest:                                                   */
    /* ---------------------------------------------------------------------- */
    /** This test consists of 2 threads.  The first one gets a lease, holds on
     * to it for 5 seconds and then releases it.  The second thread tries to 
     * get the lease and is expected to succeed shortly after the first thread
     * releases it.  Error reporting, synchronization and data sharing take 
     * place using instance fields.
     */
    @Test(enabled=true)
    public void alternateThreadTest()
    {   
        // Start thread 1.
        Thread1 thread1 = new Thread1();
        thread1.setUncaughtExceptionHandler(thread1);
        thread1.setDaemon(true);
        thread1.start();
        
        // Wait for thread 1 to acquire test lock.
        while (!_altThreadLock.isLocked()) {
            try {Thread.sleep(10);}
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Start thread 2.
        Thread2 thread2 = new Thread2();
        thread2.setUncaughtExceptionHandler(thread2);
        thread2.setDaemon(true);
        thread2.start();

        // Wait a limited period for both threads to terminate.
        try {
            thread1.join(30000);
            thread2.join(30000);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // See if we failed.
        if (_thread1Exception != null) 
            Assert.fail("Thread 1 failure: " + _thread1Exception.getMessage());
        if (_thread2Exception != null) 
            Assert.fail("Thread 2 failure: " + _thread2Exception.getMessage());
    }
    
    /* ---------------------------------------------------------------------- */
    /* getAndClearLeasesTest:                                                 */
    /* ---------------------------------------------------------------------- */
    /** Test the mass retrieve and clear methods.  We first acquire every type
     * of lease and then clear all leases.
     */
    @Test(enabled=true)
    public void getAndClearLeasesTest()
    {   
        // Initialize the dao object.
        JobLeaseDao dao1 = new JobLeaseDao(JobPhaseType.STAGING, LESSEE_1);
        JobLeaseDao dao2 = new JobLeaseDao(JobPhaseType.SUBMITTING, LESSEE_1);
        JobLeaseDao dao3 = new JobLeaseDao(JobPhaseType.MONITORING, LESSEE_1);
        JobLeaseDao dao4 = new JobLeaseDao(JobPhaseType.ARCHIVING, LESSEE_1);
        
        // Acquire the lease.
        boolean haveLease1 = dao1.acquireLease();
        Assert.assertTrue(haveLease1, "FAILED to acquire STAGING lease for " + LESSEE_1);
        boolean haveLease2 = dao2.acquireLease();
        Assert.assertTrue(haveLease2, "FAILED to acquire SUBMITTING lease for " + LESSEE_1);
        boolean haveLease3 = dao3.acquireLease();
        Assert.assertTrue(haveLease3, "FAILED to acquire MONITORING lease for " + LESSEE_1);
        boolean haveLease4 = dao4.acquireLease();
        Assert.assertTrue(haveLease4, "FAILED to acquire ARCHIVING lease for " + LESSEE_1);
        
        // Get all leases and check the lessee.
        List<JobLease> list = JobLeaseDao.getLeases();
        Assert.assertEquals(list.size(), 4, "Unexpected number of leases retrieved.");
        for (JobLease lease : list) {
            Assert.assertTrue(LESSEE_1.equals(lease.getLessee()), 
                    "Unexpected lessee retrieved: " + lease.getLessee());
            Assert.assertNotNull(lease.getExpiresAt(), 
                    "Unexpected lessee retrieved: " + lease.getLessee());
        }
        
        // Clear all leases.
        int changed = JobLeaseDao.clearLeases();
        Assert.assertEquals(changed, 4, "Unexpected number of leases changed.");
        
        // Check that the leases are cleared.
        list = JobLeaseDao.getLeases();
        Assert.assertEquals(list.size(), 4, "(2) Unexpected number of leases retrieved.");
        for (JobLease lease : list) {
            Assert.assertNull(lease.getLessee(), 
                    "Unexpected lessee retrieved: " + lease.getLessee());
            Assert.assertNull(lease.getExpiresAt(), 
                    "Unexpected expiresAt date retrieved: " + lease.getExpiresAt());
        }        
    }
    
    /* ********************************************************************** */
    /*                              Thread1 Class                             */
    /* ********************************************************************** */ 
    private final class Thread1
     extends Thread
     implements Thread.UncaughtExceptionHandler
    {
        // Constructor.
        public Thread1(){super("AltThreadTest-1");}
        
        @Override
        public void run()
        {
            // Grab the lock before thread 2 starts.
            _altThreadLock.lock();
            
            // Initialize the dao object.
            JobLeaseDao dao = new JobLeaseDao(JobPhaseType.STAGING, LESSEE_THREAD_1);
            
            // Acquire the lease.
            boolean haveLease = dao.acquireLease();
            Assert.assertTrue(haveLease, "FAILED to acquire lease for " + LESSEE_THREAD_1);
            _thread1Timestamp1 = System.currentTimeMillis();
            
            // Release the lock so thread 2 can start work.
            _altThreadLock.unlock();
            
            // Sleep for a little while.
            try {Thread.sleep(LEASE_HOLD_MILLIS);}
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // Now release the lease.
            dao.releaseLease();
        }

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            _thread1Exception = e;
        }
    }
    
    /* ********************************************************************** */
    /*                              Thread1 Class                             */
    /* ********************************************************************** */ 
    private final class Thread2
    extends Thread
    implements Thread.UncaughtExceptionHandler
   {
       // Constructor.
       public Thread2(){super("AltThreadTest-2");}
        
       @Override
       public void run()
       {
           // Initialize the dao object.
           JobLeaseDao dao = new JobLeaseDao(JobPhaseType.STAGING, LESSEE_THREAD_2);
           
           // Wait until we can acquire the lock.
           _altThreadLock.lock();
           
           // Acquire the lease by trying until we get it.
           boolean haveLease = false;
           while (true) {
               // Try to get the lease.
               haveLease = dao.acquireLease();
               if (haveLease) break;
               
               // Sleep for a while 
               try {Thread.sleep(100);}
               catch (InterruptedException e) {
                   e.printStackTrace();
               }
           }
           Assert.assertTrue(haveLease, "FAILED to acquire lease for " + LESSEE_THREAD_2);
           
           // Get the current time.
           long acqureTime = System.currentTimeMillis();
           
           // Release the lease before the final check to avoid orphaning it.
           dao.releaseLease();
           
           // Determine if we got the lease within a reasonable window.
           long lockoutPeriod = acqureTime - _thread1Timestamp1;
           System.out.println("Thread2 lockout period in milliseconds: " + lockoutPeriod);
           
           // Assert that we got the lease within a second of the minimum possible time
           // given that Thread1 held on to if for LEASE_HOLD_MILLIS.
           Assert.assertTrue((lockoutPeriod > LEASE_HOLD_MILLIS) && (lockoutPeriod < LEASE_HOLD_MILLIS + 1000),
                          "Thread2 was locked out for an unexpected number of milliseconds: " + lockoutPeriod);
       }
       
       @Override
       public void uncaughtException(Thread t, Throwable e) {
           _thread2Exception = e;
       }
   }
}
