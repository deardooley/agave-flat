package org.iplantc.service.jobs.phases.queuemessages;

import java.io.IOException;
import java.util.ArrayList;

import org.iplantc.service.common.util.AgaveStringUtils;
import org.iplantc.service.jobs.model.enumerations.JobPhaseType;
import org.testng.Assert;
import org.testng.annotations.Test;

/** This test suite tests the conversion to and from json for all queue messages.
 * 
 * @author rcardone
 */
public class QueueMessagesTest {
    /* ********************************************************************** */
    /*                              Test Methods                              */
    /* ********************************************************************** */    
    /* ---------------------------------------------------------------------- */
    /* toAndFromJsonTest:                                                     */
    /* ---------------------------------------------------------------------- */
    @Test(enabled=true)
    public void toAndFromJsonTest() throws IOException
    {   
        // ------------------------ NoOp ------------------------------
        {
            // Generate the json.
            NoOpMessage m = new NoOpMessage();
            m.testMessage = "This is a test message";
            String json = m.toJson();
            System.out.println(json);
            Assert.assertEquals(
                    json, "{\"command\":\"NOOP\",\"testMessage\":\"This is a test message\"}", 
                    "Unexpected JSON generated");
      
            // Regenerate the message object.
            NoOpMessage m2 = NoOpMessage.fromJson(json);
            Assert.assertEquals(AgaveStringUtils.toComparableString(m2),  
                                AgaveStringUtils.toComparableString(m),
                                "Regenerated message object does not match the original object.");
        }
        
        // ------------------------ ProcessJob ------------------------
        {
            // Generate the json.
            ProcessJobMessage m = new ProcessJobMessage();
            m.name  = "processJob";
            m.uuid  = "73";
            m.epoch = 22;
            String json = m.toJson();
            System.out.println(json);
            Assert.assertEquals(
                    json, "{\"command\":\"WKR_PROCESS_JOB\",\"name\":\"processJob\",\"uuid\":\"73\",\"epoch\":22}", 
                    "Unexpected JSON generated");
      
            // Regenerate the message object.
            ProcessJobMessage m2 = ProcessJobMessage.fromJson(json);
            Assert.assertEquals(AgaveStringUtils.toComparableString(m2),  
                                AgaveStringUtils.toComparableString(m),
                                "Regenerated message object does not match the original object.");
        }
        
        // ------------------------ DeleteJob -------------------------
        {
            // Generate the json.
            DeleteJobMessage m = new DeleteJobMessage();
            m.jobName = "deleteJob";
            m.jobUuid = "999";
            m.tenantId = "squatter";
            String json = m.toJson();
            System.out.println(json);
            Assert.assertEquals(
                    json, "{\"command\":\"TPC_DELETE_JOB\",\"tenantId\":\"squatter\",\"jobName\":\"deleteJob\",\"jobUuid\":\"999\",\"epoch\":0}", 
                    "Unexpected JSON generated");
      
            // Regenerate the message object.
            DeleteJobMessage m2 = DeleteJobMessage.fromJson(json);
            Assert.assertEquals(AgaveStringUtils.toComparableString(m2),  
                                AgaveStringUtils.toComparableString(m),
                                "Regenerated message object does not match the original object.");
         }
        
        // ------------------------ StopJob ---------------------------
        {
            // Generate the json.
            StopJobMessage m = new StopJobMessage();
            m.jobName = "stopJob";
            m.jobUuid = "1001";
            m.tenantId = "squatter";
            String json = m.toJson();
            System.out.println(json);
            Assert.assertEquals(
                    json, "{\"command\":\"TPC_STOP_JOB\",\"tenantId\":\"squatter\",\"jobName\":\"stopJob\",\"jobUuid\":\"1001\",\"epoch\":0}", 
                    "Unexpected JSON generated");
      
            // Regenerate the message object.
            StopJobMessage m2 = StopJobMessage.fromJson(json);
            Assert.assertEquals(AgaveStringUtils.toComparableString(m2),  
                                AgaveStringUtils.toComparableString(m),
                                "Regenerated message object does not match the original object.");
        }
        
        // ------------------------ PauseJob --------------------------
        {
            // Generate the json.
            PauseJobMessage m = new PauseJobMessage();
            m.jobName = "pauseJob";
            m.jobUuid = "34";
            m.tenantId = "squatter";
            m.epoch    = 66;
            String json = m.toJson();
            System.out.println(json);
            Assert.assertEquals(
                    json, "{\"command\":\"TPC_PAUSE_JOB\",\"tenantId\":\"squatter\",\"jobName\":\"pauseJob\",\"jobUuid\":\"34\",\"epoch\":66}", 
                    "Unexpected JSON generated");
      
            // Regenerate the message object.
            PauseJobMessage m2 = PauseJobMessage.fromJson(json);
            Assert.assertEquals(AgaveStringUtils.toComparableString(m2),  
                                AgaveStringUtils.toComparableString(m),
                                "Regenerated message object does not match the original object.");
        }
        
        // ------------------------ ResetNumWorkers ------------------
        {
            // Generate the json.
            ResetNumWorkersMessage m = new ResetNumWorkersMessage();
            m.queueName = "myQueue";
            m.numWorkersDelta = 22;
            m.tenantId = "squatter";
            m.phase = JobPhaseType.ARCHIVING;
            String json = m.toJson();
            System.out.println(json);
            Assert.assertEquals(
                    json, "{\"command\":\"TPC_RESET_NUM_WORKERS\",\"queueName\":\"myQueue\",\"tenantId\":\"squatter\",\"phase\":\"ARCHIVING\",\"numWorkersDelta\":22,\"schedulers\":[]}", 
                    "Unexpected JSON generated");
      
            // Regenerate the message object.
            ResetNumWorkersMessage m2 = ResetNumWorkersMessage.fromJson(json);
            Assert.assertEquals(AgaveStringUtils.toComparableString(m2),  
                                AgaveStringUtils.toComparableString(m),
                                "Regenerated message object does not match the original object.");
        }
        
        // ------------------------ ResetNumWorkers 2 ----------------
        {
            // Generate the json.
            ResetNumWorkersMessage m = new ResetNumWorkersMessage();
            m.queueName = "myQueue";
            m.numWorkersDelta = 33;
            m.tenantId = "squatter";
            m.phase = JobPhaseType.STAGING;
            m.schedulers.add("myScheduler");
            String json = m.toJson();
            System.out.println(json);
            Assert.assertEquals(
                    json, "{\"command\":\"TPC_RESET_NUM_WORKERS\",\"queueName\":\"myQueue\",\"tenantId\":\"squatter\",\"phase\":\"STAGING\",\"numWorkersDelta\":33,\"schedulers\":[\"myScheduler\"]}", 
                    "Unexpected JSON generated");
      
            // Regenerate the message object.
            ResetNumWorkersMessage m2 = ResetNumWorkersMessage.fromJson(json);
            Assert.assertEquals(AgaveStringUtils.toComparableString(m2),  
                                AgaveStringUtils.toComparableString(m),
                                "Regenerated message object does not match the original object.");
        }
        
        // ------------------------ RefreshQueueInfo ---------------
        {
            // Generate the json.
            RefreshQueueInfoMessage m = new RefreshQueueInfoMessage();
            String json = m.toJson();
            System.out.println(json);
            Assert.assertEquals(
                    json, "{\"command\":\"TPC_REFRESH_QUEUE_INFO\"}", 
                    "Unexpected JSON generated");
      
            // Regenerate the message object.
            RefreshQueueInfoMessage m2 = RefreshQueueInfoMessage.fromJson(json);
            Assert.assertEquals(AgaveStringUtils.toComparableString(m2),  
                                AgaveStringUtils.toComparableString(m),
                                "Regenerated message object does not match the original object.");
        }
        
        // ------------------------ Shutdown --------------------------
        {
            // Generate the json.
            ShutdownMessage m = new ShutdownMessage();
            ArrayList<JobPhaseType> list = new ArrayList<>();
            list.add(JobPhaseType.STAGING);
            list.add(JobPhaseType.ARCHIVING);
            m.phases = list;
            String json = m.toJson();
            System.out.println(json);
            Assert.assertEquals(
                    json, "{\"command\":\"TPC_SHUTDOWN\",\"phases\":[\"STAGING\",\"ARCHIVING\"]}", 
                    "Unexpected JSON generated");
      
            // Regenerate the message object.
            ShutdownMessage m2 = ShutdownMessage.fromJson(json);
            Assert.assertEquals(AgaveStringUtils.toComparableString(m2),  
                                AgaveStringUtils.toComparableString(m),
                                "Regenerated message object does not match the original object.");
        }
        
        // ------------------------ Shutdown 2 ------------------------
        {
            // Generate the json.
            ShutdownMessage m = new ShutdownMessage();
            String json = m.toJson();
            System.out.println(json);
            Assert.assertEquals(
                    json, "{\"command\":\"TPC_SHUTDOWN\",\"phases\":[]}", 
                    "Unexpected JSON generated");
      
            // Regenerate the message object.
            ShutdownMessage m2 = ShutdownMessage.fromJson(json);
            Assert.assertEquals(AgaveStringUtils.toComparableString(m2),  
                                AgaveStringUtils.toComparableString(m),
                                "Regenerated message object does not match the original object.");
        }
    }
}