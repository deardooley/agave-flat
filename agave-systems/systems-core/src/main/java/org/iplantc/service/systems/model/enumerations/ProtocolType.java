package org.iplantc.service.systems.model.enumerations;

public interface ProtocolType {

	/**
	 * Verifies whether the protocol supports the provided LoginCredentialType.
	 * 
	 * @param type
	 * @return true if supported, false otherwise.
	 */
	public boolean accepts(AuthConfigType type);
	
	public String name();
}
