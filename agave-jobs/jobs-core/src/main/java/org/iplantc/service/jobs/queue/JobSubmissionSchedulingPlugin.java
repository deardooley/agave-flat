/**
 * 
 */
package org.iplantc.service.jobs.queue;

import org.iplantc.service.common.queue.GenericSchedulingPlugin;
import org.iplantc.service.jobs.Settings;

/**
 * Class to initialize worker tasks to submit jobs for
 * execution. This class is called by a servlet filter
 * on startup so it will begin running even if no service
 * is called.
 * 
 * @author dooley
 *
 */
public class JobSubmissionSchedulingPlugin extends GenericSchedulingPlugin 
{
	/**
	 * 
	 */
	public JobSubmissionSchedulingPlugin() {
		super();
	}

	@Override
	protected Class<?> getJobClass()
	{
		return SubmissionWatch.class;
	}

	@Override
	protected String getPluginGroup()
	{
		return "Submission";
	}

	@Override
	protected int getTaskCount()
	{
        try {
            getClass().getClassLoader().loadClass("org.iplantc.service.jobs.Settings");
//            return Settings.MAX_SUBMISSION_TASKS;
            return 1;
        } catch (ClassNotFoundException e) {
            return 0;
        }
	}

	

}
