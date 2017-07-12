package org.iplantc.service.jobs.managers.monitors.parsers.responses;

import java.util.ArrayList;
import java.util.List;

import org.iplantc.service.jobs.exceptions.RemoteJobMonitorCommandSyntaxException;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitorEmptyResponseException;
import org.iplantc.service.jobs.exceptions.RemoteJobMonitorResponseParsingException;
import org.iplantc.service.jobs.managers.monitors.parsers.SlurmHPCMonitorResponseParser;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SlurmJobStatusResponseTest {

	@DataProvider
	protected Object[][] parseResponseStatusProvider() {
		
		List<Object[]> testCases = new ArrayList<Object[]>();
		for (SlurmHPCMonitorResponseParser.SlurmStatusType statusType: SlurmHPCMonitorResponseParser.SlurmStatusType.values()) {
			testCases.add(new Object[]{ "10974959|" + statusType.name() + "|0:0|", statusType.name() });
		}
		testCases.add(new Object[]{ "10974959|UNKNOWN|0:0|", "UNKNOWN" });
		testCases.add(new Object[]{ "10974959|asdfasdfasdfadfa|0:0|", "asdfasdfasdfadfa" });
		testCases.add(new Object[]{ "620162|PENDING|0:0|", "PENDING"});
		
		return testCases.toArray(new Object[][]{});
	}
	
	@Test(dataProvider = "parseResponseStatusProvider")
	public void parseResponseStatus(String rawServerResponse, String expectedStatus) 
	throws RemoteJobMonitorEmptyResponseException, RemoteJobMonitorResponseParsingException 
	{
		SlurmJobStatusResponse jobStatusResponse = new SlurmJobStatusResponse();
		
		List<String> tokens = jobStatusResponse.parseResponse(rawServerResponse);	
		
		Assert.assertEquals(tokens.get(1), expectedStatus, "Parsed status did not match expected status");
	}
	
	@Test(dataProvider = "parseResponseStatusProvider")
	public void newSlurmJobStatusResponse(String rawServerResponse, String expectedStatus) 
	throws RemoteJobMonitorEmptyResponseException, RemoteJobMonitorResponseParsingException, RemoteJobMonitorCommandSyntaxException 
	{
		 SlurmJobStatusResponse jobStatusResponse = new SlurmJobStatusResponse(rawServerResponse);	
		
		Assert.assertEquals(jobStatusResponse.getStatus(), expectedStatus, "Parsed status did not match expected status");
	}
	
	@DataProvider
	protected Object[][] parseResponseStatusWithEmptyFieldsProvider() {
		return new Object[][] {
				{ "10974959|COMPLETED||", "COMPLETED" },
				{ "|COMPLETED|0:0|", "COMPLETED" },
				{ "|COMPLETED||", "COMPLETED" },
		};
	}
	
	@Test(dataProvider = "parseResponseStatusWithEmptyFieldsProvider")
	public void parseResponseStatusWithEmptyFields(String rawServerResponse, String expectedStatus) 
	throws RemoteJobMonitorEmptyResponseException, RemoteJobMonitorResponseParsingException 
	{
		SlurmJobStatusResponse jobStatusResponse = new SlurmJobStatusResponse();
		
		List<String> tokens = jobStatusResponse.parseResponse(rawServerResponse);	
		
		Assert.assertEquals(tokens.get(1), expectedStatus, "Parsed status did not match expected status");
	}
	
	@DataProvider
	protected Object[][] parseResponseStatusEmptyOrMissingStatusDoesNotThrowExceptionProvider() {
		return new Object[][] {
				{ "10974959||0.0|" },
				{ "10974959|||" },
				{ "||0:0|" },
				{ "|||" },
		};
	}
	
	@Test(dataProvider = "parseResponseStatusEmptyOrMissingStatusDoesNotThrowExceptionProvider")
	public void parseResponseStatusEmptyOrMissingStatusDoesNotThrowException(String rawServerResponse) 
	throws RemoteJobMonitorEmptyResponseException, RemoteJobMonitorResponseParsingException 
	{
		try {
			SlurmJobStatusResponse jobStatusResponse = new SlurmJobStatusResponse();
			
			List<String> tokens = jobStatusResponse.parseResponse(rawServerResponse);	
			
			Assert.assertEquals(tokens.get(1), "", "Empty status should be parsed as empty");
		} 
		catch (Exception e) {
			Assert.fail("Missing status should not throw exception", e);
		}
	}
	
	@DataProvider
	protected Object[][] parseResponseEmptyValueThrowsRemoteJobMonitorEmptyResponseExceptionProvider() {
		return new Object[][] {
				{ "", "Empty server response should throw RemoteJobMonitorEmptyResponseException" },
				{ null, "Null server response should throw RemoteJobMonitorEmptyResponseException" },
				{ "  ", "Blank server response should throw RemoteJobMonitorEmptyResponseException" },
		};
	}
	
	@Test(dataProvider="parseResponseEmptyValueThrowsRemoteJobMonitorEmptyResponseExceptionProvider")
	public void parseResponseEmptyValueThrowsRemoteJobMonitorEmptyResponseException(String rawServerResponse, String message) 
	throws RemoteJobMonitorResponseParsingException 
	{
		try {
			SlurmJobStatusResponse jobStatusResponse = new SlurmJobStatusResponse();
			
			jobStatusResponse.parseResponse(rawServerResponse);
			
			Assert.fail(message);
		}
		catch (RemoteJobMonitorEmptyResponseException e) {
			
		}
		catch (Exception e) {
			Assert.fail(message, e);
		}
	}
	
	@DataProvider
	protected Object[][] parseResponseBadDelimiterThrowsRemoteJobMonitorResponseParsingExceptionProvider() {
		return new Object[][] {
				{ "10974959 COMPLETED 0:0 2016-11-28T15:54:11 ", "Space delimited server response should throw RemoteJobMonitorResponseParsingException" },
				{ "10974959,COMPLETED,0:0,2016-11-28T15:54:11,00:36:31,", "Comma delimited server response should throw RemoteJobMonitorResponseParsingException" },
				{ "COMPLETED", "Status only response should throw RemoteJobMonitorResponseParsingException" },
		};
	}
	
	@Test(dataProvider="parseResponseBadDelimiterThrowsRemoteJobMonitorResponseParsingExceptionProvider")
	public void parseResponseBadDelimiterThrowsRemoteJobMonitorResponseParsingException(String rawServerResponse, String message) 
	throws RemoteJobMonitorEmptyResponseException 
	{
		try {
			SlurmJobStatusResponse jobStatusResponse = new SlurmJobStatusResponse();
			
			jobStatusResponse.parseResponse(rawServerResponse);
			
			Assert.fail(message);
		}
		catch (RemoteJobMonitorResponseParsingException e) {
			
		}
		catch (Exception e) {
			Assert.fail(message, e);
		}
	}
	
	@DataProvider
	protected Object[][] parseResponseMultilineProvider() {
		return new Object[][] {
				{ "10974959|COMPLETED|0:0|\n10974959.batch|COMPLETED|0:0|\n10974959.extern|COMPLETED|0:0|", "COMPLETED" },
				{ "57544|TIMEOUT|1:0|\n8057544.batch|CANCELLED|0:15|", "TIMEOUT" }
		};
	}
	
	@Test(dataProvider = "parseResponseMultilineProvider")
	public void parseResponseMultiline(String rawServerResponse, String expectedStatus) 
	throws RemoteJobMonitorEmptyResponseException, RemoteJobMonitorResponseParsingException 
	{
		SlurmJobStatusResponse jobStatusResponse = new SlurmJobStatusResponse();
		
		List<String> tokens = jobStatusResponse.parseResponse(rawServerResponse);	
		
		Assert.assertEquals(tokens.get(1), expectedStatus, "multiline parsed status did not match expected status");
	}
}
