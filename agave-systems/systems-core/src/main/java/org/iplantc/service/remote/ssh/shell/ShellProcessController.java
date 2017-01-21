package org.iplantc.service.remote.ssh.shell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShellProcessController
  extends ShellController
{
  ShellProcess process;
  static Logger log = LoggerFactory.getLogger(ShellProcessController.class);
  
  public ShellProcessController(ShellProcess process) {
    this(process, new ShellDefaultMatcher());
  }
  
  public ShellProcessController(ShellProcess process, ShellMatcher matcher) {
    super(process.getShell(), matcher, process.getInputStream());
    this.process = process;
  }
  
  public boolean isActive()
  {
    return process.isActive();
  }
}