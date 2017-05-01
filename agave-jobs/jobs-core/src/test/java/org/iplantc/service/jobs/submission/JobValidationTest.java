package org.iplantc.service.jobs.submission;

import java.util.HashMap;

import org.iplantc.service.apps.dao.SoftwareDao;
import org.iplantc.service.apps.model.Software;
import org.iplantc.service.apps.model.SoftwareInput;
import org.iplantc.service.apps.model.SoftwareOutput;
import org.iplantc.service.apps.model.SoftwareParameter;
import org.iplantc.service.jobs.dao.JobDao;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test class to verify the job submission validation checks in the JobResource
 * class. All tests should pass before user job submissions can be assumed to work.
 * These tests do not, however, test the actual data staging or placement of a job
 * into queue. Those tests must be done independently in their respective test
 * classes.
 * 
 * @author dooley
 *
 */
@Test(groups={"integration"})
public class JobValidationTest extends AbstractJobSubmissionTest{
	
	/**
	 * Initalizes the test db and adds the test app 
	 */
	@BeforeClass
	public void beforeClass() throws Exception {
	    super.beforeClass();
	}
	
	@AfterClass
    public void afterClass() throws Exception {
		super.afterClass();
    }
	
	public void getDefaultJobSubmissionForm()
	{
		Software software = SoftwareDao.getSoftwareByUniqueName("head-5.97u1");
		jobRequestMap = new HashMap<String, Object>();
		jobRequestMap.put("jobName", "test-job");
		jobRequestMap.put("softwareName", software.getUniqueName());
		jobRequestMap.put("processorCount", "1");
		jobRequestMap.put("maxMemory", "512");
		jobRequestMap.put("callbackUrl", "bob@example.com");
		for(SoftwareInput input: software.getInputs()) {
		    jobRequestMap.put(input.getKey(), input.getDefaultValueAsJsonArray().iterator().next().asText());
		}
		for(SoftwareOutput output: software.getOutputs()) {
		    jobRequestMap.put(output.getKey(), output.getDefaultValue());
		}
		for(SoftwareParameter parameter: software.getParameters()) {
		    jobRequestMap.put(parameter.getKey(), parameter.getDefaultValueAsJsonArray().iterator().next().asText());
		}
		jobRequestMap.put("requestedTime","5:00:00");
	}
	
	@AfterMethod
	public void afterMethod() {
		try {
			JobDao.delete(job);
		} catch(Exception e) {}
	}
	
	@DataProvider(name = "jobSubmissionName")
    public Object[][] jobSubmissionName() 
	{
		getDefaultJobSubmissionForm();
		
		Object[][] mydata = {
    		{ "jobName", null, "jobName must not be null", true },
    		{ "jobName", "", "jobName must not be empty", true },
    		{ "jobName", "0abc", "jobName can start with a number", false },
    		{ "jobName", "test job", "jobName can contain a space", false },
    		{ "jobName", "test-job", "jobName can contain a dash", false },
    		{ "jobName", "test_job", "jobName can contain a underscore", false },
    		{ "jobName", jobRequestMap.get("jobName"), "jobName must be a valid string", false }
        };
        return mydata;
    }
	
	@DataProvider(name = "jobSubmissionSoftware")
    public Object[][] jobSubmissionSoftware() 
	{
		getDefaultJobSubmissionForm();
		
		Object[][] mydata = {
    		{ "softwareName", null, "softwareName must not be null", true },
    		{ "softwareName", "", "softwareName must not be empty", true },
    		{ "softwareName", "abc", "softwareName must be a valid software id", true },
    		{ "softwareName", "abc-123", "softwareName must exist", true },
    		{ "softwareName", jobRequestMap.get("softwareName"), "softwareName must exist", false },
        };
        return mydata;
    }
	
	@DataProvider(name = "jobSubmissionSerialProcessorCount")
    public Object[][] jobSubmissionSerialProcessorCount() 
	{
		getDefaultJobSubmissionForm();
		
		Object[][] mydata = {
    		{ "processorCount", null, "processorCount not be null on serial jobs", true },
    		{ "processorCount", "0", "processorCount not be 0 on serial jobs", true },
    		{ "processorCount", "1", "processorCount be 1 on serial jobs", false },
    		{ "processorCount", "2", "processorCount can be 2 or more on serial jobs", true }
        };
        return mydata;
    }
	
	@DataProvider(name = "jobSubmissionParallelProcessorCount")
    public Object[][] jobSubmissionParallelProcessorCount() 
	{
		getDefaultJobSubmissionForm();
		
		jobRequestMap.put("softwareName", "bwa-lonestar-0.5.9u1");
		
        Object[][] mydata = {
			{ "processorCount", null, "processorCount not be null on parallel jobs", true },
			{ "processorCount", "0", "processorCount not be 0 on parallel jobs", true },
			{ "processorCount", "1", "processorCount be greater than 1 on parallel jobs", true },
			{ "processorCount", "2", "processorCount can be 2 or more on parallel jobs", false }
        };
        return mydata;
    }
	
	@Test (dataProvider="jobSubmissionName")
	public void jobNameChecks(String attribute, String value, String message, boolean shouldThrowException) 
	{	
		super.genericJobSubmissionTestCase(attribute, value, message, shouldThrowException);
	}
	
	@Test (dataProvider="jobSubmissionSoftware")
	public void jobSoftwareChecks(String attribute, String value, String message, boolean shouldThrowException) 
	{	
		super.genericJobSubmissionTestCase(attribute, value, message, shouldThrowException);
	}
	
	@Test (dataProvider="jobSubmissionSerialProcessorCount")
	public void jobSerialProcessorCountChecks(String attribute, String value, String message, boolean shouldThrowException) 
	{	
		super.genericJobSubmissionTestCase(attribute, value, message, shouldThrowException);
	}
	
	@Test (dataProvider="jobSubmissionParallelProcessorCount")
	public void jobParallelProcessorCountChecks(String attribute, String value, String message, boolean shouldThrowException)
	{	
		super.genericJobSubmissionTestCase(attribute, value, message, shouldThrowException);
	}
	
//	@DataProvider(name = "jobNotificationProvider")
//    public Object[][] jobNotificationProvider() 
//	{
//		getDefaultJobSubmissionForm();
//		
//		jobRequestMap.put("softwareName", "bwa-lonestar-0.5.9u1");
//		
//        Object[][] mydata = {
//			{ "processorCount", null, "processorCount not be null on parallel jobs", true },
//			{ "processorCount", "0", "processorCount not be 0 on parallel jobs", true },
//			{ "processorCount", "1", "processorCount be greater than 1 on parallel jobs", true },
//			{ "processorCount", "2", "processorCount can be 2 or more on parallel jobs", false }
//        };
//        return mydata;
//    }
//	
//	@Test (dataProvider="jobNotificationProvider")
//	public void jobNotification(String attribute, String value, String message, boolean shouldThrowException)
//	{	
//		super.genericJobSubmissionTestCase(attribute, value, message, shouldThrowException);
//	}
	
}
