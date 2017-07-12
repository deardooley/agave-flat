package org.iplantc.service.jobs.managers.monitors;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwareInput;
import org.iplantc.service.apps.model.SoftwareParameter;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.SoftwareUnavailableException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.model.enumerations.WrapperTemplateAttributeVariableType;
import org.iplantc.service.jobs.model.enumerations.WrapperTemplateStatusVariableType;
import org.iplantc.service.jobs.submission.AbstractJobSubmissionTest;
import org.iplantc.service.jobs.util.Slug;
import org.iplantc.service.remote.RemoteSubmissionClient;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.systems.model.enumerations.SchedulerType;
import org.iplantc.service.systems.model.enumerations.StorageProtocolType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.exceptions.RemoteDataException;
import org.json.JSONObject;
import org.mockito.Spy;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class JobMonitorTest extends AbstractJobSubmissionTest {

	private String remoteFilePath;
	protected Software software;
	protected SchedulerType schedulerType;
	protected ObjectMapper mapper = new ObjectMapper();
	
	public JobMonitorTest(SchedulerType schedulerType) {
		this.schedulerType = schedulerType;
	}
	
	@BeforeClass
	@Override
	protected void beforeClass() throws Exception {
		super.beforeClass();
	}

	@AfterClass
	protected void afterClass() throws Exception {
		RemoteDataClient remoteDataClient = null;
		RemoteSubmissionClient remoteSubmissionClient = null;
		try {
			
		
		} finally {
			clearJobs();
			clearSoftware();
			clearSystems();
		}
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.submission.AbstractJobSubmissionTest#clearSystems()
	 */
	@Override
	protected void clearSystems() {
		SystemDao dao = new SystemDao();
		try { dao.remove(executionSystem); } catch (Exception e){}
		try { dao.remove(storageSystem); } catch (Exception e){}
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.submission.AbstractJobSubmissionTest#clearSoftware()
	 */
	@Override
	protected void clearSoftware() throws Exception {
		try { SoftwareDao.delete(software); } catch (Exception e){}
	}

	@Override
	protected void initSystems() throws Exception {
	    storageSystem = (StorageSystem) getNewInstanceOfRemoteSystem(RemoteSystemType.STORAGE, 
	    		getStorageSystemProtocolType().name().toLowerCase());
        storageSystem.setOwner(SYSTEM_OWNER);
        storageSystem.setPubliclyAvailable(true);
        storageSystem.setGlobalDefault(true);
        storageSystem.getUsersUsingAsDefault().add(SYSTEM_OWNER);
        systemDao.persist(storageSystem);
        
        executionSystem = (ExecutionSystem)getNewInstanceOfRemoteSystem(RemoteSystemType.EXECUTION, 
        		getExectionSystemSchedulerType().name().toLowerCase());
        executionSystem.setOwner(SYSTEM_OWNER);
        executionSystem.setPubliclyAvailable(true);
        executionSystem.setType(RemoteSystemType.EXECUTION);
        systemDao.persist(executionSystem);
    }

	/**
	 * The type of scheduler to we'll use in this test. The 
	 * {@link SchedulerType} maps to a config file named 
	 * as systems/execution/{@link SchedulerType#name()}.example.com.json
	 * @return the scheduler to use for the storage system
	 */
	protected SchedulerType getExectionSystemSchedulerType() {
		return this.schedulerType;
	}
	
	/**
	 * Specifies the type of storage system we'll use in this test. 
	 * The {@link StorageProtocolType} maps to a config file named 
	 * as systems/storage/{@link StorageProtocolType#name()}.example.com.json
	 * 
	 * @return the protocol to use for the storage system
	 */
	protected StorageProtocolType getStorageSystemProtocolType() {
		return StorageProtocolType.SFTP;
	}

	@Override
	protected void initSoftware() throws Exception {
		JSONObject json = jtd.getTestDataObject(SOFTWARE_SYSTEM_TEMPLATE_DIR + File.separator + 
				executionSystem.getExecutionType().name().toLowerCase() + File.separator + 
				executionSystem.getScheduler().name().toLowerCase() + ".json");
		
		json.put("deploymentSystem", storageSystem.getSystemId());
		
		software = Software.fromJSON(json, SYSTEM_OWNER);
		software.setOwner(SYSTEM_OWNER);
		
		SoftwareDao.persist(software);
	}

	protected void stageSoftwareDeploymentDirectory(Software software)
	throws Exception {
		RemoteDataClient remoteDataClient = null;
		try 
		{
			remoteDataClient = software.getStorageSystem().getRemoteDataClient();
			remoteDataClient.authenticate();
			remoteDataClient.mkdirs(software.getDeploymentPath());
			String remoteTemplatePath = software.getDeploymentPath() + File.separator + software.getExecutablePath();
			remoteDataClient.put(SOFTWARE_WRAPPER_FILE, 
					software.getDeploymentPath() + File.separator + software.getExecutablePath());
			Assert.assertTrue(remoteDataClient.doesExist(remoteTemplatePath), 
					"Failed to copy software assets to deployment system " 
			+ software.getStorageSystem().getSystemId());
		} catch (RemoteDataException e) {
			Assert.fail("Failed to authenticate to the storage system " + job.getSoftwareName(), e);
		} catch (Exception e) {
			Assert.fail("Failed to copy input file to remote system", e);
		} 
		finally {
			try {remoteDataClient.disconnect();} catch (Exception e){}
		}
	}

	@DataProvider(name = "submitJobProvider")
	protected Object[][] submitJobProvider() throws Exception {
//		List<Software> testApps = SoftwareDao.getUserApps(SYSTEM_OWNER, false);
		List<Software> testApps = new ArrayList<Software>();
		testApps.add(software);
		Object[][] testData = new Object[testApps.size()][];
		for(int i=0; i< testApps.size(); i++) {
			testData[i] = new Object[] { testApps.get(i), "Submission to " + testApps.get(i).getExecutionSystem().getSystemId() + " failed.", false };
		}
		
		return testData;
	}

	protected Job createAndPersistJob(Software software) throws Exception {
		Job job = new Job();
		job.setName( software.getExecutionSystem().getScheduler().name() + " test");
		job.setArchiveOutput(false);
		job.setArchivePath("/");
		job.setArchiveSystem(storageSystem);
		job.setCreated(new Date());
		job.setMemoryPerNode((double).5);
		job.setOwner(software.getOwner());
		job.setProcessorsPerNode((long)1);
		job.setMaxRunTime("1:00:00");
		job.setSoftwareName(software.getUniqueName());
		job.setStatus(JobStatusType.PENDING, job.getErrorMessage());
		job.setSystem(software.getExecutionSystem().getSystemId());
		job.setBatchQueue(software.getExecutionSystem().getDefaultQueue().getName());
		
		ObjectNode jobInputs = mapper.createObjectNode();
		for(SoftwareInput input: software.getInputs()) {
			jobInputs.put(input.getKey(), String.format("agave://%s/%s/%s", 
					software.getStorageSystem().getSystemId(),
					software.getDeploymentPath(),
					software.getExecutablePath()));
		}
		job.setInputsAsJsonObject(jobInputs);
		
		ObjectNode jobParameters = mapper.createObjectNode();
		for (SoftwareParameter parameter: software.getParameters()) {
			jobParameters.set(parameter.getKey(), parameter.getDefaultValueAsJsonArray());
		}
		job.setParametersAsJsonObject(jobParameters);
		
		JobDao.persist(job);
		
		String remoteWorkPath = null;
		if (StringUtils.isEmpty(software.getExecutionSystem().getScratchDir())) {
			remoteWorkPath = job.getOwner() +
				"/job-" + job.getId() + "-" + Slug.toSlug(job.getName()) + 
				"/" + FilenameUtils.getName(software.getDeploymentPath());
		} else {
			remoteWorkPath = software.getExecutionSystem().getScratchDir() + 
					job.getOwner() + 
					"/job-" + job.getId() + "-" + Slug.toSlug(job.getName()) +
					"/" + FilenameUtils.getName(software.getDeploymentPath());
		}
		
		job.setWorkPath(remoteWorkPath);
		
		JobDao.persist(job);
		
		return job;
	}

	@Test(groups = { "job", "launcher", "submission" }, dataProvider = "submitJobProvider", enabled = true)
	public void monitorJob(Software software, String message, boolean shouldThrowException)
	throws Exception {
		Job job = null;
		try {
			job = createAndPersistJob(software);
		
			stageSoftwareDeploymentDirectory(software);
			
			this.genericRemoteSubmissionTestCase(job, true, "Condor job submission failed", false);
		}
		finally {
			try { JobDao.delete(job); } catch (Exception e) {}
			
		}
	}
	
	@Test(groups = { "job", "launcher", "submission" }, dataProvider = "submitJobProvider", enabled = true)
	public void monitorTakesNoActionOnInvalidMonitorCommand(Software software, String message, boolean shouldThrowException)
	throws Exception {
		RemoteSubmissionClient client = null;
		Job job = null;
		try {
			// create a job
			job = createAndPersistJob(software);
			job.setStatus(JobStatusType.QUEUED, "Job is queued");

			// get a valid monitor for the job
			JobMonitor monitor = JobMonitorFactory.getInstance(job);
			
			// get a remote submissions client to test the response
			ExecutionSystem executionSystem = software.getExecutionSystem();
			client = org.mockito.Mockito.spy(executionSystem.getRemoteSubmissionClient(null));
			
			// find the original command
			String[] commandTokens = StringUtils.split(executionSystem.getScheduler().getBatchQueryCommand());
			
			org.mockito.Mockito.when(monitor.getJobQueryCommand()).thenReturn(commandTokens[0] + " --someillformattedargument");
			
			Job updatedJob = monitor.monitor();
			
			Assert.assertEquals(updatedJob.getStatus(), job.getStatus(), "Job should not change status when an error response comes back from the batch scheduler.");
			
		}
		finally {
			
			try { client.close(); } catch (Exception e) {}
		}
	}
	
	@Test(groups = { "job", "launcher", "submission" }, dataProvider = "submitJobProvider", enabled = true)
	public void monitorStoppedJob(Software software, String message, boolean shouldThrowException)
	throws Exception {
		Job job = null;
		try {
			job = createAndPersistJob(software);
		
			stageSoftwareDeploymentDirectory(software);
		
			stageJobInputs(job);
			
			this.genericRemoteSubmissionTestCase(job, true, "Condor job submission failed", false);
		}
		finally {
			try { JobDao.delete(job); } catch (Exception e) {}
			
		}
	}
	
	/**
	 * @return the remoteFilePath
	 */
	protected String getRemoteFilePath() {
		return remoteFilePath;
	}

	/**
	 * @param remoteFilePath the remoteFilePath to set
	 */
	protected void setRemoteFilePath(String remoteFilePath) {
		this.remoteFilePath = remoteFilePath;
	}

}