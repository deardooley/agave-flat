package org.iplantc.service.jobs.managers.launchers;

import java.io.File;
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
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;


public abstract class AbstractJobLauncherTest extends AbstractJobSubmissionTest {

	private String remoteFilePath;

	public AbstractJobLauncherTest() {
		super();
	}

	@BeforeClass
	@Override
	public void beforeClass() throws Exception {
		super.beforeClass();
	}

	@AfterClass
	public void afterClass() throws Exception {
		
		try {
			RemoteDataClient remoteDataClient = null;
//			try 
//			{
//				Software software = SoftwareDao.getSoftwareByUniqueName(job.getSoftwareName());
//				
//				remoteDataClient = software.getStorageSystem().getRemoteDataClient();
//				remoteDataClient.authenticate();
//				if (remoteDataClient.doesExist(software.getDeploymentPath())) {
//					remoteDataClient.delete(software.getDeploymentPath());
//					Assert.assertFalse(remoteDataClient.doesExist(software.getDeploymentPath()), 
//							"Failed to delete software deployment path from software storage system");
//				}
//			} catch (RemoteDataException e) {
//				Assert.fail("Failed to authenticate to the storage system " + job.getSoftwareName(), e);
//			} catch (Exception e) {
//				Assert.fail("Failed to delete software deployment path", e);
//			} finally {
//				try {remoteDataClient.disconnect();} catch (Exception e){}
//			}
			
//			try {
//				remoteDataClient = new SystemDao().findBySystemId(job.getSystem()).getRemoteDataClient(job.getInternalUsername());
//				remoteDataClient.authenticate();
//				if (remoteDataClient.doesExist(job.getWorkPath())) {
//					remoteDataClient.delete(job.getWorkPath());
//					Assert.assertFalse(remoteDataClient.doesExist(job.getWorkPath()), "Failed to delete work directory from execution system");
//				}
//			} catch (Exception e) {
//				Assert.fail("Failed delete work directory", e);
//			} finally {
//				try {remoteDataClient.disconnect();} catch (Exception e){}
//			}
		
		} finally {
			clearJobs();
			clearSoftware();
			clearSystems();
		}
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
	protected abstract SchedulerType getExectionSystemSchedulerType();
	
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
		
		Software software = Software.fromJSON(json, SYSTEM_OWNER);
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
		List<Software> testApps = SoftwareDao.getUserApps(SYSTEM_OWNER, false);
		
		Object[][] testData = new Object[testApps.size()][3];
		for(int i=0; i< testApps.size(); i++) {
			testData[i] = new Object[] { testApps.get(i), "Submission to " + testApps.get(i).getExecutionSystem().getSystemId() + " failed.", false };
		}
		
		return testData;
	}

