package org.iplantc.service.remote.ssh.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.sshtools.ssh.SocketTimeoutSupport;
import com.sshtools.ssh.SshTransport;

public class SocketWrapper
  implements SshTransport, SocketTimeoutSupport
{
  protected Socket socket;
  
  public SocketWrapper(Socket socket)
  {
    this.socket = socket;
  }
  
  public InputStream getInputStream() throws IOException {
    return socket.getInputStream();
  }
  
  public OutputStream getOutputStream() throws IOException {
    return socket.getOutputStream();
  }
  
  public String getHost() {
    return socket.getInetAddress() == null ? "proxied" : socket.getInetAddress().getHostAddress();
  }
  
  public int getPort() {
    return socket.getPort();
  }
  
  public void close() throws IOException {
    socket.close();
  }
  
  public SshTransport duplicate() throws IOException {
    return new SocketWrapper(new Socket(getHost(), socket.getPort()));
  }
  
  public void setSoTimeout(int timeout) throws IOException {
    socket.setSoTimeout(timeout);
  }
  
  public int getSoTimeout() throws IOException {
    return socket.getSoTimeout();
  }
}