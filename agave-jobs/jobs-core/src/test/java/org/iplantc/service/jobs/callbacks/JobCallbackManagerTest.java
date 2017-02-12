package org.iplantc.service.jobs.callbacks;

import static org.iplantc.service.jobs.callbacks.JobCallbackTest.DEFAULT_CALLBACK_TOKEN;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.ARCHIVING;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.ARCHIVING_FAILED;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.ARCHIVING_FINISHED;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.CLEANING_UP;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.FINISHED;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.HEARTBEAT;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.PENDING;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.PROCESSING_INPUTS;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.QUEUED;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.RUNNING;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.jobs.dao.AbstractDaoTest;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobCallbackException;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.JSONTestDataUtil;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobEvent;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.statemachine.JobFSMUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class JobCallbackManagerTest extends AbstractDaoTest {

    private static final Logger log = Logger.getLogger(JobCallbackManagerTest.class);
    
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
    
    @Test()
    public void processNullCallbackFails() 
    throws Exception 
    {
        try 
        { 
            JobCallbackManager manager = new JobCallbackManager();
            manager.process(null);
            Assert.fail("Null callback should throw JobCallbackException");
        } catch (JobCallbackException e) {
            // success!
        } catch (Exception e) {
            Assert.fail("Null callback should throw JobCallbackException", e);
        }
    }
    
    @Test()
    public void processCallbackWithNullJobFails() 
    throws Exception 
    {
        Job job = createJob(PENDING);
        JobCallbackManager manager = new JobCallbackManager();
        
        try 
        { 
            JobCallback nullJobCallback = new JobCallback(job, PROCESSING_INPUTS);
            nullJobCallback.setJob(null);
            manager.process(nullJobCallback);
            Assert.fail("Null job in callback should throw JobCallbackException");
        } catch (JobCallbackException e) {
            // success!
        } catch (Exception e) {
            Assert.fail("Null job in callback should throw JobCallbackException", e);
        }
    }
    
    @Test()
    public void processCallbackWithNullStatusFails() 
    throws Exception 
    {
        Job job = createJob(PENDING);
        JobCallbackManager manager = new JobCallbackManager();
        
        try 
        { 
            JobCallback nullStatusCallback = new JobCallback(job, PROCESSING_INPUTS);
            nullStatusCallback.setStatus((String)null);
            manager.process(nullStatusCallback);
            Assert.fail("Null status in callback should throw JobCallbackException");
        } catch (JobCallbackException e) {
            // success!
        } catch (Exception e) {
            Assert.fail("Null status in callback should throw JobCallbackException", e);
        }
    }
    
    @DataProvider
    protected Object[][] processToNextNaturalStatusProvider() 
    {
        ArrayList<Object[]> testCases = new ArrayList<Object[]>();
        
        for(JobStatusType currentStatus: JobStatusType.values()) {
            if (currentStatus == HEARTBEAT) continue;
            
            for(JobStatusType nextStatus: JobStatusType.values())
            {
                if (nextStatus == HEARTBEAT) continue;
                
                if (JobFSMUtils.hasTransition(currentStatus, nextStatus)) {
                    testCases.add(new Object[]{currentStatus, nextStatus, false, "Attempting to update status from " + currentStatus.name() + " to " + nextStatus.name() + " should succeed." });
                } else {
                    testCases.add(new Object[]{currentStatus, nextStatus, true, "Attempting to update status from " + currentStatus.name() + " to " + nextStatus.name() + " should throw an exception" });
                }
            }
        }
        return testCases.toArray(new Object[][] {});
    }
    
    @Test(dataProvider="processToNextNaturalStatusProvider")
    public void processCallbackToNextNaturalStatus(JobStatusType currentStatus, JobStatusType newStatus, boolean shouldThrowException, String message) 
    throws Exception 
    {
        Job job = createJob(currentStatus);
        JobCallback callback = new JobCallback(job, newStatus);
        JobCallbackManager manager = new JobCallbackManager();
        
        try 
        {
            Job updatedJob = manager.process(callback);
            Assert.assertFalse(shouldThrowException, message);
            Assert.assertNotNull(updatedJob, "No job returned from processing callback");
            Assert.assertEquals(updatedJob.getStatus(), (newStatus == HEARTBEAT ? currentStatus : newStatus), message);
        } 
        catch(JobException | JobCallbackException e) {
            if (!shouldThrowException) {
                Assert.fail(message, e);
            }
        }
    }
    
    @DataProvider
    protected Object[][] processCallbackToNextNaturalStatusCreatesOrderedEventsProvider() 
    {
        ArrayList<Object[]> testCases = new ArrayList<Object[]>();
        
        for(JobStatusType currentStatus: JobStatusType.values()) 
        {
            if (currentStatus == HEARTBEAT) continue;
            
            for(JobStatusType nextStatus: JobStatusType.values())
            {
                if (nextStatus == HEARTBEAT) continue;
                
                if (!JobFSMUtils.hasTransition(currentStatus, nextStatus)) {
                    testCases.add(new Object[]{currentStatus, nextStatus, 
                            "Attempting to update status from " + currentStatus.name() + " to " 
                            + nextStatus.name() + " should append matching job event to job history." });
                }
            }
        }
        return testCases.toArray(new Object[][] {});
    }
    
    @Test(dataProvider="processCallbackToNextNaturalStatusCreatesOrderedEventsProvider", enabled=true)
    public void processCallbackToNextNaturalStatusCreatesOrderedEvents(JobStatusType currentStatus, JobStatusType newStatus, String message) 
    throws Exception 
    {
        Job job = createJob(currentStatus);
        JobCallback callback = new JobCallback(job, newStatus);
        JobCallbackManager manager = new JobCallbackManager();
        
        try 
        {
            Job updatedJob = manager.process(callback);
            Assert.assertNotNull(updatedJob, "No job returned from processing callback");
            
            // verify events were created
            List<JobEvent> events = updatedJob.getEvents();
            Assert.assertFalse(events.isEmpty(), "No job event was written by status update");
            
            if (newStatus == currentStatus) 
            {
                Assert.assertEquals(events.get(events.size() -2).getStatus(), newStatus.name(), 
                        "Next to last job event after updating non-archiving job with status " + newStatus.name() + " to the same status should have status " + newStatus.name() + ".");
                Assert.assertEquals(events.get(events.size() -1).getStatus(), newStatus.name(), 
                        "Last job event after updating non-archiving job with status " + newStatus.name() + " to the same status should have status " + newStatus.name() + ".");
                Assert.assertEquals(events.get(events.size() -1).getDescription(), "Job receieved duplicate " + newStatus.name() + " notification", 
                        "Last job event after updating non-archiving job with status " + newStatus.name() + " to the same status should be a redundant job status update message.");
            } 
            else { 
                Assert.assertEquals(events.get(events.size() -1).getStatus(), newStatus.name(), 
                    "Updated job event was not the last one written.");
            }
        } 
        catch(JobException | JobCallbackException e) {
            Assert.fail(message, e);
        }
    }
    
    @DataProvider
    protected Object[][] processCallbackArchivingStatusFailsInNonArchivingJobProvider() 
    {
        ArrayList<Object[]> testCases = new ArrayList<Object[]>();
        
        for(JobStatusType currentStatus: JobStatusType.values()) 
        {
            if (HEARTBEAT == currentStatus || Arrays.asList(ARCHIVING, ARCHIVING_FINISHED, ARCHIVING_FAILED).contains(currentStatus)) continue;
            
            for(JobStatusType nextStatus: JobStatusType.values())
            {
                if (HEARTBEAT == nextStatus || !Arrays.asList(ARCHIVING, ARCHIVING_FINISHED, ARCHIVING_FAILED).contains(nextStatus)) continue;
                
                if (!JobFSMUtils.hasTransition(currentStatus, nextStatus)) {
                    testCases.add(new Object[]{currentStatus, nextStatus, 
                            "Attempting to update non-archiving job status from " + currentStatus.name() + " to " 
                            + nextStatus.name() + " should throw JobCallbackException." });
                }
            }
        }
        return testCases.toArray(new Object[][] {});
    }
    
    @Test(dataProvider="processCallbackArchivingStatusFailsInNonArchivingJobProvider", enabled=true)
    public void processCallbackArchivingStatusFailsInNonArchivingJob(JobStatusType currentStatus, JobStatusType newStatus, String message) 
    throws Exception 
    {
        Job job = createJob(currentStatus, false); // don't save yet
        job.setArchiveOutput(false);
        JobDao.create(job);
        JobCallback callback = new JobCallback(job, newStatus);
        JobCallbackManager manager = new JobCallbackManager();
        
        try 
        {
            manager.process(callback);
            Assert.fail(message);
        } 
        catch(JobCallbackException e) {
            // expected
        }
        catch(Exception e) {
            Assert.fail(message, e);
        }
    }
    
    @DataProvider
    protected Object[][] processCallbackToNextNaturalStatusCreatesOrderedEventsInNonArchivingJobProvider() 
    {
        ArrayList<Object[]> testCases = new ArrayList<Object[]>();
        
        for(JobStatusType currentStatus: JobStatusType.values()) 
        {
            if (currentStatus == HEARTBEAT) continue;
            
            for(JobStatusType nextStatus: JobStatusType.values())
            {
                if (Arrays.asList(HEARTBEAT, ARCHIVING, ARCHIVING_FINISHED, ARCHIVING_FAILED).contains(nextStatus)) continue;
                
                if (!JobFSMUtils.hasTransition(currentStatus, nextStatus)) {
                    testCases.add(new Object[]{currentStatus, nextStatus, 
                            "Attempting to update status from " + currentStatus.name() + " to " 
                            + nextStatus.name() + " should append matching job event to job history." });
                }
            }
        }
        return testCases.toArray(new Object[][] {});
    }
    
    @Test(dataProvider="processCallbackToNextNaturalStatusCreatesOrderedEventsInNonArchivingJobProvider", enabled=true)
    public void processCallbackToNextNaturalStatusCreatesOrderedEventsInNonArchivingJob(JobStatusType currentStatus, JobStatusType newStatus, String message) 
    throws Exception 
    {
        Job job = createJob(currentStatus, false); // don't save yet
        job.setArchiveOutput(false);
        JobDao.create(job);
        JobCallback callback = new JobCallback(job, newStatus);
        JobCallbackManager manager = new JobCallbackManager();
        
        try 
        {
            Job updatedJob = manager.process(callback);
            Assert.assertNotNull(updatedJob, "No job returned from processing callback");
            if (newStatus == CLEANING_UP) {
                Assert.assertEquals(updatedJob.getStatus(), FINISHED, 
                        "Updating non-archiving job from " + currentStatus.name() + " to " 
                                + newStatus.name() + " should set job status to FINISHED.");
            } else {
                Assert.assertEquals(updatedJob.getStatus(), newStatus, 
                        "Updating non-archiving job from " + currentStatus.name() + " to " 
                                + newStatus.name() + " should set job status to " + newStatus.name() + ".");
            }
            
            // verify events were created
            List<JobEvent> events = updatedJob.getEvents();
            Assert.assertFalse(events.isEmpty(), "No job event was written by status update");
            if (newStatus == CLEANING_UP) {
                if (currentStatus == CLEANING_UP) {
                    Assert.assertEquals(events.get(events.size() -3).getStatus(), newStatus.name(), 
                            "Second to last job event after updating non-archiving job with status " + newStatus.name() + " to the same status should have status " + newStatus.name() + ".");
                    Assert.assertEquals(events.get(events.size() -2).getStatus(), newStatus.name(), 
                            "Next to last job event after updating non-archiving job with status " + newStatus.name() + " to the same status should have status " + newStatus.name() + ".");
                    Assert.assertEquals(events.get(events.size() -2).getDescription(), "Job receieved duplicate " + newStatus.name() + " notification", 
                            "Next to last job event after updating non-archiving job with status " + newStatus.name() + " to the same status should be a redundant job status update message.");
                    Assert.assertEquals(events.get(events.size() -1).getStatus(), FINISHED.name(), 
                            "Last job event after updating non-archiving job to CLEANING_UP should have status FINISHED.");
                } else {
                    Assert.assertEquals(events.get(events.size() -2).getStatus(), CLEANING_UP.name(), 
                            "Next to last job event after updating non-archiving job to CLEANING_UP should have status CLEANING_UP.");
                    Assert.assertEquals(events.get(events.size() -1).getStatus(), FINISHED.name(), 
                            "Last job event after updating non-archiving job to CLEANING_UP should have status FINISHED.");
                }
            } else if (newStatus == currentStatus) {
                Assert.assertEquals(events.get(events.size() -2).getStatus(), newStatus.name(), 
                        "Next to last job event after updating non-archiving job with status " + newStatus.name() + " to the same status should have status " + newStatus.name() + ".");
                Assert.assertEquals(events.get(events.size() -1).getStatus(), newStatus.name(), 
                        "Last job event after updating non-archiving job with status " + newStatus.name() + " to the same status should have status " + newStatus.name() + ".");
                Assert.assertEquals(events.get(events.size() -1).getDescription(), "Job receieved duplicate " + newStatus.name() + " notification", 
                        "Last job event after updating non-archiving job with status " + newStatus.name() + " to the same status should be a redundant job status update message.");
            } else {
                Assert.assertEquals(events.get(events.size() -1).getStatus(), newStatus.name(), 
                        "Last job event after updating non-archiving job to " + newStatus.name() + " should have status " + newStatus.name() + ".");
            }
        } 
        catch(JobException | JobCallbackException e) {
            Assert.fail(message, e);
        }
    }
    
    @DataProvider
    protected Object[][] processHeartbeatCallbackProvider() {
        ArrayList<Object[]> testCases = new ArrayList<Object[]>();
        
        for(JobStatusType currentStatus: JobStatusType.values()) 
        {
            if (currentStatus == HEARTBEAT) continue;
            
            if (!JobFSMUtils.hasTransition(currentStatus, HEARTBEAT)) {
                testCases.add(new Object[]{currentStatus, false, 
                        "Attempting to update status from " + currentStatus.name() + " to " 
                        + HEARTBEAT.name() + " should succeed." });
            } else {
                testCases.add(new Object[]{currentStatus, true, 
                        "Attempting to update status from " + currentStatus.name() + " to " 
                        + HEARTBEAT.name() + " should throw an exception" });
            }
        }
        return testCases.toArray(new Object[][] {});
    }
    
    @Test(dataProvider="processHeartbeatCallbackProvider", enabled=true)
    public void processHeartbeatCallback(JobStatusType currentStatus, boolean shouldThrowException, String message) 
    throws Exception 
    {
        Job job = createJob(currentStatus);
        int originalEventCount = job.getEvents().size();
        JobCallback callback = new JobCallback(job, HEARTBEAT);
        JobCallbackManager manager = new JobCallbackManager();
        
        try 
        {
            Job updatedJob = manager.process(callback);
            Assert.assertFalse(shouldThrowException, message);
            Assert.assertNotNull(updatedJob, "No job returned from processing callback");
            Assert.assertEquals(updatedJob.getStatus(), currentStatus, message);
            
            // verify events were created
            List<JobEvent> events = updatedJob.getEvents();
            Assert.assertFalse(events.isEmpty(), "No HEARTBEAT job event was written by status update");
            Assert.assertTrue(events.size() > originalEventCount, 
                    "Valid heartbeat event should be written to job event history when received.");
            Assert.assertEquals(events.get(events.size() -1).getStatus(), HEARTBEAT.name(), 
                    "Updated job event was not the last one written.");
        } 
        catch(JobException | JobCallbackException e) {
            if (!shouldThrowException) {
                Assert.fail(message, e);
            }
        }
    }
    
    @DataProvider(name = "validateLocalSchedulerJobIdProvider")
    protected Object[][] validateLocalSchedulerJobIdProvider() {
        ArrayList<Object[]> testCases = new ArrayList<Object[]>();

        testCases.add(new Object[] { null, null, null, false, "Updating null local schedulerId to null should not throw an exception" });
        testCases.add(new Object[] { "", null, null, false, "Updating null local schedulerId to empty string should not throw an exception" });
        testCases.add(new Object[] { DEFAULT_CALLBACK_TOKEN, null, DEFAULT_CALLBACK_TOKEN, false, "Updating null local schedulerId to valid scheduler string should not throw an exception" });
        testCases.add(new Object[] { null, DEFAULT_CALLBACK_TOKEN, DEFAULT_CALLBACK_TOKEN, false, "Updating existing local schedulerId to null should not throw an exception" });
        testCases.add(new Object[] { "", DEFAULT_CALLBACK_TOKEN, DEFAULT_CALLBACK_TOKEN, false, "Updating existing local schedulerId to empty value should not throw an exception" });
        testCases.add(new Object[] { "12345", DEFAULT_CALLBACK_TOKEN, DEFAULT_CALLBACK_TOKEN, true, "Updating existing local schedulerId to new value should throw an exception" });
        testCases.add(new Object[] { DEFAULT_CALLBACK_TOKEN, DEFAULT_CALLBACK_TOKEN, DEFAULT_CALLBACK_TOKEN, false, "Updating existing local schedulerId to identical value should not throw an exception" });
        
        return testCases.toArray(new Object[][] {});
    }

    @Test(dataProvider="validateLocalSchedulerJobIdProvider", enabled=true)
    public void validateLocalSchedulerJobId(String newLocalSchedulerJobId, String currentLocalSchedulerJobId, String expectedLocalSchedulerJobId, boolean shouldThrowException, String message)
    throws Exception 
    {
        Job job = createJob(QUEUED, false); // don't save yet
        job.setLocalJobId(currentLocalSchedulerJobId);
        JobDao.create(job);
        
        JobCallback callback = new JobCallback(job, RUNNING);
        callback.setLocalSchedulerJobId(newLocalSchedulerJobId);
        JobCallbackManager manager = new JobCallbackManager();
        
        try 
        {
            Job updatedJob = manager.process(callback);
            Assert.assertFalse(shouldThrowException, message);
            Assert.assertEquals(updatedJob.getLocalJobId(), expectedLocalSchedulerJobId, message);
        } 
        catch(JobException | JobCallbackException e) {
            if (!shouldThrowException) {
                Assert.fail(message, e);
            }
        }
    }
    
    @Test(enabled=true)
    public void processIgnoresTenancy()
    throws Exception 
    {
        Job job = createJob(QUEUED, false); // don't save yet
        job.setLocalJobId(null);
        JobDao.create(job);
        
        JobCallback callback = new JobCallback(job, RUNNING);
        callback.setLocalSchedulerJobId(DEFAULT_CALLBACK_TOKEN);
        
        JobCallbackManager manager = new JobCallbackManager();
        
        TenancyHelper.setCurrentEndUser(JSONTestDataUtil.TEST_SHARED_OWNER);
        TenancyHelper.setCurrentTenantId("notwhatyouthinkitis");
        
        try 
        {
            Job updatedJob = manager.process(callback);
            
            Assert.assertEquals(updatedJob.getLocalJobId(), DEFAULT_CALLBACK_TOKEN, 
                "Updating null local schedulerId to valid scheduler string should not "
                + "throw an exception regardless of tenant");
            
            Assert.assertEquals(updatedJob.getStatus().name(), RUNNING.name(), 
                    "Updating archiving job from " + QUEUED.name() + " to " 
                            + RUNNING.name() + " should set job status to " + RUNNING.name() + ".");
        } 
        catch(JobException | JobCallbackException e) {
            Assert.fail("Processing callback should work regardless of tenant.", e);
        }
    }
}
