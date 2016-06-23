/**
 * 
 */
package org.iplantc.service.jobs.model.scripts;

import org.iplantc.service.apps.model.enumerations.ParallelismType;
import org.iplantc.service.jobs.exceptions.JobException;

/**
 * @author dooley
 * 
 */
public interface SubmitScript {

	public String getScriptText() throws JobException;
	
	/**
	 * @return the name
	 */
	public abstract String getName();

	/**
	 * @param name
	 *            the name to set
	 */
	public abstract void setName(String name);

	/**
	 * @return the inCurrentWorkingDirectory
	 */
	public abstract boolean isInCurrentWorkingDirectory();

	/**
	 * @param inCurrentWorkingDirectory
	 *            the inCurrentWorkingDirectory to set
	 */
	public abstract void setInCurrentWorkingDirectory(
			boolean inCurrentWorkingDirectory);

	/**
	 * @return the verbose
	 */
	public abstract boolean isVerbose();

	/**
	 * @param verbose
	 *            the verbose to set
	 */
	public abstract void setVerbose(boolean verbose);

	/**
	 * @return the standardOutputFile
	 */
	public abstract String getStandardOutputFile();

	/**
	 * @param standardOutputFile
	 *            the standardOutputFile to set
	 */
	public abstract void setStandardOutputFile(String standardOutputFile);

	/**
	 * @return the standardErrorFile
	 */
	public abstract String getStandardErrorFile();

	/**
	 * @param standardErrorFile
	 *            the standardErrorFile to set
	 */
	public abstract void setStandardErrorFile(String standardErrorFile);

	/**
	 * @return the time
	 */
	public abstract String getTime();

	/**
	 * @param time
	 *            the time to set
	 */
	public abstract void setTime(String time);

	/**
	 * @return the parallel
	 */
	public abstract ParallelismType getParallelismType();

	/**
	 * @param parallel
	 *            the parallel to set
	 */
	public abstract void setParallelismType(ParallelismType parallelismType);

	/**
	 * @return the processors
	 */
	public abstract long getProcessors();

	/**
	 * @param processors
	 *            the processors to set
	 */
	public abstract void setProcessors(long processors);

	/**
	 * @param batchInstructions
	 *            the batchInstructions to set
	 */
	public abstract void setBatchInstructions(String batchInstructions);

	/**
	 * @return the batchInstructions
	 */
	public abstract String getBatchInstructions();
	
}
