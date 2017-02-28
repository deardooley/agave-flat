package org.iplantc.service.jobs.callbacks;

import static org.iplantc.service.jobs.model.enumerations.JobStatusType.PENDING;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.jobs.dao.AbstractDaoTest;
import org.iplantc.service.jobs.exceptions.JobCallbackException;
import org.iplantc.service.jobs.model.JSONTestDataUtil;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups= {"callback", "status"}, singleThreaded=true)
public class JobCallbackTest extends AbstractDaoTest {
    
    private static final Logger log = Logger.getLogger(JobCallbackTest.class);
    
    public static final String DEFAULT_CALLBACK_TOKEN = "df1ce5b2-f093-4f2f-92d1-95b84d16abfe";
    
    
    @BeforeMethod
    public void beforeMethod() throws Exception {
        clearJobs();
        clearCurrentTenancyInfo();
    }

    @AfterMethod
    public void afterMethod() throws Exception {
        clearJobs();
        clearCurrentTenancyInfo();
    }
    
    @DataProvider(name="jobCallbackJobJobStatusTypeProvider")
    protected Object[][] jobCallbackJobJobStatusTypeProvider() {
        ArrayList<Object[]> testCases = new ArrayList<Object[]>();

        for (JobStatusType status : JobStatusType.values()) {
            testCases.add(new Object[] { previousState(status), status,
                    "Job should update to next natural state without an issue." });
        }
        return testCases.toArray(new Object[][] {});
    }

    @Test(dataProvider = "jobCallbackJobJobStatusTypeProvider")
    public void jobCallbackJobJobStatusType(JobStatusType previousStatus, JobStatusType newStatus, String message) 
    throws Exception
    {
        boolean shouldThrowException  = false;
        Job job = createJob(previousStatus);
        
        try {
            new JobCallback(job, newStatus);
        } catch (JobCallbackException e) {
            if (!shouldThrowException) {
                Assert.fail(message, e);
            }
        }
    }
    
    @DataProvider(name = "jobCallbackFailsOnBadStatusProvider")
    protected Object[][] jobCallbackFailsOnBadStatusProvider() {
        ArrayList<Object[]> testCases = new ArrayList<Object[]>();

        testCases.add(new Object[] { null });
        testCases.add(new Object[] { "" });
        testCases.add(new Object[] { "notarealStaTus" });
        
        return testCases.toArray(new Object[][] {});
    }

    @Test(dataProvider = "jobCallbackFailsOnBadStatusProvider")
    public void jobCallbackFailsOnBadStatus(String badStatus) {
        
        try {
            Job job = createJob(PENDING);
            new JobCallback(job.getUuid(), badStatus, job.getUpdateToken());
            
            Assert.fail("JobCallbackException should be thrown on bad status value " + badStatus);
        } 
        catch (PermissionException e) {
            Assert.fail("Permission exception should not be thrown on bad status value" , e);
        }
        catch (JobCallbackException e) {
            // success!
        }
        catch (Exception e) {
            Assert.fail("JobCallbackException should be thrown on bad status value " + badStatus, e);
        }
    }
    
    @DataProvider(name = "jobCallbackFailsOnBadUuidProvider")
    protected Object[][] jobCallbackFailsOnBadUuidProvider() {
        ArrayList<Object[]> testCases = new ArrayList<Object[]>();

        testCases.add(new Object[] { null, PENDING.name(), DEFAULT_CALLBACK_TOKEN, true, "Null uuid should throw exception" });
        testCases.add(new Object[] { "", PENDING.name(), DEFAULT_CALLBACK_TOKEN, true, "Empty uuid should throw exception" });
        testCases.add(new Object[] { "12345", PENDING.name(), DEFAULT_CALLBACK_TOKEN, true, "Invalid uuid should throw exception" });
        
        return testCases.toArray(new Object[][] {});
    }
    
    @Test(dataProvider = "jobCallbackFailsOnBadUuidProvider")
    public void jobCallbackFailsOnBadUuid(String uuid, String status, String callbackToken, boolean shouldThrowException, String message) {
        try 
        {
            new JobCallback(uuid, status, callbackToken);
        } 
        catch (Exception e) {
            if (!shouldThrowException) {
                Assert.fail(message, e);
            }
        }
    }

    @DataProvider(name = "jobCallbackFailsOnBadUpdateTokenProvider")
    protected Object[][] jobCallbackFailsOnBadUpdateTokenProvider() {
        ArrayList<Object[]> testCases = new ArrayList<Object[]>();

        testCases.add(new Object[] { null });
        testCases.add(new Object[] { "" });
        testCases.add(new Object[] { "notarealUpdateToken" });
        
        return testCases.toArray(new Object[][] {});
    }

