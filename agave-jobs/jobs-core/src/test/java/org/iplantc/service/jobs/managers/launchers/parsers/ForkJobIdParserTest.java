package org.iplantc.service.jobs.managers.launchers.parsers;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.iplantc.service.jobs.exceptions.RemoteJobIDParsingException;
import org.iplantc.service.jobs.managers.launchers.parsers.ForkJobIdParser;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ForkJobIdParserTest {
	
	private static final String EXPECTED_JOB_ID = "12345";
	private static final String UNEXPECTED_JOB_ID = "54321";
	private static final String SCRIPT_NAME = "foo.ipcexe";
	
	@DataProvider
	public Object[][] getJobIdProvider() {
		return new Object[][] { 
			new Object[] { EXPECTED_JOB_ID + " " + SCRIPT_NAME , EXPECTED_JOB_ID, false, "Valid process response from the server should return the process id" },
			new Object[] { EXPECTED_JOB_ID, EXPECTED_JOB_ID, false, "Partial response from the server including process id first should return the process id" },
			new Object[] { SCRIPT_NAME, null, true, "Partial response from the server including only script name first should throw exception" },
			new Object[] { "", null, true, "empty response from the server should throw exception" }, 
			new Object[] { null, null, true, "null response from the server should throw exception" }, 
		};
	}

	@Test(dataProvider = "getJobIdProvider")
	public void getJobId(String remoteOutput, String expectedJobId,
			boolean shouldThrowException, String message) {
		
		ForkJobIdParser parser = new ForkJobIdParser();
		
		try {
			Assert.assertEquals(parser.getJobId(remoteOutput), expectedJobId, message);
		}
		catch (RemoteJobIDParsingException e) {
			if (!shouldThrowException) {
				Assert.fail(message, e);
			}
		}
	}
	
	@DataProvider
	public Object[][] getJobIdIgnoresWhitespacePaddingProvider() {
		List<Object[]> testCases = new ArrayList<Object[]>();
		String singleSpace = " ";
		String doubleSpace = "  ";
		String tab = "\t";
		
		for (String whitespace: Arrays.asList(singleSpace, doubleSpace, tab)) {
			testCases.add(new Object[] { EXPECTED_JOB_ID + whitespace + SCRIPT_NAME , EXPECTED_JOB_ID, false, "Job id and process name separated by '"+whitespace+"' returns the process id" });
			testCases.add(new Object[] { whitespace + EXPECTED_JOB_ID + whitespace + SCRIPT_NAME , EXPECTED_JOB_ID, false, "Job id and process name preceded and separated by '"+whitespace+"' returns the process id" });
			testCases.add(new Object[] { EXPECTED_JOB_ID + whitespace + SCRIPT_NAME + whitespace , EXPECTED_JOB_ID, false, "Job id and process name trailed and separated by '"+whitespace+"' returns the process id" });
			testCases.add(new Object[] { whitespace + EXPECTED_JOB_ID + whitespace + SCRIPT_NAME + whitespace , EXPECTED_JOB_ID, false, "Job id and process name bookended and separated by '"+whitespace+"' returns the process id" });
			
			testCases.add(new Object[] { whitespace + EXPECTED_JOB_ID , EXPECTED_JOB_ID, false, "Partial response with only the job id preceded by '"+whitespace+"' returns the process id" });
			testCases.add(new Object[] { EXPECTED_JOB_ID + whitespace , EXPECTED_JOB_ID, false, "Partial response with only the job id trailed by '"+whitespace+"' returns the process id" });
			testCases.add(new Object[] { whitespace + EXPECTED_JOB_ID + whitespace , EXPECTED_JOB_ID, false, "Partial response with only the job id bookended by '"+whitespace+"' returns the process id" });
			
			testCases.add(new Object[] { whitespace + SCRIPT_NAME , null, true, "Partial response with only the script name preceded by '"+whitespace+"' should throw an exception" });
			testCases.add(new Object[] { SCRIPT_NAME + whitespace , null, true, "Partial response with only the script name trailed by '"+whitespace+"' should throw an exception" });
			testCases.add(new Object[] { whitespace + SCRIPT_NAME + whitespace , null, true, "Partial response with only the script name bookended by '"+whitespace+"' should throw an exception" });
			
			testCases.add(new Object[] { whitespace, null, true, "Respone with only whitespace value of '"+whitespace+"' should throw an exception" });
		}
	
		return testCases.toArray(new Object[][]{}); 
	}
	
	@Test(dataProvider = "getJobIdIgnoresWhitespacePaddingProvider")
	public void getJobIdIgnoresWhitespacePadding(String remoteOutput, String expectedJobId,
			boolean shouldThrowException, String message) {
		
		ForkJobIdParser parser = new ForkJobIdParser();
		
		try {
			Assert.assertEquals(parser.getJobId(remoteOutput), expectedJobId, message);
		}
		catch (RemoteJobIDParsingException e) {
			if (!shouldThrowException) {
				Assert.fail(message, e);
			}
		}
	}
}
