/**
 * 
 */
package org.iplantc.service.jobs.dao;

import static org.iplantc.service.jobs.model.JSONTestDataUtil.TEST_EXECUTION_SYSTEM_FILE;
import static org.iplantc.service.jobs.model.JSONTestDataUtil.TEST_OWNER;
import static org.iplantc.service.jobs.model.JSONTestDataUtil.TEST_SOFTWARE_SYSTEM_FILE;
import static org.iplantc.service.jobs.model.JSONTestDataUtil.TEST_STORAGE_SYSTEM_FILE;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.PENDING;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.QUEUED;
import static org.iplantc.service.jobs.model.enumerations.JobStatusType.STAGED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.log4j.Logger;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.systems.model.BatchQueue;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.enumerations.RemoteSystemType;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.Collections2;

/**
 * Tests checking that the {@link JobDao#getNextQueuedJobUuid(testStatus, )}
 * method
 * honors the service capability configuration options allowing the jobs a
 * worker selects
 * be filtered based on the capabilities of the current worker.
 * 
 * @author dooley
 *
 */
@Test(groups={"broken"})
public class FairShareJobSelectionTest extends AbstractDaoTest {

    public static final Logger log = Logger.getLogger(ProgressiveMonitoringBackoffTest.class);

    private String[] queues = new String[] { "small", "medium", "large" };
    private String[] systemsIds = new String[] { "execute1.example.com", "execute2.example.com",
            "execute3.example.com" };
    private String[] tenantIds = new String[] { "alpha", "beta", "gamma" };
    private String[] usernames = new String[] { "user-0", "user-1", "user-2" };

    @BeforeClass
    public void beforeClass() throws Exception {
        super.beforeClass();
        SoftwareDao.persist(software);
    }

    @AfterClass
    public void afterClass() throws Exception {
        super.afterClass();
    }

    @BeforeMethod
    public void setUp() throws Exception {
        clearJobs();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        clearJobs();
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
        for (String qname : queues) {
            BatchQueue q = unlimitedQueue.clone();
            q.setName(qname);
            privateExecutionSystem.addBatchQueue(q);
        }
        systemDao.persist(privateExecutionSystem);

        privateStorageSystem = StorageSystem.fromJSON(jtd
                .getTestDataObject(TEST_STORAGE_SYSTEM_FILE));
        privateStorageSystem.setOwner(TEST_OWNER);
        privateStorageSystem.setType(RemoteSystemType.STORAGE);
        privateStorageSystem.setGlobalDefault(true);
        privateStorageSystem.setPubliclyAvailable(true);
        log.debug("Inserting public storage system " + privateStorageSystem.getSystemId());
        systemDao.persist(privateStorageSystem);

        for (String tenantId : tenantIds) {
            for (String systemId : systemsIds) {
                ExecutionSystem exeSystem = ExecutionSystem.fromJSON(exeSystemJson);
                exeSystem.setSystemId(tenantId + "-" + systemId);
                exeSystem.setOwner(TEST_OWNER);
                exeSystem.getBatchQueues().clear();
                exeSystem.addBatchQueue(unlimitedQueue.clone());
                for (String qname : queues) {
                    BatchQueue q = unlimitedQueue.clone();
                    q.setName(qname);
                    exeSystem.addBatchQueue(q);
                }
                exeSystem.setPubliclyAvailable(true);
                exeSystem.setType(RemoteSystemType.EXECUTION);
                exeSystem.setTenantId(tenantId);
                log.debug("Inserting execution system " + exeSystem.getSystemId());
                systemDao.persist(exeSystem);
            }

            StorageSystem storageSystem = privateStorageSystem.clone();
            storageSystem.setSystemId(tenantId + "-" + "storage1.example.com");
            storageSystem.setOwner(TEST_OWNER);
            storageSystem.setPubliclyAvailable(true);
            storageSystem.setType(RemoteSystemType.STORAGE);
            storageSystem.setTenantId(tenantId);
            systemDao.persist(storageSystem);
        }

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

        for (String tenantId : tenantIds) {
            TenancyHelper.setCurrentTenantId(tenantId);
            TenancyHelper.setCurrentEndUser(TEST_OWNER);

            // Session session = HibernateUtil.getSession();
            // List<ExecutionSystem> exeSystems = (List<ExecutionSystem>)session
            // .createQuery("from ExecutionSystem where systemId <> :exeid")
            // .setString("exeid", privateExecutionSystem.getSystemId())
            // .list();
            List<ExecutionSystem> exeSystems = systemDao.getAllExecutionSystems();
            for (ExecutionSystem exeSystem : exeSystems) {
                for (BatchQueue q : exeSystem.getBatchQueues()) {
                    Software software = this.software.clone();
                    software.setExecutionSystem((ExecutionSystem) exeSystem);
                    software.setName("test-" + exeSystem.getSystemId() + "-" + q.getName());
                    software.setDefaultQueue(q.getName());
                    software.setDefaultMaxRunTime(q.getMaxRequestedTime());
                    software.setDefaultMemoryPerNode(q.getMaxMemoryPerNode() > 0 ? q
                            .getMaxMemoryPerNode() : null);
                    software.setDefaultNodes(q.getMaxNodes() > 0 ? q.getMaxNodes() : null);
                    software.setDefaultProcessorsPerNode(q.getMaxProcessorsPerNode() > 0 ? q
                            .getMaxProcessorsPerNode() : null);
                    log.debug("Adding software " + software.getUniqueName());
                    SoftwareDao.persist(software);
                }
            }
        }

        TenancyHelper.setCurrentTenantId(null);
        TenancyHelper.setCurrentEndUser(null);
    }

