/**
 * 
 */
package org.teragrid.service.tgcdb.dto;

/**
 * Types of batch schedulers.
 * 
 * @author dooley
 * 
 */
public enum SchedulerType
{
	LSF, LOADLEVELER, PBS, SGE, CONDOR, FORK, COBALT, TORQUE, MOAB, SLURM, UNKNOWN;


	public String getBatchSubmitCommand() 
	{
		switch (this) 
		{
			case LSF:
			case COBALT:
				return "bsub";
			case LOADLEVELER:
				return "llsub";
			case TORQUE:
			case MOAB:
			case PBS:
			case SGE:
				return "qsub";
			case CONDOR:
				return "condor-submit";
			case FORK:
				return "sh -c";
			case SLURM:
				return "sbatch";
			default:
				return "qsub";
				
		}
	}
	
	public String getBatchKillCommand() 
	{
		switch (this) 
		{
			case LSF:
			case COBALT:
				return "bkill";
			case LOADLEVELER:
				return "llcancel";
			case TORQUE:
			case MOAB:
			case PBS:
			case SGE:
				return "qdel";
			case CONDOR:
				return "condor-rm";
			case FORK:
				return "kill -9 ";
			case SLURM:
				return "scancel";
			default:
				return "qdel";
				
		}
	}
}