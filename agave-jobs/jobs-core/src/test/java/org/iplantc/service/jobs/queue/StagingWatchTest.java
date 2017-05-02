package org.iplantc.service.jobs.queue;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwareInput;
import org.iplantc.service.apps.model.SoftwareParameter;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.jobs.Settings;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.managers.JobManager;
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
import org.iplantc.service.systems.model.BatchQueue;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.enumerations.LoginProtocolType;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.transfer.RemoteDataClient;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Test(groups={"broken", "integration"})
public class StagingWatchTest extends AbstractJobSubmissionTest 
{
    private static final Logger log = Logger.getLogger(StagingWatch.class);
    
	protected static String LOCAL_TXT_FILE = "target/test-classes/transfer/test_upload.bin";
	
	private JSONTestDataUtil jtd;
	private SystemDao systemDao = new SystemDao();
	private SystemManager systemManager = new SystemManager();
	
	@BeforeClass
	public void beforeClass() throws Exception
	{
		clearInternalUsers();
		clearSoftware();
		clearSystems();
		clearJobs();
		
		jtd = JSONTestDataUtil.getInstance();
		
		JSONObject json = null;
		
		//File storageDir = new File(STORAGE_SYSTEM_TEMPLATE_DIR);
		for(String protocol: new String[]{"gridftp","irods","sftp", "s3"}) {
//		for(String protocol: new String[]{"sftp","gridftp", "irods", "s3", "ftp"}) {
			String testSystemDescriptionFilePath = STORAGE_SYSTEM_TEMPLATE_DIR + File.separator + protocol + ".example.com.json";
			StorageSystem storageSystem = null;
			try 
			{
				json = jtd.getTestDataObject(testSystemDescriptionFilePath);
				storageSystem = (StorageSystem) systemManager.parseSystem(json, SYSTEM_OWNER, null);
				storageSystem.setOwner(SYSTEM_OWNER);
				if (protocol.equals("gridftp")) {
					storageSystem.setGlobalDefault(true);
					storageSystem.setPubliclyAvailable(true);
				}
				systemDao.persist(storageSystem);
			} 
			catch (Exception e) {
				Assert.fail("Unable to create " + protocol + 
						" storage system from description in file " + 
						testSystemDescriptionFilePath, e);
			}
			
			RemoteDataClient client = null;
			try 
			{
				// copy the test file to each remote system
				client = storageSystem.getRemoteDataClient();
				client.authenticate();
				client.put(LOCAL_TXT_FILE, "");
			}
			catch (Exception e) {
				Assert.fail("Unable to authenticated to " + 
						storageSystem.getSystemId() + " and upload test data", e);
			}
			finally {
			    try { client.disconnect(); } catch (Exception e) {}
			}
		}
		
		
		
		//for(File jsonFile: executionDir.listFiles()) {
		for(String protocol: new String[]{"ssh","gsissh"}){//,"condor"}) {
			json = jtd.getTestDataObject(EXECUTION_SYSTEM_TEMPLATE_DIR + File.separator + protocol + ".example.com.json");
			//json = jtd.getTestDataObject(jsonFile.getPath());
			ExecutionSystem system = (ExecutionSystem) systemManager.parseSystem(json, SYSTEM_OWNER, null);
			system.setOwner(SYSTEM_OWNER);
			system.getBatchQueues().clear();
			BatchQueue q = unlimitedQueue.clone();
			q.setSystemDefault(true);
			system.addBatchQueue(q);
			
			systemDao.persist(system);
			
			// register an app on the system
			json = jtd.getTestDataObject(SOFTWARE_SYSTEM_TEMPLATE_DIR + "/system-software.json");
			json.put("name", "test-" + protocol);
			json.put("executionSystem", system.getSystemId());
			json.put("deploymentSystem", systemManager.getDefaultStorageSystem().getSystemId());
			Software software = Software.fromJSON(json, SYSTEM_OWNER);
			software.setDefaultQueue(q.getName());
	        software.setOwner(SYSTEM_OWNER);
			SoftwareDao.persist(software);
		}
		
//		File softwareDir = new File(SOFTWARE_SYSTEM_TEMPLATE_DIR);
//		for(File jsonFile: softwareDir.listFiles()) {
//			json = jtd.getTestDataObject(jsonFile.getPath());
//			Software software = Software.fromJSON(json, SYSTEM_OWNER);
//			software.setOwner(SYSTEM_OWNER);
//			SoftwareDao.persist(software);
//		}
	}
	
