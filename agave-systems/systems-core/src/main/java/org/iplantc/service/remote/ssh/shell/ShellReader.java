package org.iplantc.service.remote.ssh.shell;

import com.sshtools.ssh.SshException;

public abstract interface ShellReader
{
  public abstract String readLine()
    throws SshException, ShellTimeoutException;
  
  public abstract String readLine(long paramLong)
    throws SshException, ShellTimeoutException;
}