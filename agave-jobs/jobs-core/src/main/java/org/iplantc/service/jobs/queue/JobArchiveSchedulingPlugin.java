/**
 * 
 */
package org.iplantc.service.jobs.queue;

import org.apache.log4j.Logger;
import org.iplantc.service.common.queue.GenericSchedulingPlugin;
import org.iplantc.service.jobs.Settings;

/**
 * Class to initialize worker tasks to archive job data upon
 * job completion. This class is called by a servlet filter
 * on startup so it will begin running even if no service
 * is called.
 * 
 * @author dooley
 *
 */
public class JobArchiveSchedulingPlugin extends GenericSchedulingPlugin 
{
    final Logger log = Logger.getLogger(JobArchiveSchedulingPlugin.class);
    
	/**
	 * 
	 */
	public JobArchiveSchedulingPlugin() {
		super();
	}

	@Override
	protected Class<?> getJobClass()
	{
		return ArchiveWatch.class;
	}

	@Override
	protected String getPluginGroup()
	{
		return "Archive";
	}

	@Override
	protected int getTaskCount()
	{
        try {
            getClass().getClassLoader().loadClass("org.iplantc.service.jobs.Settings");
//            return Settings.MAX_ARCHIVE_TASKS;
            return 1;
        } catch (ClassNotFoundException e) {
            return 0;
        }
	}	
}
