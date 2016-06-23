package org.iplantc.service.jobs.model.enumerations;

public enum JobMacroType
{
	HEARTBEAT("Job heartbeat received"),
	NOTIFICATION("Job notification request received");
	
	private final String description;
	
	JobMacroType(String description) {
		this.description = description;
	}
	
	public String getDescription() {
		return description;
	}
	
	@Override
	public String toString() {
		return name();
	}
}
