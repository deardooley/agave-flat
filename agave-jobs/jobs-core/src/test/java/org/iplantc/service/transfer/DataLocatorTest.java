package org.iplantc.service.transfer;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwareInput;
import org.iplantc.service.apps.model.SoftwareParameter;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.jobs.model.FileBean;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.util.DataLocator;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.jobs.model.JSONTestDataUtil;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.json.JSONObject;
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
 * Verifies that job data can be found at any time once the job has started to run.
 *
 * @author dooley
 *
 */
public class DataLocatorTest
{
	public static final String SYSTEM_OWNER = "testuser";
	public static final String SYSTEM_SHARE_USER = "bob";
	public static final String SYSTEM_PUBLIC_USER = "public";
	public static final String SYSTEM_UNSHARED_USER = "dan";
	public static final String SYSTEM_INTERNAL_USERNAME = "test_user";

	public static String EXECUTION_SYSTEM_TEMPLATE_DIR = "src/test/resources/systems/execution";
	public static String STORAGE_SYSTEM_TEMPLATE_DIR = "src/test/resources/systems/storage";
	public static String SOFTWARE_SYSTEM_TEMPLATE_DIR = "src/test/resources/software";

	protected JSONTestDataUtil jtd;
	protected JSONObject jsonTree;

	private SystemDao systemDao = new SystemDao();
	private SystemManager systemManager = new SystemManager();
	private Job job;

	@BeforeMethod
	public void beforeMethod() throws Exception
	{
		clearSystems();
		clearJobs();
	}

	@AfterMethod
	public void afterMethod() throws Exception
	{
		clearSystems();
		clearJobs();
	}

	@DataProvider(name="listOutputDirectoryProvider")
	public Object[][] listOutputDirectoryProvider() throws Exception
	{
		return new Object[][] {
				new Object[] { createJob(JobStatusType.PENDING, "gsissh.example.com", "irods.example.com", false), false },
				new Object[] { createJob(JobStatusType.PROCESSING_INPUTS, "gsissh.example.com", "irods.example.com", false), false },
				new Object[] { createJob(JobStatusType.STAGING_INPUTS, "gsissh.example.com", "irods.example.com", false), false },
				new Object[] { createJob(JobStatusType.STAGED, "gsissh.example.com", "irods.example.com", false), false },
				new Object[] { createJob(JobStatusType.STAGING_JOB, "gsissh.example.com", "irods.example.com", false), false },
				new Object[] { createJob(JobStatusType.SUBMITTING, "gsissh.example.com", "irods.example.com", false), false },
				new Object[] { createJob(JobStatusType.QUEUED, "gsissh.example.com", "irods.example.com", false), false },
				new Object[] { createJob(JobStatusType.RUNNING, "gsissh.example.com", "irods.example.com", false), true },
				new Object[] { createJob(JobStatusType.FAILED, "gsissh.example.com", "irods.example.com", false), true },
				new Object[] { createJob(JobStatusType.STOPPED, "gsissh.example.com", "irods.example.com", false), true },
				new Object[] { createJob(JobStatusType.KILLED, "gsissh.example.com", "irods.example.com", false), true },
				new Object[] { createJob(JobStatusType.FINISHED, "gsissh.example.com", "irods.example.com", false), true },
				new Object[] { createJob(JobStatusType.PAUSED, "gsissh.example.com", "irods.example.com", false), true },
				new Object[] { createJob(JobStatusType.ARCHIVING, "gsissh.example.com", "irods.example.com", false), true },
				new Object[] { createJob(JobStatusType.ARCHIVING_FAILED, "gsissh.example.com", "irods.example.com", false), true },
				new Object[] { createJob(JobStatusType.ARCHIVING_FINISHED, "gsissh.example.com", "irods.example.com", false), true },
		};
	}

