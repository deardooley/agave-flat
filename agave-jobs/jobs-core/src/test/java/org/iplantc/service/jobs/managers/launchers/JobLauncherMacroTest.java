package org.iplantc.service.jobs.managers.launchers;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

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
import org.iplantc.service.jobs.model.JobUpdateParameters;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.model.enumerations.WrapperTemplateAttributeVariableType;
import org.iplantc.service.jobs.model.enumerations.WrapperTemplateStatusVariableType;
import org.iplantc.service.jobs.submission.AbstractJobSubmissionTest;
import org.iplantc.service.jobs.util.Slug;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemUnavailableException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.enumerations.ExecutionType;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.systems.model.enumerations.SchedulerType;
import org.iplantc.service.systems.model.enumerations.StorageProtocolType;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;


/**
 * Tests macro resolution in software wrapper templates by a {@link JobLauncher}
 * implementation. This class is parameterized in the constructor so it can be 
 * initialized and used in the {@link JobLauncherMacroTestFactory} factory class.
 * @author dooley
 *
 */
public class JobLauncherMacroTest extends AbstractJobSubmissionTest {

	private Software software;
	private SchedulerType schedulerType;
	
	public JobLauncherMacroTest(SchedulerType schedulerType) {
		super();
		this.schedulerType = schedulerType;
	}

	@BeforeClass
	@Override
	protected void beforeClass() throws Exception {
		super.beforeClass();
	}

	@AfterClass
	@Override
	protected void afterClass() throws Exception {
	}

	@Override
	protected void initSystems() throws Exception {
		SystemDao dao = new SystemDao();
	    storageSystem = (StorageSystem) getNewInstanceOfRemoteSystem(RemoteSystemType.STORAGE, 
	    		getStorageSystemProtocolType().name().toLowerCase());
        storageSystem.setOwner(SYSTEM_OWNER);
        storageSystem.setPubliclyAvailable(true);
        storageSystem.setGlobalDefault(true);
        storageSystem.getUsersUsingAsDefault().add(SYSTEM_OWNER);
        dao.persist(storageSystem);
        
        executionSystem = (ExecutionSystem)getNewInstanceOfRemoteSystem(RemoteSystemType.EXECUTION, 
        		getExectionSystemSchedulerType().name().toLowerCase());
        executionSystem.setOwner(SYSTEM_OWNER);
        executionSystem.setPubliclyAvailable(true);
        executionSystem.setType(RemoteSystemType.EXECUTION);
        dao.persist(executionSystem);
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
	 * The {@link StorageProtocolType} maps to a JSON file named 
	 * as systems/storage/{@link StorageProtocolType#name()}.example.com.json
	 * 
	 * @return the protocol to use for the storage system
	 */
	protected StorageProtocolType getStorageSystemProtocolType() {
		return StorageProtocolType.SFTP;
	}
	
	/**
	 * Creates a {@link Mockity.spy(JobLauncher)} instance returning non-persistent
	 * {@link Job}, {@link Software}, and {@link ExecutionSystem} objects when needed.
	 * 
	 * @param job the job to associate with the returned {@link JobLauncher.
	 * @return a {@link Mockity.spy(JobLauncher)} instance valid for the given {@code job}.
	 */
	protected JobLauncher getTestJobLauncher(Job job) {
		JobLauncher launcher = null;
		if (software.getExecutionSystem().getExecutionType().equals(ExecutionType.HPC)) {
			launcher = spy(new HPCLauncher());
		}
		else if (software.getExecutionSystem().getExecutionType().equals(ExecutionType.CONDOR)) {
			launcher = spy(new CondorLauncher());
		}
		else {
			launcher = spy(new CLILauncher());
		}
		
		when(launcher.getSoftware()).thenReturn(software);
		when(launcher.getExecutionSystem()).thenReturn(executionSystem);
		when(launcher.getJob()).thenReturn(job);
		
		return launcher;
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
	
	protected Job createJob(Software software) throws Exception {
		
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
		job.initStatus(JobStatusType.PENDING, job.getErrorMessage());
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
		
		JobDao.create(job);
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
        JobUpdateParameters parms = new JobUpdateParameters();
        parms.setWorkPath(job.getWorkPath());
        JobDao.update(job, parms);
		
		return job;
	}

	@DataProvider
	protected Object[][] resolveMacrosProvider() throws Exception {
		List<Software> testApps = SoftwareDao.getUserApps(SYSTEM_OWNER, false);
		
		Job job = createJob(testApps.get(0));
		
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
	
	@Test(groups = { "job", "launcher", "macros" }, dataProvider = "resolveMacrosProvider", enabled = true)
	public void resolveMacros(Job job, String macro, String expectedValue, boolean archive) 
	throws JobException, SystemUnavailableException, SoftwareUnavailableException 
	{
		JobLauncher launcher = getTestJobLauncher(job);
		
		Assert.assertEquals(launcher.resolveMacros("${" + macro + "}"), expectedValue, "Launcher did not resolve wrapper template macro " + macro + " properly.");
	}
	
	@DataProvider
	protected Object[][] resolveNotificationsMacrosProvider() throws Exception {
		
		Job job = createJob(software);
		
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

	@Test(groups = { "job", "launcher", "macros" }, dataProvider = "resolveNotificationsMacrosProvider", dependsOnMethods = { "resolveMacros" }, enabled = true)
	public void resolveNotificationMacros(Job job, String macroVars, String expectedValue, boolean archive) 
	throws JobException, SystemUnavailableException, SoftwareUnavailableException 
	{
		JobLauncher launcher = getTestJobLauncher(job);
		
		Assert.assertEquals(launcher.resolveRuntimeNotificationMacros("${AGAVE_JOB_CALLBACK_NOTIFICATION|" + macroVars + "}"), expectedValue, "Launcher did not resolve wrapper template notification macro properly.");
	}
}