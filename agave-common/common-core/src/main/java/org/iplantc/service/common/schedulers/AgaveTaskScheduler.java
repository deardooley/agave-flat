/**
 * 
 */
package org.iplantc.service.common.schedulers;

import java.util.Set;

import org.iplantc.service.common.discovery.ServiceCapability;
import org.iplantc.service.common.exceptions.TaskSchedulerException;

/**
 * General interface defining job schedulers.
 * 
 * @author dooley
 *
 */
public interface AgaveTaskScheduler
{
	public String getNextTaskId(Set<ServiceCapability> capabilities) throws TaskSchedulerException;
}
