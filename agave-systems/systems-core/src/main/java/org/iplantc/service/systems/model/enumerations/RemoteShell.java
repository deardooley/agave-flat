package org.iplantc.service.systems.model.enumerations;

public enum RemoteShell
{
	BASH, TCSH, CSH;
	
	public String getDefaultRCFile() {
		if (this == CSH) {
			return "~/.cshrc";
		} else if (this == TCSH) {
			return "~/.tcsh";
		} else {
			return "~/.bashrc";
		}
	}
}