	public void clearSystems()
	{
		for (RemoteSystem system: systemDao.getAll()) {
			systemDao.remove(system);
		}
	}

	public void clearSoftware()
	{
		for (Software software: SoftwareDao.getAll()) {
			SoftwareDao.delete(software);
		}
	}

	public void clearJobs() throws JobException
	{
		for (Job job: JobDao.getAll()) {
			JobDao.delete(job);
		}
	}
	
	private void clearInternalUsers() throws ProfileException
	{
		InternalUserDao internalUserDao = new InternalUserDao();
		
		for (InternalUser internalUser: internalUserDao.getAll()) {
			internalUserDao.delete(internalUser);
		}
	}

	@AfterClass
	public void afterClass() throws Exception
	{
//		clearInternalUsers();
//		clearJobs();
		clearSoftware();
		clearSystems();
	}
	
	public Job createJob(Software software, String inputUri) 
	throws JobException, JSONException 
	{
		return createJob(software, new String[] {inputUri});
	}
	
	public Job createJob(Software software, String[] inputUris) 
    throws JobException, JSONException 
    {
	    return createJob(JobStatusType.PENDING, software, inputUris);
    }
	
	public Job createJob(JobStatusType status, Software software, String[] inputUris) 
	throws JobException, JSONException 
	{
		ObjectMapper mapper = new ObjectMapper();
		
		Job job = new Job();
		job.setName( software.getExecutionSystem().getName() + " test");
		job.setOwner(software.getOwner());
		job.setArchiveOutput(false);
		
		job.setArchivePath(software.getOwner() + "/archive/jobs/job-" + job.getUuid());
        job.setArchiveSystem(systemDao.getGlobalDefaultSystemForTenant(RemoteSystemType.STORAGE, TenancyHelper.getCurrentTenantId()));
        job.setCreated(new Date());
        job.setMemoryPerNode((double)4);
        job.setOwner(software.getOwner());
        job.setProcessorsPerNode((long)1);
        job.setMaxRunTime("00:10::00");
        job.setSoftwareName(software.getUniqueName());
        job.setSystem(software.getExecutionSystem().getSystemId());
		job.setStatus(status, job.getErrorMessage());
		job.setBatchQueue(software.getDefaultQueue());
		
		String remoteWorkPath = null;
		if (StringUtils.isEmpty(software.getExecutionSystem().getScratchDir())) {
			remoteWorkPath = job.getOwner() +
				"/job-" + job.getUuid() + "-" + Slug.toSlug(job.getName()) + 
				"/" + FilenameUtils.getName(software.getDeploymentPath());
		} else {
			remoteWorkPath = software.getExecutionSystem().getScratchDir() + 
					job.getOwner() + 
					"/job-" + job.getUuid() + "-" + Slug.toSlug(job.getName()) +
					"/" + FilenameUtils.getName(software.getDeploymentPath());
		}
		job.setWorkPath(remoteWorkPath);
		
		ObjectNode jsonInputs = mapper.createObjectNode();
		for (SoftwareInput input: software.getInputs()) {
			ArrayNode inputValues = mapper.createArrayNode();
			for(String uri: inputUris) {
				inputValues.add(uri);
			}
			jsonInputs.put(input.getKey(), inputValues);
		}
		job.setInputsAsJsonObject(jsonInputs);
		
		ObjectNode jsonParameters = mapper.createObjectNode();
		for (SoftwareParameter param: software.getParameters()) {
			jsonParameters.put(param.getKey(), param.getDefaultValueAsJsonArray());
		}
		job.setParametersAsJsonObject(jsonParameters);
		
		return job;
	}
	
