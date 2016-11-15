/**
 * 
 */
package org.iplantc.service.jobs.model.scripts;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.apps.model.enumerations.ParallelismType;
import org.iplantc.service.jobs.model.Job;

/**
 * @author dooley
 * 
 */
public class LsfSubmitScript extends AbstractSubmitScript {

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

		String prefix = "#BSUB ";
		String result = "#!/bin/bash \n" 
			+ prefix + "-J " + name + "\n"
			+ prefix + "-o " + standardOutputFile + "\n" 
			+ prefix + "-e " + standardErrorFile + "\n" 
			+ prefix + "-W " + time + "\n"
			+ prefix + "-q " + queue.getEffectiveMappedName() + "\n";

		if (parallelismType.equals(ParallelismType.PTHREAD))
		{
			result += prefix + "-n " + nodes + "\n";
			result += prefix + "-R 'span[ptile=1]'\n";
		}
		else if (parallelismType.equals(ParallelismType.SERIAL))
		{
			result += prefix + "-n 1\n";
			result += prefix + "-R 'span[ptile=1]'\n";
		}
		else
		{
			// assume parallel
			result += prefix + "-n " + (nodes * processors) + "\n";
			result += prefix + "-R 'span[ptile=" + processors + "]'\n";
		}
		
		if (!StringUtils.isEmpty(queue.getCustomDirectives())) {
			result += prefix + queue.getCustomDirectives() + "\n";
		}
		
		return result;
	}

}