	@Test(dataProvider = "listOutputDirectoryProvider")
	public void listOutputDirectory(Job job, boolean shouldlocateData)
	throws Exception
	{
		ExecutionSystem exeSystem = (ExecutionSystem)systemDao.findBySystemId(job.getSystem());

		// stage data to execution system if it started running
		if (job.isArchiveOutput())
		{
			stageData(job.getArchiveSystem(), "src/test/resources/data", job.getArchivePath());
		}

		// stage data to archive system if archived
		if (!StringUtils.isEmpty(job.getWorkPath()))
		{
			stageData(exeSystem, "src/test/resources/data", job.getWorkPath());
		}

		try {
			DataLocator dataLocator = new DataLocator(job);
			List<FileBean> outputFiles = dataLocator.listOutputDirectory(job.getArchivePath());

			if (shouldlocateData)
				Assert.assertFalse(outputFiles.isEmpty(), "No data found when it should have been");
			else
				Assert.assertTrue(outputFiles.isEmpty(), "Found data when it was not expected");
		}
		finally
		{
			// stage data to execution system if it started running
			if (job.isArchiveOutput())
			{
				deleteData(job.getArchiveSystem(), job.getArchivePath() + "/" + "data");
			}

			// stage data to archive system if archived
			if (!StringUtils.isEmpty(job.getWorkPath()))
			{
				deleteData(exeSystem, job.getWorkPath() + "/" + "data");
			}
		}
	}

	public void stageData(RemoteSystem system, String localPath, String remotePath) throws Exception {
		RemoteDataClient dataClient = null;
		try {
			dataClient = system.getRemoteDataClient();
			dataClient.put("src/test/resources/data", remotePath);
		} finally {
			try { dataClient.disconnect(); } catch (Exception e) {}
		}
	}

	public void deleteData(RemoteSystem system, String remotePath) throws Exception {
		RemoteDataClient dataClient = null;
		try {
			dataClient = system.getRemoteDataClient();
			dataClient.delete(remotePath);
		} finally {
			try { dataClient.disconnect(); } catch (Exception e) {}
		}
	}

	@BeforeClass
	public void beforeClass() throws Exception
	{
		jtd = JSONTestDataUtil.getInstance();
		setupSystems();
		setupSoftware();
	}

	@AfterClass
	public void afterClass() throws Exception
	{
		clearSystems();
		clearJobs();
		clearSoftware();
	}

