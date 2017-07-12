package org.iplantc.service.jobs.managers.monitors.parsers;

import java.util.ArrayList;
import java.util.List;

import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.RemoteJobFailureDetectedException;
import org.iplantc.service.jobs.exceptions.RemoteJobIDParsingException;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitorEmptyResponseException;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitorResponseParsingException;
import org.iplantc.service.jobs.exceptions.RemoteJobUnrecoverableStateException;
import org.iplantc.service.jobs.exceptions.SchedulerException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SlurmHPCMonitorResponseParserTest {

	@DataProvider
	protected Object[][] isJobRunningBlankStatusReturnsFalseProvider() {
		return new Object[][] {
				{ "10974959| |0:0|", false },
				{ "10974959||0:0|", false }
		};
	}
	
	@Test(dataProvider = "isJobRunningBlankStatusReturnsFalseProvider")
	public void isJobRunningBlankStatusReturnsFalse(String schedulerOutput, boolean shouldBeRunning) 
	throws RemoteJobIDParsingException, JobException, SchedulerException, RemoteJobMonitorEmptyResponseException, RemoteJobMonitorResponseParsingException, RemoteJobUnrecoverableStateException 
	{
		_isJobRunning(schedulerOutput, shouldBeRunning);
	}
	
	@DataProvider
	protected Object[][] isJobRunningFailedStatusThrowsRemoteJobFailureDetectedExceptionProvider() {
		List<Object[]> testCases = new ArrayList<Object[]>();
		for (SlurmHPCMonitorResponseParser.SlurmStatusType statusType: SlurmHPCMonitorResponseParser.SlurmStatusType.values()) {
			if (SlurmHPCMonitorResponseParser.SlurmStatusType.isFailureStatus(statusType.name())) {
				testCases.add(new Object[]{ "10974959|" + statusType.name() + "|0:1|" });
			}
		}
		
		return testCases.toArray(new Object[][]{});
	}
	
	@Test(dataProvider = "isJobRunningFailedStatusThrowsRemoteJobFailureDetectedExceptionProvider")
	public void isJobRunningFailedStatusThrowsRemoteJobFailureDetectedException(String schedulerOutput) 
	throws RemoteJobIDParsingException, JobException, SchedulerException, RemoteJobMonitorEmptyResponseException, RemoteJobMonitorResponseParsingException, RemoteJobUnrecoverableStateException 
	{
		_isJobRunningFailedStatusThrowsRemoteJobFailureDetectedException(schedulerOutput);
	}
	
	protected void _isJobRunningFailedStatusThrowsRemoteJobFailureDetectedException(String schedulerOutput) {

		try {
			SlurmHPCMonitorResponseParser parser = new SlurmHPCMonitorResponseParser();
			parser.isJobRunning(schedulerOutput);
			Assert.fail("Failed status should throw RemoteJobFailureDetectedException");
		}
		catch (RemoteJobFailureDetectedException e) {
			
		}
		catch (Exception e) {
			Assert.fail("Failed status should throw RemoteJobFailureDetectedException");
		}
	}

	@DataProvider
	protected Object[][] isJobRunningUnrecoverableStatusThrowsRemoteJobUnrecoverableStateExceptionProvider() {
		return new Object[][] {
				{ "10974959|ecw|0:0|" }
		};
	}
	
	@Test(dataProvider = "isJobRunningUnrecoverableStatusThrowsRemoteJobUnrecoverableStateExceptionProvider")
	public void isJobRunningUnrecoverableStatusThrowsRemoteJobUnrecoverableStateException(String schedulerOutput) 
	throws RemoteJobIDParsingException, JobException, SchedulerException, RemoteJobMonitorEmptyResponseException, RemoteJobMonitorResponseParsingException, RemoteJobUnrecoverableStateException 
	{
		_isJobRunningUnrecoverableStatusThrowsRemoteJobUnrecoverableStateException(schedulerOutput);
	}
	
	public void _isJobRunningUnrecoverableStatusThrowsRemoteJobUnrecoverableStateException(String schedulerOutput) {

		try {
			SlurmHPCMonitorResponseParser parser = new SlurmHPCMonitorResponseParser();
			parser.isJobRunning(schedulerOutput);
			Assert.fail("Unrecoverable status should throw RemoteJobUnrecoverableStateException");
		}
		catch (RemoteJobUnrecoverableStateException e) {
			
		}
		catch (Exception e) {
			Assert.fail("Unrecoverable status should throw RemoteJobUnrecoverableStateException");
		}
	}
	
	@DataProvider
	protected Object[][] isJobRunningProvider() {
		
		List<Object[]> testCases = new ArrayList<Object[]>();
//		for (SlurmHPCMonitorResponseParser.SlurmStatusType statusType: SlurmHPCMonitorResponseParser.SlurmStatusType.values()) {
//			if (statusType.isFailureStatus() || !statusType.isActiveStatus()) {
//				testCases.add(new Object[]{ "10974959|" + statusType.name() + "|0:1|", false });
//			}
//			else {
//				testCases.add(new Object[]{ "10974959|" + statusType.name() + "|0:0|", true });
//			}
//		}
//		
//		testCases.add(new Object[]{ "10974959||0:0|", false });
//		testCases.add(new Object[]{ "10974959| |0:0|", false });
		testCases.add(new Object[]{ "620162|PENDING|0:0|", true });
		
		return testCases.toArray(new Object[][]{});
//		
//		return new Object[][] {
//				{ "10974959||0:0|", false },
//				{ "10974959| |0:0|", false },
//				{ "10974959|RESIZING|0:0|", true },
//				{ "10974959|RUNNING|0:0|", true },
//				{ "10974959|PENDING|0:0|", true },
//				{ "10974959|COMPLETED|0:0|", false },
//				
//		};
	}
	
	@Test(dataProvider = "isJobRunningProvider")
	public void isJobRunning(String schedulerOutput, boolean shouldBeRunning) 
	throws RemoteJobIDParsingException, JobException, SchedulerException, RemoteJobMonitorEmptyResponseException, RemoteJobMonitorResponseParsingException, RemoteJobUnrecoverableStateException 
	{
		_isJobRunning(schedulerOutput, shouldBeRunning);
	}
	
	@DataProvider
	protected Object[][] isJobRunningCaseInsensitiveStatusProvider() {
		return new Object[][] {
				{ "10974959|running|0:0|", true },
				{ "10974959|Running|0:0|", true },
				{ "10974959|RuNnInG|0:0|", true }
		};
	}
	
	@Test(dataProvider = "isJobRunningCaseInsensitiveStatusProvider")
	public void isJobRunningCaseInsensitiveStatus(String schedulerOutput, boolean shouldBeRunning) 
	throws RemoteJobIDParsingException, JobException, SchedulerException, RemoteJobMonitorEmptyResponseException, RemoteJobMonitorResponseParsingException, RemoteJobUnrecoverableStateException 
	{
		_isJobRunning(schedulerOutput, shouldBeRunning);
	}
	
	@DataProvider
	protected Object[][] isJobRunningMultilineStatusProvider() {
		return new Object[][] {
				{ "10974959|COMPLETED|0:0|\n10974959.batch|COMPLETED|0:0|\n10974959.extern|COMPLETED|0:0|", false },
		};
	}
	
	@Test(dataProvider = "isJobRunningMultilineStatusProvider")
	public void isJobRunningMultilineStatus(String schedulerOutput, boolean shouldBeRunning) 
	throws RemoteJobIDParsingException, JobException, SchedulerException, RemoteJobMonitorEmptyResponseException, RemoteJobMonitorResponseParsingException, RemoteJobUnrecoverableStateException 
	{
		_isJobRunning(schedulerOutput, shouldBeRunning);
	}
	
	protected void _isJobRunning(String schedulerOutput, boolean shouldBeRunning) 
	throws RemoteJobIDParsingException, JobException, SchedulerException, RemoteJobMonitorEmptyResponseException, RemoteJobMonitorResponseParsingException, RemoteJobUnrecoverableStateException 
	{
		SlurmHPCMonitorResponseParser parser = new SlurmHPCMonitorResponseParser();
		boolean running = false;
		try {
			running = parser.isJobRunning(schedulerOutput);
		}
		catch (Exception e) {}
		
		Assert.assertEquals(running, shouldBeRunning, "Job running state and expected state did not match");
	}
	
	@DataProvider
	protected Object[][] isJobRunningAfterUsageResponseProvider() {
		return new Object[][] {
				{ "", false },
		};
	}
	
	@Test(dataProvider = "isJobRunningAfterUsageResponse")
	public void isJobRunningAfterUsageResponse(String schedulerOutput, boolean shouldBeRunning) 
	throws RemoteJobIDParsingException, JobException, SchedulerException, RemoteJobMonitorEmptyResponseException, RemoteJobMonitorResponseParsingException, RemoteJobUnrecoverableStateException 
	{
		_isJobRunning(schedulerOutput, shouldBeRunning);
	}
	
	protected void _isJobRunningAfterUsageResponse(String schedulerOutput, boolean shouldBeRunning) 
	throws RemoteJobIDParsingException, JobException, SchedulerException, RemoteJobMonitorEmptyResponseException, RemoteJobMonitorResponseParsingException, RemoteJobUnrecoverableStateException 
	{
		SlurmHPCMonitorResponseParser parser = new SlurmHPCMonitorResponseParser();
		boolean running = false;
		try {
			running = parser.isJobRunning(schedulerOutput);
		}
		catch (Exception e) {}
		
		Assert.assertEquals(running, shouldBeRunning, "Job running state and expected state did not match");
	}
}
