/**
 * 
 */
package org.iplantc.service.jobs.model.scripts;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.apps.model.enumerations.ParallelismType;
import org.iplantc.service.jobs.model.Job;

/**
 * Concreate class for fully custom LSF batch submit scripts. This behaves 
 * similarly to the {@link LSFSubmitScript}, but does not attempt to 
 * set any info, rather deferring to the user to customize their scheduler
 * directives as they see fit.
 * @author dooley
 * 
 */
public class CustomLsfSubmitScript extends LsfSubmitScript {
	
	public static final String DIRECTIVE_PREFIX = "#BSUB ";
	
	/**
	 * @param job
	 */
	public CustomLsfSubmitScript(Job job)
	{
		super(job);
	}

	/**
	 * Serializes the object to a batch submit script using a predefined 
	 * job name error, and output directive and whatever was provided in the
	 * {@link BatchQueue#getCustomDirectives()} for the queue assigned to 
	 * the associated job.
	 * 
	 * @return serialized scheduler directives for appending to the job *.ipcexe script
	 */
	@Override
	public String getScriptText()
	{
		if (StringUtils.isEmpty(queue.getCustomDirectives())) {
			return super.getScriptText();
		}
		else {
			String result = "#!/bin/bash \n" 
				+ DIRECTIVE_PREFIX + "-J " + name + "\n"
				+ DIRECTIVE_PREFIX + "-oo " + standardOutputFile + "\n" 
				+ DIRECTIVE_PREFIX + "-e " + standardErrorFile + "\n" ;
	
			result += resolveMacros(queue.getCustomDirectives()) + "\n\n";
			
			return result;
		}
	}

}
