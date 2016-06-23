/**
 * 
 */
package org.iplantc.service.jobs.model.scripts;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.jobs.model.Job;

/**
 * Concreate class for fully custom GridEngine batch submit scripts. This behaves 
 * similarly to the {@link SgeSubmitScript}, but does not attempt to 
 * set any info, rather deferring to the user to customize their scheduler
 * directives as they see fit.
 * 
 * @author dooley
 * 
 */
public class CustomGridEngineSubmitScript extends SgeSubmitScript 
{
	/**
	 * 
	 */
	public CustomGridEngineSubmitScript(Job job)
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
			String result = "#!/bin/bash\n\n" 
					+ SgeSubmitScript.DIRECTIVE_PREFIX + "-N " + name + "\n"
					+ ( inCurrentWorkingDirectory ? SgeSubmitScript.DIRECTIVE_PREFIX + "-cwd\n" : "" )
					+ SgeSubmitScript.DIRECTIVE_PREFIX + "-V\n" 
					+ SgeSubmitScript.DIRECTIVE_PREFIX + "-o " + standardOutputFile + "\n" 
					+ SgeSubmitScript.DIRECTIVE_PREFIX + "-e " + standardErrorFile + "\n";
			result += resolveMacros(queue.getCustomDirectives()) + "\n\n";
			
			return result;
		}
	}
}
