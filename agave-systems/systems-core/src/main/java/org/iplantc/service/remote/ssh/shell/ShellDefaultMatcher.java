package org.iplantc.service.remote.ssh.shell;

public class ShellDefaultMatcher implements ShellMatcher {
	public boolean matches(String line, String pattern) {
		return line.indexOf(pattern) > -1;
	}
}