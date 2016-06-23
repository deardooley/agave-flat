package org.iplantc.service.monitor.model.enumeration;

import org.iplantc.service.systems.model.enumerations.SystemStatusType;

public enum MonitorStatusType {
	PASSED, FAILED, UNKNOWN;
	
	public SystemStatusType getSystemStatus()
	{
		if (this.equals(PASSED)) {
			return SystemStatusType.UP;
		} else if (this.equals(FAILED)){
			return SystemStatusType.DOWN;
		} else {
			return SystemStatusType.UNKNOWN;
		}
	}
}