	protected void clearSystems() throws Exception {
		Session session = null;
		try
		{
			HibernateUtil.beginTransaction();
			session = HibernateUtil.getSession();
			session.clear();
			HibernateUtil.disableAllFilters();

			session.createQuery("DELETE FROM RemoteSystem s WHERE 1=1").executeUpdate();
			session.createQuery("DELETE FROM BatchQueue s WHERE 1=1").executeUpdate();
			session.createQuery("DELETE FROM StorageConfig s WHERE 1=1").executeUpdate();
			session.createQuery("DELETE FROM LoginConfig s WHERE 1=1").executeUpdate();
			session.createQuery("DELETE FROM AuthConfig s WHERE 1=1").executeUpdate();
			session.createQuery("DELETE FROM SystemRole s WHERE 1=1").executeUpdate();
			session.createQuery("DELETE FROM CredentialServer s WHERE 1=1").executeUpdate();
		}
		catch (HibernateException ex)
		{
			throw new SystemException(ex);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	protected void clearSoftware() throws Exception {
		Session session = null;
		try
		{
			HibernateUtil.beginTransaction();
			session = HibernateUtil.getSession();
			session.clear();
			HibernateUtil.disableAllFilters();

			session.createQuery("DELETE FROM Software s WHERE 1=1").executeUpdate();
			session.createQuery("DELETE FROM SoftwarePermission s WHERE 1=1").executeUpdate();
		}
		catch (HibernateException ex)
		{
			throw new SystemException(ex);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	protected void clearJobs() throws Exception {
		Session session = null;
		try
		{
			HibernateUtil.beginTransaction();
			session = HibernateUtil.getSession();
			session.clear();
			HibernateUtil.disableAllFilters();

			session.createQuery("DELETE FROM Job s WHERE 1=1").executeUpdate();
		}
		catch (HibernateException ex)
		{
			throw new SystemException(ex);
		}
		finally
		{
			try { HibernateUtil.commitTransaction(); } catch (Exception e) {}
		}
	}

	protected void setupSystems() throws Exception
	{
		JSONObject json = null;

		File storageDir = new File(STORAGE_SYSTEM_TEMPLATE_DIR);
		for(File jsonFile: storageDir.listFiles()) {
			json = jtd.getTestDataObject(jsonFile.getPath());
			StorageSystem system = (StorageSystem) systemManager.parseSystem(json, SYSTEM_OWNER, null);
			system.setOwner(SYSTEM_OWNER);
			systemDao.persist(system);
		}

		File executionDir = new File(EXECUTION_SYSTEM_TEMPLATE_DIR);
		for(File jsonFile: executionDir.listFiles()) {
			json = jtd.getTestDataObject(jsonFile.getPath());
			ExecutionSystem system = (ExecutionSystem) systemManager.parseSystem(json, SYSTEM_OWNER, null);
			system.setOwner(SYSTEM_OWNER);
			systemDao.persist(system);
		}
	}

	private void setupSoftware() throws Exception
	{
		String softwareDescriptionPath = SOFTWARE_SYSTEM_TEMPLATE_DIR + "/" + "system-software.json";
		JSONObject json = jtd.getTestDataObject(softwareDescriptionPath);

		for(RemoteSystem system: systemDao.getUserSystems(SYSTEM_OWNER, true, RemoteSystemType.EXECUTION)) {
			Software software = Software.fromJSON(json, SYSTEM_OWNER);
			software.setExecutionSystem((ExecutionSystem)system);
			software.setOwner(SYSTEM_OWNER);
			SoftwareDao.persist(software);
		}
	}

	private Job createJob(JobStatusType jobStatus, String executionSystemId, String archiveSystemId, boolean shouldArchive)
	throws Exception
	{
		ObjectMapper mapper = new ObjectMapper();
		StorageSystem archiveSystem = (StorageSystem)systemDao.findBySystemId(archiveSystemId);
		ExecutionSystem executionSystem = (ExecutionSystem)systemDao.findBySystemId(archiveSystemId);

		Software software = SoftwareDao.getAllBySystemId(executionSystemId).get(0);

		job = new Job();
		job.setName("datalocator test");
		job.setArchiveOutput(shouldArchive);
		job.setArchivePath(archiveSystem.getStorageConfig().getHomeDir());
		job.setArchiveSystem(archiveSystem);
		job.setCreated(new Date());
		job.setMemoryPerNode((double)512);
		job.setOwner("dooley");
		job.setProcessorsPerNode((long)1);
		job.setMaxRunTime("1:00:00");
		job.setSoftwareName(software.getUniqueName());
		job.initStatus(jobStatus, (String)null);
		job.setSystem(executionSystemId);

		if (!JobStatusType.hasQueued(jobStatus) && !jobStatus.equals(JobStatusType.QUEUED)) {
			job.setWorkPath(executionSystem.getStorageConfig().getHomeDir());
		}

		ObjectNode inputs = mapper.createObjectNode();
		for(SoftwareInput swInput: software.getInputs()) {
			inputs.put(swInput.getKey(), swInput.getDefaultValueAsJsonArray());
		}
		job.setInputsAsJsonObject(inputs);

		ObjectNode parameters = mapper.createObjectNode();
		for(SoftwareParameter swParameter: software.getParameters()) {
			parameters.put(swParameter.getKey(), swParameter.getDefaultValueAsJsonArray());
		}
		job.setParametersAsJsonObject(parameters);

		return job;
	}
}
