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
            m.name = "processJob";
            m.uuid = "73";
            String json = m.toJson();
            System.out.println(json);
            Assert.assertEquals(
                    json, "{\"command\":\"WKR_PROCESS_JOB\",\"name\":\"processJob\",\"uuid\":\"73\"}", 
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
                    json, "{\"command\":\"TPC_DELETE_JOB\",\"tenantId\":\"squatter\",\"jobName\":\"deleteJob\",\"jobUuid\":\"999\"}", 
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
                    json, "{\"command\":\"TPC_STOP_JOB\",\"tenantId\":\"squatter\",\"jobName\":\"stopJob\",\"jobUuid\":\"1001\"}", 
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
            String json = m.toJson();
            System.out.println(json);
            Assert.assertEquals(
                    json, "{\"command\":\"TPC_PAUSE_JOB\",\"tenantId\":\"squatter\",\"jobName\":\"pauseJob\",\"jobUuid\":\"34\"}", 
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
            m.numWorkers = 22;
            m.tenantId = "squatter";
            m.phase = JobPhaseType.ARCHIVING;
            String json = m.toJson();
            System.out.println(json);
            Assert.assertEquals(
                    json, "{\"command\":\"TPC_RESET_NUM_WORKERS\",\"queueName\":\"myQueue\",\"tenantId\":\"squatter\",\"phase\":\"ARCHIVING\",\"numWorkers\":22}", 
                    "Unexpected JSON generated");
      
            // Regenerate the message object.
            ResetNumWorkersMessage m2 = ResetNumWorkersMessage.fromJson(json);
            Assert.assertEquals(AgaveStringUtils.toComparableString(m2),  
                                AgaveStringUtils.toComparableString(m),
                                "Regenerated message object does not match the original object.");
        }
        
        // ------------------------ ResetPriority ------------------
        {
            // Generate the json.
            ResetPriorityMessage m = new ResetPriorityMessage();
            m.queueName = "myQueue";
            m.priority = 339;
            m.tenantId = "squatter";
            m.phase = JobPhaseType.ARCHIVING;
            String json = m.toJson();
            System.out.println(json);
            Assert.assertEquals(
                    json, "{\"command\":\"TPC_RESET_PRIORITY\",\"queueName\":\"myQueue\",\"tenantId\":\"squatter\",\"phase\":\"ARCHIVING\",\"priority\":339}", 
                    "Unexpected JSON generated");
      
            // Regenerate the message object.
            ResetPriorityMessage m2 = ResetPriorityMessage.fromJson(json);
            Assert.assertEquals(AgaveStringUtils.toComparableString(m2),  
                                AgaveStringUtils.toComparableString(m),
                                "Regenerated message object does not match the original object.");
        }
        
        // ------------------------ ResetMaxMessages ------------------
        {
            // Generate the json.
            ResetMaxMessagesMessage m = new ResetMaxMessagesMessage();
            m.queueName = "myQueue";
            m.maxMessages = 45;
            m.tenantId = "squatter";
            m.phase = JobPhaseType.ARCHIVING;
            String json = m.toJson();
            System.out.println(json);
            Assert.assertEquals(
                    json, "{\"command\":\"TPC_RESET_MAX_MESSAGES\",\"queueName\":\"myQueue\",\"tenantId\":\"squatter\",\"phase\":\"ARCHIVING\",\"maxMessages\":45}", 
                    "Unexpected JSON generated");
      
            // Regenerate the message object.
            ResetMaxMessagesMessage m2 = ResetMaxMessagesMessage.fromJson(json);
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
    }
}