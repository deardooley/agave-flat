/**
 * 
 */
package org.iplantc.service.jobs.submission;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.util.Date;

import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwarePermission;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.managers.JobManager;
import org.iplantc.service.jobs.managers.launchers.JobLauncher;
import org.iplantc.service.jobs.model.JSONTestDataUtil;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.phases.workers.IPhaseWorker;
import org.iplantc.service.jobs.queue.actions.SubmissionAction;
import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.transfer.model.enumerations.PermissionType;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
//import org.iplantc.service.systems.model.JSONTestDataUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests the user and overall job quotas enforced by the system.
 * 
 * @author dooley
 *
 */
public class JobSubmissionQuotaTest extends AbstractJobSubmissionTest {

	private Software software;
	
	@BeforeClass
	public void beforeClass() throws Exception
	{
		super.beforeClass();
		
		
	}
	
	@AfterClass
	public void afterClass() throws Exception
	{
		super.afterClass();
	}
	
	protected void initSoftware() throws Exception {
		JSONObject appJson = jtd.getTestDataObject(JSONTestDataUtil.TEST_SOFTWARE_SYSTEM_FILE);
		appJson.put("executionHost", executionSystem.getSystemId());
		software = Software.fromJSON(appJson, SYSTEM_OWNER);
		  
		software.setExecutionSystem(executionSystem);
		software.setOwner(SYSTEM_OWNER);
		software.setPubliclyAvailable(false);
		software.getPermissions().add(new SoftwarePermission(SYSTEM_OWNER_SHARED, PermissionType.ALL));
		SoftwareDao.persist(software);
	}
//	@BeforeClass
//	public void beforeClass() throws Exception
//	{
//		jtd = JSONTestDataUtil.getInstance();
////	    File file = new File("src/test/resources/hibernate.cfg.xml");
////		Configuration configuration = new Configuration().configure(file);
////		HibernateUtil.rebuildSessionFactory(configuration);
//		systemsDao = new SystemDao();
//		
//		JSONObject systemJson = jtd.getTestDataObject(JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE);
//		
//		ExecutionSystem existingSystem = (ExecutionSystem)systemsDao.findBySystemId(systemJson.getString("id"));
//		if (existingSystem == null) { 
//			existingSystem =  ExecutionSystem.fromJSON(systemJson);
//			existingSystem.setOwner(SYSTEM_OWNER);
//			existingSystem.setPubliclyAvailable(false);
//			existingSystem.setStatus(SystemStatusType.DOWN);
//			existingSystem.getRoles().add(new SystemRole(SYSTEM_OWNER_SHARED, RoleType.PUBLISHER));
//			systemsDao.persist(existingSystem);
//			existingSystem = (ExecutionSystem)systemsDao.findById(existingSystem.getId());
//		}
//		
//		JSONObject appJson = jtd.getTestDataObject(JSONTestDataUtil.TEST_SOFTWARE_SYSTEM_FILE);
//        appJson.put("executionHost", existingSystem.getSystemId());
//        software = Software.fromJSON(appJson, SYSTEM_OWNER);
//        
//        software.setExecutionSystem(existingSystem);
//        software.setOwner(SYSTEM_OWNER);
//        software.setPubliclyAvailable(false);
//        software.getPermissions().add(new SoftwarePermission(SYSTEM_OWNER_SHARED, PermissionType.ALL));
//        SoftwareDao.persist(software);
//	}
	
//	@AfterClass
//	public void clearDb()
//	{
//		try { systemsDao.remove(software.getExecutionSystem()); } catch (Exception e) {}
//		
//		try { SoftwareDao.delete(software); } catch (Exception e) {}
//	}
	
	@BeforeMethod
	public void beforeMethod() throws SystemArgumentException, JSONException, IOException 
	{
		HibernateUtil.flush();
	}
	
	public Job getDefaultJob() throws JobException
	{
		ObjectMapper mapper = new ObjectMapper();
		Job job = new Job();
		job.setName("test-job");
		job.setArchiveOutput(false);
		job.setArchiveSystem(storageSystem);
		job.setBatchQueue(longQueue.getEffectiveMappedName());
		job.setArchivePath("/iplant-test/archive/test-job-999");
		job.setCreated(new Date());
		job.setInputsAsJsonObject(mapper.createObjectNode());
		job.setMemoryPerNode((double)512);
		job.setOwner(SYSTEM_OWNER);
		job.setParametersAsJsonObject(mapper.createObjectNode());
		job.setProcessorsPerNode((long)1);
		job.setMaxRunTime("1:00");
		job.setSoftwareName(software.getUniqueName());
		job.initStatus(JobStatusType.STAGED, "Input data staged to execution system");
		job.setSystem(software.getExecutionSystem().getSystemId());
		job.setUpdateToken("asodfuasodu2342asdfar23rsdafa");
		job.setVisible(true);
		
		return job;
	}
	