	@DataProvider(name="stageJobDataFromRegisteredSystemsProvider")
	public Object[][] stageJobDataFromRegisteredSystemsProvider() throws JobException, JSONException
	{
		String remoteFileName = FilenameUtils.getName(LOCAL_TXT_FILE);
		
		String irodsInternalUriInput = Settings.IPLANT_IO_SERVICE + "media/system/irods.example.com/" + remoteFileName;
		String irodsAgaveUriInput = "agave://irods.example.com/" + remoteFileName;
		String sftpInternalUriInput = Settings.IPLANT_IO_SERVICE + "media/system/sftp.example.com/" + remoteFileName;
		String sftpAgaveUriInput = "agave://sftp.example.com/" + remoteFileName;
		String gridftpInternalUriInput = Settings.IPLANT_IO_SERVICE + "media/system/gridftp.example.com/" + remoteFileName;
		String gridftpAgaveUriInput = "agave://gridftp.example.com/" + remoteFileName;
//		String ftpInternalUriInput = Settings.IPLANT_IO_SERVICE + "media/system/ftp.example.com/" + remoteFileName;
//        String ftpAgaveUriInput = "agave://ftp.example.com/" + remoteFileName;
        String s3InternalUriInput = Settings.IPLANT_IO_SERVICE + "media/system/s3.example.com/" + remoteFileName;
        String s3AgaveUriInput = "agave://s3.example.com/" + remoteFileName;
        
		List<Software> testApps = SoftwareDao.getAll();
		List<Object[]> testCases = new ArrayList<Object[]>();
		
		for (Software software: testApps) 
		{
		    testCases.add(new Object[] { createJob(software, s3InternalUriInput), "Failed to stage s3 internal uri input to job execution system " + software.getExecutionSystem().getSystemId() });
            testCases.add(new Object[] { createJob(software, s3AgaveUriInput), "Failed to stage s3 agave uri input to job execution system " + software.getExecutionSystem().getSystemId() });
//            testCases.add(new Object[] { createJob(software, ftpInternalUriInput), "Failed to stage ftp internal uri input to job execution system " + software.getExecutionSystem().getSystemId() });
//            testCases.add(new Object[] { createJob(software, ftpAgaveUriInput), "Failed to stage ftp agave uri input to job execution system " + software.getExecutionSystem().getSystemId() });
            
		    testCases.add(new Object[] { createJob(software, irodsInternalUriInput), "Failed to stage irods internal uri input to job execution system " + software.getExecutionSystem().getSystemId() });
			testCases.add(new Object[] { createJob(software, irodsAgaveUriInput), "Failed to stage irods agave uri input to job execution system " + software.getExecutionSystem().getSystemId() });
			testCases.add(new Object[] { createJob(software, sftpInternalUriInput), "Failed to stage sftp internal uri input to job execution system " + software.getExecutionSystem().getSystemId() });
			testCases.add(new Object[] { createJob(software, sftpAgaveUriInput), "Failed to stage sftp agave uri input to job execution system " + software.getExecutionSystem().getSystemId() });
			testCases.add(new Object[] { createJob(software, gridftpInternalUriInput), "Failed to stage gridftp internal uri input to job execution system " + software.getExecutionSystem().getSystemId() });
			testCases.add(new Object[] { createJob(software, gridftpAgaveUriInput), "Failed to stage gridftp agave uri input to job execution system " + software.getExecutionSystem().getSystemId() });
			testCases.add(new Object[] { createJob(software, new String[] {irodsInternalUriInput, sftpInternalUriInput, gridftpInternalUriInput}), "Failed to stage multiple internal uri input to job execution system " + software.getExecutionSystem().getSystemId() });
			testCases.add(new Object[] { createJob(software, new String[] {irodsAgaveUriInput, sftpAgaveUriInput, gridftpAgaveUriInput}), "Failed to stage multiple agave uri input to job execution system " + software.getExecutionSystem().getSystemId() });
			testCases.add(new Object[] { createJob(software, new String[] {sftpInternalUriInput, gridftpInternalUriInput}), "Failed to stage multiple internal uri input to job execution system " + software.getExecutionSystem().getSystemId() });
			testCases.add(new Object[] { createJob(software, new String[] {sftpAgaveUriInput, gridftpAgaveUriInput}), "Failed to stage multiple agave uri input to job execution system " + software.getExecutionSystem().getSystemId() });
		}
		
		return testCases.toArray(new Object[][] {});
	}
	
