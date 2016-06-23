package org.iplantc.service.jobs.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.jobs.dao.AbstractDaoTest;
import org.iplantc.service.jobs.dao.JobDao;
import org.iplantc.service.jobs.dao.JobDaoTest;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.model.JSONTestDataUtil;
import org.iplantc.service.jobs.model.Job;
import org.iplantc.service.jobs.model.enumerations.JobStatusType;
import org.iplantc.service.systems.dao.SystemDao;
import org.iplantc.service.systems.exceptions.SystemArgumentException;
import org.iplantc.service.systems.model.ExecutionSystem;
import org.iplantc.service.systems.model.RemoteSystem;
import org.iplantc.service.systems.model.StorageSystem;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: wcs
 * Date: 4/24/13
 * Time: 8:24 AM
 * To change this template use File | Settings | File Templates.
 */
public class DomainSetup extends AbstractDaoTest
{
	private static final String SOFTWARE_OWNER =       "api_sample_user";       // default software owner if none given
    private static final String SYSTEM_OWNER =         "sysowner";      // default system owner
    
    private List<JSONObject> jsonStorageList;                           // list of base Storage objects as json
                                                                        // created from template directory
    private List<JSONObject> jsonExecutionList;                         // list of base Execution objects as json
                                                                        // created from template directory
    private List<JSONObject> jsonSoftwareList;
    private Map<String,Software> softwareMap = new HashMap<String, Software>();
    private Map<String, ExecutionSystem> executionSystemMap = new HashMap<String, ExecutionSystem>();
    private Map<String, StorageSystem> storageSystemMap = new HashMap<String, StorageSystem>();
    private SystemDao systemDao = new SystemDao();
    
    public Map<String, Software> getSoftwareMap() {
        return softwareMap;
    }

    public Map<String, ExecutionSystem> getExecutionSystemMap() {
        return executionSystemMap;
    }

    public Map<String, StorageSystem> getStorageSystemMap() {
        return storageSystemMap;
    }

    /*
     *                                                      *
     * Methods to handle json file retrieval                *
     *                                                      *
     */

