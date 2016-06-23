package org.iplantc.service.apps.model;

import java.util.Calendar;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.iplantc.service.apps.exceptions.SoftwareException;
import org.iplantc.service.apps.model.enumerations.ParallelismType;
import org.iplantc.service.apps.util.DomainSetup;
import org.iplantc.service.apps.util.JndiSetup;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.model.BatchQueue;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.iplantc.service.systems.model.enumerations.ExecutionType;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class GSoftwareTest  extends GModelTestCommon {
    //public Settings settings = new Settings();
    public Software software;
    public JSONObject json;


    @BeforeClass
    public void setUp() throws Exception {
        super.setUp();
        JndiSetup.doSetup("iplant-io");
        DomainSetup dm = new DomainSetup();
        dm.fillListsMaps();
        dm.persistDefaultTestSystemsData();
        	
    }

    @DataProvider(name = "json data")
    public Object[][] createData1() {
        Object[][] testData = jtd.genJsonTestMessages();
        return testData;
    }

    @Test (groups="model", dataProvider="json data")
    public void testFromJSON(String name, Object changeValue, String message, boolean expectExceptionThrown) 
    throws Exception 
    {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = (ObjectNode)mapper.readTree(jsonTreeSoftwareBase.toString());
        
        //change element name and value "$name and $changeValue"
        if (changeValue == null) {
        	node.putNull(name);
        } else if (changeValue instanceof List) {
        	ArrayNode changeArray = mapper.createArrayNode();
        	for(Object o: (List<?>)changeValue) {
        		if (o instanceof List) {
        			changeArray.addArray();
        		} else if (o instanceof Integer) {
        			changeArray.add((Integer)o);
                } else if (o instanceof Float) {
                	changeArray.add((Float)o);
                } else if (o instanceof Boolean) {
                	changeArray.add((Boolean)o);
                } else if (o instanceof String) {
                	changeArray.add((String)o);
                } else {
                	changeArray.addObject();
                }
        	}
        	node.set(name, changeArray);
        } else if (changeValue instanceof Integer) {
        	node.put(name, (Integer)changeValue);
        } else if (changeValue instanceof Long) {
        	node.put(name, (Long)changeValue);
        } else if (changeValue instanceof Float) {
            node.put(name, (Float)changeValue);
        } else if (changeValue instanceof Boolean) {
        	node.put(name, (Boolean)changeValue);
        } else if (changeValue instanceof String) {
        	node.put(name, (String)changeValue);
        } else {
        	node.putObject(name);
        }
        

        boolean exceptionOccurred = false;
        String exceptionMsg = message;

        try{
            Software.fromJSON(new JSONObject(mapper.writeValueAsString(node)), JSONTestDataUtil.TEST_OWNER);
        }catch(SoftwareException se){
            exceptionOccurred = true;
            exceptionMsg = "Invalid iPlant JSON submitted, attribute " + name + " " + message + " \n\"" + name + "\" = \"" + changeValue + "\"\n" + se.getMessage();
        }catch(JSONException je){
            exceptionOccurred  = true;
            exceptionMsg = "Malformed JSON parsing exception attribute " + name + " " + message + " \n\"" + name + "\" = \"" + changeValue + "\"\n" + je.getMessage();
            if (!expectExceptionThrown)
                je.printStackTrace();
        }

        Assert.assertEquals(exceptionOccurred, expectExceptionThrown, exceptionMsg);
    }
    
//    private BatchQueue createBatchQueue(int maxNodes, int maxMemoryPerNode, int maxProcessorsPerNode,
//			String maxRequestedTime, boolean systemDefault) 
//    {
//		BatchQueue batchQueue = new BatchQueue();
//		batchQueue.setName("sanitycheckqueue");
//		batchQueue.setMaxJobs((long)-1);
//		batchQueue.setMaxUserJobs((long)-1);
//		batchQueue.setMaxNodes((long)maxNodes);
//		batchQueue.setMaxMemoryPerNode((long)maxMemoryPerNode);
//		batchQueue.setMaxProcessorsPerNode((long)maxProcessorsPerNode);
//		batchQueue.setMaxRequestedTime(maxRequestedTime);
//		batchQueue.setSystemDefault(systemDefault);
//	}

    @DataProvider
    public Object[][] defaultProcessorsSanityChecksProvider() 
    {
    	String batchQueueNodesField = "maxNodes";
    	String softwareNodesField = "defaultNodeCount";
    	
    	String batchQueueProcessorsField = "maxProcessorsPerNode";
    	String softwareProcessorsField = "defaultProcessorsPerNode";
    	
    	String batchQueueMemoryField = "maxMemoryPerNode";
    	String softwareMemoryField = "defaultMemoryPerNode";
    	
    	return new Object[][] {
    			{ batchQueueNodesField, null, softwareNodesField, null, false, "software." + softwareNodesField + " can be null when queue." + batchQueueNodesField + " is null", false },
    			{ batchQueueNodesField, null, softwareNodesField, new Long(-1), false, "software." + softwareNodesField + " cannot be negative when queue." + batchQueueNodesField + " is null", true },
    			{ batchQueueNodesField, null, softwareNodesField, new Long(0), false, "software." + softwareNodesField + " can not be 0 when queue." + batchQueueNodesField + " is null", true },
    			{ batchQueueNodesField, null, softwareNodesField, new Long(1), false, "software." + softwareNodesField + " can be any positive integer when queue." + batchQueueNodesField + " is null", false },
    			
    			{ batchQueueNodesField, new Long(-1), softwareNodesField, null, false, "software." + softwareNodesField + " can be null when queue." + batchQueueNodesField + " is -1", false },
    			{ batchQueueNodesField, new Long(-1), softwareNodesField, new Long(-1), false, "software." + softwareNodesField + " cannot be negative when queue." + batchQueueNodesField + " is -1", true },
    			{ batchQueueNodesField, new Long(-1), softwareNodesField, new Long(0), false, "software." + softwareNodesField + " can not be 0 when queue." + batchQueueNodesField + " is -1", true },
    			{ batchQueueNodesField, new Long(-1), softwareNodesField, new Long(1), false, "software." + softwareNodesField + " can be any positive integer when queue." + batchQueueNodesField + " = -1", false },
    			
    			{ batchQueueNodesField, new Long(1), softwareNodesField, null, false, "software." + softwareNodesField + " can be null when queue." + batchQueueNodesField + " is 1", false },
    			{ batchQueueNodesField, new Long(1), softwareNodesField, new Long(-1), false, "software." + softwareNodesField + " cannot be negative when queue." + batchQueueNodesField + " is 1", true },
    			{ batchQueueNodesField, new Long(1), softwareNodesField, new Long(1), false, "software." + softwareNodesField + " can be equal to queue." + batchQueueNodesField + "", false },
    			{ batchQueueNodesField, new Long(1), softwareNodesField, new Long(0), false, "software." + softwareNodesField + " can not be 0", true },
    			{ batchQueueNodesField, new Long(1), softwareNodesField, new Long(2), false, "software." + softwareNodesField + " cannot be greater than queue." + batchQueueNodesField + "", true },
    			
    			
    			{ batchQueueProcessorsField, null, softwareProcessorsField, null, false, "software." + softwareProcessorsField + " can be null when queue." + batchQueueProcessorsField + " is null", false },
    			{ batchQueueProcessorsField, null, softwareProcessorsField, new Long(-1), false, "software." + softwareProcessorsField + " cannot be negative when queue." + batchQueueProcessorsField + " is null", true },
    			{ batchQueueProcessorsField, null, softwareProcessorsField, new Long(0), false, "software." + softwareProcessorsField + " can not be 0 when queue." + batchQueueProcessorsField + " is null", true },
    			{ batchQueueProcessorsField, null, softwareProcessorsField, new Long(1), false, "software." + softwareProcessorsField + " can be any positive integer when queue." + batchQueueProcessorsField + " is null", false },
    			
    			{ batchQueueProcessorsField, new Long(-1), softwareProcessorsField, null, false, "software." + softwareProcessorsField + " can be null when queue." + batchQueueProcessorsField + " is -1", false },
    			{ batchQueueProcessorsField, new Long(-1), softwareProcessorsField, new Long(-1), false, "software." + softwareProcessorsField + " cannot be negative when queue." + batchQueueProcessorsField + " is -1", true },
    			{ batchQueueProcessorsField, new Long(-1), softwareProcessorsField, new Long(0), false, "software." + softwareProcessorsField + " can not be 0 when queue." + batchQueueProcessorsField + " is -1", true },
    			{ batchQueueProcessorsField, new Long(-1), softwareProcessorsField, new Long(1), false, "software." + softwareProcessorsField + " can be any positive integer when queue." + batchQueueProcessorsField + " = -1", false },
    			
    			{ batchQueueProcessorsField, new Long(1), softwareProcessorsField, null, false, "software." + softwareProcessorsField + " can be null when queue." + batchQueueProcessorsField + " is 1", false },
    			{ batchQueueProcessorsField, new Long(1), softwareProcessorsField, new Long(-1), false, "software." + softwareProcessorsField + " cannot be negative when queue." + batchQueueProcessorsField + " is 1", true },
    			{ batchQueueProcessorsField, new Long(1), softwareProcessorsField, new Long(1), false, "software." + softwareProcessorsField + " can be equal to queue." + batchQueueProcessorsField + "", false },
    			{ batchQueueProcessorsField, new Long(1), softwareProcessorsField, new Long(0), false, "software." + softwareProcessorsField + " can not be 0", true },
    			{ batchQueueProcessorsField, new Long(1), softwareProcessorsField, new Long(2), false, "software." + softwareProcessorsField + " cannot be greater than queue." + batchQueueProcessorsField + "", true },
    			
    			
    			{ batchQueueMemoryField, null, softwareMemoryField, null, false, "software." + softwareMemoryField + " can be null when queue." + batchQueueMemoryField + " is null", false },
    			{ batchQueueMemoryField, null, softwareMemoryField, new Long(-1), false, "software." + softwareMemoryField + " cannot be negative when queue." + batchQueueMemoryField + " is null", true },
    			{ batchQueueMemoryField, null, softwareMemoryField, new Long(0), false, "software." + softwareMemoryField + " can not be 0 when queue." + batchQueueMemoryField + " is null", true },
    			{ batchQueueMemoryField, null, softwareMemoryField, new Long(1), false, "software." + softwareMemoryField + " can be any positive integer when queue." + batchQueueMemoryField + " is null", false },
    			
    			{ batchQueueMemoryField, "-1GB", softwareMemoryField, null, false, "software." + softwareMemoryField + " can be null when queue." + batchQueueMemoryField + " is -1", false },
    			{ batchQueueMemoryField, "-1GB", softwareMemoryField, new Long(-1), false, "software." + softwareMemoryField + " cannot be negative when queue." + batchQueueMemoryField + " is -1", true },
    			{ batchQueueMemoryField, "-1GB", softwareMemoryField, new Long(0), false, "software." + softwareMemoryField + " can not be 0 when queue." + batchQueueMemoryField + " is -1", true },
    			{ batchQueueMemoryField, "-1GB", softwareMemoryField, new Long(1), false, "software." + softwareMemoryField + " can be any positive integer when queue." + batchQueueMemoryField + " = -1", false },
    			
    			{ batchQueueMemoryField, "1GB", softwareMemoryField, null, false, "software." + softwareMemoryField + " can be null when queue." + batchQueueMemoryField + " is 1", false },
    			{ batchQueueMemoryField, "1GB", softwareMemoryField, new Long(-1), false, "software." + softwareMemoryField + " cannot be negative when queue." + batchQueueMemoryField + " is 1", true },
    			{ batchQueueMemoryField, "1GB", softwareMemoryField, "1GB", false, "software." + softwareMemoryField + " can be equal to queue." + batchQueueMemoryField + "", false },
    			{ batchQueueMemoryField, "1GB", softwareMemoryField, "0GB", false, "software." + softwareMemoryField + " can not be 0", true },
    			{ batchQueueMemoryField, "1GB", softwareMemoryField, "2GB", false, "software." + softwareMemoryField + " cannot be greater than queue." + batchQueueMemoryField + "", true },
    	};
    }
    
    
    /**
     * Tests that the software default parameters used in job submission have the proper sanity checking against
     * the batch queue max values.
     *  
     * @param batchQueueMaxProcessorsPerNode
     * @param softwareDefaultProcessorsPerNode
     * @param useDefaultQueue
     * @param message
     * @param expectExceptionThrown
     * @throws Exception
     */
    @Test (groups="model", dataProvider="defaultProcessorsSanityChecksProvider")
    private void defaultDefaultParametersSanityChecks(String batchQueueField,
    										  Object batchQueueValue, 
    										  String softwareField,
    										  Object softwareValue, 
    										  boolean useDefaultQueue, 
    										  String message, 
    										  boolean expectExceptionThrown)
	throws Exception 
    {
    	SystemDao systemDao = new SystemDao();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = (ObjectNode)mapper.readTree(jsonTreeSoftwareBase.toString());
        ExecutionSystem executionSystem = null;
        
        boolean exceptionOccurred = false;
        String exceptionMsg = message;
        try 
        {
        	// create a new system for these tests
        	JSONObject executionJson = jtd.getTestDataObject("target/test-classes/systems/execution/execute.example.com.json");
        	
        	executionSystem = (ExecutionSystem) systemDao.findBySystemId("sanitychecksystem");
        	if (executionSystem != null) 
        	{
        		systemDao.remove(executionSystem);
        	}
        	
        	executionSystem = ExecutionSystem.fromJSON(executionJson);
        	executionSystem.setSystemId("sanitychecksystem");
        	executionSystem.setOwner(JSONTestDataUtil.TEST_OWNER);
        	executionSystem.getBatchQueues().clear();
    	
	        JSONObject jsonQueue = executionJson.getJSONArray("queues").getJSONObject(0);
        	jsonQueue.put("name", "sanitycheckqueue");
        	jsonQueue.put("defaultNodes", -1);
        	jsonQueue.put("defaultProcessorsPerNode", -1);
        	jsonQueue.put("defaultMemoryPerNode", -1);
        	jsonQueue.put("defaultNodes", -1);
        	jsonQueue.put("default", useDefaultQueue);
        	jsonQueue.put(batchQueueField, batchQueueValue);
        	
        	BatchQueue testQueue = BatchQueue.fromJSON(jsonQueue);
        	executionSystem.addBatchQueue(testQueue);
        	systemDao.persist(executionSystem);
        	
            
            if (!useDefaultQueue) {
            	node.put("defaultQueue", testQueue.getName());
            }
            node.put("executionSystem", executionSystem.getSystemId());
            node.remove("executionType");
            node.put("executionType", executionSystem.getExecutionType().name());
            if (softwareValue instanceof Long)
            	node.put(softwareField, (Long)softwareValue);
            else 
            	node.put(softwareField, (String)softwareValue);
            //node.put("defaultProcessorsPerNode", softwareDefaultProcessorsPerNode);
            
	        Software.fromJSON(new JSONObject(mapper.writeValueAsString(node)), JSONTestDataUtil.TEST_OWNER);
        }
        catch(SoftwareException e)
        {
            exceptionOccurred = true;
            exceptionMsg = "Invalid iPlant JSON submitted, attribute defaultProcessorsPerNode " + message + " \n"
            		+ "\"app." + softwareField + "\" = \"" + softwareValue + "\"\n"
            		+ "\"executionSystem.queue." + batchQueueField + "\" = \"" + batchQueueValue + "\"\n" 
            		+ e.getMessage();
            
        }
        catch(JSONException e)
        {
            exceptionOccurred  = true;
            exceptionMsg = "Malformed JSON parsing exception attribute defaultProcessorsPerNode " + message + " \n"
            		+ "\"app." + softwareField + "\" = \"" + softwareValue + "\"\n"
            		+ "\"executionSystem.queue." + batchQueueField + "\" = \"" + batchQueueValue + "\"\n" 
            		+ e.getMessage();
            if (!expectExceptionThrown)
                e.printStackTrace();
        }
        finally {
        	try { systemDao.remove(executionSystem); } catch (Throwable e) {}
        }

        Assert.assertEquals(exceptionOccurred, expectExceptionThrown, exceptionMsg);
    }
    
    @Test(groups="model")
    public void cloneCopiesSoftwareAttributes() 
    throws Exception 
    {
        try {
            StorageSystem storageSystem = new StorageSystem();
            storageSystem.setSystemId("originalStorageSystem");
            storageSystem.setId(new Long(1));
            
            ExecutionSystem executionSystem = new ExecutionSystem();
            executionSystem.setSystemId("originalExecutionSystem");
            executionSystem.setId(new Long(3));
            
            // set fields to positive values to ensure we do not get false positives during clone.
            Software software = new Software();
            software.setId(new Long(1));
            software.setAvailable(true);
            software.setCheckpointable(true);
            software.setChecksum("12348567asdfaf223");
            software.setDefaultMaxRunTime("09:09:09");
            software.setDefaultMemoryPerNode(25.5);
            software.setDefaultNodes(new Long(2));
            software.setDefaultProcessorsPerNode(new Long(12));
            software.setDefaultQueue("abnormal");
            software.setDeploymentPath("/dev/null");
            software.setExecutablePath("foo");
            software.setExecutionSystem(executionSystem);
            software.setExecutionType(ExecutionType.CLI);
            software.setHelpURI("http://example.com");
            software.setIcon("http://example.com/icon");
            software.setLabel("testapp");
            software.setLongDescription("some long description about the test description");
            software.setModules("purge,load,magic,miracle,profit");
            software.setName("testapp");
            software.setOntology(new JSONArray().put("xs:string").put("xs:foo").toString());
            software.setOwner(JSONTestDataUtil.TEST_OWNER);
            software.setParallelism(ParallelismType.SERIAL);
            software.setPubliclyAvailable(true);
            software.setRevisionCount(999);
            software.setShortDescription("Shorter app description");
            software.setStorageSystem(storageSystem);
            software.setTags("foo,bat,bar");
            software.setTenantId("foo.dat");
            software.setTestPath("test/foo");
            software.setCreated(new DateTime().minusDays(20).toDate());
            software.setLastUpdated(new DateTime().minusDays(10).toDate());
            software.setVersion("22.22.22");
            
            // run the clone
            Software clonedSoftware = software.clone();
            
            // validate every field was copied
            Assert.assertEquals(clonedSoftware.isAvailable(), software.isAvailable(), "Argument attribute was not cloned from one software to another");
            Assert.assertEquals(clonedSoftware.isCheckpointable(), software.isCheckpointable(), "checkpointable attribute was not cloned from one software to another");
            Assert.assertEquals(clonedSoftware.getLongDescription(), software.getLongDescription(), "long description attribute was not cloned from one software to another");
            Assert.assertEquals(clonedSoftware.getShortDescription(), software.getShortDescription(), "short description attribute was not cloned from one software to another");
            Assert.assertNull(clonedSoftware.getChecksum(), "checksum attribute should be set to null when cloning one software to another");
            Assert.assertNull(clonedSoftware.getId(), "id attribute should not be cloned from one software to another");
            Assert.assertEquals(clonedSoftware.getDefaultMaxRunTime(), software.getDefaultMaxRunTime(), "defaultMaxRunTime attribute was not cloned from one software to another");
            Assert.assertEquals(clonedSoftware.getDefaultMemoryPerNode(), software.getDefaultMemoryPerNode(), "defaultMemoryPerNode attribute was not cloned from one software to another");
            Assert.assertEquals(clonedSoftware.getDefaultNodes(), software.getDefaultNodes(), "defaultNodes attribute was not cloned from one software to another");
            Assert.assertEquals(clonedSoftware.getDefaultProcessorsPerNode(), software.getDefaultProcessorsPerNode(), "defaultProcessorsPerNode attribute was not cloned from one software to another");
            Assert.assertEquals(clonedSoftware.getDeploymentPath(), software.getDeploymentPath(), "deploymentPath attribute was not cloned from one software to another");
            Assert.assertTrue(CollectionUtils.isEqualCollection(clonedSoftware.getOntologyAsList(), software.getOntologyAsList()), "ontology attribute was not cloned from one software to another");
            Assert.assertEquals(clonedSoftware.getExecutablePath(), software.getExecutablePath(), "executablePath attribute was not cloned from one software to another");
            Assert.assertNotNull(clonedSoftware.getExecutionSystem(), "executionSystem attribute should be cloned from one software to another");
            Assert.assertEquals(clonedSoftware.getExecutionSystem().getId(), executionSystem.getId(), "executionSystem reference should be identical to the original after cloning from one software to another");
            
            Assert.assertEquals(clonedSoftware.getExecutionType(), software.getExecutionType(), "executionType attribute was not cloned from one software to another");
            Assert.assertEquals(clonedSoftware.getIcon(), software.getIcon(), "icon attribute was not cloned from one software to another");
            Assert.assertEquals(clonedSoftware.getLabel(), software.getLabel(), "label attribute was not cloned from one software to another");
            Assert.assertNotNull(clonedSoftware.getStorageSystem(), "storageSystem attribute was not cloned from one software to another");
            Assert.assertEquals(clonedSoftware.getStorageSystem().getId(), storageSystem.getId(), "Wrong storage system reference was not cloned from one software to another");
            
            Assert.assertEquals(clonedSoftware.getLongDescription(), software.getLongDescription(), "longDescription attribute was not cloned from one software to another");
            Assert.assertTrue(CollectionUtils.isEqualCollection(clonedSoftware.getModulesAsList(), software.getModulesAsList()), "modules attribute was not cloned from one software to another");
            Assert.assertEquals(clonedSoftware.getLongDescription(), software.getLongDescription(), "longDescription attribute was not cloned from one software to another");
            Assert.assertEquals(clonedSoftware.getName(), software.getName(), "name attribute was not cloned from one software to another");
            Assert.assertEquals(clonedSoftware.getOwner(), software.getOwner(), "owner attribute was not cloned from one software to another");
            Assert.assertEquals(clonedSoftware.getParallelism(), software.getParallelism(), "parallelism attribute was not cloned from one software to another");
            Assert.assertFalse(clonedSoftware.isPubliclyAvailable(), "publiclyAvailable should always be false after cloning one software to another");
            Assert.assertEquals(clonedSoftware.getShortDescription(), software.getShortDescription(), "shortDescription attribute was not cloned from one software to another");
            Assert.assertTrue(CollectionUtils.isEqualCollection(clonedSoftware.getTagsAsList(), software.getTagsAsList()), "tags attribute was not cloned from one software to another");
            Assert.assertEquals(clonedSoftware.getTenantId(), software.getTenantId(), "tenantId attribute was not cloned from one software to another");
            Assert.assertEquals(clonedSoftware.getTestPath(), software.getTestPath(), "testPath attribute was not cloned from one software to another");
            Assert.assertNotEquals(clonedSoftware.getUuid(), software.getUuid(), "uuid attribute was not cloned from one software to another");
            Assert.assertNotEquals(clonedSoftware.getLastUpdated().getTime(), software.getLastUpdated().getTime(), "lastUpdated attribute should not be cloned from one software to another");
            Assert.assertNotEquals(clonedSoftware.getCreated().getTime(), software.getCreated().getTime(), "created attribute should not be cloned from one software to another");
            Assert.assertEquals(clonedSoftware.getVersion(), software.getVersion(), "version attribute should be cloned from one software to another");
        }
        catch (Exception e) {
            Assert.fail("Cloning software should not throw exception",e);
        }
    }
    
    @AfterClass
    public void tearDown() throws Exception {

    }
}



/*

    @DataProvider(name = "json array data")
    public Object[][] createData2() {
        Object[][] testData = jtd.genJsonTestArrayMessages()
        return testData
    }


    @Test (dataProvider="json array data")
    public void testFromJSONArrays(String name, JSONArray changeValue, String message, boolean exceptionThrown) throws Exception {
        json = changeValue
        def exceptionFlag = false
        def exceptionMsg = message

        try{
            Software.fromJSON(json)
        }catch(SoftwareException se){
            exceptionFlag = true
            exceptionMsg = "Invalid iPlant JSON submitted, label ${name} ${message} "
        }catch(JSONException je){
            exceptionFlag = true
            exceptionMsg = "Malformed JSON parsing exception label ${name} ${message} "
        }
        println "$name change exception thrown?  expected $exceptionThrown actual $exceptionFlag"
        Assert.assertTrue(exceptionFlag == exceptionThrown, exceptionMsg)
    }

*/
