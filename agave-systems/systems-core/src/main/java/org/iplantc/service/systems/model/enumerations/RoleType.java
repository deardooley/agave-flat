package org.iplantc.service.systems.model.enumerations;

import org.iplantc.service.systems.model.SystemRole;

public enum RoleType implements Comparable<RoleType>
{
	NONE, GUEST, USER, PUBLISHER, ADMIN, OWNER;
	
	public boolean canAdmin()
    {
        return this == ADMIN || this == OWNER;
    }
	
	public boolean canPublish() {
        return this == PUBLISHER || canAdmin();
    }
    
    public boolean canUse() {
		return this == USER || canPublish();
	}
	
	public boolean canRead() {
        return this == GUEST || canUse();
    }
    
    public int intVal() {
		if (this.equals(ADMIN)) {
			return 5;
		} else if (this.equals(OWNER)) {
			return 4;
		} else if (this.equals(PUBLISHER)) {
			return 3;
		} else if (this.equals(USER)) {
			return 2;
		} else if (this.equals(GUEST)) {
			return 1;
		} else {
			return 0;
		}
	}
	
	public static String supportedValuesAsString()
	{
		return NONE + ", " + USER + ", " + PUBLISHER + ", " + OWNER + ", " + ADMIN;
	}
	
	@Override
	public String toString() {
		return name();
	}
}
