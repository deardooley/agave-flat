package org.iplantc.service.systems.model.enumerations;

public enum SystemStatusType
{
	UP("The system is up"), 
	DOWN("The system is down"), 
	UNKNOWN("The system status is unknown"), 
	MAINTENANCE("The system is under maintenance");
	
	private String expression;
	
	private SystemStatusType(String expression) {
		this.expression = expression;
	}
	@Override
	public String toString() {
		return name();
	}
	
	/**
	 * Rturns a human readable expression describing
	 * the system status. Ex
	 * @return
	 */
	public String getExpression() {
		return this.expression;
	}
}
