/**
 * 
 */
package org.iplantc.service.jobs.submission;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.common.util.Slug;
import org.iplantc.service.jobs.model.JSONTestDataUtil;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Tests the placing of a submitted job into the remote HPC system queue.
 * Tests should run across multiple systems and schedulers.
 * @author dooley
 *
 */
public class RemoteHPCJobSubmissionTest extends AbstractJobSubmissionTest 
{
	private ObjectMapper mapper = new ObjectMapper();
	
	private Job job;
	private JSONTestDataUtil jtd;
	private SystemDao systemDao;
	private SystemManager systemManager;
	private StorageSystem storageSystem;
	
	@DataProvider(name="submitJobProvider")
	public Object[][] submitJobProvider() throws JSONException, IOException, SystemArgumentException, SystemException, PermissionException
	{
		systemDao = new SystemDao();
		systemManager = new SystemManager();
		
		jtd = JSONTestDataUtil.getInstance();
		
		JSONObject json = jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + 
				File.separator + "storage.example.com.json");
		storageSystem = (StorageSystem) systemManager.parseSystem(json, SYSTEM_OWNER, null);
		storageSystem.setOwner(SYSTEM_OWNER);
		storageSystem.setPubliclyAvailable(true);
		storageSystem.setGlobalDefault(true);
		systemDao.persist(storageSystem);
		
		File executionDir = new File(EXECUTION_SYSTEM_TEMPLATE_DIR);
		for(File jsonFile: executionDir.listFiles()) {
			json = jtd.getTestDataObject(jsonFile.getPath());
			ExecutionSystem system = (ExecutionSystem) systemManager.parseSystem(json, SYSTEM_OWNER, null);
			system.setOwner(SYSTEM_OWNER);
			systemDao.persist(system);
		}
		
		File softwareDir = new File(SOFTWARE_SYSTEM_TEMPLATE_DIR);
		for(File jsonFile: softwareDir.listFiles()) {
			json = jtd.getTestDataObject(jsonFile.getPath());
			Software software = Software.fromJSON(json, SYSTEM_OWNER);
			software.setOwner(SYSTEM_OWNER);
			SoftwareDao.persist(software);
		}
		
		List<Software> testApps = SoftwareDao.getUserApps(SYSTEM_OWNER, false);
		Object[][] testData = new Object[testApps.size()][3];
		int i=0;
		for(Software software: testApps) {
			testData[i] = new Object[] { software, "Submission to " + software.getExecutionSystem().getSystemId() + " works.", false };
		}
		
		return testData;
	}
	
	@Test (groups={"submission"}, dataProvider="submitJobProvider")
	public void submitJob(Software software, String message, boolean shouldThrowException) throws Exception
	{
		job = new Job();
		job.setName( software.getExecutionSystem().getName() + " test");
		job.setArchiveOutput(false);
		job.setArchivePath(job.getOwner() + "/archive/jobs/job-" + job.getUuid() + "-" + Slug.toSlug(job.getName()));
		job.setArchiveSystem(storageSystem);
		job.setCreated(new Date());
		job.setMemoryPerNode((double)512);
		job.setOwner("dooley");
		job.setProcessorsPerNode((long)1);
		job.setMaxRunTime("1:00:00");
		job.setSoftwareName(software.getUniqueName());
		job.setStatus(JobStatusType.STAGED, "Job inputs staged to execution system");
		job.setSystem(software.getExecutionSystem().getSystemId());
		
		ObjectNode jobInputs = mapper.createObjectNode()
				.put("inputfile", "dooley/lorem.txt");
		
		job.setInputsAsJsonObject(jobInputs);
		
		ObjectNode jobParameters = mapper.createObjectNode()
				.put("numberofbytes","")
				.put("numberoflines","10");
		job.setParametersAsJsonObject(jobParameters);
		
		super.genericRemoteSubmissionTestCase(job, JobStatusType.QUEUED, "Job submission to stampede succeeds", false);
	}
//	
//	public void jobStampedeAppCheck()  throws JSONException, JobException
//	{
//		job = new Job();
//		job.setName("stampede-test");
//		job.setArchiveOutput(true);
//		job.setArchivePath("/dooley/archive/test");
//		job.setCreated(new Date());
//		job.setMemoryPerNode((double)512);
//		job.setOwner("dooley");
//		job.setProcessorsPerNode((long)1);
//		job.setMaxRunTime("1:00:00");
//		job.setSoftwareName("head-stampede-5.97");
//		job.setStatus(JobStatusType.STAGED, "Job inputs staged to execution system");
//		job.setSystem("stampede.tacc.xsede.org");
//		
//		ObjectNode jobInputs = mapper.createObjectNode()
//				.put("inputfile", "dooley/lorem.txt");
//		
//		job.setInputsAsJsonObject(jobInputs);
//		
//		ObjectNode jobParameters = mapper.createObjectNode()
//				.put("numberofbytes","")
//				.put("numberoflines","10");
//		job.setParametersAsJsonObject(jobParameters);
//		
//		super.genericRemoteSubmissionTestCase(job, "Job submission to stampede succeeds", false);
//	}
//	
//	@Test
//	public void jobTrestlesAppCheck() throws JSONException, JobException 
//	{
//		job = new Job();
//		job.setName("trestles-test");
//		job.setArchiveOutput(true);
//		job.setArchivePath("/dooley/archive/test");
//		job.setCreated(new Date());
//		job.setMemoryPerNode((double)512);
//		job.setOwner("dooley");
//		job.setProcessorsPerNode((long)1);
//		job.setMaxRunTime("1:00:00");
//		job.setSoftwareName("head-trestles-5.97");
//		job.setStatus(JobStatusType.STAGED, "Job inputs staged to execution system");
//		job.setSystem("trestles.sdsc.teragrid.org");
//		
//		ObjectNode jobInputs = mapper.createObjectNode()
//				.put("inputfile", "dooley/lorem.txt");
//		
//		job.setInputsAsJsonObject(jobInputs);
//		
//		ObjectNode jobParameters = mapper.createObjectNode()
//				.put("numberofbytes","")
//				.put("numberoflines","10");
//		job.setParametersAsJsonObject(jobParameters);
//		
//		super.genericRemoteSubmissionTestCase(job, "Job submission to trestles succeeds", false);
//	}
}
