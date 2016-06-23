/**
 * 
 */
package org.iplantc.service.jobs.model.scripts;

import org.apache.commons.lang.StringUtils;
import org.iplantc.service.jobs.model.Job;

/**
 * Concreate class for SGE batch submit scripts.
 * 
 * @author dooley
 * 
 */
public class PbsSubmitScript extends AbstractSubmitScript {

	/**
	 * 
	 */
	public PbsSubmitScript(Job job)
	{
		super(job);
	}

	/**
	 * Serializes the object into a PBS submit script. Assumption made are that
	 * for PTHREAD applications, the processor value is the number of cores per
	 * node. i.e. 1 node, N cores. For serial jobs, an entire node is requested.
	 * For parallel applications, half the processor value of nodes is requested
	 * with two cores per node.
	 */
	public String getScriptText()
	{
		// #!/bin/bash
		// #PBS -q workq
		// # the queue to be used.
		// #
		// #PBS -A your_allocation
		// # specify your project allocation
		// #
		// #PBS -l nodes=4:ppn=8
		// # number of nodes and number of processors on each node to be used.
		// # Do NOT use ppn = 1. Note that there are 8 processors on each Queen
		// Bee node.
		// #
		// #PBS -l cput=20:00:00
		// # requested CPU time.
		// #
		// #PBS -l walltime=20:00:00
		// # requested Wall-clock time.
		// #
		// #PBS -o myoutput2
		// # name of the standard out file to be "output-file".
		// #
		// #PBS -j oe
		// # standard error output merge to the standard output file.
		// #
		// #PBS -N s_type
		// # name of the job (that will appear on executing the qstat command).
		// #
		// # Following are non PBS commands. PLEASE ADOPT THE SAME EXECUTION
		// SCHEME
		// # i.e. execute the job by copying the necessary files from your home
		// directpory
		// # to the scratch space, execute in the scratch space, and copy back
		// # the necessary files to your home directory.
		// #
		// export WORK_DIR=/work/$USER/your_code_directory
		// cd $WORK_DIR
		// # changing to your working directory (we recommend you to use work
		// volume for batch job run)
		// #
		// export NPROCS=`wc -l $PBS_NODEFILE |gawk '//{print $1}'`
		// #
		// date
		// #timing the time job starts
		// #

		String prefix = "#PBS ";
		String result = "#!/bin/bash\n" 
				+ prefix + "-q normal\n"
				+ prefix + "-N " + name + "\n"
				+ prefix + "-o " + standardOutputFile + "\n" 
				+ prefix + "-e " + standardErrorFile + "\n" 
				+ prefix + "-l cput=" + time + "\n"
				+ prefix + "-l walltime=" + time + "\n"
				+ prefix + "-q " + queue.getEffectiveMappedName() + "\n"
				+ prefix + "-l nodes=" + nodes + ":ppn=" + processors + "\n";
				
				if (!StringUtils.isEmpty(queue.getCustomDirectives())) {
					result += prefix + queue.getCustomDirectives() + "\n";
				}

		return result;
	}

}