	@Test(dataProvider="stageJobDataFromRegisteredSystemsProvider", enabled=false)
	public void stageJobDataFromRegisteredSystems(Job job, String message)
	{
		RemoteDataClient remoteDataClient = null;
		try 
		{
			JobDao.persist(job);
			
			StagingWatch watch = new StagingWatch();
			Map<String, String[]> jobInputMap = JobManager.getJobInputMap(job);
			watch.setJob(job);
			watch.doExecute();
			
			job = JobDao.getById(job.getId());
			Assert.assertEquals(job.getStatus(), JobStatusType.STAGED, "Job status was not changed to staged after staging watch completed.");
			ExecutionSystem executionSystem = (ExecutionSystem)systemDao.findBySystemId(job.getSystem()); 
			remoteDataClient = executionSystem.getRemoteDataClient(job.getInternalUsername());
			remoteDataClient.authenticate();
			String relativeWorkPath = remoteDataClient.resolvePath(executionSystem.getScratchDir());
			relativeWorkPath = executionSystem.getScratchDir() + StringUtils.replaceOnce(job.getWorkPath(), relativeWorkPath, "/");
			for (String inputKey: jobInputMap.keySet()) {
				for (String inputUri: jobInputMap.get(inputKey)) {
					URI uri = URI.create(inputUri);
					Assert.assertTrue(remoteDataClient.doesExist(relativeWorkPath + File.separator + FilenameUtils.getName(uri.getPath())), message);
				}
			}
		} catch (Exception e) {
			Assert.fail(message, e);
		} finally {
			try { remoteDataClient.disconnect(); } catch (Exception e) {}
		}
	}
	
	public String createGenericUri(String systemId) throws Exception
	{
		String remoteFileName = FilenameUtils.getName(LOCAL_TXT_FILE);
		
		RemoteSystem system = systemDao.findBySystemId(systemId);
		RemoteDataClient client = system.getRemoteDataClient();
		String salt = system.getEncryptionKeyForAuthConfig(system.getStorageConfig().getDefaultAuthConfig());
		String scheme = system.getStorageConfig().getProtocol().name().toLowerCase();
		String username = StringUtils.isEmpty(system.getStorageConfig().getDefaultAuthConfig().getUsername()) ? 
		        system.getStorageConfig().getDefaultAuthConfig().getUsername() : 
		        null;
        String pass = StringUtils.isEmpty(system.getStorageConfig().getDefaultAuthConfig().getClearTextPassword(salt)) ? 
                system.getStorageConfig().getDefaultAuthConfig().getClearTextPassword(salt): 
                null;
		String sUri =  String.format("%s://%s%s:%d/%s", 
		        scheme,
		        username != null && pass != null ? (username + ":" + pass +"@") : "",
				system.getStorageConfig().getHost(),
				system.getStorageConfig().getPort(), 
				client.resolvePath(remoteFileName));
		return sUri;
	}
	
