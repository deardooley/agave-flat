package org.iplantc.service.remote.ssh.shell;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.ssh.SshException;
import com.sshtools.ssh.SshIOException;

public class ShellController
  implements ShellReader, ShellWriter
{
  protected Shell shell;
  protected ShellMatcher matcher = null;
  
  protected InputStream in;
  static Logger log = LoggerFactory.getLogger(ShellController.class);
  
  ShellController(Shell shell, ShellMatcher matcher, InputStream in) {
    this.shell = shell;
    this.matcher = matcher;
    this.in = in;
  }
  
  public void setMatcher(ShellMatcher matcher) {
    this.matcher = matcher;
  }
  
  public void interrupt()
    throws IOException
  {
    shell.type(new String(new char[] { '\003' }));
  }
  

  public synchronized void type(String string)
    throws IOException
  {
    shell.type(string);
  }
  

  public synchronized void carriageReturn()
    throws IOException
  {
    shell.carriageReturn();
  }
  

  public synchronized void typeAndReturn(String string)
    throws IOException
  {
    shell.typeAndReturn(string);
  }
  







  public synchronized boolean expect(String pattern)
    throws ShellTimeoutException, SshException
  {
    return expect(pattern, false, 0L, 0L);
  }
  









  public synchronized boolean expect(String pattern, boolean consumeRemainingLine)
    throws ShellTimeoutException, SshException
  {
    return expect(pattern, consumeRemainingLine, 0L, 0L);
  }
  









  public synchronized boolean expect(String pattern, long timeout)
    throws ShellTimeoutException, SshException
  {
    return expect(pattern, false, timeout, 0L);
  }
  









  public synchronized boolean expect(String pattern, boolean consumeRemainingLine, long timeout)
    throws ShellTimeoutException, SshException
  {
    return expect(pattern, consumeRemainingLine, timeout, 0L);
  }
  





  public synchronized boolean expectNextLine(String pattern)
    throws ShellTimeoutException, SshException
  {
    return expect(pattern, false, 0L, 1L);
  }
  







  public synchronized boolean expectNextLine(String pattern, boolean consumeRemainingLine)
    throws ShellTimeoutException, SshException
  {
    return expect(pattern, consumeRemainingLine, 0L, 1L);
  }
  








  public synchronized boolean expectNextLine(String pattern, boolean consumeRemainingLine, long timeout)
    throws ShellTimeoutException, SshException
  {
    return expect(pattern, consumeRemainingLine, timeout, 1L);
  }
  









  public synchronized boolean expect(String pattern, boolean consumeRemainingLine, long timeout, long maxLines)
    throws ShellTimeoutException, SshException
  {
    StringBuffer line = new StringBuffer();
    long time = System.currentTimeMillis();
    long lines = 0L;
    
    while ((System.currentTimeMillis() - time < timeout) || (timeout == 0L))
    {
      if ((maxLines > 0L) && (lines >= maxLines)) {
        return false;
      }
      try {
        int ch = in.read();
        if (ch == -1) {
          return false;
        }
        if ((ch != 10) && (ch != 13)) {
          line.append((char)ch);
        }
        
        if (matcher.matches(line.toString(), pattern)) {
          if (log.isDebugEnabled())
            log.debug("Matched: [" + pattern + "] " + line.toString());
          while ((consumeRemainingLine) && (ch != 10) && (ch != -1) && 
          

            (ch != 10) && (ch != -1)) {}
          
          if (log.isDebugEnabled())
            log.debug("Shell output: " + line.toString());
          return true;
        }
        
        if (ch == 10) {
          lines += 1L;
          if (log.isDebugEnabled())
            log.debug("Shell output: " + line.toString());
          line.delete(0, line.length());
        }
      }
      catch (SshIOException e) {
        if (e.getRealException().getReason() != 21)
        {

          throw e.getRealException();
        }
      } catch (IOException e) {
        throw new SshException(e);
      }
    }
    
    throw new ShellTimeoutException();
  }
  
  public boolean isActive() {
    return shell.inStartup();
  }
  

  public synchronized String readLine()
    throws SshException, ShellTimeoutException
  {
    return readLine(0L);
  }
  


  public synchronized String readLine(long timeout)
    throws SshException, ShellTimeoutException
  {
    if (!isActive()) {
      return null;
    }
    StringBuffer line = new StringBuffer();
    

    long time = System.currentTimeMillis();
    do
    {
      try {
        int ch = in.read();
        if ((ch == -1) || (ch == 10)) {
          if ((line.length() == 0) && (ch == -1))
            return null;
          return line.toString();
        }
        if ((ch != 10) && (ch != 13)) {
          line.append((char)ch);
        }
      } catch (SshIOException e) {
        if (e.getRealException().getReason() != 21)
        {

          throw e.getRealException();
        }
      } catch (IOException e) {
        throw new SshException(e);
      }
      
    }
    while ((System.currentTimeMillis() - time < timeout) || (timeout == 0L));
    
    throw new ShellTimeoutException();
  }
}