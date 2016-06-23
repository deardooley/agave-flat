package org.iplantc.service.jobs.managers.launchers;

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
import org.iplantc.service.jobs.submission.AbstractJobSubmissionTest;
import org.iplantc.service.jobs.util.Slug;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.jobs.model.JSONTestDataUtil;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.transfer.RemoteDataClient;
import org.json.JSONException;
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
 * Created with IntelliJ IDEA.
 * User: steve
 * Date: 1/14/13
 * Time: 3:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class CondorLauncherTest extends AbstractJobSubmissionTest
{
	private Job job;
	private JSONTestDataUtil jtd;
	private SystemManager systemManager = new SystemManager();
	private StorageSystem storageSystem;
	private String remoteFilePath;
	
	@BeforeClass
	public void beforeClass() throws Exception 
	{
		clearSoftware();
		clearSystems();
		clearJobs();
		
		jtd = JSONTestDataUtil.getInstance();
		
		initSystems();
		
		initSoftware();
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
	
	protected void initSoftware() throws Exception {
		File softwareDir = new File(SOFTWARE_SYSTEM_TEMPLATE_DIR, "/wc-condor.example.com.json");
		JSONObject json = jtd.getTestDataObject(softwareDir.getPath());
		Software software = Software.fromJSON(json, SYSTEM_OWNER);
		software.setOwner(SYSTEM_OWNER);
		
		SoftwareDao.persist(software);
	}
	
	protected void stageInputData() throws Exception 
	{
		remoteFilePath = job.getWorkPath() + "/" + FilenameUtils.getName(TEST_INPUT_FILE);
		
		RemoteDataClient remoteDataClient = null;
		try {
			remoteDataClient = new SystemDao().findBySystemId(job.getSystem()).getRemoteDataClient(job.getInternalUsername());
			remoteDataClient.authenticate();
			remoteDataClient.mkdirs(job.getWorkPath());
			remoteDataClient.put(TEST_INPUT_FILE, job.getWorkPath());
			Assert.assertTrue(remoteDataClient.doesExist(remoteFilePath), "Failed to copy input file to remote system");
		} catch (Exception e) {
			Assert.fail("Failed to copy input file to remote system");
		} finally {
			try {remoteDataClient.disconnect();} catch (Exception e){}
		}
	}
	
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
		} catch (Exception e) {
			Assert.fail("Failed to copy input file to remote system");
		} finally {
			try {remoteDataClient.disconnect();} catch (Exception e){}
		}
	}
	
	protected void initSystems() throws JSONException, IOException, SystemException, PermissionException {
		JSONObject json = jtd.getTestDataObject(STORAGE_SYSTEM_TEMPLATE_DIR + 
				File.separator + "storage.example.com.json");
		storageSystem = (StorageSystem) systemManager.parseSystem(json, SYSTEM_OWNER, null);
		storageSystem.setOwner(SYSTEM_OWNER);
		storageSystem.getUsersUsingAsDefault().add(SYSTEM_OWNER);
		systemDao.persist(storageSystem);
		
		File executionDir = new File(EXECUTION_SYSTEM_TEMPLATE_DIR, "condor.example.com.json");;
		json = jtd.getTestDataObject(executionDir.getPath());
		ExecutionSystem system = (ExecutionSystem) systemManager.parseSystem(json, SYSTEM_OWNER, null);
		system.setOwner(SYSTEM_OWNER);
		systemDao.persist(system);
	}
	
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
		job.setStatus(JobStatusType.PENDING, "Job accepted and queued for submission.");
		job.setSystem(software.getExecutionSystem().getSystemId());
		job.setBatchQueue(software.getExecutionSystem().getDefaultQueue().getName());
		
		ObjectNode jobInputs = mapper.createObjectNode();
		for(SoftwareInput input: software.getInputs()) {
			jobInputs.put(input.getKey(), remoteFilePath);
		}
		job.setInputsAsJsonObject(jobInputs);
		
		ObjectNode jobParameters = mapper.createObjectNode();
		for (SoftwareParameter parameter: software.getParameters()) {
			jobParameters.put(parameter.getKey(), parameter.getDefaultValueAsJsonArray());
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
		
		return testData;
	}
	
	
	@Test (groups={"submission"}, dataProvider="resolveMacrosProvider")
	public void resolveMacros(Job job, String macro, String expectedValue, boolean archive) throws JobException {
		JobLauncher launcher = new CondorLauncher(job);
		Assert.assertEquals(launcher.resolveMacros("${" + macro + "}"), expectedValue, "Launcher did not resolve wrapper template macro " + macro + " properly.");
	}
	
//	@Test (groups={"submission"}, dataProvider="processApplicationTemplateProvider", dependsOnMethods={"resolveMacros"})
//	public void processApplicationTemplate(Software software, String message, boolean shouldThrowException) 
//	throws Exception 
//	{
//		job = createAndPersistJob(software);
//		super.genericProcessApplicationTemplate(job, message, shouldThrowException);
//	}
	
	@Test (groups={"submission"}, dataProvider="submitJobProvider", dependsOnMethods={"resolveMacros"})
	public void submitJob(Software software, String message, boolean shouldThrowException) throws Exception
	{
		job = createAndPersistJob(software);
		
		stageSoftwareDeploymentDirectory(software);
		
		stageInputData();
		
		super.genericRemoteSubmissionTestCase(job, JobStatusType.QUEUED, "Condor job submission failed", false);
	}
	

