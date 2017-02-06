package org.iplantc.service.jobs.dao;

import static org.iplantc.service.jobs.model.JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE;
import static org.iplantc.service.jobs.model.JSONTestDataUtil.TEST_OWNER;
import static org.iplantc.service.jobs.model.JSONTestDataUtil.TEST_SOFTWARE_SYSTEM_FILE;
import static org.iplantc.service.jobs.model.JSONTestDataUtil.TEST_STORAGE_SYSTEM_FILE;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.PAUSED;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.QUEUED;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.RUNNING;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.log4j.Logger;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.jobs.dao.utils.DedicatedConfig;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.JobUpdateParameters;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.jobs.phases.schedulers.dto.JobMonitorInfo;
import org.iplantc.service.systems.model.BatchQueue;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.Collections2;

@Test(groups={"broken"})
public class ProgressiveMonitoringBackoffTest extends AbstractDaoTest 
{
	public static final Logger log = Logger.getLogger(ProgressiveMonitoringBackoffTest.class); 

	String[] tenantIds = new String[] {"alpha", "beta", "gamma"};
    String[] usernames = new String[] {"user-0", "user-1", "user-2"};
    String[] systemsIds = new String[] {"execute1.example.com", "execute2.example.com", "execute3.example.com"};
    String[] queues = new String[] {"short", "medium", "long"};
    
    @BeforeSuite
    public void beforeSuite()
    {
        // Initialize the singleton instance of DedicatedConfig to use
        // a test provider whose state we can change.  By creating the 
        // singleton here, we control the configuration parameters that 
        // JobDao methods use in queries.  See DedicatedConfig for details.
        SimpleDedicatedProvider provider = new SimpleDedicatedProvider();
        DedicatedConfig.getInstance(provider);
    }
    
	@BeforeClass
	@Override
	public void beforeClass() throws Exception
	{
		super.beforeClass();
	}
	
	@AfterClass
	@Override
	public void afterClass() throws Exception
	{
		super.afterClass();
	}
	
	@BeforeMethod
	public void beforeMethod() throws Exception {
		initSystems();
        initSoftware();
        SoftwareDao.persist(software);
		clearJobs();
		
	}
	
