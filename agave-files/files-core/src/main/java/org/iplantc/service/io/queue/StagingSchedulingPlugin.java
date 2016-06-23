package org.iplantc.service.io.queue;

import org.iplantc.service.common.queue.GenericSchedulingPlugin;
import org.iplantc.service.io.Settings;

/**
 * Class to initialize worker tasks to encode data upon offline and
 * stage it somewhere. This class is called by a servlet filter
 * on startup so it will begin running even if no service
 * is called.
 * 
 * @author dooley
 *
 */
public class StagingSchedulingPlugin extends GenericSchedulingPlugin 
{
	/**
	 * 
	 */
	public StagingSchedulingPlugin() {
		super();
	}

	@Override
	protected Class<?> getJobClass()
	{
		return StagingJob.class;
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
			getClass().getClassLoader().loadClass("org.iplantc.service.io.Settings");
			return Settings.MAX_STAGING_TASKS;
//			return 1;
		} catch (ClassNotFoundException e) {
			return 0;
		}
	}

	

}
