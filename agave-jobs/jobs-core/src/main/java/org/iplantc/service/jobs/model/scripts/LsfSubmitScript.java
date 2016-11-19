/**
 * 
 */
package org.iplantc.service.jobs.model.scripts;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.apps.model.enumerations.ParallelismType;
import org.iplantc.service.common.util.TimeUtils;
import org.iplantc.service.jobs.model.Job;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

/**
 * @author dooley
 * 
 */
public class LsfSubmitScript extends AbstractSubmitScript {

	public static final String DIRECTIVE_PREFIX = "#BSUB ";
	
	/**
	 * @param job
	 */
	public LsfSubmitScript(Job job)
	{
		super(job);
	}

	/**
	 * Serializes the object to a bsub submit script.
	 */
	@Override
	public String getScriptText()
	{

		String result = "#!/bin/bash \n" 
			+ DIRECTIVE_PREFIX + "-J " + name + "\n"
			+ DIRECTIVE_PREFIX + "-oo " + standardOutputFile + "\n" 
			+ DIRECTIVE_PREFIX + "-e " + standardErrorFile + "\n" 
			+ DIRECTIVE_PREFIX + "-W " + getTime() + "\n"
			+ DIRECTIVE_PREFIX + "-q " + queue.getEffectiveMappedName() + "\n"
			+ DIRECTIVE_PREFIX + "-L bash";
		
		if (parallelismType.equals(ParallelismType.PTHREAD))
		{
			result += DIRECTIVE_PREFIX + "-n " + nodes + "\n";
			result += DIRECTIVE_PREFIX + "-R 'span[ptile=1]'\n";
		}
		else if (parallelismType.equals(ParallelismType.SERIAL))
		{
			result += DIRECTIVE_PREFIX + "-n 1\n";
			result += DIRECTIVE_PREFIX + "-R 'span[ptile=1]'\n";
		}
		else
		{
			// assume parallel
			result += DIRECTIVE_PREFIX + "-n " + (nodes * processors) + "\n";
			result += DIRECTIVE_PREFIX + "-R 'span[ptile=" + processors + "]'\n";
		}
		
		if (!StringUtils.isEmpty(queue.getCustomDirectives())) {
			result += DIRECTIVE_PREFIX + queue.getCustomDirectives() + "\n";
		}
		
		return result;
	}
	
	/** 
	 * Formats the requested {@link Job#getMaxRunTime()} without the seconds
	 * value. If seconds was greater than zero, the number is rounded down. 
	 * If the overall duration is zero, a minimum run time of 1 minute is 
	 * returned. 
	 * 
	 * @returns the {@link Job#getMaxRunTime()} formatted into HH:mm.
	 */
	@Override
	public String getTime()
	{
		// convert the requested time from hhh:mm:ss format to milliseconds
		int maxRequestedTimeInMilliseconds = TimeUtils.getMillisecondsForMaxTimeValue(time);
		
		// LSF acceptes a minmum run time of 1 minute. Adjust 
		maxRequestedTimeInMilliseconds = Math.max(maxRequestedTimeInMilliseconds, 60000);
		
		// convert to a duration and print. we already pull in joda time
		// so this saves us having to check for runtime ranges, rounding, etc.
		Duration duration = new Duration(maxRequestedTimeInMilliseconds);
		
		PeriodFormatter hm = new PeriodFormatterBuilder()
		    .printZeroAlways()
		    .minimumPrintedDigits(2) // gives the '01'
		    .appendHours()
		    .appendSeparator(":")
		    .appendMinutes()
		    .toFormatter();
	
		return hm.print(duration.toPeriod());
		
	}


}