	@DataProvider(name="stageJobDataFromGenericUriProvider")
	public Object[][] stageJobDataFromGenericUriProvider() throws Exception
	{	
		String sftpUriInput = createGenericUri("sftp.example.com");
		String gridftpUriInput = createGenericUri("gridftp.example.com");
		String irodsUriInput = createGenericUri("irods.example.com");
		String s3UriInput = createGenericUri("s3.example.com");
//		String ftpUriInput = createGenericUri("ftp.example.com");
		String httpInput = "http://a2.espncdn.com/prod/assets/espn_top_nav_logo_109x27.png";
		String httpsInput = "https://www.google.com/images/srpr/logo4w.png";
		
		List<Software> testApps = SoftwareDao.getAll();
		List<Object[]> testCases = new ArrayList<Object[]>();
		
		for (Software software: testApps) 
		{
//		    testCases.add(new Object[] { createJob(software, ftpUriInput), false, "Failed to stage ftp generic uri input to job execution system " + software.getExecutionSystem().getSystemId() });
		    testCases.add(new Object[] { createJob(software, s3UriInput), true, "Generic s3 uri input should fail to stage. The protocol needs auth and is not supported as a generic URL. " });
		    testCases.add(new Object[] { createJob(software, irodsUriInput), true, "Generic irods uri input should fail to stage. The protocol needs auth and is not supported as a generic URL. " });			
			testCases.add(new Object[] { createJob(software, gridftpUriInput), true, "Generic gridftp uri input should fail to stage. The protocol needs auth and is not supported as a generic URL." });
			testCases.add(new Object[] { createJob(software, sftpUriInput), false, "Failed to stage sftp generic uri input to job execution system " + software.getExecutionSystem().getSystemId() });
			testCases.add(new Object[] { createJob(software, httpInput), false, "Failed to stage http generic uri input to job execution system " + software.getExecutionSystem().getSystemId() });
			testCases.add(new Object[] { createJob(software, httpsInput), false, "Failed to stage https generic uri input to job execution system " + software.getExecutionSystem().getSystemId() });
			testCases.add(new Object[] { createJob(software, new String[] {sftpUriInput, httpsInput, httpInput}), false, "Failed to stage https generic uri input to job execution system " + software.getExecutionSystem().getSystemId() });
		}
		
		return testCases.toArray(new Object[][] {});
	}
	
//	@Test(dataProvider="stageJobDataFromGenericUriProvider", dependsOnMethods={"stageJobDataFromRegisteredSystems"})
	@Test(dataProvider="stageJobDataFromGenericUriProvider", enabled=false)
	public void stageJobDataFromGenericUri(Job job, boolean shouldFail, String message)
	{
		RemoteDataClient remoteDataClient = null;
		try 
		{
			JobDao.persist(job);
			
			StagingWatch watch = new StagingWatch();
			Map<String, String[]> jobInputMap = JobManager.getJobInputMap(job);
			watch.setJob(job);
			watch.doExecute();
			
			if (!shouldFail)
			{
				job = JobDao.getById(job.getId());
				Assert.assertEquals(job.getStatus(), JobStatusType.STAGED, "Job status was not changed to staged after staging watch completed.");
				ExecutionSystem executionSystem = (ExecutionSystem)systemDao.findBySystemId(job.getSystem()); 
				remoteDataClient = executionSystem.getRemoteDataClient(job.getInternalUsername());
				remoteDataClient.authenticate();
				String relativeWorkPath = remoteDataClient.resolvePath(executionSystem.getScratchDir());
				relativeWorkPath = executionSystem.getScratchDir() + StringUtils.replaceOnce(job.getWorkPath(), relativeWorkPath, "/");
				for (String inputKey: jobInputMap.keySet()) {
					for (String inputUri: jobInputMap.get(inputKey)) {
						URI uri = URI.create(inputUri);
						Assert.assertTrue(remoteDataClient.doesExist(relativeWorkPath + File.separator + FilenameUtils.getName(uri.getPath())), message);
					}
				}
			} else {
				Assert.assertNotNull(job.getErrorMessage(), message);
			}
		} catch (Exception e) {
			Assert.assertNotNull(job.getErrorMessage(), message);
			Assert.assertTrue(shouldFail, message);
		} finally {
			try { remoteDataClient.disconnect(); } catch (Exception e) {}
		}
	}
	
