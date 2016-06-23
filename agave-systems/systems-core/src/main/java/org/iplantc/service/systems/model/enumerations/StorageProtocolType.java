package org.iplantc.service.systems.model.enumerations;

public enum StorageProtocolType implements ProtocolType {
	GRIDFTP, FTP, SFTP, IRODS, IRODS4, LOCAL, AZURE, S3, SWIFT, HTTP, HTTPS;

	@Override
	public boolean accepts(AuthConfigType type)
	{
		if (this == GRIDFTP) {
			return (type == AuthConfigType.X509);
		} else if (this == FTP) {
			return (type == AuthConfigType.PASSWORD || type == AuthConfigType.ANONYMOUS);
		} else if (this == SFTP) {
			return (type == AuthConfigType.PASSWORD) || type == AuthConfigType.SSHKEYS;
		} else if (this == IRODS || this == IRODS4) {
			return type == AuthConfigType.PASSWORD || type == AuthConfigType.PAM || type == AuthConfigType.X509;
		} else if (this == LOCAL) {
			return type == AuthConfigType.LOCAL;
		} else if (this == AZURE
				|| this == S3
//				|| this == SWIFT)
				) {
			return type == AuthConfigType.APIKEYS;
		} else {
			return false;
		}
	}
	
	/**
	 * Returns whether this {@link StorageProtocolType} supports 
	 * specifying the authentication parameters in the URL. This is 
	 * used to determine whether Agave can transfer a URL with this
	 * protocol value given as the schema. 
	 * @return true if the sytem can be authenticated and accessed from a
	 * URL, false otherwise.
	 */
	public boolean allowsURLAuth() {
	    if (this == GRIDFTP) {
            return false;
        } else if (this == FTP) {
            return true;
        } else if (this == SFTP) {
            return true;
        } else if (this == IRODS || this == IRODS4) {
            return false;
        } else if (this == LOCAL) {
            return false;
        } else if (this == AZURE
                || this == S3
//              || this == SWIFT)
                ) {
            return false;
        } else if (this == HTTP || this == HTTPS) {
            return true;
        } else {
            return false;
        }
	}
	
	@Override
	public String toString() {
		return name();
	}
}

