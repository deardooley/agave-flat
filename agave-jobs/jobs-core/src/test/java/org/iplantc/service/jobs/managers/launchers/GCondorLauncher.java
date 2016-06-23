package org.iplantc.service.jobs.managers.launchers;

import java.io.File;

import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.systems.dao.SystemDao;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created with IntelliJ IDEA.
 * User: steve
 * Date: 1/14/13
 * Time: 3:23 PM
 * To change this template use File | Settings | File Templates.
 */
class GCondorLauncher {

    // this is the root directory for creating job specific

    File root = new File("target/test/tmp/scratch/iplant");
    CondorLauncher launcher;
    Job job;
    SystemDao dao;

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

    @BeforeClass
    void setup(){
        System.out.println("in setup");
		//CommonHibernateTest.initdb();
        dao = new SystemDao();
//        SystemManager sysManager = new SystemManager();
//        SystemDao systemDao = new SystemDao();

        //JobStoreSoftExecSystemSetup jobrecord = new JobStoreSoftExecSystemSetup();
        //jobrecord.gSqlData.cleanAllTablesByRecord();

        //job = jobrecord.insertFullJobTestRecordObjectGraph();
    }
	
    @Test
    void testLaunch()
	{   

        try
        {
            launcher = new CondorLauncher(job);
            launcher.launch();
        } 
		catch (Exception e) {
            e.printStackTrace();
        }
		finally 
		{
//			Software software = null;
//			SystemManager sysManager = new SystemManager();

			
/*
			try {
				software = SoftwareDao.getSoftwareByUniqueName(job.getSoftwareName());
				SoftwareDao.delete(software);
			} catch (Exception e) {}
			
			try { 
				RemoteSystem defaultStorageSystem = sysManager.getDefaultStorageSystem();
				dao.remove(defaultStorageSystem);
			} catch (Exception e) {}
			
			try {
				RemoteSystem defaultExecutionSystem = sysManager.getDefaultExecutionSystem();
				dao.remove(defaultExecutionSystem);
			} catch (Exception e) {}
			
			try { JobDao.delete(job); } catch (Exception e) {}
*/
		}

        JobStatusType actualStatus = job.getStatus();
        JobStatusType  expectedStatus = JobStatusType.RUNNING;

        boolean result = (actualStatus == expectedStatus ) ? true : false;
        //sleep(10000);
        Assert.assertTrue(result,"The status is RUNNING");
        // expectedLocalJobId should not be NULL but can be any integer value

    }

    public static void main(String[] args){
        System.out.println("this works ...");
        GCondorLauncher gcl = new GCondorLauncher();
        gcl.setup();
        gcl.testLaunch();


    }

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
