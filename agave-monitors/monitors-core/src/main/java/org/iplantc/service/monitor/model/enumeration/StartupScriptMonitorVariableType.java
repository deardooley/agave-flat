package org.iplantc.service.monitor.model.enumeration;

import org.apache.log4j.Logger;
import org.hibernate.cfg.NotYetImplementedException;
import org.iplantc.service.monitor.model.Monitor;
import org.iplantc.service.monitor.model.MonitorCheck;
import org.iplantc.service.systems.model.ExecutionSystem;


public enum StartupScriptMonitorVariableType 
{
	MONITOR_ID,
	MONITOR_OWNER,
	MONITOR_CHECK_ID;
	
	private static final Logger log = Logger.getLogger(StartupScriptMonitorVariableType.class);

	/**
	 * Resolves a template variable into the actual value for the
	 * monitor. Tenancy is honored with respect to the monitor.
	 * 
	 * @param monitor A valid monitor object
	 * @return resolved value of the variable.
	 */
	public String resolveForSystem(MonitorCheck monitorCheck)
	{	
		if (this == MONITOR_ID)
		{
			return monitorCheck.getMonitor().getUuid();
		}
		else if (this == MONITOR_OWNER)
		{
			return monitorCheck.getMonitor().getOwner();
		}
		else if (this == MONITOR_CHECK_ID)
		{
			return monitorCheck.getUuid();
		}
		else {
			throw new NotYetImplementedException("The startupScript variable " + name() + " is not yet supported.");
		}
	}
}