	protected Job createAndPersistJob(Software software) throws Exception {
		//RemoteDataClient remoteDataClient = null;
		ObjectMapper mapper = new ObjectMapper();
		
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

	@DataProvider
	protected Object[][] resolveMacrosProvider() throws Exception {
		List<Software> testApps = SoftwareDao.getUserApps(SYSTEM_OWNER, false);
		
		job = createAndPersistJob(testApps.get(0));
		
		Object[][] testData = new Object[WrapperTemplateAttributeVariableType.values().length + WrapperTemplateStatusVariableType.values().length + 1][3];
		int i = 0;
		for (WrapperTemplateAttributeVariableType macro: WrapperTemplateAttributeVariableType.values()) {
			testData[i++] = new Object[] { job, macro.name(), macro.resolveForJob(job), true };
		}
		
		for (WrapperTemplateStatusVariableType macro: WrapperTemplateStatusVariableType.values()) {
			testData[i++] = new Object[] { job, macro.name(), macro.resolveForJob(job), true };
		}
		
		testData[i++] = new Object[] { job, WrapperTemplateAttributeVariableType.AGAVE_JOB_ARCHIVE_URL.name(), "", false  };
		
		return testData;
	}

	@Test(groups = { "submission" }, dataProvider = "resolveMacrosProvider", enabled = true)
	public void resolveMacros(Job job, String macro, String expectedValue, boolean archive) 
	throws JobException, SystemUnavailableException, SoftwareUnavailableException 
	{
		JobLauncher launcher = new HPCLauncher(job);
		Assert.assertEquals(launcher.resolveMacros("${" + macro + "}"), expectedValue, "Launcher did not resolve wrapper template macro " + macro + " properly.");
	}
	

//	@Test (groups={"submission"}, dataProvider="processApplicationTemplateProvider", dependsOnMethods={"resolveMacros"})
//	public void processApplicationTemplate(Software software, String message, boolean shouldThrowException) 
//	throws Exception 
//	{
//		job = createAndPersistJob(software);
//		super.genericProcessApplicationTemplate(job, message, shouldThrowException);
//	}
	

	@DataProvider
	protected Object[][] resolveNotificationsMacrosProvider() throws Exception {
		List<Software> testApps = SoftwareDao.getUserApps(SYSTEM_OWNER, false);
		
		job = createAndPersistJob(testApps.get(0));
		
		return new Object[][] { 
			{job, "", 
				WrapperTemplateStatusVariableType.resolveNotificationEventMacro(job, null, null), 
				false  },
			{job, "HOME", 
				WrapperTemplateStatusVariableType.resolveNotificationEventMacro(job, null, new String[]{"HOME"}), 
				false  },
			{job, "HOME,HOSTNAME,SHELL", 
				WrapperTemplateStatusVariableType.resolveNotificationEventMacro(job, null, new String[]{"HOME","HOSTNAME","SHELL"}), 
				false  },
			{job, "MY_EVENT|", 
				WrapperTemplateStatusVariableType.resolveNotificationEventMacro(job, "MY_EVENT", new String[]{}), 
				false  },
			{job, "MY_EVENT|HOME", 
				WrapperTemplateStatusVariableType.resolveNotificationEventMacro(job, "MY_EVENT", new String[]{"HOME"}), 
				false  },
			{job, "MY_EVENT|HOME,HOSTNAME,SHELL", 
				WrapperTemplateStatusVariableType.resolveNotificationEventMacro(job, "MY_EVENT", new String[]{"HOME","HOSTNAME","SHELL"}), 
				false  },
		};
	}

	@Test(groups = { "submission" }, dataProvider = "resolveNotificationsMacrosProvider", dependsOnMethods = { "resolveMacros" }, enabled = true)
	public void resolveNotificationMacros(Job job, String macroVars, String expectedValue, boolean archive) 
	throws JobException, SystemUnavailableException, SoftwareUnavailableException 
	{
		JobLauncher launcher = new HPCLauncher(job);
		Assert.assertEquals(launcher.resolveRuntimeNotificationMacros("${AGAVE_JOB_CALLBACK_NOTIFICATION|" + macroVars + "}"), expectedValue, "Launcher did not resolve wrapper template notification macro properly.");
	}

	@Test(groups = { "submission" }, dataProvider = "submitJobProvider", dependsOnMethods = { "resolveMacros" }, enabled = true)
	public void submitJob(Software software, String message, boolean shouldThrowException)
	throws Exception {
		try {
			job = createAndPersistJob(software);
		
			stageSoftwareDeploymentDirectory(software);
		
			stageJobInputs(job);
			
			this.genericRemoteSubmissionTestCase(job, true, "Condor job submission failed", false);
		}
		finally {
			try { JobDao.delete(job); } catch (Exception e) {}
			try { SoftwareDao.delete(software); } catch (Exception e) {}
			try { new SystemDao().remove(executionSystem); } catch (Exception e) {}
		}
	}

	/**
	 * @return the remoteFilePath
	 */
	public String getRemoteFilePath() {
		return remoteFilePath;
	}

	/**
	 * @param remoteFilePath the remoteFilePath to set
	 */
	public void setRemoteFilePath(String remoteFilePath) {
		this.remoteFilePath = remoteFilePath;
	}

}