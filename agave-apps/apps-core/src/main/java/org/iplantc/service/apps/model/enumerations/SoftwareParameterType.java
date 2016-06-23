package org.iplantc.service.apps.model.enumerations;

public enum SoftwareParameterType
{
	string, number, bool, enumeration, flag;
	
	@Override
	public String toString() {
		return name();
	}
	
}
