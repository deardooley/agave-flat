package org.iplantc.service.apps.model.enumerations;

public enum AuthenticationType
{
	TERAGRID, IPLANT, TACC;
	
	@Override
	public String toString() {
		return name();
	}
}
