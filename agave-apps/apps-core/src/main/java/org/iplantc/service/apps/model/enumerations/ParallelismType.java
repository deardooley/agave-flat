package org.iplantc.service.apps.model.enumerations;

public enum ParallelismType
{
	SERIAL, PARALLEL, PTHREAD;
	
	@Override
	public String toString() {
		return name();
	}
}