	@Test
	public void jobSystemUnavailableStaysInQueue() throws Exception
	{
		Job job;
		try 
		{
			job = getDefaultJob();
			JobDao.create(job);
			
			// TODO: here we should only be checking that the exceptions 
			
			JobLauncher mockJobLauncher = mock(JobLauncher.class);
			doThrow(new SystemUnavailableException()).when(mockJobLauncher).launch();
			
			IPhaseWorker worker = Mockito.mock(IPhaseWorker.class);
			SubmissionAction submissionAction = spy(new SubmissionAction(job, worker));
			doReturn(mockJobLauncher).when(submissionAction).getJobLauncher();
			
			PowerMockito.mockStatic(JobManager.class);
//			when(JobManager.updateStatus(job, JobStatusType.STAGED)).thenReturn(job);
			
			submissionAction.run();
		
			PowerMockito.verifyStatic();
			job = JobManager.updateStatus(job, JobStatusType.STAGED);
			
			Assert.assertEquals(job.getStatus(), JobStatusType.STAGED, "Job status is still at staged when system unavailable.");
			
			Assert.assertTrue(job.getErrorMessage().contains("unavailable"), "Unavailable system should queue job");
			
		} 
		catch(Exception e) 
		{
			throw e;
		}
		finally 
		{
			for(Job j: JobDao.getByUsername(SYSTEM_OWNER)) {
				try { JobDao.delete(j); } catch (Exception e) {}
			}
			
//			try {
//				executionSystem.setStatus(SystemStatusType.UP);
//				systemsDao.persist(executionSystem);
//			} catch (Exception e) {}
//			
		}
	}
	
	@Test
	public void jobSystemQueueQuotaStaysInQueue() throws Exception
	{
		Job job;
		
		try 
		{
			job = getDefaultJob();
			
			
			for (int i=0; i<software.getExecutionSystem().getDefaultQueue().getMaxJobs(); i++) {
				Job j = job.copy();
				j.initStatus(JobStatusType.RUNNING, "Job began running");
				j.setSystem(job.getSystem());
				JobDao.create(j);
			}
			
			JobDao.create(job);
			
			IPhaseWorker worker = Mockito.mock(IPhaseWorker.class);
			SubmissionAction submissionAction = new SubmissionAction(job, worker);
            submissionAction.run();
            
            job = JobDao.getById(job.getId());
			
			Assert.assertEquals(job.getStatus(), JobStatusType.STAGED, "Job status is still at staged when system unavailable.");
			
			Assert.assertTrue(job.getErrorMessage().contains("capacity for new jobs"), "No new jobs ");
			
		} 
		catch(Exception e) 
		{
			throw e;
		}
		finally 
		{
			for(Job j: JobDao.getByUsername(SYSTEM_OWNER)) {
				try { JobDao.delete(j); } catch (Exception e) {}
			}
		}
	}
	
	@Test
	public void jobSystemSoftUserQuotaEnforced() throws Exception
	{
		Job job;
		
		try 
		{
			job = getDefaultJob();
			Software software = SoftwareDao.getSoftwareByUniqueName(job.getSoftwareName());
			ExecutionSystem executionSystem = software.getExecutionSystem();
			for (int i=0; i<Math.ceil(executionSystem.getMaxSystemJobsPerUser() * 1.2); i++) {
				Job j = job.copy();
				j.initStatus(JobStatusType.RUNNING, "Job began running");
				j.setSystem(job.getSystem());
				JobDao.create(j);
			}
			
			JobDao.create(job);
			
			IPhaseWorker worker = Mockito.mock(IPhaseWorker.class);
			SubmissionAction submissionAction = new SubmissionAction(job, worker);
            submissionAction.run();
            
            job = JobDao.getById(job.getId());
			
			Assert.assertEquals(job.getStatus(), JobStatusType.STAGED, "Job status is still at staged when soft user quota enforced.");
			
			Assert.assertTrue(job.getErrorMessage().contains("simultaneous jobs"), "No new jobs ");
			
		} 
		catch(Exception e) 
		{
			throw e;
		}
		finally {
			for(Job j: JobDao.getByUsername(SYSTEM_OWNER)) {
				try { JobDao.delete(j); } catch (Exception e) {}
			}
		}
	}
	
	@Test
	public void jobSystemHardUserQuotaEnforced() throws Exception
	{
		Job job;
		
		try 
		{
			job = getDefaultJob();
			
			job.initStatus(JobStatusType.RUNNING, "Job began running");
			
			job.setName("test-test-2");
			
			double queueQuota = Math.ceil(software.getExecutionSystem().getDefaultQueue().getMaxJobs() / 2) + 1;
			
			for (int i=0; i<queueQuota; i++) {
				Job j = job.copy();
				job.setOwner(SYSTEM_OWNER_SHARED);
				j.initStatus(JobStatusType.RUNNING, "Job began running");
				j.setSystem(job.getSystem());
				JobDao.create(j);
			}
			
			job.setName("test-user-2");
			Software software = SoftwareDao.getSoftwareByUniqueName(job.getSoftwareName());
			ExecutionSystem executionSystem = software.getExecutionSystem();
			for (int i=0; i<Math.ceil(executionSystem.getMaxSystemJobsPerUser() * 1.2); i++) {
				Job j = job.copy();
				j.initStatus(JobStatusType.RUNNING, "Job began running");
				j.setSystem(job.getSystem());
				JobDao.create(j);
			}
			
			JobDao.create(job);
			
			IPhaseWorker worker = Mockito.mock(IPhaseWorker.class);
			SubmissionAction submissionAction = new SubmissionAction(job, worker);
            submissionAction.run();
            
            job = JobDao.getById(job.getId());
			
			Assert.assertEquals(job.getStatus(), JobStatusType.STAGED, "Job status is still at staged when hard user quota enforced.");
			Assert.assertNotNull(job.getErrorMessage(), "Job was not run. Error stored with job");
			Assert.assertTrue(job.getErrorMessage().contains("simultaneous jobs"), "No new jobs ");
			
		} 
		catch(Exception e) 
		{
			throw e;
		}
		finally {
			for(Job j: JobDao.getByUsername(SYSTEM_OWNER)) {
				try { JobDao.delete(j); } catch (Exception e) {}
			}
			
			for(Job j: JobDao.getByUsername(SYSTEM_OWNER_SHARED)) {
				try { JobDao.delete(j); } catch (Exception e) {}
			}
		}
	}
}
