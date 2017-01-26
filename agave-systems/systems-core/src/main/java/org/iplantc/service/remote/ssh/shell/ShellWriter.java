package org.iplantc.service.remote.ssh.shell;

import java.io.IOException;

public abstract interface ShellWriter
{
  public abstract void interrupt()
    throws IOException;
  
  public abstract void type(String paramString)
    throws IOException;
  
  public abstract void carriageReturn()
    throws IOException;
  
  public abstract void typeAndReturn(String paramString)
    throws IOException;
}