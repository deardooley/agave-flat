/**
 * 
 */
package org.iplantc.service.jobs.queue;

import org.iplantc.service.common.queue.GenericSchedulingPlugin;
import org.iplantc.service.jobs.Settings;

/**
 * Class to initialize worker tasks to stage job data prior to
 * job execution. This class is called by a servlet filter
 * on startup so it will begin running even if no service
 * is called.
 * 
 * @author dooley
 *
 */
public class JobStagingSchedulingPlugin extends GenericSchedulingPlugin 
{
	/**
	 * 
	 */
	public JobStagingSchedulingPlugin() {
		super();
	}

	@Override
	protected Class<?> getJobClass()
	{
		return StagingWatch.class;
	}

	@Override
	protected String getPluginGroup()
	{
		return "Staging";
	}

	@Override
	protected int getTaskCount()
	{
        try {
            getClass().getClassLoader().loadClass("org.iplantc.service.jobs.Settings");
//            return Settings.MAX_STAGING_TASKS;
            return 1;
        } catch (ClassNotFoundException e) {
            return 0;
        }
	}

	

}
