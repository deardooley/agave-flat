package org.iplantc.service.jobs.managers.parsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.iplantc.service.jobs.exceptions.RemoteJobIDParsingException;

/**
 * Parses the output from a sbatcj command into a local job id 
 * that can be used for querying later on.
 * 
 * @author dooley
 *
 */
public class SlurmJobIdParser implements RemoteJobIdParser {

	@Override
	public String getJobId(String output) throws RemoteJobIDParsingException
	{
		Pattern pattern = Pattern.compile("Submitted batch job \\d+");
		Matcher matcher = pattern.matcher(output);
		
		if (!matcher.find())
		{
			if (output.startsWith("sbatch: error:")) {
				output = output.substring("sbatch: error:".length());
			}
			throw new RemoteJobIDParsingException(output); 
		} 
		else
		{
			output = matcher.group();
			
			pattern = Pattern.compile("\\d+");
			matcher = pattern.matcher(output);
			matcher.find();
			
			return matcher.group();
		}
	}

}
