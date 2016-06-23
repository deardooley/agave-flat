/**
 * 
 */
package org.iplantc.service.jobs.dao;

import static org.iplantc.service.jobs.model.JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE;
import static org.iplantc.service.jobs.model.JSONTestDataUtil.TEST_OWNER;
import static org.iplantc.service.jobs.model.JSONTestDataUtil.TEST_SOFTWARE_SYSTEM_FILE;
import static org.iplantc.service.jobs.model.JSONTestDataUtil.TEST_STORAGE_SYSTEM_FILE;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.log4j.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.iplantc.service.apps.exceptions.SoftwareException;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwareInput;
import org.iplantc.service.apps.model.SoftwareParameter;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.common.util.Slug;
import org.iplantc.service.jobs.exceptions.JobException;
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
import org.iplantc.service.transfer.RemoteDataClient;
import org.iplantc.service.transfer.dao.TransferTaskDao;
import org.iplantc.service.transfer.model.TransferTask;
import org.iplantc.service.transfer.model.enumerations.TransferStatusType;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author dooley
 *
 */
public class AbstractDaoTest 
{	
    private static final Logger log = Logger.getLogger(AbstractDaoTest.class);
            
    public static final BatchQueue shortQueue = new BatchQueue("short", (long)1000, (long)10, (long)1, 16.0, (long)16, "01:00:00", null, true);
    public static final BatchQueue mediumQueue = new BatchQueue("medium", (long)100, (long)10, (long)1, 16.0, (long)16, "12:00:00", null, false);
    public static final BatchQueue longQueue = new BatchQueue("long", (long)10, (long)4, (long)1, 16.0, (long)16, "48:00:00", null, false);
    public static final BatchQueue dedicatedQueue = new BatchQueue("dedicated", (long)1, (long)1, (long)1, 16.0, (long)16, "144:00:00", null, false);
    public static final BatchQueue unlimitedQueue = new BatchQueue("dedicated", (long)-1, 2048.0);
    
    
	public static String EXECUTION_SYSTEM_TEMPLATE_DIR = "target/test-classes/systems/execution";
	public static String STORAGE_SYSTEM_TEMPLATE_DIR = "target/test-classes/systems/storage";
	public static String SOFTWARE_SYSTEM_TEMPLATE_DIR = "target/test-classes/software";
	public static String INTERNAL_USER_TEMPLATE_DIR = "target/test-classes/internal_users";
	public static String CREDENTIALS_TEMPLATE_DIR = "target/test-classes/credentials";
	
	protected JSONTestDataUtil jtd;
	protected SystemDao systemDao = new SystemDao();
	protected StorageSystem privateStorageSystem;
	protected ExecutionSystem privateExecutionSystem;
	public Software software;
	
	/**
	 * Clears database of all {@link RemoteSystem}, {@link Software}, {@link Job}, 
	 * {@link TransferTask}, and {@link Notification}. Creates new test instances 
	 * of each type as needed.
	 * 
	 * @throws Exception
	 */
	@BeforeClass
	protected void beforeClass() throws Exception
	{
		systemDao = new SystemDao();
		
		jtd = JSONTestDataUtil.getInstance();
		
        initSystems();
        initSoftware();
        clearJobs();
	}
	
	/**
     * Initialize one {@link ExecutionSystem} and one {@link StorageSystem} object for testing.
     * These systems will be the target for the {@link Software} record created by the 
     * {@link #initSoftware()} method.<br>
     * <br>
     * <strong>note:</note> This should be overridden by implementing classes
     * to instantiate all needed {@link ExecutionSystem} and {@link StorageSystem} records for testing.
     * 
     * @throws Exception
     */
	protected void initSystems() throws Exception
	{
		clearSystems();
		
		privateExecutionSystem = ExecutionSystem.fromJSON(jtd.getTestDataObject(TEST_EXECUTION_SYSTEM_FILE));
		privateExecutionSystem.setOwner(TEST_OWNER);
		privateExecutionSystem.getUsersUsingAsDefault().add(TEST_OWNER);
		privateExecutionSystem.setType(RemoteSystemType.EXECUTION);
		systemDao.persist(privateExecutionSystem);
		
		privateStorageSystem = StorageSystem.fromJSON(jtd.getTestDataObject(TEST_STORAGE_SYSTEM_FILE));
		privateStorageSystem.setOwner(TEST_OWNER);
		privateStorageSystem.getUsersUsingAsDefault().add(TEST_OWNER);
		privateStorageSystem.setType(RemoteSystemType.STORAGE);
        systemDao.persist(privateStorageSystem);
	}
	
