package org.iplantc.service.remote.ssh.shell;

public abstract interface ShellMatcher {
	public abstract boolean matches(String paramString1, String paramString2);
}