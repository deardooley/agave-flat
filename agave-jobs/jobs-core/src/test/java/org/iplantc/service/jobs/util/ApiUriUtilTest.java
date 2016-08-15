package org.iplantc.service.jobs.util;

import static org.iplantc.service.jobs.model.enumerations.JobStatusType.FINISHED;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.PAUSED;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.RUNNING;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.STOPPED;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.managers.JobPermissionManager;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.model.enumerations.PermissionType;
import org.iplantc.service.jobs.submission.AbstractJobSubmissionTest;
import org.iplantc.service.systems.Settings;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.util.ApiUriUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups={"broken"})
public class ApiUriUtilTest extends AbstractJobSubmissionTest
{
	public static final String SYSTEM_SHARED_USER = "testshareuser";
	public static final String SYSTEM_OTHER_USER = "testotheruser";

	private static final String JOB_OUTPUT_RELATIVE_PATH = "some/path/to/file";
	private static final String JOB_OUTPUT_ABSOLUTE_PATH = "/some/path/to/file";
	
	@BeforeClass
	public void beforeClass() throws Exception {
	    
	    super.beforeClass();
	}
	
	@AfterClass
    public void afterClass() throws Exception
    {
        super.afterClass();
    }
	
	@DataProvider(name="getSystemFollowsJobStatusProvider", parallel=false)
    protected Object[][] getSystemFollowsJobStatusProvider() throws Exception
    {
        Software privateSoftware = SoftwareDao.getUserApps(SYSTEM_OWNER, false).get(0);
        
        List<Object[]> testCases = new ArrayList<Object[]>();
        
        for (JobStatusType testStatus: JobStatusType.values()) 
        {   
            Job archivingJob = createJob(testStatus, privateSoftware, SYSTEM_OWNER);
            Job noArchiveJob = createJob(testStatus, privateSoftware, SYSTEM_OWNER);
            noArchiveJob.setArchiveOutput(false);
            
            if (JobStatusType.isRunning(testStatus) 
                    || JobStatusType.isFailed(testStatus) 
                    || testStatus == STOPPED
                    || testStatus == PAUSED) 
            {
                testCases.add(new Object[]{ archivingJob, privateSoftware.getExecutionSystem(), false, "Job output url should return job execution system for archiving job when status is " + testStatus.name()});
            } else {
                testCases.add(new Object[]{ archivingJob, archivingJob.getArchiveSystem(), false, "Job output url should return job archive system for archiving job when status is " + testStatus.name()});
            }
            
            testCases.add(new Object[]{ noArchiveJob, privateSoftware.getExecutionSystem(), false, "Job output url should always return job execution system for non-archiving jobs"});
        }
        
        return testCases.toArray(new Object[][] {});
    }
	
	@Test(dataProvider="getSystemFollowsJobStatusProvider")
	public void getSystemFollowsJobStatus(Job job, RemoteSystem expectedSystem, boolean shouldThrowException, String message) 
	throws Exception 
	{
	    try 
	    {
	        JobDao.persist(job);
	        
	        String outputUrl = TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE) + job.getUuid() 
	                + "/outputs/media/" + JOB_OUTPUT_RELATIVE_PATH;
	        RemoteSystem system = ApiUriUtil.getRemoteSystem(job.getOwner(), URI.create(outputUrl));
	        
	        Assert.assertNotNull(system, "Null system should not be returned from ApiUriUtil#getRemoteSystem(String,URI)");
	        Assert.assertEquals(system, expectedSystem, message);
	        
	    }
	    catch (Exception e) {
	        Assert.assertTrue(shouldThrowException, e.getMessage());
	    }
	    finally {
	        try { JobDao.delete(job); } catch (Exception e) {}
	    }
	}
	