//    /**
//     * reset flushes the directory and setsup the data in the database to run the test
//     * @return Boolean value from the gsql calls to setup database
//     */
//    @Test(disabled="true")
//    def reset(){
//        root.mkdirs()
//        def dirs = []
//        root.eachFileRecurse { file ->
//            if(file.isDirectory()){ dirs << file }
//            else{ file.delete() }
//        }
//        dirs.each { file ->
//            file.deleteDir()
//        }
//        // reset the database with well known data
//        GSqlData gsd = new GSqlData();
//        gsd.setupKnownJobAndSoftwareValues()
//    }

//    @BeforeClass
//    void setup(){
//        System.out.println("in setup");
//        // assume Condor job and setup already exist
///*
//		CommonHibernateTest.initdb();
//        dao = new SystemDao();
//        SystemManager sysManager = new SystemManager();
//        SystemDao systemDao = new SystemDao();
//
//        JobStoreSoftExecSystemSetup jobrecord = new JobStoreSoftExecSystemSetup();
//        jobrecord.gSqlData.cleanAllTablesByRecord()
//
//        job = jobrecord.insertFullJobTestRecordObjectGraph();
//*/
//    }
//	
//    @Test
//    void testLaunch() throws InterruptedException {
//
//        try
//        {
//            launcher = new CondorLauncher(job);
//            launcher.launch();
//        } 
//		catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        JobStatusType actualStatus = job.getStatus();
//        JobStatusType expectedStatus = JobStatusType.RUNNING;
//
//        boolean result = (actualStatus == expectedStatus ) ? true : false;
//        sleep(10000);
//        Assert.assertTrue(result,"The status is RUNNING");
//        // expectedLocalJobId should not be NULL but can be any integer value
//
//    }
//
//    public static void main(String[] args) throws InterruptedException {
//        System.out.println("this works ...");
//        CondorLauncherTest gcl = new CondorLauncherTest();
//        gcl.setup();
//        gcl.testLaunch();
//
//
//    }

    /*@Test(dependsOnMethods=("testLaunch") )
    void testReturnFromCondor(){
        sleep(10000)   // wait on condor_submit to completely finish
        boolean fileExists = new File(launcher.getTempAppDirPath()+"/wc_out.txt").exists()
        Assert.assertTrue("Our wc output file exits", fileExists)
    }
*/
}

/*
GCondorLaunchera gc = new GCondorLaunchera()
gc.reset()
*/

//actual = new File(launcher.tempAppDirPath+"/wc_out.txt").text
/*
// we are looking for the wc_out.txt result file and it's contents
String expected = "  400004  400004 14582797 wc-1.00/read1.fq\n";
Assert.assertTrue("not implemented yet",false)
*/

/*
		for(RemoteSystem s: dao.findByExample("available", true)) {
			dao.remove(s);
		}

		for(Software software: SoftwareDao.getUserApps("sterry1", true)) {
			SoftwareDao.delete(software);
		}

		for(Job job: JobDao.getJobs("sterry1")) {
			JobDao.delete(job);
		}



		GSqlData gSqlData = new GSqlData("CondorLauncher")
        gSqlData.cleanAllTablesByRecord()

		// load up a storage system
		String irodsString = FileUtils.readFileToString(new File("src/test/resources/systems/storage/data.iplantcollaborative.org.json"));
		JSONObject irodsJson = new JSONObject(irodsString);
		RemoteSystem irods = sysManager.parseSystem(irodsJson, "sterry1");
		irods.setAvailable(true);
		irods.setGlobalDefault(true);
		irods.setPubliclyAvailable(true);
		systemDao.persist(irods);

		// load up a compute system
		String condorString = FileUtils.readFileToString(new File("src/test/resources/systems/execution/condor.opensciencegrid.org.json"));
		JSONObject condorJson = new JSONObject(condorString);
		RemoteSystem condor = sysManager.parseSystem(condorJson, "sterry1");
		condor.setAvailable(true);
		condor.setGlobalDefault(true);
		condor.setPubliclyAvailable(true);
		systemDao.persist(condor);

		String wcString = FileUtils.readFileToString(new File("src/test/resources/software/wc-iplant-condor.tacc.utexas.edu.json"));
		JSONObject wcJson = new JSONObject(wcString);
		Software software = Software.fromJSON(wcJson);
		software.setOwner("sterry1");
		SoftwareDao.persist(software);

		Job job = new Job();
		job.setName("SteveTest");
		job.setOwner("sterry1");
		job.setSystem(software.getSystem().getSystemId());
		job.setSoftwareName(software.getUniqueName());
		job.setProcessorCount(1);
		job.setMemoryRequest(1);
		job.setArchiveOutput(true);
		job.setArchivePath("/sterry1/jobs/condor");
		job.setStatus(JobStatusType.PENDING);
		job.setUpdateToken("7d7e5472e5159d726d905b4c06009c2f");
        JSONObject jsonobj = new JSONObject();
        jsonobj.put("query1","sterry1/applications/wc-1.00/read1.fq");
		job.setInputs(jsonobj.toString());
        jsonobj = new JSONObject();
        jsonobj.put("printLongestLine","1");
		job.setParameters(jsonobj.toString());
		job.setErrorMessage("Failed to submit job 68 Failed to put job in queue:");
		job.setRequestedTime("02:00:00");

		JobDao.persist(job);
*/
