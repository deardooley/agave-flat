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
        
        // ------------------------ CancelJob -------------------------
        {
            // Generate the json.
            DeleteJobMessage m = new DeleteJobMessage();
            m.jobName = "cancelJob";
            m.jobUuid = "999";
            String json = m.toJson();
            System.out.println(json);
            Assert.assertEquals(
                    json, "{\"command\":\"TCP_CANCEL_JOB\",\"name\":\"cancelJob\",\"uuid\":\"999\"}", 
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
            m.jobName = "stopJbo";
            m.jobUuid = "1001";
            String json = m.toJson();
            System.out.println(json);
            Assert.assertEquals(
                    json, "{\"command\":\"TCP_STOP_JOB\",\"name\":\"stopJbo\",\"uuid\":\"1001\"}", 
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
            String json = m.toJson();
            System.out.println(json);
            Assert.assertEquals(
                    json, "{\"command\":\"TCP_PAUSE_JOB\",\"name\":\"pauseJob\",\"uuid\":\"34\"}", 
                    "Unexpected JSON generated");
      
            // Regenerate the message object.
            PauseJobMessage m2 = PauseJobMessage.fromJson(json);
            Assert.assertEquals(AgaveStringUtils.toComparableString(m2),  
                                AgaveStringUtils.toComparableString(m),
                                "Regenerated message object does not match the original object.");
        }
        
        // ------------------------ TerminateWorkers ------------------
        {
            // Generate the json.
            TerminateWorkersMessage m = new TerminateWorkersMessage();
            m.queueName = "myQueue";
            m.numWorkers = 22;
            String json = m.toJson();
            System.out.println(json);
            Assert.assertEquals(
                    json, "{\"command\":\"TCP_TERMINATE_WORKERS\",\"queueName\":\"myQueue\",\"numWorkers\":22}", 
                    "Unexpected JSON generated");
      
            // Regenerate the message object.
            TerminateWorkersMessage m2 = TerminateWorkersMessage.fromJson(json);
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