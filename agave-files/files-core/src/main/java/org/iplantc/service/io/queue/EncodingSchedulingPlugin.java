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
public class EncodingSchedulingPlugin extends GenericSchedulingPlugin 
{
	/**
	 * 
	 */
	public EncodingSchedulingPlugin() {
		super();
	}

	@Override
	protected Class<?> getJobClass()
	{
		return EncodingJob.class;
	}

	@Override
	protected String getPluginGroup()
	{
		return "Encoding";
	}

	@Override
	protected int getTaskCount()
	{
		try {
			getClass().getClassLoader().loadClass("org.iplantc.service.io.Settings");
			return Settings.MAX_TRANSFORM_TASKS;
//			return 1;
		} catch (ClassNotFoundException e) {
			return 0;
		}
	}

	

}
