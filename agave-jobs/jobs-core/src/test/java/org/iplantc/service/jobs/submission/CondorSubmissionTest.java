package org.iplantc.service.jobs.submission;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwareInput;
import org.iplantc.service.apps.model.SoftwareParameter;
import org.iplantc.service.common.exceptions.PermissionException;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.model.JSONTestDataUtil;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.queue.AbstractJobWatch;
import org.iplantc.service.jobs.queue.ArchiveWatch;
import org.iplantc.service.jobs.queue.MonitoringWatch;
import org.iplantc.service.jobs.queue.StagingWatch;
import org.iplantc.service.jobs.queue.SubmissionWatch;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.iplantc.service.systems.exceptions.SystemException;
import org.iplantc.service.systems.manager.SystemManager;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.RemoteDataClientFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Tests end to end integration of a job submission by manually pushing
 * through each stage of each queue.
 */
public class CondorSubmissionTest extends AbstractJobSubmissionTest
{
	private SystemManager systemManager = new SystemManager();
	private Software software;
	
	
	@BeforeClass
	@Override
    public void beforeClass() throws Exception 
	{
		clearSystems();
		clearSoftware();
		clearJobs();
		
		jtd = JSONTestDataUtil.getInstance();
		
		initSystems();
		
		initSoftware();
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.submission.AbstractJobSubmissionTest#initSoftware()
	 */
	@Override
	protected void initSoftware() throws Exception {
		
	    File softwareDir = new File(SOFTWARE_SYSTEM_TEMPLATE_DIR, "/wc-condor.example.com.json");
		JSONObject json = jtd.getTestDataObject(softwareDir.getPath());
		software = Software.fromJSON(json, SYSTEM_OWNER);
		software.setOwner(SYSTEM_OWNER);
		SoftwareDao.persist(software);
		
		stageSofwareAssets(software);
		
		stageSoftwareInputDefaultData(software);
	}
	
	/* (non-Javadoc)
	 * @see org.iplantc.service.jobs.submission.AbstractJobSubmissionTest#initSystems()
	 */
	@Override
	protected void initSystems() throws Exception 
	{
        storageSystem = (StorageSystem) getNewInstanceOfRemoteSystem(RemoteSystemType.STORAGE, "storage");
        storageSystem.setOwner(SYSTEM_OWNER);
        storageSystem.setPubliclyAvailable(true);
        storageSystem.setGlobalDefault(true);
        storageSystem.getUsersUsingAsDefault().add(SYSTEM_OWNER);
        systemDao.persist(storageSystem);
        
        executionSystem = (ExecutionSystem) getNewInstanceOfRemoteSystem(RemoteSystemType.EXECUTION, "condor");
        executionSystem.setOwner(SYSTEM_OWNER);
        executionSystem.getBatchQueues().clear();
        executionSystem.addBatchQueue(dedicatedQueue.clone());
        executionSystem.addBatchQueue(longQueue.clone());
        executionSystem.addBatchQueue(mediumQueue.clone());
        executionSystem.addBatchQueue(shortQueue.clone());
        executionSystem.setPubliclyAvailable(true);
        executionSystem.setType(RemoteSystemType.EXECUTION);
        systemDao.persist(executionSystem);
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
	
	@Test (groups={"submission"}, dataProvider="submitJobProvider")
	public void submitJob(Software software, String message, boolean shouldThrowException) throws Exception
	{
		ObjectMapper mapper = new ObjectMapper();
		
		RemoteDataClient remoteDataClient = null;
		
		job = new Job();
		job.setName( software.getExecutionSystem().getName() + " test");
		job.setArchiveOutput(true);
		job.setArchivePath("archive/job-1-"+System.currentTimeMillis());
		job.setArchiveSystem(storageSystem);
		job.setCreated(new Date());
		job.setMemoryPerNode((double)4);
		job.setOwner(software.getOwner());
		job.setProcessorsPerNode((long)1);
		job.setMaxRunTime("1:00:00");
		job.setSoftwareName(software.getUniqueName());
		job.setStatus(JobStatusType.PENDING, "Job accepted and queued for submission.");
		job.setSystem(software.getExecutionSystem().getSystemId());
		
		//job.setWorkPath("iplant/job-1-open-science-grid-test/wc-1.00");
		
		ObjectNode jsonInputs = mapper.createObjectNode();
		for(SoftwareInput input: software.getInputs()) {
			jsonInputs.put(input.getKey(), input.getDefaultValueAsJsonArray());
		}
		job.setInputsAsJsonObject(jsonInputs);
		
		ObjectNode jsonParameters = mapper.createObjectNode();
		for (SoftwareParameter parameter: software.getParameters()) {
			jsonParameters.put(parameter.getKey(), parameter.getDefaultValueAsJsonArray());
		}
		job.setParametersAsJsonObject(jsonParameters);
		
		JobDao.persist(job);
		
		// set the localsystem id to the job system so it will run
		//Settings.LOCAL_SYSTEM_ID = job.getSystem();
		
		// move data to the system
		try 
		{
			StagingWatch stagingWatch = new StagingWatch();
			stagingWatch.execute(null);
			Assert.assertEquals(job.getStatus(), JobStatusType.STAGED, 
					"Job status was not STAGED after StagingWatch completed.");
			
			remoteDataClient = new SystemDao().findBySystemId(job.getSystem()).getRemoteDataClient(job.getInternalUsername());
			remoteDataClient.authenticate();
			Assert.assertTrue(remoteDataClient.doesExist(job.getWorkPath()), 
					"Work folder does not exist on remote system. Staging failed.");
		} 
		catch (Exception e) {
			Assert.fail("Failed to stage job input data to " + job.getSystem(), e);
		}
		finally {
			try { remoteDataClient.disconnect(); } catch (Exception e) {}
		}
		
		// submit the job after the data was staged
		try 
		{
			SubmissionWatch submissionWatch = new SubmissionWatch();
			submissionWatch.execute(null);
			Assert.assertEquals(job.getStatus(), JobStatusType.QUEUED, 
					"Job status was not QUEUED after SubmissionWatch completed.");
		} 
		catch (Exception e) {
			Assert.fail("Failed to submit job to " + job.getSystem(), e);
		}
		
		// monitor the job while it's running
		try 
		{
			MonitoringWatch condorWatch = new MonitoringWatch();
			condorWatch.execute(null);
			Assert.assertEquals(job.getStatus(), JobStatusType.CLEANING_UP, 
					"Job status was not CLEANING_UP after JobStatusWatch completed.");
		} 
		catch (Exception e) {
			Assert.fail("Failed to stage job input data to " + job.getSystem(), e);
		}
		
		// archive the job when it's done
		try 
		{
			AbstractJobWatch archiveWatch = new ArchiveWatch();
			archiveWatch.execute(null);
			Assert.assertEquals(job.getStatus(), JobStatusType.FINISHED, 
					"Job status was not FINISHED after ArchiveWatch completed.");
			
			remoteDataClient = job.getArchiveSystem().getRemoteDataClient(job.getInternalUsername());
			remoteDataClient.authenticate();
			Assert.assertTrue(remoteDataClient.doesExist(job.getArchivePath()), 
					"Archive folder does not exist on remote system.");
		} 
		catch (Exception e) {
			Assert.fail("Failed to archive job data to " + job.getArchiveSystem().getSystemId(), e);
		} 
		finally {
			try { remoteDataClient.disconnect(); } catch (Exception e) {}
		}
		
		
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
