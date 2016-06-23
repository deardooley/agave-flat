/**
 * 
 */
package org.iplantc.service.jobs.queue;

import org.iplantc.service.common.queue.GenericSchedulingPlugin;
import org.iplantc.service.jobs.Settings;

/**
 * Class to initialize worker tasks to clean up zombie
 * jobs across the platform. This class is called by a servlet filter
 * on startup so it will begin running even if no service
 * is called.
 * 
 * @author dooley
 *
 */
public class ZombieJobSchedulingPlugin extends GenericSchedulingPlugin 
{
	/**
	 * 
	 */
	public ZombieJobSchedulingPlugin() {
		super();
	}

	@Override
	protected Class<?> getJobClass()
	{
		return ZombieJobWatch.class;
	}

	@Override
	protected String getPluginGroup()
	{
		return "ZombieCleanup";
	}

	@Override
	protected int getTaskCount()
	{
        try {
            getClass().getClassLoader().loadClass("org.iplantc.service.jobs.Settings");
            return Settings.ENABLE_ZOMBIE_CLEANUP ? 1 : 0;
        } catch (ClassNotFoundException e) {
            return 0;
        }
	}

	

}