//	@Test(dependsOnMethods={"getSystemFollowsJobStatus"})
//    public void getPathIndependeOfJobStatus() 
//    throws Exception 
//    {
//	    Software privateSoftware = SoftwareDao.getUserApps(SYSTEM_OWNER, false).get(0);
//	    
//	    String outputUrl = null;
//        String path = null;
//        for (JobStatusType testStatus: JobStatusType.values()) 
//        {   
//            Job archivingJob = null;
//            Job noArchiveJob = null;
//            
//            try
//            {   
//                // archived job
//                archivingJob = createJob(testStatus, privateSoftware, SYSTEM_OWNER);
//                JobDao.persist(archivingJob);
//                
//                outputUrl = TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE) 
//                        + archivingJob.getUuid() + "/outputs/media/" + JOB_OUTPUT_RELATIVE_PATH;
//                path = ApiUriUtil.getPath(URI.create(outputUrl));
//                
//                Assert.assertNotNull(path, "Null system should never be returned from ApiUriUtil#getRemoteSystem(String,URI)");
//                Assert.assertEquals(path, JOB_OUTPUT_RELATIVE_PATH, "ApiUriUtil.getPath(URI) should not change in archving job regardless of job status");
//                
//                // non archived job
//                noArchiveJob = createJob(testStatus, privateSoftware, SYSTEM_OWNER);
//                noArchiveJob.setArchiveOutput(false);
//                JobDao.persist(noArchiveJob);
//                
//                outputUrl = TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE) 
//                        + noArchiveJob.getUuid() + "/outputs/media/" + JOB_OUTPUT_RELATIVE_PATH;
//                path = ApiUriUtil.getPath(URI.create(outputUrl));
//            
//                Assert.assertNotNull(path, "Null system should never be returned from ApiUriUtil#getRemoteSystem(String,URI)");
//                Assert.assertEquals(path, JOB_OUTPUT_RELATIVE_PATH, "ApiUriUtil.getPath(URI) should not change in non-archving job regardless of job status");
//                
//            }
//            finally {
//                try { JobDao.delete(archivingJob); } catch (Exception e) {}
//                try { JobDao.delete(noArchiveJob); } catch (Exception e) {}
//            }
//        }
//        
//    }
//	
//	@Test(dependsOnMethods={"getPathIndependeOfJobStatus"})
//    public void getAbsolutePathFollowsStatus() 
//    throws Exception 
//    {
//        Software privateSoftware = SoftwareDao.getUserApps(SYSTEM_OWNER, false).get(0);
//        
//        String outputUrl = null;
//        String absolutePath = null;
//        String expectedPath = null;
//        for (JobStatusType testStatus: JobStatusType.values()) 
//        {   
//            Job archivingJob = null;
//            Job noArchiveJob = null;
//            
//            try
//            {   
//                // archived job
//                archivingJob = createJob(testStatus, privateSoftware, SYSTEM_OWNER);
//                JobDao.persist(archivingJob);
//                
//                if (JobStatusType.isRunning(testStatus) || JobStatusType.isFailed(testStatus) || testStatus == STOPPED || testStatus == PAUSED) {
//                    expectedPath = privateSoftware.getExecutionSystem().getRemoteDataClient().resolvePath(archivingJob.getWorkPath() + "/" + JOB_OUTPUT_RELATIVE_PATH);
//                } else {
//                    expectedPath = archivingJob.getArchiveSystem().getRemoteDataClient().resolvePath(archivingJob.getArchivePath() + "/" + JOB_OUTPUT_RELATIVE_PATH);
//                }
//                
//                outputUrl = TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE) 
//                        + archivingJob.getUuid() + "/outputs/media/" + JOB_OUTPUT_RELATIVE_PATH;
//                absolutePath = ApiUriUtil.getAbsolutePath(archivingJob.getOwner(), URI.create(outputUrl));
//                
//                Assert.assertEquals(absolutePath, expectedPath, "ApiUriUtil.getAbsolutePath(String, URI) should not change in archviing job regardless of job status " + testStatus);
//                Assert.assertNotNull(absolutePath, "Null system should never be returned from ApiUriUtil#getAbsolutePath(String,URI)");
//                
//                
//                // non archived job   
//                noArchiveJob = createJob(testStatus, privateSoftware, SYSTEM_OWNER);
//                noArchiveJob.setArchiveOutput(false);
//                JobDao.persist(noArchiveJob);
//                
//                outputUrl = TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE) 
//                        + noArchiveJob.getUuid() + "/outputs/media/" + JOB_OUTPUT_RELATIVE_PATH;
//                absolutePath = ApiUriUtil.getAbsolutePath(noArchiveJob.getOwner(), URI.create(outputUrl));
//                
//                expectedPath = noArchiveJob.getArchiveSystem().getRemoteDataClient().resolvePath(noArchiveJob.getWorkPath() + "/" + JOB_OUTPUT_RELATIVE_PATH);
//                
//                Assert.assertNotNull(absolutePath, "Null system should never be returned from ApiUriUtil#getAbsolutePath(String,URI)");
//                Assert.assertEquals(absolutePath, expectedPath, "ApiUriUtil.getAbsolutePath(String, URI) should not change in non-archving job regardless of job status " + testStatus);
//                
//            }
//            finally {
//                try { JobDao.delete(archivingJob); } catch (Exception e) {}
//                try { JobDao.delete(noArchiveJob); } catch (Exception e) {}
//            }
//        }
//    }
//	
//	@Test(dependsOnMethods={"getAbsolutePathFollowsStatus"})
//    public void getSystemHonorsJobPermissions() 
//    throws Exception 
//    {
//	    Job sharedJob = null;
//        try 
//        {
//            sharedJob = createJob(RUNNING, SYSTEM_OWNER);
//            JobDao.persist(sharedJob);
//            
//            JobPermissionManager pm = new JobPermissionManager(sharedJob, SYSTEM_OWNER);
//            pm.setPermission(SYSTEM_SHARED_USER, PermissionType.READ.name());
//            
//            String outputUrl = TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE) + sharedJob.getUuid() 
//                    + "/outputs/media/" + JOB_OUTPUT_RELATIVE_PATH;
//            RemoteSystem system = ApiUriUtil.getRemoteSystem(SYSTEM_SHARED_USER, URI.create(outputUrl));
//            
//            Assert.assertEquals(system.getSystemId(), sharedJob.getSystem(), "User should retrieve the job execution system back on a job shared with them.");
//            
//            try {
//                system = ApiUriUtil.getRemoteSystem(SYSTEM_OTHER_USER, URI.create(outputUrl));
//                Assert.fail("User requesting system for job output uri they do not have access to should be a permission error.");
//            } catch (PermissionException e) {
//                // this is what should happen
//            } catch (Exception e) {
//                Assert.fail("User requesting system for job output uri they do not have access to should be a permission error.");
//            }
//        }
//        catch (Exception e) {
//            Assert.fail("Unexpected error", e);
//        }
//        finally {
//            try { JobDao.delete(sharedJob); } catch (Exception e) {}
//        }
//    }
//	
//	@DataProvider(parallel=false)
//	private Object[][] getSystemHonorsPrivacyProvider() throws Exception {
//	    Software sharedSoftware = SoftwareDao.getUserApps(SYSTEM_OWNER, false).get(0);
//	    
//	    return new Object[][] {
//	            { createJob(RUNNING, sharedSoftware, SYSTEM_OWNER), sharedSoftware.getExecutionSystem(), SYSTEM_OWNER, false, "Job owner should get their own system back from job output uri" },
//	            { createJob(RUNNING, sharedSoftware, SYSTEM_OWNER), null, SYSTEM_SHARED_USER, true, "System user should not retrieve system from private job." },
//	            { createJob(RUNNING, sharedSoftware, SYSTEM_OWNER), null, SYSTEM_OTHER_USER, true, "Other user should not retrieve system from private job." },
//	            
//	            { createJob(FINISHED, sharedSoftware, SYSTEM_OWNER), systemManager.getUserDefaultStorageSystem(SYSTEM_OWNER), SYSTEM_OWNER, false, "Job owner should get their own public archive system back from job output uri when job is finished" },
//                { createJob(FINISHED, sharedSoftware, SYSTEM_OWNER), null, SYSTEM_SHARED_USER, true, "Requesting system from job output uri when job is archived and system is public and user is not job owner should throw exception" },
//                { createJob(FINISHED, sharedSoftware, SYSTEM_OWNER), null, SYSTEM_OTHER_USER, true, "Requesting system from job output uri when job is archived and system is public and user is not job owner should throw exception" },
//        };
//	}   
//	
//	@Test(dataProvider="getSystemHonorsPrivacyProvider", dependsOnMethods={"getSystemHonorsJobPermissions"})
//    public void getSystemHonorsPrivacy(Job job, RemoteSystem expectedSystem, String username, boolean shouldThrowException, String message) 
//    throws Exception 
//    {  
//        try 
//        {
//            JobDao.persist(job);
//            
//            String outputUrl = TenancyHelper.resolveURLToCurrentTenant(Settings.IPLANT_JOB_SERVICE) 
//                    + job.getUuid() + "/outputs/media/" + JOB_OUTPUT_RELATIVE_PATH;
//            
//            RemoteSystem system = ApiUriUtil.getRemoteSystem(username, URI.create(outputUrl));
//            
//            Assert.assertEquals(system, expectedSystem, message);
//        }
//        catch (Exception e) {
//            Assert.assertTrue(shouldThrowException, message);
//        }
//        finally {
//            try { JobDao.delete(job); } catch (Exception e) {}
//        }
//    }
//    
}