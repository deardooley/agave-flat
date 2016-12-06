package org.iplantc.service.jobs.managers.launchers;

import static org.iplantc.service.jobs.model.enumerations.WrapperTemplateStatusVariableType.*;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.iplantc.service.apps.Settings;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwareInput;
import org.iplantc.service.apps.model.SoftwareParameter;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.model.enumerations.WrapperTemplateAttributeVariableType;
import org.iplantc.service.jobs.model.enumerations.WrapperTemplateStatusVariableType;
import org.iplantc.service.jobs.phases.workers.IPhaseWorker;
import org.iplantc.service.jobs.submission.AbstractJobSubmissionTest;
import org.iplantc.service.jobs.util.Slug;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.jobs.model.JSONTestDataUtil;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Created with IntelliJ IDEA.
 * User: steve
 * Date: 1/14/13
 * Time: 3:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class CondorLauncherTest extends AbstractJobSubmissionTest
{
	private String remoteFilePath;
	
	@BeforeClass
	@Override
	public void beforeClass() throws Exception 
	{
		super.beforeClass();
		
//		clearSoftware();
//		clearSystems();
//		clearJobs();
//		
//		jtd = JSONTestDataUtil.getInstance();
//		
//		initSystems();
//		
//		initSoftware();
	}
	
	@AfterClass 
	public void afterClass() throws Exception {
		
		RemoteDataClient remoteDataClient = null;
		try 
		{
			Software software = SoftwareDao.getSoftwareByUniqueName(job.getSoftwareName());
			
			remoteDataClient = software.getStorageSystem().getRemoteDataClient();
			remoteDataClient.authenticate();
			if (remoteDataClient.doesExist(software.getDeploymentPath())) {
				remoteDataClient.delete(software.getDeploymentPath());
				Assert.assertFalse(remoteDataClient.doesExist(software.getDeploymentPath()), 
						"Failed to delete software deployment path from software storage system");
			}
		} catch (Exception e) {
			Assert.fail("Failed to delete software deployment path", e);
		} finally {
			try {remoteDataClient.disconnect();} catch (Exception e){}
		}
		
		try {
			remoteDataClient = new SystemDao().findBySystemId(job.getSystem()).getRemoteDataClient(job.getInternalUsername());
			remoteDataClient.authenticate();
			if (remoteDataClient.doesExist(job.getWorkPath())) {
				remoteDataClient.delete(job.getWorkPath());
				Assert.assertFalse(remoteDataClient.doesExist(job.getWorkPath()), "Failed to delete work directory from execution system");
			}
		} catch (Exception e) {
			Assert.fail("Failed delete work directory", e);
		} finally {
			try {remoteDataClient.disconnect();} catch (Exception e){}
		}
		
		clearJobs();
		clearSoftware();
		clearSystems();
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.submission.AbstractJobSubmissionTest#initSystems()
	 */
	@Override
	protected void initSystems() throws Exception {
	    storageSystem = (StorageSystem) getNewInstanceOfRemoteSystem(RemoteSystemType.STORAGE, "storage");
        storageSystem.setOwner(SYSTEM_OWNER);
        storageSystem.setPubliclyAvailable(true);
        storageSystem.setGlobalDefault(true);
        storageSystem.getUsersUsingAsDefault().add(SYSTEM_OWNER);
        systemDao.persist(storageSystem);
        
//        JSONObject json = jtd.getTestDataObject(EXECUTION_SYSTEM_TEMPLATE_DIR + 
//                File.separator + "condor.opensciencegrid.org.json");
//        executionSystem = (ExecutionSystem) systemManager.parseSystem(json, SYSTEM_OWNER, null);
        executionSystem = 
                (ExecutionSystem) getNewInstanceOfRemoteSystem(RemoteSystemType.EXECUTION, "condor");
        executionSystem.setOwner(SYSTEM_OWNER);
        executionSystem.setPubliclyAvailable(true);
        executionSystem.setType(RemoteSystemType.EXECUTION);
        systemDao.persist(executionSystem);
    }
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.submission.AbstractJobSubmissionTest#initSoftware()
	 */
	@Override
	protected void initSoftware() throws Exception {
		JSONObject json = jtd.getTestDataObject(SOFTWARE_SYSTEM_TEMPLATE_DIR + File.separator + "wc-condor.example.com.json");
		Software software = Software.fromJSON(json, SYSTEM_OWNER);
		software.setOwner(SYSTEM_OWNER);
		
		SoftwareDao.persist(software);
	}
	
//	protected void stageInputData() throws Exception 
//	{
//		remoteFilePath = job.getWorkPath() + "/" + FilenameUtils.getName(TEST_INPUT_FILE);
//		
//		RemoteDataClient remoteDataClient = null;
//		try {
//			remoteDataClient = new SystemDao().findBySystemId(job.getSystem()).getRemoteDataClient(job.getInternalUsername());
//			remoteDataClient.authenticate();
//			remoteDataClient.mkdirs(job.getWorkPath());
//			remoteDataClient.put(TEST_INPUT_FILE, job.getWorkPath());
//			Assert.assertTrue(remoteDataClient.doesExist(remoteFilePath), "Failed to copy input file to remote system");
//		} catch (Exception e) {
//			Assert.fail("Failed to copy input file to remote system");
//		} finally {
//			try {remoteDataClient.disconnect();} catch (Exception e){}
//		}
//	}
	
	protected void stageSoftwareDeploymentDirectory(Software software) throws Exception
	{
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
		} 
		catch (Exception e) {
			Assert.fail("Failed to copy input file to remote system", e);
		} 
		finally {
			try {remoteDataClient.disconnect();} catch (Exception e){}
		}
	}
	
//	protected void initSystems() throws JSONException, IOException, SystemException, PermissionException {
//		JSONObject json = jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + 
//				File.separator + "storage.example.com.json");
//		storageSystem = (StorageSystem) systemManager.parseSystem(json, SYSTEM_OWNER, null);
//		storageSystem.setOwner(SYSTEM_OWNER);
//		storageSystem.getUsersUsingAsDefault().add(SYSTEM_OWNER);
//		systemDao.persist(storageSystem);
//		
//		File executionDir = new File(EXECUTION_SYSTEM_TEMPLATE_DIR, "condor.example.com.json");;
//		json = jtd.getTestDataObject(executionDir.getPath());
//		ExecutionSystem system = (ExecutionSystem) systemManager.parseSystem(json, SYSTEM_OWNER, null);
//		system.setOwner(SYSTEM_OWNER);
//		systemDao.persist(system);
//	}
	
	@DataProvider(name="submitJobProvider")
	public Object[][] submitJobProvider() throws Exception
	{
		List<Software> testApps = SoftwareDao.getUserApps(SYSTEM_OWNER, false);
		
		Object[][] testData = new Object[testApps.size()][3];
		for(int i=0; i< testApps.size(); i++) {
			testData[i] = new Object[] { testApps.get(i), "Submission to " + testApps.get(i).getExecutionSystem().getSystemId() + " failed.", false };
		}
		
		return testData;
	}
	
	@BeforeMethod
	private void beforeMethod() throws IOException {
		// create job work directory on local system and put input file there
		
	}
	
	@AfterMethod
	private void afterMethod() {
		// create job work directory on local system and put input file there
		//FileUtils.deleteQuietly(workDir);
	}
	
	private Job createAndPersistJob(Software software) throws Exception 
	{
		//RemoteDataClient remoteDataClient = null;
		ObjectMapper mapper = new ObjectMapper();
		
		Job job = new Job();
		job.setName( software.getExecutionSystem().getName() + " test");
		job.setArchiveOutput(false);
		job.setArchivePath("/");
		job.setArchiveSystem(storageSystem);
		job.setCreated(new Date());
		job.setMemoryPerNode((double)4);
		job.setOwner(software.getOwner());
		job.setProcessorsPerNode((long)1);
		job.setMaxRunTime("1:00:00");
		job.setSoftwareName(software.getUniqueName());
		job.setStatus(JobStatusType.PENDING, job.getErrorMessage());
		job.setSystem(software.getExecutionSystem().getSystemId());
		job.setBatchQueue(software.getExecutionSystem().getDefaultQueue().getName());
		
		ObjectNode jobInputs = mapper.createObjectNode();
		for(SoftwareInput input: software.getInputs()) {
			jobInputs.put(input.getKey(), "agave://" + storageSystem.getSystemId() + "//etc/hosts");
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
	private Object[][] resolveMacrosProvider() throws Exception {
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
		
//		return new Object[][] { 
//			{job, WrapperTemplateStatusVariableType.AGAVE_JOB_CALLBACK_NOTIFICATION.name(), WrapperTemplateStatusVariableType.AGAVE_JOB_CALLBACK_NOTIFICATION.resolveForJob(job), false  }
//		};
		return testData;
	}
	
	@Test (groups={"submission"}, dataProvider="resolveMacrosProvider", enabled=true)
	public void resolveMacros(Job job, String macro, String expectedValue, boolean archive) throws JobException {
	    IPhaseWorker worker = Mockito.mock(IPhaseWorker.class);
		JobLauncher launcher = new CondorLauncher(job, worker);
		Assert.assertEquals(launcher.resolveMacros("${" + macro + "}"), expectedValue, "Launcher did not resolve wrapper template macro " + macro + " properly.");
	}
	
	@DataProvider
	private Object[][] resolveNotificationsMacrosProvider() throws Exception {
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
	
	@Test (groups={"submission"}, dataProvider="resolveNotificationsMacrosProvider", dependsOnMethods={"resolveMacros"}, enabled=true)
	public void resolveNotificationMacros(Job job, String macroVars, String expectedValue, boolean archive) throws JobException {
	    IPhaseWorker worker = Mockito.mock(IPhaseWorker.class);
		JobLauncher launcher = new CondorLauncher(job, worker);
		Assert.assertEquals(launcher.resolveRuntimeNotificationMacros("${AGAVE_JOB_CALLBACK_NOTIFICATION|" + macroVars + "}"), expectedValue, "Launcher did not resolve wrapper template notification macro properly.");
	}
	
//	@Test (groups={"submission"}, dataProvider="processApplicationTemplateProvider", dependsOnMethods={"resolveMacros"})
//	public void processApplicationTemplate(Software software, String message, boolean shouldThrowException) 
//	throws Exception 
//	{
//		job = createAndPersistJob(software);
//		super.genericProcessApplicationTemplate(job, message, shouldThrowException);
//	}
	
	@Test (groups={"submission"}, dataProvider="submitJobProvider", dependsOnMethods={"resolveMacros"}, enabled=true)
	public void submitJob(Software software, String message, boolean shouldThrowException) throws Exception
	{
		job = createAndPersistJob(software);
		
		stageSoftwareDeploymentDirectory(software);
		
//		stageSoftwareInputDefaultData(software);
		
		stageJobInputs(job);
		
		super.genericRemoteSubmissionTestCase(job, JobStatusType.QUEUED, "Condor job submission failed", false);
	}
	

}
