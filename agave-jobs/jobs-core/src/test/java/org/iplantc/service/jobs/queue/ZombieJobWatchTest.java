package org.iplantc.service.jobs.queue;

import java.io.File;
import java.util.Date;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwareInput;
import org.iplantc.service.apps.model.SoftwareParameter;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.JSONTestDataUtil;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.submission.AbstractJobSubmissionTest;
import org.iplantc.service.jobs.util.Slug;
import org.iplantc.service.profile.dao.InternalUserDao;
import org.iplantc.service.profile.exceptions.ProfileException;
import org.iplantc.service.profile.model.InternalUser;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Test(groups={"broken", "integration"})
public class ZombieJobWatchTest extends AbstractJobSubmissionTest {

	protected static String LOCAL_TXT_FILE = "src/test/resources/transfer/test_upload.txt";

	private JSONTestDataUtil jtd;
	private SystemDao systemDao = new SystemDao();
	private SystemManager systemManager = new SystemManager();

	@BeforeClass
	public void beforeClass() throws Exception {
		clearInternalUsers();
		clearSoftware();
		clearSystems();
		clearJobs();

		jtd = JSONTestDataUtil.getInstance();

		JSONObject json = null;

		String testSystemDescriptionFilePath = STORAGE_SYSTEM_TEMPLATE_DIR
				+ File.separator + "storage.example.com.json";
		StorageSystem storageSystem = null;
		try {
			json = jtd.getTestDataObject(testSystemDescriptionFilePath);
			storageSystem = (StorageSystem) systemManager.parseSystem(json,
					SYSTEM_OWNER, null);
			storageSystem.setOwner(SYSTEM_OWNER);
			storageSystem.setGlobalDefault(true);
			storageSystem.setPubliclyAvailable(true);
			systemDao.persist(storageSystem);
		} catch (Exception e) {
			Assert.fail(
					"Unable to create default storage system from description in file "
							+ testSystemDescriptionFilePath, e);
		}

		json = jtd.getTestDataObject(EXECUTION_SYSTEM_TEMPLATE_DIR
				+ File.separator + "execute.example.com.json");
		ExecutionSystem system = (ExecutionSystem) systemManager.parseSystem(
				json, SYSTEM_OWNER, null);
		system.setOwner(SYSTEM_OWNER);
		systemDao.persist(system);

		// register an app on the system
		json = jtd.getTestDataObject(SOFTWARE_SYSTEM_TEMPLATE_DIR
				+ "/system-software.json");
		json.put("name", "zombie-test");
		json.put("executionSystem", system.getSystemId());
		json.put("deploymentSystem", systemManager.getDefaultStorageSystem()
				.getSystemId());
		Software software = Software.fromJSON(json, SYSTEM_OWNER);
		software.setOwner(SYSTEM_OWNER);
		SoftwareDao.persist(software);

	}

	public void clearSystems() {
		for (RemoteSystem system : systemDao.getAll()) {
			systemDao.remove(system);
		}
	}

	public void clearSoftware() {
		for (Software software : SoftwareDao.getAll()) {
			SoftwareDao.delete(software);
		}
	}

	public void clearJobs() throws JobException {
		for (Job job : JobDao.getAll()) {
			JobDao.delete(job);
		}
	}

	private void clearInternalUsers() throws ProfileException {
		InternalUserDao internalUserDao = new InternalUserDao();

		for (InternalUser internalUser : internalUserDao.getAll()) {
			internalUserDao.delete(internalUser);
		}
	}

	@AfterClass
	public void afterClass() throws Exception {
		clearInternalUsers();
		clearJobs();
		clearSoftware();
		clearSystems();
	}

	public Job createJob(Software software, String inputUri)
			throws JobException, JSONException {
		return createJob(software, new String[] { inputUri });
	}

	public Job createJob(Software software, String[] inputUris)
			throws JobException, JSONException {
		ObjectMapper mapper = new ObjectMapper();

		Job job = new Job();
		job.setName(software.getExecutionSystem().getName() + " test");
		job.setArchiveOutput(false);
		job.setArchivePath("/");
		job.setArchiveSystem(software.getStorageSystem());
		job.setCreated(new Date());
		job.setMemoryPerNode((double) 512);
		job.setOwner(software.getExecutionSystem().getOwner());
		job.setProcessorsPerNode((long) 1);
		job.setMaxRunTime("1:00:00");
		job.setSoftwareName(software.getUniqueName());
		job.setStatus(JobStatusType.STAGED,
				"Job inputs staged to execution system");
		job.setSystem(software.getExecutionSystem().getSystemId());
		job.setBatchQueue(software.getExecutionSystem().getDefaultQueue()
				.getName());

		String remoteWorkPath = null;
		if (StringUtils.isEmpty(software.getExecutionSystem().getScratchDir())) {
			remoteWorkPath = job.getOwner() + "/job-" + job.getUuid() + "-"
					+ Slug.toSlug(job.getName()) + "/"
					+ FilenameUtils.getName(software.getDeploymentPath());
		} else {
			remoteWorkPath = software.getExecutionSystem().getScratchDir()
					+ job.getOwner() + "/job-" + job.getUuid() + "-"
					+ Slug.toSlug(job.getName()) + "/"
					+ FilenameUtils.getName(software.getDeploymentPath());
		}
		job.setWorkPath(remoteWorkPath);

		ObjectNode jsonInputs = mapper.createObjectNode();
		for (SoftwareInput input : software.getInputs()) {
			ArrayNode inputValues = mapper.createArrayNode();
			for (String uri : inputUris) {
				inputValues.add(uri);
			}
			jsonInputs.put(input.getKey(), inputValues);
		}
		job.setInputsAsJsonObject(jsonInputs);

		ObjectNode jsonParameters = mapper.createObjectNode();
		for (SoftwareParameter param : software.getParameters()) {
			jsonParameters.put(param.getKey(),
					param.getDefaultValueAsJsonArray());
		}
		job.setParametersAsJsonObject(jsonParameters);

		return job;
	}

	@Test
	public void cancelCurrentTransfers() {

	}

	@Test
	public void doExecute() {
		throw new RuntimeException("Test not implemented");
	}
}
