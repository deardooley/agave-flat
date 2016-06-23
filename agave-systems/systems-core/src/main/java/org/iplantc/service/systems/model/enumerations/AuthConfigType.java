package org.iplantc.service.systems.model.enumerations;

public enum AuthConfigType
{
	TOKEN, KERBEROSE, PASSWORD, X509, LOCAL, SSHKEYS, PAM, APIKEYS, ANONYMOUS;
	
	public boolean accepts(CredentialServerProtocolType type) {
		if (this.equals(X509)) {
			return (type.equals(CredentialServerProtocolType.MYPROXY) ||
					type.equals(CredentialServerProtocolType.VOMS) ||
					type.equals(CredentialServerProtocolType.OA4MP) || 
					type.equals(CredentialServerProtocolType.MPG));
		} else if (this.equals(PASSWORD) || this.equals(SSHKEYS) || this.equals(ANONYMOUS)) {
			return (type.equals(CredentialServerProtocolType.NONE));
		} else if (this.equals(KERBEROSE)) {
			return (type.equals(CredentialServerProtocolType.KERBEROSE));
		} else if (this.equals(TOKEN)) {
			return (type.equals(CredentialServerProtocolType.OAUTH2));
		} else if (this.equals(APIKEYS)) {
			return type.equals(CredentialServerProtocolType.NONE);
		} else {
			return false;
		}
	}
	
	@Override
	public String toString() {
		return name();
	}
}