    @Test
    public void getNextQueuedJobUuidSelectsFailsOnMismatchedDependencies() {
        Hashtable<String, Hashtable<String, Integer>> tenantJobSelections = new Hashtable<String, Hashtable<String, Integer>>();
        Hashtable<String, Hashtable<String, Integer>> userJobSelections = new Hashtable<String, Hashtable<String, Integer>>();
        Hashtable<String, Hashtable<String, Integer>> systemJobSelections = new Hashtable<String, Hashtable<String, Integer>>();
        Hashtable<String, Hashtable<String, Integer>> systemQueueJobSelection = new Hashtable<String, Hashtable<String, Integer>>();
        Hashtable<String, Long> uuidSelections = new Hashtable<String, Long>();

        try {
            // initialize several jobs from each status
            Date created = new DateTime().minusMinutes(30).toDate();
            Date lastUpdated = new DateTime().minusSeconds(30).toDate();

            Job job = createJob(QUEUED);

            for (String tenantId : tenantIds) {
                Hashtable<String, Integer> tenantJobHits = new Hashtable<String, Integer>();

                for (String username : usernames) {
                    Hashtable<String, Integer> userJobHits = new Hashtable<String, Integer>();
                    for (String systemId : systemsIds) {
                        Hashtable<String, Integer> systemJobHits = new Hashtable<String, Integer>();
                        for (String queueName : queues) {
                            Hashtable<String, Integer> queueJobHits = new Hashtable<String, Integer>();

                            for (JobStatusType status : new JobStatusType[] { PENDING, STAGED }) {
                                Job testJob = job.copy();
                                testJob.setCreated(created);
                                testJob.setLastUpdated(lastUpdated);
                                testJob.setStatus(status, status.getDescription());
                                testJob.setTenantId(tenantId);
                                testJob.setOwner(tenantId + "@" + username);
                                testJob.setSystem(tenantId + "-" + systemId);
                                testJob.setLocalJobId(testJob.getUuid());
                                testJob.setBatchQueue(queueName);
                                JobDao.persist(testJob, false);

                                queueJobHits.put(testJob.getUuid(), 0);
                                tenantJobHits.put(testJob.getUuid(), 0);
                                systemJobHits.put(testJob.getUuid(), 0);
                                userJobHits.put(testJob.getUuid(), 0);
                                uuidSelections.put(testJob.getUuid(), (long) 0);
                            }

                            for (JobStatusType status : JobStatusType.values()) {
                                if (status != PENDING && status != STAGED) {
                                    Job decoyJob = job.copy();
                                    decoyJob.setCreated(created);
                                    decoyJob.setLastUpdated(lastUpdated);
                                    decoyJob.setStatus(status, status.getDescription());
                                    decoyJob.setTenantId(tenantId);
                                    decoyJob.setOwner(tenantId + "@" + username);
                                    decoyJob.setSystem(tenantId + "-" + systemId);
                                    decoyJob.setLocalJobId(decoyJob.getUuid());
                                    decoyJob.setBatchQueue(queueName);
                                    JobDao.persist(decoyJob, false);
                                }
                            }

                            systemQueueJobSelection.put(
                                    tenantId + "-" + systemId + "#" + queueName, queueJobHits);
                            break;
                        }

                        systemJobSelections.put(tenantId + "-" + systemId, systemJobHits);
                        break;
                    }

                    userJobSelections.put(tenantId + "@" + username, userJobHits);
                    break;
                }

                tenantJobSelections.put(tenantId, tenantJobHits);
                break;
            }

            String nextJobUuid;
            for (JobStatusType testStatus : new JobStatusType[] { PENDING, STAGED }) {
                nextJobUuid = JobDao
                        .getNextQueuedJobUuid(testStatus, "someotherclient", null, null);
                Assert.assertNull(
                        nextJobUuid,
                        "getNextQueuedJobUuid with mismatched tenant id and empty usernames(s) & systemIds should always return null.");

                nextJobUuid = JobDao.getNextQueuedJobUuid(testStatus, "someotherclient",
                        new String[] { tenantIds[0] + "@" + usernames[0] }, null);
                Assert.assertNull(nextJobUuid,
                        "getNextQueuedJobUuid with mismatched tenant id and valid username(s) should always return null.");

                nextJobUuid = JobDao.getNextQueuedJobUuid(testStatus, "someotherclient",
                        new String[] { tenantIds[0] + "@" + usernames[0] },
                        new String[] { tenantIds[0] + "/" + systemsIds[0] });
                Assert.assertNull(
                        nextJobUuid,
                        "getNextQueuedJobUuid with mismatched tenant id and valid username and systemIds should always return null.");

                nextJobUuid = JobDao.getNextQueuedJobUuid(testStatus, "someotherclient",
                        new String[] { tenantIds[0] + "@" + usernames[0] },
                        new String[] { tenantIds[0] + "/" + systemsIds[0] + "#" + queues[0] });
                Assert.assertNull(
                        nextJobUuid,
                        "getNextQueuedJobUuid with mismatched tenant id and valid username, systemIds, and queues should always return null.");

                nextJobUuid = JobDao.getNextQueuedJobUuid(testStatus, null,
                        new String[] { "bullwinkle" }, null);
                Assert.assertNull(
                        nextJobUuid,
                        "getNextQueuedJobUuid with mismatched username and empty tenantId and systemIds should always return null.");

                nextJobUuid = JobDao.getNextQueuedJobUuid(testStatus, null,
                        new String[] { "bullwinkle" }, new String[] { tenantIds[0] + "/"
                                + systemsIds[0] + "#" + queues[0] });
                Assert.assertNull(
                        nextJobUuid,
                        "getNextQueuedJobUuid with mismatched tenant id and valid systemIds, and queues should always return null.");

                nextJobUuid = JobDao.getNextQueuedJobUuid(testStatus, null,
                        new String[] { "bullwinkle" }, new String[] { tenantIds[0] + "/"
                                + systemsIds[0] });
                Assert.assertNull(nextJobUuid,
                        "getNextQueuedJobUuid with mismatched tenant id and valid systemIds should always return null.");

                nextJobUuid = JobDao.getNextQueuedJobUuid(testStatus, tenantIds[0],
                        new String[] { "bullwinkle" }, null);
                Assert.assertNull(
                        nextJobUuid,
                        "getNextQueuedJobUuid with invalid user and valid tenant, system, and queue list should always return null.");

                nextJobUuid = JobDao.getNextQueuedJobUuid(testStatus, tenantIds[0],
                        new String[] { "bullwinkle" }, new String[] { tenantIds[0] + "/"
                                + systemsIds[0] + "#" + queues[0] });
                Assert.assertNull(
                        nextJobUuid,
                        "getNextQueuedJobUuid with invalid user and valid tenant, system, and queue list should always return null.");

                nextJobUuid = JobDao.getNextQueuedJobUuid(testStatus, tenantIds[0],
                        new String[] { "bullwinkle" }, new String[] { tenantIds[0] + "/"
                                + systemsIds[0] });
                Assert.assertNull(nextJobUuid,
                        "getNextQueuedJobUuid with invalid user and valid system list should always return null.");
            }
        } catch (Exception e) {
            Assert.fail("Unexpected error occurred running test for next queued job", e);
        } finally {
            try { clearJobs(); } catch (Exception e) {}
        }
    }

