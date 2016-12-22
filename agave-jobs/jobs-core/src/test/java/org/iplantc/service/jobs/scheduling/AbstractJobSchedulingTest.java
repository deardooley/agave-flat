package org.iplantc.service.jobs.scheduling;

import static org.iplantc.service.jobs.model.JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE;
import static org.iplantc.service.jobs.model.JSONTestDataUtil.TEST_OWNER;
import static org.iplantc.service.jobs.model.JSONTestDataUtil.TEST_SOFTWARE_SYSTEM_FILE;
import static org.iplantc.service.jobs.model.JSONTestDataUtil.TEST_STORAGE_SYSTEM_FILE;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.log4j.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwareInput;
import org.iplantc.service.apps.model.SoftwareParameter;
import org.iplantc.service.jobs.dao.AbstractDaoTest;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.dao.JobEventDao;
import org.iplantc.service.jobs.dao.TaskDistributor;
import org.iplantc.service.jobs.model.JSONTestDataUtil;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobEvent;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.BatchQueue;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.iplantc.service.transfer.dao.TransferTaskDao;
import org.iplantc.service.transfer.model.TransferTask;
import org.iplantc.service.transfer.model.enumerations.TransferStatusType;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AbstractJobSchedulingTest extends AbstractDaoTest 
{
    private static final Logger log = Logger.getLogger(AbstractJobSchedulingTest.class);

    public static final BatchQueue shortQueue = new BatchQueue("short", (long)1000, (long)10, (long)1, 16.0, (long)16, "01:00:00", null, true);
	public static final BatchQueue mediumQueue = new BatchQueue("medium", (long)100, (long)10, (long)1, 16.0, (long)16, "12:00:00", null, false);
	public static final BatchQueue longQueue = new BatchQueue("long", (long)10, (long)4, (long)1, 16.0, (long)16, "48:00:00", null, false);
	public static final BatchQueue dedicatedQueue = new BatchQueue("dedicated", (long)1, (long)1, (long)1, 16.0, (long)16, "144:00:00", null, false);
	public static final BatchQueue unlimitedQueue = new BatchQueue("dedicated", (long)-1, 2048.0);
	
	private StorageSystem publicStorageSystem = null;
	protected List<String> testUsernames = new ArrayList<String>();
	private List<Integer> testUserIds = new ArrayList<Integer>();
	protected static final int TEST_USER_COUNT = 10;

	public AbstractJobSchedulingTest() {
		super();
	}

	@Override
	@BeforeClass
	public void beforeClass() throws Exception {
    	systemDao = new SystemDao();
    	
    	jtd = JSONTestDataUtil.getInstance();
    	
    	for (int i=0;i<TEST_USER_COUNT;i++) {
    		testUsernames.add("user-" + i);
    		testUserIds.add(i);
    	}
    	
    	Collections.shuffle(testUsernames);
    	Collections.shuffle(testUserIds);
    	
    	initSystems();
        initSoftware();
        clearJobs();
        
    //		HibernateUtil.getConfiguration().getProperties().setProperty("hibernate.show_sql", "true");
    	
    }

	@AfterMethod
	public void afterMethod() throws Exception {
		clearJobs();
	}

	@AfterClass
	@Override
	public void afterClass() throws Exception {
		super.afterClass();
	}

	@Override
	protected void initSystems() throws Exception {
		clearSystems();
		
		JSONObject exeSystemJson = jtd.getTestDataObject(TEST_EXECUTION_SYSTEM_FILE);
		privateExecutionSystem = ExecutionSystem.fromJSON(exeSystemJson);
		privateExecutionSystem.setOwner(TEST_OWNER);
		privateExecutionSystem.setType(RemoteSystemType.EXECUTION);
		privateExecutionSystem.getBatchQueues().clear();
		privateExecutionSystem.addBatchQueue(unlimitedQueue.clone());
		privateExecutionSystem.addBatchQueue(mediumQueue.clone());
		privateExecutionSystem.addBatchQueue(longQueue.clone());
		systemDao.persist(privateExecutionSystem);
		
		for (int i =0; i< 1; i++) {
			ExecutionSystem exeSystem = ExecutionSystem.fromJSON(exeSystemJson);
			exeSystem.setSystemId(exeSystem.getSystemId() + "-" + i);
			exeSystem.setOwner(TEST_OWNER);
			exeSystem.getBatchQueues().clear();
			exeSystem.addBatchQueue(dedicatedQueue.clone());
			exeSystem.addBatchQueue(longQueue.clone());
			exeSystem.addBatchQueue(mediumQueue.clone());
			exeSystem.addBatchQueue(shortQueue.clone());
			exeSystem.setPubliclyAvailable(true);
			exeSystem.setType(RemoteSystemType.EXECUTION);
			log.debug("Inserting execution system " + exeSystem.getSystemId());
			systemDao.persist(exeSystem);
		}
		
		publicStorageSystem = StorageSystem.fromJSON(jtd.getTestDataObject(TEST_STORAGE_SYSTEM_FILE));
		publicStorageSystem.setOwner(TEST_OWNER);
		publicStorageSystem.setType(RemoteSystemType.STORAGE);
		publicStorageSystem.setGlobalDefault(true);
		publicStorageSystem.setPubliclyAvailable(true);
		log.debug("Inserting public storage system " + publicStorageSystem.getSystemId());
	    systemDao.persist(publicStorageSystem);
	}

	protected void initSoftware() throws Exception {
		clearSoftware(); 
		
		JSONObject json = jtd.getTestDataObject(TEST_SOFTWARE_SYSTEM_FILE);
		this.software = Software.fromJSON(json, TEST_OWNER);
		this.software.setPubliclyAvailable(true);
		this.software.setOwner(TEST_OWNER);
		this.software.setDefaultQueue(unlimitedQueue.getName());
		this.software.setDefaultMaxRunTime(null);
		this.software.setDefaultMemoryPerNode(null);
		this.software.setDefaultNodes(null);
		this.software.setDefaultProcessorsPerNode(null);
		
		int i = 0;
		for (RemoteSystem exeSystem: systemDao.getAllExecutionSystems()) 
		{
			if (exeSystem.getSystemId().equals(privateExecutionSystem.getSystemId())) continue;
			
			for(BatchQueue q: ((ExecutionSystem)exeSystem).getBatchQueues()) 
			{
				Software software = this.software.clone();
				software.setExecutionSystem((ExecutionSystem)exeSystem);
				software.setName("test-" + exeSystem.getSystemId() + "-" + q.getName() );
				software.setDefaultQueue(q.getName());
				software.setDefaultMaxRunTime(q.getMaxRequestedTime());
				software.setDefaultMemoryPerNode(q.getMaxMemoryPerNode());
				software.setDefaultNodes(q.getMaxNodes());
				software.setDefaultProcessorsPerNode(q.getMaxProcessorsPerNode());
				log.debug("Adding software " + software.getUniqueName());
				SoftwareDao.persist(software);
				i++;
			}
		}
	}

	public TreeMap<String,List<String>> createJobDistribution(JobStatusType status) 
	throws Exception {
		TreeMap<String,List<String>> jobDistribution = new TreeMap<String,List<String>>();
		for (String username: testUsernames) {
			jobDistribution.put(username, new ArrayList<String>());
		}
		
		for (RemoteSystem exeSystem: systemDao.getAllExecutionSystems()) 
		{
			for(BatchQueue q: ((ExecutionSystem)exeSystem).getBatchQueues()) 
			{
				int maxQueueJobs = q.getMaxJobs() > 0 ? new Long(q.getMaxJobs()).intValue() : 1000;
				int maxUserQueueJobs = Math.min(maxQueueJobs, (q.getMaxUserJobs() > 0 ? new Long(q.getMaxUserJobs()).intValue() : 1000));
				
				int activeCount = RandomUtils.nextInt(maxQueueJobs);
				
				//int activeUserCount = RandomUtils.nextInt(maxUserQueueJobs);
				
				int userCount = RandomUtils.nextInt(10);
				Collections.shuffle(testUsernames);
				List<Integer> userIds = testUserIds.subList(0, userCount);
	
				// generates a list of buckets with the task indices arranged in a list within each bucket
				List<List<Integer>> buckets = TaskDistributor.getDistribution(userIds.toArray(new Integer[] {}), activeCount);
				
				for (int i=0;i<userIds.size(); i++)
				{
					String username = "user-"+userIds.get(i);
					
					for(int j=0; j<buckets.get(i).size(); j++)
					{
						Job job = createJob(status, (ExecutionSystem)exeSystem, q, username);
					
						jobDistribution.get(username).add(job.getUuid());
					}
				}
			}
		}
		
		return jobDistribution;
	}

	public Job createJob(JobStatusType status, ExecutionSystem exeSystem, BatchQueue queue, String username)
	throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		
		Job job = new Job();
		job.setName("test-" + exeSystem.getName() + "_" + queue.getName());
		job.setOutputPath(exeSystem.getScratchDir() + "/" + username + "job-" + job.getUuid());
		job.setOwner(username);
		job.setInternalUsername(null);
		
		job.setArchiveOutput(true);
		job.setArchiveSystem(publicStorageSystem);
		job.setArchivePath(username + "/archive/test-job-999");
		
		job.setSoftwareName("test-" + exeSystem.getName() + "_" + queue.getName());
		job.setSystem(exeSystem.getSystemId());
		job.setBatchQueue(queue.getName());
		job.setMaxRunTime(StringUtils.isEmpty(software.getDefaultMaxRunTime()) ? "00:30:00" : software.getDefaultMaxRunTime());
		job.setMemoryPerNode((software.getDefaultMemoryPerNode() == null) ? (double)1 : software.getDefaultMemoryPerNode());
		job.setNodeCount((software.getDefaultNodes() == null) ? (long)1 : software.getDefaultNodes());
		job.setProcessorsPerNode((software.getDefaultProcessorsPerNode() == null) ? (long)1 : software.getDefaultProcessorsPerNode());
		
		ObjectNode inputs = mapper.createObjectNode();
		for(SoftwareInput swInput: this.software.getInputs()) {
			inputs.put(swInput.getKey(), swInput.getDefaultValueAsJsonArray());
		}
		job.setInputsAsJsonObject(inputs);
		
		ObjectNode parameters = mapper.createObjectNode();
		for(SoftwareParameter swParameter: software.getParameters()) {
			parameters.put(swParameter.getKey(), swParameter.getDefaultValueAsJsonArray());
		}
		job.setParametersAsJsonObject(parameters);
		int minutesAgoJobWasCreated = RandomUtils.nextInt(360)+1;
		DateTime created = new DateTime().minusMinutes(minutesAgoJobWasCreated+20);
		job.setCreated(created.toDate());
		
		if (JobStatusType.isExecuting(status) || status == JobStatusType.CLEANING_UP) {
			job.setLocalJobId("q." + System.currentTimeMillis());
			job.setSchedulerJobId(job.getUuid());
			job.initStatus(status, status.getDescription());
			
			int minutesAgoJobWasSubmitted = RandomUtils.nextInt(minutesAgoJobWasCreated)+1;
			int minutesAgoJobWasStarted = minutesAgoJobWasSubmitted + RandomUtils.nextInt(minutesAgoJobWasSubmitted);
			job.setSubmitTime(created.plusMinutes(minutesAgoJobWasSubmitted).toDate());
			job.setStartTime(created.plusMinutes(minutesAgoJobWasStarted).toDate());
			
			if (status == JobStatusType.CLEANING_UP) {
			    int minutesAgoJobWasEnded =  minutesAgoJobWasStarted + RandomUtils.nextInt(minutesAgoJobWasStarted);
			    job.setEndTime(created.plusMinutes(minutesAgoJobWasEnded).toDate());
			}
		} else if (JobStatusType.isFinished(status) || JobStatusType.isArchived(status)) {
			job.setLocalJobId("q." + System.currentTimeMillis());
			job.setSchedulerJobId(job.getUuid());
			job.initStatus(status, status.getDescription());
			
			int minutesAgoJobWasSubmitted = RandomUtils.nextInt(minutesAgoJobWasCreated)+1;
			int minutesAgoJobWasStarted = minutesAgoJobWasSubmitted + RandomUtils.nextInt(minutesAgoJobWasSubmitted);
			int minutesAgoJobWasEnded = minutesAgoJobWasStarted + RandomUtils.nextInt(minutesAgoJobWasStarted);
			job.setSubmitTime(created.plusMinutes(minutesAgoJobWasSubmitted).toDate());
			job.setStartTime(created.plusMinutes(minutesAgoJobWasStarted).toDate());
			job.setEndTime(created.plusMinutes(minutesAgoJobWasEnded).toDate());
			
		} else if (status == JobStatusType.STAGING_INPUTS) {
			
			int minutesAgoJobStartedStaging = RandomUtils.nextInt(minutesAgoJobWasCreated)+1;
			DateTime stagingTime = created.plusMinutes(minutesAgoJobStartedStaging);
			
			for(SoftwareInput input: this.software.getInputs()) 
			{
				for (Iterator<JsonNode> iter = input.getDefaultValueAsJsonArray().iterator(); iter.hasNext();)
				{
					String val = iter.next().asText();
					
					TransferTask stagingTransferTask = new TransferTask(
							val, 
							"agave://" + job.getSystem() + "/" + job.getWorkPath() + "/" + FilenameUtils.getName(URI.create(val).getPath()), 
							job.getOwner(), 
							null, 
							null);
					stagingTransferTask.setStatus(TransferStatusType.TRANSFERRING);
					stagingTransferTask.setCreated(stagingTime.toDate());
					stagingTransferTask.setLastUpdated(stagingTime.toDate());
					
					TransferTaskDao.persist(stagingTransferTask);
					
					JobEvent event = new JobEvent(
							JobStatusType.STAGING_INPUTS, 
							"Copy in progress", 
							stagingTransferTask, 
							job.getOwner());
					event.setCreated(stagingTime.toDate());
					event.setJob(job);
					JobEventDao.persist(event);
					
					job.initStatus(JobStatusType.STAGING_INPUTS, event.getDescription());
				}
			}
			
			job.setLastUpdated(stagingTime.toDate());
			
		} else if (status == JobStatusType.ARCHIVING) {
			
			DateTime stagingTime = created.plusMinutes(5);
			DateTime stagingEnded = stagingTime.plusMinutes(1);
			DateTime startTime = stagingEnded.plusMinutes(1);
			DateTime endTime = startTime.plusMinutes(1);
			DateTime archiveTime = endTime.plusMinutes(1);
			
			for(SoftwareInput input: this.software.getInputs()) 
			{
				for (Iterator<JsonNode> iter = input.getDefaultValueAsJsonArray().iterator(); iter.hasNext();)
				{
					String val = iter.next().asText();
					
					TransferTask stagingTransferTask = new TransferTask(
							val, 
							"agave://" + job.getSystem() + "/" + job.getWorkPath() + "/" + FilenameUtils.getName(URI.create(val).getPath()), 
							job.getOwner(), 
							null, 
							null);
					
					stagingTransferTask.setStatus(TransferStatusType.COMPLETED);
					stagingTransferTask.setCreated(stagingTime.toDate());
					stagingTransferTask.setStartTime(stagingTime.toDate());
					stagingTransferTask.setEndTime(stagingEnded.toDate());
					stagingTransferTask.setLastUpdated(stagingEnded.toDate());
					
					TransferTaskDao.persist(stagingTransferTask);
					
					JobEvent event = new JobEvent(
							JobStatusType.STAGING_INPUTS, 
							"Staging completed", 
							stagingTransferTask, 
							job.getOwner());
					event.setCreated(stagingTime.toDate());
                    event.setJob(job);
                    JobEventDao.persist(event);
					
					job.initStatus(JobStatusType.STAGING_INPUTS, event.getDescription());
				}
			}
			
			job.initStatus(JobStatusType.STAGED, JobStatusType.STAGED.getDescription());
			job.setLocalJobId("q." + System.currentTimeMillis());
			job.setSchedulerJobId(job.getUuid());
			job.initStatus(status, status.getDescription());
			
			job.setSubmitTime(stagingEnded.toDate());
			job.setStartTime(startTime.toDate());
			job.setEndTime(endTime.toDate());
			
			TransferTask archivingTransferTask = new TransferTask(
					"agave://" + job.getSystem() + "/" + job.getWorkPath(),
					job.getArchiveCanonicalUrl(), 
					job.getOwner(), 
					null, 
					null);
			
			archivingTransferTask.setCreated(archiveTime.toDate());
			archivingTransferTask.setStartTime(archiveTime.toDate());
			TransferTaskDao.persist(archivingTransferTask);
			
			JobEvent event = new JobEvent(
					JobStatusType.ARCHIVING ,
					JobStatusType.ARCHIVING.getDescription(), 
					archivingTransferTask, 
					job.getOwner());
			event.setCreated(archiveTime.toDate());
            event.setJob(job);
            JobEventDao.persist(event);
            
			job.initStatus(status, event.getDescription());
			
			job.setLastUpdated(archiveTime.toDate());
		}
		else {
		    job.initStatus(status, status.getDescription());
		}
		
		log.debug("Adding job " + job.getId() + " - " + job.getUuid());
		JobDao.create(job, false);
		
		return job;
	}

	/**
	 * Creates a distribution of jobs uniformly spread across all users.
	 * 
	 * @param totalJobs number of jobs to create
	 * @return Map of usernames to a list of UUID for the jobs created for that user
	 * @throws Exception 
	 */
	protected TreeMap<String,List<String>> loadRandomDistributionOfJobsForSingleApp(int totalJobs, JobStatusType status) 
	throws Exception {
		TreeMap<String,List<String>> jobDistribution = new TreeMap<String,List<String>>();
		Integer[] weights = new Integer[TEST_USER_COUNT];
		
		for (int i=0;i<TEST_USER_COUNT;i++) {
			jobDistribution.put(testUsernames.get(i), new ArrayList<String>());
			weights[i] = RandomUtils.nextInt(TEST_USER_COUNT-1)+1;
		}
		
		// generates a list of buckets with the task indices arranged in a list within each bucket
		List<List<Integer>> buckets = TaskDistributor.getDistribution(weights, totalJobs);
		
		for (int i=0;i<TEST_USER_COUNT; i++)
		{
			String username = "user-"+i;
			
			for(int j=0; j<buckets.get(i).size(); j++)
			{
				Job job = createJob(status, 
									software.getExecutionSystem(), 
									software.getExecutionSystem().getQueue(software.getDefaultQueue()), 
									username);
			
				jobDistribution.get(username).add(job.getUuid());
			}
		}
		
		return jobDistribution;
	}
	
	/**
     * Creates a distribution of jobs uniformly spread across all users.
     * 
     * @param totalJobs number of jobs to create
     * @return Map of usernames to a list of UUID for the jobs created for that user
     * @throws Exception 
     */
    protected TreeMap<String,List<String>> loadRandomDistributionOfJobsAcrossMultipleSystems(int totalJobs, JobStatusType status) 
    throws Exception {
        TreeMap<String,List<String>> jobDistribution = new TreeMap<String,List<String>>();
        Integer[] weights = new Integer[TEST_USER_COUNT];
        
        for (int i=0;i<TEST_USER_COUNT;i++) {
            jobDistribution.put(testUsernames.get(i), new ArrayList<String>());
            weights[i] = RandomUtils.nextInt(TEST_USER_COUNT-1)+1;
        }
        
        // generates a list of buckets with the task indices arranged in a list within each bucket
        List<List<Integer>> buckets = TaskDistributor.getDistribution(weights, totalJobs);
        
        for(Software software: SoftwareDao.getAll()) 
        {
            for (int i=0;i<TEST_USER_COUNT; i++)
            {
                String username = "user-"+i;
                
                for(int j=0; j<buckets.get(i).size(); j++)
                {   
                    Job job = createJob(status, 
                                    software.getExecutionSystem(), 
                                    software.getExecutionSystem().getQueue(software.getDefaultQueue()), 
                                    username);
            
                    jobDistribution.get(username).add(job.getUuid());
                }
            }
        }
        
        return jobDistribution;
    }

	/**
	 * Creates a distribution of jobs uniformly spread across all users.
	 * 
	 * @param totalJobs number of jobs to create
	 * @return Map of usernames to a list of UUID for the jobs created for that user
	 * @throws Exception 
	 */
	protected TreeMap<String,List<String>> loadUniformDistributionOfJobsForSingleApp(int totalJobs, JobStatusType status) 
	throws Exception 
	{
		TreeMap<String, List<String>> jobDistribution = new TreeMap<String, List<String>>();
		for (String username : testUsernames) {
			jobDistribution.put(username, new ArrayList<String>());
		}

		// generates a list of buckets with the task indices arranged in a list
		// within each bucket
		for (int i = 0; i < TEST_USER_COUNT; i++) {
			String username = "user-" + i;

			for (int j = 0; j < totalJobs / TEST_USER_COUNT; j++) {
				Job job = createJob(
						status,
						software.getExecutionSystem(),
						software.getExecutionSystem().getQueue(
								software.getDefaultQueue()), username);

				jobDistribution.get(username).add(job.getUuid());
			}
		}

		return jobDistribution;
	}

}