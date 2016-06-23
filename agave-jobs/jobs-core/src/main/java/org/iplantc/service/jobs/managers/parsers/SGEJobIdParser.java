package org.iplantc.service.jobs.managers.parsers;

import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.RemoteJobIDParsingException;
import org.iplantc.service.jobs.exceptions.SchedulerException;

/**
 * Parses the output from a qsub command into a local job id 
 * that can be used for querying later on.
 * 
 * @author dooley
 *
 */
public class SGEJobIdParser implements RemoteJobIdParser {

	@Override
	public String getJobId(String output) throws RemoteJobIDParsingException, JobException, SchedulerException
	{
		if (!output.contains("has been submitted"))
		{
			if (output.contains("You have exceeded the max job time limit."))
			{
				throw new JobException(output);
			} 
			else if (output.contains("qmaster")) 
			{
				throw new SchedulerException("Failed to submit job. The system " +
						"is temporarily down while the scheduler is reset.", 
						new JobException(output));
			}
			else
			{
				throw new JobException("Failed to submit job. " + output); 
			}
		}
		else
		{
			// save job with local jobid
			output = output.replaceAll("\n", "").replaceAll("\\n", "");
			// output is of the form "Your job xxxxxxx (\"test_job\") has
			// been submitted
			output = output.substring(output.indexOf("Your job ") + 9);
			output = output.substring(0, output.indexOf("("));
			output = output.trim();
			return output;
		}
	}

}
