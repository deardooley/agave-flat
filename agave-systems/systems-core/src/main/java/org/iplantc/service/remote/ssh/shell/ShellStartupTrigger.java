package org.iplantc.service.remote.ssh.shell;

import java.io.IOException;

public abstract interface ShellStartupTrigger
{
  public abstract boolean canStartShell(String paramString, ShellWriter paramShellWriter)
    throws IOException;
}