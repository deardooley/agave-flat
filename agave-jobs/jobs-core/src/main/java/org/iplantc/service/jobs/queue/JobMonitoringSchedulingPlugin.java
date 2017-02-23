/**
 * 
 */
package org.iplantc.service.jobs.queue;

import org.iplantc.service.common.queue.GenericSchedulingPlugin;
import org.iplantc.service.jobs.Settings;

/**
 * Class to initialize worker tasks to watch condor jobs until
 * completion. This class is called by a servlet filter
 * on startup so it will begin running even if no service
 * is called.
 * 
 * @author dooley
 *
 */
public class JobMonitoringSchedulingPlugin extends GenericSchedulingPlugin 
{
	/**
	 * 
	 */
	public JobMonitoringSchedulingPlugin() {
		super();
	}

	@Override
	protected Class<?> getJobClass()
	{
		return MonitoringWatch.class;
	}

	@Override
	protected String getPluginGroup()
	{
		return "Monitoring";
	}

	@Override
	protected int getTaskCount()
	{
        try {
            getClass().getClassLoader().loadClass("org.iplantc.service.jobs.Settings");
//            return Settings.MAX_MONITORING_TASKS;
            return 1;
        } catch (ClassNotFoundException e) {
            return 0;
        }
	}

}
