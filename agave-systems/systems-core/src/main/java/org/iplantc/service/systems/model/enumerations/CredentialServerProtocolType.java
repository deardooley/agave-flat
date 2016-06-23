package org.iplantc.service.systems.model.enumerations;

public enum CredentialServerProtocolType
{
	MYPROXY, KERBEROSE, OAUTH2, OA4MP, VOMS, MPG, NONE;
	
	@Override
	public String toString() {
		return name();
	}
}
