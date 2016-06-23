package org.iplantc.service.data.queue;

import org.iplantc.service.common.queue.GenericSchedulingPlugin;
import org.iplantc.service.data.Settings;

/**
 * Class to initialize worker tasks to decode data upon offline and
 * stage it somewhere. This class is called by a servlet filter
 * on startup so it will begin running even if no service
 * is called.
 * 
 * @author dooley
 *
 */
public class ZombieTransformSchedulingPlugin extends GenericSchedulingPlugin 
{
	/**
	 * 
	 */
	public ZombieTransformSchedulingPlugin() {
		super();
	}

	@Override
	protected Class getJobClass()
	{
		return ZombieTransformWatch.class;
	}

	@Override
	protected String getPluginGroup()
	{
		return "ZombieTransform";
	}

	@Override
	protected int getTaskCount()
	{
		try {
			getClass().getClassLoader().loadClass("org.iplantc.service.data.Settings");
			return Settings.ENABLE_ZOMBIE_CLEANUP ? 1 : 0;
		} catch (ClassNotFoundException e) {
			return 0;
		}
	}

	

}
