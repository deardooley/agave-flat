package org.iplantc.service.systems.model.enumerations;

/**
 * Represents the different ways to connect to a system to submit a job
 * 
 * @author dooley
 *
 */
public enum LoginProtocolType implements ProtocolType
{
	API, SSH, GSISSH, LOCAL;//, GRAM, UNICORE;

	@Override
	public boolean accepts(AuthConfigType type)
	{
		if (this.equals(GSISSH)) {// || this.equals(GRAM) || this.equals(UNICORE)) {
			return (type.equals(AuthConfigType.X509));
		} else if (this.equals(API)) {
			return (type.equals(AuthConfigType.PASSWORD) || 
					type.equals(AuthConfigType.TOKEN));
		} else if (this.equals(SSH)) {
			return (type.equals(AuthConfigType.PASSWORD) || type.equals(AuthConfigType.SSHKEYS));
		} else if (this.equals(LOCAL)) {
			return type.equals(AuthConfigType.LOCAL);
		} else {
			return false;
		}
	}
	
	@Override
	public String toString() {
		return name();
	}
}
