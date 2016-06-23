/**
 * 
 */
package org.iplantc.service.jobs.model.scripts;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.jobs.model.Job;

/**
 * Concreate class for SLURM batch submit scripts.
 * 
 * @author dooley
 * 
 */
public class SlurmSubmitScript extends AbstractSubmitScript {

	public static final String DIRECTIVE_PREFIX = "#SBATCH ";
	
	/**
	 * Create a batch submit script for SLURM
	 */
	public SlurmSubmitScript(Job job)
	{
		super(job);
	}

	/**
	 * Serializes the object into a SLURM submit script. Assumptions made are
	 * that the number of nodes used will be the ceiling of the number of 
	 * processors requested divided by 16. For serial jobs, an entire node is requested.
	 */
	public String getScriptText()
	{
		String result = "#!/bin/bash\n" 
				+ DIRECTIVE_PREFIX + "-J " + name + "\n"
				+ DIRECTIVE_PREFIX + "-o " + standardOutputFile + "\n" 
				+ DIRECTIVE_PREFIX + "-e " + standardErrorFile + "\n" 
				+ DIRECTIVE_PREFIX + "-t " + time + "\n"
				+ DIRECTIVE_PREFIX + "-p " + queue.getEffectiveMappedName() + "\n"
				+ DIRECTIVE_PREFIX + "-N " + nodes + " -n " + processors + "\n";
				if (!StringUtils.isEmpty(queue.getCustomDirectives())) {
					result += DIRECTIVE_PREFIX + queue.getCustomDirectives() + "\n";
				}

		return result;
	}

}
