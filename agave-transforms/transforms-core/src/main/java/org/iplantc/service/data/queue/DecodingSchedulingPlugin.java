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
public class DecodingSchedulingPlugin extends GenericSchedulingPlugin 
{
	/**
	 * 
	 */
	public DecodingSchedulingPlugin() {
		super();
	}

	@Override
	protected Class getJobClass()
	{
		return DecodingJob.class;
	}

	@Override
	protected String getPluginGroup()
	{
		return "Decoding";
	}

	@Override
	protected int getTaskCount()
	{
		try {
			getClass().getClassLoader().loadClass("org.iplantc.service.data.Settings");
			return Settings.MAX_TRANSFORM_TASKS;
		} catch (ClassNotFoundException e) {
			return 0;
		}
	}

	

}
