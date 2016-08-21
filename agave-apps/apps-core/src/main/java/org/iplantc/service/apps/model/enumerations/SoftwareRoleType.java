package org.iplantc.service.apps.model.enumerations;

public enum SoftwareRoleType
{
	ADMIN, OWNER, PUBLISHER, USER, GUEST;
	
	@Override
	public String toString() {
		return name();
	}
	
}