    @SuppressWarnings({ "unchecked" })
    @Test(dependsOnMethods = { "getNextQueuedJobUuidSelectsFailsOnMismatchedDependencies" })
    public void getNextQueuedJobUuidSelectsHonorsDedicatedParameters() {
        Hashtable<String, Hashtable<String, Integer>> tenantJobSelections = new Hashtable<String, Hashtable<String, Integer>>();
        Hashtable<String, Hashtable<String, Integer>> userJobSelections = new Hashtable<String, Hashtable<String, Integer>>();
        Hashtable<String, Hashtable<String, Integer>> systemJobSelections = new Hashtable<String, Hashtable<String, Integer>>();
        Hashtable<String, Hashtable<String, Integer>> systemQueueJobSelection = new Hashtable<String, Hashtable<String, Integer>>();
        Hashtable<String, Long> uuidSelections = new Hashtable<String, Long>();

        try {

            // initialize several jobs from each status
            Date created = new DateTime().minusMinutes(30).toDate();
            Date lastUpdated = new DateTime().minusSeconds(30).toDate();

            Job job = createJob(QUEUED);

            for (String tenantId : tenantIds) {
                Hashtable<String, Integer> tenantJobHits = new Hashtable<String, Integer>();

                for (String username : usernames) {
                    Hashtable<String, Integer> userJobHits = new Hashtable<String, Integer>();
                    for (String systemId : systemsIds) {
                        Hashtable<String, Integer> systemJobHits = new Hashtable<String, Integer>();
                        for (String queueName : queues) {
                            Hashtable<String, Integer> queueJobHits = new Hashtable<String, Integer>();

                            for (JobStatusType status : new JobStatusType[] { PENDING, STAGED }) {
                                Job testJob = job.copy();
                                testJob.setCreated(created);
                                testJob.setLastUpdated(lastUpdated);
                                testJob.setStatus(status, status.getDescription());
                                testJob.setTenantId(tenantId);
                                testJob.setOwner(tenantId + "@" + username);
                                testJob.setSystem(tenantId + "-" + systemId);
                                testJob.setLocalJobId(testJob.getUuid());
                                testJob.setBatchQueue(queueName);
                                JobDao.persist(testJob, false);

                                queueJobHits.put(testJob.getUuid(), 0);
                                tenantJobHits.put(testJob.getUuid(), 0);
                                systemJobHits.put(testJob.getUuid(), 0);
                                userJobHits.put(testJob.getUuid(), 0);
                                uuidSelections.put(testJob.getUuid(), (long) 0);
                            }

                            for (JobStatusType status : JobStatusType.values()) {
                                if (status != PENDING && status != STAGED) {
                                    Job decoyJob = job.copy();
                                    decoyJob.setCreated(created);
                                    decoyJob.setLastUpdated(lastUpdated);
                                    decoyJob.setStatus(status, status.getDescription());
                                    decoyJob.setTenantId(tenantId);
                                    decoyJob.setOwner(tenantId + "@" + username);
                                    decoyJob.setSystem(tenantId + "-" + systemId);
                                    decoyJob.setLocalJobId(decoyJob.getUuid());
                                    decoyJob.setBatchQueue(queueName);
                                    JobDao.persist(decoyJob, false);
                                }
                            }

                            systemQueueJobSelection.put(
                                    tenantId + "-" + systemId + "#" + queueName, queueJobHits);
                        }

                        systemJobSelections.put(tenantId + "-" + systemId, systemJobHits);
                    }

                    userJobSelections.put(tenantId + "@" + username, userJobHits);
                }

                tenantJobSelections.put(tenantId, tenantJobHits);
            }

            Job nextJob = null;
            String nextJobUuid;
            for (JobStatusType testStatus : new JobStatusType[] { PENDING, STAGED }) {
                for (final String tenantId : tenantIds) {

                    nextJobUuid = JobDao.getNextQueuedJobUuid(testStatus, tenantId, null, null);
                    nextJob = JobDao.getByUuid(nextJobUuid);
                    Assert.assertNotNull(
                            nextJob,
                            "getNextQueuedJobUuid of status "
                                    + testStatus.name()
                                    + " with specified tenant should never return null when valid jobs exist.");
                    Assert.assertEquals(
                            nextJob.getTenantId(),
                            tenantId,
                            "getNextQueuedJobUuid of status "
                                    + testStatus.name()
                                    + " with specified tenant should never return a job from another tenant.");
                    Assert.assertTrue(nextJob.getStatus() == PENDING
                            || nextJob.getStatus() == STAGED,
                            "Job with invalid status was returned");

                    Collection<List<String>> userPermutations = Collections2
                            .permutations(CollectionUtils.collect(Arrays.asList(usernames),
                                    new Transformer() {
                                        public String transform(Object val) {
                                            return tenantId + "@" + val;
                                        }
                                    }));

                    for (final List<String> userPermutation : userPermutations) {

                        nextJobUuid = JobDao.getNextQueuedJobUuid(testStatus, tenantId,
                                userPermutation.toArray(new String[] {}), null);
                        nextJob = JobDao.getByUuid(nextJobUuid);
                        Assert.assertNotNull(
                                nextJob,
                                "getNextQueuedJobUuid with specified tenant and user list should never return null when valid jobs exist.");
                        Assert.assertEquals(nextJob.getTenantId(), tenantId,
                                "getNextQueuedJobUuid with specified tenant should never return a job from another tenant.");
                        Assert.assertTrue(
                                userPermutation.contains(nextJob.getOwner()),
                                "getNextQueuedJobUuid with specified tenant and user list should never return a job from another user.");
                        Assert.assertTrue(nextJob.getStatus() == PENDING
                                || nextJob.getStatus() == STAGED,
                                "Job with invalid status was returned");

                        nextJobUuid = JobDao.getNextQueuedJobUuid(testStatus, null,
                                userPermutation.toArray(new String[] {}), null);
                        nextJob = JobDao.getByUuid(nextJobUuid);
                        Assert.assertNotNull(nextJob,
                                "getNextQueuedJobUuid with valid user list should never return null when valid values exist.");
                        Assert.assertTrue(
                                userPermutation.contains(nextJob.getOwner()),
                                "getNextQueuedJobUuid with specified tenant and user list should never return a job from another user.");
                        Assert.assertTrue(nextJob.getStatus() == PENDING
                                || nextJob.getStatus() == STAGED,
                                "Job with invalid status was returned");

                        Collection<List<String>> systemPermutations = Collections2
                                .permutations(CollectionUtils.collect(Arrays.asList(systemsIds),
                                        new Transformer() {
                                            public String transform(Object val) {
                                                return tenantId + "-" + val;
                                            }
                                        }));

                        for (final List<String> systemPermutation : systemPermutations) {

                            nextJobUuid = JobDao.getNextQueuedJobUuid(testStatus, null, null,
                                    systemPermutation.toArray(new String[] {}));
                            nextJob = JobDao.getByUuid(nextJobUuid);
                            Assert.assertNotNull(
                                    nextJob,
                                    "getNextQueuedJobUuid with valid tenant to valid user, system, and queue list should never rturn null when valid values exist.");
                            Assert.assertTrue(
                                    systemPermutation.contains(nextJob.getSystem()),
                                    "getNextQueuedJobUuid with specified tenant, user, and system list should never return a job from another system.");
                            Assert.assertTrue(nextJob.getStatus() == PENDING
                                    || nextJob.getStatus() == STAGED,
                                    "Job with invalid status was returned");

                            nextJobUuid = JobDao.getNextQueuedJobUuid(testStatus, null,
                                    userPermutation.toArray(new String[] {}),
                                    systemPermutation.toArray(new String[] {}));
                            nextJob = JobDao.getByUuid(nextJobUuid);
                            Assert.assertNotNull(
                                    nextJob,
                                    "getNextQueuedJobUuid with valid user, system, and queue list should never return null when valid values exist.");
                            Assert.assertTrue(
                                    userPermutation.contains(nextJob.getOwner()),
                                    "getNextQueuedJobUuid with specified tenant and user list should never return a job from another user.");
                            Assert.assertTrue(
                                    systemPermutation.contains(nextJob.getSystem()),
                                    "getNextQueuedJobUuid with specified tenant, user, and system list should never return a job from another system.");
                            Assert.assertTrue(nextJob.getStatus() == PENDING
                                    || nextJob.getStatus() == STAGED,
                                    "Job with invalid status was returned");

                            nextJobUuid = JobDao.getNextQueuedJobUuid(testStatus, tenantId,
                                    userPermutation.toArray(new String[] {}),
                                    systemPermutation.toArray(new String[] {}));
                            nextJob = JobDao.getByUuid(nextJobUuid);
                            Assert.assertNotNull(
                                    nextJob,
                                    "getNextQueuedJobUuid with specified tenant, user, and system list should never return null when valid jobs exist.");
                            Assert.assertEquals(
                                    nextJob.getTenantId(),
                                    tenantId,
                                    "getNextQueuedJobUuid with specified tenant, user, and system should never return a job from another tenant.");
                            Assert.assertTrue(
                                    userPermutation.contains(nextJob.getOwner()),
                                    "getNextQueuedJobUuid with specified tenant, user, and system list should never return a job from another user.");
                            Assert.assertTrue(
                                    systemPermutation.contains(nextJob.getSystem()),
                                    "getNextQueuedJobUuid with specified tenant, user, and system list should never return a job from another system.");
                        }

                        List<String> systemQueueFQNs = new ArrayList<String>();
                        systemQueueFQNs.add(tenantId + "-" + systemsIds[0] + "#" + queues[0]);
                        systemQueueFQNs.add(tenantId + "-" + systemsIds[1] + "#" + queues[2]);
                        systemQueueFQNs.add(tenantId + "-" + systemsIds[1] + "#" + queues[0]);
                        systemQueueFQNs.add(tenantId + "-" + systemsIds[2] + "#" + queues[1]);

                        // iterate over all
                        Collection<List<String>> systemAndQueuePermutations = Collections2
                                .permutations(systemQueueFQNs);

                        for (final List<String> systemAndQueuePermutation : systemAndQueuePermutations) {
                            nextJobUuid = JobDao.getNextQueuedJobUuid(testStatus, null, null,
                                    systemAndQueuePermutation.toArray(new String[] {}));
                            nextJob = JobDao.getByUuid(nextJobUuid);
                            Assert.assertNotNull(
                                    nextJob,
                                    "getNextQueuedJobUuid with valid tenant to valid user, system, and queue list should never rturn null when valid values exist.");
                            Assert.assertTrue(
                                    systemAndQueuePermutation.contains(nextJob.getSystem() + "#"
                                            + nextJob.getBatchQueue()),
                                    "getNextQueuedJobUuid with specified tenant, user, and system list should never return a job from another system.");
                            Assert.assertTrue(nextJob.getStatus() == PENDING
                                    || nextJob.getStatus() == STAGED,
                                    "Job with invalid status was returned");

                            nextJobUuid = JobDao.getNextQueuedJobUuid(testStatus, null,
                                    userPermutation.toArray(new String[] {}),
                                    systemAndQueuePermutation.toArray(new String[] {}));
                            nextJob = JobDao.getByUuid(nextJobUuid);
                            Assert.assertNotNull(
                                    nextJob,
                                    "getNextQueuedJobUuid with valid user, system, and queue list should never return null when valid values exist.");
                            Assert.assertTrue(
                                    userPermutation.contains(nextJob.getOwner()),
                                    "getNextQueuedJobUuid with specified tenant and user list should never return a job from another user.");
                            Assert.assertTrue(
                                    systemAndQueuePermutation.contains(nextJob.getSystem() + "#"
                                            + nextJob.getBatchQueue()),
                                    "getNextQueuedJobUuid with specified tenant, user, and system list should never return a job from another system.");
                            Assert.assertTrue(nextJob.getStatus() == PENDING
                                    || nextJob.getStatus() == STAGED,
                                    "Job with invalid status was returned");

                            nextJobUuid = JobDao.getNextQueuedJobUuid(testStatus, tenantId,
                                    userPermutation.toArray(new String[] {}),
                                    systemAndQueuePermutation.toArray(new String[] {}));
                            nextJob = JobDao.getByUuid(nextJobUuid);
                            Assert.assertNotNull(
                                    nextJob,
                                    "getNextQueuedJobUuid with specified tenant, user, system, and queue list should never return null when valid jobs exist.");
                            Assert.assertEquals(
                                    nextJob.getTenantId(),
                                    tenantId,
                                    "getNextQueuedJobUuid with specified tenant, user, system, and queue should never return a job from another tenant.");
                            Assert.assertTrue(
                                    userPermutation.contains(nextJob.getOwner()),
                                    "getNextQueuedJobUuid with specified tenant, user, system, and queue list should never return a job from another user.");
                            Assert.assertTrue(
                                    systemAndQueuePermutation.contains(nextJob.getSystem() + "#"
                                            + nextJob.getBatchQueue()),
                                    "getNextQueuedJobUuid with specified tenant, user, system, and queue list should never return a job from another system.");
                            Assert.assertTrue(nextJob.getStatus() == PENDING
                                    || nextJob.getStatus() == STAGED,
                                    "Job with invalid status was returned");
                        }
                        break;
                    }
                    break;
                }
                break;
            }
        } catch (Exception e) {
            Assert.fail(
                    "Unexpected error occurred running test for next running job with status QUEUED",
                    e);
        } finally {
            try { clearJobs(); } catch (Exception e) {}
        }
    }

}