	@DataProvider(name="concurrentQueueTerminationTestProvider")
    public Object[][] concurrentQueueTerminationTestProvider() throws JobException, JSONException
    {
        String remoteFileName = FilenameUtils.getName(LOCAL_TXT_FILE);
        
        String irodsInternalUriInput = Settings.IPLANT_IO_SERVICE + "media/system/irods.example.com/" + remoteFileName;
        String irodsAgaveUriInput = "agave://irods.example.com/" + remoteFileName;
        String sftpInternalUriInput = Settings.IPLANT_IO_SERVICE + "media/system/sftp.example.com/" + remoteFileName;
        String sftpAgaveUriInput = "agave://sftp.example.com/" + remoteFileName;
        String gridftpInternalUriInput = Settings.IPLANT_IO_SERVICE + "media/system/gridftp.example.com/" + remoteFileName;
        String gridftpAgaveUriInput = "agave://gridftp.example.com/" + remoteFileName;
//        String ftpInternalUriInput = Settings.IPLANT_IO_SERVICE + "media/system/ftp.example.com/" + remoteFileName;
//        String ftpAgaveUriInput = "agave://ftp.example.com/" + remoteFileName;
        String s3InternalUriInput = Settings.IPLANT_IO_SERVICE + "media/system/s3.example.com/" + remoteFileName;
        String s3AgaveUriInput = "agave://s3.example.com/" + remoteFileName;
        
        List<Software> testApps = SoftwareDao.getAll();
        List<Object[]> testCases = new ArrayList<Object[]>();
        
        for (Software software: testApps) 
        {
            if (software.getExecutionSystem().getLoginConfig().getProtocol() == LoginProtocolType.SSH 
                    && software.getDefaultQueue().equals(unlimitedQueue.getName())) {
//                testCases.add(new Object[] { software, new String[] {s3InternalUriInput}, "Failed to stage s3 internal uri input to job execution system " + software.getExecutionSystem().getSystemId() });
//                testCases.add(new Object[] { software, new String[] {s3AgaveUriInput}, "Failed to stage s3 agave uri input to job execution system " + software.getExecutionSystem().getSystemId() });
//                testCases.add(new Object[] { software, new String[] {ftpInternalUriInput}, "Failed to stage ftp internal uri input to job execution system " + software.getExecutionSystem().getSystemId() });
//                testCases.add(new Object[] { software, new String[] {ftpAgaveUriInput}, "Failed to stage ftp agave uri input to job execution system " + software.getExecutionSystem().getSystemId() });
                
//                testCases.add(new Object[] { software, new String[] {irodsInternalUriInput}, "Failed to stage irods internal uri input to job execution system " + software.getExecutionSystem().getSystemId() });
//                testCases.add(new Object[] { software, new String[] {irodsAgaveUriInput}, "Failed to stage irods agave uri input to job execution system " + software.getExecutionSystem().getSystemId() });
//                testCases.add(new Object[] { software, new String[] {sftpInternalUriInput}, "Failed to stage sftp internal uri input to job execution system " + software.getExecutionSystem().getSystemId() });
//                testCases.add(new Object[] { software, new String[] {sftpAgaveUriInput}, "Failed to stage sftp agave uri input to job execution system " + software.getExecutionSystem().getSystemId() });
                testCases.add(new Object[] { software, new String[] {gridftpInternalUriInput}, "Failed to stage gridftp internal uri input to job execution system " + software.getExecutionSystem().getSystemId() });
//                testCases.add(new Object[] { software, new String[] {gridftpAgaveUriInput}, "Failed to stage gridftp agave uri input to job execution system " + software.getExecutionSystem().getSystemId() });
                
//                testCases.add(new Object[] { software, new String[] {irodsInternalUriInput, sftpInternalUriInput, gridftpInternalUriInput}, "Failed to stage multiple internal uri input to job execution system " + software.getExecutionSystem().getSystemId() });
//                testCases.add(new Object[] { software, new String[] {irodsAgaveUriInput, sftpAgaveUriInput, gridftpAgaveUriInput}, "Failed to stage multiple agave uri input to job execution system " + software.getExecutionSystem().getSystemId() });
//                testCases.add(new Object[] { software, new String[] {sftpInternalUriInput, irodsAgaveUriInput, gridftpInternalUriInput}, "Failed to stage multiple internal uri input to job execution system " + software.getExecutionSystem().getSystemId() });
//                testCases.add(new Object[] { software, new String[] {s3AgaveUriInput, irodsAgaveUriInput, sftpAgaveUriInput, gridftpAgaveUriInput}, "Failed to stage multiple agave uri input to job execution system " + software.getExecutionSystem().getSystemId() });
                break;
            }
        }
        
        return testCases.toArray(new Object[][] {});
    }
	
