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
public class LoadLevelerSubmitScript extends AbstractSubmitScript {

	/**
	 * @param job
	 */
	public LoadLevelerSubmitScript(Job job)
	{
		super(job);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.iplantcollaborative.service.wrapper.model.AbstractSubmitScript#toString
	 * ()
	 */
	@Override
	public String getScriptText()
	{

		// #! /bin/bash -l
		// ## LoadLeveler script to submit 2 node, 4 task MPI program: hello
		// # @ job_type = MPICH
		// # @ class = LONG
		// # @ account_no = NONE
		// # @ node = 2
		// # @ tasks_per_node = 4
		// # @ wall_clock_limit = 10:00:00
		// # @ notification = always
		// # @ notify_user = <email_id>
		// # @ environment=COPY_ALL;
		// # @ output = hello.$(cluster).$(process).out
		// # @ error = hello.$(cluster).$(process).err
		// # @ queue

		// TODO: serialize script to pbs syntax
		// return null;

		String prefix = "#@ ";
		String result = "#! /bin/bash -l \n" 
				+ prefix + "- " + name + "\n"
				+ prefix + "environment = COPY_ALL\n" 
				+ prefix + "output = " + standardOutputFile + "\n" 
				+ prefix + "error = " + standardErrorFile + "\n" 
				+ prefix + "class = NORMAL\n"
				+ prefix + "account_no = NONE \n" 
				+ prefix + "wall_clock_limit = " + time + "\n";

		if (parallelismType.equals(ParallelismType.PTHREAD))
		{
			result += prefix + "job_type = MPICH\n";
			result += prefix + "node = 1\n";
			result += prefix + "tasks_per_node = " + processors + "\n";
		}
		else if (parallelismType.equals(ParallelismType.SERIAL))
		{
			result += prefix + "job_type = MPICH\n";
			result += prefix + "node_usage = not_shared\n";
			result += prefix + "nodes = 1\n";
			result += prefix + "tasks_per_node = 1\n";
		}
		else
		{
			result += prefix + "job_type = MPICH\n";
			result += prefix + "node_usage = not_shared\n";
			result += prefix + "nodes = " + nodes + "\n";
			result += prefix + "tasks_per_node = " + processors + "\n";
		}

		result += prefix + "queue " + queue.getEffectiveMappedName() + "\n";
		
		if (!StringUtils.isEmpty(queue.getCustomDirectives())) {
			result += prefix + queue.getCustomDirectives() + "\n";
		}
		
//		if (!StringUtils.isEmpty(system.getDefaultQueue().getCustomDirectives())) {
//			result += system.getDefaultQueue().getCustomDirectives() + "\n";
//		}
//		
////		for (String directive : system.getDefaultQueue().getCustomDirectives()) {
////			if (!StringUtils.isEmpty(directive)) {
////				result += prefix + directive + "\n";
////			}
////		}
		
		return result;
	}

}
