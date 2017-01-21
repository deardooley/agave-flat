package org.iplantc.service.remote.ssh.shell;

import java.io.InputStream;
import java.io.OutputStream;

import com.sshtools.ssh.SshIOException;

public class ShellProcess {
	Shell shell;
	ShellInputStream in;

	ShellProcess(Shell shell, ShellInputStream in) {
		this.shell = shell;
		this.in = in;
	}

	public InputStream getInputStream() {
		return in;
	}

	public OutputStream getOutputStream() throws SshIOException {
		return shell.sessionOut;
	}

	public int getExitCode() {
		return in.getExitCode();
	}

	public boolean hasSucceeded() {
		return in.hasSucceeded();
	}

	public boolean isActive() {
		return in.isActive();
	}

	public String getCommandOutput() {
		return in.getCommandOutput();
	}

	public Shell getShell() {
		return shell;
	}
}