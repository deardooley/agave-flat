package org.iplantc.service.jobs.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.jobs.dao.AbstractDaoTest;
import org.iplantc.service.jobs.model.Job;
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
 * Date: 3/11/13
 * Time: 8:40 AM
 * To change this template use File | Settings | File Templates.
 */
public class JobStoreSoftExecSystemSetup extends AbstractDaoTest {
    RemoteSystem remoteSystem;
    SystemDao systemDao = new SystemDao();
    //public GSqlData gSqlData = new GSqlData("JobStoreSoftExecSystemSetup");
    ArrayList<String> execPaths = new ArrayList<String>();
    ArrayList<String> softPaths = new ArrayList<String>();
    ArrayList<String> storePaths = new ArrayList<String>();
    JSONObject jsonTree = null;

    public void setUpMethod() throws Exception {
        super.beforeClass();  // setup data from SystemsModelTestCommon
        CommonHibernateTest.initdb();
    }

    public void insertExecutionSystem(String testDataPath){
        // retrieve a Execution System definition in json format
        try {
            jsonTree = jtd.getTestDataObject(testDataPath);
            ExecutionSystem executionSystem = ExecutionSystem.fromJSON(jsonTree);
            executionSystem.setOwner("sterry1");
            //System.out.println(jsonTree.get("name").toString());
            systemDao.persist(executionSystem);
        } catch (JSONException e) {
            e.printStackTrace();  
        } catch (IOException e) {
            e.printStackTrace();  
        } catch (SystemArgumentException e) {
            e.printStackTrace();  
        }
    }
    
    public void insertStorageSystem(String testDataPath){
        try {
            jsonTree = jtd.getTestDataObject(testDataPath);
            StorageSystem system = StorageSystem.fromJSON(jsonTree);
            system.setOwner("sterry1");
            // System.out.println(testDataPath);
            systemDao.persist(system);
        } catch (JSONException e) {
            e.printStackTrace();  
        } catch (IOException e) {
            e.printStackTrace();  
        } catch (SystemArgumentException e) {
            e.printStackTrace();  
        }
            
    }

    public void insertSoftware(String testDataPath){
        try {
            jsonTree = jtd.getTestDataObject(testDataPath);
            Software software = Software.fromJSON(jsonTree, "sterry1");
            software.setOwner("sterry1");
            SoftwareDao.persist(software);
        } catch (JSONException e) {
            e.printStackTrace();  
        } catch (IOException e) {
            e.printStackTrace();  
        }

    }

    public void setUpSoftwareExecutionRecords(){
        String dirExec  =  EXECUTION_SYSTEM_TEMPLATE_DIR;
        String dirSoft  =  SOFTWARE_SYSTEM_TEMPLATE_DIR;
        String dirStore =  STORAGE_SYSTEM_TEMPLATE_DIR;

        execPaths.add(dirExec + "/condor.example.com.json");
        execPaths.add(dirExec + "/api.example.com.json");
        execPaths.add(dirExec + "/condor.opensciencegrid.org.json");
        execPaths.add(dirExec + "/execute.example.com.json");
        execPaths.add(dirExec + "/gram.example.com.json");
        execPaths.add(dirExec + "/gsissh.example.com.json");
        execPaths.add(dirExec + "/local.execution.example.com.json");
        execPaths.add(dirExec + "/lonestar.tacc.teragrid.org.json");
        execPaths.add(dirExec + "/ssh.example.com.json");
        execPaths.add(dirExec + "/stampede.tacc.utexas.edu.json");
        execPaths.add(dirExec + "/trestles.sdsc.teragrid.org.json");
        execPaths.add(dirExec + "/unicore.example.com.json");

        softPaths.add(dirSoft+"/wc-iplant-condor.tacc.utexas.edu.json");
        softPaths.add(dirSoft+"/head-lonestar.tacc.teragrid.org.json");
        softPaths.add(dirSoft+"/head-stampede.tacc.utexas.edu.json");
        softPaths.add(dirSoft+"/head-trestles.sdsc.teragrid.org.json");
        softPaths.add(dirSoft+"/system-software.json");

        storePaths.add(dirStore+"/data.iplantcollaborative.org.json");
        storePaths.add(dirStore+"/ftp.example.com.json");
        storePaths.add(dirStore+"/gridftp.example.com.json");
        storePaths.add(dirStore+"/irods.example.com.json");
        storePaths.add(dirStore+"/local.storage.example.com.json");
        storePaths.add(dirStore+"/ranch.tacc.utexas.edu.json");
        storePaths.add(dirStore+"/sftp.example.com.json");
        storePaths.add(dirStore+"/storage.example.com.json");

        try {
            setUpMethod();
            for(String path : execPaths){
                File file = new File(path);
                String filePath = file.getAbsolutePath();
                insertExecutionSystem(filePath);
            }

            for (String path : softPaths){
                File file = new File(path);
                String filePath = file.getAbsolutePath();
                insertSoftware(filePath);
            }

            for(String path : storePaths){
                File file = new File(path);
                String filePath = file.getAbsolutePath();
                insertStorageSystem(filePath);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @SuppressWarnings("unused")
	public Job insertFullJobTestRecordObjectGraph(){
        Job job = null;

        setUpSoftwareExecutionRecords();

        // set up the Job record needed for CondorLauncher
        RemoteSystem remoteSystem = systemDao.findBySystemId("data.iplantcollaborative.org");
        List<Software> softwareList = SoftwareDao.getAll();
        remoteSystem.setPubliclyAvailable(true);
        remoteSystem.setGlobalDefault(true);
        systemDao.persist(remoteSystem);

        Software software = null;
        for(Software sft : softwareList){
            if(sft.getName().equals("wc")){
                software = sft;
                break;
            }
        }

//        try {
//            //job = gSqlData.createJob(JobStatusType.PENDING,remoteSystem,software);
//            if(job != null){
//                JobDao.persist(job);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        System.out.println("system id "+remoteSystem.getId().toString());

        return job;
    }


    public static void main(String[] args){
        JobStoreSoftExecSystemSetup ss = new JobStoreSoftExecSystemSetup();
        ss.insertFullJobTestRecordObjectGraph();

    }

}