	@Test (groups={"staging"}, dataProvider="concurrentQueueTerminationTestProvider", enabled=true)
    public void concurrentQueueTerminationTest(Software software, String[] inputs, String message) 
    throws Exception 
    {
	    clearJobs();
	    StdSchedulerFactory factory = new StdSchedulerFactory();
        factory.initialize(this.getClass().getClassLoader().getResourceAsStream("quartz-producer.properties"));
        Scheduler producerScheduler = factory.getScheduler();
        
        factory = new StdSchedulerFactory();
        factory.initialize(this.getClass().getClassLoader().getResourceAsStream("quartz-consumer.properties"));
        Scheduler sched = factory.getScheduler();
        
        		
        JobDetail jobDetail = newJob(StagingWatch.class)
                .withIdentity("primary", "Staging")
                .requestRecovery(true)
                .storeDurably()
                .build();
        
        producerScheduler.addJob(jobDetail, true);
        
        // start a block of worker processes to pull pre-staged file references
        // from the db and apply the appropriate transforms to them.
        for (int i = 0; i < 15; i++)
        {
            
            Trigger trigger = newTrigger()
                    .withIdentity("trigger"+i, "Staging")
                    .startAt(new DateTime().plusSeconds(i).toDate())
                    .withSchedule(simpleSchedule()
                            .withMisfireHandlingInstructionIgnoreMisfires()
                            .withIntervalInSeconds(2)
                            .repeatForever())
                    .forJob(jobDetail)
                    .withPriority(5)
                    .build();
            
            producerScheduler.scheduleJob(trigger);
        }
        
        final AtomicInteger jobsComplete = new AtomicInteger(0);
        sched.getListenerManager().addJobListener(
                new JobListener() {

                    @Override
                    public String getName() {
                        return "Unit Test Listener";
                    }

                    @Override
                    public void jobToBeExecuted(JobExecutionContext context) {
                        log.debug("working on a new job");                        
                    }

                    @Override
                    public void jobExecutionVetoed(JobExecutionContext context) {
                        // no idea here
                    }

                    @Override
                    public void jobWasExecuted(JobExecutionContext context, JobExecutionException e) {
                        if (e == null) {
                            log.error(jobsComplete.addAndGet(1) + "/100 Completed jobs ",e);;
                        } else {
//                            log.error("Transfer failed",e);
                        }
                    }
                    
                }, GroupMatcher.jobGroupContains("Staging")
            );
        
        try 
        {
        	sched.start();
        	producerScheduler.start();
        	
            for (int i=0;i<100;i++) {
                JobDao.persist(createJob(software, inputs));
            }
            
            log.debug("Sleeping to allow scheduler to run for a bit...");
            try { Thread.sleep(3000); } catch (Exception e) {}
            
            log.debug("Resuming test run and pausing all staging triggers...");
            sched.pauseAll();
            producerScheduler.pauseAll();
            log.debug("All triggers stopped. Interrupting executing jobs...");
            
            for (JobExecutionContext context: sched.getCurrentlyExecutingJobs()) {
                log.debug("Interrupting job " + context.getJobDetail().getKey() + "...");
                sched.interrupt(context.getJobDetail().getKey());
                log.debug("Interrupt of job " + context.getJobDetail().getKey() + " complete.");
            }
            log.debug("Shutting down consumer scheduler...");
            sched.shutdown(true);
            log.debug("...consumer schedule shut down.");
            
            log.debug("Shutting down producer scheduler...");
            producerScheduler.shutdown(true);
            log.debug("...producer schedule shut down.");
            
            
            for (Job job: JobDao.getAll())
            {
                Assert.assertTrue(job.getStatus() == JobStatusType.STAGED 
                        || job.getStatus() == JobStatusType.PENDING 
                        || job.getStatus() == JobStatusType.FAILED, 
                        "Job status was not rolled back upon interrupt.");
            }
        } 
        catch (Exception e) {
            Assert.fail("Failed to stage job data due to unexpected error", e);
        }
    }
	
	
	@Test (groups={"staging"}, dataProvider="concurrentQueueTerminationTestProvider", enabled=false)
    public void concurrentQueueThrouputTest(Software software, String[] inputs, String message) 
    throws Exception 
    {
        clearJobs();
        StdSchedulerFactory factory = new StdSchedulerFactory();
        factory.initialize(this.getClass().getClassLoader().getResourceAsStream("quartz-producer.properties"));
        Scheduler producerScheduler = factory.getScheduler();
        
        factory = new StdSchedulerFactory();
        factory.initialize(this.getClass().getClassLoader().getResourceAsStream("quartz-consumer.properties"));
        Scheduler sched = factory.getScheduler();
        
        
        // start a block of worker processes to pull pre-staged file references
        // from the db and apply the appropriate transforms to them.
        for (int i = 0; i < 15; i++)
        {
        	log.debug("Setting job and trigger " + i + " for Staging job group");
            
        	JobDetail jobDetail = newJob(StagingWatch.class)
                    .withIdentity("staging-job-" + i, "Staging")
                    .requestRecovery(true)
                    .storeDurably()
                    .build();
        	
            Trigger trigger = newTrigger()
                    .withIdentity("trigger"+i, "Staging")
                    .startAt(new DateTime().plusSeconds(i).toDate())
                    .withSchedule(simpleSchedule()
                            .withMisfireHandlingInstructionIgnoreMisfires()
                            .withIntervalInSeconds(2)
                            .repeatForever())
                    .forJob(jobDetail)
                    .withPriority(5)
                    .build();
            
            producerScheduler.scheduleJob(jobDetail, trigger);
        }
        
        final AtomicInteger jobsComplete = new AtomicInteger(0);
        sched.getListenerManager().addJobListener(
                new JobListener() {

                    @Override
                    public String getName() {
                        return "Unit Test Listener";
                    }

                    @Override
                    public void jobToBeExecuted(JobExecutionContext context) {
                        log.debug("working on a new job");                        
                    }

                    @Override
                    public void jobExecutionVetoed(JobExecutionContext context) {
                        // no idea here
                    }

                    @Override
                    public void jobWasExecuted(JobExecutionContext context, JobExecutionException e) {
                        if (e == null) {
                            log.error(jobsComplete.addAndGet(1) + "/100 Completed jobs ",e);;
                        } else {
//                            log.error("Transfer failed",e);
                        }
                    }
                    
                }, GroupMatcher.jobGroupContains("Staging")
            );
        
        try 
        {
            int totalJobs = 10;
            for (int i=0;i<totalJobs;i++) {
                for (JobStatusType postStageStatus: JobStatusType.getActiveStatuses()) {
                    Job decoyjob = createJob(postStageStatus, software, inputs);
                    decoyjob.setName("decoy-"+decoyjob.getName());
                    JobDao.persist(decoyjob);
                }
                
                Job job = createJob(software, inputs);
                JobDao.persist(job);
                
                // make sure all the job inputs are in place.
                if (i == 0) {
                    log.debug("Staging job input files to remote systems prior to starting scheduler...");
                    super.stageJobInputs(job);
                    log.debug("Completed staging inputs...");
                }
            }
            
            log.debug("Starting scheduler and letting it rip...");
            
            sched.start();
            producerScheduler.start();
            
            while (jobsComplete.get() < totalJobs-1);
            
            log.debug("Shutting down scheduler...");
            sched.shutdown(true);
            log.debug("Scheduler shut down...");
            
            int processedCount = 0;
            for (Job job: JobDao.getAll())
            {
                processedCount += (job.getStatus() == JobStatusType.STAGED || job.getStatus() == JobStatusType.FAILED) ? 1: 0;
            }
            
            Assert.assertEquals(processedCount, totalJobs, 
                    "Jobs were left unstaged.");
        } 
        catch (Exception e) {
            Assert.fail("Failed to stage job data due to unexpected error", e);
        }
        
    }

}