	/**
     * Delete all {@link ExecutionSystem} and {@link StorageSystem} records from the db.
     * @throws Exception
     */
	protected void clearSystems()
	{
	    Session session = null;
        try
        {
    		HibernateUtil.beginTransaction();
    		session = HibernateUtil.getSession();
    		session.clear();
    		session.disableFilter("systemTenantFilter");
    		session.createQuery("delete RemoteSystem").executeUpdate();
    		HibernateUtil.flush();
        }
        catch (HibernateException ex) {
            throw new SoftwareException(ex);
        }
        finally {
            try { HibernateUtil.commitTransaction();} catch (Throwable e) {}
        }	
	}
	
	/**
	 * Initialize a single software object for testing.<br>
	 * <br>
	 * <strong>note:</note> This should be overridden by implementing classes
	 * to instantiate all needed software records for testing.
	 * 
	 * @throws Exception
	 */
	protected void initSoftware() throws Exception
	{
		clearSoftware(); 
		
		JSONObject json = jtd.getTestDataObject(TEST_SOFTWARE_SYSTEM_FILE);
		software = Software.fromJSON(json, TEST_OWNER);
		software.setExecutionSystem(privateExecutionSystem);
		software.setOwner(TEST_OWNER);
		software.setVersion(software.getVersion());
	}
	