	@AfterMethod
	public void afterMethod() throws Exception {
		clearJobs();
		clearSoftware();
		clearSystems();
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
        
        for (String tenantId: tenantIds) {
            for(String systemId: systemsIds) {
//                for (int i =0; i< 3; i++) {
                    ExecutionSystem exeSystem = ExecutionSystem.fromJSON(exeSystemJson);
                    exeSystem.setOwner(TEST_OWNER);
                    exeSystem.getBatchQueues().clear();
                    exeSystem.addBatchQueue(dedicatedQueue.clone());
                    exeSystem.addBatchQueue(longQueue.clone());
                    exeSystem.addBatchQueue(mediumQueue.clone());
                    exeSystem.addBatchQueue(shortQueue.clone());
                    exeSystem.setPubliclyAvailable(true);
                    exeSystem.setType(RemoteSystemType.EXECUTION);
                    exeSystem.setTenantId(tenantId);
                    exeSystem.setSystemId(tenantId + "-" + systemId);
                    log.debug("Inserting execution system " + exeSystem.getSystemId());
                    systemDao.persist(exeSystem);
//                }
            }
        }
        
        privateStorageSystem = StorageSystem.fromJSON(jtd.getTestDataObject(TEST_STORAGE_SYSTEM_FILE));
        privateStorageSystem.setOwner(TEST_OWNER);
        privateStorageSystem.setType(RemoteSystemType.STORAGE);
        privateStorageSystem.setGlobalDefault(true);
        privateStorageSystem.setPubliclyAvailable(true);
        log.debug("Inserting public storage system " + privateStorageSystem.getSystemId());
        systemDao.persist(privateStorageSystem);
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
            }
        }
    }
    
    @Test()
	public void getNextExecutingJobUuidReturnsEmptyWhenNotExecutingJobs() 
	{
        // Initialize the reusable variables.  During monitoring,
        // we only check the executing states.
        LinkedList<JobStatusType> statusList = new LinkedList<>();
        statusList.add(JobStatusType.QUEUED);
        statusList.add(JobStatusType.RUNNING);
        statusList.add(JobStatusType.PAUSED);
        JobPhaseType phase = JobPhaseType.MONITORING; 
        List<JobMonitorInfo> monitorInfoList = null;
        
        // Get the provider for dedicated configuration information created during set up.
        SimpleDedicatedProvider dedicatedProvider = 
            (SimpleDedicatedProvider) DedicatedConfig.getInstance().getDedicatedProvider();
        
		try 
		{	
			DateTime dummyLastUpdated = new DateTime();
			dummyLastUpdated = dummyLastUpdated.minusDays(2);
			for(JobStatusType status: JobStatusType.values())
			{
				if (!JobStatusType.isExecuting(status))
				{
					Job notExecutingJob = createJob(status, false); // don't save yet
					notExecutingJob.setCreated(dummyLastUpdated.toDate());
					notExecutingJob.setLastUpdated(notExecutingJob.getCreated());
					notExecutingJob.setStatusChecks(0);
					
					JobDao.create(notExecutingJob, false);
					Assert.assertNotNull(notExecutingJob.getId(), "Failed to persist test not running job");
				}
			}
			
	        // --- Set dedicated configuration information and then run query.
	        dedicatedProvider.tenantId  = null;
	        dedicatedProvider.userNames = null;
	        dedicatedProvider.systemIds = null;
	        monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
	        Assert.assertTrue(monitorInfoList.isEmpty(),
	                "No job should be returned when calling next running job and there are no running jobs.");
		} 
		catch (Exception e) {
			Assert.fail("Unexpected error occurred running test for next running job with status QUEUED", e);
		}
		finally {
//			try { clearJobs(); } catch (Exception e) {}
		}
	}
	
	@DataProvider(name="getNextExecutingJobUuidOnlyReturnsJobsWithExecutingStatusesProvider")
	public Object[][] getNextExecutingJobUuidOnlyReturnsJobsWithExecutingStatusesProvider() {
		
		List<Object[]> testCases = new ArrayList<Object[]>();
		
		for(JobStatusType status: JobStatusType.values())
		{
			if (JobStatusType.isExecuting(status)){
				testCases.add(new Object[]{ status, "Executing job with status " + status + 
					" was not returned when all other jobs were not running and check interval was valid."});
			}
		}
		
		return testCases.toArray(new Object[][]{});
	}
	
	@Test(dataProvider="getNextExecutingJobUuidOnlyReturnsJobsWithExecutingStatusesProvider", dependsOnMethods={"getNextExecutingJobUuidReturnsEmptyWhenNotExecutingJobs"})
	public void getNextExecutingJobUuidOnlyReturnsJobsWithExecutingStatuses(JobStatusType testStatus, String message) 
	{
        // Initialize the reusable variables.  During monitoring,
        // we only check the executing states.
        LinkedList<JobStatusType> statusList = new LinkedList<>();
        statusList.add(JobStatusType.QUEUED);
        statusList.add(JobStatusType.RUNNING);
        statusList.add(JobStatusType.PAUSED);
        JobPhaseType phase = JobPhaseType.MONITORING; 
        List<JobMonitorInfo> monitorInfoList = null;
        
        // Get the provider for dedicated configuration information created during set up.
        SimpleDedicatedProvider dedicatedProvider = 
            (SimpleDedicatedProvider) DedicatedConfig.getInstance().getDedicatedProvider();
        
		try 
		{
			DateTime dummyLastUpdated = new DateTime();
			dummyLastUpdated = dummyLastUpdated.minusDays(2);
			
			for(JobStatusType status: JobStatusType.values())
			{
				if (!JobStatusType.isExecuting(status))
				{
					Job notExecutingJob = createJob(status, false); // don't save yet
					notExecutingJob.setCreated(dummyLastUpdated.toDate());
					notExecutingJob.setLastUpdated(dummyLastUpdated.toDate());
					notExecutingJob.setStatusChecks(0);
					
					JobDao.create(notExecutingJob, false);
					Assert.assertNotNull(notExecutingJob.getId(), "Failed to persist test not running job");
				}
			}
			
			Job executingJob = createJob(testStatus, false); // don't save yet
			executingJob.setCreated(dummyLastUpdated.toDate());
			executingJob.setLastUpdated(dummyLastUpdated.plusDays(1).toDate());
			executingJob.setStatusChecks(0);
			JobDao.create(executingJob, false);
			
            // --- Set dedicated configuration information and then run query.
            dedicatedProvider.tenantId  = null;
            dedicatedProvider.userNames = null;
            dedicatedProvider.systemIds = null;
            monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
            Assert.assertFalse(monitorInfoList.isEmpty(), message);
			Assert.assertEquals(monitorInfoList.get(0).getUuid(), executingJob.getUuid(), 
			                    "Executing job was not returned on call to getSchedulerJobMonitorInfo");
			
		} catch (Exception e) {
			Assert.fail("Unexpected error occurred running test for next running job with status QUEUED", e);
		}
		finally {
//			try { clearJobs(); } catch (Exception e) {}
		}
	}
	
	@DataProvider(name="getNextExecutingJobUuidProvider")
	public Object[][] getNextExecutingJobUuidProvider() {
		DateTime jobLastUpdatedTime = new DateTime();
		
		List<Object[]> testCases = new ArrayList<Object[]>();
		
		for(int i=0; i<50; i++)
		{
			int expectedInterval = getExpectedIntervalForCheckCount(i);
			
			if (i > 0) {
				testCases.add(new Object[]{jobLastUpdatedTime, i, false, "Job just updated should not be returned."});
			}
			testCases.add(new Object[]{jobLastUpdatedTime.minusSeconds(expectedInterval), i, true, "Job last updated the full expected time ago should be returned."});
			testCases.add(new Object[]{jobLastUpdatedTime.minusSeconds(expectedInterval*2), i, true, "Job last updated longer ago than the next expected time should be returned."});
			
			// increment for next set of boundary tests
			jobLastUpdatedTime = jobLastUpdatedTime.minusSeconds(expectedInterval);
		}
		
		return testCases.toArray(new Object[][]{});
	}
	
	@Test(dataProvider="getNextExecutingJobUuidProvider", dependsOnMethods={"getNextExecutingJobUuidOnlyReturnsJobsWithExecutingStatuses"})
	public void getNextExecutingJobUuid(DateTime lastUpdated, int numberOfChecks, boolean shouldReturnValue, String message) 
	{
        // Initialize the reusable variables.  During monitoring,
        // we only check the executing states.
        LinkedList<JobStatusType> statusList = new LinkedList<>();
        statusList.add(JobStatusType.QUEUED);
        statusList.add(JobStatusType.RUNNING);
        statusList.add(JobStatusType.PAUSED);
        JobPhaseType phase = JobPhaseType.MONITORING; 
        List<JobMonitorInfo> monitorInfoList = null;
        
        // Get the provider for dedicated configuration information created during set up.
        SimpleDedicatedProvider dedicatedProvider = 
            (SimpleDedicatedProvider) DedicatedConfig.getInstance().getDedicatedProvider();
        
		// create job with status RUNNING
		Job executingJob;
		try 
		{	
			executingJob = createJob(JobStatusType.QUEUED, false); // don't save yets
			executingJob.setCreated(lastUpdated.minusSeconds(30).toDate());
			executingJob.setLastUpdated(lastUpdated.toDate());
			executingJob.setStatusChecks(numberOfChecks);
			JobDao.create(executingJob, false);
			Assert.assertNotNull(executingJob.getId(), "Failed to persist test job");
			
            // --- Set dedicated configuration information and then run query.
            dedicatedProvider.tenantId  = null;
            dedicatedProvider.userNames = null;
            dedicatedProvider.systemIds = null;
            monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
            Assert.assertFalse(monitorInfoList.isEmpty(), message);
		} 
		catch (Exception e) {
			Assert.fail("Unexpected error occurred running test for next running job with status QUEUED", e);
		}
		finally {
//			try { clearJobs(); } catch (Exception e) {}
		}
	}
	
	@DataProvider(name="getNextExecutingJobUuidRespectsTenancyProvider")
    public Object[][] getNextExecutingJobUuidRespectsTenancyProvider() {
        DateTime jobLastUpdatedTime = new DateTime();
        
        List<Object[]> testCases = new ArrayList<Object[]>();
        
        for(int i=0; i<2; i++)
        {
            int expectedInterval = getExpectedIntervalForCheckCount(i);
            
            if (i > 0) {
                testCases.add(new Object[]{jobLastUpdatedTime, i, false, "Job just updated should not be returned."});
            }
            testCases.add(new Object[]{jobLastUpdatedTime.minusSeconds(expectedInterval), i, true, "Job last updated the full expected time ago should be returned."});
            testCases.add(new Object[]{jobLastUpdatedTime.minusSeconds(expectedInterval*2), i, true, "Job last updated longer ago than the next expected time should be returned."});
            
            // increment for next set of boundary tests
            jobLastUpdatedTime = jobLastUpdatedTime.minusSeconds(expectedInterval);
        }
        
        return testCases.toArray(new Object[][]{});
    }
	
	@Test(dataProvider="getNextExecutingJobUuidRespectsTenancyProvider", dependsOnMethods={"getNextExecutingJobUuid"})
    public void getNextExecutingJobUuidRespectsTenancyCheck(DateTime lastUpdated, int numberOfChecks, boolean shouldReturnValue, String message) 
    {
        // Initialize the reusable variables.  During monitoring,
        // we only check the executing states.
        LinkedList<JobStatusType> statusList = new LinkedList<>();
        statusList.add(JobStatusType.QUEUED);
        statusList.add(JobStatusType.RUNNING);
        statusList.add(JobStatusType.PAUSED);
        JobPhaseType phase = JobPhaseType.MONITORING; 
        List<JobMonitorInfo> monitorInfoList = null;
        
        // Get the provider for dedicated configuration information created during set up.
        SimpleDedicatedProvider dedicatedProvider = 
            (SimpleDedicatedProvider) DedicatedConfig.getInstance().getDedicatedProvider();
        
        // create job with status RUNNING
        Job executingJob;
        try 
        {   
            for (JobStatusType testStatus: new JobStatusType[]{QUEUED, RUNNING, PAUSED})
            {
                executingJob = createJob(testStatus, false); // don't save yet
                executingJob.setCreated(lastUpdated.minusSeconds(30).toDate());
                executingJob.setLastUpdated(lastUpdated.toDate());
                executingJob.setStatusChecks(numberOfChecks);
                JobDao.create(executingJob, false);
            
                Assert.assertNotNull(executingJob.getId(), "Failed to persist test job");
                
                // --- Set dedicated configuration information and then run query.
                dedicatedProvider.tenantId  = TenancyHelper.getCurrentTenantId();
                dedicatedProvider.userNames = null;
                dedicatedProvider.systemIds = null;
                monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
                Assert.assertFalse(monitorInfoList.isEmpty(), message);
                Assert.assertTrue(monitorInfoList.size() > 1, "Multiple executing job should return ");
                
                dedicatedProvider.tenantId = "foobar";
                monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
                Assert.assertTrue(monitorInfoList.isEmpty(), "Next executing job for a tenant without any jobs should return null");
            }
        } 
        catch (Exception e) {
            Assert.fail("Unexpected error occurred running test for next running job with status QUEUED", e);
        }
        finally {
            //try { clearJobs(); } catch (Exception e) {}
        }
    }
    
    @Test(dependsOnMethods={"getNextExecutingJobUuidRespectsTenancyCheck"})
    public void getNextExecutingJobUuidSelectsRandomly() 
    {
        // Initialize the reusable variables.  During monitoring,
        // we only check the executing states.
        LinkedList<JobStatusType> statusList = new LinkedList<>();
        statusList.add(JobStatusType.QUEUED);
        statusList.add(JobStatusType.RUNNING);
        statusList.add(JobStatusType.PAUSED);
        JobPhaseType phase = JobPhaseType.MONITORING; 
        List<JobMonitorInfo> monitorInfoList = null;
        
        // Get the provider for dedicated configuration information created during set up.
        SimpleDedicatedProvider dedicatedProvider = 
            (SimpleDedicatedProvider) DedicatedConfig.getInstance().getDedicatedProvider();
        
        Hashtable<String, Hashtable<String, Integer>> tenantJobSelections = new Hashtable<String, Hashtable<String, Integer>>();
        Hashtable<String, Hashtable<String, Integer>> userJobSelections = new Hashtable<String, Hashtable<String, Integer>>();
        Hashtable<String, Hashtable<String, Integer>> systemJobSelections = new Hashtable<String, Hashtable<String, Integer>>();
        Hashtable<String, Hashtable<String, Integer>> systemQueueJobSelection = new Hashtable<String, Hashtable<String, Integer>>();
        Hashtable<String, Long> uuidSelections = new Hashtable<String, Long>();
        
        try 
        {   
            
            // initialize several jobs from each status
            Date created = new DateTime().minusMinutes(30).toDate();
            Date lastUpdated = new DateTime().minusSeconds(30).toDate();
            
            Job job = createJob(QUEUED);
            
            for (String tenantId: tenantIds) {
                Hashtable<String, Integer> tenantJobHits = new Hashtable<String,Integer>();
                
                for (String username: usernames) {
                    Hashtable<String, Integer> userJobHits = new Hashtable<String,Integer>();
                    for (String systemId: systemsIds) {
                        Hashtable<String, Integer> systemJobHits = new Hashtable<String,Integer>();
                        for (String queueName: queues) {
                            Hashtable<String, Integer> queueJobHits = new Hashtable<String,Integer>();
                            
                            for (JobStatusType status: new JobStatusType[] {QUEUED, RUNNING,PAUSED}) {
                                Job testJob = job.copy();
                                testJob.setCreated(created);
                                testJob.setLastUpdated(lastUpdated);
                                testJob.initStatus(status, status.getDescription());
                                testJob.setTenantId(tenantId);
                                testJob.setOwner(tenantId + "@" + username);
                                testJob.setSystem(tenantId + "-" + systemId);
                                testJob.setLocalJobId(testJob.getUuid());
                                testJob.setBatchQueue(queueName);
                                JobDao.create(testJob, false);
                            
                                queueJobHits.put(testJob.getUuid(), 0);
                                tenantJobHits.put(testJob.getUuid(), 0);
                                systemJobHits.put(testJob.getUuid(), 0);
                                userJobHits.put(testJob.getUuid(), 0);
                                uuidSelections.put(testJob.getUuid(), (long)0);
                            }
                            
                            systemQueueJobSelection.put(tenantId + "-" + systemId+"#"+queueName, queueJobHits);
                        }
                        
                        systemJobSelections.put(tenantId + "-" + systemId, systemJobHits);
                    }
                    
                    userJobSelections.put(tenantId + "@" + username, userJobHits);
                }
                
                tenantJobSelections.put(tenantId, tenantJobHits);
            }
            
            // --- Set dedicated configuration information and then run query.
            dedicatedProvider.tenantId  = null;
            dedicatedProvider.userNames = null;
            dedicatedProvider.systemIds = null;
            
            int TEST_COUNT = 1000;
            for (int i=0;i<TEST_COUNT; i++)
            {
                // Note that the default result ordering is to randomize by tenant and user and 
                // then order by job create date.  So we would expect only the oldest jobs for
                // any tenant/user combination ever to be at the head of the list.
                monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
                Assert.assertFalse(monitorInfoList.isEmpty(), "Vanilla getSchedulerJobMonitorInfo should never return null when valid jobs exist.");
                String nextJobUuid = monitorInfoList.get(0).getUuid();
                uuidSelections.put(nextJobUuid, uuidSelections.get(nextJobUuid)+1);
            }
            
            double[] doubles = new double[uuidSelections.size()];
            double mean = 0;
            int i = 0;
            for(Long assignedJobs: uuidSelections.values()) {
                doubles[i] = assignedJobs;
                mean += doubles[i];
                i++;
            }
            mean = mean / uuidSelections.size();
            
            StandardDeviation stdev = new StandardDeviation();
            double sd = stdev.evaluate(doubles, mean);
            System.out.println("{standardDeviation: " + sd + ", mean: " + mean + "}");
            Assert.assertTrue(mean > ((TEST_COUNT / uuidSelections.size() ) - 0.5) 
                    && mean < ((TEST_COUNT / uuidSelections.size() ) + 0.5), 
                    "Average should be roughly equaly to " + TEST_COUNT + " / " + uuidSelections.size());
        }
        catch (Exception e) {
            Assert.fail("Unexpected error occurred running test for next running job with status QUEUED", e);
        }
        finally {
            //try { clearJobs(); } catch (Exception e) {}
        }
    }
    
    @Test(dependsOnMethods={"getNextExecutingJobUuidSelectsRandomly"})
    public void getNextExecutingJobUuidSelectsFailsOnMismatchedDependencies() 
    {
        // Initialize the reusable variables.  During monitoring,
        // we only check the executing states.
        LinkedList<JobStatusType> statusList = new LinkedList<>();
        statusList.add(JobStatusType.QUEUED);
        statusList.add(JobStatusType.RUNNING);
        statusList.add(JobStatusType.PAUSED);
        JobPhaseType phase = JobPhaseType.MONITORING; 
        List<JobMonitorInfo> monitorInfoList = null;
        
        // Get the provider for dedicated configuration information created during set up.
        SimpleDedicatedProvider dedicatedProvider = 
            (SimpleDedicatedProvider) DedicatedConfig.getInstance().getDedicatedProvider();
        
        Hashtable<String, Hashtable<String, Integer>> tenantJobSelections = new Hashtable<String, Hashtable<String, Integer>>();
        Hashtable<String, Hashtable<String, Integer>> userJobSelections = new Hashtable<String, Hashtable<String, Integer>>();
        Hashtable<String, Hashtable<String, Integer>> systemJobSelections = new Hashtable<String, Hashtable<String, Integer>>();
        Hashtable<String, Hashtable<String, Integer>> systemQueueJobSelection = new Hashtable<String, Hashtable<String, Integer>>();
        Hashtable<String, Long> uuidSelections = new Hashtable<String, Long>();
        
        try 
        {   
            // initialize several jobs from each status
            Date created = new DateTime().minusMinutes(30).toDate();
            Date lastUpdated = new DateTime().minusSeconds(30).toDate();
            
            Job job = createJob(QUEUED);
            
            for (String tenantId: tenantIds) {
                Hashtable<String, Integer> tenantJobHits = new Hashtable<String,Integer>();
                
                for (String username: usernames) {
                    Hashtable<String, Integer> userJobHits = new Hashtable<String,Integer>();
                    for (String systemId: systemsIds) {
                        Hashtable<String, Integer> systemJobHits = new Hashtable<String,Integer>();
                        for (String queueName: queues) {
                            Hashtable<String, Integer> queueJobHits = new Hashtable<String,Integer>();
                            
                            for (JobStatusType status: new JobStatusType[] {QUEUED, RUNNING,PAUSED}) {
                                Job testJob = job.copy();
                                testJob.setCreated(created);
                                testJob.setLastUpdated(lastUpdated);
                                testJob.initStatus(status, status.getDescription());
                                testJob.setTenantId(tenantId);
                                testJob.setOwner(tenantId + "@" + username);
                                testJob.setSystem(tenantId + "-" + systemId);
                                testJob.setLocalJobId(testJob.getUuid());
                                testJob.setBatchQueue(queueName);
                                JobDao.create(testJob, false);
                            
                                queueJobHits.put(testJob.getUuid(), 0);
                                tenantJobHits.put(testJob.getUuid(), 0);
                                systemJobHits.put(testJob.getUuid(), 0);
                                userJobHits.put(testJob.getUuid(), 0);
                                uuidSelections.put(testJob.getUuid(), (long)0);
                            }
                            
                            for (JobStatusType status: JobStatusType.values()) {
                                if (status != QUEUED && status != RUNNING && status != PAUSED) 
                                {
                                    Job decoyJob = job.copy();
                                    decoyJob.setCreated(created);
                                    decoyJob.setLastUpdated(lastUpdated);
                                    decoyJob.initStatus(status, status.getDescription());
                                    decoyJob.setTenantId(tenantId);
                                    decoyJob.setOwner(tenantId + "@" + username);
                                    decoyJob.setSystem(tenantId + "-" + systemId);
                                    decoyJob.setLocalJobId(decoyJob.getUuid());
                                    decoyJob.setBatchQueue(queueName);
                                    JobDao.create(decoyJob, false);
                                }
                            }
                            
                            systemQueueJobSelection.put(tenantId + "-" + systemId+"#"+queueName, queueJobHits);
                        }
                        
                        systemJobSelections.put(tenantId + "-" + systemId, systemJobHits);
                    }
                    
                    userJobSelections.put(tenantId + "@" + username, userJobHits);
                }
                
                tenantJobSelections.put(tenantId, tenantJobHits);
            }
            
            // --- Set dedicated configuration information and then run query.
            dedicatedProvider.tenantId  = "someotherclient";
            dedicatedProvider.userNames = null;
            dedicatedProvider.systemIds = null;
            monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
            Assert.assertTrue(monitorInfoList.isEmpty(), "getSchedulerJobMonitorInfo with mismatched tenant id and empty usernames(s) & systemIds should always return null.");
            
            // --- Set dedicated configuration information and then run query.
            dedicatedProvider.tenantId  = "someotherclient";
            dedicatedProvider.userNames = new String[] {tenantIds[0] + "@" + usernames[0]};
            dedicatedProvider.systemIds = null;
            monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
            Assert.assertTrue(monitorInfoList.isEmpty(), "getSchedulerJobMonitorInfo with mismatched tenant id and valid username(s) should always return null.");
            
            // --- Set dedicated configuration information and then run query.
            dedicatedProvider.tenantId  = "someotherclient";
            dedicatedProvider.userNames = new String[] {tenantIds[0] + "@" + usernames[0]};
            dedicatedProvider.systemIds = new String[] {tenantIds[0] + "-" + systemsIds[0]};
            monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
            Assert.assertTrue(monitorInfoList.isEmpty(), "getSchedulerJobMonitorInfo with mismatched tenant id and valid username and systemIds should always return null.");
            
            // --- Set dedicated configuration information and then run query.
            dedicatedProvider.tenantId  = "someotherclient";
            dedicatedProvider.userNames = new String[] {tenantIds[0] + "@" + usernames[0]};
            dedicatedProvider.systemIds = new String[] {tenantIds[0] + "-" + systemsIds[0] + "#" + queues[0]};
            monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
            Assert.assertTrue(monitorInfoList.isEmpty(), "getSchedulerJobMonitorInfo with mismatched tenant id and valid username, systemIds, and queues should always return null.");
            
            // --- Set dedicated configuration information and then run query.
            dedicatedProvider.tenantId  = null;
            dedicatedProvider.userNames = new String[] {"bullwinkle"};
            dedicatedProvider.systemIds = null;
            monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
            Assert.assertTrue(monitorInfoList.isEmpty(), "getSchedulerJobMonitorInfo with mismatched username and empty tenantId and systemIds should always return null.");
            
            // --- Set dedicated configuration information and then run query.
            dedicatedProvider.tenantId  = null;
            dedicatedProvider.userNames = new String[] {"bullwinkle"};
            dedicatedProvider.systemIds = new String[] {tenantIds[0] + "-" + systemsIds[0] + "#" + queues[0]};
            monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
            Assert.assertTrue(monitorInfoList.isEmpty(), "getSchedulerJobMonitorInfo with mismatched tenant id and valid systemIds, and queues should always return null.");
            
            // --- Set dedicated configuration information and then run query.
            dedicatedProvider.tenantId  = null;
            dedicatedProvider.userNames = new String[] {"bullwinkle"};
            dedicatedProvider.systemIds = new String[] {tenantIds[0] + "-" + systemsIds[0]};
            monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
            Assert.assertTrue(monitorInfoList.isEmpty(), "getSchedulerJobMonitorInfo with mismatched tenant id and valid systemIds should always return null.");
            
            // --- Set dedicated configuration information and then run query.
            dedicatedProvider.tenantId  = tenantIds[0];
            dedicatedProvider.userNames = new String[] {"bullwinkle"};
            dedicatedProvider.systemIds = null;
            monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
            Assert.assertTrue(monitorInfoList.isEmpty(), "getSchedulerJobMonitorInfo with invalid user and valid tenant, system, and queue list should always return null.");
            
            // --- Set dedicated configuration information and then run query.
            dedicatedProvider.tenantId  = tenantIds[0];
            dedicatedProvider.userNames = new String[] {"bullwinkle"};
            dedicatedProvider.systemIds = new String[] {tenantIds[0] + "-" + systemsIds[0] + "#" + queues[0]};
            monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
            Assert.assertTrue(monitorInfoList.isEmpty(), "getSchedulerJobMonitorInfo with invalid user and valid tenant, system, and queue list should always return null.");
            
            // --- Set dedicated configuration information and then run query.
            dedicatedProvider.tenantId  = tenantIds[0];
            dedicatedProvider.userNames = new String[] {"bullwinkle"};
            dedicatedProvider.systemIds = new String[] {tenantIds[0] + "-" + systemsIds[0]};
            monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
            Assert.assertTrue(monitorInfoList.isEmpty(), "getSchedulerJobMonitorInfo with invalid user and valid system list should always return null.");
            
        }
        catch (Exception e) {
            Assert.fail("Unexpected error occurred running test for next running job with status QUEUED", e);
        }
        finally {
            //try { clearJobs(); } catch (Exception e) {}
        }
    }
	
	@Test(dependsOnMethods={"getNextExecutingJobUuidSelectsRandomly"})
    public void getNextExecutingJobUuidSelectsHonorsDedicatedParameters() 
    {
        // Initialize the reusable variables.  During monitoring,
        // we only check the executing states.
        LinkedList<JobStatusType> statusList = new LinkedList<>();
        statusList.add(JobStatusType.QUEUED);
        statusList.add(JobStatusType.RUNNING);
        statusList.add(JobStatusType.PAUSED);
        JobPhaseType phase = JobPhaseType.MONITORING; 
        List<JobMonitorInfo> monitorInfoList = null;
        
        // Get the provider for dedicated configuration information created during set up.
        SimpleDedicatedProvider dedicatedProvider = 
            (SimpleDedicatedProvider) DedicatedConfig.getInstance().getDedicatedProvider();
        
        Hashtable<String, Hashtable<String, Integer>> tenantJobSelections = new Hashtable<String, Hashtable<String, Integer>>();
        Hashtable<String, Hashtable<String, Integer>> userJobSelections = new Hashtable<String, Hashtable<String, Integer>>();
        Hashtable<String, Hashtable<String, Integer>> systemJobSelections = new Hashtable<String, Hashtable<String, Integer>>();
        Hashtable<String, Hashtable<String, Integer>> systemQueueJobSelection = new Hashtable<String, Hashtable<String, Integer>>();
        Hashtable<String, Long> uuidSelections = new Hashtable<String, Long>();
        
        try 
        {   
            
            // initialize several jobs from each status
            Date created = new DateTime().minusMinutes(30).toDate();
            Date lastUpdated = new DateTime().minusSeconds(30).toDate();
            
            Job job = createJob(QUEUED);
            
            for (String tenantId: tenantIds) {
                Hashtable<String, Integer> tenantJobHits = new Hashtable<String,Integer>();
                
                for (String username: usernames) {
                    Hashtable<String, Integer> userJobHits = new Hashtable<String,Integer>();
                    for (String systemId: systemsIds) {
                        Hashtable<String, Integer> systemJobHits = new Hashtable<String,Integer>();
                        for (String queueName: queues) {
                            Hashtable<String, Integer> queueJobHits = new Hashtable<String,Integer>();
                            
                            for (JobStatusType status: new JobStatusType[] {QUEUED, RUNNING,PAUSED}) {
                                Job testJob = job.copy();
                                testJob.setCreated(created);
                                testJob.setLastUpdated(lastUpdated);
                                testJob.initStatus(status, status.getDescription());
                                testJob.setTenantId(tenantId);
                                testJob.setOwner(tenantId + "@" + username);
                                testJob.setSystem(tenantId + "-" + systemId);
                                testJob.setLocalJobId(testJob.getUuid());
                                testJob.setBatchQueue(queueName);
                                JobDao.create(testJob, false);
                            
                                queueJobHits.put(testJob.getUuid(), 0);
                                tenantJobHits.put(testJob.getUuid(), 0);
                                systemJobHits.put(testJob.getUuid(), 0);
                                userJobHits.put(testJob.getUuid(), 0);
                                uuidSelections.put(testJob.getUuid(), (long)0);
                            }
                            
                            for (JobStatusType status: JobStatusType.values()) {
                                if (status != QUEUED && status != RUNNING && status != PAUSED) 
                                {
                                    Job decoyJob = job.copy();
                                    decoyJob.setCreated(created);
                                    decoyJob.setLastUpdated(lastUpdated);
                                    decoyJob.initStatus(status, status.getDescription());
                                    decoyJob.setTenantId(tenantId);
                                    decoyJob.setOwner(tenantId + "@" + username);
                                    decoyJob.setSystem(tenantId + "-" + systemId);
                                    decoyJob.setLocalJobId(decoyJob.getUuid());
                                    decoyJob.setBatchQueue(queueName);
                                    JobDao.create(decoyJob, false);
                                }
                            }
                            
                            systemQueueJobSelection.put(tenantId + "-" + systemId+"#"+queueName, queueJobHits);
                        }
                        
                        systemJobSelections.put(tenantId + "-" + systemId, systemJobHits);
                    }
                    
                    userJobSelections.put(tenantId + "@" + username, userJobHits);
                }
                
                tenantJobSelections.put(tenantId, tenantJobHits);
            }
                        
            Job nextJob = null;
            for (final String tenantId: tenantIds) {
                
                // --- Set dedicated configuration information and then run query.
                dedicatedProvider.tenantId  = tenantId;
                dedicatedProvider.userNames = null;
                dedicatedProvider.systemIds = null;
                monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
                Assert.assertFalse(monitorInfoList.isEmpty(), "getSchedulerJobMonitorInfo with specified tenant should never return null when valid jobs exist.");
                nextJob = JobDao.getByUuid(monitorInfoList.get(0).getUuid());
                Assert.assertNotNull(nextJob, "getSchedulerJobMonitorInfo with specified tenant should never return null when valid jobs exist.");
                Assert.assertEquals(nextJob.getTenantId(), tenantId, "getSchedulerJobMonitorInfo with specified tenant should never return a job from another tenant.");
                Assert.assertTrue(nextJob.getStatus() == QUEUED || nextJob.getStatus() == RUNNING || nextJob.getStatus() == PAUSED, "Job with invalid status was returned");
                
                @SuppressWarnings("unchecked")
                Collection<List<String>> userPermutations = Collections2.permutations(
                        CollectionUtils.collect(Arrays.asList(usernames), new Transformer() {
                            public String transform(Object val) {
                                return tenantId + "@" + val;
                            }
                        }));
                for (final List<String> userPermutation: userPermutations) {
                    
                    // --- Set dedicated configuration information and then run query.
                    dedicatedProvider.tenantId  = tenantId;
                    dedicatedProvider.userNames = userPermutation.toArray(new String[]{});
                    dedicatedProvider.systemIds = null;
                    monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
                    Assert.assertFalse(monitorInfoList.isEmpty(), "getSchedulerJobMonitorInfo with specified tenant should never return null when valid jobs exist.");
                    nextJob = JobDao.getByUuid(monitorInfoList.get(0).getUuid());
                    Assert.assertNotNull(nextJob, "getSchedulerJobMonitorInfo with specified tenant and user list should never return null when valid jobs exist.");
                    Assert.assertEquals(nextJob.getTenantId(), tenantId, "getSchedulerJobMonitorInfo with specified tenant should never return a job from another tenant.");
                    Assert.assertTrue(userPermutation.contains(nextJob.getOwner()), "getSchedulerJobMonitorInfo with specified tenant and user list should never return a job from another user.");
                    Assert.assertTrue(nextJob.getStatus() == QUEUED || nextJob.getStatus() == RUNNING || nextJob.getStatus() == PAUSED, "Job with invalid status was returned");
                    
                    // --- Set dedicated configuration information and then run query.
                    dedicatedProvider.tenantId  = null;
                    dedicatedProvider.userNames = userPermutation.toArray(new String[]{});
                    dedicatedProvider.systemIds = null;
                    monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
                    Assert.assertFalse(monitorInfoList.isEmpty(), "getSchedulerJobMonitorInfo with specified tenant should never return null when valid jobs exist.");
                    nextJob = JobDao.getByUuid(monitorInfoList.get(0).getUuid());
                    Assert.assertNotNull(nextJob, "getSchedulerJobMonitorInfo with valid user list should never return null when valid values exist.");
                    Assert.assertTrue(userPermutation.contains(nextJob.getOwner()), "getSchedulerJobMonitorInfo with specified tenant and user list should never return a job from another user.");
                    Assert.assertTrue(nextJob.getStatus() == QUEUED || nextJob.getStatus() == RUNNING || nextJob.getStatus() == PAUSED, "Job with invalid status was returned");
                    
                    Collection<List<String>> systemPermutations = Collections2.permutations(
                            CollectionUtils.collect(Arrays.asList(systemsIds), new Transformer() {
                                public String transform(Object val) {
                                    return tenantId + "-" + val;
                                }
                            }));
                    
                    
                    for (final List<String> systemPermutation: systemPermutations) {
                        
                        // --- Set dedicated configuration information and then run query.
                        dedicatedProvider.tenantId  = null;
                        dedicatedProvider.userNames = null;
                        dedicatedProvider.systemIds = systemPermutation.toArray(new String[]{});
                        monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
                        Assert.assertFalse(monitorInfoList.isEmpty(), "getSchedulerJobMonitorInfo with specified tenant should never return null when valid jobs exist.");
                        nextJob = JobDao.getByUuid(monitorInfoList.get(0).getUuid());
                        Assert.assertNotNull(nextJob, "getSchedulerJobMonitorInfo with valid system list should never rturn null when valid values exist.");
                        Assert.assertTrue(systemPermutation.contains(nextJob.getSystem()), "getSchedulerJobMonitorInfo with specified system list should never return a job from another system. " + nextJob.getSystem() + " !in " + StringUtils.join(systemPermutation, ","));
                        Assert.assertTrue(nextJob.getStatus() == QUEUED || nextJob.getStatus() == RUNNING || nextJob.getStatus() == PAUSED, "Job with invalid status was returned");
                        
                        // --- Set dedicated configuration information and then run query.
                        dedicatedProvider.tenantId  = null;
                        dedicatedProvider.userNames = userPermutation.toArray(new String[]{});
                        dedicatedProvider.systemIds = systemPermutation.toArray(new String[]{});
                        monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
                        Assert.assertFalse(monitorInfoList.isEmpty(), "getSchedulerJobMonitorInfo with specified tenant should never return null when valid jobs exist.");
                        nextJob = JobDao.getByUuid(monitorInfoList.get(0).getUuid());
                        Assert.assertNotNull(nextJob, "getSchedulerJobMonitorInfo with valid user, system, and queue list should never return null when valid values exist.");
                        Assert.assertTrue(userPermutation.contains(nextJob.getOwner()), "getSchedulerJobMonitorInfo with specified user and system list should never return a job from another user. " + nextJob.getOwner() + " !in " +  StringUtils.join(userPermutation, ","));
                        Assert.assertTrue(systemPermutation.contains(nextJob.getSystem()), "getSchedulerJobMonitorInfo with specified user, and system list should never return a job from another system. " + nextJob.getSystem() + " !in " + StringUtils.join(systemPermutation, ","));
                        Assert.assertTrue(nextJob.getStatus() == QUEUED || nextJob.getStatus() == RUNNING || nextJob.getStatus() == PAUSED, "Job with invalid status was returned");
                        
                        // --- Set dedicated configuration information and then run query.
                        dedicatedProvider.tenantId  = tenantId;
                        dedicatedProvider.userNames = userPermutation.toArray(new String[]{});
                        dedicatedProvider.systemIds = systemPermutation.toArray(new String[]{});
                        monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
                        Assert.assertFalse(monitorInfoList.isEmpty(), "getSchedulerJobMonitorInfo with specified tenant should never return null when valid jobs exist.");
                        nextJob = JobDao.getByUuid(monitorInfoList.get(0).getUuid());
                        Assert.assertNotNull(nextJob, "getSchedulerJobMonitorInfo with specified tenant, user, and system list should never return null when valid jobs exist.");
                        Assert.assertEquals(nextJob.getTenantId(), tenantId, "getSchedulerJobMonitorInfo with specified tenant, user, and system should never return a job from another tenant. " + nextJob.getOwner() + " !in " + nextJob.getTenantId() + " != " + tenantId);
                        Assert.assertTrue(userPermutation.contains(nextJob.getOwner()), "getSchedulerJobMonitorInfo with specified tenant, user, and system list should never return a job from another user. " + nextJob.getOwner() + " !in " +  StringUtils.join(userPermutation, ","));
                        Assert.assertTrue(systemPermutation.contains(nextJob.getSystem()), "getSchedulerJobMonitorInfo with specified tenant, user, and system list should never return a job from another system. " + nextJob.getSystem() + " !in " + StringUtils.join(systemPermutation, ","));
                    }
                    
                    
                    List<String> systemQueueFQNs = new ArrayList<String>();
                    systemQueueFQNs.add(tenantId + "-" + systemsIds[0] + "#" + queues[0]);
                    systemQueueFQNs.add(tenantId + "-" + systemsIds[1] + "#" + queues[2]);
                    systemQueueFQNs.add(tenantId + "-" + systemsIds[1] + "#" + queues[0]);
                    systemQueueFQNs.add(tenantId + "-" + systemsIds[2] + "#" + queues[1]);
                    
                    // iterate over all 
                    Collection<List<String>> systemAndQueuePermutations = Collections2.permutations(systemQueueFQNs);
                        
                    for (final List<String> systemAndQueuePermutation: systemAndQueuePermutations) 
                    {
                        // --- Set dedicated configuration information and then run query.
                        dedicatedProvider.tenantId  = null;
                        dedicatedProvider.userNames = null;
                        dedicatedProvider.systemIds = systemAndQueuePermutation.toArray(new String[]{});
                        monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
                        Assert.assertFalse(monitorInfoList.isEmpty(), "getSchedulerJobMonitorInfo with specified tenant should never return null when valid jobs exist.");
                        nextJob = JobDao.getByUuid(monitorInfoList.get(0).getUuid());
                        Assert.assertNotNull(nextJob, "getSchedulerJobMonitorInfo with valid tenant to valid system+queue list should never return null when valid values exist.");
                        Assert.assertTrue(systemAndQueuePermutation.contains(nextJob.getSystem() + "#" + nextJob.getBatchQueue()), "getSchedulerJobMonitorInfo with specified system+queue list should never return a job from another system. " + nextJob.getSystem() + "#" + nextJob.getBatchQueue() + " !in " + StringUtils.join(systemAndQueuePermutation, ","));
                        Assert.assertTrue(nextJob.getStatus() == QUEUED || nextJob.getStatus() == RUNNING || nextJob.getStatus() == PAUSED, "Job with invalid status was returned");
                        
                        // --- Set dedicated configuration information and then run query.
                        dedicatedProvider.tenantId  = null;
                        dedicatedProvider.userNames = userPermutation.toArray(new String[]{});
                        dedicatedProvider.systemIds = systemAndQueuePermutation.toArray(new String[]{});
                        monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
                        Assert.assertFalse(monitorInfoList.isEmpty(), "getSchedulerJobMonitorInfo with specified tenant should never return null when valid jobs exist.");
                        nextJob = JobDao.getByUuid(monitorInfoList.get(0).getUuid());
                        Assert.assertNotNull(nextJob, "getSchedulerJobMonitorInfo with valid user and system+queue list should never return null when valid values exist.");
                        Assert.assertTrue(userPermutation.contains(nextJob.getOwner()), "getSchedulerJobMonitorInfo with specified user and system+queue list should never return a job from another user. " + nextJob.getOwner() + " !in " +  StringUtils.join(userPermutation, ","));
                        Assert.assertTrue(systemAndQueuePermutation.contains(nextJob.getSystem() + "#" + nextJob.getBatchQueue()), "getSchedulerJobMonitorInfo with specified user and system+queue list should never return a job from another system. " + nextJob.getSystem() + "#" + nextJob.getBatchQueue() + " !in " + StringUtils.join(systemAndQueuePermutation, ","));
                        Assert.assertTrue(nextJob.getStatus() == QUEUED || nextJob.getStatus() == RUNNING || nextJob.getStatus() == PAUSED, "Job with invalid status was returned");
                        
                        // --- Set dedicated configuration information and then run query.
                        dedicatedProvider.tenantId  = tenantId;
                        dedicatedProvider.userNames = userPermutation.toArray(new String[]{});
                        dedicatedProvider.systemIds = systemAndQueuePermutation.toArray(new String[]{});
                        monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
                        nextJob = JobDao.getByUuid(monitorInfoList.get(0).getUuid());
                        Assert.assertNotNull(nextJob, "getSchedulerJobMonitorInfo with specified tenant, user, system, and queue list should never return null when valid jobs exist.");
                        Assert.assertEquals(nextJob.getTenantId(), tenantId, "getSchedulerJobMonitorInfo with specified tenant, user, and system+queue should never return a job from another tenant.");
                        Assert.assertTrue(userPermutation.contains(nextJob.getOwner()), "getSchedulerJobMonitorInfo with specified tenant, user, and system+queue list should never return a job from another user. " + nextJob.getOwner() + " !in " +  StringUtils.join(userPermutation, ","));
                        Assert.assertTrue(systemAndQueuePermutation.contains(nextJob.getSystem() + "#" + nextJob.getBatchQueue()), "getSchedulerJobMonitorInfo with specified tenant, user, and system+queue list should never return a job from another system. " + nextJob.getSystem() + "#" + nextJob.getBatchQueue() + " !in " + StringUtils.join(systemAndQueuePermutation, ","));
                        Assert.assertTrue(nextJob.getStatus() == QUEUED || nextJob.getStatus() == RUNNING || nextJob.getStatus() == PAUSED, "Job with invalid status was returned");
                        
                    }
            
                }
            
            }
                
            
        }
        catch (Exception e) {
            Assert.fail("Unexpected error occurred running test for next running job with status QUEUED", e);
        }
        finally {
        }
    }
	
	@Test(dependsOnMethods={"getNextExecutingJobUuidSelectsHonorsDedicatedParameters"})
    public void getNextExecutingJobUuidSelectsHonorsNegatedDedicatedParameters() 
    {
        // Initialize the reusable variables.  During monitoring,
        // we only check the executing states.
        LinkedList<JobStatusType> statusList = new LinkedList<>();
        statusList.add(JobStatusType.QUEUED);
        statusList.add(JobStatusType.RUNNING);
        statusList.add(JobStatusType.PAUSED);
        JobPhaseType phase = JobPhaseType.MONITORING; 
        List<JobMonitorInfo> monitorInfoList = null;
        
        // Get the provider for dedicated configuration information created during set up.
        SimpleDedicatedProvider dedicatedProvider = 
            (SimpleDedicatedProvider) DedicatedConfig.getInstance().getDedicatedProvider();
        
        Hashtable<String, Hashtable<String, Integer>> tenantJobSelections = new Hashtable<String, Hashtable<String, Integer>>();
        Hashtable<String, Hashtable<String, Integer>> userJobSelections = new Hashtable<String, Hashtable<String, Integer>>();
        Hashtable<String, Hashtable<String, Integer>> systemJobSelections = new Hashtable<String, Hashtable<String, Integer>>();
        Hashtable<String, Hashtable<String, Integer>> systemQueueJobSelection = new Hashtable<String, Hashtable<String, Integer>>();
        Hashtable<String, Long> uuidSelections = new Hashtable<String, Long>();
        
        try 
        {   
            
            // initialize several jobs from each status
            Date created = new DateTime().minusMinutes(30).toDate();
            Date lastUpdated = new DateTime().minusSeconds(30).toDate();
            
            Job job = createJob(QUEUED);
            
            for (String tenantId: tenantIds) {
                Hashtable<String, Integer> tenantJobHits = new Hashtable<String,Integer>();
                
                for (String username: usernames) {
                    Hashtable<String, Integer> userJobHits = new Hashtable<String,Integer>();
                    for (String systemId: systemsIds) {
                        Hashtable<String, Integer> systemJobHits = new Hashtable<String,Integer>();
                        for (String queueName: queues) {
                            Hashtable<String, Integer> queueJobHits = new Hashtable<String,Integer>();
                            
                            for (JobStatusType status: new JobStatusType[] {QUEUED, RUNNING,PAUSED}) {
                                Job testJob = job.copy();
                                testJob.setCreated(created);
                                testJob.setLastUpdated(lastUpdated);
                                testJob.initStatus(status, status.getDescription());
                                testJob.setTenantId(tenantId);
                                testJob.setOwner(tenantId + "@" + username);
                                testJob.setSystem(tenantId + "-" + systemId);
                                testJob.setLocalJobId(testJob.getUuid());
                                testJob.setBatchQueue(queueName);
                                JobDao.create(testJob, false);
                            
                                queueJobHits.put(testJob.getUuid(), 0);
                                tenantJobHits.put(testJob.getUuid(), 0);
                                systemJobHits.put(testJob.getUuid(), 0);
                                userJobHits.put(testJob.getUuid(), 0);
                                uuidSelections.put(testJob.getUuid(), (long)0);
                            }
                            
                            for (JobStatusType status: JobStatusType.values()) {
                                if (status != QUEUED && status != RUNNING && status != PAUSED) 
                                {
                                    Job decoyJob = job.copy();
                                    decoyJob.setCreated(created);
                                    decoyJob.setLastUpdated(lastUpdated);
                                    decoyJob.initStatus(status, status.getDescription());
                                    decoyJob.setTenantId(tenantId);
                                    decoyJob.setOwner(tenantId + "@" + username);
                                    decoyJob.setSystem(tenantId + "-" + systemId);
                                    decoyJob.setLocalJobId(decoyJob.getUuid());
                                    decoyJob.setBatchQueue(queueName);
                                    JobDao.create(decoyJob, false);
                                }
                            }
                            
                            systemQueueJobSelection.put(tenantId + "-" + systemId+"#"+queueName, queueJobHits);
                        }
                        
                        systemJobSelections.put(tenantId + "-" + systemId, systemJobHits);
                    }
                    
                    userJobSelections.put(tenantId + "@" + username, userJobHits);
                }
                
                tenantJobSelections.put(tenantId, tenantJobHits);
            }
            
            Job nextJob = null;
            for (final String tenantId: tenantIds) {
                
                // --- Set dedicated configuration information and then run query.
                dedicatedProvider.tenantId  = "!" + tenantId;
                dedicatedProvider.userNames = null;
                dedicatedProvider.systemIds = null;
                monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
                Assert.assertFalse(monitorInfoList.isEmpty(), "getSchedulerJobMonitorInfo with specified tenant should never return null when valid jobs exist.");
                nextJob = JobDao.getByUuid(monitorInfoList.get(0).getUuid());
                Assert.assertNotNull(nextJob, "getSchedulerJobMonitorInfo with specified tenant should never return null when valid jobs exist.");
                Assert.assertNotEquals(nextJob.getTenantId(), tenantId, "getSchedulerJobMonitorInfo with negated tenant should never return a job from a negated tenant.");
                Assert.assertTrue(nextJob.getStatus() == QUEUED || nextJob.getStatus() == RUNNING || nextJob.getStatus() == PAUSED, "Job with invalid status was returned");
                
                @SuppressWarnings("unchecked")
                Collection<List<String>> userPermutations = Collections2.permutations(
                        CollectionUtils.collect(Arrays.asList(usernames), new Transformer() {
                            public String transform(Object val) {
                                return "!" + tenantId + "@" + val;
                            }
                        }));
                for (final List<String> userPermutation: userPermutations) {
                    
                    // --- Set dedicated configuration information and then run query.
                    dedicatedProvider.tenantId  = tenantId;
                    dedicatedProvider.userNames = userPermutation.toArray(new String[]{});
                    dedicatedProvider.systemIds = null;
                    monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
                    Assert.assertFalse(monitorInfoList.isEmpty(), "getSchedulerJobMonitorInfo with specified tenant should never return null when valid jobs exist.");
                    if (userPermutation.size() == usernames.length) {
                        Assert.assertNull(monitorInfoList.get(0).getUuid(), "getSchedulerJobMonitorInfo with all users in exclusion list should always return null.");
                    } else  {
                        nextJob = JobDao.getByUuid(monitorInfoList.get(0).getUuid());
                        Assert.assertNotNull(nextJob, "getSchedulerJobMonitorInfo with specified tenant and user list should never return null when valid jobs exist.");
                        Assert.assertEquals(nextJob.getTenantId(), tenantId, "getSchedulerJobMonitorInfo with specified tenant should never return a job from another tenant.");
                        Assert.assertFalse(userPermutation.contains(nextJob.getOwner()), "getSchedulerJobMonitorInfo with specified tenant and negated user list should never return a job from the negated user.");
                        Assert.assertTrue(nextJob.getStatus() == QUEUED || nextJob.getStatus() == RUNNING || nextJob.getStatus() == PAUSED, "Job with invalid status was returned");
                    }
                    
                    // --- Set dedicated configuration information and then run query.
                    dedicatedProvider.tenantId  = null;
                    dedicatedProvider.userNames = userPermutation.toArray(new String[]{});
                    dedicatedProvider.systemIds = null;
                    monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
                    Assert.assertFalse(monitorInfoList.isEmpty(), "getSchedulerJobMonitorInfo with specified tenant should never return null when valid jobs exist.");
                    nextJob = JobDao.getByUuid(monitorInfoList.get(0).getUuid());
                    Assert.assertNotNull(nextJob, "getSchedulerJobMonitorInfo with valid user list should never return null when valid values exist.");
                    Assert.assertFalse(userPermutation.contains(nextJob.getOwner()), "getSchedulerJobMonitorInfo with specified tenant and negated user list should never return a job from the negated user.");
                    Assert.assertTrue(nextJob.getStatus() == QUEUED || nextJob.getStatus() == RUNNING || nextJob.getStatus() == PAUSED, "Job with invalid status was returned");
                    
                    Collection<List<String>> systemPermutations = Collections2.permutations(
                            CollectionUtils.collect(Arrays.asList(systemsIds), new Transformer() {
                                public String transform(Object val) {
                                    return "!" + tenantId + "-" + val;
                                }
                            }));
                    
                    for (final List<String> systemPermutation: systemPermutations) {
                        
                        // --- Set dedicated configuration information and then run query.
                        dedicatedProvider.tenantId  = null;
                        dedicatedProvider.userNames = null;
                        dedicatedProvider.systemIds = systemPermutation.toArray(new String[]{});
                        monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
                        Assert.assertFalse(monitorInfoList.isEmpty(), "getSchedulerJobMonitorInfo with specified tenant should never return null when valid jobs exist.");
                        nextJob = JobDao.getByUuid(monitorInfoList.get(0).getUuid());
                        Assert.assertNotNull(nextJob, "getSchedulerJobMonitorInfo with valid system list should never rturn null when valid values exist.");
                        Assert.assertFalse(systemPermutation.contains(nextJob.getSystem()), "getSchedulerJobMonitorInfo with negated system list should never return a job from the negated system. " + nextJob.getSystem() + " !in " + StringUtils.join(systemPermutation, ","));
                        Assert.assertTrue(nextJob.getStatus() == QUEUED || nextJob.getStatus() == RUNNING || nextJob.getStatus() == PAUSED, "Job with invalid status was returned");
                        
                        // --- Set dedicated configuration information and then run query.
                        dedicatedProvider.tenantId  = null;
                        dedicatedProvider.userNames = userPermutation.toArray(new String[]{});
                        dedicatedProvider.systemIds = systemPermutation.toArray(new String[]{});
                        monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
                        Assert.assertFalse(monitorInfoList.isEmpty(), "getSchedulerJobMonitorInfo with specified tenant should never return null when valid jobs exist.");
                        nextJob = JobDao.getByUuid(monitorInfoList.get(0).getUuid());
                        Assert.assertNotNull(nextJob, "getSchedulerJobMonitorInfo with valid user, system, and queue list should never return null when valid values exist.");
                        Assert.assertFalse(userPermutation.contains(nextJob.getOwner()), "getSchedulerJobMonitorInfo with specified user and negated system list should never return a job from the negated user. " + nextJob.getOwner() + " !in " +  StringUtils.join(userPermutation, ","));
                        Assert.assertFalse(systemPermutation.contains(nextJob.getSystem()), "getSchedulerJobMonitorInfo with specified user, and negated system list should never return a job from the negated system. " + nextJob.getSystem() + " !in " + StringUtils.join(systemPermutation, ","));
                        Assert.assertTrue(nextJob.getStatus() == QUEUED || nextJob.getStatus() == RUNNING || nextJob.getStatus() == PAUSED, "Job with invalid status was returned");
                        
                        // --- Set dedicated configuration information and then run query.
                        dedicatedProvider.tenantId  = tenantId;
                        dedicatedProvider.userNames = userPermutation.toArray(new String[]{});
                        dedicatedProvider.systemIds = systemPermutation.toArray(new String[]{});
                        monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
                        Assert.assertFalse(monitorInfoList.isEmpty(), "getSchedulerJobMonitorInfo with specified tenant should never return null when valid jobs exist.");
                        if (systemPermutation.size() == systemsIds.length) {
                            Assert.assertNull(monitorInfoList.get(0).getUuid(), "getSchedulerJobMonitorInfo with all systems in exclusion list should always return null.");
                        } else if (userPermutation.size() == usernames.length) {
                            Assert.assertNull(monitorInfoList.get(0).getUuid(), "getSchedulerJobMonitorInfo with all users in exclusion list should always return null.");
                        } else {
                            nextJob = JobDao.getByUuid(monitorInfoList.get(0).getUuid());
                            Assert.assertNotNull(nextJob, "getSchedulerJobMonitorInfo with specified tenant, user, and system list should never return null when valid jobs exist.");
                            Assert.assertEquals(nextJob.getTenantId(), tenantId, "getSchedulerJobMonitorInfo with specified tenant, user, and system should never return a job from another tenant. " + nextJob.getOwner() + " !in " + nextJob.getTenantId() + " != " + tenantId);
                            Assert.assertFalse(userPermutation.contains(nextJob.getOwner()), "getSchedulerJobMonitorInfo with specified tenant, user, and negated system list should never return a job from the negated user. " + nextJob.getOwner() + " !in " +  StringUtils.join(userPermutation, ","));
                            Assert.assertFalse(systemPermutation.contains(nextJob.getSystem()), "getSchedulerJobMonitorInfo with specified tenant, user, and negated system list should never return a job from the negated system. " + nextJob.getSystem() + " !in " + StringUtils.join(systemPermutation, ","));
                        }
                    }
                    
                    
                    List<String> systemQueueFQNs = new ArrayList<String>();
                    systemQueueFQNs.add("!" + tenantId + "-" + systemsIds[0] + "#" + queues[0]);
                    systemQueueFQNs.add("!" + tenantId + "-" + systemsIds[1] + "#" + queues[2]);
                    systemQueueFQNs.add("!" + tenantId + "-" + systemsIds[1] + "#" + queues[0]);
                    systemQueueFQNs.add("!" + tenantId + "-" + systemsIds[2] + "#" + queues[1]);
                    
                    // iterate over all 
                    Collection<List<String>> systemAndQueuePermutations = Collections2.permutations(systemQueueFQNs);
                        
                    for (final List<String> systemAndQueuePermutation: systemAndQueuePermutations) 
                    {
                        // --- Set dedicated configuration information and then run query.
                        dedicatedProvider.tenantId  = null;
                        dedicatedProvider.userNames = null;
                        dedicatedProvider.systemIds = systemAndQueuePermutation.toArray(new String[]{});
                        monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
                        Assert.assertFalse(monitorInfoList.isEmpty(), "getSchedulerJobMonitorInfo with specified tenant should never return null when valid jobs exist.");
                        nextJob = JobDao.getByUuid(monitorInfoList.get(0).getUuid());
                        Assert.assertNotNull(nextJob, "getSchedulerJobMonitorInfo with valid tenant to valid system+queue list should never return null when valid values exist.");
                        Assert.assertFalse(systemAndQueuePermutation.contains(nextJob.getSystem() + "#" + nextJob.getBatchQueue()), "getSchedulerJobMonitorInfo with negated system+queue list should never return a job from the negated system. " + nextJob.getSystem() + "#" + nextJob.getBatchQueue() + " !in " + StringUtils.join(systemAndQueuePermutation, ","));
                        Assert.assertTrue(nextJob.getStatus() == QUEUED || nextJob.getStatus() == RUNNING || nextJob.getStatus() == PAUSED, "Job with invalid status was returned");
                        
                        // --- Set dedicated configuration information and then run query.
                        dedicatedProvider.tenantId  = null;
                        dedicatedProvider.userNames = userPermutation.toArray(new String[]{});
                        dedicatedProvider.systemIds = systemAndQueuePermutation.toArray(new String[]{});
                        monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
                        Assert.assertFalse(monitorInfoList.isEmpty(), "getSchedulerJobMonitorInfo with specified tenant should never return null when valid jobs exist.");
                        nextJob = JobDao.getByUuid(monitorInfoList.get(0).getUuid());
                        Assert.assertNotNull(nextJob, "getSchedulerJobMonitorInfo with valid user and system+queue list should never return null when valid values exist.");
                        Assert.assertFalse(userPermutation.contains(nextJob.getOwner()), "getSchedulerJobMonitorInfo with specified user and system+queue list should never return a job from the negated user. " + nextJob.getOwner() + " !in " +  StringUtils.join(userPermutation, ","));
                        Assert.assertFalse(systemAndQueuePermutation.contains(nextJob.getSystem() + "#" + nextJob.getBatchQueue()), "getSchedulerJobMonitorInfo with negated specified user and system+queue list should never return a job from the negated system. " + nextJob.getSystem() + "#" + nextJob.getBatchQueue() + " !in " + StringUtils.join(systemAndQueuePermutation, ","));
                        Assert.assertTrue(nextJob.getStatus() == QUEUED || nextJob.getStatus() == RUNNING || nextJob.getStatus() == PAUSED, "Job with invalid status was returned");
                
                        // --- Set dedicated configuration information and then run query.
                        dedicatedProvider.tenantId  = tenantId;
                        dedicatedProvider.userNames = userPermutation.toArray(new String[]{});
                        dedicatedProvider.systemIds = systemAndQueuePermutation.toArray(new String[]{});
                        monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
                        Assert.assertFalse(monitorInfoList.isEmpty(), "getSchedulerJobMonitorInfo with specified tenant should never return null when valid jobs exist.");
                        if (userPermutation.size() == usernames.length) {
                            Assert.assertNull(monitorInfoList.get(0).getUuid(), "getNextExecutingJobUuid with all users in exclusion list should always return null.");
                        } else  {
                            nextJob = JobDao.getByUuid(monitorInfoList.get(0).getUuid());
                            Assert.assertNotNull(nextJob, "getSchedulerJobMonitorInfo with specified tenant, user, system, and queue list should never return null when valid jobs exist.");
                            Assert.assertEquals(nextJob.getTenantId(), tenantId, "getSchedulerJobMonitorInfo with specified tenant, user, and negated system+queue should never return a job from another tenant.");
                            Assert.assertFalse(userPermutation.contains(nextJob.getOwner()), "getSchedulerJobMonitorInfo with specified tenant, user, and negated system+queue list should never return a job from the negated user. " + nextJob.getOwner() + " !in " +  StringUtils.join(userPermutation, ","));
                            Assert.assertFalse(systemAndQueuePermutation.contains(nextJob.getSystem() + "#" + nextJob.getBatchQueue()), "getSchedulerJobMonitorInfo with specified tenant, user, and negated system+queue list should never return a job from the negated system. " + nextJob.getSystem() + "#" + nextJob.getBatchQueue() + " !in " + StringUtils.join(systemAndQueuePermutation, ","));
                            Assert.assertTrue(nextJob.getStatus() == QUEUED || nextJob.getStatus() == RUNNING || nextJob.getStatus() == PAUSED, "Job with invalid status was returned");
                        }
                    }
                    
                    systemQueueFQNs.clear();
                    for (String systemId: systemsIds) {
                        for (String queueName: queues) {
                            systemQueueFQNs.add("!" + tenantId + "-" + systemId + "#" + queueName);
                        }
                    }
                    
                    // --- Set dedicated configuration information and then run query.
                    dedicatedProvider.tenantId  = tenantId;
                    dedicatedProvider.userNames = userPermutation.toArray(new String[]{});
                    dedicatedProvider.systemIds = systemQueueFQNs.toArray(new String[]{});
                    monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
                    Assert.assertTrue(monitorInfoList.isEmpty(), "getSchedulerJobMonitorInfo with specified tenant should never return null when valid jobs exist.");
                }
            
            }
                
            
        }
        catch (Exception e) {
            Assert.fail("Unexpected error occurred running test for next running job with status QUEUED", e);
        }
        finally {
//            try { clearJobs(); } catch (Exception e) {}
        }
    }
	
	//@Test(dependsOnMethods={"getNextExecutingJobUuid"})
	public void getNextExecutingJobUuidConcurrencyTest() throws Exception 
	{
		DateTime dummyLastUpdated = new DateTime();
		dummyLastUpdated = dummyLastUpdated.minusDays(2);
		
		Job executingJob = createJob(JobStatusType.QUEUED, false); // don't save yet
		executingJob.setCreated(dummyLastUpdated.minusSeconds(30).toDate());
		executingJob.setLastUpdated(dummyLastUpdated.toDate());
		executingJob.setStatusChecks(0);
		JobDao.create(executingJob, false);
		Assert.assertNotNull(executingJob.getId(), "Failed to persist test job");
		
		int threadCount = 500;
		ExecutorService executorService = Executors.newFixedThreadPool(20);
        List<Future<Void>> futures = new ArrayList<Future<Void>>();
        for (int x = 0; x < threadCount; x++) 
        {
            Callable<Void> callable = new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    // Initialize the reusable variables.
                    LinkedList<JobStatusType> statusList = new LinkedList<>();
                    statusList.add(JobStatusType.QUEUED);
                    statusList.add(JobStatusType.RUNNING);
                    statusList.add(JobStatusType.PAUSED);
                    JobPhaseType phase = JobPhaseType.MONITORING; 
                    List<JobMonitorInfo> monitorInfoList = null;
                    
                    // Get the provider for dedicated configuration information created during set up.
                    SimpleDedicatedProvider dedicatedProvider = 
                        (SimpleDedicatedProvider) DedicatedConfig.getInstance().getDedicatedProvider();
                    
                    // --- Set dedicated configuration information and then run query.
                    dedicatedProvider.tenantId  = null;
                    dedicatedProvider.userNames = null;
                    dedicatedProvider.systemIds = null;
                    monitorInfoList = JobDao.getSchedulerJobMonitorInfo(phase, statusList);
                    Assert.assertFalse(monitorInfoList.isEmpty(),
                            "Expected to retrieve executing job.");
                    
                	Job job = JobDao.getByUuid(monitorInfoList.get(0).getUuid());
                    
                	job.setRetries(job.getRetries()+1);
                	//(JobStatusType.RUNNING, "Updating from the future");
                	JobUpdateParameters jobUpdateParameters = new JobUpdateParameters();
                	jobUpdateParameters.setRetries(job.getRetries());
                	jobUpdateParameters.setLastUpdated(job.getLastUpdated());
                	JobDao.update(job, jobUpdateParameters);
                    return null;
                }
            };
            Future<Void> submit = executorService.submit(callable);
            futures.add(submit);
        }
 
        List<Exception> exceptions = new ArrayList<Exception>();
        for (Future<Void> future : futures) 
        {
            try {
                future.get();
            } catch (Exception e) {
                exceptions.add(e);
            }
        }
 
        executorService.shutdown();
 
        HibernateUtil.getSession().clear();
        Assert.assertFalse(exceptions.isEmpty(), "There should be concurrency exceptions thrown from simultaneous access attempts.");
        Job testJobRecord = JobDao.getById(executingJob.getId());
		
//        JobDao.refresh(executingJob);
        Assert.assertTrue(testJobRecord.getRetries() > 0, "Job was never updated by any of the concurrent threads.");
        Assert.assertTrue(testJobRecord.getRetries() < threadCount, "Job was updated by all of the concurrent threads.");
        Assert.assertEquals(threadCount, exceptions.size() + testJobRecord.getRetries(), "Some threads failed to update a job, but did not throw an exception.");
	}
	
	private int getExpectedIntervalForCheckCount(int numberOfChecks) 
	{
		int backoff = 0;
		if (numberOfChecks < 4) {
			backoff = 15;
		} else if (numberOfChecks < 14) {
			backoff = 30;
		} else if (numberOfChecks < 44) {
			backoff = 60;
		} else if (numberOfChecks < 56) {
			backoff = 300;
		} else if (numberOfChecks < 104) {
			backoff = 900;
		} else if (numberOfChecks < 152) {
			backoff = 1800;
		} else {
			backoff = 3600;
		} 
		
		return backoff;
	}
}
