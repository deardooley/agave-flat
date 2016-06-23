package org.iplantc.service.jobs.managers.parsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.jobs.exceptions.JobException;
import org.iplantc.service.jobs.exceptions.RemoteJobIDParsingException;
import org.iplantc.service.jobs.exceptions.SchedulerException;

/**
 * Parses the output from a msub command into a local job id 
 * that can be used for querying later on.
 * 
 * @author dooley
 *
 */
public class MoabJobIdParser implements RemoteJobIdParser {

	@Override
	public String getJobId(String output) throws RemoteJobIDParsingException, JobException, SchedulerException
	{
		String jobID = null;
		Pattern pattern = Pattern.compile("([0-9]+).*");
		
		String lines[] = output.replaceAll("\r", "\\n").split("\n");
		for (int idx=0; idx<lines.length; idx++) {
			String line = lines[idx].trim();
			Matcher matcher = pattern.matcher(line);
			if (matcher.matches()) {
				jobID = line.split(" ")[0];
				break;
			}
		}
		
		if (StringUtils.isEmpty(jobID)) {
			if (output.contains("qsub") || output.contains("submit error"))
			{
				throw new SchedulerException(output); 
			}
			else
			{
				throw new RemoteJobIDParsingException(output);
			}
		} else {
			return jobID;
		}
	}
	
	public static void main(String[] args) {
		String[] outputs = new String[]{
				"2059.sang-hn-01.cm.cluster",
				"Failed to submit job",
				"Success",
				"  2059.sang-hn-01.cm.cluster ",
				"2059.sang-hn-01.cm.cluster   submitted to cluster",
				"2059.sang-hn-01.cm.cluster submitted to cluster",
				
		};
		MoabJobIdParser parser = new MoabJobIdParser();
		for (String output: outputs) {
			try {
				System.out.println("Success: " + parser.getJobId(output));
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Error: " + output);
			}
		}
	}
}