	/**
	 * Delete all software records from the db.
	 * @throws Exception
	 */
	protected void clearSoftware() throws Exception
	{
		Session session = null;
		try
		{
			HibernateUtil.beginTransaction();
			session = HibernateUtil.getSession();
			session.clear();
			HibernateUtil.disableAllFilters();
            session.createQuery("delete Software").executeUpdate();
			session.flush();
		}
		catch (HibernateException ex) {
			throw new SoftwareException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Throwable e) {}
		}
	}
	
	/**
     * Delete all {@link JobEvent}, {@link TransferTask}, {@link JobPermission}, {@link Notification}, 
     * and {@link Job}s from the database.
     * @throws Exception
     */
	protected void clearJobs() throws Exception
	{
		Session session = null;
		try
		{
			HibernateUtil.beginTransaction();
			session = HibernateUtil.getSession();
			session.clear();
			HibernateUtil.disableAllFilters();
			
            session.createQuery("delete JobEvent").executeUpdate();
			session.createQuery("delete TransferTask").executeUpdate();
			session.createQuery("delete JobPermission").executeUpdate();
            session.createQuery("delete Notification").executeUpdate();
			session.createQuery("delete Job").executeUpdate();
			
			session.flush();
		}
		catch (Throwable ex) {
			throw new JobException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction();} catch (Throwable e) {}
		}
	}
	
	/**
	 * Delete all {@link Notification}s from the database
	 * @throws Exception
	 */
	protected void clearJobNotifications() throws Exception
	{
	    Session session = null;
        try
        {
            HibernateUtil.beginTransaction();
            session = HibernateUtil.getSession();
            session.clear();
            HibernateUtil.disableAllFilters();
            session.createQuery("delete Notification").executeUpdate();
            session.flush();
        }
        catch (Throwable ex) {
            throw new JobException(ex);
        }
        finally {
            try { HibernateUtil.commitTransaction();} catch (Throwable e) {}
        }
	}

	/**
     * Clears database of all {@link RemoteSystem}, {@link Software}, {@link Job}, 
     * {@link TransferTask}, and {@link Notification}. 
     * 
     * @throws Exception
     */
	@AfterClass
	protected void afterClass() throws Exception
	{
		clearJobs();
		clearSoftware();
		clearSystems();
	}
	
	/**
	 * Creates and persists a {@link Job} on the given {@code exeSystem} and {@code queue} for the
	 * with the given {@code username} as owner. The resulting {@link Job#getEvents()} list will
	 * contain fabricated {@link JobEvent}s as expected by a normal chain of execution.
	 * 
	 * @param status
	 * @param exeSystem
	 * @param queue
	 * @param username
	 * @return
	 * @throws Exception
	 */
	public Job createJob(JobStatusType status, ExecutionSystem exeSystem, BatchQueue queue, String username)
    throws Exception 
	{
	    try {
            ObjectMapper mapper = new ObjectMapper();
            
            Job job = new Job();
            job.setName("test-" + exeSystem.getName() + "_" + queue.getName());
            job.setOutputPath(exeSystem.getScratchDir() + username + "job-" + Slug.toSlug(job.getName()));
            job.setOwner(username);
            job.setInternalUsername(null);
            
            job.setArchiveOutput(true);
            job.setArchiveSystem(privateStorageSystem);
            job.setArchivePath(username + "/archive/test-job-999");
            
            job.setSoftwareName(software.getUniqueName());
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
                job.setStatus(status, status.getDescription());
                
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
                job.setStatus(status, status.getDescription());
                
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
                        
                        job.setStatus(JobStatusType.STAGING_INPUTS, event);
                    }
                }
                
                job.setLastUpdated(stagingTime.toDate());
                
            } else if (status == JobStatusType.ARCHIVING 
                        || status == JobStatusType.ARCHIVING_FAILED 
                        || status == JobStatusType.ARCHIVING_FINISHED ) {
                
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
                        
                        job.setStatus(JobStatusType.STAGING_INPUTS, event);
                    }
                }
                
                job.setStatus(JobStatusType.STAGED, JobStatusType.STAGED.getDescription());
                job.setLocalJobId("q." + System.currentTimeMillis());
                job.setSchedulerJobId(job.getUuid());
                job.setStatus(status, status.getDescription());
                
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
                job.setStatus(status, event);
                
                if (status != JobStatusType.ARCHIVING) {
                    JobEvent event2 = new JobEvent(
                            status,
                            status.getDescription(),
                            job.getOwner());
                    event2.setCreated(archiveTime.toDate());
                    job.setStatus(status, event2);
                }
                
                job.setLastUpdated(archiveTime.toDate());
            }
            else if (JobStatusType.isFailed(status)) {
                job.setEndTime(new Date());
            }
            else {
                job.setStatus(status, status.getDescription());
            }
            
            log.debug("Adding job " + job.getId() + " - " + job.getUuid());
            JobDao.persist(job, false);
            
            return job;
	    } catch (Exception e) {
	        log.error("Failed to create test job", e);
	        throw e;
	    }
    }
	
	/**
	 * Creates a {@link Job} delegating to the {@link #createJob(JobStatusType, ExecutionSystem, BatchQueue, String)} method.
	 * 
	 * @param status
	 * @return persisted job object
	 * @throws Exception
	 */
	public Job createJob(JobStatusType status) throws Exception
	{
	    ExecutionSystem exeSystem = software.getExecutionSystem();
	    String batchQueue = StringUtils.isEmpty(software.getDefaultQueue()) ? software.getExecutionSystem().getDefaultQueue().getName() : software.getDefaultQueue();
	    return createJob(status, exeSystem, exeSystem.getQueue(batchQueue), TEST_OWNER);
	}
	
	/**
	 * Removes the current tenant id and end user from the current thread.
	 */
	public void clearCurrentTenancyInfo() {
	    TenancyHelper.setCurrentEndUser(null);
        TenancyHelper.setCurrentTenantId(null);
	}
	
	protected void stageRemoteSoftwareAssets() throws Exception 
    {
        RemoteSystem storageSystem = software.getStorageSystem();
        RemoteDataClient storageDataClient = null;
        try 
        {
            storageDataClient = storageSystem.getRemoteDataClient();
            storageDataClient.authenticate();
            if (!storageDataClient.doesExist(software.getDeploymentPath())) {
                storageDataClient.mkdirs(FilenameUtils.getPath(software.getDeploymentPath()));
                storageDataClient.put(SOFTWARE_SYSTEM_TEMPLATE_DIR + File.separator + software.getUniqueName(), FilenameUtils.getPath(software.getDeploymentPath()));
            }
            else
            {
                for (File localSoftwareAssetPath: new File(SOFTWARE_SYSTEM_TEMPLATE_DIR + File.separator + software.getUniqueName()).listFiles()) {
                    if (!storageDataClient.doesExist(software.getDeploymentPath() + File.separator + localSoftwareAssetPath.getName())) {
                        storageDataClient.put(localSoftwareAssetPath.getAbsolutePath(), FilenameUtils.getPath(software.getDeploymentPath()) + File.separator + localSoftwareAssetPath.getName());
                    }
                }
            }
        }
        finally {
            try {storageDataClient.disconnect();} catch (Exception e) {}
        }
        
    }
    
    protected void deleteRemoteSoftwareAssets() throws Exception 
    {
        RemoteSystem storageSystem = software.getStorageSystem();
        RemoteDataClient storageDataClient = null;
        try 
        {
            storageDataClient = storageSystem.getRemoteDataClient();
            storageDataClient.authenticate();
            storageDataClient.delete(software.getDeploymentPath());
        }
        finally {
            try {storageDataClient.disconnect();} catch (Exception e) {}
        }
        
    }
}