    /**
     * Turn a file with json content into a JSONObject
     * @param pathToFile path to the file with json content
     * @return JSONObject from a file with json content
     */
    public JSONObject retrieveDataFile(String pathToFile){
        JSONObject json = null;
        try {
            String contents = FileUtils.readFileToString(new File(pathToFile));
            json = new JSONObject(contents);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    /**
     * Turn a file with json content into a String
     * @param pathToFile path to the file with json content
     * @return String from a file with json content
     */
    public String retrieveDataFileAsString(String pathToFile){
        String contents = null;
        try {
            contents = FileUtils.readFileToString(new File(pathToFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contents;
    }

    /**
     * Write a file out to filesystem
     * @param file  File to write out
     * @param content  String content
     * @return  true for success
     */
    public boolean writeDataFile(File file, String content){
        try {
            FileUtils.writeStringToFile(file,content);
        } catch (IOException e) {
            String name = file.getName();
            System.out.println("failed writing "+name+" to file");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Write a file out to filesystem
     * @param pathToFile path to file
     * @param content    contents to write
     * @return  true if successful
     */
    public boolean writeDataFile(String pathToFile,String content){
        File file = FileUtils.getFile(pathToFile);
        return writeDataFile(file,content);
    }

    /*
     *                                                                      *
     * Methods to load up lists and maps of json objects and domain objects *
     *                                                                      *
     */

    /**
     * Given a list of files with json content create a list of JsonObjects
     * @param files
     * @return List of JSONObjects from a list of with File types
     * @throws java.io.IOException
     * @throws org.json.JSONException
     */
    private List<JSONObject> listOfJsonObjects(List<File> files) throws IOException, JSONException {
        List<JSONObject> jsonObjects = new ArrayList<JSONObject>();

        for(File file : files){
            jsonObjects.add(jtd.getTestDataObject(file));
        }
        return jsonObjects;
    }

    /**
     * Create a list of JSONObjects given a directory with json text files
     * @param directory root directory where files are kept
     * @return  a list of JSONObjects
     * @throws java.io.IOException
     * @throws org.json.JSONException
     */
    private List<JSONObject> collectFiles(String directory) throws IOException, JSONException {
        List<File> fileList;
        File dir = new File(directory);
        fileList = (List<File>) FileUtils.listFiles(dir, new String[]{"json"}, false);
        return listOfJsonObjects(fileList);
    }

    /**
     * turn all the domain object json lists into domain object maps for keyed access
     */
    public void fillSystemMaps(){

        for(JSONObject json : jsonStorageList){
            try {
                String key = (String)json.get("id");
                storageSystemMap.put(key, StorageSystem.fromJSON(json));
            } catch (SystemArgumentException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        for(JSONObject json : jsonExecutionList){
            try {
                String key = (String)json.get("id");
                executionSystemMap.put(key, ExecutionSystem.fromJSON(json));
            } catch (SystemArgumentException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * default to load the files found in the storage, execution and software template directories
     * making lists of json objects and maps of domain objects. Maps of system domain objects an be used
     * to change values before persistence
     *
     */
    public void fillListsMaps(){
        jtd = JSONTestDataUtil.getInstance();
        try {
            jsonStorageList = collectFiles(STORAGE_SYSTEM_TEMPLATE_DIR);
            jsonExecutionList = collectFiles(EXECUTION_SYSTEM_TEMPLATE_DIR);
            jsonSoftwareList = collectFiles(SOFTWARE_SYSTEM_TEMPLATE_DIR);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        fillSystemMaps();
    }

    /**
     * This will fill up a list of Software domain objects that can be used for testing. They are not
     * persisted as yet but require that base systems data is in place in the database.
     *
     */
    public void fillSoftwareMap(){
        // this requires base system data in database to work
        for(JSONObject json : jsonSoftwareList){
            try {
                String key = (String)json.get("id");
                // must be able read remote system from db to create a software object.
                System.out.println(key);
                softwareMap.put(key, Software.fromJSON(json,SOFTWARE_OWNER));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /*
     *                                                      *
     * Methods to create single domain objects from json    *
     *                                                      *
     */

    /**
     * Create a Software object from json object
     * @param json software json
     * @return Software object
     */
    public Software createSoftwareFromJson(JSONObject json){
        Software software = null;
        try {
            software = Software.fromJSON(json,SOFTWARE_OWNER);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return software;
    }

    /**
     * Create an execution system domain object from a json object
     * @param json JSONObject that represents and ExecutionSystem
     * @return ExecutionSystem that can be used in testing or persisted to database.
     */
    public ExecutionSystem createExecutionSystemFromJson(JSONObject json){
        ExecutionSystem executionSystem = null;
        try {
            executionSystem = ExecutionSystem.fromJSON(json);
        } catch (SystemArgumentException e) {
            e.printStackTrace();
        }
        return executionSystem;
    }

    /**
     * create a StorageSystem domain object from a json object
     * @param json
     * @return StrorageSystem
     */
    public StorageSystem createStorageSystemFromJson(JSONObject json){
        StorageSystem storageSystem = null;
        try{
            storageSystem = StorageSystem.fromJSON(json);
        } catch (SystemArgumentException e) {
            e.printStackTrace();
        }
        return storageSystem;
    }

    /**
     * Get a Software object from dehydrated json file on disk
     * @param pathToSoftwareFile path to file with dehydrated Software
     * @return Software object
     */
    public Software hydrateSoftware(String pathToSoftwareFile){
        JSONObject json = retrieveDataFile(pathToSoftwareFile);
        Software software = null;
        try {
             software = Software.fromJSON(json,SOFTWARE_OWNER);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return software;
    }

    /**
     * persist the template dir systems for base data set of domain objects for storage and
     * execution systems
     */
    public void persistDefaultTestSystemsData(){
        fillListsMaps();
        for(JSONObject storageJson : jsonStorageList){
            try {
                RemoteSystem system = (RemoteSystem)StorageSystem.fromJSON(storageJson);
                System.out.println("Storage system "+system.getName());
                system.setOwner(SYSTEM_OWNER);         // default system owner
                systemDao.persist(system);
            } catch (SystemArgumentException e) {
                e.printStackTrace();
            }
        }

        for(JSONObject executionJson : jsonExecutionList){
            try {
                RemoteSystem system = (RemoteSystem)ExecutionSystem.fromJSON(executionJson);
                System.out.println("Execution system "+system.getName());
                system.setOwner(SYSTEM_OWNER);         // default system owner
                systemDao.persist(system);
            } catch (SystemArgumentException e) {
                e.printStackTrace();
            }catch (ConstraintViolationException ce){
                System.out.println("Continue with loading data if possible");
            }

        }
    }

    /**
     * Assuming Execution and Storage systems are in the database we can persist a set of Software domain objects
     * for testing
     */
    public void persistSoftwareDomain(){
        fillSoftwareMap(); // we assume persistSystemDomain has been executed before we begin due to Software.fromJson
                           // dependency on systems being available in the database.
        for(JSONObject softwareJson : jsonSoftwareList){
            try {
                System.out.println(softwareJson.get("id"));
                Software software = Software.fromJSON(softwareJson,SOFTWARE_OWNER);
                software.setOwner(SOFTWARE_OWNER);
                SoftwareDao.persist(software);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * persists a basic set of software that draws on known execution and storage systems
     */
    public void persistSoftwareAndSystemsDomain(){
        persistDefaultTestSystemsData();
        for(JSONObject softwareJson : jsonSoftwareList){
            try {
                Software software = Software.fromJSON(softwareJson,SOFTWARE_OWNER);
                software.setOwner(SYSTEM_OWNER);
                SoftwareDao.persist(software);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * put a software json data file into the database
     * @param pathToSoftwareJsonFile  path to software json file
     */
    public void persistSingleSoftwareEntryFromFile(String pathToSoftwareJsonFile) {
        JSONObject softwareJson = retrieveDataFile(pathToSoftwareJsonFile);

        try {
            Software software = Software.fromJSON(softwareJson,SOFTWARE_OWNER);
            SoftwareDao.persist(software);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * persist a single Software domain object using a path to a json file into the database
     * @param pathToSoftwareJsonFile
     * @param owner
     */
    public void persistSingleSoftwareEntryFromFile(String pathToSoftwareJsonFile, String owner) {

        Software software = hydrateSoftware(pathToSoftwareJsonFile);
        software.setOwner(owner);
        SoftwareDao.persist(software);
    }

    /**
     * persist a single Software domain object using a json string to the database
     * @param json
     */
    public void persistSingleSoftwareEntry(String json) {
        JSONObject softwareJson;
        try {
            softwareJson =new JSONObject(json);
            Software software = Software.fromJSON(softwareJson,SOFTWARE_OWNER);
            software.setOwner(SYSTEM_OWNER);
            SoftwareDao.persist(Software.fromJSON(softwareJson,SOFTWARE_OWNER));
        } catch (JSONException e) {
            System.out.println("failed to persist json \n"+json);
            e.printStackTrace();
        }
    }

    /**
     * persist a single Software domain object using a json object to the database
     * @param softwareJson
     */
    public void persistSingleSoftwareEntry(JSONObject softwareJson) {
        try {
            Software software = Software.fromJSON(softwareJson,SOFTWARE_OWNER);
            software.setOwner(SYSTEM_OWNER);
            SoftwareDao.persist(software);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Setup a single base set of persisted domain objects of type storage and execution to test software registrations
     * @param storageFile
     * @param executeFile
     */
    public void getBaseSystemDomainSetForSoftwareRegistration(String storageFile, String executeFile){
        JSONObject storageJ = retrieveDataFile(STORAGE_SYSTEM_TEMPLATE_DIR+"/"+storageFile);
        JSONObject executeJ = retrieveDataFile(EXECUTION_SYSTEM_TEMPLATE_DIR+"/"+executeFile);
        RemoteSystem storageSystem = null;
        RemoteSystem executionSystem = null;
        try {
            storageSystem = (RemoteSystem)StorageSystem.fromJSON(storageJ);
            storageSystem.setOwner(SYSTEM_OWNER);         // default system owner
            executionSystem = (RemoteSystem)ExecutionSystem.fromJSON(executeJ);
            executionSystem.setOwner(SYSTEM_OWNER);
        } catch (SystemArgumentException e) {
            e.printStackTrace();
        }
    }

    /**
     * gets the json data from a file as a String
     * @param pathToData
     * @return
     */
    public String getRegistrationData(String pathToData){
        return retrieveDataFileAsString(pathToData);
    }

    public void setupCondorTestDataStructure(){
        // the base data is already persisted for storage & execution systems
        // set the global default storage of iplant for wc-condor software.
        SystemDao idao = new SystemDao();
        RemoteSystem iplantStorage  = idao.findBySystemId("data.iplantcollaborative.org");
        iplantStorage.setGlobalDefault(true);
        iplantStorage.setPubliclyAvailable(true);
        idao.persist(iplantStorage);
        System.out.println();
    }

    /**
     *
     * These are simple methods for setting up base data for a particular test scenario
     *
     *
     */

    public void baseDataForSoftwareRegistrationTest(){
        //GSqlData g = new GSqlData("DomainSetup");
        //System.out.println("Total records in all tables "+g.totalTableRecords());
        //g.lockAndWipeTables();
       // System.out.println("Total records in all tables " + g.totalTableRecords());
        System.out.println("\n\n");
        persistDefaultTestSystemsData();
        //System.out.println("Total records in all tables "+g.totalTableRecords());
        System.out.println("\n\n");
        //g.closeConnection("DomainSetup");
    }

    /**
     * Job setup is currently written for wca-iplant-condor software definition and assumes that
     * wca-iplant-condor is persisted in the database.
     * @throws JobException 
     */
    public void  addAJobSubmissionToDatabase() throws JobException{
        // this sets up the ability to create
        //RemoteHPCJobSubmissionTest remoteJob = new RemoteHPCJobSubmissionTest();
        JobDaoTest jobDaoTest = new JobDaoTest();
        Job testJob = null;
        try {

            jobDaoTest.software = SoftwareDao.get("wca-1.00");
            testJob = jobDaoTest.createJob(JobStatusType.PENDING);
            System.out.println();
        } catch (Exception e) {
            System.out.println("our Job creator jobDaoTest is failing for some reason");
            e.printStackTrace();
        }

        // need to change some values on this job and then persist in database in order to
        // kick off the test for CondorLauncher.

        testJob.setName("testname");
        testJob.setOwner("sterry1");
        testJob.setInternalUsername("sterry1");
        testJob.setOutputPath("");
        testJob.setArchivePath("/iplant/home/sterry1/archive/test-job-999");
        testJob.setWorkPath("/dev/null");
        testJob.setUpdateToken("232a28d8930d43fbc4c58069eaae8bba");
        testJob.setLocalJobId("");
        testJob.setSchedulerJobId("");
        testJob.setCharge(Float.parseFloat("1001.5"));

        try {
            JobDao.persist(testJob);
        } catch (JobException e) {
            e.printStackTrace();
        }

    }




    public static void main(String[] args) throws IOException, JobException {

        DomainSetup ds = new DomainSetup();

        //ds.baseDataForSoftwareRegistrationTest();
        ds.addAJobSubmissionToDatabase();


        //ds.fillListsMaps();
        //GSqlData g = new GSqlData("DomainSetup");
        //ds.setupCondorTestDataStructure();
        //ds.fillSoftwareMap();
        //ds.persistSoftwareDomain();
        //ds.persistSingleSoftwareEntryFromFile(SOFTWARE_SYSTEM_TEMPLATE_DIR+"/wc-iplant-condor.tacc.utexas.edu.json");
        // pick a software def
        //ds.viewDomain();
        //g.closeConnection("DomainSetup");
        //ds.persistSingleSoftwareEntryFromFile(SOFTWARE_SYSTEM_TEMPLATE_DIR+"/system-software.json","ipctest");

    }


}

