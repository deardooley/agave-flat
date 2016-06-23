/**
 * 
 */
package org.iplantc.service.jobs.model.scripts;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.jobs.model.Job;

/**
 * Concreate class for fully custom Torque batch submit scripts. This behaves 
 * similarly to the {@link TorqueSubmitScript}, but does not attempt to 
 * set any info, rather deferring to the user to customize their scheduler
 * directives as they see fit.
 * 
 * @author dooley
 * 
 */
public class CustomTorqueSubmitScript extends TorqueSubmitScript {

	public static final String DIRECTIVE_PREFIX = "#PBS ";
	
	/**
	 * 
	 */
	public CustomTorqueSubmitScript(Job job)
	{
		super(job);
	}

	@Override
	public String getScriptText()
	{			
		if (StringUtils.isEmpty(queue.getCustomDirectives())) {
			return super.getScriptText();
		}
		else {
			String result = "#!/bin/bash\n" 
				+ DIRECTIVE_PREFIX + "-N " + name + "\n"
				+ DIRECTIVE_PREFIX + "-o " + standardOutputFile + "\n" 
				+ DIRECTIVE_PREFIX + "-e " + standardErrorFile + "\n";

			result += resolveMacros(queue.getCustomDirectives()) + "\n\n";
			
			return result;
		}
	}
}
