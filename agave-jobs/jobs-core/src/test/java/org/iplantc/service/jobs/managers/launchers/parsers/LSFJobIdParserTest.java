package org.iplantc.service.jobs.managers.launchers.parsers;

import org.apache.commons.lang3.StringUtils;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.RemoteJobIDParsingException;
import org.iplantc.service.jobs.exceptions.SchedulerException;
import org.iplantc.service.jobs.managers.launchers.parsers.PBSJobIdParser;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Unit tests validating the parsing of a standard and non-standard
 * job submission response from the LSF scheduler.
 * @author dooley
 *
 */
@Test(groups={"unit"})
public class LSFJobIdParserTest {

	@DataProvider
	protected Object[][] getJobIdProvider() {
		return new Object[][] {
				{ "Job <657598> is submitted", "657598" },
				{ "Job <657598> is submitted.", "657598" },
				{ "Job <657598> is submitted <>", "657598" },
				{ "Job <657598> is submitted windfall", "657598" },
				{ "Job <657598> is submitted to queue <windfall>.", "657598" },
				{ "Job <657598> is submitted to queue <windfall>", "657598" },
				{ "Job <657598> is submitted to queue <windfall> ", "657598" },
				{ "Job <657598> is submitted to queue <windfall>\t", "657598" },
				{ "Job <657598> is submitted to queue <windfall>\n", "657598" },
				{ " Job <657598> is submitted to queue <windfall>.", "657598" },
				{ "\tJob <657598> is submitted to queue <windfall>.", "657598" },
				{ "\nJob <657598> is submitted to queue <windfall>.", "657598" },
				{ "Something\nJob <657598> is submitted to queue <windfall>.", "657598" },
				{ "Something\tJob <657598> is submitted to queue <windfall>.", "657598" },
				{ "Something\nJob <657598> is submitted to queue <windfall>\n", "657598" },
				{ "Something\nJob <657598> is submitted to queue <windfall>\nSomething", "657598" },
				{ "Something\nJob <657598> is submitted to queue <windfall>    ", "657598" },
				{ "Something\nSomething\n657598\nSomething\n\n", "657598" },
				{ " 657598 ", "657598" },
				{ "  657598.foo  ", "657598.foo" },
		};
	}
	
	@Test(dataProvider = "getJobIdProvider")
	public void getJobId(String schedulerOutput, String expectedJobId) 
	throws RemoteJobIDParsingException, JobException, SchedulerException 
	{
		LSFJobIdParser parser = new LSFJobIdParser();
		String foundJobId = parser.getJobId(schedulerOutput);
		Assert.assertTrue(StringUtils.isNotEmpty(foundJobId), "LSF job id should not be null");
		Assert.assertEquals(foundJobId, expectedJobId, "LSF job id found did not match the expected job id");
	}

}