    @Test(dataProvider = "jobCallbackFailsOnBadUpdateTokenProvider")
    public void jobCallbackFailsOnBadUpdateToken(String updateToken) {
        
        try {
            Job job = createJob(PENDING);
            new JobCallback(job.getUuid(), PENDING.name(), updateToken);
            
            Assert.fail("PermissionException should be thrown on bad updateToken value " + updateToken);
        } 
        catch (PermissionException e) {
            if (StringUtils.isEmpty(updateToken)) {
                // should throw PermissionException on mismatched token
                Assert.fail("PermissionException should not be thrown on bad updateToken value" , e);
            }
        }
        catch (JobCallbackException e) {
            if (StringUtils.isNotEmpty(updateToken)) {
                // should throw JobCallBackException on null or empty token
                Assert.fail("PermissionException should not be thrown on bad updateToken value" , e);
            }
        }
        catch (Exception e) {
            Assert.fail("PermissionException should be thrown on bad updateToken value " + updateToken, e);
        }
    }
    
    @Test()
    public void jobCallbackStringStringStringIgnoresTenancy() throws Exception {
        
        Job job = createJob(PENDING);
        
        TenancyHelper.setCurrentEndUser(JSONTestDataUtil.TEST_SHARED_OWNER);
        TenancyHelper.setCurrentTenantId("notwhatyouthinkitis");
        
        for (JobStatusType status : JobStatusType.values()) {
            try {
                new JobCallback(job.getUuid(), status.name(), job.getUpdateToken());
            } catch (JobCallbackException e) {
                Assert.fail("Callback should succeed for valid uuid, status, and updateToken", e);
            }
        }
    }
    
    @Test()
    public void jobCallbackStringStringString() throws Exception {
        
        Job job = createJob(PENDING);
        
        for (JobStatusType status : JobStatusType.values()) {
            try {
                new JobCallback(job.getUuid(), status.name(), job.getUpdateToken());
            } catch (JobCallbackException e) {
                Assert.fail("Callback should succeed for valid uuid, status, and updateToken", e);
            }
        }
    }
    
    @Test
    public void JobCallbackStringStringStringString() throws Exception {
        Job job = createJob(PENDING);
        
        for (JobStatusType status : JobStatusType.values()) {
            try {
                new JobCallback(job.getUuid(), status.name(), job.getUpdateToken(), null);
            } catch (JobCallbackException e) {
                Assert.fail("Callback should succeed for valid uuid, status, updateToken, and empty localid", e);
            }
            
            try {
                new JobCallback(job.getUuid(), status.name(), job.getUpdateToken(), "");
            } catch (JobCallbackException e) {
                Assert.fail("Callback should succeed for valid uuid, status, updateToken, and empty localid", e);
            }
        }
    }
    
    @Test
    public void JobCallbackStringStringStringStringIgnoresTenancy() throws Exception {
        Job job = createJob(PENDING);
        
        TenancyHelper.setCurrentEndUser(JSONTestDataUtil.TEST_SHARED_OWNER);
        TenancyHelper.setCurrentTenantId("notwhatyouthinkitis");
        
        for (JobStatusType status : JobStatusType.values()) {
            try {
                new JobCallback(job.getUuid(), status.name(), job.getUpdateToken(), null);
            } catch (JobCallbackException e) {
                Assert.fail("Callback should succeed for valid uuid, status, updateToken, and empty localid", e);
            }
            
            try {
                new JobCallback(job.getUuid(), status.name(), job.getUpdateToken(), "");
            } catch (JobCallbackException e) {
                Assert.fail("Callback should succeed for valid uuid, status, updateToken, and empty localid", e);
            }
        }
    }
    
    /**
     * The job state that logically comes directly before the 
     * current state. Some states such as {@link PAUSED}, 
     * {@link KILLED}, {@link STOPPED}, {@link FINISHED}, and
     * {@link FAILED} do not have a well-defined predecessor
     * so they fall back to {@link PENDING}.
     * 
     * Note that this method does not pull from the actual 
     * job history, but rather represents the backward
     * progression of a job in an ideal situation.
     *   
     * @return job state logically before this one.
     */
    private JobStatusType previousState(JobStatusType status)
    {
        if (status == JobStatusType.PENDING || status == JobStatusType.PROCESSING_INPUTS) {
            return JobStatusType.PENDING;
        } else if (status == JobStatusType.STAGING_INPUTS) {
            return JobStatusType.PROCESSING_INPUTS; 
        } else if (status == JobStatusType.STAGED) {
            return JobStatusType.STAGING_INPUTS;
        } else if (status == JobStatusType.STAGING_JOB) {
            return JobStatusType.STAGED;
        } else if (status == JobStatusType.SUBMITTING) {
            return JobStatusType.STAGING_JOB;
        } else if (status == JobStatusType.QUEUED) {
            return JobStatusType.SUBMITTING;
        } else if (status == JobStatusType.RUNNING) {
            return JobStatusType.QUEUED;
        } else if (status == JobStatusType.CLEANING_UP) {
            return JobStatusType.RUNNING;
        } else if (status == JobStatusType.ARCHIVING) {
            return JobStatusType.CLEANING_UP;
        } else if (status == JobStatusType.ARCHIVING_FAILED || status == JobStatusType.ARCHIVING_FINISHED) {
            return JobStatusType.ARCHIVING;
        } else { // PAUSED, KILLED, STOPPED, FINISHED, FAILED, HEARTBEAT
            return JobStatusType.PENDING;
        }
    }
}
